package io.github.maximcuynat.worldwideblocks.network.ip;

import java.util.Objects;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;

/**
 * An IPv4 address of an interface, with its prefix, its broadcast address, its scope and its lifetime.
 *
 * @param broadcast  broadcast address of its network, written {@code brd} by {@code ip addr}
 * @param validUntil game time when the address stops being valid, or {@link #FOREVER}. An address with a finite
 *                   lifetime is not permanent: {@code ip addr} writes it {@code dynamic} (kernel
 *                   {@code net/ipv4/devinet.c}, {@code set_ifa_lifetime}: {@code IFA_F_PERMANENT} only for an
 *                   infinite lifetime; iproute2 {@code ip/ipaddress.c} prints {@code dynamic} without it)
 */
public record InterfaceAddress(Ipv4Address address, int prefixLength, Optional<Ipv4Address> broadcast, Scope scope,
        long validUntil) {
    /** Lifetime of a permanent address: {@code valid_lft forever}. */
    public static final long FOREVER = Long.MAX_VALUE;

    public InterfaceAddress {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(broadcast, "broadcast");
        Objects.requireNonNull(scope, "scope");
        if (prefixLength < 0 || prefixLength > 32) {
            throw new IllegalArgumentException("Prefix length out of range: " + prefixLength);
        }
    }

    /** The network of the address: destination of the route the kernel adds with it. */
    public Ipv4Prefix network() {
        return Ipv4Prefix.of(this.address, this.prefixLength);
    }

    public boolean permanent() {
        return this.validUntil == FOREVER;
    }
}
