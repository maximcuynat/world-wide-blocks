package dev.netcraft.network.sync;

import java.util.List;

import dev.netcraft.network.Link;
import dev.netcraft.network.NetworkManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Envoie aux clients les câbles de leur dimension, pour l'affichage. Le serveur reste seul juge :
 * le client ne reçoit qu'une copie et ne renvoie rien.
 *
 * <p>Limite assumée au lot 1a : chaque joueur reçoit tous les câbles de sa dimension. Il faudra
 * filtrer par distance quand les réseaux seront grands.
 */
public final class NetworkSync {
    private static final String PROTOCOL_VERSION = "1";

    private NetworkSync() {
    }

    /** Bus du mod : déclare les paquets. Leurs réceptions sont enregistrées côté client seulement. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToClient(LinksSyncPayload.TYPE, LinksSyncPayload.STREAM_CODEC)
                .playToClient(LinkAddedPayload.TYPE, LinkAddedPayload.STREAM_CODEC)
                .playToClient(LinkRemovedPayload.TYPE, LinkRemovedPayload.STREAM_CODEC);
    }

    // ---- Envoi de la liste complète ----

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendAll(player);
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendAll(player);
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendAll(player);
        }
    }

    private static void sendAll(ServerPlayer player) {
        ServerLevel level = player.level();
        String dimension = level.dimension().identifier().toString();
        List<ClientLink> links = NetworkManager.of(level.getServer()).links().stream()
                .filter(link -> link.a().device().dimension().equals(dimension))
                .map(ClientLink::of)
                .toList();
        PacketDistributor.sendToPlayer(player, new LinksSyncPayload(links));
    }

    // ---- Envoi des changements ----

    public static void linkAdded(MinecraftServer server, Link link) {
        ServerLevel level = levelOf(server, link);
        if (level != null) {
            PacketDistributor.sendToPlayersInDimension(level, new LinkAddedPayload(ClientLink.of(link)));
        }
    }

    public static void linksRemoved(MinecraftServer server, List<Link> links) {
        for (Link link : links) {
            ServerLevel level = levelOf(server, link);
            if (level != null) {
                PacketDistributor.sendToPlayersInDimension(level, new LinkRemovedPayload(link.id()));
            }
        }
    }

    /** Niveau où se trouve le câble ; ses deux extrémités sont toujours dans la même dimension. */
    private static ServerLevel levelOf(MinecraftServer server, Link link) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(link.a().device().dimension())));
    }
}
