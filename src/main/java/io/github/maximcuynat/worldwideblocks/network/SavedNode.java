package io.github.maximcuynat.worldwideblocks.network;

import java.util.List;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * A device as written in the save: its description and the MAC addresses of its ports.
 * Plain Java: rereading old formats can be tested without launching the game.
 *
 * @param macs empty for a world saved before lot 1b; the graph then generates new ones
 */
record SavedNode(NetworkNode node, List<MacAddress> macs) {

    SavedNode {
        macs = List.copyOf(macs);
    }

    /**
     * Rereads a device. Before lot 1b, only the number of ports was written: all devices
     * were computers and all their ports Ethernet jacks. Since then, the role and the kind of
     * each port are written and are authoritative.
     */
    static SavedNode decode(DeviceId id, String type, Optional<DeviceRole> role, int portCount,
            Optional<List<PortKind>> portKinds, List<MacAddress> macs) {
        return new SavedNode(new NetworkNode(id, type, role.orElse(DeviceRole.HOST),
                portKinds.orElseGet(() -> PortKind.ethernet(portCount))), macs);
    }
}
