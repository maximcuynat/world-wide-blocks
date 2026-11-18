package io.github.maximcuynat.worldwideblocks.network.sim;

/** Direction of a frame seen by the tap, from the device's point of view. */
public enum FrameDirection {
    /** The frame arrives on a port of the device. */
    RECEIVED,
    /** The frame leaves a port of the device (whether or not a cable is connected to it). */
    SENT
}
