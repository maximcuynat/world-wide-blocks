package dev.netcraft.gametest;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dev.netcraft.NetCraft;
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
         * limite des signalements) : seuls dans leur lot, pour que le résultat ne dépende ni des
         * autres tests ni de l'ordre d'exécution.
         */
        ENGINE_LIMITS("engine_limits");

        private final String path;

        Batch(String path) {
            this.path = path;
        }
    }

    record Case(String name, Batch batch, int maxTicks, Consumer<GameTestHelper> function) {
    }

    private static final List<Case> CASES = List.of(
            new Case("cable_no_free_port", Batch.SHARED, 20, CableTests::noFreePort));

    private NetCraftGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> CASES.forEach(test -> registry.register(id(test.name()), test.function())));
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        Map<Batch, Holder<TestEnvironmentDefinition<?>>> environments = new EnumMap<>(Batch.class);
        for (Batch batch : Batch.values()) {
            environments.put(batch, event.registerEnvironment(id(batch.path)));
        }
        for (Case test : CASES) {
            event.registerTest(id(test.name()), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id(test.name())),
                    new TestData<>(environments.get(test.batch()), STRUCTURE, test.maxTicks(), 0, true)));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NetCraft.MODID, path);
    }
}
