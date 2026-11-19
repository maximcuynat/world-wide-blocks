package io.github.maximcuynat.worldwideblocks.network.ip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpFrames;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

class Ipv4StackTest {
    private static final MacAddress OWN = new MacAddress(0x02_00_00_00_00_01L);
    private static final MacAddress OTHER = new MacAddress(0x02_00_00_00_00_02L);
    private static final Ipv4Address ADDRESS = Ipv4Address.of(192, 168, 1, 1);

    private final List<Integer> received = new ArrayList<>();
    private final Ipv4Stack.Context context = new Ipv4Stack.Context() {
        @Override
        public long now() {
            return 0;
        }

        @Override
        public void send(NetInterface out, EthernetFrame frame) {
        }

        @Override
        public void schedule(long delayTicks, Object key) {
        }
    };
    private Ipv4Stack stack;
    private NetInterface eth0;

    @BeforeEach
    void setUp() {
        this.stack = new Ipv4Stack();
        this.eth0 = this.stack.addEthernet("eth0", 0, OWN, true);
        this.stack.bindUdp(DhcpMessage.SERVER_PORT, (call, in, frame, packet, datagram) -> this.received.add(datagram.destinationPort()));
    }

    private static InterfaceAddress permanent(Ipv4Address address, int length) {
        return new InterfaceAddress(address, length, Optional.of(Ipv4Prefix.of(address, length).broadcast()), Scope.GLOBAL,
                InterfaceAddress.FOREVER);
    }

    private static EthernetFrame toServer(MacAddress destinationMac, Ipv4Address destination) {
        return DhcpFrames.fromClient(OTHER, Ipv4Address.ANY, destinationMac, destination, DhcpMessage.discover(1, OTHER));
    }

    @Test
    void aNewStackHasItsLoopbackOnly() {
        assertEquals(List.of("lo", "eth0"), this.stack.interfaces().stream().map(NetInterface::name).toList());
        NetInterface lo = this.stack.interfaces().getFirst();
        assertEquals(1, lo.index());
        assertEquals(Ipv4Stack.LOOPBACK_MTU, lo.mtu());
        assertEquals(Ipv4Stack.LOOPBACK, lo.addresses().getFirst().address());
        assertEquals(Scope.HOST, lo.addresses().getFirst().scope());
        assertEquals(2, this.eth0.index());
        assertTrue(this.stack.routes().routes().isEmpty(), "the routes of the loopback are in the local table");
    }

    @Test
    void anAddressBringsTheRouteOfItsNetwork() {
        this.stack.addAddress(this.eth0, permanent(ADDRESS, 24));

        assertEquals(List.of(new Route(Ipv4Prefix.of(ADDRESS, 24), Optional.empty(), "eth0", RouteProtocol.KERNEL, Scope.LINK,
                Optional.of(ADDRESS))), this.stack.routes().routes());
        assertTrue(this.stack.isLocal(ADDRESS));
    }

    @Test
    void removingTheAddressesRemovesTheRoutesOfTheInterface() {
        this.stack.addAddress(this.eth0, permanent(ADDRESS, 24));
        this.stack.addRoute(new Route(Ipv4Prefix.DEFAULT, Optional.of(Ipv4Address.of(192, 168, 1, 254)), "eth0",
                RouteProtocol.BOOT, Scope.GLOBAL, Optional.empty()));

        this.stack.removeAddresses(this.eth0);

        assertTrue(this.eth0.addresses().isEmpty());
        assertTrue(this.stack.routes().routes().isEmpty());
        assertFalse(this.stack.isLocal(ADDRESS));
    }

    @Test
    void theLongestPrefixWins() {
        this.stack.addAddress(this.eth0, permanent(ADDRESS, 24));
        this.stack.addRoute(new Route(Ipv4Prefix.DEFAULT, Optional.of(Ipv4Address.of(192, 168, 1, 254)), "eth0",
                RouteProtocol.BOOT, Scope.GLOBAL, Optional.empty()));

        assertEquals(24, this.stack.routes().lookup(Ipv4Address.of(192, 168, 1, 7)).orElseThrow().destination().length());
        assertEquals(0, this.stack.routes().lookup(Ipv4Address.of(8, 8, 8, 8)).orElseThrow().destination().length());
        assertEquals(Ipv4Prefix.DEFAULT, this.stack.routes().routes().getFirst().destination(),
                "default first, as in the real output");
    }

    @Test
    void aBoundServiceReceivesWhatIsAddressedToThisMachine() {
        this.stack.addAddress(this.eth0, permanent(ADDRESS, 24));

        this.stack.receive(this.context, this.eth0, toServer(MacAddress.BROADCAST, Ipv4Address.BROADCAST));
        this.stack.receive(this.context, this.eth0, toServer(OWN, ADDRESS));
        this.stack.receive(this.context, this.eth0, toServer(MacAddress.BROADCAST, Ipv4Address.of(192, 168, 1, 255)));

        assertEquals(3, this.received.size());
    }

    @Test
    void aPacketForAnotherMachineOrAFrameForAnotherCardIsDropped() {
        this.stack.addAddress(this.eth0, permanent(ADDRESS, 24));

        this.stack.receive(this.context, this.eth0, toServer(OWN, Ipv4Address.of(192, 168, 1, 99)));
        this.stack.receive(this.context, this.eth0, toServer(OTHER, ADDRESS));

        assertTrue(this.received.isEmpty());
        assertFalse(this.stack.keeps(this.eth0, toServer(OTHER, ADDRESS)));
    }
}
