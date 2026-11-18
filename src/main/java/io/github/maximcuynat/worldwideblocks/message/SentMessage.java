package io.github.maximcuynat.worldwideblocks.message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A message actually sent, as received by the listeners ({@link MessageListener}): tests thus
 * check the channel, the key, the values and the recipient without going through a player's
 * chat.
 *
 * @param recipient recipient player, empty for the log
 * @param gameTime  game time of the sending, in ticks (that of the network simulation)
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
