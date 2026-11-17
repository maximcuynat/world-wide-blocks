package dev.netcraft.network;

import java.util.ArrayList;
import java.util.List;

import dev.netcraft.message.MessageArg;
import dev.netcraft.network.protocol.MacAddress;

/**
 * Descriptions en anglais du réseau pour le log : identifiants de bloc plutôt que noms traduits, même
 * format d'appareil que les messages ({@link MessageArg.Device#logText()}).
 *
 * <p>Java pur, sans classe Minecraft.
 */
public final class NetworkDescriptions {

    private NetworkDescriptions() {
    }

    /** Par exemple {@code netcraft:internet_box [12, 64, -5] minecraft:overworld}. */
    public static String device(NetworkGraph graph, DeviceId id) {
        String type = graph.node(id).map(NetworkNode::deviceType).orElse("?");
        return MessageArg.device(id, type).logText();
    }

    /** Un port d'un appareil, avec sa MAC : {@code … port 2 (MAC 02:…)}. */
    public static String port(NetworkGraph graph, PortRef port) {
        String mac = graph.macOf(port).map(m -> " (MAC " + m + ")").orElse("");
        return device(graph, port.device()) + " port " + port.displayNumber() + mac;
    }

    /** État de chaque port : {@code 1 free (MAC …), 2 connected (MAC …), 5 line inactive}. */
    public static String ports(NetworkGraph graph, NetworkNode node) {
        List<String> parts = new ArrayList<>();
        List<MacAddress> macs = graph.macsOf(node.id());
        for (int i = 0; i < node.portCount(); i++) {
            String state = node.portKind(i) == PortKind.LINE ? "line inactive"
                    : graph.linkAt(node.port(i)).isPresent() ? "connected" : "free";
            String mac = i < macs.size() ? " (MAC " + macs.get(i) + ")" : "";
            parts.add((i + 1) + " " + state + mac);
        }
        return String.join(", ", parts);
    }
}
