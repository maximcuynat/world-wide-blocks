package dev.netcraft.network.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.netcraft.network.ChunkKey;
import dev.netcraft.network.DeviceId;
import dev.netcraft.network.Link;
import dev.netcraft.network.NetworkDevice;
import dev.netcraft.network.NetworkDevices;
import dev.netcraft.network.NetworkManager;
import dev.netcraft.network.protocol.MacAddress;
import dev.netcraft.network.sim.DeviceLogic;
import dev.netcraft.network.switching.MacTable;
import dev.netcraft.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Envoie aux clients ce qu'ils affichent : les câbles qu'ils peuvent voir, et l'état de l'appareil
 * que le joueur vise. Un joueur ne reçoit un câble que si une de ses extrémités se trouve dans un chunk
 * que son client a reçu (voir {@link PlayerLinkView}), et l'état d'un appareil que s'il le vise à portée
 * de main. Le serveur reste seul juge : le client ne reçoit qu'une copie et ne renvoie rien.
 */
public final class NetworkSync {
    private static final String PROTOCOL_VERSION = "2";
    /** L'appareil visé par chaque joueur est relevé tous les 5 ticks (un quart de seconde). */
    private static final int STATUS_INTERVAL_TICKS = 5;

    private NetworkSync() {
    }

    /** Bus du mod : déclare les paquets. Leurs réceptions sont enregistrées côté client seulement. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToClient(LinksResetPayload.TYPE, LinksResetPayload.STREAM_CODEC)
                .playToClient(LinkAddedPayload.TYPE, LinkAddedPayload.STREAM_CODEC)
                .playToClient(LinkRemovedPayload.TYPE, LinkRemovedPayload.STREAM_CODEC)
                .playToClient(DeviceStatusPayload.TYPE, DeviceStatusPayload.STREAM_CODEC);
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
            player.getData(ModAttachments.PLAYER_STATUS_VIEW).reset();
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

    // ---- État de l'appareil visé ----

    /** Bus de NeoForge : envoie à chaque joueur l'état de l'appareil qu'il vise, s'il a changé. */
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % STATUS_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DeviceStatusPayload status = statusAimedBy(player);
            if (player.getData(ModAttachments.PLAYER_STATUS_VIEW).update(status)) {
                PacketDistributor.sendToPlayer(player, status);
            }
        }
    }

    /** État de l'appareil réseau que le joueur vise à portée de main, ou {@code null}. */
    private static DeviceStatusPayload statusAimedBy(ServerPlayer player) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        ServerLevel level = player.level();
        BlockPos pos = blockHit.getBlockPos();
        if (!level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof NetworkDevice)) {
            return null;
        }
        NetworkManager network = NetworkManager.of(level.getServer());
        DeviceId id = NetworkDevices.idOf(level, pos);
        List<MacAddress> macs = network.macsOf(id);
        if (macs.isEmpty()) {
            return null; // appareil pas encore connu du moteur
        }
        List<Long> portMacs = new ArrayList<>(macs.size());
        macs.forEach(mac -> portMacs.add(mac.value()));
        boolean switching = network.logic(id).map(DeviceLogic::bridges).orElse(false);
        List<LearnedMac> learned = new ArrayList<>();
        for (MacTable.Entry entry : network.logic(id).map(DeviceLogic::learnedMacs).orElse(List.of())) {
            learned.add(new LearnedMac(entry.port(), entry.mac().value()));
        }
        return new DeviceStatusPayload(pos, switching, portMacs, learned);
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
