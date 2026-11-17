package dev.netcraft.gametest;

import java.util.HashSet;
import java.util.Set;

import dev.netcraft.gametest.scene.TrialScenario;
import dev.netcraft.gametest.scene.TrialScenes;
import dev.netcraft.network.DeviceId;
import dev.netcraft.network.Link;
import dev.netcraft.network.NetworkDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Tests en jeu des décors de {@code /netcraft test} : chaque décor se construit avec ses appareils
 * enregistrés, tournés vers le joueur et câblés, et un second décor au même endroit est refusé.
 */
final class SceneTests {
    private SceneTests() {
    }

    static void buildsScenario(GameTestHelper helper, TrialScenario scenario) {
        NetworkScene scene = NetworkScene.of(helper);
        // Le joueur regarderait vers le sud de la zone, depuis son bord nord.
        Direction forward = helper.getAbsoluteDirection(Direction.SOUTH);
        BlockPos origin = helper.absolutePos(new BlockPos(8, 0, 2));

        TrialScenes.Result result = TrialScenes.build(helper.getLevel(), scenario, origin, forward);
        scene.adopt(result.devices());

        helper.assertTrue(result.refusal().isEmpty(), "Décor refusé : " + result.refusal().orElse(""));
        helper.assertValueEqual(result.devices().size(), scenario.devices().size(), "appareils posés");
        Set<Link> links = new HashSet<>();
        for (BlockPos pos : result.devices()) {
            DeviceId id = NetworkDevices.idOf(helper.getLevel(), pos);
            helper.assertTrue(scene.network().node(id).isPresent(), "Appareil non enregistré : " + id);
            helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(HorizontalDirectionalBlock.FACING),
                    forward.getOpposite(), "façade de " + id);
            links.addAll(scene.network().linksOf(id));
        }
        helper.assertValueEqual(links.size(), scenario.cables().size(), "câbles du décor");

        TrialScenes.Result again = TrialScenes.build(helper.getLevel(), scenario, origin, forward);
        helper.assertTrue(again.refusal().isPresent() && again.devices().isEmpty(),
                "Un second décor a été construit sur le premier");
        helper.succeed();
    }
}
