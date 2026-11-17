package dev.netcraft.network.sync;

import dev.netcraft.NetCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Un câble vient d'être branché dans la dimension du joueur. */
public record LinkAddedPayload(ClientLink link) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LinkAddedPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NetCraft.MODID, "link_added"));

    public static final StreamCodec<ByteBuf, LinkAddedPayload> STREAM_CODEC = StreamCodec.composite(
            ClientLink.STREAM_CODEC, LinkAddedPayload::link,
            LinkAddedPayload::new);

    @Override
    public CustomPacketPayload.Type<LinkAddedPayload> type() {
        return TYPE;
    }
}
