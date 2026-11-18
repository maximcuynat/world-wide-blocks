package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import org.jspecify.annotations.Nullable;

/**
 * Test player connected to the server through an in-memory connection, modeled on the
 * {@code GameTestPlayer} of the NeoForge tests: it is in the player list and in its level, the
 * server ticks it like any player (inventory included), it can change dimension and really
 * disconnect.
 *
 * <p>Messages are checked through the message module listener ({@link NetworkScene#messages()}),
 * not through this player.
 */
public final class TestPlayer extends ServerPlayer {
    private boolean left;

    private TestPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation information) {
        super(server, level, profile, information);
    }

    /**
     * Connects a test player, like a client joining the server, then places it at
     * {@code relativePos} in the test area. An {@code id} already known to the world takes over that
     * player's saved data (inventory included): it is a reconnection.
     */
    public static TestPlayer connect(GameTestHelper helper, UUID id, GameType gameType, Vec3 relativePos) {
        MinecraftServer server = helper.getLevel().getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(id, "wwb-test"), false);
        TestPlayer player = new TestPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        // Full name: in a subclass of ServerPlayer, "Connection" means the inherited type
        // WaypointTransmitter.Connection, which hides the import.
        net.minecraft.network.Connection memory = new net.minecraft.network.Connection(PacketFlow.SERVERBOUND) {
            @Override
            public boolean isMemoryConnection() {
                return true;
            }

            @Override
            public void send(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
                super.send(packet, listener, flush);
                // The client answers keep-alive packets at once, otherwise the server would disconnect it.
                if (packet instanceof ClientboundKeepAlivePacket keepAlive) {
                    player.connection.handleKeepAlive(new ServerboundKeepAlivePacket(keepAlive.getId()));
                }
            }
        };
        // The constructor binds the channel to the connection.
        new EmbeddedChannel(memory);
        NetworkRegistry.configureMockConnection(memory);
        // Like PrepareSpawnTask.spawn on a real connection: the player's saved data
        // (inventory included) is read back before they enter the world.
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(player.problemPath(), WorldWideBlocks.LOGGER)) {
            server.getPlayerList().loadPlayerData(player.nameAndId())
                    .map(tag -> TagValueInput.create(reporter, server.registryAccess(), tag))
                    .ifPresent(player::load);
            EventHooks.firePlayerLoadingEvent(player, server.getPlayerList(), player.getStringUUID());
        }
        server.getPlayerList().placeNewPlayer(memory, player, cookie);
        // Registered among the server connections, it is ticked: the player lives like a real one.
        server.getConnection().getConnections().add(memory);
        player.gameMode.changeGameModeForPlayer(gameType);
        player.placeAt(helper.absoluteVec(relativePos));
        player.connection.markClientLoaded();
        return player;
    }

    /** Moves the player within its current level and sends it the chunks around. */
    public void placeAt(Vec3 absolutePos) {
        this.snapTo(absolutePos.x, absolutePos.y, absolutePos.z);
        this.level().getChunkSource().move(this);
        this.connection.chunkSender.sendNextChunks(this);
        this.connection.chunkSender.onChunkBatchReceivedByClient(64.0F);
    }

    /**
     * Disconnection, like a client leaving: the server saves the player and removes it at once.
     * (Not {@code disconnect()}, which is an internal step of {@code ServerPlayer}.)
     */
    public void leaveServer() {
        if (this.left) {
            return;
        }
        this.left = true;
        net.minecraft.network.Connection memory = this.connection.getConnection();
        this.connection.disconnect(Component.literal("World Wide Blocks test finished"));
        // The disconnection is already handled: without this removal, the server's connection loop
        // would handle it a second time (warning "handleDisconnection() called twice").
        this.level().getServer().getConnection().getConnections().remove(memory);
    }
}
