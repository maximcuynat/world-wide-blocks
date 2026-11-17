package dev.netcraft.gametest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import dev.netcraft.NetCraft;
import dev.netcraft.gametest.scene.TrialScenario;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Tests en jeu automatiques de NetCraft, lancés sans fenêtre ni joueur par
 * {@code ./gradlew runGameTestServer}, dans un monde jetable ({@code run-gametest/}).
 *
 * <p>Chaque test est une fonction (registre {@code test_function}) et une instance de test qui dit
 * dans quel lot il tourne, sur quelle structure et pendant combien de ticks au plus. Minecraft
 * exécute les lots l'un après l'autre et les tests d'un même lot en même temps.
 *
 * <p>Jeu de sources {@code gametest} : chargé par les runs de développement, absent du jar.
 */
@EventBusSubscriber(modid = NetCraft.MODID)
public final class NetCraftGameTests {
    /** Zone vide de 16 x 4 x 16 blocs posée sur le sol plat du monde de test. */
    private static final Identifier STRUCTURE = id("empty_16x4x16");

    /** Lot d'exécution. Un lot = un environnement de test distinct. */
    enum Batch {
        /** Tests indépendants qui ne s'approchent pas des limites globales du moteur. */
        SHARED("shared"),
        /**
         * Tests qui touchent une limite globale du moteur (trames par tick, taille de la file,
         * limite des signalements) : chacun seul dans son propre lot, pour que le résultat ne
         * dépende ni des autres tests ni de l'ordre d'exécution.
         */
        ENGINE_LIMITS("engine_limits");

        private final String path;

        Batch(String path) {
            this.path = path;
        }
    }

    record Case(String name, Batch batch, int maxTicks, Consumer<GameTestHelper> function) {
    }

    private static final List<Case> CASES = Stream.concat(Stream.of(
            new Case("cable_no_free_port", Batch.SHARED, 20, CableTests::noFreePort),
            new Case("cable_start_broken_creative", Batch.SHARED, 100, CableTests::startBrokenCreative),
            new Case("cable_start_broken_survival", Batch.SHARED, 100, CableTests::startBrokenSurvival),
            new Case("cable_start_exploded_survival", Batch.SHARED, 100, CableTests::startExplodedSurvival),
            new Case("cable_dimension_change", Batch.SHARED, 100, CableTests::dimensionChange),
            new Case("cable_start_broken_while_disconnected", Batch.SHARED, 100, CableTests::startBrokenWhileDisconnected),
            new Case("network_save_and_reload", Batch.SHARED, 20, NetworkTests::networkSurvivesSaveAndReload),
            // 300 s d'expiration : le serveur de tests ne marque pas de pause entre deux ticks, quelques secondes réelles.
            new Case("mac_expires_after_300_seconds", Batch.SHARED, 6200, NetworkTests::macExpiresAfter300Seconds),
            new Case("loop_reported_once_per_minute", Batch.ENGINE_LIMITS, 4200, NetworkTests::loopReportedOncePerMinute),
            new Case("trial_command_refused_outside_trial_world", Batch.SHARED, 20, SceneTests::commandRefusedOutsideTrialWorld)),
            // Décors de /netcraft test, sauf « distance » (99 blocs, plus grand que la zone de test).
            // Le décor de boucle touche les limites globales du moteur : lot à part.
            Arrays.stream(TrialScenario.values())
                    .filter(scenario -> scenario != TrialScenario.DISTANCE)
                    .map(scenario -> new Case("trial_scene_" + scenario.id(),
                            scenario == TrialScenario.BOUCLE ? Batch.ENGINE_LIMITS : Batch.SHARED, 20,
                            helper -> SceneTests.buildsScenario(helper, scenario))))
            .toList();

    private NetCraftGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> CASES.forEach(test -> registry.register(id(test.name()), test.function())));
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        // Minecraft regroupe en un lot les tests d'un même environnement : un environnement par test
        // isolé, un seul pour les tests partagés.
        Map<String, Holder<TestEnvironmentDefinition<?>>> environments = new HashMap<>();
        for (Case test : CASES) {
            String environment = test.batch() == Batch.SHARED ? test.batch().path : test.batch().path + "/" + test.name();
            Holder<TestEnvironmentDefinition<?>> holder = environments.computeIfAbsent(environment,
                    path -> event.registerEnvironment(id(path)));
            event.registerTest(id(test.name()), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id(test.name())),
                    new TestData<>(holder, STRUCTURE, test.maxTicks(), 0, true)));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NetCraft.MODID, path);
    }
}
