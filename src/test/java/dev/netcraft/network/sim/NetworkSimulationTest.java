package dev.netcraft.network.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.netcraft.network.DeviceId;
import dev.netcraft.network.DeviceRole;
import dev.netcraft.network.Link;
import dev.netcraft.network.LinkMedium;
import dev.netcraft.network.NetworkGraph;
import dev.netcraft.network.NetworkNode;
import dev.netcraft.network.PortKind;
import dev.netcraft.network.PortRef;
import dev.netcraft.network.protocol.EtherType;
import dev.netcraft.network.protocol.EthernetFrame;
import dev.netcraft.network.protocol.MacAddress;
import dev.netcraft.network.switching.MacTable;

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
        NetworkNode node = new NetworkNode(new DeviceId(OVERWORLD, x, 64, 0), "netcraft:pc_tower", 1);
        this.graph.putNode(node);
        this.sim.deviceAdded(node);
        return node.id();
    }

    private DeviceId box(int x) {
        NetworkNode node = new NetworkNode(new DeviceId(OVERWORLD, x, 64, 0), "netcraft:internet_box", DeviceRole.BOX, BOX_PORTS);
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
        graph.putNode(new NetworkNode(box, "netcraft:internet_box", DeviceRole.BOX, BOX_PORTS));
        graph.putNode(new NetworkNode(tour, "netcraft:pc_tower", 1));
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
        List<SimulationEvent> events = sim.drainEvents();
        assertTrue(events.contains(new SimulationEvent.LoopDetected(box1, 2)), events.toString());
        assertTrue(events.contains(new SimulationEvent.LoopDetected(box2, 2)), events.toString());
        // La MAC du poste n'a pas basculé de port à cause des copies supprimées.
        assertEquals(List.of(0), learnedPorts(box1, mac(tour, 0)));
    }

    @Test
    void laBoucleEstSignaleeAuPlusUneFoisParMinuteEtParAppareil() {
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        DeviceId tour = host(5);
        plug(box1, 1, box2, 1);
        plug(box1, 2, box2, 2);
        plug(box1, 0, tour, 0);
        sim.tick(0);
        sim.drainEvents();

        sim.tick(600); // nouvelle présence, même boucle, moins d'une minute après
        assertTrue(sim.drainEvents().isEmpty());

        sim.tick(1200); // une minute après le premier signalement
        assertTrue(sim.drainEvents().contains(new SimulationEvent.LoopDetected(box1, 2)));
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

        assertTrue(sim.drainEvents().contains(new SimulationEvent.QueueSaturated()));
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
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        plug(box1, 1, box2, 1);
        plug(box1, 2, box2, 2);
        sim.tick(50);
        DeviceId tour = host(5);
        plug(box1, 0, tour, 0); // entre le tick 50 et le tick 51

        List<Long> signalements = new ArrayList<>();
        for (long t = 51; t <= 2500; t++) {
            sim.tick(t);
            if (sim.drainEvents().contains(new SimulationEvent.LoopDetected(box1, 2))) {
                signalements.add(t);
            }
        }

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
        NetworkNode poste = new NetworkNode(id, "netcraft:pc_tower", 1);
        graph.putNode(poste);
        sim.deviceAdded(poste);
        NetworkNode boxAuMemeEndroit = new NetworkNode(id, "netcraft:internet_box", DeviceRole.BOX, BOX_PORTS);
        graph.putNode(boxAuMemeEndroit);
        sim.deviceAdded(boxAuMemeEndroit);

        sim.tick(1);

        assertTrue(sim.logic(id).orElseThrow() instanceof BoxLogic);
    }
}
