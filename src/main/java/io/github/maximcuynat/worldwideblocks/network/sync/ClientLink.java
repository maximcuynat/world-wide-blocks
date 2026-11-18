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
 * What the client knows about a cable: its two ends in the dimension where the player is.
 * The client uses it only for display.
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
