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

/** Tests en jeu du cordon Ethernet : branchement refusé, branchement en attente annulé. */
final class CableTests {
    private static final String START_GONE = "message.worldwideblocks.cable.start_gone";
    private static final String CANCELLED_DIMENSION = "message.worldwideblocks.cable.cancelled_dimension";

    /** Box A, point de départ de l'attente. */
    private static final BlockPos START = new BlockPos(4, 0, 8);
    /** Tour B, dont on retire le bout. */
    private static final BlockPos END = new BlockPos(12, 0, 8);

    private CableTests() {
    }

    /**
     * Box aux 4 ports réseau local occupés : un cordon de plus est refusé avec un message, sans
     * branchement commencé ni cordon perdu, et le port de ligne n'est jamais pris pour un port libre.
     */
    static void noFreePort(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        BlockPos boxPos = new BlockPos(8, 0, 8);
        DeviceId box = scene.place(boxPos, ModBlocks.INTERNET_BOX.get());
        for (BlockPos pos : List.of(new BlockPos(4, 0, 4), new BlockPos(12, 0, 4), new BlockPos(4, 0, 12), new BlockPos(12, 0, 12))) {
            scene.cable(box, scene.place(pos, ModBlocks.PC_TOWER.get()));
        }
        helper.assertTrue(scene.network().firstFreePort(box, LinkMedium.COPPER_ETHERNET).isEmpty(),
                "La box a encore un port libre pour un câble Ethernet : le port de ligne a-t-il été compté ?");

        TestPlayer player = scene.player(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ETHERNET_CABLE.get()));
        scene.rightClick(player, boxPos);

        scene.assertMessages(player, MessageChannel.ACTION_BAR, "message.worldwideblocks.cable.no_free_port", 1);
        helper.assertFalse(player.getMainHandItem().has(ModDataComponents.CABLE_START.get()),
                "Un branchement a commencé sur une box sans port libre");
        helper.assertValueEqual(scene.cordsHeldBy(player), 1, "cordons dans l'inventaire");
        helper.assertValueEqual(scene.network().linksOf(box).size(), 4, "câbles de la box");
        helper.succeed();
    }

    /** Bug du 2026-11-17, en créatif : le départ de l'attente est cassé par le joueur. */
    static void startBrokenCreative(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        TestPlayer player = pendingFromStart(scene, GameType.CREATIVE);
        scene.breakAsPlayer(player, START);
        helper.succeedWhen(() -> assertCancelled(scene, player, START_GONE));
    }

    /** Même bug en survie : le cordon branché puis repris n'est ni perdu ni dupliqué. */
    static void startBrokenSurvival(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        TestPlayer player = pendingFromStart(scene, GameType.SURVIVAL);
        scene.breakAsPlayer(player, START);
        helper.succeedWhen(() -> assertCancelled(scene, player, START_GONE));
    }

    /** Le départ de l'attente explose (cas relevé en survie le 2026-11-17 à 20:04). */
    static void startExplodedSurvival(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        TestPlayer player = pendingFromStart(scene, GameType.SURVIVAL);
        scene.explodeAt(START, 2.0F);
        helper.assertTrue(scene.network().node(scene.id(START)).isEmpty(), "La box a survécu à l'explosion");
        helper.succeedWhen(() -> assertCancelled(scene, player, START_GONE));
    }

    /** Le porteur passe dans le Nether pendant l'attente : elle est annulée, le cordon reste libre. */
    static void dimensionChange(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        TestPlayer player = pendingFromStart(scene, GameType.CREATIVE);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Pas de Nether dans le monde de test");
        player.teleport(new TeleportTransition(nether, new Vec3(0.5, 80.0, 0.5), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
        helper.succeedWhen(() -> {
            helper.assertTrue(player.level() == nether, "Le joueur n'est pas dans le Nether");
            assertCancelled(scene, player, CANCELLED_DIMENSION);
        });
    }

    /**
     * Le joueur se déconnecte pendant l'attente, le départ est cassé en son absence : à la
     * reconnexion, le cordon relu de la sauvegarde du joueur voit que le départ n'existe plus.
     */
    static void startBrokenWhileDisconnected(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        scene.place(START, ModBlocks.INTERNET_BOX.get());
        UUID id = UUID.randomUUID();
        TestPlayer player = scene.player(id, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ETHERNET_CABLE.get()));
        scene.rightClick(player, START);
        helper.assertTrue(scene.hasPendingCord(player), "Le premier clic n'a pas commencé de branchement");

        player.leaveServer();
        helper.destroyBlock(START);
        TestPlayer back = scene.player(id, GameType.SURVIVAL);
        helper.assertTrue(back != player, "La reconnexion a réutilisé le même joueur");
        helper.succeedWhen(() -> assertCancelled(scene, back, START_GONE));
    }

    /**
     * Étapes 1 à 3 du bug : box A reliée à la tour B, bout retiré de B (le cordon attend, accroché à
     * A), puis changement d'emplacement dans la barre d'objets. En survie, le câble est branché par le
     * joueur avec son unique cordon, comme en jeu.
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
            helper.assertValueEqual(scene.cordsHeldBy(player), 0, "cordons après un branchement en survie");
        } else {
            scene.cable(box, tower);
        }
        helper.assertValueEqual(scene.network().linksOf(box).size(), 1, "câbles de la box avant le retrait");

        // Main vide, accroupi + clic droit sur B : le bout est retiré et reste en main.
        scene.sneakRightClick(player, END);
        scene.assertMessages(player, MessageChannel.ACTION_BAR, "message.worldwideblocks.cable.end_taken", 1);
        helper.assertTrue(scene.hasPendingCord(player), "Le bout retiré n'attend pas d'être rebranché");
        helper.assertValueEqual(scene.cordsHeldBy(player), 1, "cordons après le retrait du bout");
        helper.assertValueEqual(scene.network().linksOf(box).size(), 0, "câbles de la box après le retrait");

        player.getInventory().setSelectedSlot(1);
        return player;
    }

    /** L'attente est annulée, le joueur prévenu une fois, et il a toujours exactement un cordon. */
    private static void assertCancelled(NetworkScene scene, TestPlayer player, String messageKey) {
        GameTestHelper helper = scene.helper();
        helper.assertFalse(scene.hasPendingCord(player), "Le cordon attend encore d'être branché");
        scene.assertMessages(player, MessageChannel.ACTION_BAR, messageKey, 1);
        helper.assertValueEqual(scene.cordsHeldBy(player), 1, "cordons dans l'inventaire");
        helper.assertValueEqual(scene.cordsOnGround(), 0, "cordons au sol");
    }
}
