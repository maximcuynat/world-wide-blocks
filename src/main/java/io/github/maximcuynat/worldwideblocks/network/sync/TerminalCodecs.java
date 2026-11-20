package io.github.maximcuynat.worldwideblocks.network.sync;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.os.ConsoleLine;
import io.github.maximcuynat.worldwideblocks.os.shell.Prompt;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalUpdate;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** How the updates of a console travel to a terminal screen: a kind, then its content. */
final class TerminalCodecs {
    private static final StreamCodec<ByteBuf, String> TEXT = ByteBufCodecs.STRING_UTF8;

    static final StreamCodec<ByteBuf, Prompt> PROMPT = StreamCodec.composite(
            TEXT, Prompt::user,
            TEXT, Prompt::host,
            TEXT, Prompt::directory,
            Prompt::new);

    static final StreamCodec<ByteBuf, ConsoleLine> LINE = StreamCodec.of(TerminalCodecs::encodeLine, TerminalCodecs::decodeLine);

    static final StreamCodec<ByteBuf, TerminalUpdate> UPDATE = StreamCodec.of(TerminalCodecs::encodeUpdate, TerminalCodecs::decodeUpdate);

    private TerminalCodecs() {
    }

    private static void encodeLine(ByteBuf buffer, ConsoleLine line) {
        switch (line) {
            case ConsoleLine.Output output -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 0);
                TEXT.encode(buffer, output.text());
            }
            case ConsoleLine.Input input -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 1);
                PROMPT.encode(buffer, input.prompt());
                TEXT.encode(buffer, input.text());
            }
            case ConsoleLine.Game game -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 2);
                TEXT.encode(buffer, game.key());
                TEXT.apply(ByteBufCodecs.list()).encode(buffer, game.values());
            }
            case ConsoleLine.Advice advice -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 3);
                TEXT.encode(buffer, advice.key());
                TEXT.apply(ByteBufCodecs.list()).encode(buffer, advice.values());
            }
        }
    }

    private static ConsoleLine decodeLine(ByteBuf buffer) {
        int kind = ByteBufCodecs.VAR_INT.decode(buffer);
        return switch (kind) {
            case 0 -> new ConsoleLine.Output(TEXT.decode(buffer));
            case 1 -> new ConsoleLine.Input(PROMPT.decode(buffer), TEXT.decode(buffer));
            case 2 -> new ConsoleLine.Game(TEXT.decode(buffer), TEXT.apply(ByteBufCodecs.list()).decode(buffer));
            case 3 -> new ConsoleLine.Advice(TEXT.decode(buffer), TEXT.apply(ByteBufCodecs.list()).decode(buffer));
            default -> throw new IllegalArgumentException("Unknown kind of console line " + kind);
        };
    }

    private static void encodeUpdate(ByteBuf buffer, TerminalUpdate update) {
        switch (update) {
            case TerminalUpdate.Reset reset -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 0);
                ByteBufCodecs.VAR_INT.encode(buffer, reset.session());
                PROMPT.encode(buffer, reset.prompt());
                TEXT.encode(buffer, reset.current());
                ByteBufCodecs.BOOL.encode(buffer, reset.running());
            }
            case TerminalUpdate.Line line -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 1);
                LINE.encode(buffer, line.line());
            }
            case TerminalUpdate.Current current -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 2);
                TEXT.encode(buffer, current.text());
            }
            case TerminalUpdate.Cleared cleared -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 3);
                ByteBufCodecs.BOOL.encode(buffer, cleared.scrollbackKept());
            }
            case TerminalUpdate.ScreenStart start -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 4);
                ByteBufCodecs.VAR_INT.encode(buffer, start.index());
            }
            case TerminalUpdate.Running running -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 5);
                ByteBufCodecs.BOOL.encode(buffer, running.running());
            }
            case TerminalUpdate.History history -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 6);
                TEXT.encode(buffer, history.line());
            }
            case TerminalUpdate.HistoryCleared cleared -> ByteBufCodecs.VAR_INT.encode(buffer, 7);
            case TerminalUpdate.Ended ended -> {
                ByteBufCodecs.VAR_INT.encode(buffer, 8);
                ByteBufCodecs.VAR_INT.encode(buffer, ended.status());
            }
        }
    }

    private static TerminalUpdate decodeUpdate(ByteBuf buffer) {
        int kind = ByteBufCodecs.VAR_INT.decode(buffer);
        return switch (kind) {
            case 0 -> new TerminalUpdate.Reset(ByteBufCodecs.VAR_INT.decode(buffer), PROMPT.decode(buffer),
                    TEXT.decode(buffer), ByteBufCodecs.BOOL.decode(buffer));
            case 1 -> new TerminalUpdate.Line(LINE.decode(buffer));
            case 2 -> new TerminalUpdate.Current(TEXT.decode(buffer));
            case 3 -> new TerminalUpdate.Cleared(ByteBufCodecs.BOOL.decode(buffer));
            case 4 -> new TerminalUpdate.ScreenStart(ByteBufCodecs.VAR_INT.decode(buffer));
            case 5 -> new TerminalUpdate.Running(ByteBufCodecs.BOOL.decode(buffer));
            case 6 -> new TerminalUpdate.History(TEXT.decode(buffer));
            case 7 -> new TerminalUpdate.HistoryCleared();
            case 8 -> new TerminalUpdate.Ended(ByteBufCodecs.VAR_INT.decode(buffer));
            default -> throw new IllegalArgumentException("Unknown kind of terminal update " + kind);
        };
    }

    /** A list of updates, its size first. */
    static final StreamCodec<ByteBuf, List<TerminalUpdate>> UPDATES = UPDATE.apply(ByteBufCodecs.list());
}
