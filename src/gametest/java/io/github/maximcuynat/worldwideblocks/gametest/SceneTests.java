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
 * Game tests of the scenes of {@code /worldwideblocks test}: each scene is built with its devices
 * registered, turned towards the player and cabled, and a second scene at the same place is refused.
 * The command itself refuses to build outside a trial world.
 */
final class SceneTests {
    private SceneTests() {
    }

    static void buildsScenario(GameTestHelper helper, TrialScenario scenario) {
        NetworkScene scene = NetworkScene.of(helper);
        // The player would look towards the south of the area, from its north edge.
        Direction forward = helper.getAbsoluteDirection(Direction.SOUTH);
        BlockPos origin = helper.absolutePos(new BlockPos(8, 0, 2));

        TrialScenes.Result result = TrialScenes.build(helper.getLevel(), scenario, origin, forward);
        scene.adopt(result.devices());

        helper.assertTrue(result.refusal().isEmpty(), "Scene refused: " + result.refusal().orElse(""));
        helper.assertValueEqual(result.devices().size(), scenario.devices().size(), "devices placed");
        Set<Link> links = new HashSet<>();
        for (BlockPos pos : result.devices()) {
            DeviceId id = NetworkDevices.idOf(helper.getLevel(), pos);
            helper.assertTrue(scene.network().node(id).isPresent(), "Device not registered: " + id);
            helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(HorizontalDirectionalBlock.FACING),
                    forward.getOpposite(), "front of " + id);
            links.addAll(scene.network().linksOf(id));
        }
        helper.assertValueEqual(links.size(), scenario.cables().size(), "cables of the scene");

        TrialScenes.Result again = TrialScenes.build(helper.getLevel(), scenario, origin, forward);
        helper.assertTrue(again.refusal().isPresent() && again.devices().isEmpty(),
                "A second scene was built on the first one");
        helper.succeed();
    }

    /**
     * Rule of the world name, then the real command on the game test server, whose world ("Test Level")
     * does not start with "Trials": refusal with a message, no block placed, through the full command
     * and through the /wwb alias.
     */
    static void commandRefusedOutsideTrialWorld(GameTestHelper helper) {
        for (String accepted : List.of("Trials 1b-1", "Trials")) {
            helper.assertTrue(TrialSceneCommand.isTrialWorldName(accepted), "\"" + accepted + "\" must be a trial world");
        }
        // "Test Level" is the world of the game test server: it must never count as a trial world.
        for (String refused : List.of("World Wide Blocks", "My Trials", "trials 1b-1", "Test Level")) {
            helper.assertFalse(TrialSceneCommand.isTrialWorldName(refused), "\"" + refused + "\" must not be a trial world");
        }

        MinecraftServer server = helper.getLevel().getServer();
        String worldName = server.getWorldData().getLevelName();
        helper.assertFalse(TrialSceneCommand.isTrialWorldName(worldName),
                "The game test server's world is called \"" + worldName + "\": the test needs a world that is not a trial world");

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
        // Operator, so that only the world name can refuse the command.
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
                throw helper.assertionException(Component.literal("Command not recognised (" + command + "): " + e.getMessage()));
            }

            helper.assertValueEqual(result, 0, "result of the command " + command);
            helper.assertValueEqual(received.size(), 1, "messages received for " + command);
            String message = received.get(0).getString();
            helper.assertTrue(message.contains(worldName) && message.contains(TrialSceneCommand.TRIAL_WORLD_PREFIX),
                    "Unexpected refusal message for " + command + ": " + message);
            helper.assertTrue(helper.getLevel().isEmptyBlock(wouldBeBox), "A block was placed despite the refusal at " + wouldBeBox);
        }
        helper.succeed();
    }
}
