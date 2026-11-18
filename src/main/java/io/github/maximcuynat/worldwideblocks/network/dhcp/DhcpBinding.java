package io.github.maximcuynat.worldwideblocks.network.dhcp;

import java.util.Objects;

import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * What a server remembers of a client (RFC 2131, 1.: "binding"): the address lent to its MAC. The
 * binding outlives its lease: an expired binding frees the address for the others, but as long as no
 * other client needs it, the same client gets it back.
 *
 * @param expiresAt  game time when the lease (or the reservation of an offer) ends
 * @param assignedAt game time of the last offer or acknowledgement: the oldest expired binding is the
 *                   first reused when the pool is full (RFC 2131, 2.2: "the least recently assigned address")
 * @param offered    true while the address is only offered, not yet acknowledged
 */
public record DhcpBinding(MacAddress client, Ipv4Address address, long expiresAt, long assignedAt, boolean offered) {

    public DhcpBinding {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(address, "address");
    }

    /** True until {@code expiresAt} excluded: at that tick, the lease is over. */
    public boolean activeAt(long now) {
        return now < this.expiresAt;
    }

    /** True for an acknowledged lease still running: the address is in use by the client. */
    public boolean leasedAt(long now) {
        return !this.offered && this.activeAt(now);
    }
}
