package io.github.maximcuynat.worldwideblocks.message;

import java.util.Optional;

/**
 * Channel rule: the code that sends a message says why (use) and how serious it is
 * (level); it never chooses the channel.
 *
 * <ul>
 * <li>gesture: the player's action bar, whatever the level, and a DEBUG line;</li>
 * <li>network event, warning or error: chat of the players concerned, and a WARN or ERROR line;</li>
 * <li>network event, info or success: log only (INFO).</li>
 * </ul>
 */
public final class MessageRouting {

    /**
     * Destination of a message.
     *
     * @param player channel shown to players, empty if the message only goes to the log
     * @param log    level of the line written in the log
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
