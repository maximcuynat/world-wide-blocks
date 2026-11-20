package io.github.maximcuynat.worldwideblocks.client;

import java.util.Locale;

import io.github.maximcuynat.worldwideblocks.message.Message;
import io.github.maximcuynat.worldwideblocks.message.MessageLevel;
import io.github.maximcuynat.worldwideblocks.network.sync.MessagePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The only place that decides how a message looks: the server only sends the channel, the level,
 * the key and the values. Replacing this drawing (future HUD) touches neither the server nor the
 * texts.
 *
 * <p>The level is always read through a symbol or a word, never through color alone. Display goes
 * through the game's {@code ChatListener}, which keeps the player's narration and chat settings.
 */
final class ClientMessages {
    private final WorldWideBlocksClientConfig config;

    ClientMessages(WorldWideBlocksClientConfig config) {
        this.config = config;
    }

    void show(MessagePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Component text = this.render(payload.message());
        switch (payload.channel()) {
            case ACTION_BAR -> minecraft.gui.chatListener().handleOverlay(text);
            case CHAT -> minecraft.gui.chatListener().handleSystemMessage(text, true);
            case LOG -> {
                // Never sent to the client: the log stays on the server.
            }
        }
    }

    private Component render(Message message) {
        Component body = ClientText.of(message.key(), message.args());
        MessageLevel level = message.level();
        return withLevel(this.config.messageSymbols(), level, body).withStyle(color(level));
    }

    /** A text with its level in front: its symbol, or its translated word (client setting {@code messageSymbols}). */
    static MutableComponent withLevel(boolean symbols, MessageLevel level, Component body) {
        return symbols
                ? Component.literal(symbol(level) + " ").append(body)
                : Component.translatable("message.worldwideblocks.with_level",
                        Component.translatable("message.worldwideblocks.level." + level.name().toLowerCase(Locale.ROOT)), body);
    }

    private static String symbol(MessageLevel level) {
        return switch (level) {
            case INFO -> "ℹ";
            case SUCCESS -> "✔";
            case WARNING -> "⚠";
            case ERROR -> "✖";
        };
    }

    private static ChatFormatting color(MessageLevel level) {
        return switch (level) {
            case INFO -> ChatFormatting.WHITE;
            case SUCCESS -> ChatFormatting.GREEN;
            case WARNING -> ChatFormatting.YELLOW;
            case ERROR -> ChatFormatting.RED;
        };
    }
}
