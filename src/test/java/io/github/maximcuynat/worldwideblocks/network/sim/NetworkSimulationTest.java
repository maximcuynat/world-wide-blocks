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

    /** What the tap has seen go by. */
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

    // ---- Network setup, the way NetworkManager does it ----

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

    // ---- Reading ----

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

    // ---- Scenarios ----

    @Test
    void theBoxLearnsWhichMacIsBehindWhichPort() {
        DeviceId box = box(0);
        DeviceId tower = host(5);
        DeviceId laptop = host(10);
        plug(box, 0, tower, 0);
        plug(box, 1, laptop, 0);

        sim.tick(0);

        assertEquals(List.of(0), learnedPorts(box, mac(tower, 0)));
        assertEquals(List.of(1), learnedPorts(box, mac(laptop, 0)));
        assertEquals(2, learned(box).size());
        assertEquals(0, sim.pendingFrames());
    }

    @Test
    void thePresenceIsAnExperimentalTypeBroadcastRelayedToTheOtherComputers() {
        DeviceId box = box(0);
        DeviceId tower = host(5);
        DeviceId laptop = host(10);
        plug(box, 0, tower, 0);
        plug(box, 1, laptop, 0);

        sim.tick(0);

        Seen received = seen.stream()
                .filter(s -> s.direction() == FrameDirection.RECEIVED && s.port().device().equals(laptop))
                .filter(s -> s.frame().source().equals(mac(tower, 0)))
                .findFirst().orElseThrow();
        assertTrue(received.frame().isBroadcast());
        assertEquals(EtherType.LOCAL_EXPERIMENTAL, received.frame().type());
        // The box received it on port 0 and sent it back on ports 1, 2 and 3, not on the line input.
        List<Integer> boxOutputs = seen.stream()
                .filter(s -> s.direction() == FrameDirection.SENT && s.port().device().equals(box))
                .filter(s -> s.frame().source().equals(mac(tower, 0)))
                .map(s -> s.port().index()).toList();
        assertEquals(List.of(1, 2, 3), boxOutputs);
    }

    @Test
    void aComputerRebroadcastsItsPresenceEvery30Seconds() {
        DeviceId box = box(0);
        DeviceId tower = host(5);
        plug(box, 0, tower, 0);

        tickUntil(0, 1300);

        assertEquals(3, presencesSentBy(tower, 0, 1300)); // ticks 0, 600 and 1200
    }

    @Test
    void removingTheCableForgetsTheMacAtTheNextTickAndStopsThePresence() {
        DeviceId box = box(0);
        DeviceId tower = host(5);
        DeviceId laptop = host(10);
        plug(box, 0, tower, 0);
        plug(box, 1, laptop, 0);
        sim.tick(0);

        unplug(box, 0);
        sim.tick(1);

        assertTrue(learnedPorts(box, mac(tower, 0)).isEmpty());
        assertEquals(List.of(1), learnedPorts(box, mac(laptop, 0)));
        tickUntil(2, 700);
        assertEquals(0, presencesSentBy(tower, 1, 700));
    }

    @Test
    void anEntryLearnedFarAwayExpiresAfter300Seconds() {
        // box1 port 0 <-> box2 port 0, tower on box2 port 1.
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        DeviceId tower = host(25);
        plug(box1, 0, box2, 0);
        plug(box2, 1, tower, 0);
        sim.tick(0);
        assertEquals(List.of(0), learnedPorts(box1, mac(tower, 0)));

        unplug(box2, 1);
        sim.tick(1);

        // box2 forgets it at the next tick, box1 (whose cable is still there) only after the delay.
        assertTrue(learnedPorts(box2, mac(tower, 0)).isEmpty());
        sim.tick(MacTable.DEFAULT_AGING_TICKS - 10);
        assertEquals(List.of(0), learnedPorts(box1, mac(tower, 0)));
        sim.tick(MacTable.DEFAULT_AGING_TICKS + 10);
        assertTrue(learnedPorts(box1, mac(tower, 0)).isEmpty());
    }

    @Test
    void aFrameInTransitOnARemovedCableIsNotDelivered() {
        // Only one delivery per tick: the laptop's presence stays in the queue after tick 0.
        useLimits(new SimulationLimits(1, 50_000, 1200));
        DeviceId box = box(0);
        DeviceId tower = host(5);
        DeviceId laptop = host(10);
        plug(box, 0, tower, 0);
        plug(box, 1, laptop, 0);
        sim.tick(0);
        // Still queued: the laptop's presence and the tower's presence relayed to it.
        assertTrue(sim.pendingFrames() > 0);

        unplug(box, 1);
        tickUntil(1, 2);

        assertEquals(List.of(0), learnedPorts(box, mac(tower, 0)));
        assertTrue(learnedPorts(box, mac(laptop, 0)).isEmpty());
        assertEquals(0, sim.pendingFrames());
    }

    @Test
    void aRemovedDeviceSendsNothingMore() {
        DeviceId box = box(0);
        DeviceId tower = host(5);
        plug(box, 0, tower, 0);
        sim.tick(0);

        remove(tower);
        tickUntil(1, 700);

        assertEquals(0, presencesSentBy(tower, 1, 700));
        assertTrue(learned(box).isEmpty());
        assertTrue(sim.logic(tower).isEmpty());
    }

    @Test
    void atStartupEveryExistingCableIsAnnounced() {
        // Graph read back from the save: the cables exist before the simulation.
        DeviceId box = new DeviceId(OVERWORLD, 0, 64, 0);
        DeviceId tower = new DeviceId(OVERWORLD, 5, 64, 0);
        graph.putNode(new NetworkNode(box, "worldwideblocks:internet_box", DeviceRole.BOX, BOX_PORTS));
        graph.putNode(new NetworkNode(tower, "worldwideblocks:pc_tower", 1));
        graph.connect(new PortRef(box, 2), new PortRef(tower, 0), LinkMedium.COPPER_ETHERNET, true);

        sim.tick(100);

        assertEquals(List.of(2), learnedPorts(box, mac(tower, 0)));
    }

    @Test
    void twoBoxesInALoopDoNotSpinForeverAndTheLoopIsReported() {
        // Two cables between the two boxes: a broadcast frame would come back endlessly.
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        DeviceId tower = host(5);
        plug(box1, 1, box2, 1);
        plug(box1, 2, box2, 2);
        plug(box1, 0, tower, 0);

        sim.tick(0);

        assertEquals(0, sim.pendingFrames());
        long receptions = seen.stream().filter(s -> s.direction() == FrameDirection.RECEIVED).count();
        assertTrue(receptions < 10, "receptions: " + receptions);
        // Both boxes see the loop: a single event, which names both of them.
        List<SimulationEvent> events = sim.drainEvents();
        assertEquals(1, events.size(), events.toString());
        SimulationEvent.LoopDetected loop = (SimulationEvent.LoopDetected) events.getFirst();
        assertEquals(box1, loop.network());
        assertTrue(loop.points().contains(new SimulationEvent.LoopPoint(box1, 2)), loop.toString());
        assertTrue(loop.points().contains(new SimulationEvent.LoopPoint(box2, 2)), loop.toString());
        // The computer's MAC did not flip ports because of the dropped copies.
        assertEquals(List.of(0), learnedPorts(box1, mac(tower, 0)));
    }

    @Test
    void theSimulationReportsTheLoopAtEveryTickWhereItSeesItWithoutRateLimit() {
        // The once-per-minute limit belongs to the message module, not to the engine.
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        DeviceId tower = host(5);
        plug(box1, 1, box2, 1);
        plug(box1, 2, box2, 2);
        plug(box1, 0, tower, 0);
        sim.tick(0);
        sim.drainEvents();

        sim.tick(599);
        assertTrue(sim.drainEvents().isEmpty(), "no frame between two presences");

        sim.tick(600); // new presence, same loop
        List<SimulationEvent> events = sim.drainEvents();
        assertEquals(1, events.size(), events.toString());
        assertEquals(box1, ((SimulationEvent.LoopDetected) events.getFirst()).network());
    }

    @Test
    void twoLoopsInTwoNetworksGiveTwoEvents() {
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
    void beyondThePerTickLimitFramesWaitForTheNextTick() {
        useLimits(new SimulationLimits(2, 50_000, 1200));
        DeviceId box = box(0);
        DeviceId tower = host(5);
        DeviceId laptop = host(10);
        plug(box, 0, tower, 0);
        plug(box, 1, laptop, 0);

        sim.tick(0);
        assertTrue(sim.pendingFrames() > 0);

        sim.tick(1);
        sim.tick(2);
        assertEquals(0, sim.pendingFrames());
        assertEquals(2, learned(box).size());
    }

    @Test
    void aFullQueueDropsNewFramesAndReportsIt() {
        useLimits(new SimulationLimits(10_000, 1, 1200));
        DeviceId box = box(0);
        plug(box, 0, host(5), 0);
        plug(box, 1, host(10), 0);

        sim.tick(0);

        // At most one event per tick, even if many frames are lost.
        assertEquals(List.of(new SimulationEvent.QueueSaturated()), sim.drainEvents());
    }

    // ---- Clock: changes that happen between two ticks ----

    @Test
    void aConnectionBetweenTwoTicksStartsAtTheNextTick() {
        DeviceId box = box(0);
        sim.tick(100);
        DeviceId tower = host(5);
        plug(box, 0, tower, 0); // between tick 100 and tick 101, like a player click

        sim.tick(101);
        // Learned at the very tick of sending: the frame is delivered in the tick where it leaves.
        assertEquals(101L, learned(box).getFirst().lastSeen());
        tickUntil(102, 800);

        List<Long> sends = seen.stream()
                .filter(s -> s.direction() == FrameDirection.SENT && s.port().device().equals(tower))
                .map(Seen::now).toList();
        assertEquals(List.of(101L, 701L), sends);
    }

    @Test
    void aLoopBornBetweenTwoTicksIsReportedEveryMinuteExactly() {
        // Defect found on 2026-11-18 by the game test: reports 1,800 ticks apart instead of 1,200.
        // The engine sees the loop at every presence (every 600 ticks); the message module,
        // with the same time, forwards only one per minute.
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        plug(box1, 1, box2, 1);
        plug(box1, 2, box2, 2);
        sim.tick(50);
        DeviceId tower = host(5);
        plug(box1, 0, tower, 0); // between tick 50 and tick 51

        MessageThrottle throttle = new MessageThrottle(SimulationLimits.DEFAULT.reportIntervalTicks());
        List<Long> sightings = new ArrayList<>();
        List<Long> reports = new ArrayList<>();
        for (long t = 51; t <= 2500; t++) {
            sim.tick(t);
            for (SimulationEvent event : sim.drainEvents()) {
                if (event instanceof SimulationEvent.LoopDetected loop) {
                    sightings.add(t);
                    if (throttle.allow("loop:" + loop.network(), t)) {
                        reports.add(t);
                    }
                }
            }
        }

        assertEquals(List.of(51L, 651L, 1251L, 1851L, 2451L), sightings);
        assertEquals(List.of(51L, 1251L, 2451L), reports);
    }

    @Test
    void aMacExpiresExactly300SecondsAfterItsLastFrame() {
        DeviceId box1 = box(0);
        DeviceId box2 = box(20);
        plug(box1, 0, box2, 0);
        sim.tick(0);
        DeviceId tower = host(25);
        plug(box2, 1, tower, 0); // between tick 0 and tick 1: presence delivered at tick 1
        sim.tick(1);
        unplug(box2, 1);
        assertEquals(1L, learned(box1).getFirst().lastSeen());

        long expiration = 1 + MacTable.DEFAULT_AGING_TICKS;
        tickUntil(2, expiration - 1);
        assertEquals(List.of(0), learnedPorts(box1, mac(tower, 0)));
        // Expired at 6001; removed from the table at the next cleanup, one second later at most.
        tickUntil(expiration, expiration + BoxLogic.PURGE_INTERVAL_TICKS - 1);
        assertTrue(learnedPorts(box1, mac(tower, 0)).isEmpty());
    }

    @Test
    void aComputerConnectedAndRemovedBetweenTwoTicksDoesNothingAndDoesNotCrash() {
        // Test server crash on 2026-11-18: a test places, cables and removes a computer in the same
        // tick; at the next tick, the pending connection targeted a computer that no longer existed.
        DeviceId box = box(0);
        sim.tick(0);
        DeviceId tower = host(5);
        plug(box, 0, tower, 0);
        remove(tower);

        sim.tick(1);

        assertEquals(0, presencesSentBy(tower, 0, 1));
        assertTrue(learned(box).isEmpty());
        assertTrue(sim.logic(tower).isEmpty());
    }

    @Test
    void aCableConnectedAndUnpluggedBetweenTwoTicksSendsNothing() {
        DeviceId box = box(0);
        DeviceId tower = host(5);
        sim.tick(0);
        plug(box, 1, tower, 0); // port 0 has no cable at startup
        unplug(box, 1);

        tickUntil(1, 700);

        assertEquals(0, presencesSentBy(tower, 1, 700));
        assertTrue(learned(box).isEmpty());
    }

    @Test
    void aDeviceReplacedBetweenTwoTicksStartsWithItsNewRole() {
        DeviceId id = new DeviceId(OVERWORLD, 0, 64, 0);
        sim.tick(0);
        NetworkNode computer = new NetworkNode(id, "worldwideblocks:pc_tower", 1);
        graph.putNode(computer);
        sim.deviceAdded(computer);
        NetworkNode boxAtTheSameSpot = new NetworkNode(id, "worldwideblocks:internet_box", DeviceRole.BOX, BOX_PORTS);
        graph.putNode(boxAtTheSameSpot);
        sim.deviceAdded(boxAtTheSameSpot);

        sim.tick(1);

        assertTrue(sim.logic(id).orElseThrow() instanceof BoxLogic);
    }
}
