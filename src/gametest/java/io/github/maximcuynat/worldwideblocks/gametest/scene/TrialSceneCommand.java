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
 * {@code /worldwideblocks test <scénario>} construit devant le joueur le décor d'un essai à l'œil (panneau
 * d'état, câble dessiné, lisibilité des messages) ; {@code /worldwideblocks test effacer} retire le dernier
 * décor qu'il a construit.
 *
 * <p>Outil de développement : jeu de sources {@code gametest}, absent du mod distribué. Réservé aux
 * opérateurs, et seulement dans un monde dont le nom commence par {@value #TRIAL_WORLD_PREFIX}, pour
 * ne jamais poser de blocs dans un monde de jeu. Messages en français seulement : ils ne sont pas
 * dans les fichiers de langue du mod, qui partent dans le jar.
 */
@EventBusSubscriber(modid = WorldWideBlocks.MODID)
public final class TrialSceneCommand {
    public static final String TRIAL_WORLD_PREFIX = "Essais";

    /** Décors construits par le joueur depuis sa connexion, le plus récent en tête. Non sauvegardé. */
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
                .then(Commands.literal("effacer").executes(TrialSceneCommand::erase));
        for (TrialScenario scenario : TrialScenario.values()) {
            test.then(Commands.literal(scenario.id()).executes(context -> build(context, scenario)));
        }
        // Même nœud « worldwideblocks » que /worldwideblocks liens : Brigadier fusionne les deux. Même exigence
        // d'opérateur, quel que soit l'ordre d'enregistrement.
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
        source.sendSuccess(() -> Component.literal("Décor « " + scenario.id() + " » (" + scenario.description() + ") : "
                + result.devices().size() + " appareil(s), " + result.cables() + " câble(s). "
                + "/worldwideblocks test effacer pour le retirer."), false);
        return 1;
    }

    private static int erase(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        if (!isTrialWorld(source)) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        List<BlockPos> last = player.getData(BUILT_SCENES).scenes.poll();
        if (last == null) {
            source.sendFailure(Component.literal("Aucun décor à retirer depuis votre connexion."));
            return 0;
        }
        int removed = TrialScenes.remove(player.level(), last);
        source.sendSuccess(() -> Component.literal("Décor retiré : " + removed + " appareil(s)."), false);
        return 1;
    }

    /**
     * Règle des mondes d'essai : le nom commence par {@value #TRIAL_WORLD_PREFIX}, majuscule comprise.
     * « Essais 1b-1 » est accepté ; « World Wide Blocks », « Mes Essais » et « essais » sont refusés.
     */
    public static boolean isTrialWorldName(String name) {
        return name.startsWith(TRIAL_WORLD_PREFIX);
    }

    private static boolean isTrialWorld(CommandSourceStack source) {
        String name = source.getServer().getWorldData().getLevelName();
        if (isTrialWorldName(name)) {
            return true;
        }
        source.sendFailure(Component.literal("Commande réservée aux mondes d'essai : ce monde s'appelle « " + name
                + " ». Créez un monde dont le nom commence par « " + TRIAL_WORLD_PREFIX + " »."));
        return false;
    }
}
