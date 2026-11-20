package io.github.maximcuynat.worldwideblocks.network.sync;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The player is no longer at the keyboard of the computer at {@code pos} (too far, other dimension, computer removed,
 * lid closed, shell ended): the client closes its terminal screen.
 */
public record TerminalClosePayload(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TerminalClosePayload> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "terminal_close"));

    public static final StreamCodec<ByteBuf, TerminalClosePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalClosePayload::pos,
            TerminalClosePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
