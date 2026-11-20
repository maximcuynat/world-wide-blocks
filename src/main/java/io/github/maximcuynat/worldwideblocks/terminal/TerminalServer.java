package io.github.maximcuynat.worldwideblocks.terminal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.block.LaptopBlock;
import io.github.maximcuynat.worldwideblocks.block.PcTowerBlock;
import io.github.maximcuynat.worldwideblocks.message.Message;
import io.github.maximcuynat.worldwideblocks.message.MessageLevel;
import io.github.maximcuynat.worldwideblocks.message.server.Diagnostics;
import io.github.maximcuynat.worldwideblocks.message.server.MessageCenter;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevices;
import io.github.maximcuynat.worldwideblocks.network.NetworkManager;
import io.github.maximcuynat.worldwideblocks.network.sim.HostLogic;
import io.github.maximcuynat.worldwideblocks.network.sync.TerminalClosePayload;
import io.github.maximcuynat.worldwideblocks.network.sync.TerminalInputPayload;
import io.github.maximcuynat.worldwideblocks.network.sync.TerminalUpdatesPayload;
import io.github.maximcuynat.worldwideblocks.os.ConsoleSession;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The terminals on the server side ({@code docs/lot-plan.md}, lot 1c-1, "Machine, console and session"): a player
 * opens the local console of a computer, one player at the keyboard at a time; the server sends the console, then its
 * changes, each tick; it reads the keys of the player at the keyboard only, each packet checked again (session, size,
 * characters, rate, distance); it frees the keyboard when the player leaves, closes the screen, walks away, changes
 * dimension, when the computer is removed or its lid closed, and when the shell ends.
 *
 * <p>The console and its commands go on when nobody watches. State: {@link TerminalHub}, in the running server.
 */
public final class TerminalServer {
    /** Message of a right-click on a computer whose keyboard another player has. */
    public static final String BUSY = "message.worldwideblocks.terminal.busy";

    private TerminalServer() {
    }

    // ---- Opening ----

    /**
     * Right-click on a computer: its local console for this player (a new shell if the last one ended), or "computer
     * busy". A player at another keyboard leaves it.
     */
    public static void open(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        MinecraftServer server = level.getServer();
        NetworkManager network = NetworkManager.of(server);
        DeviceId id = NetworkDevices.idOf(level, pos);
        Optional<HostLogic> host = host(network, id);
        if (host.isEmpty()) {
            return; // not known to the engine yet
        }
        Keyboards keyboards = network.terminals().keyboards();
        Optional<Keyboards.Watch> current = keyboards.watch(id);
        if (current.isPresent() && !current.get().player().equals(player.getUUID())) {
            network.node(id).ifPresent(node -> MessageCenter.of(server).gesture(player,
                    Message.of(MessageLevel.ERROR, BUSY, NetworkDevices.nameArg(node))));
            return;
        }
        keyboards.deviceOf(player.getUUID()).filter(other -> !other.equals(id)).ifPresent(other -> release(server, other));
        ConsoleSession console = host.get().system().openLocalConsole();
        keyboards.take(id, player.getUUID(), console.id());
        Keyboards.Watch watch = keyboards.watch(id).orElseThrow();
        TerminalUpdate.of(console.snapshot()).forEach(watch::offer);
        host.get().system().setConsoleListener((session, change) -> keyboards.watch(id)
                .filter(viewer -> viewer.session() == session.id())
                .ifPresent(viewer -> viewer.offer(TerminalUpdate.of(change))));
        send(player, pos, watch); // the screen opens at once, not at the next tick
        if (Diagnostics.enabled()) {
            Diagnostics.debug("Terminal: {} at the keyboard of {} (session {})", player.getName().getString(), id, console.id());
        }
    }

    // ---- Keys of the player ----

    /** A packet of a terminal screen, on the main thread. Anything that does not hold is dropped, never thrown. */
    public static void onInput(TerminalInputPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            input(player, payload);
        }
    }

    /** The checks and effect of one packet: public for the game tests, which have no client. */
    public static void input(ServerPlayer player, TerminalInputPayload payload) {
        MinecraftServer server = player.level().getServer();
        NetworkManager network = NetworkManager.of(server);
        TerminalHub hub = network.terminals();
        if (!hub.allow(player.getUUID(), server.getTickCount())) {
            drop(player, payload, "more than " + TerminalRules.BURST + " packets, then one per tick");
            return;
        }
        DeviceId id = NetworkDevices.idOf(player.level(), payload.pos());
        Optional<Keyboards.Watch> watch = hub.keyboards().watch(id).filter(viewer -> viewer.player().equals(player.getUUID()));
        if (watch.isEmpty()) {
            drop(player, payload, "not at this keyboard");
            return;
        }
        if (watch.get().session() != payload.session()) {
            drop(player, payload, "session " + payload.session() + ", the screen shows " + watch.get().session());
            return;
        }
        Optional<String> away = leaveReason(server, player, id);
        if (away.isPresent()) {
            leave(server, player, id, away.get());
            return;
        }
        switch (payload.kind()) {
            case LINE, INTERRUPT -> {
                Optional<String> refusal = TerminalRules.refusal(payload.text());
                if (refusal.isPresent()) {
                    drop(player, payload, refusal.get());
                } else if (payload.kind() == TerminalInputPayload.Kind.LINE) {
                    network.consoleLine(id, payload.text());
                } else {
                    network.consoleInterrupt(id, payload.text());
                }
            }
            case END_OF_INPUT -> network.consoleEndOfInput(id);
            case CLEAR -> network.consoleClear(id);
            case CLOSE -> release(server, id); // Esc: the screen is already hidden, the console goes on
        }
    }

    // ---- Each tick ----

    /** NeoForge bus, after the simulation: frees the keyboards whose player left, then sends the changes. */
    public static void onServerTick(ServerTickEvent.Post event) {
        tick(event.getServer());
    }

    static void tick(MinecraftServer server) {
        Keyboards keyboards = NetworkManager.of(server).terminals().keyboards();
        for (Keyboards.Watch watch : keyboards.watches()) {
            ServerPlayer player = server.getPlayerList().getPlayer(watch.player());
            Optional<String> away = leaveReason(server, player, watch.device());
            if (away.isPresent()) {
                leave(server, player, watch.device(), away.get());
                continue;
            }
            boolean ended = watch.ended();
            send(player, NetworkDevices.posOf(watch.device()), watch);
            if (ended) {
                leave(server, player, watch.device(), "the shell ended");
            }
        }
    }

    /** NeoForge bus: a player who leaves the server leaves the keyboard. */
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.level().getServer();
            NetworkManager network = NetworkManager.of(server);
            network.terminals().keyboards().deviceOf(player.getUUID()).ifPresent(id -> release(server, id));
            network.terminals().forget(player.getUUID());
        }
    }

    /** The player at the keyboard of this computer, if any: for the game tests. */
    public static Optional<UUID> playerAt(MinecraftServer server, DeviceId device) {
        return NetworkManager.of(server).terminals().keyboards().watch(device).map(Keyboards.Watch::player);
    }

    // ---- Helpers ----

    /**
     * Why the player can no longer be at the keyboard of this computer, or empty if they still can: left or dead,
     * another dimension, too far (as for the containers of the game), computer removed, lid closed.
     */
    private static Optional<String> leaveReason(MinecraftServer server, ServerPlayer player, DeviceId id) {
        if (player == null || player.isRemoved()) {
            return Optional.of("left the server");
        }
        if (!player.isAlive()) {
            return Optional.of("dead");
        }
        Optional<ServerLevel> level = NetworkDevices.levelOf(server, id);
        if (level.isEmpty() || level.get() != player.level()) {
            return Optional.of("in another dimension");
        }
        BlockPos pos = NetworkDevices.posOf(id);
        if (!level.get().isLoaded(pos)) {
            return Optional.of("computer not loaded");
        }
        BlockState state = level.get().getBlockState(pos);
        if (!(state.getBlock() instanceof PcTowerBlock || state.getBlock() instanceof LaptopBlock)
                || host(NetworkManager.of(server), id).isEmpty()) {
            return Optional.of("computer removed");
        }
        if (state.getBlock() instanceof LaptopBlock && !state.getValue(LaptopBlock.OPEN)) {
            return Optional.of("lid closed");
        }
        if (!player.isWithinBlockInteractionRange(pos, TerminalRules.REACH_BUFFER)) {
            return Optional.of("too far");
        }
        return Optional.empty();
    }

    /** Frees the keyboard and closes the screen of the player, if they are still there. */
    private static void leave(MinecraftServer server, ServerPlayer player, DeviceId id, String reason) {
        release(server, id);
        if (player != null && !player.isRemoved()) {
            PacketDistributor.sendToPlayer(player, new TerminalClosePayload(NetworkDevices.posOf(id)));
        }
        if (Diagnostics.enabled()) {
            Diagnostics.debug("Terminal: keyboard of {} freed, {}", id, reason);
        }
    }

    /** The keyboard of this computer is free; its console is no longer watched. */
    private static void release(MinecraftServer server, DeviceId id) {
        NetworkManager network = NetworkManager.of(server);
        network.terminals().keyboards().release(id);
        host(network, id).ifPresent(host -> host.system().setConsoleListener(null));
    }

    private static void send(ServerPlayer player, BlockPos pos, Keyboards.Watch watch) {
        List<List<TerminalUpdate>> packets = watch.drain(TerminalRules.UPDATES_PER_PACKET);
        for (List<TerminalUpdate> updates : packets) {
            PacketDistributor.sendToPlayer(player, new TerminalUpdatesPayload(pos, updates));
        }
    }

    private static Optional<HostLogic> host(NetworkManager network, DeviceId id) {
        return network.logic(id).filter(HostLogic.class::isInstance).map(HostLogic.class::cast);
    }

    private static void drop(ServerPlayer player, TerminalInputPayload payload, String reason) {
        if (Diagnostics.enabled()) {
            Diagnostics.debug("Terminal: packet {} of {} dropped: {}", payload.kind(), player.getName().getString(), reason);
        }
    }
}
