package io.github.maximcuynat.worldwideblocks.network.ip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.Latency;
import io.github.maximcuynat.worldwideblocks.network.protocol.ArpPacket;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.IcmpMessage;
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
 * <p>The network card keeps the frames addressed to its MAC or to all. A packet for another machine is dropped,
 * unless the stack forwards ({@link #setForwarding}, the box): it then follows its route, and a destination without
 * a route gets back a destination unreachable, code 0 (RFC 1812, 4.3.3.1; Linux {@code ip_error}).
 *
 * <p>A packet sent ({@link #send}) follows the route of the longest prefix to its next hop, whose hardware address
 * comes from the ARP cache ({@link NeighbourTable}). A packet for this machine itself (one of its addresses, or
 * 127.0.0.0/8) goes through the loopback {@code lo}, as the local table of Linux sends it.
 *
 * <p>ICMP (RFC 792): an echo request for one of the addresses of the machine is answered from that address, an echo
 * to a broadcast address is ignored ({@code icmp_echo_ignore_broadcasts}, on by default). Echo replies and the
 * errors about an echo of this machine go to the ping socket of their identifier ({@link IcmpSocket}); an echo of
 * this machine whose next hop is not resolved by ARP comes back to its socket as a destination unreachable, code 1,
 * from the address of the machine (Linux {@code arp_error_report}, then {@code icmp_send}). No error is sent about an
 * ICMP error, about a packet from 0.0.0.0 or to a broadcast address (RFC 1122, 3.2.2).
 *
 * <p>Differences: no Time Exceeded (the TTL of a forwarded packet that reaches 0 is dropped silently, until
 * {@code traceroute}, lot 6c), no redirect, no rate limit on ICMP errors (decision of 2026-11-19).
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
    /** 127.0.0.0/8: every address of it belongs to the loopback (local table of Linux). */
    private static final Ipv4Prefix LOOPBACK_NETWORK = new Ipv4Prefix(Ipv4Address.of(127, 0, 0, 0), 8);

    /** What the stack needs from the machine during a call. */
    public interface Context {
        /** Game time, in ticks. */
        long now();

        /** Puts a frame on the wire of this interface. */
        void send(NetInterface out, EthernetFrame frame);

        /** Calls {@link Ipv4Stack#onTimer} with this key in {@code delayTicks} ticks. */
        void schedule(long delayTicks, Object key);

        /** Time spent since the start of the tick in this call, in nanoseconds ({@link Latency}). */
        default long latency() {
            return 0L;
        }

        /** Time passes in the machine during this call (the loopback). */
        default void elapse(long nanos) {
        }
    }

    /** What became of a packet given to {@link #send}. */
    public enum Output {
        /** Sent, or waiting for the hardware address of its next hop. */
        SENT,
        /** No route to its destination ({@code ENETUNREACH}). */
        NO_ROUTE
    }

    /** A service bound to a UDP port. */
    @FunctionalInterface
    public interface UdpHandler {
        void receive(Context context, NetInterface in, EthernetFrame frame, Ipv4Packet packet, UdpDatagram datagram);
    }

    private final List<NetInterface> interfaces = new ArrayList<>();
    private final RoutingTable routes = new RoutingTable();
    private final NeighbourTable neighbours = new NeighbourTable(this);
    private final Map<Integer, UdpHandler> udp = new HashMap<>();
    private final Map<Integer, IcmpSocket> pingSockets = new HashMap<>();
    private int nextIdentifier = 1;
    private boolean forwarding;

    /** An echo of this machine that ARP could not deliver comes back to its socket. */
    private final NeighbourTable.Failure localFailure = new NeighbourTable.Failure() {
        @Override
        public void unreachable(Ipv4Packet packet) {
            throw new IllegalStateException("Needs the context of the call");
        }

        @Override
        public void unreachable(Context context, Ipv4Packet packet) {
            Ipv4Stack.this.icmpInput(context, new Ipv4Packet(packet.source(), packet.source(),
                    new IcmpMessage.Unreachable(IcmpMessage.Unreachable.HOST_UNREACHABLE, packet)));
        }
    };

    /** A forwarded packet that ARP could not deliver: its source is told. */
    private final NeighbourTable.Failure forwardFailure = new NeighbourTable.Failure() {
        @Override
        public void unreachable(Ipv4Packet packet) {
            throw new IllegalStateException("Needs the context of the call");
        }

        @Override
        public void unreachable(Context context, Ipv4Packet packet) {
            Ipv4Stack.this.sendError(context, packet, IcmpMessage.Unreachable.HOST_UNREACHABLE);
        }
    };

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

    /** The cable of the port is plugged in or removed; without carrier, the ARP entries of the interface go. */
    public void setCarrier(NetInterface link, boolean carrier) {
        link.setCarrier(carrier);
        if (!carrier) {
            this.neighbours.flush(link);
        }
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

    public NeighbourTable neighbours() {
        return this.neighbours;
    }

    /** First address of an interface: the source of what it sends when nothing else chooses one. */
    public Optional<Ipv4Address> primaryAddress(NetInterface link) {
        return link.mutableAddresses().stream().findFirst().map(InterfaceAddress::address);
    }

    /** True if the address belongs to one of the interfaces of this machine. */
    public boolean isLocal(Ipv4Address address) {
        return this.interfaces.stream().flatMap(link -> link.mutableAddresses().stream())
                .anyMatch(owned -> owned.address().equals(address));
    }

    /** True for a packet this machine delivers to itself: one of its addresses, or 127.0.0.0/8. */
    private boolean isLoopbackDestination(Ipv4Address address) {
        return this.isLocal(address) || LOOPBACK_NETWORK.contains(address);
    }

    /** 255.255.255.255, or the broadcast address of the network of one of the interfaces. */
    public boolean isBroadcast(Ipv4Address address) {
        return address.isBroadcast() || this.interfaces.stream().flatMap(link -> link.mutableAddresses().stream())
                .anyMatch(owned -> owned.broadcast().filter(address::equals).isPresent());
    }

    /**
     * The source address of what this machine sends to {@code destination}, as the kernel chooses it when
     * {@code ping} connects its socket: the destination itself if it is local, otherwise the source of its route,
     * or the first address of the interface of that route. Empty without a route ({@code ENETUNREACH}).
     */
    public Optional<Ipv4Address> sourceFor(Ipv4Address destination) {
        if (this.isLoopbackDestination(destination)) {
            return Optional.of(destination);
        }
        return this.routes.lookup(destination).flatMap(route -> route.preferredSource()
                .or(() -> this.interfaceNamed(route.device()).flatMap(this::primaryAddress)));
    }

    /** Forwarding of the packets for other machines ({@code net.ipv4.ip_forward}): on for the box only. */
    public void setForwarding(boolean forwarding) {
        this.forwarding = forwarding;
    }

    public boolean forwarding() {
        return this.forwarding;
    }

    // ---- Services ----

    public void bindUdp(int port, UdpHandler handler) {
        if (this.udp.putIfAbsent(port, Objects.requireNonNull(handler, "handler")) != null) {
            throw new IllegalStateException("UDP port " + port + " already bound");
        }
    }

    // ---- Ping sockets ----

    /** Opens a ping socket, with the first identifier free from the last one given. */
    public IcmpSocket openPing() {
        for (int tries = 0; tries < 0xFFFF; tries++) {
            int identifier = this.nextIdentifier;
            this.nextIdentifier = identifier == 0xFFFF ? 1 : identifier + 1;
            if (!this.pingSockets.containsKey(identifier)) {
                IcmpSocket socket = new IcmpSocket(this, identifier);
                this.pingSockets.put(identifier, socket);
                return socket;
            }
        }
        throw new IllegalStateException("No free ping identifier");
    }

    /** Open ping sockets. */
    public List<IcmpSocket> pingSockets() {
        return List.copyOf(this.pingSockets.values());
    }

    void release(IcmpSocket socket) {
        this.pingSockets.remove(socket.identifier(), socket);
    }

    /**
     * Sends an echo request of this socket, from the source address of its route ({@link #sourceFor}). If ARP does
     * not resolve its next hop, a destination unreachable comes back to the socket.
     *
     * @return {@link Output#NO_ROUTE} when there is no route: nothing is sent and nothing comes back
     */
    public Output sendEcho(Context context, IcmpSocket socket, Ipv4Address destination, int sequence, int dataLength) {
        Optional<Ipv4Address> source = this.sourceFor(destination);
        if (source.isEmpty()) {
            return Output.NO_ROUTE;
        }
        IcmpMessage.Echo request = IcmpMessage.Echo.request(socket.identifier(), sequence & 0xFFFF, dataLength);
        return this.send(context, new Ipv4Packet(source.get(), destination, request), this.localFailure);
    }

    // ---- Packets ----

    /**
     * Sends a packet built by this machine: route of the longest prefix, then the next hop (the gateway, or the
     * destination itself on a connected network), resolved by ARP.
     *
     * @param failure told of the packet if its next hop cannot be resolved
     */
    public Output send(Context context, Ipv4Packet packet, NeighbourTable.Failure failure) {
        if (this.isLoopbackDestination(packet.destination())) {
            context.elapse(Latency.LOOPBACK_NANOS);
            if (packet.payload() instanceof IcmpMessage) {
                this.icmpInput(context, packet); // other protocols have no local service through lo in lot 1c-1
            }
            return Output.SENT;
        }
        Optional<Route> route = this.routes.lookup(packet.destination());
        if (route.isEmpty()) {
            return Output.NO_ROUTE;
        }
        NetInterface out = this.interfaceNamed(route.get().device()).orElseThrow();
        Ipv4Address nextHop = route.get().gateway().orElse(packet.destination());
        this.neighbours.output(context, out, nextHop, packet, failure);
        return Output.SENT;
    }

    /** A timer asked through {@link Context#schedule} is due. */
    public void onTimer(Context context, Object key) {
        if (key instanceof NeighbourTable.Timer timer) {
            this.neighbours.onTimer(context, timer);
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
        if (frame.payload() instanceof ArpPacket arp) {
            this.neighbours.receive(context, in, frame, arp);
            return;
        }
        if (!(frame.payload() instanceof Ipv4Packet packet)) {
            return;
        }
        if (!this.isForThisMachine(in, packet.destination())) {
            if (this.forwarding && !frame.isBroadcast()) {
                this.forward(context, packet);
            }
            return;
        }
        if (packet.payload() instanceof UdpDatagram datagram) {
            UdpHandler handler = this.udp.get(datagram.destinationPort());
            if (handler != null) {
                handler.receive(context, in, frame, packet, datagram);
            }
        } else if (packet.payload() instanceof IcmpMessage) {
            this.icmpInput(context, packet);
        }
    }

    /** {@code icmp_rcv}: an ICMP message for this machine. */
    private void icmpInput(Context context, Ipv4Packet packet) {
        switch ((IcmpMessage) packet.payload()) {
            case IcmpMessage.Echo echo when !echo.reply() -> {
                if (!this.isBroadcast(packet.destination()) && !packet.source().isAny()
                        && !this.isBroadcast(packet.source())) {
                    this.send(context, new Ipv4Packet(packet.destination(), packet.source(), echo.answer()),
                            NeighbourTable.Failure.NONE);
                }
            }
            case IcmpMessage.Echo echo -> this.toSocket(context, echo.identifier(), packet);
            case IcmpMessage.Unreachable error -> {
                if (error.original().payload() instanceof IcmpMessage.Echo echo && !echo.reply()
                        && this.isLoopbackDestination(error.original().source())) {
                    this.toSocket(context, echo.identifier(), packet);
                }
            }
        }
    }

    private void toSocket(Context context, int identifier, Ipv4Packet packet) {
        IcmpSocket socket = this.pingSockets.get(identifier);
        if (socket != null) {
            socket.deliver(new IcmpSocket.Received(packet.source(), (IcmpMessage) packet.payload(), packet.ttl(),
                    context.latency()));
        }
    }

    /** {@code ip_forward}: a packet for another machine, on a stack that forwards. */
    private void forward(Context context, Ipv4Packet packet) {
        if (packet.source().isAny() || packet.destination().isBroadcast()) {
            return;
        }
        Optional<Route> route = this.routes.lookup(packet.destination());
        if (route.isEmpty()) {
            this.sendError(context, packet, IcmpMessage.Unreachable.NET_UNREACHABLE);
            return;
        }
        if (packet.ttl() <= 1) {
            return; // Time Exceeded comes with traceroute (lot 6c)
        }
        Ipv4Packet forwarded = new Ipv4Packet(packet.source(), packet.destination(), packet.protocol(),
                packet.ttl() - 1, packet.payload());
        NetInterface out = this.interfaceNamed(route.get().device()).orElseThrow();
        this.neighbours.output(context, out, route.get().gateway().orElse(packet.destination()), forwarded,
                this.forwardFailure);
    }

    /** {@code icmp_send}: a destination unreachable back to the source of {@code original}, if one is allowed. */
    private void sendError(Context context, Ipv4Packet original, int code) {
        if (original.payload() instanceof IcmpMessage message && message.isError()) {
            return;
        }
        if (original.source().isAny() || this.isBroadcast(original.source()) || this.isBroadcast(original.destination())) {
            return;
        }
        this.sourceFor(original.source()).ifPresent(from -> this.send(context,
                new Ipv4Packet(from, original.source(), new IcmpMessage.Unreachable(code, original)),
                NeighbourTable.Failure.NONE));
    }

    /** Addressed to one of the addresses of the machine, to the broadcast of the network of {@code in}, or to all. */
    private boolean isForThisMachine(NetInterface in, Ipv4Address destination) {
        return destination.isBroadcast() || this.isLocal(destination) || in.mutableAddresses().stream()
                .anyMatch(owned -> owned.broadcast().filter(destination::equals).isPresent());
    }
}
