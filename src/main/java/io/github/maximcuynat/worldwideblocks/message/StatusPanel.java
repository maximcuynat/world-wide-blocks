package io.github.maximcuynat.worldwideblocks.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

import io.github.maximcuynat.worldwideblocks.network.PortKind;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * Content of the status panel of the targeted device, without any drawing: lines made of an indicator light,
 * a language key and values. The client draws them as it wishes; a future HUD
 * replaces the drawing without touching this content.
 *
 * <ul>
 * <li>Compact (default): a summary, then "Sneak: details". For a device with several
 * local network ports, the number of connected ports ("3/4 ports connected"); for a computer,
 * "Connected" or "Not connected".</li>
 * <li>Detailed (sneaking player): one line per port; for a device that switches, the learned MAC
 * behind each port and the number of others ("(+1)"); for a computer, the MAC of each port.</li>
 * </ul>
 *
 * <p>Plain Java, no Minecraft class.
 */
public final class StatusPanel {

    /** Indicator light of a line; {@link #NONE} for a detail line, without a light. */
    public enum Light {
        CONNECTED,
        FREE,
        INACTIVE,
        NONE
    }

    public record Line(Light light, String key, List<MessageArg> args) {
        public Line {
            args = List.copyOf(args);
        }

        static Line of(Light light, String key, MessageArg... args) {
            return new Line(light, key, List.of(args));
        }
    }

    /**
     * What the server said about the targeted device.
     *
     * @param switching true for a device that switches (box, switch)
     * @param portMacs  MAC of each port, in port order
     * @param learned   MACs learned behind each port (number starting at 0), for a device that switches
     */
    public record DeviceStatus(boolean switching, List<MacAddress> portMacs, Map<Integer, List<MacAddress>> learned) {
        public DeviceStatus {
            portMacs = List.copyOf(portMacs);
            learned = Map.copyOf(learned);
        }
    }

    private StatusPanel() {
    }

    /**
     * @param ports     type of each port of the device
     * @param connected true if a cable is connected to the port (number starting at 0)
     * @param status    state sent by the server, {@code null} if it has not arrived yet
     * @param detailed  true when the player is sneaking
     */
    public static List<Line> lines(List<PortKind> ports, IntPredicate connected, DeviceStatus status, boolean detailed) {
        return detailed ? detailedLines(ports, connected, status) : compactLines(ports, connected);
    }

    private static List<Line> compactLines(List<PortKind> ports, IntPredicate connected) {
        int lan = 0;
        int lanConnected = 0;
        for (int i = 0; i < ports.size(); i++) {
            if (ports.get(i) == PortKind.ETHERNET) {
                lan++;
                if (connected.test(i)) {
                    lanConnected++;
                }
            }
        }
        List<Line> lines = new ArrayList<>();
        Light light = lanConnected > 0 ? Light.CONNECTED : Light.FREE;
        if (lan > 1) {
            lines.add(Line.of(light, "hud.worldwideblocks.ports_connected", MessageArg.number(lanConnected), MessageArg.number(lan)));
        } else {
            lines.add(Line.of(light, lanConnected > 0 ? "hud.worldwideblocks.connected" : "hud.worldwideblocks.not_connected"));
        }
        lines.add(Line.of(Light.NONE, "hud.worldwideblocks.sneak_for_details"));
        return lines;
    }

    private static List<Line> detailedLines(List<PortKind> ports, IntPredicate connected, DeviceStatus status) {
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < ports.size(); i++) {
            MessageArg number = MessageArg.number(i + 1);
            if (ports.get(i) == PortKind.LINE) {
                lines.add(Line.of(Light.INACTIVE, "hud.worldwideblocks.port.line", number));
                continue;
            }
            boolean isConnected = connected.test(i);
            Light light = isConnected ? Light.CONNECTED : Light.FREE;
            List<MacAddress> learned = status != null && status.switching() ? status.learned().getOrDefault(i, List.of()) : List.of();
            if (isConnected && learned.size() == 1) {
                lines.add(Line.of(light, "hud.worldwideblocks.port.connected_learned", number, mac(learned.get(0))));
            } else if (isConnected && learned.size() > 1) {
                lines.add(Line.of(light, "hud.worldwideblocks.port.connected_learned_more", number, mac(learned.get(0)),
                        MessageArg.number(learned.size() - 1)));
            } else {
                lines.add(Line.of(light, isConnected ? "hud.worldwideblocks.port.connected" : "hud.worldwideblocks.port.free", number));
            }
            if (status != null && !status.switching() && i < status.portMacs().size()) {
                lines.add(Line.of(Light.NONE, "hud.worldwideblocks.mac", mac(status.portMacs().get(i))));
            }
        }
        return lines;
    }

    private static MessageArg mac(MacAddress mac) {
        return MessageArg.text(mac.toString());
    }
}
