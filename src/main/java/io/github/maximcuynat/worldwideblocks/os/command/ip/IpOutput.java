package io.github.maximcuynat.worldwideblocks.os.command.ip;

import java.util.ArrayList;
import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.ip.InterfaceAddress;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Prefix;
import io.github.maximcuynat.worldwideblocks.network.ip.NeighbourTable;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.ip.Route;
import io.github.maximcuynat.worldwideblocks.network.ip.RouteProtocol;
import io.github.maximcuynat.worldwideblocks.network.ip.Scope;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * Lines of {@code ip address}, {@code ip link} and {@code ip route}, in the format of iproute2 6.15.0, trailing
 * spaces included. Reference: the real outputs of {@code src/test/resources/linux-real-outputs.txt} (Debian in
 * Docker); what they do not show (a cable unplugged, an address from DHCP) is read in the sources:
 *
 * <ul>
 * <li>{@code print_link_flags} ({@code ip/ipaddress.c}): {@code NO-CARRIER} first when the interface is up without
 * carrier, then the flags in a fixed order, {@code LOWER_UP} only with a carrier.</li>
 * <li>{@code print_addrinfo}: {@code inet}, address and prefix, {@code brd}, {@code scope}, {@code dynamic} for an
 * address that is not permanent, then the label; then {@code valid_lft} and {@code preferred_lft}, in seconds or
 * {@code forever}. The kernel gives the time left as the lifetime minus the whole seconds elapsed
 * ({@code net/ipv4/devinet.c}): the seconds left, rounded up.</li>
 * <li>{@code print_neigh} ({@code ip/ipneigh.c}): address, {@code dev} unless the listing is filtered on a device,
 * {@code lladdr} when the hardware address is known, then the state; each field followed by a space.</li>
 * <li>{@code print_route} ({@code ip/iproute.c}): destination ({@code default} for 0.0.0.0/0), {@code via},
 * {@code dev}, {@code proto} unless {@code boot}, {@code scope} unless global, {@code src}; each field followed by a
 * space.</li>
 * </ul>
 *
 * <p>Queueing discipline of an Ethernet interface: {@code fq_codel}, the default systemd gives to the kernel
 * ({@code sysctl.d/50-default.conf}, {@code net.core.default_qdisc = fq_codel}), with the default queue length of
 * Ethernet devices, 1000 ({@code DEFAULT_TX_QUEUE_LEN}, {@code net/ethernet/eth.c}). To check on a real Debian
 * machine: the reference output comes from a Docker interface ({@code noqueue}, without queue length), and a card with
 * several queues shows {@code mq}.
 */
public final class IpOutput {
    static final String ETHERNET_QDISC = "fq_codel";
    /** Queue length of every interface: 1000, also for {@code lo} in the reference output. */
    static final int QUEUE_LENGTH = 1000;
    /** Ticks per second of game time. */
    private static final long TICKS_PER_SECOND = 20L;

    private IpOutput() {
    }

    /** {@code ip address} for one interface: its link lines, then each address with its lifetimes. */
    public static List<String> address(NetInterface link, long now) {
        List<String> lines = new ArrayList<>();
        lines.add(header(link, false));
        lines.add(linkLayer(link));
        for (InterfaceAddress address : link.addresses()) {
            StringBuilder line = new StringBuilder("    inet ").append(address.address()).append('/')
                    .append(address.prefixLength()).append(' ');
            address.broadcast().ifPresent(broadcast -> line.append("brd ").append(broadcast).append(' '));
            line.append("scope ").append(address.scope().text()).append(' ');
            if (!address.permanent()) {
                line.append("dynamic ");
            }
            line.append(link.name());
            lines.add(line.toString());
            String lifetime = lifetime(address, now);
            lines.add("       valid_lft " + lifetime + " preferred_lft " + lifetime);
        }
        return lines;
    }

    /** {@code ip link} for one interface. */
    public static List<String> link(NetInterface link) {
        return List.of(header(link, true), linkLayer(link));
    }

    /** {@code ip route}: one line per route, in the order given. */
    public static List<String> routes(List<Route> routes) {
        return routes.stream().map(IpOutput::route).toList();
    }

    /** {@code ip neigh}: one entry of the ARP cache. */
    public static String neighbour(NeighbourTable.Neighbour neighbour, boolean withDevice) {
        StringBuilder line = new StringBuilder().append(neighbour.address()).append(' ');
        if (withDevice) {
            line.append("dev ").append(neighbour.device()).append(' ');
        }
        neighbour.mac().ifPresent(mac -> line.append("lladdr ").append(mac).append(' '));
        return line.append(neighbour.state().name()).append(' ').toString();
    }

    static String route(Route route) {
        StringBuilder line = new StringBuilder();
        line.append(route.destination().equals(Ipv4Prefix.DEFAULT) ? "default" : destination(route.destination()))
                .append(' ');
        route.gateway().ifPresent(gateway -> line.append("via ").append(gateway).append(' '));
        line.append("dev ").append(route.device()).append(' ');
        if (route.protocol() != RouteProtocol.BOOT) {
            line.append("proto ").append(route.protocol().text()).append(' ');
        }
        if (route.scope() != Scope.GLOBAL) {
            line.append("scope ").append(route.scope().text()).append(' ');
        }
        route.preferredSource().ifPresent(source -> line.append("src ").append(source).append(' '));
        return line.toString();
    }

    /** A host route (/32) is written as its address alone. */
    private static String destination(Ipv4Prefix prefix) {
        return prefix.length() == 32 ? prefix.network().toString() : prefix.toString();
    }

    private static String header(NetInterface link, boolean linkMode) {
        boolean loopback = link.kind() == NetInterface.Kind.LOOPBACK;
        String state = loopback ? "UNKNOWN" : link.carrier() ? "UP" : "DOWN";
        return link.index() + ": " + link.name() + ": <" + flags(link) + "> mtu " + link.mtu()
                + " qdisc " + (loopback ? "noqueue" : ETHERNET_QDISC) + " state " + state + " "
                + (linkMode ? "mode DEFAULT " : "") + "group default qlen " + QUEUE_LENGTH;
    }

    private static String flags(NetInterface link) {
        if (link.kind() == NetInterface.Kind.LOOPBACK) {
            return "LOOPBACK,UP,LOWER_UP";
        }
        return link.carrier() ? "BROADCAST,MULTICAST,UP,LOWER_UP" : "NO-CARRIER,BROADCAST,MULTICAST,UP";
    }

    private static String linkLayer(NetInterface link) {
        if (link.kind() == NetInterface.Kind.LOOPBACK) {
            MacAddress zero = link.mac();
            return "    link/loopback " + zero + " brd " + zero;
        }
        return "    link/ether " + link.mac() + " brd " + MacAddress.BROADCAST;
    }

    private static String lifetime(InterfaceAddress address, long now) {
        if (address.permanent()) {
            return "forever";
        }
        long left = Math.max(0L, address.validUntil() - now);
        return Math.ceilDiv(left, TICKS_PER_SECOND) + "sec";
    }
}
