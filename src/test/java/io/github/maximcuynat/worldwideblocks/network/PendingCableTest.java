package io.github.maximcuynat.worldwideblocks.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Bug found in game on 2026-11-17: connect a box A to a device B, remove the end on B's side
 * (the cord stays attached to A, pending), change slot in the hotbar, then break A. The cord stayed
 * pending although its start no longer existed.
 */
class PendingCableTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    private NetworkGraph graph;
    private PortRef start;
    private PortRef other;

    @BeforeEach
    void setUp() {
        this.graph = new NetworkGraph(new SplittableRandom(3));
        DeviceId box = new DeviceId(OVERWORLD, 0, 64, 0);
        DeviceId tower = new DeviceId(OVERWORLD, 5, 64, 0);
        this.graph.putNode(new NetworkNode(box, "worldwideblocks:internet_box", DeviceRole.BOX,
                List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE)));
        this.graph.putNode(new NetworkNode(tower, "worldwideblocks:pc_tower", 1));
        this.start = new PortRef(box, 0);
        this.other = new PortRef(tower, 0);
    }

    /** Steps 1 and 2 of the bug: A connected to B, then the end on B's side removed; the cord waits, attached to A. */
    private void connectThenRemoveTheEndOnSideB() {
        this.graph.connect(this.start, this.other, LinkMedium.COPPER_ETHERNET, true);
        this.graph.disconnect(this.other);
    }

    @Test
    void thePendingCableStaysValidWhileTheStartExists() {
        connectThenRemoveTheEndOnSideB();

        assertEquals(PendingCable.Status.VALID, PendingCable.check(graph, start, OVERWORLD));
    }

    @Test
    void breakingTheStartCancelsThePendingCable() {
        connectThenRemoveTheEndOnSideB();

        // Break or explosion: same path, the device leaves the engine.
        graph.removeDevice(start.device());

        assertEquals(PendingCable.Status.START_GONE, PendingCable.check(graph, start, OVERWORLD));
    }

    @Test
    void aStartReplacedByADeviceWithoutThisPortCancelsThePendingCable() {
        PortRef port4 = new PortRef(start.device(), 3);

        graph.putNode(new NetworkNode(start.device(), "worldwideblocks:pc_tower", 1));

        assertEquals(PendingCable.Status.START_GONE, PendingCable.check(graph, port4, OVERWORLD));
    }

    @Test
    void changingDimensionCancelsThePendingCable() {
        connectThenRemoveTheEndOnSideB();

        assertEquals(PendingCable.Status.OTHER_DIMENSION, PendingCable.check(graph, start, NETHER));
    }

    @Test
    void aMissingStartTakesPrecedenceOverTheDimensionChange() {
        connectThenRemoveTheEndOnSideB();
        graph.removeDevice(start.device());

        assertEquals(PendingCable.Status.START_GONE, PendingCable.check(graph, start, NETHER));
    }
}
