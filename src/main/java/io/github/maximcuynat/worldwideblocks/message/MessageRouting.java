package io.github.maximcuynat.worldwideblocks.message;

import java.util.Optional;

/**
 * Règle des canaux : le code qui envoie un message dit pourquoi (usage) et à quel point c'est grave
 * (niveau) ; il ne choisit jamais le canal.
 *
 * <ul>
 * <li>geste : barre d'action du joueur, quel que soit le niveau, et une ligne DEBUG ;</li>
 * <li>événement réseau, avertissement ou erreur : chat des joueurs concernés, et une ligne WARN ou ERROR ;</li>
 * <li>événement réseau, information ou succès : log seulement (INFO).</li>
 * </ul>
 */
public final class MessageRouting {

    /**
     * Destination d'un message.
     *
     * @param player canal montré aux joueurs, vide si le message ne va qu'au log
     * @param log    niveau de la ligne écrite dans le log
     */
    public record Route(Optional<MessageChannel> player, LogLevel log) {
    }

    private MessageRouting() {
    }

    public static Route route(MessageUsage usage, MessageLevel level) {
        return switch (usage) {
            case GESTURE -> new Route(Optional.of(MessageChannel.ACTION_BAR), LogLevel.DEBUG);
            case EVENT -> switch (level) {
                case ERROR -> new Route(Optional.of(MessageChannel.CHAT), LogLevel.ERROR);
                case WARNING -> new Route(Optional.of(MessageChannel.CHAT), LogLevel.WARN);
                case INFO, SUCCESS -> new Route(Optional.empty(), LogLevel.INFO);
            };
        };
    }
}
