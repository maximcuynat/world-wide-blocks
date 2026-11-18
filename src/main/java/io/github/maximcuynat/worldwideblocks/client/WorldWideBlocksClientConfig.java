package io.github.maximcuynat.worldwideblocks.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client settings, in {@code config/worldwideblocks-client.toml}. Instance held by the client (no static
 * state); NeoForge rereads the file when it changes, without restarting the game.
 */
final class WorldWideBlocksClientConfig {
    private final ModConfigSpec spec;
    private final ModConfigSpec.BooleanValue messageSymbols;

    WorldWideBlocksClientConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        this.messageSymbols = builder
                .comment("Show the level of World Wide Blocks messages as a symbol (true) or as a word (false), for fonts that render the symbols badly.")
                .translation("worldwideblocks.configuration.messageSymbols")
                .define("messageSymbols", true);
        this.spec = builder.build();
    }

    ModConfigSpec spec() {
        return this.spec;
    }

    /** True: symbol before messages; false: translated word ("Warning: ..."). */
    boolean messageSymbols() {
        return this.messageSymbols.getAsBoolean();
    }
}
