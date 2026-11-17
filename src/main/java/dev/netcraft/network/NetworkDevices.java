package dev.netcraft.network;

import java.util.List;
import java.util.Optional;

import dev.netcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Passerelle entre le monde Minecraft (niveaux, positions, blocs) et le moteur réseau,
 * qui ne manipule que des {@link DeviceId}.
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

    /** L'appareil décrit par le bloc présent à cette position, si c'est un appareil réseau. */
    public static Optional<NetworkNode> nodeAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetworkDevice device)) {
            return Optional.empty();
        }
        String type = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return Optional.of(new NetworkNode(idOf(level, pos), type, device.ports()));
    }

    /**
     * Un appareil vient d'être posé : on efface d'abord tout câble resté enregistré à cette position
     * (un bloc retiré sans que le jeu le signale, par exemple par un {@code /setblock} sans mise à jour),
     * puis on enregistre l'appareil.
     *
     * @return les câbles orphelins retirés
     */
    public static List<Link> onPlaced(ServerLevel level, BlockPos pos) {
        NetworkManager network = NetworkManager.of(level.getServer());
        DeviceId id = idOf(level, pos);
        List<Link> orphans = network.removeDevice(id);
        nodeAt(level, pos).ifPresent(network::registerDevice);
        return orphans;
    }

    /**
     * L'appareil a quitté cette position (cassé, explosé, remplacé) : il est retiré du réseau avec ses câbles.
     *
     * @return les câbles retirés
     */
    public static List<Link> onRemoved(ServerLevel level, BlockPos pos) {
        return NetworkManager.of(level.getServer()).removeDevice(idOf(level, pos));
    }

    /**
     * Revérifie un appareil avant de s'en servir. Si sa zone est chargée et que le bloc n'est plus un
     * appareil (ou plus le même), le moteur est corrigé. Si la zone n'est pas chargée, on se fie au moteur.
     *
     * @return l'appareil tel que le moteur le connaît après vérification
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
     * Fait tomber à cette position un cordon par câble retiré, mais seulement pour les câbles
     * qui en avaient consommé un : un câble posé en créatif ne rend rien.
     */
    public static void dropCables(ServerLevel level, BlockPos pos, List<Link> removed) {
        for (Link link : removed) {
            if (link.cableConsumed()) {
                Block.popResource(level, pos, new ItemStack(ModItems.ETHERNET_CABLE.get()));
            }
        }
    }

    /** Nom de l'appareil tel qu'il apparaît en jeu, par exemple « Tour PC ». */
    public static Component displayName(NetworkNode node) {
        return Component.translatable("block." + node.deviceType().replace(':', '.'));
    }
}
