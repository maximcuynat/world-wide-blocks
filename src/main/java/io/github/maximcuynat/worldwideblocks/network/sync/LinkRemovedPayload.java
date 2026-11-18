package io.github.maximcuynat.worldwideblocks.network.sync;

import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A cable was just removed in the player's dimension. */
public record LinkRemovedPayload(UUID id) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LinkRemovedPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "link_removed"));

    public static final StreamCodec<ByteBuf, LinkRemovedPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, LinkRemovedPayload::id,
            LinkRemovedPayload::new);

    @Override
    public CustomPacketPayload.Type<LinkRemovedPayload> type() {
        return TYPE;
    }
}
