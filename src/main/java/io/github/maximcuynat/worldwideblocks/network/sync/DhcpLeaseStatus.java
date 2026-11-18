package io.github.maximcuynat.worldwideblocks.network.sync;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * An address lent by the DHCP server of a box, as the status panel shows it.
 *
 * @param address the address (32-bit value)
 * @param client  MAC it was lent to (48-bit value)
 */
public record DhcpLeaseStatus(int address, long client) {
    public static final StreamCodec<ByteBuf, DhcpLeaseStatus> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DhcpLeaseStatus::address,
            ByteBufCodecs.LONG, DhcpLeaseStatus::client,
            DhcpLeaseStatus::new);
}
