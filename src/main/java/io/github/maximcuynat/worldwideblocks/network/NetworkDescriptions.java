package io.github.maximcuynat.worldwideblocks.network;

import java.util.ArrayList;
import java.util.List;

import io.github.maximcuynat.worldwideblocks.message.MessageArg;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * English descriptions of the network for the log: block ids rather than translated names, same
 * device format as the messages ({@link MessageArg.Device#logText()}).
 *
 * <p>Plain Java, no Minecraft class.
 */
public final class NetworkDescriptions {

    private NetworkDescriptions() {
    }

    /** For example {@code worldwideblocks:internet_box [12, 64, -5] minecraft:overworld}. */
    public static String device(NetworkGraph graph, DeviceId id) {
        String type = graph.node(id).map(NetworkNode::deviceType).orElse("?");
        return MessageArg.device(id, type).logText();
    }

    /** A port of a device, with its MAC: {@code … port 2 (MAC 02:…)}. */
    public static String port(NetworkGraph graph, PortRef port) {
        String mac = graph.macOf(port).map(m -> " (MAC " + m + ")").orElse("");
        return device(graph, port.device()) + " port " + port.displayNumber() + mac;
    }

    /** State of each port: {@code 1 free (MAC …), 2 connected (MAC …), 5 line inactive}. */
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
