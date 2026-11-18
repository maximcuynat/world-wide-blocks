package io.github.maximcuynat.worldwideblocks.network.sync;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The DHCP client of a computer port, as the status panel shows it. The lease is sent as its expiry time in
 * game time, not as a countdown: the client computes the time left, and the state does not change every tick.
 *
 * @param port      port number, from 0
 * @param state     ordinal of {@code DhcpClientState}
 * @param address   address of the port (32-bit value), 0 without a lease, like the other addresses
 * @param expiresAt end of the lease in game time, 0 without a lease
 */
public record DhcpPortStatus(int port, int state, int address, int subnetMask, int router, int dnsServer, long expiresAt) {
    public static final StreamCodec<ByteBuf, DhcpPortStatus> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DhcpPortStatus::port,
            ByteBufCodecs.VAR_INT, DhcpPortStatus::state,
            ByteBufCodecs.INT, DhcpPortStatus::address,
            ByteBufCodecs.INT, DhcpPortStatus::subnetMask,
            ByteBufCodecs.INT, DhcpPortStatus::router,
            ByteBufCodecs.INT, DhcpPortStatus::dnsServer,
            ByteBufCodecs.LONG, DhcpPortStatus::expiresAt,
            DhcpPortStatus::new);
}
