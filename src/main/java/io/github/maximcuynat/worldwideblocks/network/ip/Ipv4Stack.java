package io.github.maximcuynat.worldwideblocks.network.ip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Packet;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.protocol.UdpDatagram;

/**
 * The IPv4 stack of a machine, as the Linux kernel holds it: its interfaces and their addresses, the main
 * routing table, and the services bound to a UDP port.
 *
 * <p>A service bound to a UDP port ({@link #bindUdp}) receives the datagrams of the packets addressed to this
 * machine: one of its addresses, the broadcast address of the network of the interface, or 255.255.255.255. The
 * DHCP server of the box is one. A program that reads the frames before the IP layer, like ISC {@code dhclient}
 * through its packet socket on Linux, is called by the device before {@link #receive}, with {@link #keeps}.
 *
 * <p>The network card keeps the frames addressed to its MAC or to all. Packets for another machine are dropped:
 * forwarding comes with the router of the box (ICMP, issue #14).
 *
 * <p>Runtime state, not saved: rebuilt at server start, the addresses coming back with DHCP. Plain Java.
 */
public final class Ipv4Stack {
    /** Maximum transmission unit of an Ethernet interface: 1500 bytes. */
    public static final int ETHERNET_MTU = 1500;
    /** MTU of the loopback on Linux, as in the real output of {@code ip addr} ({@code mtu 65536}). */
    public static final int LOOPBACK_MTU = 65536;
    /** The loopback address. */
    public static final Ipv4Address LOOPBACK = Ipv4Address.of(127, 0, 0, 1);

    /** What the stack needs from the machine during a call. */
    public interface Context {
        /** Game time, in ticks. */
        long now();

        /** Puts a frame on the wire of this interface. */
        void send(NetInterface out, EthernetFrame frame);
    }

    /** A service bound to a UDP port. */
    @FunctionalInterface
    public interface UdpHandler {
        void receive(Context context, NetInterface in, EthernetFrame frame, Ipv4Packet packet, UdpDatagram datagram);
    }

    private final List<NetInterface> interfaces = new ArrayList<>();
    private final RoutingTable routes = new RoutingTable();
    private final Map<Integer, UdpHandler> udp = new HashMap<>();

    /** A stack with its loopback {@code lo}, 127.0.0.1/8. */
    public Ipv4Stack() {
        NetInterface lo = new NetInterface(1, "lo", NetInterface.Kind.LOOPBACK, NetInterface.NO_PORT,
                new MacAddress(0), LOOPBACK_MTU, true);
        this.interfaces.add(lo);
        this.addAddress(lo, new InterfaceAddress(LOOPBACK, 8, Optional.empty(), Scope.HOST, InterfaceAddress.FOREVER));
    }

    // ---- Interfaces ----

    /** Adds an Ethernet interface on a port of the device; its index follows the last one. */
    public NetInterface addEthernet(String name, int port, MacAddress mac, boolean carrier) {
        if (this.interfaceNamed(name).isPresent()) {
            throw new IllegalArgumentException("Interface " + name + " already exists");
        }
        NetInterface added = new NetInterface(this.interfaces.size() + 1, name, NetInterface.Kind.ETHERNET, port, mac,
                ETHERNET_MTU, carrier);
        this.interfaces.add(added);
        return added;
    }

    /** Interfaces, by index. */
    public List<NetInterface> interfaces() {
        return List.copyOf(this.interfaces);
    }

    public Optional<NetInterface> interfaceNamed(String name) {
        return this.interfaces.stream().filter(candidate -> candidate.name().equals(name)).findFirst();
    }

    /** The cable of the port is plugged in or removed. */
    public void setCarrier(NetInterface link, boolean carrier) {
        link.setCarrier(carrier);
    }

    // ---- Addresses and routes ----

    /**
     * Adds an address to an interface. Like the kernel ({@code fib_add_ifaddr}, {@code net/ipv4/fib_frontend.c}),
     * the route of its network goes to the main table, in {@code proto kernel scope link} with the address as
     * source; on the loopback, it goes to the local table, which {@code ip route} does not show.
     */
    public void addAddress(NetInterface link, InterfaceAddress address) {
        link.mutableAddresses().removeIf(existing -> existing.address().equals(address.address()));
        link.mutableAddresses().add(address);
        if (link.kind() != NetInterface.Kind.LOOPBACK && address.prefixLength() < 32) {
            this.routes.add(new Route(address.network(), Optional.empty(), link.name(), RouteProtocol.KERNEL,
                    Scope.LINK, Optional.of(address.address())));
        }
    }

    /**
     * Removes every address of an interface, and with the last one every route through it: the kernel disables IP
     * on an interface whose last address is removed ({@code fib_disable_ip}).
     */
    public void removeAddresses(NetInterface link) {
        link.mutableAddresses().clear();
        this.routes.removeDevice(link.name());
    }

    public void addRoute(Route route) {
        if (this.interfaceNamed(route.device()).isEmpty()) {
            throw new IllegalArgumentException("No interface " + route.device());
        }
        this.routes.add(route);
    }

    public RoutingTable routes() {
        return this.routes;
    }

    /** True if the address belongs to one of the interfaces of this machine. */
    public boolean isLocal(Ipv4Address address) {
        return this.interfaces.stream().flatMap(link -> link.mutableAddresses().stream())
                .anyMatch(owned -> owned.address().equals(address));
    }

    // ---- Services ----

    public void bindUdp(int port, UdpHandler handler) {
        if (this.udp.putIfAbsent(port, Objects.requireNonNull(handler, "handler")) != null) {
            throw new IllegalStateException("UDP port " + port + " already bound");
        }
    }

    // ---- Frames ----

    /** True if the network card of this interface keeps the frame: addressed to its MAC or to all. */
    public boolean keeps(NetInterface in, EthernetFrame frame) {
        MacAddress destination = frame.destination();
        return destination.isBroadcast() || destination.equals(in.mac());
    }

    /** A frame reached the network card of an interface. */
    public void receive(Context context, NetInterface in, EthernetFrame frame) {
        if (!this.keeps(in, frame)) {
            return; // not for this network card
        }
        if (frame.payload() instanceof Ipv4Packet packet && this.isForThisMachine(in, packet.destination())
                && packet.payload() instanceof UdpDatagram datagram) {
            UdpHandler handler = this.udp.get(datagram.destinationPort());
            if (handler != null) {
                handler.receive(context, in, frame, packet, datagram);
            }
        }
    }

    /** Addressed to one of the addresses of the machine, to the broadcast of the network of {@code in}, or to all. */
    private boolean isForThisMachine(NetInterface in, Ipv4Address destination) {
        return destination.isBroadcast() || this.isLocal(destination) || in.mutableAddresses().stream()
                .anyMatch(owned -> owned.broadcast().filter(destination::equals).isPresent());
    }
}
