package io.github.maximcuynat.worldwideblocks.network;

/**
 * Rule of a pending connection: a cord held in hand with one end already plugged into a
 * device (first click done, or end removed from a device). Plain Java, testable without the game.
 */
public final class PendingCable {

    /** State of a pending connection. */
    public enum Status {
        /** The connection can still be completed. */
        VALID,
        /** The start device no longer exists (broken, exploded, replaced): the wait is cancelled. */
        START_GONE,
        /** Whoever holds the cord is no longer in the start dimension: the wait is cancelled. */
        OTHER_DIMENSION
    }

    private PendingCable() {
    }

    /**
     * @param start           start port remembered on the cord
     * @param holderDimension dimension where the holder of the cord is
     */
    public static Status check(NetworkGraph graph, PortRef start, String holderDimension) {
        // A vanished start takes precedence: going back to the right dimension would change nothing.
        if (graph.node(start.device()).filter(node -> node.hasPort(start.index())).isEmpty()) {
            return Status.START_GONE;
        }
        if (!start.device().dimension().equals(holderDimension)) {
            return Status.OTHER_DIMENSION;
        }
        return Status.VALID;
    }
}
