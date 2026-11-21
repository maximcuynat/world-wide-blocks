package io.github.maximcuynat.worldwideblocks.capture;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenario;
import net.minecraft.client.CameraType;
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

        /**
         * A close-up view of a device of the last scene: the player's eye at {@code distance} blocks from the centre of
         * its shape, {@code around} degrees clockwise (seen from above) from the side of the player who built the scene
         * (0: from the south, the front of a device that faces the player; 180: from behind), {@code above} degrees
         * above the horizontal, looking at that centre, moved {@code east} blocks toward the east (to centre a row of
         * devices). Fractional positions; meant for a player in spectator mode ({@link GameMode}), who goes through
         * blocks and is not pushed out of them. The reach of the player is 0 during the view, so that no block is
         * aimed (no outline, no status panel); the next {@link Aim} or {@link OpenTerminal} gives it back.
         */
        record Orbit(int device, int around, int above, double distance, double east) implements Step {
            public Orbit(int device, int around, int above, double distance) {
                this(device, around, above, distance, 0);
            }
        }

        /** Hides the HUD as F1 does (no crosshair, hand, hotbar or panel), or shows it again; shown again at the end of the run. */
        record HideHud(boolean hidden) implements Step {
        }

        /**
         * Opens a capture screen that draws this item as the inventory does, with its real {@code gui} display
         * settings: large, then at its real size in a slot ({@link CaptureItemScreen}). {@link CloseScreen} closes it.
         */
        record ShowItem(Supplier<? extends ItemLike> item) implements Step {
        }

        /** The camera of the player: first person, third person from behind or from the front; put back at the end of the run. */
        record Camera(CameraType type) implements Step {
        }

        /**
         * The server drops this item on the ground, {@code east} blocks east of a device of the last scene, where nobody
         * can pick it up: the item as it lies in the world.
         */
        record DropItem(Supplier<? extends ItemLike> item, int device, int east) implements Step {
        }

        /**
         * The server places a stone block {@code east} blocks east of a device of the last scene and hangs an item frame
         * holding this item on its south face, toward the player who built the scene.
         */
        record FrameItem(Supplier<? extends ItemLike> item, int device, int east) implements Step {
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
         * as a player would; the game reads the file again. The file is put back at the end of the run, whatever
         * changed it (this step, or Ctrl+= in the terminal).
         */
        record MessageSymbols(boolean enabled) implements Step {
        }

        /** Sets the client option {@code terminalAdvice} of the mod, as {@link MessageSymbols} does. */
        record TerminalAdvice(boolean enabled) implements Step {
        }

        /** Lets the game run for some ticks. */
        record Wait(int ticks) implements Step {
        }

        /**
         * The player stands 2 blocks south of a computer of the last scene, aims at it (at the keyboard of an open
         * laptop) and clicks the use key, as a player does; the step ends when its terminal screen is open.
         */
        record OpenTerminal(int device) implements Step {
        }

        /**
         * Characters typed in the terminal open, one by one, as the operating system hands them to the game. A
         * {@code {address:N}} in the text is replaced by the address that the computer N of the last scene holds at
         * that moment (its DHCP lease, read on the server); the run fails if it has none.
         */
        record Type(String text) implements Step {
        }

        /**
         * Fails the run unless exactly {@code count} rows shown by the terminal open match this regular expression
         * (whole row, trailing spaces ignored). A {@code {address:N}} in it is replaced, quoted, by the address of the
         * computer N of the last scene, as in {@link Type}.
         */
        record Expect(String regex, int count) implements Step {
        }

        /** A key of the terminal, with its modifiers, as the game hands it to the screen. */
        record Press(TerminalKey key) implements Step {
        }

        /**
         * Notches of the mouse wheel over the screen open, as the operating system reports them to the game
         * ({@code MouseHandler.onScroll}): positive turns the wheel up, towards the older lines.
         */
        record Scroll(int notches) implements Step {
        }

        /**
         * Keys of the terminal: the physical key (SDL3 scancode, {@code InputConstants}), the symbol of a QWERTY layout
         * ({@code shortcutKey}, what the terminal reads for Ctrl shortcuts), the modifiers (SDL masks: Shift 3, Ctrl
         * 192).
         */
        enum TerminalKey {
            ENTER(InputConstants.KEY_RETURN, '\r', 0),
            ESCAPE(InputConstants.KEY_ESCAPE, 27, 0),
            UP(InputConstants.KEY_UP, 0, 0),
            SHIFT_PAGE_UP(InputConstants.KEY_PAGEUP, 0, InputConstants.MOD_SHIFT),
            SHIFT_PAGE_DOWN(InputConstants.KEY_PAGEDOWN, 0, InputConstants.MOD_SHIFT),
            CTRL_C(InputConstants.KEY_C, 'c', InputConstants.MOD_CONTROL),
            CTRL_D(InputConstants.KEY_D, 'd', InputConstants.MOD_CONTROL),
            CTRL_L(InputConstants.KEY_L, 'l', InputConstants.MOD_CONTROL),
            CTRL_PLUS(InputConstants.KEY_EQUALS, '=', InputConstants.MOD_CONTROL),
            CTRL_MINUS(InputConstants.KEY_MINUS, '-', InputConstants.MOD_CONTROL),
            CTRL_ZERO(InputConstants.KEY_0, '0', InputConstants.MOD_CONTROL);

            final int key;
            final int keycode;
            final int modifiers;

            TerminalKey(int key, int keycode, int modifiers) {
                this.key = key;
                this.keycode = keycode;
                this.modifiers = modifiers;
            }
        }

        /**
         * Lets the game run for some ticks and fails the run if a line reaches the chat meanwhile: proves that a message
         * stops.
         */
        record Silence(int ticks) implements Step {
        }

        /**
         * The server removes one cable between two devices of the last scene (their indexes), as the engine does when a
         * player unplugs it, without a gesture: a gesture unplugs the first cable of the device, maybe not this one.
         */
        record RemoveCable(int from, int to) implements Step {
        }

        /**
         * The server runs these ticks as fast as it can, as {@code /tick sprint} does, without the command in the chat;
         * the step ends when the sprint is over. The game reports the sprint to the operators: one chat line.
         */
        record Sprint(int ticks) implements Step {
        }

        /**
         * Saves and quits to the title screen, then opens the same world again, as a player does: the integrated server
         * stops and reads its save back. The player looks up first, so that the panel hides before quitting and a panel
         * seen after the reopening is a new one; aim again afterwards.
         */
        record Restart() implements Step {
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
