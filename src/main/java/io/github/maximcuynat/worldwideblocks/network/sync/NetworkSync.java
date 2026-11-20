package io.github.maximcuynat.worldwideblocks.network.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.network.ChunkKey;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevice;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevices;
import io.github.maximcuynat.worldwideblocks.network.NetworkManager;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpBinding;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClient;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServerState;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.sim.BoxLogic;
import io.github.maximcuynat.worldwideblocks.network.sim.DeviceLogic;
import io.github.maximcuynat.worldwideblocks.network.sim.HostLogic;
import io.github.maximcuynat.worldwideblocks.network.switching.MacTable;
import io.github.maximcuynat.worldwideblocks.registry.ModAttachments;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalServer;
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
 * Sends the clients what they display: the cables they can see, and the state of the device
 * the player is aiming at. A player only receives a cable if one of its ends is in a chunk
 * its client has received (see {@link PlayerLinkView}), and the state of a device only if they aim at it within
 * reach. The server stays the only judge: the client receives only a copy; the only packet it sends is the keys of
 * a terminal screen, checked again by the server ({@code TerminalServer}).
 */
public final class NetworkSync {
    private static final String PROTOCOL_VERSION = "5"; // 5: terminal packets (lot 1c-1)
    /** The device each player aims at is sampled every 5 ticks (a quarter of a second). */
    private static final int STATUS_INTERVAL_TICKS = 5;

    private NetworkSync() {
    }

    /** Mod bus: declares the packets. Their receivers are registered on the client side only. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToClient(LinksResetPayload.TYPE, LinksResetPayload.STREAM_CODEC)
                .playToClient(LinkAddedPayload.TYPE, LinkAddedPayload.STREAM_CODEC)
                .playToClient(LinkRemovedPayload.TYPE, LinkRemovedPayload.STREAM_CODEC)
                .playToClient(DeviceStatusPayload.TYPE, DeviceStatusPayload.STREAM_CODEC)
                .playToClient(MessagePayload.TYPE, MessagePayload.STREAM_CODEC)
                .playToClient(TerminalUpdatesPayload.TYPE, TerminalUpdatesPayload.STREAM_CODEC)
                .playToClient(TerminalClosePayload.TYPE, TerminalClosePayload.STREAM_CODEC)
                .playToServer(TerminalInputPayload.TYPE, TerminalInputPayload.STREAM_CODEC, TerminalServer::onInput);
    }

    // ---- Chunk tracking per player ----

    /** The client just received a chunk: it can now know the cables that end in it. */
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        ServerPlayer player = event.getPlayer();
        ServerLevel level = event.getLevel();
        PlayerLinkView view = player.getData(ModAttachments.PLAYER_LINK_VIEW);
        String dimension = dimensionOf(level);
        if (view.enterDimension(dimension)) {
            // Login, respawn or dimension change: the client starts again from zero.
            player.getData(ModAttachments.PLAYER_STATUS_VIEW).reset();
            PacketDistributor.sendToPlayer(player, LinksResetPayload.INSTANCE);
        }
        ChunkKey chunk = new ChunkKey(dimension, event.getPos().x(), event.getPos().z());
        for (Link link : view.chunkSent(chunk, NetworkManager.of(level.getServer()).linksTouching(chunk))) {
            PacketDistributor.sendToPlayer(player, new LinkAddedPayload(ClientLink.of(link)));
        }
    }

    /** The player no longer tracks a chunk: it forgets the cables whose other end is not tracked either. */
    public static void onChunkUnWatch(ChunkWatchEvent.UnWatch event) {
        ServerPlayer player = event.getPlayer();
        ServerLevel level = event.getLevel();
        PlayerLinkView view = player.getData(ModAttachments.PLAYER_LINK_VIEW);
        ChunkKey chunk = new ChunkKey(dimensionOf(level), event.getPos().x(), event.getPos().z());
        for (UUID id : view.chunkUnwatched(chunk, NetworkManager.of(level.getServer()).linksTouching(chunk))) {
            PacketDistributor.sendToPlayer(player, new LinkRemovedPayload(id));
        }
    }

    // ---- State of the targeted device ----

    /** NeoForge bus: sends each player the state of the device they aim at, if it changed. */
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

    /** State of the network device the player aims at within reach, or {@code null}. */
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
            return null; // device not yet known to the engine
        }
        List<Long> portMacs = new ArrayList<>(macs.size());
        macs.forEach(mac -> portMacs.add(mac.value()));
        boolean switching = network.logic(id).map(DeviceLogic::bridges).orElse(false);
        List<LearnedMac> learned = new ArrayList<>();
        for (MacTable.Entry entry : network.logic(id).map(DeviceLogic::learnedMacs).orElse(List.of())) {
            learned.add(new LearnedMac(entry.port(), entry.mac().value()));
        }
        List<DhcpPortStatus> dhcp = new ArrayList<>();
        Optional<Boolean> dhcpServer = Optional.empty();
        List<DhcpLeaseStatus> leases = new ArrayList<>();
        Optional<DeviceLogic> logic = network.logic(id);
        if (logic.isPresent() && logic.get() instanceof HostLogic host) {
            for (int port = 0; port < macs.size(); port++) {
                dhcp.add(dhcpStatus(port, host.dhcpClient(port)));
            }
        } else if (logic.isPresent() && logic.get() instanceof BoxLogic box) {
            DhcpServerState server = box.dhcpServer().state();
            dhcpServer = Optional.of(server.enabled());
            long now = level.getServer().overworld().getGameTime();
            for (DhcpBinding lease : server.leasesAt(now)) {
                leases.add(new DhcpLeaseStatus(lease.address().value(), lease.client().value()));
            }
        }
        return new DeviceStatusPayload(pos, switching, portMacs, learned, dhcp, dhcpServer, leases);
    }

    private static DhcpPortStatus dhcpStatus(int port, DhcpClient client) {
        int state = client.state().ordinal();
        return client.lease()
                .map(lease -> new DhcpPortStatus(port, state, lease.address().value(), lease.subnetMask().value(),
                        lease.router().value(), lease.dnsServer().value(), lease.expiresAt()))
                .orElseGet(() -> new DhcpPortStatus(port, state, 0, 0, 0, 0, 0L));
    }

    // ---- Network changes ----

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

    /** Level where the cable is; its two ends are always in the same dimension. */
    private static ServerLevel levelOf(MinecraftServer server, Link link) {
        return NetworkDevices.levelOf(server, link.a().device()).orElse(null);
    }
}
