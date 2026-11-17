package dev.netcraft.item;

import java.util.Locale;
import java.util.Optional;

import dev.netcraft.network.ConnectOutcome;
import dev.netcraft.network.ConnectResult;
import dev.netcraft.network.DeviceId;
import dev.netcraft.network.Link;
import dev.netcraft.network.LinkMedium;
import dev.netcraft.network.NetworkDevice;
import dev.netcraft.network.NetworkDevices;
import dev.netcraft.network.NetworkManager;
import dev.netcraft.network.NetworkNode;
import dev.netcraft.network.PortRef;
import dev.netcraft.registry.ModDataComponents;
import dev.netcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Cordon Ethernet : un clic droit sur un appareil, puis un clic droit sur un autre, les relie.
 * Accroupi + clic droit sur un appareil, cordon en main : débranche son câble des deux bouts.
 * Accroupi + clic droit main vide : retire un seul bout et le garde en main (voir {@link #takeCableEnd}).
 * Accroupi + clic droit dans le vide : annule un branchement commencé, ou range un bout tenu en main.
 *
 * <p>Toute la logique tourne sur le serveur ; le client se contente de confirmer le clic.
 */
public class EthernetCableItem extends Item {
    private static final LinkMedium MEDIUM = LinkMedium.COPPER_ETHERNET;

    public EthernetCableItem(Item.Properties properties) {
        super(properties);
    }

    /** Branchement commencé : l'objet brille, comme un objet enchanté. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.CABLE_START.get()) || super.isFoil(stack);
    }

    /**
     * Appelé avant le clic droit du bloc : avec le cordon en main, un clic sur le portable branche
     * au lieu d'ouvrir ou fermer l'écran.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockState(pos).getBlock() instanceof NetworkDevice)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        DeviceId id = NetworkDevices.idOf(level, pos);
        Optional<NetworkNode> node = NetworkDevices.validate(serverLevel, id);
        if (node.isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (player.isSecondaryUseActive()) {
            this.unplug(serverLevel, player, node.get());
        } else if (stack.has(ModDataComponents.CABLE_START.get())) {
            this.finish(serverLevel, player, stack, node.get());
        } else {
            this.start(serverLevel, player, stack, node.get());
        }
        return InteractionResult.SUCCESS;
    }

    /** Accroupi + clic droit dans le vide : annule le branchement commencé. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() || !stack.has(ModDataComponents.CABLE_START.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            stack.remove(ModDataComponents.CABLE_START.get());
            player.sendOverlayMessage(Component.translatable("message.netcraft.cable.cancelled"));
        }
        return InteractionResult.SUCCESS;
    }

    private void start(ServerLevel level, Player player, ItemStack stack, NetworkNode node) {
        NetworkManager network = NetworkManager.of(level.getServer());
        Optional<PortRef> port = network.firstFreePort(node.id());
        if (port.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.netcraft.cable.no_free_port", NetworkDevices.displayName(node)));
            return;
        }
        GlobalPos device = GlobalPos.of(level.dimension(), NetworkDevices.posOf(node.id()));
        stack.set(ModDataComponents.CABLE_START.get(), new CableStart(device, port.get().index()));
        player.sendOverlayMessage(Component.translatable("message.netcraft.cable.start",
                NetworkDevices.displayName(node), port.get().displayNumber()));
    }

    private void finish(ServerLevel level, Player player, ItemStack stack, NetworkNode end) {
        NetworkManager network = NetworkManager.of(level.getServer());
        CableStart pending = stack.get(ModDataComponents.CABLE_START.get());
        DeviceId startId = new DeviceId(pending.device().dimension().identifier().toString(),
                pending.device().pos().getX(), pending.device().pos().getY(), pending.device().pos().getZ());

        // La première extrémité a pu disparaître depuis le premier clic : on la revérifie.
        if (!startId.sameDimension(end.id())) {
            player.sendOverlayMessage(result(ConnectResult.OTHER_DIMENSION, 0));
            return;
        }
        Optional<NetworkNode> start = NetworkDevices.validate(level, startId);
        if (start.isEmpty() || !start.get().hasPort(pending.port())) {
            stack.remove(ModDataComponents.CABLE_START.get());
            player.sendOverlayMessage(Component.translatable("message.netcraft.cable.start_gone"));
            return;
        }
        PortRef startPort = start.get().port(pending.port());
        if (network.linkAt(startPort).isPresent()) {
            stack.remove(ModDataComponents.CABLE_START.get());
            player.sendOverlayMessage(Component.translatable("message.netcraft.cable.start_taken"));
            return;
        }

        // Pas de port libre en face : on vise le port 0 pour obtenir le refus « port occupé ».
        PortRef endPort = network.firstFreePort(end.id()).orElse(end.port(0));
        boolean consume = !player.hasInfiniteMaterials();
        ConnectOutcome outcome = network.connect(startPort, endPort, MEDIUM, consume);
        if (!outcome.isSuccess()) {
            player.sendOverlayMessage(result(outcome.result(), startId.distanceTo(end.id())));
            return;
        }
        Link link = outcome.link().orElseThrow();
        stack.remove(ModDataComponents.CABLE_START.get());
        if (consume) {
            stack.shrink(1);
        }
        player.sendOverlayMessage(Component.translatable("message.netcraft.cable.connected",
                NetworkDevices.displayName(start.get()), startPort.displayNumber(),
                NetworkDevices.displayName(end), endPort.displayNumber(),
                String.format(Locale.ROOT, "%.1f", link.length())));
    }

    /** Accroupi + clic droit, cordon en main : débranche les deux bouts et range le cordon. */
    private void unplug(ServerLevel level, Player player, NetworkNode node) {
        Optional<Link> removed = disconnectFirst(level, node);
        if (removed.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.netcraft.cable.nothing_to_unplug", NetworkDevices.displayName(node)));
            return;
        }
        // Seul un câble qui a consommé un cordon en rend un.
        if (removed.get().cableConsumed()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(this), Prediction.SERVER_ONLY);
        }
        player.sendOverlayMessage(Component.translatable("message.netcraft.cable.unplugged",
                NetworkDevices.displayName(node), portOn(removed.get(), node).displayNumber()));
    }

    /**
     * Accroupi + clic droit main vide sur un appareil : on retire de cet appareil le bout de son câble
     * et on le garde en main, l'autre bout restant branché. Le cordon en main porte cette autre
     * extrémité comme un branchement commencé : un clic droit sur un appareil le rebranche,
     * accroupi + clic dans le vide le débranche aussi de l'autre bout et le range.
     *
     * <p>Un câble posé en créatif (sans cordon consommé) ne donne pas de cordon en survie : il est
     * simplement débranché des deux bouts, sinon on fabriquerait des cordons gratuits.
     */
    public static void takeCableEnd(ServerLevel level, Player player, BlockPos pos) {
        Optional<NetworkNode> node = NetworkDevices.validate(level, NetworkDevices.idOf(level, pos));
        if (node.isEmpty()) {
            return;
        }
        Optional<Link> removed = disconnectFirst(level, node.get());
        if (removed.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.netcraft.cable.nothing_to_unplug", NetworkDevices.displayName(node.get())));
            return;
        }
        Link link = removed.get();
        PortRef here = portOn(link, node.get());
        if (!link.cableConsumed() && !player.hasInfiniteMaterials()) {
            player.sendOverlayMessage(Component.translatable("message.netcraft.cable.unplugged",
                    NetworkDevices.displayName(node.get()), here.displayNumber()));
            return;
        }
        PortRef other = link.otherEnd(here);
        ItemStack cable = new ItemStack(ModItems.ETHERNET_CABLE.get());
        GlobalPos otherDevice = GlobalPos.of(level.dimension(), NetworkDevices.posOf(other.device()));
        cable.set(ModDataComponents.CABLE_START.get(), new CableStart(otherDevice, other.index()));
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, cable);
        } else {
            player.getInventory().placeItemBackInInventory(cable, Prediction.SERVER_ONLY);
        }
        Component otherName = NetworkManager.of(level.getServer()).node(other.device())
                .map(NetworkDevices::displayName)
                .orElse(Component.literal("?"));
        player.sendOverlayMessage(Component.translatable("message.netcraft.cable.end_taken",
                NetworkDevices.displayName(node.get()), otherName, other.displayNumber()));
    }

    /** Débranche le câble du premier port occupé de l'appareil. */
    private static Optional<Link> disconnectFirst(ServerLevel level, NetworkNode node) {
        NetworkManager network = NetworkManager.of(level.getServer());
        for (int i = 0; i < node.portCount(); i++) {
            Optional<Link> removed = network.disconnect(node.port(i));
            if (removed.isPresent()) {
                return removed;
            }
        }
        return Optional.empty();
    }

    /** Extrémité du lien qui se trouve sur cet appareil. */
    private static PortRef portOn(Link link, NetworkNode node) {
        return link.a().device().equals(node.id()) ? link.a() : link.b();
    }

    private static Component result(ConnectResult result, double distance) {
        return Component.translatable("message.netcraft.cable.result." + result.key(),
                String.format(Locale.ROOT, "%.1f", distance),
                String.format(Locale.ROOT, "%.0f", MEDIUM.maxLength()));
    }
}
