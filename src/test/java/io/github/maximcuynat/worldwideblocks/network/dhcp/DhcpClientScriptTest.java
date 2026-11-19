package io.github.maximcuynat.worldwideblocks.network.dhcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.ip.InterfaceAddress;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Prefix;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.ip.Route;
import io.github.maximcuynat.worldwideblocks.network.ip.RouteProtocol;
import io.github.maximcuynat.worldwideblocks.network.ip.Scope;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/** A lease applied to an interface as Debian's dhclient-script does. */
class DhcpClientScriptTest {
    private static final Ipv4Address BOX = Ipv4Address.of(192, 168, 1, 1);
    private static final Ipv4Address ADDRESS = Ipv4Address.of(192, 168, 1, 10);
    private static final Ipv4Address MASK = Ipv4Address.of(255, 255, 255, 0);

    private Ipv4Stack stack;
    private NetInterface eth0;

    @BeforeEach
    void setUp() {
        this.stack = new Ipv4Stack();
        this.eth0 = this.stack.addEthernet("eth0", 0, new MacAddress(0x02_00_00_00_00_01L), true);
    }

    private static DhcpLease lease(Ipv4Address address, long obtainedAt) {
        return new DhcpLease(address, MASK, BOX, BOX, BOX, new MacAddress(0x02_00_00_00_00_FFL), obtainedAt, obtainedAt + 24_000L);
    }

    @Test
    void aLeaseGivesADynamicAddressWithItsBroadcastAndTheDefaultRoute() {
        DhcpClientScript.apply(this.stack, this.eth0, Optional.of(lease(ADDRESS, 100)));

        InterfaceAddress address = this.eth0.addresses().getFirst();
        assertEquals(ADDRESS, address.address());
        assertEquals(24, address.prefixLength());
        assertEquals(Optional.of(Ipv4Address.of(192, 168, 1, 255)), address.broadcast(), "computed by dhclient without option 28");
        assertEquals(Scope.GLOBAL, address.scope());
        assertFalse(address.permanent());
        assertEquals(24_100L, address.validUntil());
        assertEquals(List.of(
                new Route(Ipv4Prefix.DEFAULT, Optional.of(BOX), "eth0", RouteProtocol.BOOT, Scope.GLOBAL, Optional.empty()),
                new Route(Ipv4Prefix.of(ADDRESS, 24), Optional.empty(), "eth0", RouteProtocol.KERNEL, Scope.LINK, Optional.of(ADDRESS))),
                this.stack.routes().routes());
    }

    @Test
    void aRenewalOnlyExtendsTheLifetime() {
        DhcpClientScript.apply(this.stack, this.eth0, Optional.of(lease(ADDRESS, 100)));
        List<Route> before = this.stack.routes().routes();

        DhcpClientScript.apply(this.stack, this.eth0, Optional.of(lease(ADDRESS, 12_100)));

        assertEquals(1, this.eth0.addresses().size());
        assertEquals(36_100L, this.eth0.addresses().getFirst().validUntil());
        assertEquals(before, this.stack.routes().routes());
    }

    @Test
    void aNewAddressReplacesTheOldOne() {
        DhcpClientScript.apply(this.stack, this.eth0, Optional.of(lease(ADDRESS, 100)));

        DhcpClientScript.apply(this.stack, this.eth0, Optional.of(lease(Ipv4Address.of(192, 168, 1, 11), 200)));

        assertEquals(List.of(Ipv4Address.of(192, 168, 1, 11)), this.eth0.addresses().stream().map(InterfaceAddress::address).toList());
        assertEquals(2, this.stack.routes().routes().size());
    }

    @Test
    void aLostLeaseRemovesTheAddressAndTheRoutes() {
        DhcpClientScript.apply(this.stack, this.eth0, Optional.of(lease(ADDRESS, 100)));

        DhcpClientScript.apply(this.stack, this.eth0, Optional.empty());

        assertTrue(this.eth0.addresses().isEmpty());
        assertTrue(this.stack.routes().routes().isEmpty());
    }
}
