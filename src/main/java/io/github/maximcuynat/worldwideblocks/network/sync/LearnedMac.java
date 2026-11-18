package io.github.maximcuynat.worldwideblocks.network.sync;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A MAC learned by a switch behind one of its ports, as the status panel shows it.
 *
 * @param port port number, from 0
 * @param mac  48-bit address value (see {@code MacAddress})
 */
public record LearnedMac(int port, long mac) {
    public static final StreamCodec<ByteBuf, LearnedMac> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LearnedMac::port,
            ByteBufCodecs.LONG, LearnedMac::mac,
            LearnedMac::new);
}
