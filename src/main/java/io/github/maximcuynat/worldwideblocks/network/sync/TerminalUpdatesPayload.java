package io.github.maximcuynat.worldwideblocks.network.sync;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalUpdate;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Updates of the console of the computer at {@code pos} for the player at its keyboard, in order; the first packet of
 * a screen starts with {@link TerminalUpdate.Reset}. The only packet where the server sends written text: the output of
 * the commands, literal English ({@code docs/messages.md}, section 3).
 */
public record TerminalUpdatesPayload(BlockPos pos, List<TerminalUpdate> updates) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TerminalUpdatesPayload> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "terminal_updates"));

    public static final StreamCodec<ByteBuf, TerminalUpdatesPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalUpdatesPayload::pos,
            TerminalCodecs.UPDATES, TerminalUpdatesPayload::updates,
            TerminalUpdatesPayload::new);

    public TerminalUpdatesPayload {
        updates = List.copyOf(updates);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
