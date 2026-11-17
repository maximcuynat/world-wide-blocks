package dev.netcraft.message.server;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

import dev.netcraft.NetCraft;
import dev.netcraft.message.LogLevel;
import dev.netcraft.message.Message;
import dev.netcraft.message.MessageChannel;
import dev.netcraft.message.MessageHub;
import dev.netcraft.message.MessageListener;
import dev.netcraft.message.MessageRouting;
import dev.netcraft.message.MessageUsage;
import dev.netcraft.message.SentMessage;
import dev.netcraft.network.NetworkSavedData;
import dev.netcraft.network.sync.MessagePayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Point d'entrée unique des messages côté serveur. L'appelant dit pourquoi il parle (geste, événement)
 * et donne un niveau, une clé et des valeurs ; le centre choisit le canal ({@link MessageRouting}),
 * envoie au client un {@link MessagePayload} sans aucun texte rédigé, écrit le log en anglais et
 * prévient les écouteurs.
 *
 * <p>Aucun état statique : façade légère sur l'état porté par les données du serveur en cours.
 */
public final class MessageCenter {
    private static final Logger LOGGER = NetCraft.LOGGER;

    private final MinecraftServer server;
    private final MessageHub hub;

    private MessageCenter(MinecraftServer server, MessageHub hub) {
        this.server = server;
        this.hub = hub;
    }

    public static MessageCenter of(MinecraftServer server) {
        return new MessageCenter(server, server.getDataStorage().computeIfAbsent(NetworkSavedData.TYPE).messageHub());
    }

    /** Abonne un écouteur à tous les messages envoyés (tests) ; fermer l'abonnement le retire. */
    public MessageHub.Subscription listen(MessageListener listener) {
        return this.hub.listen(listener);
    }

    /** Réponse immédiate au geste d'un joueur. */
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

    /** Temps de jeu de l'Overworld, le même que celui de la simulation réseau. */
    private long gameTime() {
        return this.server.overworld().getGameTime();
    }

    /** Écrit une ligne en anglais ; un {@link Message} n'est mis en texte que si la ligne est écrite. */
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
