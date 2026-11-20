package io.github.maximcuynat.worldwideblocks.os;

/**
 * Where a process writes: the console it runs in. The text is the literal English output of the simulated tool,
 * never a translated text ({@code docs/messages.md}, "Terminal").
 */
@FunctionalInterface
public interface TerminalOutput {
    /** Writes one line, without its line break. */
    void line(String text);

    /**
     * Advice of the game for this failure ({@link ConsoleLine.Advice}): shown after the command ends, only if it ended
     * with a failure (a status other than 0). The last advice given wins. Ignored where there is no console.
     */
    default void advise(String key, String... values) {
    }

    /**
     * A line of the game ({@link ConsoleLine.Game}): a language key and its values, translated by the client and always
     * shown. Ignored where there is no console.
     */
    default void game(String key, String... values) {
    }

    /**
     * Clears the screen, as the escape sequences of {@code clear} do: {@code ESC[H ESC[2J}, then {@code ESC[3J} unless
     * the scrollback is kept. Ignored where there is no console.
     */
    default void clear(boolean keepScrollback) {
    }
}
