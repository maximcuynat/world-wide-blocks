package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.item.CableStart;
import io.github.maximcuynat.worldwideblocks.message.MessageChannel;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.LinkMedium;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.registry.ModBlocks;
import io.github.maximcuynat.worldwideblocks.registry.ModDataComponents;
import io.github.maximcuynat.worldwideblocks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Game tests of the port chosen by aiming, server side (issue #62): the four gestures follow the aimed port (start,
 * finish, unplug with a cord, take an end off with an empty hand); elsewhere on the device, the first free port; an
 * aimed port in use, an aimed port without a cable and the aimed line input of the box are refused with a message.
 */
final class PortAimTests {
    private static final BlockPos BOX = new BlockPos(4, 0, 8);
    private static final BlockPos SWITCH = new BlockPos(12, 0, 8);
    private static final BlockPos TOWER_A = new BlockPos(4, 0, 4);
    private static final BlockPos TOWER_B = new BlockPos(12, 0, 4);

    private PortAimTests() {
    }

    /** Cord in hand: a click on port 3 of the box, then on port 4 of the switch, links exactly these two ports. */
    static void bothEndsFollowTheAimedPorts(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(BOX, ModBlocks.INTERNET_BOX.get());
        DeviceId sw = scene.place(SWITCH, ModBlocks.DESKTOP_SWITCH_5.get());
        TestPlayer player = scene.player(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ETHERNET_CABLE.get()));

        scene.rightClickPort(player, BOX, 2);
        CableStart start = player.getMainHandItem().get(ModDataComponents.CABLE_START.get());
        helper.assertTrue(start != null, "no connection started on the aimed port");
        helper.assertValueEqual(start.port(), 2, "start port (port 3 aimed)");

        scene.rightClickPort(player, SWITCH, 3);
        Optional<Link> link = scene.network().linkAt(new PortRef(box, 2));
        helper.assertTrue(link.isPresent(), "no cable on port 3 of the box");
        helper.assertValueEqual(link.get().otherEnd(new PortRef(box, 2)), new PortRef(sw, 3), "other end (port 4 of the switch aimed)");
        helper.assertValueEqual(scene.network().linksOf(sw).size(), 1, "cables of the switch");
        helper.succeed();
    }

    /** A click outside every zone (the top of the box, the middle of the block): the first free port, as before. */
    static void outsideTheZonesTheFirstFreePort(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(BOX, ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(TOWER_A, ModBlocks.PC_TOWER.get());
        scene.network().connect(new PortRef(box, 0), new PortRef(tower, 0), LinkMedium.COPPER_ETHERNET, false);
        TestPlayer player = scene.player(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ETHERNET_CABLE.get()));
        scene.rightClick(player, BOX);
        CableStart start = player.getMainHandItem().get(ModDataComponents.CABLE_START.get());
        helper.assertTrue(start != null, "no connection started");
        helper.assertValueEqual(start.port(), 1, "start port: the first free one");
        helper.succeed();
    }

    /** An aimed port with a cable: "port N: already in use", no connection started, at the first and at the second click. */
    static void aimedPortInUse(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(BOX, ModBlocks.INTERNET_BOX.get());
        DeviceId sw = scene.place(SWITCH, ModBlocks.DESKTOP_SWITCH_5.get());
        DeviceId tower = scene.place(TOWER_A, ModBlocks.PC_TOWER.get());
        DeviceId other = scene.place(TOWER_B, ModBlocks.PC_TOWER.get());
        scene.network().connect(new PortRef(box, 1), new PortRef(tower, 0), LinkMedium.COPPER_ETHERNET, false);
        scene.network().connect(new PortRef(sw, 2), new PortRef(other, 0), LinkMedium.COPPER_ETHERNET, false);
        TestPlayer player = scene.player(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ETHERNET_CABLE.get()));

        scene.rightClickPort(player, BOX, 1);
        scene.assertMessages(player, MessageChannel.ACTION_BAR, "message.worldwideblocks.cable.port_in_use", 1);
        helper.assertFalse(scene.hasPendingCord(player), "a connection started on a port in use");

        scene.rightClickPort(player, BOX, 0);
        helper.assertTrue(scene.hasPendingCord(player), "no connection started on the free port 1");
        scene.rightClickPort(player, SWITCH, 2);
        scene.assertMessages(player, MessageChannel.ACTION_BAR, "message.worldwideblocks.cable.port_in_use", 2);
        helper.assertTrue(scene.hasPendingCord(player), "the pending connection was lost on a refused second click");
        helper.assertValueEqual(scene.network().linksOf(sw).size(), 1, "cables of the switch");
        helper.assertValueEqual(scene.cordsHeldBy(player), 1, "cords in the inventory");
        helper.succeed();
    }

    /** The line input of the box, aimed with an Ethernet cord: "This port does not accept this cable." */
    static void aimedLineInputRefused(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        scene.place(BOX, ModBlocks.INTERNET_BOX.get());
        TestPlayer player = scene.player(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ETHERNET_CABLE.get()));
        scene.rightClickPort(player, BOX, 4);
        scene.assertMessages(player, MessageChannel.ACTION_BAR, "message.worldwideblocks.cable.result.incompatible_port", 1);
        helper.assertFalse(scene.hasPendingCord(player), "a connection started on the line input");
        helper.succeed();
    }

    /** Sneak + click with a cord on port 3: that cable is unplugged, the one of port 1 stays; an empty aimed port says so. */
    static void unplugTheAimedPort(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(BOX, ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(TOWER_A, ModBlocks.PC_TOWER.get());
        DeviceId other = scene.place(TOWER_B, ModBlocks.PC_TOWER.get());
        scene.network().connect(new PortRef(box, 0), new PortRef(tower, 0), LinkMedium.COPPER_ETHERNET, false);
        scene.network().connect(new PortRef(box, 2), new PortRef(other, 0), LinkMedium.COPPER_ETHERNET, false);
        TestPlayer player = scene.player(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ETHERNET_CABLE.get()));

        scene.sneakRightClickPort(player, BOX, 2);
        helper.assertTrue(scene.network().linkAt(new PortRef(box, 2)).isEmpty(), "port 3 still has its cable");
        helper.assertTrue(scene.network().linkAt(new PortRef(box, 0)).isPresent(), "port 1 lost its cable");
        scene.assertMessages(player, MessageChannel.ACTION_BAR, "message.worldwideblocks.cable.unplugged", 1);

        scene.sneakRightClickPort(player, BOX, 3);
        scene.assertMessages(player, MessageChannel.ACTION_BAR, "message.worldwideblocks.cable.port_empty", 1);
        helper.assertTrue(scene.network().linkAt(new PortRef(box, 0)).isPresent(), "port 1 lost its cable on an empty aimed port");
        helper.succeed();
    }

    /** Sneak + click with an empty hand on port 3 of the switch: that end is taken off and held, the other cable stays. */
    static void takeTheEndOfTheAimedPort(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId sw = scene.place(SWITCH, ModBlocks.DESKTOP_SWITCH_5.get());
        DeviceId tower = scene.place(TOWER_A, ModBlocks.PC_TOWER.get());
        DeviceId other = scene.place(TOWER_B, ModBlocks.PC_TOWER.get());
        scene.network().connect(new PortRef(sw, 0), new PortRef(tower, 0), LinkMedium.COPPER_ETHERNET, true);
        scene.network().connect(new PortRef(sw, 2), new PortRef(other, 0), LinkMedium.COPPER_ETHERNET, true);
        TestPlayer player = scene.player(GameType.SURVIVAL);

        scene.sneakRightClickPort(player, SWITCH, 2);
        helper.assertTrue(scene.network().linkAt(new PortRef(sw, 2)).isEmpty(), "port 3 still has its cable");
        helper.assertTrue(scene.network().linkAt(new PortRef(sw, 0)).isPresent(), "port 1 lost its cable");
        CableStart held = player.getMainHandItem().get(ModDataComponents.CABLE_START.get());
        helper.assertTrue(held != null, "no end held in hand");
        helper.assertValueEqual(held.portRef(), new PortRef(other, 0), "end held: the other end of the cable of port 3");
        scene.assertMessages(player, MessageChannel.ACTION_BAR, "message.worldwideblocks.cable.end_taken", 1);
        helper.succeed();
    }
}
