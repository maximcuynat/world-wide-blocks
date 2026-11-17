package dev.netcraft.network.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import dev.netcraft.network.DeviceId;
import dev.netcraft.network.Link;
import dev.netcraft.network.NetworkGraph;
import dev.netcraft.network.NetworkNode;
import dev.netcraft.network.PortRef;
import dev.netcraft.network.protocol.EthernetFrame;
import dev.netcraft.network.protocol.MacAddress;

/**
 * Fait circuler de vraies trames Ethernet entre les appareils du graphe.
 *
 * <p>Une trame émise sur un port est déposée dans une file d'attente, puis livrée à l'autre bout du
 * câble lors du traitement de la file, à chaque tick du serveur : elle arrive instantanément, sans
 * temps de trajet le long du câble. Les trames produites pendant ce traitement (relais d'un switch,
 * réponses) sont livrées dans le même tick, dans la limite de {@link SimulationLimits#maxDeliveriesPerTick()}.
 *
 * <p>Protection contre les boucles, en attendant le STP du lot 8b : chaque trame émise reçoit une
 * trace, partagée par toutes ses copies relayées. Une trace n'existe que dans le moteur, pas dans la
 * trame. Si une copie revient à un switch que la trace a déjà traversé, il existe deux chemins entre
 * eux : la copie est supprimée et la boucle signalée. Dans un réseau sans boucle, cela n'arrive jamais.
 *
 * <p>Horloge : le temps de la simulation est celui du tick en cours de traitement. Un changement du
 * graphe (branchement, débranchement, appareil posé ou retiré) survient souvent entre deux ticks :
 * un clic de joueur est traité après le tick de la simulation. Il est donc mis en attente et appliqué
 * au début du tick suivant, avec le temps de ce tick. Ainsi ses trames, ses minuteries et ses
 * signalements partent du tick même où ses trames sont livrées. (Appliqué sur-le-champ avec le temps
 * du tick précédent, une présence émise au branchement était livrée un tick après sa date : les
 * présences suivantes tombaient 1 199 ticks après le signalement de boucle au lieu de 1 200.)
 *
 * <p>L'état d'exécution (comportement des appareils, file, minuteries) n'est pas sauvegardé : au
 * démarrage du serveur, chaque appareil repart de zéro et chaque câble existant est annoncé comme
 * branché, comme après une coupure de courant.
 *
 * <p>Java pur, sans classe Minecraft. Pas thread-safe : n'y accéder que depuis le thread du serveur.
 */
public final class NetworkSimulation {
    private final NetworkGraph graph;
    private final SimulationLimits limits;
    private final Map<DeviceId, DeviceLogic> logics = new HashMap<>();
    private final ArrayDeque<Delivery> queue = new ArrayDeque<>();
    /** Changements du graphe survenus depuis le dernier tick, appliqués dans l'ordre au tick suivant. */
    private final ArrayDeque<Runnable> pendingChanges = new ArrayDeque<>();
    private final PriorityQueue<Timer> timers = new PriorityQueue<>(
            Comparator.comparingLong(Timer::due).thenComparingLong(Timer::sequence));
    private final List<SimulationEvent> events = new ArrayList<>();
    private final Map<DeviceId, Long> lastLoopReport = new HashMap<>();
    private Long lastSaturationReport;
    private FrameTap tap = FrameTap.NONE;
    private long now;
    private long timerSequence;
    private boolean started;

    /** Une trame en route vers l'autre bout d'un câble. */
    private record Delivery(PortRef ingress, UUID linkId, EthernetFrame frame, Trace trace) {
    }

    /** Une émission et toutes ses copies relayées : les switchs qu'elles ont déjà traversés. */
    private static final class Trace {
        private final Set<DeviceId> visited = new HashSet<>();
    }

    private record Timer(long due, long sequence, DeviceId device, DeviceLogic logic, Object key) {
    }

    public NetworkSimulation(NetworkGraph graph, SimulationLimits limits) {
        this.graph = graph;
        this.limits = limits;
    }

    public NetworkSimulation(NetworkGraph graph) {
        this(graph, SimulationLimits.DEFAULT);
    }

    public void setTap(FrameTap tap) {
        this.tap = tap;
    }

    // ---- Lecture ----

    public SimulationLimits limits() {
        return this.limits;
    }

    public Optional<DeviceLogic> logic(DeviceId device) {
        return Optional.ofNullable(this.logics.get(device));
    }

    /** Trames en attente de livraison. */
    public int pendingFrames() {
        return this.queue.size();
    }

    /** Événements survenus depuis le dernier appel, à transformer en messages. */
    public List<SimulationEvent> drainEvents() {
        List<SimulationEvent> drained = List.copyOf(this.events);
        this.events.clear();
        return drained;
    }

    // ---- Changements du graphe ----
    // Appliqués au début du tick suivant, avec son temps (voir la classe). Avant le premier tick, ils
    // sont ignorés : le démarrage relit tout le graphe.
    //
    // Un changement en attente ne s'applique qu'à ce qui existe encore dans le graphe au moment du
    // tick : un appareil ou un câble retiré entre-temps est déjà remplacé par son retrait, lui aussi en
    // attente. (Un poste posé, câblé et retiré dans le même tick faisait planter le serveur.)

    /** Un appareil vient d'être ajouté, ou remplacé par un autre (son ancien comportement est oublié). */
    public void deviceAdded(NetworkNode node) {
        this.later(() -> {
            if (this.graph.node(node.id()).filter(node::equals).isPresent()) {
                this.start(node);
            }
        });
    }

    public void deviceRemoved(DeviceId device) {
        this.later(() -> {
            this.logics.remove(device);
            this.lastLoopReport.remove(device);
        });
    }

    public void linkUp(Link link) {
        this.later(() -> this.notifyLink(link, true));
    }

    /** À appeler après le retrait du câble du graphe. */
    public void linkDown(Link link) {
        this.later(() -> this.notifyLink(link, false));
    }

    private void later(Runnable change) {
        if (this.started) {
            this.pendingChanges.add(change);
        }
    }

    // ---- Tick ----

    /**
     * Fait avancer la simulation jusqu'à {@code now} : changements du graphe survenus depuis le tick
     * précédent, minuteries échues, puis livraison des trames.
     *
     * @param now temps de jeu, en ticks ; il ne doit jamais reculer
     */
    public void tick(long now) {
        this.now = now;
        if (!this.started) {
            this.started = true;
            this.graph.nodes().forEach(this::start);
            this.graph.links().forEach(link -> this.notifyLink(link, true));
        }
        while (!this.pendingChanges.isEmpty()) {
            this.pendingChanges.poll().run();
        }
        while (!this.timers.isEmpty() && this.timers.peek().due() <= now) {
            Timer timer = this.timers.poll();
            // Un appareil retiré ou remplacé a un autre comportement : ses minuteries sont périmées.
            if (this.logics.get(timer.device()) == timer.logic()) {
                timer.logic().onTimer(new Context(timer.device(), timer.logic(), null), timer.key());
            }
        }
        int budget = this.limits.maxDeliveriesPerTick();
        while (budget > 0 && !this.queue.isEmpty()) {
            budget--;
            this.deliver(this.queue.poll());
        }
    }

    // ---- Interne ----

    private void start(NetworkNode node) {
        DeviceLogic logic = DeviceLogics.create(node);
        this.logics.put(node.id(), logic);
        logic.onStart(new Context(node.id(), logic, null));
    }

    private void notifyLink(Link link, boolean up) {
        if (up && !this.isInGraph(link)) {
            return; // débranché depuis : son débranchement suit dans la file
        }
        for (PortRef end : List.of(link.a(), link.b())) {
            DeviceLogic logic = this.logics.get(end.device());
            if (logic == null || this.graph.node(end.device()).isEmpty()) {
                continue; // appareil retiré depuis : son comportement va être oublié
            }
            Context context = new Context(end.device(), logic, null);
            if (up) {
                logic.onLinkUp(context, end.index());
            } else {
                logic.onLinkDown(context, end.index());
            }
        }
    }

    private boolean isInGraph(Link link) {
        return this.graph.linkAt(link.a()).filter(current -> current.id().equals(link.id())).isPresent();
    }

    private void emit(PortRef egress, EthernetFrame frame, Trace trace) {
        this.tap.onFrame(this.now, egress, FrameDirection.SENT, frame);
        Optional<Link> link = this.graph.linkAt(egress);
        if (link.isEmpty()) {
            return; // aucun câble sur ce port : la trame est perdue
        }
        if (this.queue.size() >= this.limits.maxQueued()) {
            this.reportSaturation();
            return;
        }
        this.queue.add(new Delivery(link.get().otherEnd(egress), link.get().id(), frame, trace));
    }

    private void deliver(Delivery delivery) {
        PortRef ingress = delivery.ingress();
        Optional<Link> link = this.graph.linkAt(ingress);
        if (link.isEmpty() || !link.get().id().equals(delivery.linkId())) {
            return; // câble retiré (ou remplacé) avant la livraison
        }
        DeviceLogic logic = this.logics.get(ingress.device());
        if (logic == null) {
            return;
        }
        this.tap.onFrame(this.now, ingress, FrameDirection.RECEIVED, delivery.frame());
        if (logic.bridges() && !delivery.trace().visited.add(ingress.device())) {
            this.reportLoop(ingress.device(), ingress.index());
            return;
        }
        logic.onFrame(new Context(ingress.device(), logic, delivery.trace()), ingress.index(), delivery.frame());
    }

    private void reportLoop(DeviceId device, int port) {
        Long last = this.lastLoopReport.get(device);
        if (last == null || this.now - last >= this.limits.reportIntervalTicks()) {
            this.lastLoopReport.put(device, this.now);
            this.events.add(new SimulationEvent.LoopDetected(device, port));
        }
    }

    private void reportSaturation() {
        if (this.lastSaturationReport == null || this.now - this.lastSaturationReport >= this.limits.reportIntervalTicks()) {
            this.lastSaturationReport = this.now;
            this.events.add(new SimulationEvent.QueueSaturated());
        }
    }

    /** Contexte d'un appareil pendant un appel ; {@code trace} est la trame en cours, s'il y en a une. */
    private final class Context implements DeviceContext {
        private final DeviceId device;
        private final DeviceLogic logic;
        private final Trace trace;

        private Context(DeviceId device, DeviceLogic logic, Trace trace) {
            this.device = device;
            this.logic = logic;
            this.trace = trace;
        }

        @Override
        public long now() {
            return NetworkSimulation.this.now;
        }

        @Override
        public DeviceId device() {
            return this.device;
        }

        @Override
        public MacAddress mac(int port) {
            return graph.macOf(new PortRef(this.device, port))
                    .orElseThrow(() -> new IllegalArgumentException("Port " + port + " inconnu sur " + this.device));
        }

        @Override
        public boolean isLinked(int port) {
            return graph.linkAt(new PortRef(this.device, port)).isPresent();
        }

        @Override
        public void send(int port, EthernetFrame frame) {
            Trace fresh = new Trace();
            if (this.logic.bridges()) {
                fresh.visited.add(this.device);
            }
            emit(new PortRef(this.device, port), frame, fresh);
        }

        @Override
        public void relay(int port, EthernetFrame frame) {
            if (this.trace == null) {
                throw new IllegalStateException("Aucune trame en cours à relayer");
            }
            emit(new PortRef(this.device, port), frame, this.trace);
        }

        @Override
        public void schedule(long delayTicks, Object key) {
            if (delayTicks <= 0) {
                throw new IllegalArgumentException("Délai non positif : " + delayTicks);
            }
            timers.add(new Timer(NetworkSimulation.this.now + delayTicks, timerSequence++, this.device, this.logic, key));
        }
    }
}
