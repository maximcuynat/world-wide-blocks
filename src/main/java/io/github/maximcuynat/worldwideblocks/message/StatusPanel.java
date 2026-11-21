package io.github.maximcuynat.worldwideblocks.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;

import io.github.maximcuynat.worldwideblocks.network.PortKind;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClientState;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * Content of the status panel of the targeted device, without any drawing: lines made of an indicator light,
 * a language key and values. The client draws them as it wishes; a future HUD
 * replaces the drawing without touching this content.
 *
 * <ul>
 * <li>Compact (default): a summary, then "Sneak: details". For a device with several
 * local network ports, the number of connected ports ("3/4 ports connected"); for a computer,
 * its address and the state of its DHCP client ("192.168.1.10 (DHCP: bound)", "No address (DHCP:
 * selecting)"), or "Not connected".</li>
 * <li>Detailed (sneaking player): one line per port; for a device that switches, the learned MAC
 * behind each port and the number of others ("(+1)"), then the addresses lent by its DHCP server; for a
 * computer, the MAC of each port, then its address and mask, gateway, name server, the time left on its
 * lease and the state of its DHCP client.</li>
 * </ul>
 *
 * <p>The lease arrives as an expiry time in game time: the remaining time is computed here, with the
 * client's game time, in real minutes and seconds (a game second lasts a real second).
 *
 * <p>Plain Java, no Minecraft class.
 */
public final class StatusPanel {
    /** Leases listed at most in the detailed panel of a box; the others are counted ("+2 more"). */
    public static final int MAX_LEASE_LINES = 4;

    private static final long TICKS_PER_SECOND = 20L;

    /** Indicator light of a line; {@link #NONE} for a detail line, without a light. */
    public enum Light {
        CONNECTED,
        /** Cable connected, but the computer has no address yet. */
        WAITING,
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
     * Address of a computer port, as obtained by DHCP.
     *
     * @param expiresAt end of the lease, in game time
     */
    public record Address(Ipv4Address address, Ipv4Address subnetMask, Ipv4Address router, Ipv4Address dnsServer,
            long expiresAt) {
    }

    /** DHCP client of a computer port: its state, and its address when it has one. */
    public record PortAddress(DhcpClientState state, Optional<Address> address) {
    }

    /** An address lent by the DHCP server of a box, and the MAC it was lent to. */
    public record Lent(Ipv4Address address, MacAddress client) {
    }

    /** DHCP server of a box: whether it is enabled, and the leases in progress, in address order. */
    public record ServerStatus(boolean enabled, List<Lent> leases) {
        public ServerStatus {
            leases = List.copyOf(leases);
        }
    }

    /**
     * What the server said about the targeted device.
     *
     * @param switching true for a device that switches (box, switch)
     * @param portMacs  MAC of each port, in port order
     * @param learned   MACs learned behind each port (number starting at 0), for a device that switches
     * @param dhcp      DHCP client of each computer port (number starting at 0)
     * @param server    DHCP server of the device, for a box
     */
    public record DeviceStatus(boolean switching, List<MacAddress> portMacs, Map<Integer, List<MacAddress>> learned,
            Map<Integer, PortAddress> dhcp, Optional<ServerStatus> server) {
        public DeviceStatus {
            portMacs = List.copyOf(portMacs);
            learned = Map.copyOf(learned);
            dhcp = Map.copyOf(dhcp);
        }

        /** A device without DHCP (or whose DHCP state has not arrived). */
        public DeviceStatus(boolean switching, List<MacAddress> portMacs, Map<Integer, List<MacAddress>> learned) {
            this(switching, portMacs, learned, Map.of(), Optional.empty());
        }
    }

    /**
     * The port the player aims at with a cord in hand, or sneaking with an empty hand (issue #62).
     *
     * @param index    number of the port, starting at 0
     * @param otherEnd the other end of its cable, when it has one and the client knows it
     */
    public record AimedPort(int index, Optional<OtherEnd> otherEnd) {
    }

    /**
     * The other end of the cable of the aimed port.
     *
     * @param deviceKey language key of the name of the device there, empty when its chunk is not loaded on the client
     * @param port      number of its port, starting at 0
     */
    public record OtherEnd(Optional<String> deviceKey, int port) {
    }

    private StatusPanel() {
    }

    /**
     * The lines of the panel, with a first line for the aimed port when there is one: compact "Port 3: free" or
     * "Port 3: connected"; detailed (sneaking), the device and the port at the other end of its cable.
     */
    public static List<Line> lines(List<PortKind> ports, IntPredicate connected, DeviceStatus status, boolean detailed,
            long gameTime, Optional<AimedPort> aimed) {
        List<Line> lines = new ArrayList<>();
        aimed.filter(port -> port.index() >= 0 && port.index() < ports.size())
                .ifPresent(port -> lines.add(aimedLine(ports, connected, port, detailed)));
        lines.addAll(lines(ports, connected, status, detailed, gameTime));
        return lines;
    }

    private static Line aimedLine(List<PortKind> ports, IntPredicate connected, AimedPort port, boolean detailed) {
        MessageArg number = MessageArg.number(port.index() + 1);
        if (ports.get(port.index()) == PortKind.LINE) {
            return Line.of(Light.INACTIVE, "hud.worldwideblocks.port.line", number);
        }
        if (!connected.test(port.index())) {
            return Line.of(Light.FREE, "hud.worldwideblocks.aim.free", number);
        }
        if (detailed && port.otherEnd().isPresent()) {
            OtherEnd end = port.otherEnd().get();
            MessageArg other = MessageArg.number(end.port() + 1);
            return end.deviceKey()
                    .map(key -> Line.of(Light.CONNECTED, "hud.worldwideblocks.aim.connected_to", number, MessageArg.key(key), other))
                    .orElseGet(() -> Line.of(Light.CONNECTED, "hud.worldwideblocks.aim.connected_to_unseen", number, other));
        }
        return Line.of(Light.CONNECTED, "hud.worldwideblocks.aim.connected", number);
    }

    /**
     * @param ports     type of each port of the device
     * @param connected true if a cable is connected to the port (number starting at 0)
     * @param status    state sent by the server, {@code null} if it has not arrived yet
     * @param detailed  true when the player is sneaking
     * @param gameTime  the client's game time, to compute the time left on a lease
     */
    public static List<Line> lines(List<PortKind> ports, IntPredicate connected, DeviceStatus status, boolean detailed,
            long gameTime) {
        return detailed ? detailedLines(ports, connected, status, gameTime) : compactLines(ports, connected, status);
    }

    private static List<Line> compactLines(List<PortKind> ports, IntPredicate connected, DeviceStatus status) {
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
        Optional<PortAddress> dhcp = lan == 1 && lanConnected == 1 && status != null
                ? Optional.ofNullable(status.dhcp().get(firstEthernet(ports))) : Optional.empty();
        if (lan > 1) {
            lines.add(Line.of(light, "hud.worldwideblocks.ports_connected", MessageArg.number(lanConnected), MessageArg.number(lan)));
        } else if (dhcp.isPresent()) {
            lines.add(addressSummary(dhcp.get()));
        } else {
            lines.add(Line.of(light, lanConnected > 0 ? "hud.worldwideblocks.connected" : "hud.worldwideblocks.not_connected"));
        }
        lines.add(Line.of(Light.NONE, "hud.worldwideblocks.sneak_for_details"));
        return lines;
    }

    /** "192.168.1.10 (DHCP: bound)" in green, or "No address (DHCP: selecting)" in waiting colour. */
    private static Line addressSummary(PortAddress dhcp) {
        MessageArg state = state(dhcp.state());
        return dhcp.address()
                .map(address -> Line.of(Light.CONNECTED, "hud.worldwideblocks.dhcp.compact_address", ip(address.address()), state))
                .orElseGet(() -> Line.of(Light.WAITING, "hud.worldwideblocks.dhcp.compact_no_address", state));
    }

    private static List<Line> detailedLines(List<PortKind> ports, IntPredicate connected, DeviceStatus status, long gameTime) {
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
            if (status != null && isConnected && status.dhcp().containsKey(i)) {
                addressDetails(lines, status.dhcp().get(i), gameTime);
            }
        }
        if (status != null) {
            status.server().ifPresent(server -> serverDetails(lines, server));
        }
        return lines;
    }

    /** Address and mask, gateway, name server, time left, then the state of the client. */
    private static void addressDetails(List<Line> lines, PortAddress dhcp, long gameTime) {
        dhcp.address().ifPresent(address -> {
            lines.add(Line.of(Light.NONE, "hud.worldwideblocks.dhcp.address", ip(address.address()), ip(address.subnetMask())));
            lines.add(Line.of(Light.NONE, "hud.worldwideblocks.dhcp.gateway", ip(address.router())));
            lines.add(Line.of(Light.NONE, "hud.worldwideblocks.dhcp.dns", ip(address.dnsServer())));
            lines.add(Line.of(Light.NONE, "hud.worldwideblocks.dhcp.lease", MessageArg.text(remaining(address.expiresAt() - gameTime))));
        });
        lines.add(Line.of(Light.NONE, "hud.worldwideblocks.dhcp.state", state(dhcp.state())));
    }

    /** "DHCP: 3 addresses lent", up to four "address → MAC" lines, then "+2 more"; or "DHCP server: off". */
    private static void serverDetails(List<Line> lines, ServerStatus server) {
        if (!server.enabled()) {
            lines.add(Line.of(Light.NONE, "hud.worldwideblocks.dhcp.server_off"));
            return;
        }
        lines.add(Line.of(Light.NONE, "hud.worldwideblocks.dhcp.leases", MessageArg.number(server.leases().size())));
        for (int i = 0; i < Math.min(server.leases().size(), MAX_LEASE_LINES); i++) {
            Lent lent = server.leases().get(i);
            lines.add(Line.of(Light.NONE, "hud.worldwideblocks.dhcp.lease_line", ip(lent.address()), mac(lent.client())));
        }
        if (server.leases().size() > MAX_LEASE_LINES) {
            lines.add(Line.of(Light.NONE, "hud.worldwideblocks.dhcp.leases_more", MessageArg.number(server.leases().size() - MAX_LEASE_LINES)));
        }
    }

    /**
     * Time left, in real minutes and seconds ("18:42"), with hours beyond one hour ("1:05:00"). The last
     * started second counts: 1 tick left shows "0:01", and an expired lease "0:00".
     */
    static String remaining(long ticks) {
        long seconds = Math.max(0, (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
        long hours = seconds / 3600;
        long minutes = seconds / 60 % 60;
        long rest = seconds % 60;
        return hours > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest)
                : String.format(Locale.ROOT, "%d:%02d", minutes, rest);
    }

    /** Language key of a state of the DHCP client, for example {@code hud.worldwideblocks.dhcp.state.bound}. */
    private static MessageArg state(DhcpClientState state) {
        return MessageArg.key("hud.worldwideblocks.dhcp.state." + state.name().toLowerCase(Locale.ROOT));
    }

    private static int firstEthernet(List<PortKind> ports) {
        return ports.indexOf(PortKind.ETHERNET);
    }

    private static MessageArg ip(Ipv4Address address) {
        return MessageArg.text(address.toString());
    }

    private static MessageArg mac(MacAddress mac) {
        return MessageArg.text(mac.toString());
    }
}
