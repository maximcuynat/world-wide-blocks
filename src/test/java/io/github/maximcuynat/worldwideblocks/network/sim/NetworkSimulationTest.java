package io.github.maximcuynat.worldwideblocks.network.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.message.MessageThrottle;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.DeviceRole;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.LinkMedium;
import io.github.maximcuynat.worldwideblocks.network.NetworkGraph;
import io.github.maximcuynat.worldwideblocks.network.NetworkNode;
import io.github.maximcuynat.worldwideblocks.network.PortKind;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.network.protocol.EtherType;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.switching.MacTable;

class NetworkSimulationTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final List<PortKind> BOX_PORTS =
            List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE);

    /** Ce que le point d'écoute a vu passer. */
    private record Seen(long now, PortRef port, FrameDirection direction, EthernetFrame frame) {
    }

    private NetworkGraph graph;
    private NetworkSimulation sim;
    private final List<Seen> seen = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.graph = new NetworkGraph(new SplittableRandom(7));
        this.useLimits(SimulationLimits.DEFAULT);
    }

    private void useLimits(SimulationLimits limits) {
        this.sim = new NetworkSimulation(this.graph, limits);
        this.sim.setTap((now, port, direction, frame) -> this.seen.add(new Seen(now, port, direction, frame)));
    }

    // ---- Montage du réseau, comme le fait NetworkManager ----

    private DeviceId host(int x) {
        NetworkNode node = new NetworkNode(new DeviceId(OVERWORLD, x, 64, 0), "worldwideblocks:pc_tower", 1);
        this.graph.putNode(node);
        this.sim.deviceAdded(node);
        return node.id();
    }

    private DeviceId box(int x) {
        NetworkNode node = new NetworkNode(new DeviceId(OVERWORLD, x, 64, 0), "worldwideblocks:internet_box", DeviceRole.BOX, BOX_PORTS);
        this.graph.putNode(node);
        this.sim.deviceAdded(node);
        return node.id();
    }

    private Link plug(DeviceId a, int portA, DeviceId b, int portB) {
        Link link = this.graph.connect(new PortRef(a, portA), new PortRef(b, portB), LinkMedium.COPPER_ETHERNET, true)
                .link().orElseThrow();
        this.sim.linkUp(link);
        return link;
    }

    private void unplug(DeviceId device, int port) {
        this.sim.linkDown(this.graph.disconnect(new PortRef(device, port)).orElseThrow());
    }

    private void remove(DeviceId device) {
        this.graph.removeDevice(device).forEach(this.sim::linkDown);
        this.sim.deviceRemoved(device);
    }

    // ---- Lecture ----

    private MacAddress mac(DeviceId device, int port) {
        return this.graph.macOf(new PortRef(device, port)).orElseThrow();
    }

    private List<MacTable.Entry> learned(DeviceId box) {
        return ((BoxLogic) this.sim.logic(box).orElseThrow()).learnedMacs();
    }

    private List<Integer> learnedPorts(DeviceId box, MacAddress mac) {
        return this.learned(box).stream().filter(e -> e.mac().equals(mac)).map(MacTable.Entry::port).toList();
    }

    private long presencesSentBy(DeviceId host, long from, long to) {
        return this.seen.stream()
                .filter(s -> s.direction() == FrameDirection.SENT && s.port().device().equals(host))
                .filter(s -> s.now() >= from && s.now() <= to)
                .count();
    }

    private void tickUntil(long from, long to) {
        for (long t = from; t <= to; t++) {
            this.sim.tick(t);
        }
    }

    // ---- Scénarios ----

    @Test
    void laBoxApprendQuelleMacEstDerriereQuelPort() {
        DeviceId box = box(0);
        DeviceId tour = host(5);
        DeviceId portable = host(10);
        plug(box, 0, tour, 0);
        plug(box, 1, portable, 0);

        sim.tick(0);

        assertEquals(List.of(0), learnedPorts(box, mac(tour, 0)));
        assertEquals(List.of(1), learnedPorts(box, mac(portable, 0)));
        assertEquals(2, learned(box).size());
        assertEquals(0, sim.pendingFrames());
    }

    @Test
    void laPresenceEstUneDiffusionDeTypeExperimentalRelayeeAuxAutresPostes() {
        DeviceId box = box(0);
        DeviceId tour = host(5);
        DeviceId portable = host(10);
        plug(box, 0, tour, 0);
        plug(box, 1, portable, 0);

        sim.tick(0);

        Seen recu = seen.stream()
                .filter(s -> s.direction() == FrameDirection.RECEIVED && s.port().device().equals(portable))
                .filter(s -> s.frame().source().equals(mac(tour, 0)))
                .findFirst().orElseThrow();
        assertTrue(recu.frame().isBroadcast());
        assertEquals(EtherType.LOCAL_EXPERIMENTAL, recu.frame().type());
        // La box l'a reçue sur le port 0 et l'a renvoyée sur les ports 1, 2 et 3, pas sur l'arrivée de ligne.
        List<Integer> sortiesBox = seen.stream()
                .filter(s -> s.direction() == FrameDirection.SENT && s.port().device().equals(box))
                .filter(s -> s.frame().source().equals(mac(tour, 0)))
                .map(s -> s.port().index()).toList();
        assertEquals(List.of(1, 2, 3), sortiesBox);
    }

    @Test
    void lePosteRediffuseSaPresenceToutesLes30Secondes() {
        DeviceId box = box(0);
        DeviceId tour = host(5);
        plug(box, 0, tour, 0);

        tickUntil(0, 1300);

        assertEquals(3, presencesSentBy(tour, 0, 1300)); // ticks 0, 600 et 1200
    }

    @Test
    void retirerLeCableOublieLaMacAuTickSuivantEtArreteLaPresence() {
        DeviceId box = box(0);
        DeviceId tour = host(5);
        DeviceId portable = host(10);
        plug(box, 0, tour, 0);
        plug(box, 1, portable, 0);
        sim.tick(0);

        unplug(box, 0);
        sim.tick(1);

        assertTrue(learnedPorts(box, mac(tour, 0)).isEmpty());
        assertEquals(List.of(1), learnedPorts(box, mac(portable, 0)));
        tickUntil(2, 700);
        assertEquals(0, presencesSentBy(tour, 1, 700));
    }

    @Test
    void uneEntreeApriseAuLoinExpireApres300Secondes() {
        // box1 port 0 <-> box2 port 0, tour sur box2 port 1.
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        DeviceId tour = host(25);
        plug(box1, 0, box2, 0);
        plug(box2, 1, tour, 0);
        sim.tick(0);
        assertEquals(List.of(0), learnedPorts(box1, mac(tour, 0)));

        unplug(box2, 1);
        sim.tick(1);

        // box2 l'oublie au tick suivant, box1 (dont le câble est toujours là) seulement après le délai.
        assertTrue(learnedPorts(box2, mac(tour, 0)).isEmpty());
        sim.tick(MacTable.DEFAULT_AGING_TICKS - 10);
        assertEquals(List.of(0), learnedPorts(box1, mac(tour, 0)));
        sim.tick(MacTable.DEFAULT_AGING_TICKS + 10);
        assertTrue(learnedPorts(box1, mac(tour, 0)).isEmpty());
    }

    @Test
    void uneTrameEnRouteSurUnCableRetireNEstPasLivree() {
        // Une seule livraison par tick : la présence du portable reste dans la file après le tick 0.
        useLimits(new SimulationLimits(1, 50_000, 1200));
        DeviceId box = box(0);
        DeviceId tour = host(5);
        DeviceId portable = host(10);
        plug(box, 0, tour, 0);
        plug(box, 1, portable, 0);
        sim.tick(0);
        // Restent en file : la présence du portable et la présence de la tour relayée vers lui.
        assertTrue(sim.pendingFrames() > 0);

        unplug(box, 1);
        tickUntil(1, 2);

        assertEquals(List.of(0), learnedPorts(box, mac(tour, 0)));
        assertTrue(learnedPorts(box, mac(portable, 0)).isEmpty());
        assertEquals(0, sim.pendingFrames());
    }

    @Test
    void unAppareilRetireNEmetPlusRien() {
        DeviceId box = box(0);
        DeviceId tour = host(5);
        plug(box, 0, tour, 0);
        sim.tick(0);

        remove(tour);
        tickUntil(1, 700);

        assertEquals(0, presencesSentBy(tour, 1, 700));
        assertTrue(learned(box).isEmpty());
        assertTrue(sim.logic(tour).isEmpty());
    }

    @Test
    void auDemarrageChaqueCableExistantEstAnnonce() {
        // Graphe relu depuis la sauvegarde : les câbles existent avant la simulation.
        DeviceId box = new DeviceId(OVERWORLD, 0, 64, 0);
        DeviceId tour = new DeviceId(OVERWORLD, 5, 64, 0);
        graph.putNode(new NetworkNode(box, "worldwideblocks:internet_box", DeviceRole.BOX, BOX_PORTS));
        graph.putNode(new NetworkNode(tour, "worldwideblocks:pc_tower", 1));
        graph.connect(new PortRef(box, 2), new PortRef(tour, 0), LinkMedium.COPPER_ETHERNET, true);

        sim.tick(100);

        assertEquals(List.of(2), learnedPorts(box, mac(tour, 0)));
    }

    @Test
    void deuxBoxEnBoucleNeTournentPasALInfiniEtLaBoucleEstSignalee() {
        // Deux câbles entre les deux box : une trame diffusée reviendrait sans fin.
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        DeviceId tour = host(5);
        plug(box1, 1, box2, 1);
        plug(box1, 2, box2, 2);
        plug(box1, 0, tour, 0);

        sim.tick(0);

        assertEquals(0, sim.pendingFrames());
        long receptions = seen.stream().filter(s -> s.direction() == FrameDirection.RECEIVED).count();
        assertTrue(receptions < 10, "réceptions : " + receptions);
        // Les deux box voient la boucle : un seul événement, qui les cite toutes les deux.
        List<SimulationEvent> events = sim.drainEvents();
        assertEquals(1, events.size(), events.toString());
        SimulationEvent.LoopDetected loop = (SimulationEvent.LoopDetected) events.getFirst();
        assertEquals(box1, loop.network());
        assertTrue(loop.points().contains(new SimulationEvent.LoopPoint(box1, 2)), loop.toString());
        assertTrue(loop.points().contains(new SimulationEvent.LoopPoint(box2, 2)), loop.toString());
        // La MAC du poste n'a pas basculé de port à cause des copies supprimées.
        assertEquals(List.of(0), learnedPorts(box1, mac(tour, 0)));
    }

    @Test
    void laSimulationSignaleLaBoucleAChaqueTickOuElleLaVoitSansLimiteDeFrequence() {
        // La limite d'une fois par minute appartient au module des messages, pas au moteur.
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        DeviceId tour = host(5);
        plug(box1, 1, box2, 1);
        plug(box1, 2, box2, 2);
        plug(box1, 0, tour, 0);
        sim.tick(0);
        sim.drainEvents();

        sim.tick(599);
        assertTrue(sim.drainEvents().isEmpty(), "aucune trame entre deux présences");

        sim.tick(600); // nouvelle présence, même boucle
        List<SimulationEvent> events = sim.drainEvents();
        assertEquals(1, events.size(), events.toString());
        assertEquals(box1, ((SimulationEvent.LoopDetected) events.getFirst()).network());
    }

    @Test
    void deuxBouclesDansDeuxReseauxDonnentDeuxEvenements() {
        DeviceId a1 = box(0);
        DeviceId a2 = box(20);
        plug(a1, 1, a2, 1);
        plug(a1, 2, a2, 2);
        plug(a1, 0, host(5), 0);
        DeviceId b1 = box(100);
        DeviceId b2 = box(120);
        plug(b1, 1, b2, 1);
        plug(b1, 2, b2, 2);
        plug(b1, 0, host(105), 0);

        sim.tick(0);

        List<DeviceId> networks = sim.drainEvents().stream()
                .map(event -> ((SimulationEvent.LoopDetected) event).network())
                .toList();
        assertEquals(List.of(a1, b1), networks);
    }

    @Test
    void auDelaDeLaLimiteParTickLesTramesAttendentLeTickSuivant() {
        useLimits(new SimulationLimits(2, 50_000, 1200));
        DeviceId box = box(0);
        DeviceId tour = host(5);
        DeviceId portable = host(10);
        plug(box, 0, tour, 0);
        plug(box, 1, portable, 0);

        sim.tick(0);
        assertTrue(sim.pendingFrames() > 0);

        sim.tick(1);
        sim.tick(2);
        assertEquals(0, sim.pendingFrames());
        assertEquals(2, learned(box).size());
    }

    @Test
    void uneFilePleinePerdLesNouvellesTramesEtLeSignale() {
        useLimits(new SimulationLimits(10_000, 1, 1200));
        DeviceId box = box(0);
        plug(box, 0, host(5), 0);
        plug(box, 1, host(10), 0);

        sim.tick(0);

        // Au plus un événement par tick, même si beaucoup de trames sont perdues.
        assertEquals(List.of(new SimulationEvent.QueueSaturated()), sim.drainEvents());
    }

    // ---- Horloge : changements survenus entre deux ticks ----

    @Test
    void unBranchementEntreDeuxTicksPartDuTickSuivant() {
        DeviceId box = box(0);
        sim.tick(100);
        DeviceId tour = host(5);
        plug(box, 0, tour, 0); // entre le tick 100 et le tick 101, comme un clic de joueur

        sim.tick(101);
        // Apprise au tick même de l'envoi : la trame est livrée dans le tick où elle part.
        assertEquals(101L, learned(box).getFirst().lastSeen());
        tickUntil(102, 800);

        List<Long> envois = seen.stream()
                .filter(s -> s.direction() == FrameDirection.SENT && s.port().device().equals(tour))
                .map(Seen::now).toList();
        assertEquals(List.of(101L, 701L), envois);
    }

    @Test
    void uneBoucleNeeEntreDeuxTicksEstSignaleeToutesLesMinutesExactement() {
        // Défaut relevé le 2026-11-18 par le test en jeu : signalements à 1 800 ticks d'écart au lieu de 1 200.
        // Le moteur voit la boucle à chaque présence (toutes les 600 ticks) ; le module des messages,
        // avec le même temps, n'en transmet qu'une par minute.
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        plug(box1, 1, box2, 1);
        plug(box1, 2, box2, 2);
        sim.tick(50);
        DeviceId tour = host(5);
        plug(box1, 0, tour, 0); // entre le tick 50 et le tick 51

        MessageThrottle throttle = new MessageThrottle(SimulationLimits.DEFAULT.reportIntervalTicks());
        List<Long> vues = new ArrayList<>();
        List<Long> signalements = new ArrayList<>();
        for (long t = 51; t <= 2500; t++) {
            sim.tick(t);
            for (SimulationEvent event : sim.drainEvents()) {
                if (event instanceof SimulationEvent.LoopDetected loop) {
                    vues.add(t);
                    if (throttle.allow("loop:" + loop.network(), t)) {
                        signalements.add(t);
                    }
                }
            }
        }

        assertEquals(List.of(51L, 651L, 1251L, 1851L, 2451L), vues);
        assertEquals(List.of(51L, 1251L, 2451L), signalements);
    }

    @Test
    void uneMacExpireExactement300SecondesApresSaDerniereTrame() {
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        plug(box1, 0, box2, 0);
        sim.tick(0);
        DeviceId tour = host(25);
        plug(box2, 1, tour, 0); // entre le tick 0 et le tick 1 : présence livrée au tick 1
        sim.tick(1);
        unplug(box2, 1);
        assertEquals(1L, learned(box1).getFirst().lastSeen());

        long expiration = 1 + MacTable.DEFAULT_AGING_TICKS;
        tickUntil(2, expiration - 1);
        assertEquals(List.of(0), learnedPorts(box1, mac(tour, 0)));
        // Expirée à 6001 ; retirée de la table au nettoyage suivant, au plus une seconde après.
        tickUntil(expiration, expiration + BoxLogic.PURGE_INTERVAL_TICKS - 1);
        assertTrue(learnedPorts(box1, mac(tour, 0)).isEmpty());
    }

    @Test
    void unPosteBrancheEtRetireEntreDeuxTicksNeFaitRienEtNePlantePas() {
        // Plantage du serveur de tests le 2026-11-18 : un test pose, câble et retire un poste dans le même
        // tick ; au tick suivant, le branchement en attente visait un poste qui n'existait plus.
        DeviceId box = box(0);
        sim.tick(0);
        DeviceId tour = host(5);
        plug(box, 0, tour, 0);
        remove(tour);

        sim.tick(1);

        assertEquals(0, presencesSentBy(tour, 0, 1));
        assertTrue(learned(box).isEmpty());
        assertTrue(sim.logic(tour).isEmpty());
    }

    @Test
    void unCableBrancheEtDebrancheEntreDeuxTicksNEmetRien() {
        DeviceId box = box(0);
        DeviceId tour = host(5);
        sim.tick(0);
        plug(box, 1, tour, 0); // le port 0 n'a pas de câble au démarrage
        unplug(box, 1);

        tickUntil(1, 700);

        assertEquals(0, presencesSentBy(tour, 1, 700));
        assertTrue(learned(box).isEmpty());
    }

    @Test
    void unAppareilRemplaceEntreDeuxTicksDemarreAvecSonNouveauRole() {
        DeviceId id = new DeviceId(OVERWORLD, 0, 64, 0);
        sim.tick(0);
        NetworkNode poste = new NetworkNode(id, "worldwideblocks:pc_tower", 1);
        graph.putNode(poste);
        sim.deviceAdded(poste);
        NetworkNode boxAuMemeEndroit = new NetworkNode(id, "worldwideblocks:internet_box", DeviceRole.BOX, BOX_PORTS);
        graph.putNode(boxAuMemeEndroit);
        sim.deviceAdded(boxAuMemeEndroit);

        sim.tick(1);

        assertTrue(sim.logic(id).orElseThrow() instanceof BoxLogic);
    }
}
