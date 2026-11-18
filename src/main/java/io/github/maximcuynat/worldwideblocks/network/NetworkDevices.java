package io.github.maximcuynat.worldwideblocks.network;

import java.util.List;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.message.MessageArg;
import io.github.maximcuynat.worldwideblocks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bridge between the Minecraft world (levels, positions, blocks) and the network engine,
 * which only handles {@link DeviceId}s.
 */
public final class NetworkDevices {

    private NetworkDevices() {
    }

    public static DeviceId idOf(Level level, BlockPos pos) {
        return new DeviceId(level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static BlockPos posOf(DeviceId id) {
        return new BlockPos(id.x(), id.y(), id.z());
    }

    /** Level (dimension) where the device is, if it exists on this server. */
    public static Optional<ServerLevel> levelOf(MinecraftServer server, DeviceId id) {
        return Optional.ofNullable(server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(id.dimension()))));
    }

    /** The device described by the block at this position, if it is a network device. */
    public static Optional<NetworkNode> nodeAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetworkDevice device)) {
            return Optional.empty();
        }
        String type = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return Optional.of(new NetworkNode(idOf(level, pos), type, device.role(), device.ports()));
    }

    /**
     * A device was just placed: first clear any cable still registered at this position
     * (a block removed without the game reporting it, for example by a {@code /setblock} without update),
     * then register the device.
     *
     * @return the orphan cables removed
     */
    public static List<Link> onPlaced(ServerLevel level, BlockPos pos) {
        NetworkManager network = NetworkManager.of(level.getServer());
        DeviceId id = idOf(level, pos);
        List<Link> orphans = network.removeDevice(id);
        nodeAt(level, pos).ifPresent(network::registerDevice);
        return orphans;
    }

    /**
     * The device left this position (broken, exploded, replaced): it is removed from the network with its cables.
     *
     * @return the removed cables
     */
    public static List<Link> onRemoved(ServerLevel level, BlockPos pos) {
        return NetworkManager.of(level.getServer()).removeDevice(idOf(level, pos));
    }

    /**
     * Rechecks a device before using it. If its area is loaded and the block is no longer a
     * device (or no longer the same one), the engine is corrected. If the area is not loaded, trust the engine.
     *
     * @return the device as the engine knows it after the check
     */
    public static Optional<NetworkNode> validate(ServerLevel level, DeviceId id) {
        NetworkManager network = NetworkManager.of(level.getServer());
        BlockPos pos = posOf(id);
        if (!idOf(level, pos).equals(id) || !level.isLoaded(pos)) {
            return network.node(id);
        }
        Optional<NetworkNode> actual = nodeAt(level, pos);
        List<Link> removed = actual.isEmpty() ? network.removeDevice(id) : network.registerDevice(actual.get());
        dropCables(level, pos, removed);
        return actual;
    }

    /**
     * Drops one cord at this position per removed cable, but only for the cables
     * that had consumed one: a cable placed in creative gives nothing back.
     */
    public static void dropCables(ServerLevel level, BlockPos pos, List<Link> removed) {
        for (Link link : removed) {
            if (link.cableConsumed()) {
                Block.popResource(level, pos, new ItemStack(ModItems.ETHERNET_CABLE.get()));
            }
        }
    }

    /** Device as a message value: translated name and coordinates for the player, identifier in the log. */
    public static MessageArg deviceArg(NetworkNode node) {
        return MessageArg.device(node.id(), node.deviceType());
    }

    /** Device name as a message value: its language key, translated by the client. */
    public static MessageArg nameArg(NetworkNode node) {
        return MessageArg.key("block." + node.deviceType().replace(':', '.'));
    }

    /** Device name as it appears in game, for example "PC Tower". */
    public static Component displayName(NetworkNode node) {
        return Component.translatable("block." + node.deviceType().replace(':', '.'));
    }
}
