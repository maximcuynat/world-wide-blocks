package io.github.maximcuynat.worldwideblocks.network.ip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.Latency;
import io.github.maximcuynat.worldwideblocks.network.protocol.ArpPacket;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.IcmpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Packet;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/** ICMP in the stack: echo, ping sockets, loopback, errors, and the forwarding of the box. */
class IcmpStackTest {
    private static final long SECOND = 20;
    private static final MacAddress OWN = new MacAddress(0x02_00_00_00_00_10L);
    private static final MacAddress PEER_MAC = new MacAddress(0x02_00_00_00_00_20L);
    private static final Ipv4Address ADDRESS = Ipv4Address.of(192, 168, 1, 10);
    private static final Ipv4Address PEER = Ipv4Address.of(192, 168, 1, 20);
    private static final Ipv4Address BROADCAST = Ipv4Address.of(192, 168, 1, 255);
    private static final Ipv4Address OUTSIDE = Ipv4Address.of(10, 9, 9, 9);

    private Ipv4Stack stack;
    private NetInterface eth0;
    private FakeMachine machine;

    @BeforeEach
    void setUp() {
        this.stack = new Ipv4Stack();
        this.eth0 = this.stack.addEthernet("eth0", 0, OWN, true);
        this.stack.addAddress(this.eth0, new InterfaceAddress(ADDRESS, 24, Optional.of(BROADCAST), Scope.GLOBAL,
                InterfaceAddress.FOREVER));
        this.machine = new FakeMachine(this.stack);
    }

    private void receive(MacAddress from, MacAddress to, Ipv4Packet packet) {
        this.stack.receive(this.machine, this.eth0, new EthernetFrame(from, to, packet));
    }

    /** The peer is known: REACHABLE, so what is sent to it leaves at once. */
    private void peerKnown() {
        this.stack.receive(this.machine, this.eth0, new EthernetFrame(PEER_MAC, OWN,
                ArpPacket.request(PEER_MAC, PEER, ADDRESS)));
        this.machine.clearSent();
    }

    private List<Ipv4Packet> packetsSent() {
        return this.machine.sent().stream().map(FakeMachine.Sent::frame).filter(f -> f.payload() instanceof Ipv4Packet)
                .map(f -> (Ipv4Packet) f.payload()).toList();
    }

    @Test
    void anEchoIsAnsweredFromTheAddressAsked() {
        this.peerKnown();

        this.receive(PEER_MAC, OWN, new Ipv4Packet(PEER, ADDRESS, IcmpMessage.Echo.request(7, 3, 56)));

        assertEquals(List.of(new Ipv4Packet(ADDRESS, PEER, new IcmpMessage.Echo(true, 7, 3, 56))), this.packetsSent());
        assertEquals(PEER_MAC, this.machine.sent().getFirst().frame().destination());
    }

    @Test
    void anEchoToABroadcastAddressIsIgnored() {
        this.peerKnown();

        this.receive(PEER_MAC, MacAddress.BROADCAST, new Ipv4Packet(PEER, BROADCAST, IcmpMessage.Echo.request(7, 1, 56)));
        this.receive(PEER_MAC, MacAddress.BROADCAST,
                new Ipv4Packet(PEER, Ipv4Address.BROADCAST, IcmpMessage.Echo.request(7, 2, 56)));

        assertTrue(this.machine.sent().isEmpty());
    }

    @Test
    void aReplyReachesTheSocketOfItsIdentifierWithItsLatency() {
        IcmpSocket socket = this.stack.openPing();
        IcmpSocket other = this.stack.openPing();
        this.machine.setLatency(104_000L);

        this.receive(PEER_MAC, OWN, new Ipv4Packet(PEER, ADDRESS, IcmpMessage.Echo.request(socket.identifier(), 1, 56).answer()));

        assertEquals(Optional.of(new IcmpSocket.Received(PEER, new IcmpMessage.Echo(true, socket.identifier(), 1, 56), 64, 104_000L)),
                socket.poll());
        assertFalse(other.hasWaiting());
    }

    @Test
    void aClosedSocketReceivesNothingAndItsIdentifierIsFreed() {
        IcmpSocket socket = this.stack.openPing();
        socket.close();

        this.receive(PEER_MAC, OWN, new Ipv4Packet(PEER, ADDRESS, IcmpMessage.Echo.request(socket.identifier(), 1, 56).answer()));

        assertFalse(socket.hasWaiting());
        assertTrue(this.stack.pingSockets().isEmpty());
    }

    @Test
    void anEchoToItselfGoesThroughTheLoopback() {
        IcmpSocket socket = this.stack.openPing();

        this.stack.sendEcho(this.machine, socket, ADDRESS, 1, 56);
        this.stack.sendEcho(this.machine, socket, Ipv4Address.of(127, 0, 0, 1), 2, 56);

        assertTrue(this.machine.sent().isEmpty(), "nothing on the wire");
        IcmpSocket.Received own = socket.poll().orElseThrow();
        assertEquals(ADDRESS, own.from());
        assertEquals(2 * Latency.LOOPBACK_NANOS, own.latency(), "the request then the reply through lo");
        assertEquals(Ipv4Address.of(127, 0, 0, 1), socket.poll().orElseThrow().from());
    }

    @Test
    void anEchoWithoutRouteIsNotSent() {
        IcmpSocket socket = this.stack.openPing();

        assertEquals(Ipv4Stack.Output.NO_ROUTE, this.stack.sendEcho(this.machine, socket, OUTSIDE, 1, 56));
        assertTrue(this.machine.sent().isEmpty());
        assertEquals(Optional.empty(), this.stack.sourceFor(OUTSIDE));
    }

    @Test
    void anEchoThatArpCannotDeliverComesBackAsHostUnreachableFromTheMachineItself() {
        IcmpSocket socket = this.stack.openPing();
        Ipv4Address absent = Ipv4Address.of(192, 168, 1, 250);

        this.stack.sendEcho(this.machine, socket, absent, 1, 56);
        this.machine.runUntil(3 * SECOND - 1);
        assertFalse(socket.hasWaiting());
        this.machine.runUntil(3 * SECOND);

        IcmpSocket.Received error = socket.poll().orElseThrow();
        assertEquals(ADDRESS, error.from());
        IcmpMessage.Unreachable unreachable = (IcmpMessage.Unreachable) error.message();
        assertEquals(IcmpMessage.Unreachable.HOST_UNREACHABLE, unreachable.code());
        assertEquals(new Ipv4Packet(ADDRESS, absent, IcmpMessage.Echo.request(socket.identifier(), 1, 56)), unreachable.original());
    }

    @Test
    void aComputerDropsAPacketForAnotherMachine() {
        this.peerKnown();

        this.receive(PEER_MAC, OWN, new Ipv4Packet(PEER, OUTSIDE, IcmpMessage.Echo.request(7, 1, 56)));

        assertTrue(this.machine.sent().isEmpty());
    }

    @Test
    void aRouterWithoutRouteAnswersNetUnreachable() {
        this.stack.setForwarding(true);
        this.peerKnown();
        Ipv4Packet request = new Ipv4Packet(PEER, OUTSIDE, IcmpMessage.Echo.request(7, 1, 56));

        this.receive(PEER_MAC, OWN, request);

        assertEquals(List.of(new Ipv4Packet(ADDRESS, PEER,
                new IcmpMessage.Unreachable(IcmpMessage.Unreachable.NET_UNREACHABLE, request))), this.packetsSent());
    }

    @Test
    void noErrorAboutAnErrorNorAboutABroadcastOrAnUnspecifiedSource() {
        this.stack.setForwarding(true);
        this.peerKnown();
        Ipv4Packet lost = new Ipv4Packet(OUTSIDE, PEER, IcmpMessage.Echo.request(7, 1, 56));

        this.receive(PEER_MAC, OWN, new Ipv4Packet(PEER, OUTSIDE,
                new IcmpMessage.Unreachable(IcmpMessage.Unreachable.HOST_UNREACHABLE, lost)));
        this.receive(PEER_MAC, OWN, new Ipv4Packet(Ipv4Address.ANY, OUTSIDE, IcmpMessage.Echo.request(7, 1, 56)));
        this.receive(PEER_MAC, MacAddress.BROADCAST, new Ipv4Packet(PEER, OUTSIDE, IcmpMessage.Echo.request(7, 1, 56)));

        assertTrue(this.machine.sent().isEmpty());
    }

    @Test
    void aRouterForwardsAlongItsRouteWithOneHopLess() {
        this.stack.setForwarding(true);
        NetInterface eth1 = this.stack.addEthernet("eth1", 1, new MacAddress(0x02_00_00_00_00_11L), true);
        this.stack.addAddress(eth1, new InterfaceAddress(Ipv4Address.of(10, 9, 9, 1), 24,
                Optional.of(Ipv4Address.of(10, 9, 9, 255)), Scope.GLOBAL, InterfaceAddress.FOREVER));

        this.receive(PEER_MAC, OWN, new Ipv4Packet(PEER, OUTSIDE, IcmpMessage.Echo.request(7, 1, 56)));

        FakeMachine.Sent request = this.machine.sent().getFirst();
        assertEquals(eth1, request.out());
        assertEquals(OUTSIDE, ((ArpPacket) request.frame().payload()).targetIp(), "the next hop is resolved first");
    }

    @Test
    void theSourceOfAPacketFollowsItsRoute() {
        this.stack.addRoute(new Route(Ipv4Prefix.DEFAULT, Optional.of(Ipv4Address.of(192, 168, 1, 1)), "eth0",
                RouteProtocol.BOOT, Scope.GLOBAL, Optional.empty()));

        assertEquals(Optional.of(ADDRESS), this.stack.sourceFor(OUTSIDE));
        assertEquals(Optional.of(Ipv4Address.of(127, 0, 0, 5)), this.stack.sourceFor(Ipv4Address.of(127, 0, 0, 5)));
        assertTrue(this.stack.isBroadcast(BROADCAST));
        assertFalse(this.stack.isBroadcast(PEER));
    }
}
