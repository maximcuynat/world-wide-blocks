package dev.netcraft.network.sync;

import java.util.UUID;

import dev.netcraft.NetCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Un câble vient d'être retiré dans la dimension du joueur. */
public record LinkRemovedPayload(UUID id) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LinkRemovedPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NetCraft.MODID, "link_removed"));

    public static final StreamCodec<ByteBuf, LinkRemovedPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, LinkRemovedPayload::id,
            LinkRemovedPayload::new);

    @Override
    public CustomPacketPayload.Type<LinkRemovedPayload> type() {
        return TYPE;
    }
}
