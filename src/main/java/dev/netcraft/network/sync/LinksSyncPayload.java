package dev.netcraft.network.sync;

import java.util.List;

import dev.netcraft.NetCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Liste complète des câbles de la dimension du joueur : remplace tout ce que le client connaissait. */
public record LinksSyncPayload(List<ClientLink> links) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LinksSyncPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NetCraft.MODID, "links_sync"));

    public static final StreamCodec<ByteBuf, LinksSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ClientLink.STREAM_CODEC.apply(ByteBufCodecs.list()), LinksSyncPayload::links,
            LinksSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<LinksSyncPayload> type() {
        return TYPE;
    }
}
