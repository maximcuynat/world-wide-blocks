package io.github.maximcuynat.worldwideblocks.network.dhcp;

import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.ip.InterfaceAddress;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Prefix;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.ip.Route;
import io.github.maximcuynat.worldwideblocks.network.ip.RouteProtocol;
import io.github.maximcuynat.worldwideblocks.network.ip.Scope;

/**
 * Applies a DHCP lease to an interface, as Debian's {@code dhclient-script} does on Linux (decision of lot 1c-1:
 * the machine behaves like Debian with ifupdown and {@code dhclient}). Read in {@code debian/dhclient-script.linux}
 * of the {@code isc-dhcp} package on 2026-11-19, not run:
 *
 * <ul>
 * <li>New address: {@code ip -4 addr add <address>/<mask> broadcast <broadcast> valid_lft <lease> preferred_lft
 * <lease> dev <interface>}. When the server sends no broadcast address (option 28, which the box does not send),
 * {@code dhclient} computes it from the address and the mask ({@code client/dhclient.c}, {@code broadcast_addr}).
 * The kernel adds the route of the network with the address.</li>
 * <li>Then {@code ip -4 route add default via <router> dev <interface>}, without {@code proto}: the route is
 * {@code proto boot}, which {@code ip route} does not write, and without metric for a single router.</li>
 * <li>Same address again (renewal): {@code ip -4 addr change} with the new lifetimes, the routes stay.</li>
 * <li>Lease lost: the addresses of the interface are removed, and the kernel removes its routes with the last one.</li>
 * </ul>
 */
public final class DhcpClientScript {

    private DhcpClientScript() {
    }

    public static void apply(Ipv4Stack stack, NetInterface link, Optional<DhcpLease> lease) {
        if (lease.isEmpty()) {
            stack.removeAddresses(link);
            return;
        }
        DhcpLease bound = lease.get();
        int length = Ipv4Prefix.lengthOf(bound.subnetMask());
        InterfaceAddress address = new InterfaceAddress(bound.address(), length,
                Optional.of(Ipv4Prefix.of(bound.address(), length).broadcast()), Scope.GLOBAL, bound.expiresAt());
        boolean renewal = link.addresses().stream().anyMatch(owned -> owned.address().equals(bound.address()));
        if (!renewal) {
            stack.removeAddresses(link);
        }
        stack.addAddress(link, address);
        if (!renewal) {
            stack.addRoute(new Route(Ipv4Prefix.DEFAULT, Optional.of(bound.router()), link.name(), RouteProtocol.BOOT,
                    Scope.GLOBAL, Optional.empty()));
        }
    }
}
