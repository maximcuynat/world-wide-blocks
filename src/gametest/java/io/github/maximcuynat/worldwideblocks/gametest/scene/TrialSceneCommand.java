package io.github.maximcuynat.worldwideblocks.gametest.scene;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * {@code /worldwideblocks test <scene>} builds in front of the player the scene of a check by eye (status
 * panel, drawn cable, readability of the messages); {@code /worldwideblocks test clear} removes the last
 * scene they built. The short alias {@code /wwb} works too.
 *
 * <p>Development tool: {@code gametest} source set, absent from the released mod. Operators only, and
 * only in a world whose name starts with {@value #TRIAL_WORLD_PREFIX}, so that no block is ever placed
 * in a world that is played. Messages in English only: they are not in the mod's language files,
 * which ship in the jar.
 */
@EventBusSubscriber(modid = WorldWideBlocks.MODID)
public final class TrialSceneCommand {
    public static final String TRIAL_WORLD_PREFIX = "Trials";

    /** Scenes built by the player since they joined, the most recent first. Not saved. */
    public static final class BuiltScenes {
        private final Deque<List<BlockPos>> scenes = new ArrayDeque<>();
    }

    private static final AttachmentType<BuiltScenes> BUILT_SCENES = AttachmentType.builder(() -> new BuiltScenes()).build();

    private TrialSceneCommand() {
    }

    @SubscribeEvent
    static void registerAttachment(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
                Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "trial_scenes"), () -> BUILT_SCENES);
    }

    @SubscribeEvent
    static void registerCommand(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> test = Commands.literal("test")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("clear").executes(TrialSceneCommand::clear));
        for (TrialScenario scenario : TrialScenario.values()) {
            test.then(Commands.literal(scenario.id()).executes(context -> build(context, scenario)));
        }
        // Same "worldwideblocks" node as /worldwideblocks links: Brigadier merges the two. Same operator
        // requirement, whatever the registration order.
        event.getDispatcher().register(Commands.literal("worldwideblocks")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(test));
    }

    private static int build(CommandContext<CommandSourceStack> context, TrialScenario scenario) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        if (!isTrialWorld(source)) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        Direction forward = player.getDirection();
        BlockPos origin = player.blockPosition().relative(forward, TrialScenes.DISTANCE_FROM_PLAYER);
        TrialScenes.Result result = TrialScenes.build(player.level(), scenario, origin, forward);
        if (!result.devices().isEmpty()) {
            player.getData(BUILT_SCENES).scenes.push(result.devices());
        }
        if (result.refusal().isPresent()) {
            source.sendFailure(Component.literal(result.refusal().get()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Scene \"" + scenario.id() + "\" (" + scenario.description() + "): "
                + result.devices().size() + " device(s), " + result.cables() + " cable(s). "
                + "/worldwideblocks test clear to remove it."), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        if (!isTrialWorld(source)) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        List<BlockPos> last = player.getData(BUILT_SCENES).scenes.poll();
        if (last == null) {
            source.sendFailure(Component.literal("No scene to remove since you joined."));
            return 0;
        }
        int removed = TrialScenes.remove(player.level(), last);
        source.sendSuccess(() -> Component.literal("Scene removed: " + removed + " device(s)."), false);
        return 1;
    }

    /**
     * Rule of the trial worlds: the name starts with {@value #TRIAL_WORLD_PREFIX}, capital letter included.
     * "Trials 1b-1" is accepted; "World Wide Blocks", "My Trials", "trials" and "Test Level" (the world
     * of the game test server) are refused. Not "Test": the game test world would then be accepted.
     */
    public static boolean isTrialWorldName(String name) {
        return name.startsWith(TRIAL_WORLD_PREFIX);
    }

    private static boolean isTrialWorld(CommandSourceStack source) {
        String name = source.getServer().getWorldData().getLevelName();
        if (isTrialWorldName(name)) {
            return true;
        }
        source.sendFailure(Component.literal("Command reserved for trial worlds: this world is called \"" + name
                + "\". Create a world whose name starts with \"" + TRIAL_WORLD_PREFIX + "\"."));
        return false;
    }
}
