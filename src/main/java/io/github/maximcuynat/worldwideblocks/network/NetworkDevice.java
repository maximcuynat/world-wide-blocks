package io.github.maximcuynat.worldwideblocks.network;

import java.util.List;

/**
 * Block that is a network device. Its ports (count and kind) are fixed for a device type:
 * they depend neither on the block state nor on its configuration.
 */
public interface NetworkDevice {

    /** Behavior of the device in the simulation. */
    DeviceRole role();

    /** Kind of each port, in port number order. */
    List<PortKind> ports();

    default int portCount() {
        return this.ports().size();
    }
}
