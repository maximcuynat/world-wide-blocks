package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialSceneCommand;
import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenario;
import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenes;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevices;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Tests en jeu des décors de {@code /worldwideblocks test} : chaque décor se construit avec ses appareils
 * enregistrés, tournés vers le joueur et câblés, et un second décor au même endroit est refusé. La
 * commande elle-même refuse de construire hors d'un monde d'essai.
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

    /**
     * Règle du nom de monde, puis la vraie commande sur le serveur de tests, dont le monde ne s'appelle
     * pas « Essais… » : refus avec un message, aucun bloc posé.
     */
    static void commandRefusedOutsideTrialWorld(GameTestHelper helper) {
        helper.assertTrue(TrialSceneCommand.isTrialWorldName("Essais 1b-1"), "« Essais 1b-1 » doit être un monde d'essai");
        helper.assertTrue(TrialSceneCommand.isTrialWorldName("Essais"), "« Essais » doit être un monde d'essai");
        helper.assertFalse(TrialSceneCommand.isTrialWorldName("World Wide Blocks"), "« World Wide Blocks » ne doit pas être un monde d'essai");
        helper.assertFalse(TrialSceneCommand.isTrialWorldName("Mes Essais"), "« Mes Essais » ne doit pas être un monde d'essai");
        helper.assertFalse(TrialSceneCommand.isTrialWorldName("essais 1b-1"), "« essais 1b-1 » ne doit pas être un monde d'essai");

        MinecraftServer server = helper.getLevel().getServer();
        String worldName = server.getWorldData().getLevelName();
        helper.assertFalse(TrialSceneCommand.isTrialWorldName(worldName),
                "Le monde du serveur de tests s'appelle « " + worldName + " » : le test suppose un monde hors essai");

        NetworkScene scene = NetworkScene.of(helper);
        TestPlayer player = scene.player(GameType.CREATIVE);
        BlockPos wouldBeBox = player.blockPosition().relative(player.getDirection(), TrialScenes.DISTANCE_FROM_PLAYER);
        List<Component> received = new ArrayList<>();
        CommandSource capture = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                received.add(message);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        // Opérateur, pour que seul le nom du monde puisse refuser la commande.
        CommandSourceStack source = player.createCommandSourceStack()
                .withPermission(LevelBasedPermissionSet.OWNER)
                .withSource(capture);

        // The full command, then the /wwb alias: the alias must reach the same "test" subcommand.
        for (String command : List.of("worldwideblocks test box", "wwb test box")) {
            received.clear();
            int result;
            try {
                result = server.getCommands().getDispatcher().execute(command, source);
            } catch (CommandSyntaxException e) {
                throw helper.assertionException(Component.literal("Commande non reconnue (" + command + ") : " + e.getMessage()));
            }

            helper.assertValueEqual(result, 0, "résultat de la commande " + command);
            helper.assertValueEqual(received.size(), 1, "messages reçus pour " + command);
            String message = received.get(0).getString();
            helper.assertTrue(message.contains(worldName) && message.contains(TrialSceneCommand.TRIAL_WORLD_PREFIX),
                    "Message de refus inattendu pour " + command + " : " + message);
            helper.assertTrue(helper.getLevel().isEmptyBlock(wouldBeBox), "Un bloc a été posé malgré le refus en " + wouldBeBox);
        }
        helper.succeed();
    }
}
