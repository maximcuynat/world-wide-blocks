package io.github.maximcuynat.worldwideblocks.network;

/**
 * Designates a specific port of a device.
 *
 * @param index port number, from 0 (shown from 1 to the player)
 */
public record PortRef(DeviceId device, int index) {

    /** Port number as shown to the player. */
    public int displayNumber() {
        return this.index + 1;
    }
}
