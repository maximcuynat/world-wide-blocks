package io.github.maximcuynat.worldwideblocks.os;

import java.util.List;
import java.util.Objects;

import io.github.maximcuynat.worldwideblocks.os.shell.Prompt;

/**
 * A line of a console. The kinds are kept strictly apart ({@code docs/messages.md}, "Terminal"): the output of a
 * command, literal English built by the server; a prompt and what was typed after it, which the client draws with
 * the colours of the prompt; and the lines of the game, a language key and its values that the client translates
 * and draws apart.
 */
public sealed interface ConsoleLine {

    /** Output of a command or of the shell, as a real Linux writes it. */
    record Output(String text) implements ConsoleLine {
        public Output {
            Objects.requireNonNull(text, "text");
        }
    }

    /**
     * A prompt and the text after it on the same line: the line typed then Enter, or what was typed before Ctrl+C
     * ({@code hostn^C}) or Ctrl+D.
     */
    record Input(Prompt prompt, String text) implements ConsoleLine {
        public Input {
            Objects.requireNonNull(prompt, "prompt");
            Objects.requireNonNull(text, "text");
        }
    }

    /**
     * A line of the game that a command writes, translated by the client and always shown, such as the lines of
     * {@code help}: a language key of the family {@code terminal.worldwideblocks.help.} and its values.
     */
    record Game(String key, List<String> values) implements ConsoleLine {
        public Game {
            Objects.requireNonNull(key, "key");
            values = List.copyOf(values);
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
