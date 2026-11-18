package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.List;
import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.message.MessageChannel;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.LinkMedium;
import io.github.maximcuynat.worldwideblocks.registry.ModBlocks;
import io.github.maximcuynat.worldwideblocks.registry.ModDataComponents;
import io.github.maximcuynat.worldwideblocks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Game tests of the Ethernet cord: refused connection, cancelled pending connection. */
final class CableTests {
    private static final String START_GONE = "message.worldwideblocks.cable.start_gone";
    private static final String CANCELLED_DIMENSION = "message.worldwideblocks.cable.cancelled_dimension";

    /** Box A, the start of the pending connection. */
    private static final BlockPos START = new BlockPos(4, 0, 8);
    /** PC tower B, whose end is taken off. */
    private static final BlockPos END = new BlockPos(12, 0, 8);

    private CableTests() {
    }

    /**
     * Box with its 4 local network ports in use: one more cord is refused with a message, with no
     * connection started and no cord lost, and the line port is never taken for a free port.
     */
    static void noFreePort(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        BlockPos boxPos = new BlockPos(8, 0, 8);
        DeviceId box = scene.place(boxPos, ModBlocks.INTERNET_BOX.get());
        for (BlockPos pos : List.of(new BlockPos(4, 0, 4), new BlockPos(12, 0, 4), new BlockPos(4, 0, 12), new BlockPos(12, 0, 12))) {
            scene.cable(box, scene.place(pos, ModBlocks.PC_TOWER.get()));
        }
        helper.assertTrue(scene.network().firstFreePort(box, LinkMedium.COPPER_ETHERNET).isEmpty(),
                "The box still has a free port for an Ethernet cable: was the line port counted?");

        TestPlayer player = scene.player(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ETHERNET_CABLE.get()));
        scene.rightClick(player, boxPos);

        scene.assertMessages(player, MessageChannel.ACTION_BAR, "message.worldwideblocks.cable.no_free_port", 1);
        helper.assertFalse(player.getMainHandItem().has(ModDataComponents.CABLE_START.get()),
                "A connection started on a box with no free port");
        helper.assertValueEqual(scene.cordsHeldBy(player), 1, "cords in the inventory");
        helper.assertValueEqual(scene.network().linksOf(box).size(), 4, "cables of the box");
        helper.succeed();
    }

    /** Bug of 2026-11-17, in creative: the start of the pending connection is broken by the player. */
    static void startBrokenCreative(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        TestPlayer player = pendingFromStart(scene, GameType.CREATIVE);
        scene.breakAsPlayer(player, START);
        helper.succeedWhen(() -> assertCancelled(scene, player, START_GONE));
    }

    /** Same bug in survival: the cord that was plugged in then taken back is neither lost nor duplicated. */
    static void startBrokenSurvival(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        TestPlayer player = pendingFromStart(scene, GameType.SURVIVAL);
        scene.breakAsPlayer(player, START);
        helper.succeedWhen(() -> assertCancelled(scene, player, START_GONE));
    }

    /** The start of the pending connection explodes (case seen in survival on 2026-11-17 at 20:04). */
    static void startExplodedSurvival(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        TestPlayer player = pendingFromStart(scene, GameType.SURVIVAL);
        scene.explodeAt(START, 2.0F);
        helper.assertTrue(scene.network().node(scene.id(START)).isEmpty(), "The box survived the explosion");
        helper.succeedWhen(() -> assertCancelled(scene, player, START_GONE));
    }

    /** The holder goes through to the Nether while waiting: the wait is cancelled, the cord stays free. */
    static void dimensionChange(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        TestPlayer player = pendingFromStart(scene, GameType.CREATIVE);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "No Nether in the test world");
        player.teleport(new TeleportTransition(nether, new Vec3(0.5, 80.0, 0.5), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
        helper.succeedWhen(() -> {
            helper.assertTrue(player.level() == nether, "The player is not in the Nether");
            assertCancelled(scene, player, CANCELLED_DIMENSION);
        });
    }

    /**
     * The player disconnects while waiting and the start is broken in their absence: on
     * reconnection, the cord read back from the player's save sees that the start no longer exists.
     */
    static void startBrokenWhileDisconnected(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        scene.place(START, ModBlocks.INTERNET_BOX.get());
        UUID id = UUID.randomUUID();
        TestPlayer player = scene.player(id, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ETHERNET_CABLE.get()));
        scene.rightClick(player, START);
        helper.assertTrue(scene.hasPendingCord(player), "The first click did not start a connection");

        player.leaveServer();
        helper.destroyBlock(START);
        TestPlayer back = scene.player(id, GameType.SURVIVAL);
        helper.assertTrue(back != player, "The reconnection reused the same player");
        helper.succeedWhen(() -> assertCancelled(scene, back, START_GONE));
    }

    /**
     * Steps 1 to 3 of the bug: box A linked to PC tower B, end taken off B (the cord waits, hooked to
     * A), then a change of slot in the hotbar. In survival, the cable is plugged in by the player
     * with their only cord, as in the game.
     */
    private static TestPlayer pendingFromStart(NetworkScene scene, GameType gameType) {
        GameTestHelper helper = scene.helper();
        DeviceId box = scene.place(START, ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(END, ModBlocks.PC_TOWER.get());
        TestPlayer player = scene.player(gameType);
        if (gameType == GameType.SURVIVAL) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ETHERNET_CABLE.get()));
            scene.rightClick(player, START);
            scene.rightClick(player, END);
            helper.assertValueEqual(scene.cordsHeldBy(player), 0, "cords after a connection in survival");
        } else {
            scene.cable(box, tower);
        }
        helper.assertValueEqual(scene.network().linksOf(box).size(), 1, "cables of the box before the removal");

        // Empty hand, sneaking + right-click on B: the end is taken off and stays in hand.
        scene.sneakRightClick(player, END);
        scene.assertMessages(player, MessageChannel.ACTION_BAR, "message.worldwideblocks.cable.end_taken", 1);
        helper.assertTrue(scene.hasPendingCord(player), "The end taken off is not waiting to be plugged in again");
        helper.assertValueEqual(scene.cordsHeldBy(player), 1, "cords after the end was taken off");
        helper.assertValueEqual(scene.network().linksOf(box).size(), 0, "cables of the box after the removal");

        player.getInventory().setSelectedSlot(1);
        return player;
    }

    /** The wait is cancelled, the player is told once, and still has exactly one cord. */
    private static void assertCancelled(NetworkScene scene, TestPlayer player, String messageKey) {
        GameTestHelper helper = scene.helper();
        helper.assertFalse(scene.hasPendingCord(player), "The cord is still waiting to be plugged in");
        scene.assertMessages(player, MessageChannel.ACTION_BAR, messageKey, 1);
        helper.assertValueEqual(scene.cordsHeldBy(player), 1, "cords in the inventory");
        helper.assertValueEqual(scene.cordsOnGround(), 0, "cords on the ground");
    }
}
