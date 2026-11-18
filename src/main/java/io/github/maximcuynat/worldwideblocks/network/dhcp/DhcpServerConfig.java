package io.github.maximcuynat.worldwideblocks.network.dhcp;

import java.util.Objects;

import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;

/**
 * Configuration of a DHCP server: its own address, the pool it lends from, what it gives with each
 * address, and how long.
 *
 * @param serverAddress   address of the server, sent as its identifier (option 54)
 * @param subnetMask      mask of the network (option 1)
 * @param router          gateway (option 3)
 * @param dnsServer       name server (option 6)
 * @param firstAddress    first address of the pool
 * @param lastAddress     last address of the pool, included
 * @param leaseSeconds    lease time sent to the client (option 51), in game seconds
 * @param offerHoldTicks  how long an offered address stays reserved for the client if it does not request it.
 *                        RFC 2131 (4.3.2) leaves it to the implementation: "an implementation-specific timeout"
 */
public record DhcpServerConfig(Ipv4Address serverAddress, Ipv4Address subnetMask, Ipv4Address router,
        Ipv4Address dnsServer, Ipv4Address firstAddress, Ipv4Address lastAddress, long leaseSeconds, long offerHoldTicks) {
    /** A game second is 20 ticks. */
    public static final long TICKS_PER_SECOND = 20L;

    private static final Ipv4Address BOX_ADDRESS = Ipv4Address.of(192, 168, 1, 1);

    /**
     * The internet box (approved by Max on 2026-11-17): pool 192.168.1.10 to 192.168.1.200 in /24, the box at
     * 192.168.1.1 is gateway and name server, lease of one game day (24,000 ticks, 1,200 s), offered address
     * held 60 s.
     */
    public static final DhcpServerConfig INTERNET_BOX = new DhcpServerConfig(BOX_ADDRESS,
            Ipv4Address.of(255, 255, 255, 0), BOX_ADDRESS, BOX_ADDRESS,
            Ipv4Address.of(192, 168, 1, 10), Ipv4Address.of(192, 168, 1, 200), 1_200L, 60L * TICKS_PER_SECOND);

    public DhcpServerConfig {
        Objects.requireNonNull(serverAddress, "serverAddress");
        Objects.requireNonNull(subnetMask, "subnetMask");
        Objects.requireNonNull(router, "router");
        Objects.requireNonNull(dnsServer, "dnsServer");
        Objects.requireNonNull(firstAddress, "firstAddress");
        Objects.requireNonNull(lastAddress, "lastAddress");
        if (lastAddress.distanceFrom(firstAddress) < 0 || !firstAddress.sameNetwork(serverAddress, subnetMask)
                || !lastAddress.sameNetwork(serverAddress, subnetMask)) {
            throw new IllegalArgumentException("Pool " + firstAddress + " - " + lastAddress + " not in the network of " + serverAddress);
        }
        if (leaseSeconds <= 0 || offerHoldTicks <= 0) {
            throw new IllegalArgumentException("Non-positive duration");
        }
    }

    /** Number of addresses in the pool. */
    public int poolSize() {
        return (int) (this.lastAddress.distanceFrom(this.firstAddress) + 1);
    }

    /** Address number {@code index} of the pool, from 0. */
    public Ipv4Address poolAddress(int index) {
        return this.firstAddress.plus(index);
    }

    public boolean inPool(Ipv4Address address) {
        long position = address.distanceFrom(this.firstAddress);
        return position >= 0 && position < this.poolSize();
    }

    public long leaseTicks() {
        return this.leaseSeconds * TICKS_PER_SECOND;
    }
}
