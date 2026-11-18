package io.github.maximcuynat.worldwideblocks.message.server;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.message.LogLevel;
import io.github.maximcuynat.worldwideblocks.message.Message;
import io.github.maximcuynat.worldwideblocks.message.MessageChannel;
import io.github.maximcuynat.worldwideblocks.message.MessageHub;
import io.github.maximcuynat.worldwideblocks.message.MessageListener;
import io.github.maximcuynat.worldwideblocks.message.MessageRouting;
import io.github.maximcuynat.worldwideblocks.message.MessageUsage;
import io.github.maximcuynat.worldwideblocks.message.SentMessage;
import io.github.maximcuynat.worldwideblocks.network.NetworkSavedData;
import io.github.maximcuynat.worldwideblocks.network.sync.MessagePayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Single entry point of messages on the server side. The caller says why it speaks (gesture, event)
 * and gives a level, a key and values; the center chooses the channel ({@link MessageRouting}),
 * sends the client a {@link MessagePayload} without any written text, writes the log in English and
 * notifies the listeners.
 *
 * <p>No static state: a light facade over the state held by the running server's data.
 */
public final class MessageCenter {
    private static final Logger LOGGER = WorldWideBlocks.LOGGER;

    private final MinecraftServer server;
    private final MessageHub hub;

    private MessageCenter(MinecraftServer server, MessageHub hub) {
        this.server = server;
        this.hub = hub;
    }

    public static MessageCenter of(MinecraftServer server) {
        return new MessageCenter(server, server.getDataStorage().computeIfAbsent(NetworkSavedData.TYPE).messageHub());
    }

    /** Subscribes a listener to all sent messages (tests); closing the subscription removes it. */
    public MessageHub.Subscription listen(MessageListener listener) {
        return this.hub.listen(listener);
    }

    /**
     * Network event, without a player gesture. The same problem ({@code problemKey}) is only passed on
     * once per game minute, whatever the number of devices that see it. Warning
     * or error: each recipient gets their message in the chat; in all cases, a line in
     * English in the log.
     *
     * @param summary    the message, as kept for the log and the listeners
     * @param logText    the log line, in English, with all the technical detail
     * @param recipients message of each player concerned (it may differ from one player to another)
     * @return false if the problem has already been reported within the minute, and nothing was sent
     */
    public boolean event(String problemKey, Message summary, String logText, Map<ServerPlayer, Message> recipients) {
        long now = this.gameTime();
        if (!this.hub.throttle().allow(problemKey, now)) {
            return false;
        }
        MessageRouting.Route route = MessageRouting.route(MessageUsage.EVENT, summary.level());
        route.player().ifPresent(channel -> recipients.forEach((player, message) -> {
            PacketDistributor.sendToPlayer(player, new MessagePayload(channel, message));
            this.publish(channel, message, Optional.of(player.getUUID()), now);
        }));
        log(route.log(), "{}", logText);
        this.publish(MessageChannel.LOG, summary, Optional.empty(), now);
        return true;
    }

    /** Forgets an already reported problem (its network changed): it can be reported again right away. */
    public void forgetProblem(String problemKey) {
        this.hub.throttle().forget(problemKey);
    }

    /** Immediate answer to a player's gesture. */
    public void gesture(ServerPlayer player, Message message) {
        this.deliver(player, MessageUsage.GESTURE, message);
    }

    private void deliver(ServerPlayer player, MessageUsage usage, Message message) {
        MessageRouting.Route route = MessageRouting.route(usage, message.level());
        long now = this.gameTime();
        route.player().ifPresent(channel -> {
            PacketDistributor.sendToPlayer(player, new MessagePayload(channel, message));
            this.publish(channel, message, Optional.of(player.getUUID()), now);
        });
        String channel = route.player().map(c -> c.name().toLowerCase(Locale.ROOT)).orElse("log only");
        log(route.log(), "Message to {} ({}): {}", player.getName().getString(), channel, message);
        this.publish(MessageChannel.LOG, message, Optional.empty(), now);
    }

    private void publish(MessageChannel channel, Message message, Optional<UUID> recipient, long gameTime) {
        this.hub.publish(new SentMessage(channel, message.level(), message.key(), message.args(), recipient, gameTime));
    }

    /** Overworld game time, the same as that of the network simulation. */
    private long gameTime() {
        return this.server.overworld().getGameTime();
    }

    /** Writes a line in English; a {@link Message} is only turned into text if the line is written. */
    private static void log(LogLevel level, String format, Object... args) {
        boolean enabled = switch (level) {
            case DEBUG -> LOGGER.isDebugEnabled();
            case INFO -> LOGGER.isInfoEnabled();
            case WARN -> LOGGER.isWarnEnabled();
            case ERROR -> LOGGER.isErrorEnabled();
        };
        if (!enabled) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Message message) {
                args[i] = message.logText();
            }
        }
        switch (level) {
            case DEBUG -> LOGGER.debug(format, args);
            case INFO -> LOGGER.info(format, args);
            case WARN -> LOGGER.warn(format, args);
            case ERROR -> LOGGER.error(format, args);
        }
    }
}
