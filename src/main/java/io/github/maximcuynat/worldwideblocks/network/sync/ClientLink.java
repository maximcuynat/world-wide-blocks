package io.github.maximcuynat.worldwideblocks.network.sync;

import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevices;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Ce que le client sait d'un câble : ses deux extrémités dans la dimension où se trouve le joueur.
 * Le client s'en sert uniquement pour l'affichage.
 */
public record ClientLink(UUID id, BlockPos a, int portA, BlockPos b, int portB) {
    public static final StreamCodec<ByteBuf, ClientLink> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ClientLink::id,
            BlockPos.STREAM_CODEC, ClientLink::a,
            ByteBufCodecs.VAR_INT, ClientLink::portA,
            BlockPos.STREAM_CODEC, ClientLink::b,
            ByteBufCodecs.VAR_INT, ClientLink::portB,
            ClientLink::new);

    public static ClientLink of(Link link) {
        return new ClientLink(link.id(),
                NetworkDevices.posOf(link.a().device()), link.a().index(),
                NetworkDevices.posOf(link.b().device()), link.b().index());
    }
}
