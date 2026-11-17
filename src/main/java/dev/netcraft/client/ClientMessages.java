package dev.netcraft.client;

import java.util.Locale;

import dev.netcraft.message.Message;
import dev.netcraft.message.MessageLevel;
import dev.netcraft.network.sync.MessagePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Seul endroit qui décide de l'apparence d'un message : le serveur n'envoie que le canal, le niveau,
 * la clé et les valeurs. Remplacer ce dessin (future interface HUD) ne touche ni au serveur ni aux
 * textes.
 *
 * <p>Le niveau se lit toujours par un symbole ou un mot, jamais par la couleur seule. L'affichage passe
 * par le {@code ChatListener} du jeu, qui garde la narration et les réglages de chat du joueur.
 */
final class ClientMessages {
    private final NetCraftClientConfig config;

    ClientMessages(NetCraftClientConfig config) {
        this.config = config;
    }

    void show(MessagePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Component text = this.render(payload.message());
        switch (payload.channel()) {
            case ACTION_BAR -> minecraft.gui.chatListener().handleOverlay(text);
            case CHAT -> minecraft.gui.chatListener().handleSystemMessage(text, true);
            case LOG -> {
                // Jamais envoyé au client : le log reste sur le serveur.
            }
        }
    }

    private Component render(Message message) {
        Component body = ClientText.of(message.key(), message.args());
        MessageLevel level = message.level();
        MutableComponent line = this.config.messageSymbols()
                ? Component.literal(symbol(level) + " ").append(body)
                : Component.translatable("message.netcraft.with_level",
                        Component.translatable("message.netcraft.level." + level.name().toLowerCase(Locale.ROOT)), body);
        return line.withStyle(color(level));
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
