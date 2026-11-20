package io.github.maximcuynat.worldwideblocks.message.server;

import org.slf4j.Logger;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;

/**
 * "Log" channel of the message module, for technical detail: always in English, at the
 * DEBUG level (in development, in {@code run/logs/debug.log}). A bug of the mod caught without stopping the game
 * goes to the ERROR level, with its stack trace. No state: only the mod's logger.
 */
public final class Diagnostics {
    private static final Logger LOGGER = WorldWideBlocks.LOGGER;

    private Diagnostics() {
    }

    /** True if the technical detail is written: to test before computing an expensive description. */
    public static boolean enabled() {
        return LOGGER.isDebugEnabled();
    }

    public static void debug(String format, Object... args) {
        LOGGER.debug(format, args);
    }

    /** A bug of the mod, caught so that the game goes on: the last argument is the exception. */
    public static void bug(String format, Object... args) {
        LOGGER.error(format, args);
    }
}
