package io.github.maximcuynat.worldwideblocks.network;

import java.util.List;
import java.util.Objects;

/**
 * Device known to the network engine. It is registered when the block is placed and exists even when
 * its chunk is not loaded: the engine thus knows which ports it has and how it behaves
 * without loading the world.
 *
 * <p>This record describes what the block declares (type, role and ports). It is rebuilt from the block
 * then compared with the engine's: it therefore does not carry the MAC addresses, which the graph assigns and
 * keeps separately.
 *
 * @param deviceType block identifier, for example {@code worldwideblocks:pc_tower}
 * @param role       behavior of the device in the simulation
 * @param ports      kind of each port, in port number order
 */
public record NetworkNode(DeviceId id, String deviceType, DeviceRole role, List<PortKind> ports) {

    public NetworkNode {
        Objects.requireNonNull(role, "role");
        ports = List.copyOf(ports);
    }

    /** Computer whose ports are all Ethernet jacks. */
    public NetworkNode(DeviceId id, String deviceType, int ethernetPorts) {
        this(id, deviceType, DeviceRole.HOST, PortKind.ethernet(ethernetPorts));
    }

    public int portCount() {
        return this.ports.size();
    }

    public boolean hasPort(int index) {
        return index >= 0 && index < this.ports.size();
    }

    public PortKind portKind(int index) {
        return this.ports.get(index);
    }

    public PortRef port(int index) {
        return new PortRef(this.id, index);
    }
}
