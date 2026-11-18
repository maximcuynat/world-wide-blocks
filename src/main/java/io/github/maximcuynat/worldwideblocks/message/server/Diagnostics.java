package io.github.maximcuynat.worldwideblocks.message.server;

import org.slf4j.Logger;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;

/**
 * Canal « log » du module des messages, pour le détail technique : toujours en anglais, au niveau
 * DEBUG (en développement, dans {@code run/logs/debug.log}). Aucun état : seulement le logger du mod.
 */
public final class Diagnostics {
    private static final Logger LOGGER = WorldWideBlocks.LOGGER;

    private Diagnostics() {
    }

    /** Vrai si le détail technique est écrit : à tester avant de calculer une description coûteuse. */
    public static boolean enabled() {
        return LOGGER.isDebugEnabled();
    }

    public static void debug(String format, Object... args) {
        LOGGER.debug(format, args);
    }
}
