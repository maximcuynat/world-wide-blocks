package dev.netcraft.network.sim;

import java.util.ArrayList;
import java.util.List;

import dev.netcraft.network.NetworkNode;
import dev.netcraft.network.PortKind;

/** Crée le comportement d'un appareil d'après son rôle. */
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
