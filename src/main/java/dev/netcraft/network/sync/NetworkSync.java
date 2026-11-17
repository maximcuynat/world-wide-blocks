package dev.netcraft.network.sync;

import java.util.List;
import java.util.UUID;

import dev.netcraft.network.ChunkKey;
import dev.netcraft.network.Link;
import dev.netcraft.network.NetworkDevices;
import dev.netcraft.network.NetworkManager;
import dev.netcraft.registry.ModAttachments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Envoie aux clients les câbles qu'ils peuvent voir, pour l'affichage. Un joueur ne reçoit un câble
 * que si une de ses extrémités se trouve dans un chunk que son client a reçu (voir {@link PlayerLinkView}).
 * Le serveur reste seul juge : le client ne reçoit qu'une copie et ne renvoie rien.
 */
public final class NetworkSync {
    private static final String PROTOCOL_VERSION = "1";

    private NetworkSync() {
    }

    /** Bus du mod : déclare les paquets. Leurs réceptions sont enregistrées côté client seulement. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToClient(LinksResetPayload.TYPE, LinksResetPayload.STREAM_CODEC)
                .playToClient(LinkAddedPayload.TYPE, LinkAddedPayload.STREAM_CODEC)
                .playToClient(LinkRemovedPayload.TYPE, LinkRemovedPayload.STREAM_CODEC);
    }

    // ---- Suivi des chunks par joueur ----

    /** Le client vient de recevoir un chunk : il peut désormais connaître les câbles qui y aboutissent. */
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        ServerPlayer player = event.getPlayer();
        ServerLevel level = event.getLevel();
        PlayerLinkView view = player.getData(ModAttachments.PLAYER_LINK_VIEW);
        String dimension = dimensionOf(level);
        if (view.enterDimension(dimension)) {
            // Connexion, réapparition ou changement de dimension : le client repart de zéro.
            PacketDistributor.sendToPlayer(player, LinksResetPayload.INSTANCE);
        }
        ChunkKey chunk = new ChunkKey(dimension, event.getPos().x(), event.getPos().z());
        for (Link link : view.chunkSent(chunk, NetworkManager.of(level.getServer()).linksTouching(chunk))) {
            PacketDistributor.sendToPlayer(player, new LinkAddedPayload(ClientLink.of(link)));
        }
    }

    /** Le joueur ne suit plus un chunk : il oublie les câbles dont l'autre extrémité n'est pas suivie non plus. */
    public static void onChunkUnWatch(ChunkWatchEvent.UnWatch event) {
        ServerPlayer player = event.getPlayer();
        ServerLevel level = event.getLevel();
        PlayerLinkView view = player.getData(ModAttachments.PLAYER_LINK_VIEW);
        ChunkKey chunk = new ChunkKey(dimensionOf(level), event.getPos().x(), event.getPos().z());
        for (UUID id : view.chunkUnwatched(chunk, NetworkManager.of(level.getServer()).linksTouching(chunk))) {
            PacketDistributor.sendToPlayer(player, new LinkRemovedPayload(id));
        }
    }

    // ---- Changements du réseau ----

    public static void linkAdded(MinecraftServer server, Link link) {
        ServerLevel level = levelOf(server, link);
        if (level == null) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.getData(ModAttachments.PLAYER_LINK_VIEW).linkAdded(link)) {
                PacketDistributor.sendToPlayer(player, new LinkAddedPayload(ClientLink.of(link)));
            }
        }
    }

    public static void linksRemoved(MinecraftServer server, List<Link> links) {
        for (Link link : links) {
            ServerLevel level = levelOf(server, link);
            if (level == null) {
                continue;
            }
            for (ServerPlayer player : level.players()) {
                if (player.getData(ModAttachments.PLAYER_LINK_VIEW).linkRemoved(link.id())) {
                    PacketDistributor.sendToPlayer(player, new LinkRemovedPayload(link.id()));
                }
            }
        }
    }

    private static String dimensionOf(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    /** Niveau où se trouve le câble ; ses deux extrémités sont toujours dans la même dimension. */
    private static ServerLevel levelOf(MinecraftServer server, Link link) {
        return NetworkDevices.levelOf(server, link.a().device()).orElse(null);
    }
}
