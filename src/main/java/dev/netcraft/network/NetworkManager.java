package dev.netcraft.network;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import dev.netcraft.NetCraft;
import dev.netcraft.network.protocol.MacAddress;
import dev.netcraft.network.sim.DeviceLogic;
import dev.netcraft.network.sim.NetworkSimulation;
import dev.netcraft.network.sim.SimulationEvent;
import dev.netcraft.network.sync.NetworkSync;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Point d'entrée unique du moteur réseau côté serveur. Toute modification du réseau passe par ici,
 * pour que la sauvegarde soit marquée à jour, que la simulation et les clients soient prévenus.
 *
 * <p>Aucun état statique : l'objet est une façade légère sur les données du serveur en cours,
 * obtenue par {@link #of(MinecraftServer)}. Les données disparaissent avec le serveur, si bien
 * qu'en solo deux mondes ne se mélangent jamais.
 */
public final class NetworkManager {
    /** Les joueurs à moins de 32 blocs d'un appareil en boucle sont prévenus. */
    private static final double LOOP_MESSAGE_RADIUS = 32.0;

    private final MinecraftServer server;
    private final NetworkSavedData data;

    /** Bus de NeoForge : fait avancer la simulation à chaque tick du serveur. */
    public static void onServerTick(ServerTickEvent.Post event) {
        of(event.getServer()).tick();
    }

    private NetworkManager(MinecraftServer server, NetworkSavedData data) {
        this.server = server;
        this.data = data;
    }

    public static NetworkManager of(MinecraftServer server) {
        return new NetworkManager(server, server.getDataStorage().computeIfAbsent(NetworkSavedData.TYPE));
    }

    // ---- Lecture ----

    public Optional<NetworkNode> node(DeviceId id) {
        return this.graph().node(id);
    }

    public Collection<Link> links() {
        return this.graph().links();
    }

    public Optional<Link> linkAt(PortRef port) {
        return this.graph().linkAt(port);
    }

    public List<Link> linksOf(DeviceId device) {
        return this.graph().linksOf(device);
    }

    public Collection<Link> linksTouching(ChunkKey chunk) {
        return this.graph().linksTouching(chunk);
    }

    public Optional<PortRef> firstFreePort(DeviceId device, LinkMedium medium) {
        return this.graph().firstFreePort(device, medium);
    }

    /** Un branchement en attente depuis {@code start} peut-il encore être terminé par qui se trouve dans {@code holderDimension} ? */
    public PendingCable.Status pendingCable(PortRef start, String holderDimension) {
        return PendingCable.check(this.graph(), start, holderDimension);
    }

    public List<MacAddress> macsOf(DeviceId device) {
        return this.graph().macsOf(device);
    }

    /** Comportement de l'appareil dans la simulation (indicateur) ; vide avant le premier tick. */
    public Optional<DeviceLogic> logic(DeviceId device) {
        return this.simulation().logic(device);
    }

    // ---- Modification ----

    /** Enregistre ou met à jour un appareil ; renvoie les câbles devenus invalides et retirés. */
    public List<Link> registerDevice(NetworkNode node) {
        if (this.graph().node(node.id()).filter(node::equals).isPresent()) {
            return List.of();
        }
        List<Link> removed = this.graph().putNode(node);
        this.data.setDirty();
        removed.forEach(this.simulation()::linkDown);
        this.simulation().deviceAdded(node);
        NetworkSync.linksRemoved(this.server, removed);
        return removed;
    }

    public ConnectOutcome connect(PortRef a, PortRef b, LinkMedium medium, boolean cableConsumed) {
        ConnectOutcome outcome = this.graph().connect(a, b, medium, cableConsumed);
        outcome.link().ifPresent(link -> {
            this.data.setDirty();
            this.simulation().linkUp(link);
            NetworkSync.linkAdded(this.server, link);
        });
        return outcome;
    }

    public Optional<Link> disconnect(PortRef port) {
        Optional<Link> removed = this.graph().disconnect(port);
        removed.ifPresent(link -> {
            this.data.setDirty();
            this.simulation().linkDown(link);
            NetworkSync.linksRemoved(this.server, List.of(link));
        });
        return removed;
    }

    /** Retire l'appareil et ses câbles ; renvoie les câbles retirés. */
    public List<Link> removeDevice(DeviceId device) {
        boolean known = this.graph().node(device).isPresent();
        List<Link> removed = this.graph().removeDevice(device);
        if (known || !removed.isEmpty()) {
            this.data.setDirty();
        }
        removed.forEach(this.simulation()::linkDown);
        this.simulation().deviceRemoved(device);
        NetworkSync.linksRemoved(this.server, removed);
        return removed;
    }

    // ---- Simulation ----

    /** Un tick du serveur : la simulation avance au temps de jeu, puis ses signalements deviennent des messages. */
    private void tick() {
        // Temps de jeu de l'Overworld : sauvegardé avec le monde, il ne recule jamais.
        this.simulation().tick(this.server.overworld().getGameTime());
        for (SimulationEvent event : this.simulation().drainEvents()) {
            switch (event) {
                case SimulationEvent.LoopDetected loop -> this.reportLoop(loop);
                case SimulationEvent.QueueSaturated saturated -> NetCraft.LOGGER.warn(
                        "Réseau saturé : plus de {} trames en attente, de nouvelles trames ont été perdues.",
                        this.simulation().limits().maxQueued());
            }
        }
    }

    /** Message clair dans le log et aux joueurs proches, plutôt qu'une tempête de trames. */
    private void reportLoop(SimulationEvent.LoopDetected loop) {
        DeviceId id = loop.device();
        Component name = this.graph().node(id).map(NetworkDevices::displayName).orElse(Component.literal("?"));
        int port = loop.port() + 1;
        NetCraft.LOGGER.warn("Boucle réseau : une trame diffusée revient à {} ({}) par le port {}. Copie supprimée.",
                name.getString(), id, port);
        Optional<ServerLevel> level = NetworkDevices.levelOf(this.server, id);
        if (level.isEmpty()) {
            return;
        }
        BlockPos pos = NetworkDevices.posOf(id);
        Component message = Component.translatable("message.netcraft.network.loop", name, id.x(), id.y(), id.z(), port);
        for (ServerPlayer player : level.get().players()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= LOOP_MESSAGE_RADIUS * LOOP_MESSAGE_RADIUS) {
                player.sendSystemMessage(message);
            }
        }
    }

    private NetworkGraph graph() {
        return this.data.graph();
    }

    private NetworkSimulation simulation() {
        return this.data.simulation();
    }
}
