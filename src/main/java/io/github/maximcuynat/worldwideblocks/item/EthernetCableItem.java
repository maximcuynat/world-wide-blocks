package io.github.maximcuynat.worldwideblocks.item;

import java.util.Optional;
import java.util.function.Consumer;

import io.github.maximcuynat.worldwideblocks.message.Message;
import io.github.maximcuynat.worldwideblocks.message.MessageArg;
import io.github.maximcuynat.worldwideblocks.message.MessageLevel;
import io.github.maximcuynat.worldwideblocks.message.server.MessageCenter;
import io.github.maximcuynat.worldwideblocks.network.ConnectOutcome;
import io.github.maximcuynat.worldwideblocks.network.ConnectResult;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.LinkMedium;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevice;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevices;
import io.github.maximcuynat.worldwideblocks.network.NetworkManager;
import io.github.maximcuynat.worldwideblocks.network.NetworkNode;
import io.github.maximcuynat.worldwideblocks.network.PendingCable;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.registry.ModDataComponents;
import io.github.maximcuynat.worldwideblocks.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

/**
 * Cordon Ethernet : un clic droit sur un appareil, puis un clic droit sur un autre, les relie.
 * Accroupi + clic droit sur un appareil, cordon en main : débranche son câble des deux bouts.
 * Accroupi + clic droit main vide : retire un seul bout et le garde en main (voir {@link #takeCableEnd}).
 * Accroupi + clic droit dans le vide : annule un branchement commencé, ou range un bout tenu en main.
 * Avec un branchement commencé, un clic droit sur l'appareil où l'autre bout est branché le débranche.
 *
 * <p>Toute la logique tourne sur le serveur ; le client se contente de confirmer le clic.
 */
public class EthernetCableItem extends Item {
    private static final LinkMedium MEDIUM = LinkMedium.COPPER_ETHERNET;

    public EthernetCableItem(Item.Properties properties) {
        super(properties);
    }

    /** Consignes permanentes, trop longues pour la barre d'action : dans l'infobulle de l'inventaire. */
    @Override
    @SuppressWarnings("deprecation") // marquée obsolète par Minecraft pour l'appel, mais c'est la méthode à surcharger
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.worldwideblocks.ethernet_cable.tooltip.connect").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.worldwideblocks.ethernet_cable.tooltip.unplug").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.worldwideblocks.ethernet_cable.tooltip.cancel").withStyle(ChatFormatting.GRAY));
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
        CableStart pending = stack.get(ModDataComponents.CABLE_START.get());
        if (pending != null && pending.device().equals(GlobalPos.of(level.dimension(), pos))) {
            // Clic sur l'appareil où l'autre bout est branché : on le débranche, le cordon reste libre en main.
            stack.remove(ModDataComponents.CABLE_START.get());
            tell(player, MessageLevel.SUCCESS, "message.worldwideblocks.cable.retracted", NetworkDevices.nameArg(node.get()));
        } else if (player.isSecondaryUseActive()) {
            this.unplug(serverLevel, player, node.get());
        } else if (pending != null) {
            this.finish(serverLevel, player, stack, node.get());
        } else {
            this.start(serverLevel, player, stack, node.get());
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Côté serveur, à chaque tick, pour chaque cordon d'un inventaire : un branchement en attente dont
     * le départ a disparu (cassé, explosé, remplacé, y compris pendant une déconnexion) ou dont le
     * porteur a changé de dimension est annulé, et le joueur prévenu. Seule l'attente est effacée : le
     * cordon reste dans l'inventaire, aucun n'est créé ni perdu.
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        CableStart pending = stack.get(ModDataComponents.CABLE_START.get());
        if (pending == null) {
            return;
        }
        String holderDimension = level.dimension().identifier().toString();
        PendingCable.Status status = NetworkManager.of(level.getServer()).pendingCable(pending.portRef(), holderDimension);
        if (status == PendingCable.Status.VALID) {
            return;
        }
        stack.remove(ModDataComponents.CABLE_START.get());
        if (owner instanceof Player player) {
            tell(player, MessageLevel.WARNING, status == PendingCable.Status.START_GONE
                    ? "message.worldwideblocks.cable.start_gone"
                    : "message.worldwideblocks.cable.cancelled_dimension");
        }
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
            tell(player, MessageLevel.INFO, "message.worldwideblocks.cable.cancelled");
        }
        return InteractionResult.SUCCESS;
    }

    private void start(ServerLevel level, Player player, ItemStack stack, NetworkNode node) {
        NetworkManager network = NetworkManager.of(level.getServer());
        Optional<PortRef> port = network.firstFreePort(node.id(), MEDIUM);
        if (port.isEmpty()) {
            tell(player, MessageLevel.ERROR, "message.worldwideblocks.cable.no_free_port", NetworkDevices.nameArg(node));
            return;
        }
        GlobalPos device = GlobalPos.of(level.dimension(), NetworkDevices.posOf(node.id()));
        stack.set(ModDataComponents.CABLE_START.get(), new CableStart(device, port.get().index()));
        tell(player, MessageLevel.INFO, "message.worldwideblocks.cable.start",
                NetworkDevices.nameArg(node), MessageArg.number(port.get().displayNumber()));
    }

    private void finish(ServerLevel level, Player player, ItemStack stack, NetworkNode end) {
        NetworkManager network = NetworkManager.of(level.getServer());
        CableStart pending = stack.get(ModDataComponents.CABLE_START.get());
        DeviceId startId = pending.portRef().device();

        // La première extrémité a pu disparaître depuis le premier clic : on la revérifie.
        if (!startId.sameDimension(end.id())) {
            refuse(player, ConnectResult.OTHER_DIMENSION, end, 0);
            return;
        }
        Optional<NetworkNode> start = NetworkDevices.validate(level, startId);
        if (start.isEmpty() || !start.get().hasPort(pending.port())) {
            stack.remove(ModDataComponents.CABLE_START.get());
            tell(player, MessageLevel.WARNING, "message.worldwideblocks.cable.start_gone");
            return;
        }
        PortRef startPort = start.get().port(pending.port());
        if (network.linkAt(startPort).isPresent()) {
            stack.remove(ModDataComponents.CABLE_START.get());
            tell(player, MessageLevel.WARNING, "message.worldwideblocks.cable.start_taken");
            return;
        }

        // Pas de port libre en face : on vise le port 0 pour obtenir le refus « port occupé ».
        PortRef endPort = network.firstFreePort(end.id(), MEDIUM).orElse(end.port(0));
        boolean consume = !player.hasInfiniteMaterials();
        ConnectOutcome outcome = network.connect(startPort, endPort, MEDIUM, consume);
        if (!outcome.isSuccess()) {
            refuse(player, outcome.result(), end, startId.distanceTo(end.id()));
            return;
        }
        Link link = outcome.link().orElseThrow();
        stack.remove(ModDataComponents.CABLE_START.get());
        if (consume) {
            stack.shrink(1);
        }
        tell(player, MessageLevel.SUCCESS, "message.worldwideblocks.cable.connected",
                NetworkDevices.nameArg(start.get()), MessageArg.number(startPort.displayNumber()),
                NetworkDevices.nameArg(end), MessageArg.number(endPort.displayNumber()),
                MessageArg.blocks(link.length()));
    }

    /** Accroupi + clic droit, cordon en main : débranche les deux bouts et range le cordon. */
    private void unplug(ServerLevel level, Player player, NetworkNode node) {
        Optional<Link> removed = disconnectFirst(level, node);
        if (removed.isEmpty()) {
            tell(player, MessageLevel.ERROR, "message.worldwideblocks.cable.nothing_to_unplug", NetworkDevices.nameArg(node));
            return;
        }
        // Seul un câble qui a consommé un cordon en rend un.
        if (removed.get().cableConsumed()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(this), Prediction.SERVER_ONLY);
        }
        tell(player, MessageLevel.SUCCESS, "message.worldwideblocks.cable.unplugged",
                NetworkDevices.nameArg(node), MessageArg.number(portOn(removed.get(), node).displayNumber()));
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
            tell(player, MessageLevel.ERROR, "message.worldwideblocks.cable.nothing_to_unplug", NetworkDevices.nameArg(node.get()));
            return;
        }
        Link link = removed.get();
        PortRef here = portOn(link, node.get());
        if (!link.cableConsumed() && !player.hasInfiniteMaterials()) {
            tell(player, MessageLevel.SUCCESS, "message.worldwideblocks.cable.unplugged",
                    NetworkDevices.nameArg(node.get()), MessageArg.number(here.displayNumber()));
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
        MessageArg otherName = NetworkManager.of(level.getServer()).node(other.device())
                .map(NetworkDevices::nameArg)
                .orElse(MessageArg.text("?"));
        tell(player, MessageLevel.INFO, "message.worldwideblocks.cable.end_taken",
                NetworkDevices.nameArg(node.get()), otherName, MessageArg.number(other.displayNumber()));
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

    /**
     * Réponse au geste du joueur, par le module des messages. Niveaux : la réussite est un succès ;
     * une étape ou une annulation voulue par le joueur, une information ; une annulation causée par
     * autre chose (départ disparu, dimension), un avertissement ; un geste refusé, une erreur.
     */
    private static void tell(Player player, MessageLevel level, String key, MessageArg... args) {
        if (player instanceof ServerPlayer serverPlayer) {
            MessageCenter.of(serverPlayer.level().getServer()).gesture(serverPlayer, Message.of(level, key, args));
        }
    }

    /**
     * Refus du moteur au second clic. « Port occupé » vise l'appareil d'arrivée (le départ est
     * vérifié avant) : c'est le même message qu'au premier clic sur un appareil plein. La distance
     * n'est utile qu'au refus « trop loin ».
     */
    private static void refuse(Player player, ConnectResult result, NetworkNode end, double distance) {
        if (result == ConnectResult.PORT_IN_USE) {
            tell(player, MessageLevel.ERROR, "message.worldwideblocks.cable.no_free_port", NetworkDevices.nameArg(end));
            return;
        }
        tell(player, MessageLevel.ERROR, "message.worldwideblocks.cable.result." + result.key(),
                MessageArg.blocks(distance), MessageArg.blocks(MEDIUM.maxLength()));
    }
}
