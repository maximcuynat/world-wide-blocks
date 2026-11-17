package dev.netcraft.gametest;

import java.util.List;

import dev.netcraft.network.DeviceId;
import dev.netcraft.network.LinkMedium;
import dev.netcraft.registry.ModBlocks;
import dev.netcraft.registry.ModDataComponents;
import dev.netcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Tests en jeu du cordon Ethernet : branchement, refus, attente annulée. */
final class CableTests {
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

        scene.assertMessages(player, "message.netcraft.cable.no_free_port", 1);
        helper.assertFalse(player.getMainHandItem().has(ModDataComponents.CABLE_START.get()),
                "Un branchement a commencé sur une box sans port libre");
        helper.assertValueEqual(scene.cordsHeldBy(player), 1, "cordons dans l'inventaire");
        helper.assertValueEqual(scene.network().linksOf(box).size(), 4, "câbles de la box");
        helper.succeed();
    }
}
