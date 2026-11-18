package io.github.maximcuynat.worldwideblocks.message.server;

import org.slf4j.Logger;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;

/**
 * "Log" channel of the message module, for technical detail: always in English, at the
 * DEBUG level (in development, in {@code run/logs/debug.log}). No state: only the mod's logger.
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
}
