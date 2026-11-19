package io.github.maximcuynat.worldwideblocks.capture;

import java.util.List;
import java.util.Objects;

import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenario;

/**
 * A capture scenario: what the run does in its trial world, step by step, and the combinations of window size, GUI
 * scale and language in which each {@link Step.Shot} is taken. One scenario per check by eye of a lot.
 *
 * @param name      name given to {@code -Pscenario=}, also the folder of its screenshots
 * @param sizes     framebuffer sizes, in pixels
 * @param guiScales GUI scale options: 0 for the automatic scale of the game
 * @param languages language codes of the game ({@code en_us}, {@code fr_fr})
 */
public record CaptureScenario(String name, String description, List<Size> sizes, List<Integer> guiScales,
        List<String> languages, List<Step> steps) {

    /** The sizes asked by {@code docs/automated-checks.md}: the smallest window of the game and a common screen. */
    public static final List<Size> STANDARD_SIZES = List.of(new Size(854, 480), new Size(1920, 1080));
    /** English then French. */
    public static final List<String> BOTH_LANGUAGES = List.of("en_us", "fr_fr");

    public CaptureScenario {
        Objects.requireNonNull(name, "name");
        sizes = List.copyOf(sizes);
        guiScales = List.copyOf(guiScales);
        languages = List.copyOf(languages);
        steps = List.copyOf(steps);
        if (sizes.isEmpty() || guiScales.isEmpty() || languages.isEmpty()) {
            throw new IllegalArgumentException("A scenario needs at least one size, one GUI scale and one language");
        }
    }

    /** A framebuffer size, in pixels. */
    public record Size(int width, int height) {
        @Override
        public String toString() {
            return this.width + "x" + this.height;
        }
    }

    /** A step of a scenario. */
    public sealed interface Step {

        /** Builds a trial scene 3 blocks in front of the player, who faces north, as {@code /worldwideblocks test} does. */
        record BuildScene(TrialScenario scene) implements Step {
        }

        /**
         * The player, still at the place where the run put them, aims at the centre of a device of the last scene
         * (its index in the scene), standing or sneaking. The step waits until the crosshair is on that device and
         * its panel is drawn.
         */
        record Aim(int device, boolean sneaking) implements Step {
        }

        /** Lets the game run for some ticks. */
        record Wait(int ticks) implements Step {
        }

        /**
         * A screenshot in each combination of the scenario, named {@code <name>-<size>-<scale>-<language>.png}, with
         * the values read at that moment written to {@code values.txt}.
         */
        record Shot(String name) implements Step {
        }
    }
}
