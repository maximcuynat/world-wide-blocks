package io.github.maximcuynat.worldwideblocks.network.dhcp;

import java.util.Objects;

import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * Lease obtained by a DHCP client: its address, the configuration given with it, and its timers.
 * T1 and T2 take the default values of RFC 2131 (4.4.5): "T1 defaults to (0.5 * duration_of_lease). T2
 * defaults to (0.875 * duration_of_lease)".
 *
 * @param serverId   identifier of the server that lent the address (option 54)
 * @param serverMac  hardware address of that server, taken from its acknowledgement; saved with the lease. A
 *                   computer sends its renewal through its IP stack and ARP ({@link DhcpClient.Port#sendUnicast})
 * @param obtainedAt game time of the acknowledgement
 * @param expiresAt  game time when the lease ends
 */
public record DhcpLease(Ipv4Address address, Ipv4Address subnetMask, Ipv4Address router, Ipv4Address dnsServer,
        Ipv4Address serverId, MacAddress serverMac, long obtainedAt, long expiresAt) {

    public DhcpLease {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(subnetMask, "subnetMask");
        Objects.requireNonNull(router, "router");
        Objects.requireNonNull(dnsServer, "dnsServer");
        Objects.requireNonNull(serverId, "serverId");
        Objects.requireNonNull(serverMac, "serverMac");
        if (expiresAt <= obtainedAt) {
            throw new IllegalArgumentException("Lease ending before it starts");
        }
    }

    /** T1: time to renew with the server that lent the address. */
    public long renewAt() {
        return this.obtainedAt + (this.expiresAt - this.obtainedAt) / 2;
    }

    /** T2: time to ask any server. */
    public long rebindAt() {
        return this.obtainedAt + (this.expiresAt - this.obtainedAt) * 7 / 8;
    }
}
