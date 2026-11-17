package dev.netcraft.network.sync;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Une MAC apprise par un switch derrière un de ses ports, telle que l'indicateur l'affiche.
 *
 * @param port numéro du port, à partir de 0
 * @param mac  valeur de l'adresse sur 48 bits (voir {@code MacAddress})
 */
public record LearnedMac(int port, long mac) {
    public static final StreamCodec<ByteBuf, LearnedMac> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LearnedMac::port,
            ByteBufCodecs.LONG, LearnedMac::mac,
            LearnedMac::new);
}
