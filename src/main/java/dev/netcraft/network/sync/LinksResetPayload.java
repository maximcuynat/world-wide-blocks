package dev.netcraft.network.sync;

import dev.netcraft.NetCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Le joueur arrive dans une dimension : le client oublie tous les câbles qu'il connaissait. */
public record LinksResetPayload() implements CustomPacketPayload {
    public static final LinksResetPayload INSTANCE = new LinksResetPayload();

    public static final CustomPacketPayload.Type<LinksResetPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NetCraft.MODID, "links_reset"));

    public static final StreamCodec<ByteBuf, LinksResetPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<LinksResetPayload> type() {
        return TYPE;
    }
}
