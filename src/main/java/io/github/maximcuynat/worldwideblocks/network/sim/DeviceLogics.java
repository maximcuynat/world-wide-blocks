package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.ArrayList;
import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.NetworkNode;
import io.github.maximcuynat.worldwideblocks.network.PortKind;

/** Creates the behavior of a device from its role. */
final class DeviceLogics {

    private DeviceLogics() {
    }

    static DeviceLogic create(NetworkNode node) {
        return switch (node.role()) {
            case HOST -> new HostLogic(node.portCount());
            case BOX -> new BoxLogic(portsOfKind(node, PortKind.ETHERNET));
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
