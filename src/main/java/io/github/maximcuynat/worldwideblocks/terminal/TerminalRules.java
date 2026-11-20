package io.github.maximcuynat.worldwideblocks.terminal;

import java.util.Locale;
import java.util.Optional;

/**
 * What the server accepts from a terminal screen, checked again on every packet ({@code docs/lot-plan.md}, lot 1c-1:
 * "size of a line, rate and session"): the client is never trusted.
 */
public final class TerminalRules {
    /** Characters of a typed line, at most: more than three rows of 80 columns. */
    public static final int MAX_LINE = 256;
    /** Packets a player may send in a burst; one more each tick afterwards (20 per second). */
    public static final int BURST = 20;
    /** Updates of the console in one packet to the client. */
    public static final int UPDATES_PER_PACKET = 64;
    /** Distance beyond the reach of the player at which the keyboard is freed, as for the containers of the game. */
    public static final double REACH_BUFFER = 4.0;

    private TerminalRules() {
    }

    /** Why a typed text is refused, or empty if it is accepted: too long, or holding a control character. */
    public static Optional<String> refusal(String text) {
        if (text.length() > MAX_LINE) {
            return Optional.of(text.length() + " characters, " + MAX_LINE + " at most");
        }
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (Character.isISOControl(codePoint) || Character.getType(codePoint) == Character.LINE_SEPARATOR
                    || Character.getType(codePoint) == Character.PARAGRAPH_SEPARATOR) {
                return Optional.of(String.format(Locale.ROOT, "control character U+%04X", codePoint));
            }
            i += Character.charCount(codePoint);
        }
        return Optional.empty();
    }
}
