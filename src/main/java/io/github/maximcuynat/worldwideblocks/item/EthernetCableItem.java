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
 * Ethernet cord: a right-click on a device, then a right-click on another, connects them.
 * Sneaking + right-click on a device, cord in hand: unplugs its cable at both ends.
 * Sneaking + empty-hand right-click: takes off a single end and keeps it in hand (see {@link #takeCableEnd}).
 * Sneaking + right-click in the air: cancels a started connection, or puts away an end held in hand.
 * With a started connection, a right-click on the device where the other end is connected unplugs it.
 *
 * <p>All the logic runs on the server; the client only confirms the click.
 */
public class EthernetCableItem extends Item {
    private static final LinkMedium MEDIUM = LinkMedium.COPPER_ETHERNET;

    public EthernetCableItem(Item.Properties properties) {
        super(properties);
    }

    /** Permanent instructions, too long for the action bar: in the inventory tooltip. */
    @Override
    @SuppressWarnings("deprecation") // marked deprecated by Minecraft for the call, but it is the method to override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.worldwideblocks.ethernet_cable.tooltip.connect").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.worldwideblocks.ethernet_cable.tooltip.unplug").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.worldwideblocks.ethernet_cable.tooltip.cancel").withStyle(ChatFormatting.GRAY));
    }

    /** Connection started: the item glows, like an enchanted item. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.CABLE_START.get()) || super.isFoil(stack);
    }

    /**
     * Called before the block's right-click: with the cord in hand, a click on the laptop connects
     * instead of opening or closing the screen.
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
            // Click on the device where the other end is connected: unplug it, the cord stays free in hand.
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
     * Server side, on every tick, for each cord of an inventory: a pending connection whose
     * start has disappeared (broken, exploded, replaced, including during a disconnection) or whose
     * holder changed dimension is cancelled, and the player is notified. Only the pending state is erased: the
     * cord stays in the inventory, none is created or lost.
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

    /** Sneaking + right-click in the air: cancels the started connection. */
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

        // The first end may have disappeared since the first click: check it again.
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

        // No free port opposite: aim at port 0 to get the "port in use" refusal.
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

    /** Sneaking + right-click, cord in hand: unplugs both ends and puts the cord away. */
    private void unplug(ServerLevel level, Player player, NetworkNode node) {
        Optional<Link> removed = disconnectFirst(level, node);
        if (removed.isEmpty()) {
            tell(player, MessageLevel.ERROR, "message.worldwideblocks.cable.nothing_to_unplug", NetworkDevices.nameArg(node));
            return;
        }
        // Only a cable that consumed a cord gives one back.
        if (removed.get().cableConsumed()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(this), Prediction.SERVER_ONLY);
        }
        tell(player, MessageLevel.SUCCESS, "message.worldwideblocks.cable.unplugged",
                NetworkDevices.nameArg(node), MessageArg.number(portOn(removed.get(), node).displayNumber()));
    }

    /**
     * Sneaking + empty-hand right-click on a device: takes the end of its cable off this device
     * and keeps it in hand, the other end staying connected. The cord in hand carries this other
     * end like a started connection: a right-click on a device reconnects it,
     * sneaking + click in the air also unplugs it from the other end and puts it away.
     *
     * <p>A cable placed in creative (no cord consumed) does not give a cord in survival: it is
     * simply unplugged at both ends, otherwise free cords would be made.
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

    /** Unplugs the cable from the first occupied port of the device. */
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

    /** End of the link that is on this device. */
    private static PortRef portOn(Link link, NetworkNode node) {
        return link.a().device().equals(node.id()) ? link.a() : link.b();
    }

    /**
     * Answer to the player's gesture, through the message module. Levels: success is a success;
     * a step or a cancellation wanted by the player, an info; a cancellation caused by
     * something else (start gone, dimension), a warning; a refused gesture, an error.
     */
    private static void tell(Player player, MessageLevel level, String key, MessageArg... args) {
        if (player instanceof ServerPlayer serverPlayer) {
            MessageCenter.of(serverPlayer.level().getServer()).gesture(serverPlayer, Message.of(level, key, args));
        }
    }

    /**
     * Engine refusal at the second click. "Port in use" targets the end device (the start is
     * checked before): it is the same message as at the first click on a full device. The distance
     * is only useful for the "too far" refusal.
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
