package io.github.maximcuynat.worldwideblocks.client;

import io.github.maximcuynat.worldwideblocks.terminal.TerminalLayout;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client settings, in {@code config/worldwideblocks-client.toml}. Instance held by the client (no static
 * state); NeoForge rereads the file when it changes, without restarting the game.
 */
final class WorldWideBlocksClientConfig {
    private final ModConfigSpec spec;
    private final ModConfigSpec.BooleanValue messageSymbols;
    private final ModConfigSpec.IntValue terminalTextSize;
    private final ModConfigSpec.BooleanValue terminalAdvice;

    WorldWideBlocksClientConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        this.messageSymbols = builder
                .comment("Show the level of World Wide Blocks messages as a symbol (true) or as a word (false), for fonts that render the symbols badly.")
                .translation("worldwideblocks.configuration.messageSymbols")
                .define("messageSymbols", true);
        this.terminalTextSize = builder
                .comment("Text size of the terminal of the computers, in screen pixels per pixel of its font, independent of the GUI scale: 0 for the largest that gives at least 80 columns. Also changed in the terminal with Ctrl+= and Ctrl+- (Ctrl+0: automatic).")
                .translation("worldwideblocks.configuration.terminalTextSize")
                .defineInRange("terminalTextSize", 0, 0, TerminalLayout.MAX_SCALE);
        this.terminalAdvice = builder
                .comment("Show the advice of the game in the terminal after a command fails.")
                .translation("worldwideblocks.configuration.terminalAdvice")
                .define("terminalAdvice", true);
        this.spec = builder.build();
    }

    ModConfigSpec spec() {
        return this.spec;
    }

    /** Text size of the terminal chosen by the player, or 0 for the automatic one ({@link TerminalLayout#scale}). */
    int terminalTextSize() {
        return this.terminalTextSize.getAsInt();
    }

    /** Saves a new text size of the terminal in the options file (0: automatic). */
    void setTerminalTextSize(int size) {
        this.terminalTextSize.set(Math.max(0, Math.min(TerminalLayout.MAX_SCALE, size)));
        this.terminalTextSize.save();
    }

    /** True: the advice of the game is shown after a failed command. */
    boolean terminalAdvice() {
        return this.terminalAdvice.getAsBoolean();
    }

    /** True: symbol before messages; false: translated word ("Warning: ..."). */
    boolean messageSymbols() {
        return this.messageSymbols.getAsBoolean();
    }
}
