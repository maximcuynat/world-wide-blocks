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
 * Single entry point of the network engine on the server side. Every network change goes through here,
 * so the save is marked dirty and the simulation and the clients are notified.
 *
 * <p>No static state: the object is a light facade over the running server's data,
 * obtained through {@link #of(MinecraftServer)}. The data disappears with the server, so
 * in singleplayer two worlds never get mixed.
 */
public final class NetworkManager {
    /** Players within 32 blocks of a device that sees the loop are notified. */
    private static final double LOOP_MESSAGE_RADIUS = 32.0;
    private static final String LOOP_MESSAGE = "message.worldwideblocks.network.loop";
    private static final String SATURATED_MESSAGE = "message.worldwideblocks.network.saturated";
    private static final String SATURATED_PROBLEM = "saturated";

    private final MinecraftServer server;
    private final NetworkSavedData data;

    /** NeoForge bus: advances the simulation on every server tick. */
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

    // ---- Reading ----

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

    /** Can a pending connection started from {@code start} still be completed by whoever is in {@code holderDimension}? */
    public PendingCable.Status pendingCable(PortRef start, String holderDimension) {
        return PendingCable.check(this.graph(), start, holderDimension);
    }

    public List<MacAddress> macsOf(DeviceId device) {
        return this.graph().macsOf(device);
    }

    /** Behavior of the device in the simulation (status panel); empty before the first tick. */
    public Optional<DeviceLogic> logic(DeviceId device) {
        return this.simulation().logic(device);
    }

    // ---- Modification ----

    /** Registers or updates a device; returns the cables that became invalid and were removed. */
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
        Set<DeviceId> touched = Set.copyOf(List.of(a.device(), b.device())); // Set.of refuses duplicates (same device)
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

    /** Removes the device and its cables; returns the removed cables. */
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

    /** One server tick: the simulation advances to the game time, then its reports become messages. */
    private void tick() {
        // Overworld game time: saved with the world, it never goes backwards.
        long gameTime = this.server.overworld().getGameTime();
        this.simulation().tick(gameTime);
        for (SimulationEvent event : this.simulation().drainEvents()) {
            switch (event) {
                case SimulationEvent.LoopDetected loop -> this.reportLoop(loop);
                case SimulationEvent.QueueSaturated saturated -> this.reportSaturation();
            }
        }
    }

    // ---- Reports ----

    private static String loopProblem(DeviceId network) {
        return "loop:" + network;
    }

    /**
     * A loop, even seen by several switches, gives a single message per player: each player within
     * 32 blocks of a switch that saw it receives the name of the nearest. The log keeps the detail.
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

    /** The switch that saw the loop nearest to the player, within 32 blocks. */
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
     * The queue is global to the server: the connected operators are notified, and in singleplayer the
     * world owner (without cheats enabled, the owner is not an operator).
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

    // ---- Networks touched by a change ----

    /** The device and those directly linked to it by a cable. */
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
     * After a change, the loops already reported in the touched networks (before and after the
     * change) are forgotten: a loop still there is reported again without waiting for the
     * minute. The other networks keep their limit.
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
