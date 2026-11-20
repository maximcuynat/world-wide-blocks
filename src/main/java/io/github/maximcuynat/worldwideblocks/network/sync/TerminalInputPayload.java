package io.github.maximcuynat.worldwideblocks.network.sync;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * What the player does at the keyboard of the computer at {@code pos}, for the session of the console the screen
 * shows. The first packet from the client of the mod: the server checks each one again ({@code TerminalServer}).
 *
 * @param text the line typed ({@link Kind#LINE}), or the text typed at the prompt before Ctrl+C
 *             ({@link Kind#INTERRUPT}); empty otherwise
 */
public record TerminalInputPayload(BlockPos pos, int session, Kind kind, String text) implements CustomPacketPayload {
    /** Longest text decoded; the server accepts {@code TerminalRules.MAX_LINE} characters and refuses the rest. */
    private static final int MAX_DECODED = 4096;

    /** What the key or the line does. */
    public enum Kind {
        /** Enter: the line typed. */
        LINE,
        /** Ctrl+C. */
        INTERRUPT,
        /** Ctrl+D on an empty line. */
        END_OF_INPUT,
        /** Ctrl+L. */
        CLEAR,
        /** Esc: the screen is hidden, the session goes on. */
        CLOSE
    }

    public static final CustomPacketPayload.Type<TerminalInputPayload> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "terminal_input"));

    private static final StreamCodec<ByteBuf, Kind> KIND = ByteBufCodecs.idMapper(i -> Kind.values()[i], Kind::ordinal);

    public static final StreamCodec<ByteBuf, TerminalInputPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalInputPayload::pos,
            ByteBufCodecs.VAR_INT, TerminalInputPayload::session,
            KIND, TerminalInputPayload::kind,
            ByteBufCodecs.stringUtf8(MAX_DECODED), TerminalInputPayload::text,
            TerminalInputPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
