package io.github.maximcuynat.worldwideblocks.os;

import java.util.List;
import java.util.Objects;

/**
 * A line of a console. The two kinds are kept strictly apart ({@code docs/messages.md}, "Terminal"): the output of
 * a command, literal English built by the server, and a line of the game, a language key and its values that the
 * client translates and draws apart.
 */
public sealed interface ConsoleLine {

    /** Output of a command or of the shell, as a real Linux writes it. */
    record Output(String text) implements ConsoleLine {
        public Output {
            Objects.requireNonNull(text, "text");
        }
    }

    /**
     * Advice of the game after a command failed: a language key of the family {@code terminal.worldwideblocks.advice.}
     * and its values. The client may hide it (client setting, lot 1c-1).
     */
    record Advice(String key, List<String> values) implements ConsoleLine {
        public Advice {
            Objects.requireNonNull(key, "key");
            values = List.copyOf(values);
        }
    }
}
