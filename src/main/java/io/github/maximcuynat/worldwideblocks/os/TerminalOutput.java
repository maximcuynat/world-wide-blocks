package io.github.maximcuynat.worldwideblocks.os;

/**
 * Where a process writes: the console it runs in. The text is the literal English output of the simulated tool,
 * never a translated text ({@code docs/messages.md}, "Terminal").
 */
@FunctionalInterface
public interface TerminalOutput {
    /** Writes one line, without its line break. */
    void line(String text);
}
