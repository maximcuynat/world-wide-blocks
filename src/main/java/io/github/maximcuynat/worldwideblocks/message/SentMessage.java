package io.github.maximcuynat.worldwideblocks.message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Un message effectivement envoyé, tel que le reçoivent les écouteurs ({@link MessageListener}) : les
 * tests vérifient ainsi le canal, la clé, les valeurs et le destinataire sans passer par le chat d'un
 * joueur.
 *
 * @param recipient joueur destinataire, vide pour le log
 * @param gameTime  temps de jeu de l'envoi, en ticks (celui de la simulation réseau)
 */
public record SentMessage(MessageChannel channel, MessageLevel level, String key, List<MessageArg> args,
        Optional<UUID> recipient, long gameTime) {

    public SentMessage {
        args = List.copyOf(args);
    }

    public boolean sentTo(UUID player) {
        return this.recipient.filter(player::equals).isPresent();
    }
}
