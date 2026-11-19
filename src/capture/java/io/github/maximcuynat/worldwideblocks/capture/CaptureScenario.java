package io.github.maximcuynat.worldwideblocks.capture;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenario;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;

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

        /**
         * Builds a trial scene 3 blocks north of the place where the run put the player, as {@code /worldwideblocks
         * test} does in front of a player who faces north. One scene at a time: {@link ClearScene} removes it.
         */
        record BuildScene(TrialScenario scene) implements Step {
        }

        /** Removes the devices of the last scene (their cables go with them). */
        record ClearScene() implements Step {
        }

        /**
         * The player stands 2 blocks south of a device of the last scene (its index in the scene), faces it and aims at
         * the centre of its shape, standing or sneaking. The step ends when the client shows the crosshair on that
         * device and its panel; the shots that follow check that this still holds.
         */
        record Aim(int device, boolean sneaking) implements Step {
        }

        /**
         * The player flies at a place given from a device of the last scene ({@code east}, {@code up} and {@code south}
         * blocks from it) and looks at the centre of another device, without needing its panel: a view of the scene.
         */
        record View(int from, int east, int up, int south, int target) implements Step {
        }

        /** Puts this item in the main hand (the selected slot of the hotbar), or empties it when the item is null. */
        record Hold(Supplier<? extends ItemLike> item) implements Step {
        }

        /** A click of the use key (right button), as the player does it: the game sends what follows to the server. */
        record Use() implements Step {
        }

        /** Opens the inventory with the mouse over the selected slot of the hotbar: the game draws the tooltip of its item. */
        record HoverHeldItem() implements Step {
        }

        /** Opens the chat (key T): the recent messages are shown without fading. */
        record OpenChat() implements Step {
        }

        /** Closes the screen open. */
        record CloseScreen() implements Step {
        }

        /**
         * The game mode of the player, set by the server as the command {@code /gamemode} does, without its message in
         * the chat (it would show in the captures).
         */
        record GameMode(GameType mode) implements Step {
        }

        /** A command typed by the player, without its slash. */
        record Command(String command) implements Step {
        }

        /**
         * Sets the client option {@code messageSymbols} of the mod, by editing {@code config/worldwideblocks-client.toml}
         * as a player would; the game reads the file again. Put back at the end of the run.
         */
        record MessageSymbols(boolean enabled) implements Step {
        }

        /** Lets the game run for some ticks. */
        record Wait(int ticks) implements Step {
        }

        /**
         * A screenshot in each combination of the scenario, named {@code <name>-<size>-<scale>-<language>.png}, with
         * the values read at that moment written to {@code values.txt}. With {@code useFirst}, a click of the use key
         * just before each screenshot, for a message of the action bar, which fades after 3 s.
         */
        record Shot(String name, boolean useFirst) implements Step {
            public Shot(String name) {
                this(name, false);
            }
        }
    }
}
