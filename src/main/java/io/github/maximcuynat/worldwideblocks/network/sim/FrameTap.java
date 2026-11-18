package io.github.maximcuynat.worldwideblocks.network.sim;

import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;

/**
 * Tap: every frame that enters or leaves a device goes through it. It does nothing for
 * now; it will serve the in-game frame analyzer, and the tests use it to observe the
 * exchanges.
 *
 * <p>It must not modify the network: it only observes.
 */
@FunctionalInterface
public interface FrameTap {
    FrameTap NONE = (now, port, direction, frame) -> {
    };

    /**
     * @param now       game time, in ticks
     * @param port      device port the frame goes through
     */
    void onFrame(long now, PortRef port, FrameDirection direction, EthernetFrame frame);
}
