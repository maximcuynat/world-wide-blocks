package io.github.maximcuynat.worldwideblocks.network;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.maximcuynat.worldwideblocks.message.Message;
import io.github.maximcuynat.worldwideblocks.message.MessageArg;
import io.github.maximcuynat.worldwideblocks.message.MessageLevel;
import io.github.maximcuynat.worldwideblocks.message.server.Diagnostics;
import io.github.maximcuynat.worldwideblocks.message.server.MessageCenter;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.sim.DeviceLogic;
import io.github.maximcuynat.worldwideblocks.network.sim.NetworkSimulation;
import io.github.maximcuynat.worldwideblocks.network.sim.SimulationEvent;
import io.github.maximcuynat.worldwideblocks.network.sync.NetworkSync;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
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
    /** Les joueurs à moins de 32 blocs d'un appareil qui voit la boucle sont prévenus. */
    private static final double LOOP_MESSAGE_RADIUS = 32.0;
    private static final String LOOP_MESSAGE = "message.worldwideblocks.network.loop";
    private static final String SATURATED_MESSAGE = "message.worldwideblocks.network.saturated";
    private static final String SATURATED_PROBLEM = "saturated";

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
        Set<DeviceId> touched = this.withNeighbours(node.id());
        Set<DeviceId> networks = this.networksOf(touched);
        List<Link> removed = this.graph().putNode(node);
        networks.addAll(this.networksOf(touched));
        this.forgetLoops(networks);
        if (Diagnostics.enabled()) {
            Diagnostics.debug("Device registered: {}, ports: {}", NetworkDescriptions.device(this.graph(), node.id()),
                    NetworkDescriptions.ports(this.graph(), node));
        }
        this.data.setDirty();
        removed.forEach(this.simulation()::linkDown);
        this.simulation().deviceAdded(node);
        NetworkSync.linksRemoved(this.server, removed);
        return removed;
    }

    public ConnectOutcome connect(PortRef a, PortRef b, LinkMedium medium, boolean cableConsumed) {
        Set<DeviceId> touched = Set.copyOf(List.of(a.device(), b.device())); // Set.of refuse les doublons (même appareil)
        Set<DeviceId> networks = this.networksOf(touched);
        ConnectOutcome outcome = this.graph().connect(a, b, medium, cableConsumed);
        outcome.link().ifPresent(link -> {
            networks.addAll(this.networksOf(touched));
            this.forgetLoops(networks);
            this.data.setDirty();
            this.simulation().linkUp(link);
            if (Diagnostics.enabled()) {
                Diagnostics.debug("Cable connected: {} <-> {}", NetworkDescriptions.port(this.graph(), link.a()),
                        NetworkDescriptions.port(this.graph(), link.b()));
            }
            NetworkSync.linkAdded(this.server, link);
        });
        return outcome;
    }

    public Optional<Link> disconnect(PortRef port) {
        Optional<Link> present = this.graph().linkAt(port);
        if (present.isEmpty()) {
            return Optional.empty();
        }
        Set<DeviceId> touched = Set.copyOf(List.of(present.get().a().device(), present.get().b().device()));
        Set<DeviceId> networks = this.networksOf(touched);
        Optional<Link> removed = this.graph().disconnect(port);
        removed.ifPresent(link -> {
            networks.addAll(this.networksOf(touched));
            this.forgetLoops(networks);
            this.data.setDirty();
            this.simulation().linkDown(link);
            if (Diagnostics.enabled()) {
                Diagnostics.debug("Cable disconnected: {} <-> {}", NetworkDescriptions.port(this.graph(), link.a()),
                        NetworkDescriptions.port(this.graph(), link.b()));
            }
            NetworkSync.linksRemoved(this.server, List.of(link));
        });
        return removed;
    }

    /** Retire l'appareil et ses câbles ; renvoie les câbles retirés. */
    public List<Link> removeDevice(DeviceId device) {
        boolean known = this.graph().node(device).isPresent();
        if (known && Diagnostics.enabled()) {
            Diagnostics.debug("Device removed: {}, with {} cable(s)", NetworkDescriptions.device(this.graph(), device),
                    this.graph().linksOf(device).size());
        }
        Set<DeviceId> touched = this.withNeighbours(device);
        Set<DeviceId> networks = this.networksOf(touched);
        List<Link> removed = this.graph().removeDevice(device);
        if (known || !removed.isEmpty()) {
            networks.addAll(this.networksOf(touched));
            this.forgetLoops(networks);
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
        long gameTime = this.server.overworld().getGameTime();
        this.simulation().tick(gameTime);
        for (SimulationEvent event : this.simulation().drainEvents()) {
            switch (event) {
                case SimulationEvent.LoopDetected loop -> this.reportLoop(loop);
                case SimulationEvent.QueueSaturated saturated -> this.reportSaturation();
            }
        }
    }

    // ---- Signalements ----

    private static String loopProblem(DeviceId network) {
        return "loop:" + network;
    }

    /**
     * Une boucle, même vue par plusieurs switchs, donne un seul message par joueur : chaque joueur à
     * moins de 32 blocs d'un switch qui l'a vue reçoit le nom du plus proche. Le log en garde le détail.
     */
    private void reportLoop(SimulationEvent.LoopDetected loop) {
        List<SimulationEvent.LoopPoint> points = loop.points();
        Map<ServerPlayer, Message> recipients = new LinkedHashMap<>();
        NetworkDevices.levelOf(this.server, loop.network()).ifPresent(level -> {
            for (ServerPlayer player : level.players()) {
                this.nearest(points, player).ifPresent(point -> recipients.put(player, this.loopMessage(point.device())));
            }
        });
        String where = points.stream()
                .map(point -> this.deviceArg(point.device()).logText() + " port " + (point.port() + 1))
                .collect(Collectors.joining(", "));
        MessageCenter.of(this.server).event(loopProblem(loop.network()), this.loopMessage(points.getFirst().device()),
                "Network loop: copies of a broadcast frame came back to " + where + "; they were dropped.", recipients);
    }

    /** Le switch qui a vu la boucle le plus près du joueur, à moins de 32 blocs. */
    private Optional<SimulationEvent.LoopPoint> nearest(List<SimulationEvent.LoopPoint> points, ServerPlayer player) {
        SimulationEvent.LoopPoint best = null;
        double bestDistance = LOOP_MESSAGE_RADIUS * LOOP_MESSAGE_RADIUS;
        for (SimulationEvent.LoopPoint point : points) {
            BlockPos pos = NetworkDevices.posOf(point.device());
            double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distance <= bestDistance) {
                best = point;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    private Message loopMessage(DeviceId device) {
        return Message.of(MessageLevel.WARNING, LOOP_MESSAGE, this.deviceArg(device));
    }

    private MessageArg deviceArg(DeviceId device) {
        return this.graph().node(device).map(NetworkDevices::deviceArg).orElse(MessageArg.text(device.toString()));
    }

    /**
     * La file est globale au serveur : sont prévenus les opérateurs connectés, et en solo le
     * propriétaire du monde (sans triches, il n'est pas opérateur).
     */
    private void reportSaturation() {
        Message message = Message.of(MessageLevel.WARNING, SATURATED_MESSAGE);
        Map<ServerPlayer, Message> recipients = new LinkedHashMap<>();
        for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
            if (this.server.getPlayerList().isOp(player.nameAndId()) || this.server.isSingleplayerOwner(player.nameAndId())) {
                recipients.put(player, message);
            }
        }
        MessageCenter.of(this.server).event(SATURATED_PROBLEM, message, "Network queue saturated: more than "
                + this.simulation().limits().maxQueued() + " frames waiting, new frames were dropped.", recipients);
    }

    // ---- Réseaux touchés par un changement ----

    /** L'appareil et ceux qui lui sont reliés directement par un câble. */
    private Set<DeviceId> withNeighbours(DeviceId device) {
        Set<DeviceId> devices = new LinkedHashSet<>();
        devices.add(device);
        this.graph().node(device).ifPresent(node -> {
            for (int i = 0; i < node.portCount(); i++) {
                PortRef port = node.port(i);
                this.graph().linkAt(port).ifPresent(link -> devices.add(link.otherEnd(port).device()));
            }
        });
        return devices;
    }

    private Set<DeviceId> networksOf(Collection<DeviceId> devices) {
        Set<DeviceId> networks = new LinkedHashSet<>();
        devices.forEach(device -> networks.add(this.graph().networkKey(device)));
        return networks;
    }

    /**
     * Après un changement, les boucles déjà signalées dans les réseaux touchés (avant et après le
     * changement) sont oubliées : une boucle toujours là est signalée de nouveau sans attendre la
     * minute. Les autres réseaux gardent leur limite.
     */
    private void forgetLoops(Set<DeviceId> networks) {
        MessageCenter messages = MessageCenter.of(this.server);
        networks.forEach(network -> messages.forgetProblem(loopProblem(network)));
    }

    private NetworkGraph graph() {
        return this.data.graph();
    }

    private NetworkSimulation simulation() {
        return this.data.simulation();
    }
}
