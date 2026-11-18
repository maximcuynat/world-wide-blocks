package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Options of a DHCP message (RFC 2132) used by the simulation, each one present or absent. The message
 * type (option 53) is not here: it is a field of {@link DhcpMessage}.
 *
 * @param requestedAddress option 50, "Requested IP Address"
 * @param serverId         option 54, "Server Identifier": the address of the server
 * @param subnetMask       option 1, "Subnet Mask"
 * @param router           option 3, "Router" (the gateway; a single one is simulated)
 * @param dnsServer        option 6, "Domain Name Server" (a single one is simulated)
 * @param leaseSeconds     option 51, "IP Address Lease Time", in seconds (unsigned 32 bits)
 */
public record DhcpOptions(Optional<Ipv4Address> requestedAddress, Optional<Ipv4Address> serverId,
        Optional<Ipv4Address> subnetMask, Optional<Ipv4Address> router, Optional<Ipv4Address> dnsServer,
        OptionalLong leaseSeconds) {
    public static final DhcpOptions NONE = new DhcpOptions(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), OptionalLong.empty());

    public DhcpOptions {
        Objects.requireNonNull(requestedAddress, "requestedAddress");
        Objects.requireNonNull(serverId, "serverId");
        Objects.requireNonNull(subnetMask, "subnetMask");
        Objects.requireNonNull(router, "router");
        Objects.requireNonNull(dnsServer, "dnsServer");
        Objects.requireNonNull(leaseSeconds, "leaseSeconds");
        if (leaseSeconds.isPresent() && (leaseSeconds.getAsLong() < 0 || leaseSeconds.getAsLong() > 0xFFFF_FFFFL)) {
            throw new IllegalArgumentException("Lease time out of 32 bits: " + leaseSeconds.getAsLong());
        }
    }

    /** Only the server identifier (NAK). */
    public static DhcpOptions server(Ipv4Address serverId) {
        return new DhcpOptions(Optional.empty(), Optional.of(serverId), Optional.empty(), Optional.empty(),
                Optional.empty(), OptionalLong.empty());
    }

    /** Only the requested address (request after INIT-REBOOT). */
    public static DhcpOptions requested(Ipv4Address address) {
        return new DhcpOptions(Optional.of(address), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), OptionalLong.empty());
    }

    /** The server chosen by the client, and the address it offered (request after SELECTING). */
    public static DhcpOptions selected(Ipv4Address serverId, Ipv4Address address) {
        return new DhcpOptions(Optional.of(address), Optional.of(serverId), Optional.empty(), Optional.empty(),
                Optional.empty(), OptionalLong.empty());
    }

    /** What a server gives with an address (offer and acknowledgement). */
    public static DhcpOptions lease(Ipv4Address serverId, Ipv4Address subnetMask, Ipv4Address router,
            Ipv4Address dnsServer, long leaseSeconds) {
        return new DhcpOptions(Optional.empty(), Optional.of(serverId), Optional.of(subnetMask), Optional.of(router),
                Optional.of(dnsServer), OptionalLong.of(leaseSeconds));
    }
}
