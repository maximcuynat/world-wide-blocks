package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;

/**
 * What the simulation reports to the game, which turns it into a message. The simulation observes; the
 * message module decides whom to notify and how often (once per minute for the same problem).
 */
public sealed interface SimulationEvent {

    /** Port through which a copy of a broadcast frame came back to a switch it had already crossed. */
    record LoopPoint(DeviceId device, int port) {
    }

    /**
     * A loop in a network: a frame came back to a switch it had already crossed, so there
     * are two paths between them. The copies were dropped. A single event per network and per
     * tick, even if several switches saw it.
     *
     * @param network network key ({@code NetworkGraph#networkKey}), stable as long as the network does not change
     * @param points  switches and ports where the copies came back, in the order they were detected
     */
    record LoopDetected(DeviceId network, List<LoopPoint> points) implements SimulationEvent {
        public LoopDetected {
            points = List.copyOf(points);
        }
    }

    /** The queue was full: frames were lost. At most one per tick. */
    record QueueSaturated() implements SimulationEvent {
    }
}
