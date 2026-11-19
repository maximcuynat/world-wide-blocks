package io.github.maximcuynat.worldwideblocks.capture;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.maximcuynat.worldwideblocks.capture.CaptureScenario.Step;
import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenario;

/**
 * The capture scenarios, by name ({@code ./gradlew runClientCapture -Pscenario=<name>}). Each one replaces a check by
 * eye of {@code docs/lot-plan.md}.
 */
public final class CaptureScenarios {
    private static final int AUTO_SCALE = 0;

    /**
     * Lot 1b-1, scene {@code box}: the compact panel ("0/4 ports connected"), then the detailed one (ports 1 to 4
     * free, port 5, the line, inactive), the box facing the player.
     */
    static final CaptureScenario BOX = new CaptureScenario("box", "box alone, compact then detailed panel",
            CaptureScenario.STANDARD_SIZES, List.of(AUTO_SCALE), CaptureScenario.BOTH_LANGUAGES, List.of(
                    new Step.BuildScene(TrialScenario.BOX),
                    new Step.Aim(0, false),
                    new Step.Shot("compact"),
                    new Step.Aim(0, true),
                    new Step.Shot("detailed")));

    /**
     * Check of the run itself: a shot changes the language, the window size and the GUI scale, then a step fails on
     * purpose (no device 9 in the scene). The run must end with {@code failed: ...}, the command must fail, and the
     * settings must be back (log line "put back", {@code run-capture/options.txt}).
     */
    static final CaptureScenario FAILURE_CHECK = new CaptureScenario("failure_check",
            "a step fails on purpose after the settings changed", CaptureScenario.STANDARD_SIZES, List.of(AUTO_SCALE, 2),
            CaptureScenario.BOTH_LANGUAGES, List.of(
                    new Step.BuildScene(TrialScenario.BOX),
                    new Step.Aim(0, false),
                    new Step.Shot("before_failure"),
                    new Step.Aim(9, false)));

    private static final Map<String, CaptureScenario> BY_NAME = List.of(BOX, FAILURE_CHECK).stream()
            .collect(Collectors.toUnmodifiableMap(CaptureScenario::name, Function.identity()));

    private CaptureScenarios() {
    }

    public static Optional<CaptureScenario> byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    /** The names, sorted, for the message of an unknown scenario. */
    public static List<String> names() {
        return BY_NAME.keySet().stream().sorted().toList();
    }
}
