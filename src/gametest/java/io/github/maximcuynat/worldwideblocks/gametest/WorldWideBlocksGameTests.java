package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenario;
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
 * Automatic game tests of World Wide Blocks, run without a window or a player by
 * {@code ./gradlew runGameTestServer}, in a throwaway world ({@code run-gametest/}).
 *
 * <p>Each test is a function ({@code test_function} registry) and a test instance that says in
 * which batch it runs, on which structure and for how many ticks at most. Minecraft runs the
 * batches one after the other and the tests of one batch at the same time.
 *
 * <p>Source set {@code gametest}: loaded by the development runs, absent from the jar.
 */
@EventBusSubscriber(modid = WorldWideBlocks.MODID)
public final class WorldWideBlocksGameTests {
    /** Empty area of 16 x 4 x 16 blocks placed on the flat ground of the test world. */
    private static final Identifier STRUCTURE = id("empty_16x4x16");

    /** Execution batch. One batch = one distinct test environment. */
    enum Batch {
        /** Independent tests that do not come near the engine's global limits. */
        SHARED("shared"),
        /**
         * Tests that hit a global limit of the engine (frames per tick, queue size, report limit):
         * each one alone in its own batch, so that the result depends neither on the other tests
         * nor on the execution order.
         */
        ENGINE_LIMITS("engine_limits");

        private final String path;

        Batch(String path) {
            this.path = path;
        }
    }

    record Case(String name, Batch batch, int maxTicks, Consumer<GameTestHelper> function) {
    }

    private static final List<Case> CASES = Stream.concat(Stream.concat(Stream.of(
            new Case("cable_no_free_port", Batch.SHARED, 20, CableTests::noFreePort),
            new Case("cable_start_broken_creative", Batch.SHARED, 100, CableTests::startBrokenCreative),
            new Case("cable_start_broken_survival", Batch.SHARED, 100, CableTests::startBrokenSurvival),
            new Case("cable_start_exploded_survival", Batch.SHARED, 100, CableTests::startExplodedSurvival),
            new Case("cable_dimension_change", Batch.SHARED, 100, CableTests::dimensionChange),
            new Case("cable_start_broken_while_disconnected", Batch.SHARED, 100, CableTests::startBrokenWhileDisconnected),
            new Case("network_save_and_reload", Batch.SHARED, 20, NetworkTests::networkSurvivesSaveAndReload),
            // 300 s expiry: the test server does not pause between two ticks, so a few real seconds.
            new Case("mac_expires_after_300_seconds", Batch.SHARED, 6200, NetworkTests::macExpiresAfter300Seconds),
            new Case("loop_reported_once_per_minute", Batch.ENGINE_LIMITS, 4200, NetworkTests::loopReportedOncePerMinute),
            new Case("loop_forgotten_only_for_changed_network", Batch.ENGINE_LIMITS, 1000, NetworkTests::loopForgottenOnlyForChangedNetwork),
            new Case("trial_command_refused_outside_trial_world", Batch.SHARED, 20, SceneTests::commandRefusedOutsideTrialWorld),
            // DHCP (lot 1b-2). The lease is 24,000 ticks: the test server does not pause between ticks, so a few real seconds.
            new Case("dhcp_address_obtained", Batch.SHARED, 40, DhcpTests::addressObtained),
            new Case("dhcp_address_lost_when_unplugged", Batch.SHARED, 40, DhcpTests::addressLostWhenUnplugged),
            new Case("dhcp_same_address_when_plugged_again", Batch.SHARED, 100, DhcpTests::sameAddressWhenPluggedAgain),
            new Case("dhcp_renewal_at_half_lease", Batch.SHARED, 12_100, DhcpTests::renewalAtHalfLease),
            new Case("dhcp_lease_expires", Batch.SHARED, 24_100, DhcpTests::leaseExpires),
            new Case("dhcp_no_box", Batch.SHARED, 420, DhcpTests::noBox),
            new Case("dhcp_conflict_of_two_boxes", Batch.SHARED, 220, DhcpTests::conflictOfTwoBoxes),
            new Case("dhcp_restart_asks_for_the_same_address", Batch.SHARED, 40, DhcpTests::restartAsksForTheSameAddress),
            new Case("dhcp_pool_full", Batch.SHARED, 420, DhcpTests::poolFull),
            // Terminals (lot 1c-1, issue #11).
            new Case("terminal_tower_opens_for_one_player", Batch.SHARED, 40, TerminalTests::towerOpensForOnePlayer),
            new Case("terminal_keyboard_freed", Batch.SHARED, 40, TerminalTests::keyboardFreed),
            new Case("terminal_computer_removed_while_open", Batch.SHARED, 40, TerminalTests::computerRemovedWhileOpen),
            new Case("terminal_exit_ends_the_session", Batch.SHARED, 40, TerminalTests::exitEndsTheSession),
            new Case("terminal_packets_checked", Batch.SHARED, 80, TerminalTests::packetsChecked),
            new Case("terminal_packet_from_afar", Batch.SHARED, 40, TerminalTests::packetFromAfar),
            new Case("terminal_keys_reach_the_console", Batch.SHARED, 40, TerminalTests::keysReachTheConsole),
            new Case("terminal_payloads_round_trip", Batch.SHARED, 20, TerminalTests::payloadsRoundTrip),
            new Case("terminal_laptop_gestures", Batch.SHARED, 40, TerminalTests::laptopGestures),
            new Case("terminal_laptop_item_keeps_its_lid", Batch.SHARED, 40, TerminalTests::laptopItemKeepsItsLid),
            new Case("terminal_tower_opens_as_a_chest", Batch.SHARED, 40, TerminalTests::towerOpensAsAChest),
            new Case("terminal_laptop_sneak_unplugs", Batch.SHARED, 40, TerminalTests::laptopSneakUnplugs),
            // ping (lot 1c-1, issue #16).
            new Case("ping_between_two_computers", Batch.SHARED, 200, PingTests::betweenTwoComputers),
            new Case("ping_absent_address", Batch.SHARED, 200, PingTests::absentAddress)),
            // Scenes of /worldwideblocks test, except "distance" (99 blocks, larger than the test area).
            // The loop scene hits the engine's global limits: separate batch.
            Arrays.stream(TrialScenario.values())
                    .filter(scenario -> scenario != TrialScenario.DISTANCE)
                    .map(scenario -> new Case("trial_scene_" + scenario.id(),
                            scenario == TrialScenario.LOOP ? Batch.ENGINE_LIMITS : Batch.SHARED, 20,
                            helper -> SceneTests.buildsScenario(helper, scenario)))),
            // Shapes and placement of the devices, in the four orientations (DeviceShapeTests).
            Stream.concat(DeviceShapeTests.cases(),
                    // Port layouts against the real shapes (issue #61).
                    PortLayoutTests.cases()))
            .toList();

    private WorldWideBlocksGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> CASES.forEach(test -> registry.register(id(test.name()), test.function())));
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        // Minecraft groups the tests of one environment into a batch: one environment per isolated
        // test, a single one for the shared tests.
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
        return Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, path);
    }
}
