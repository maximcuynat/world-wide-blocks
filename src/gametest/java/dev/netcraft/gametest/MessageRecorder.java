package dev.netcraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.netcraft.message.MessageChannel;
import dev.netcraft.message.MessageListener;
import dev.netcraft.message.SentMessage;

/**
 * Retient chaque message envoyé par le module des messages pendant un test : canal, niveau, clé,
 * valeurs, destinataire, temps de jeu. Les tests d'un même lot partagent le serveur : chaque test
 * filtre ce qu'il lit par son joueur (ou ses appareils).
 */
public final class MessageRecorder implements MessageListener {
    private final List<SentMessage> messages = new ArrayList<>();

    @Override
    public void onSent(SentMessage message) {
        this.messages.add(message);
    }

    /** Messages reçus par ce joueur sur ce canal, avec cette clé. */
    public List<SentMessage> to(UUID player, MessageChannel channel, String key) {
        return this.messages.stream()
                .filter(message -> message.sentTo(player) && message.channel() == channel && message.key().equals(key))
                .toList();
    }

    /** Tous les messages reçus par ce joueur, pour expliquer un échec. */
    public List<SentMessage> to(UUID player) {
        return this.messages.stream().filter(message -> message.sentTo(player)).toList();
    }
}
