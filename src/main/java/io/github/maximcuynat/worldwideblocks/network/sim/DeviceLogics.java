package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import io.github.maximcuynat.worldwideblocks.network.NetworkNode;
import io.github.maximcuynat.worldwideblocks.network.PortKind;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServer;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServerConfig;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpStore;

/** Creates the behavior of a device from its role, with what it remembers of DHCP. */
final class DeviceLogics {

    private DeviceLogics() {
    }

    static DeviceLogic create(NetworkNode node, DhcpStore dhcp) {
        return switch (node.role()) {
            // The xid of the DHCP transactions starts at random (RFC 2131, 4.1).
            case HOST -> new HostLogic(node.portCount(), RandomGenerator.getDefault());
            case BOX -> new BoxLogic(portsOfKind(node, PortKind.ETHERNET),
                    new DhcpServer(DhcpServerConfig.INTERNET_BOX, dhcp.server(node.id())));
        };
    }

    private static List<Integer> portsOfKind(NetworkNode node, PortKind kind) {
        List<Integer> ports = new ArrayList<>();
        for (int i = 0; i < node.portCount(); i++) {
            if (node.portKind(i) == kind) {
                ports.add(i);
            }
        }
        return ports;
    }
}
