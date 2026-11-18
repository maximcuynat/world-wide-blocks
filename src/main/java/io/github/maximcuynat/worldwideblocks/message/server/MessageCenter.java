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
 * Point d'entrée unique des messages côté serveur. L'appelant dit pourquoi il parle (geste, événement)
 * et donne un niveau, une clé et des valeurs ; le centre choisit le canal ({@link MessageRouting}),
 * envoie au client un {@link MessagePayload} sans aucun texte rédigé, écrit le log en anglais et
 * prévient les écouteurs.
 *
 * <p>Aucun état statique : façade légère sur l'état porté par les données du serveur en cours.
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

    /** Abonne un écouteur à tous les messages envoyés (tests) ; fermer l'abonnement le retire. */
    public MessageHub.Subscription listen(MessageListener listener) {
        return this.hub.listen(listener);
    }

    /**
     * Événement du réseau, sans geste du joueur. Un même problème ({@code problemKey}) n'est transmis
     * qu'une fois par minute de jeu, quel que soit le nombre d'appareils qui le voient. Avertissement
     * ou erreur : chaque destinataire reçoit son message dans le chat ; dans tous les cas, une ligne en
     * anglais dans le log.
     *
     * @param summary    le message, tel qu'il est retenu pour le log et les écouteurs
     * @param logText    la ligne du log, en anglais, avec tout le détail technique
     * @param recipients message de chaque joueur concerné (il peut différer d'un joueur à l'autre)
     * @return faux si le problème a déjà été signalé dans la minute, et rien n'a été envoyé
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

    /** Oublie un problème déjà signalé (son réseau a changé) : il pourra l'être de nouveau tout de suite. */
    public void forgetProblem(String problemKey) {
        this.hub.throttle().forget(problemKey);
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
