package io.github.maximcuynat.worldwideblocks.message;

/**
 * Severity of a message. The server only sends the level: the client derives the look from it
 * (symbol or word, color), always with a symbol or a word, never color alone.
 */
public enum MessageLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR;

    /** Warning or error: a problem the player must know about. */
    public boolean isProblem() {
        return this == WARNING || this == ERROR;
    }
}
