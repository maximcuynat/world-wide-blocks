package io.github.maximcuynat.worldwideblocks.capture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;

import io.github.maximcuynat.worldwideblocks.capture.CaptureScenario.Size;
import io.github.maximcuynat.worldwideblocks.capture.CaptureScenario.Step;
import io.github.maximcuynat.worldwideblocks.client.StatusPanelShownEvent;
import io.github.maximcuynat.worldwideblocks.client.TerminalScreen;
import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenes;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevices;
import io.github.maximcuynat.worldwideblocks.network.NetworkManager;
import io.github.maximcuynat.worldwideblocks.network.sim.HostLogic;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalLayout;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Runs one capture scenario in the development client, one client tick at a time
 * ({@code docs/automated-checks.md}):
 *
 * <ol>
 * <li>At the title screen, creates the flat creative world "Trials capture &lt;scenario&gt;" in {@code run-capture/}
 * ({@code WorldOpenFlows.createFreshLevel} with the flat preset of the test worlds), replacing the one of the previous
 * run: Quick Play only opens a world that exists ({@code QuickPlay.joinSingleplayerWorld}).</li>
 * <li>In the world: noon, time paused, clear weather; the player at the spawn, facing north.</li>
 * <li>The steps of the scenario; each screenshot in every combination of language, size and GUI scale, with the
 * values read at that moment in {@code values.txt}, in {@code run-capture/screenshots/<scenario>/}.</li>
 * <li>Puts back the window size, the GUI scale, the language and the options it changed, also after a failure,
 * writes {@code run-capture/capture-result.txt} ({@code passed ...} or {@code failed: ...}), leaves the world and
 * quits. The Gradle task {@code checkCaptureResult} reads that file.</li>
 * </ol>
 *
 * <p>Every wait has a limit: a step that does not happen in time fails the run with its reason; it never hangs.
 */
final class CaptureRun {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WORLD_PREFIX = "Trials capture ";
    /** Ticks given to the world once open, and to a scene once built, for chunks and payloads to reach the client. */
    private static final int SETTLE_TICKS = 40;
    /** Ticks a combination, or an aim, must hold before it counts. */
    private static final int STABLE_TICKS = 10;
    /** Longest wait of one phase or one step: 60 s. */
    private static final int TIMEOUT_TICKS = 1200;
    private static final long SEED = "worldwideblocks capture".hashCode();

    /** Ticks given to the screenshot of a failure before the run quits without it. */
    private static final int FAILURE_SHOT_TICKS = 100;

    private enum Phase { TITLE, CREATING, SETTLING, SETUP, STEPS, FAILING, FINISHED }

    /** The screen a step expects to be open: shots check it, and a pause screen is closed only when none is. */
    private enum ExpectedScreen { NONE, INVENTORY, CHAT, TERMINAL }

    /** Stages of a {@link Step.Restart}. */
    private enum Restarting { LOOKING_AWAY, REOPENING, ENTERING }

    /** Ticks a message of the action bar counts as shown (the game shows it 3 s). */
    private static final int ACTION_BAR_TICKS = 60;
    /** Ticks between the click of a {@code useFirst} shot and its screenshot, for the server to answer. */
    private static final int USE_ANSWER_TICKS = 6;
    /** The client options file of the mod, in the game directory. */
    private static final String MOD_CLIENT_CONFIG = "config/worldwideblocks-client.toml";

    private final String scenarioName;
    private final Optional<CaptureScenario> scenario;
    private Phase phase = Phase.TITLE;
    private int phaseTicks;
    private int screenshots;

    // Settings to put back.
    private boolean settingsSaved;
    private int originalWidth;
    private int originalHeight;
    private int originalGuiScale;
    private String originalLanguage;
    private boolean originalPauseOnLostFocus;
    private TutorialSteps originalTutorialStep;
    /** Whether the HUD was hidden (F1) when the run began. */
    private boolean originalHudHidden;
    /** Where the feet of the player go for the last {@link Step.Orbit}, set on the server thread. */
    private final AtomicReference<Vec3> orbitFeet = new AtomicReference<>();
    /** The block reach of the player before the first {@link Step.Orbit} set it to 0, until it is given back. */
    private final AtomicReference<Double> normalReach = new AtomicReference<>();
    /** Content of the client options file of the mod when the run began, put back at the end; null if it had none. */
    private String originalModConfig;

    // World, written by the server thread.
    private final AtomicReference<BlockPos> stand = new AtomicReference<>();
    private final AtomicReference<TrialScenes.Result> scene = new AtomicReference<>();
    private final AtomicReference<String> serverError = new AtomicReference<>();

    // Steps.
    /** Where the player stands now: the place of the scene, or 2 blocks south of the device aimed at. */
    private BlockPos standing;
    private ExpectedScreen expectedScreen = ExpectedScreen.NONE;
    private CaptureInventoryScreen inventory;
    private int useTicks = -1;
    private int step;
    private int stepTicks;
    private boolean stepStarted;
    private boolean sneaking;
    private BlockPos aimed;
    private int combination;
    private int stableTicks;
    private CompletableFuture<Void> reload;
    private final AtomicBoolean saved = new AtomicBoolean();
    private boolean waitingForSave;
    /** Result of a failure, written once its screenshot is saved. */
    private String failure;
    /** The use key was clicked by a {@link Step.OpenTerminal}. */
    private boolean terminalClicked;
    /** The tick of the step at which a {@link Step.Type} typed its text, -1 before. */
    private int typedAt = -1;
    /** Addresses asked to the server by the current step ({@code {address:N}}), null until they arrive. */
    private final AtomicReference<Map<Integer, String>> addresses = new AtomicReference<>();
    /** A computer of the last scene named in a text: {@code {address:N}}. */
    private static final Pattern ADDRESS_OF = Pattern.compile("\\{address:(\\d+)\\}");
    /** Chat lines received when a {@link Step.Silence} began. */
    private int silenceMark;
    private final AtomicBoolean cableRemoved = new AtomicBoolean();
    private final AtomicBoolean sprintOver = new AtomicBoolean();
    /** A question about the sprint is waiting on the server thread. */
    private final AtomicBoolean sprintQueried = new AtomicBoolean();
    private Restarting restarting;
    /** Ticks in the world again after a restart, for chunks and payloads to reach the client. */
    private int enteredTicks;

    /** The last panel drawn, as the client posted it. */
    private StatusPanelShownEvent panel;
    /** Ticks since the run began, for the age of a message. */
    private long clientTicks;
    /** Lines added to the chat since the run began, as shown. */
    private final List<String> chat = new ArrayList<>();
    /** Chat lines already written to the values file. */
    private int chatWritten;
    private String actionBar;
    private long actionBarTick = Long.MIN_VALUE / 2;

    CaptureRun(String scenarioName) {
        this.scenarioName = scenarioName;
        this.scenario = CaptureScenarios.byName(scenarioName);
    }

    void onPanel(StatusPanelShownEvent event) {
        this.panel = event;
    }

    /** A system message reached the chat or the action bar of the client ({@code ChatListener}). */
    void onChat(ClientChatReceivedEvent.System event) {
        if (event.getMessage() == null) {
            return;
        }
        String text = event.getMessage().getString();
        if (event.isOverlay()) {
            this.actionBar = text;
            this.actionBarTick = this.clientTicks;
        } else {
            this.chat.add(text);
        }
    }

    void onClientTick(ClientTickEvent.Post event) {
        if (this.phase == Phase.FINISHED) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        this.clientTicks++;
        try {
            this.phaseTicks++;
            switch (this.phase) {
                case TITLE -> this.title(minecraft);
                case CREATING, SETTLING -> this.entering(minecraft);
                case SETUP -> this.setup(minecraft);
                case STEPS -> this.steps(minecraft);
                case FAILING -> this.failing(minecraft);
                case FINISHED -> {
                }
            }
        } catch (Exception e) {
            LOGGER.error("Capture {}: stopped in {} at step {}", this.scenarioName, this.phase, this.step + 1, e);
            this.fail(minecraft, "failed: " + this.phase + ", step " + (this.step + 1) + ": " + e.getMessage());
        }
    }

    /**
     * A failure: the screen as it is goes to {@code failure.png} next to the other screenshots (in the world only), then
     * the run ends.
     */
    private void fail(Minecraft minecraft, String result) {
        if (this.phase == Phase.FAILING || minecraft.level == null) {
            this.finish(minecraft, result);
            return;
        }
        this.failure = result;
        this.enter(Phase.FAILING);
        this.saved.set(false);
        try {
            Screenshot.grab(minecraft.gameDirectory, this.scenarioName + "/failure.png", minecraft.gameRenderer.mainRenderTarget(), 1,
                    message -> this.saved.set(true));
        } catch (RuntimeException e) {
            LOGGER.error("Capture {}: no screenshot of the failure", this.scenarioName, e);
            this.finish(minecraft, result);
        }
    }

    private void failing(Minecraft minecraft) {
        if (this.saved.get() || this.phaseTicks > FAILURE_SHOT_TICKS) {
            this.finish(minecraft, this.failure);
        }
    }

    private void enter(Phase next) {
        this.phase = next;
        this.phaseTicks = 0;
    }

    private void limit(String what, int ticks) {
        if (ticks > TIMEOUT_TICKS) {
            throw new IllegalStateException(what + " within " + TIMEOUT_TICKS + " ticks");
        }
        String error = this.serverError.get();
        if (error != null) {
            throw new IllegalStateException(error);
        }
    }

    // ---- World ----

    private void title(Minecraft minecraft) throws IOException {
        if (minecraft.gui.overlay() != null) {
            this.phaseTicks = 0; // the resources are still loading: no limit on that
            return;
        }
        if (minecraft.gui.screen() instanceof AccessibilityOnboardingScreen) {
            // First launch in an empty run-capture/: the game shows its accessibility onboarding once, then never
            // again (Gui.addInitialScreens). The run answers it as a player who goes on would.
            minecraft.options.onboardAccessibility = false;
            minecraft.options.save();
            minecraft.gui.setScreen(new TitleScreen());
            return;
        }
        if (!(minecraft.gui.screen() instanceof TitleScreen)) {
            this.limit("the title screen did not show (screen " + minecraft.gui.screen() + ")", this.phaseTicks);
            return;
        }
        if (this.scenario.isEmpty()) {
            this.finish(minecraft, "failed: " + (this.scenarioName.isEmpty()
                    ? "no scenario given: ./gradlew runClientCapture -Pscenario=<name>, one of " + CaptureScenarios.names()
                    : "unknown scenario \"" + this.scenarioName + "\", known: " + CaptureScenarios.names()));
            return;
        }
        this.saveSettings(minecraft);
        Path folder = this.folder(minecraft);
        if (Files.isDirectory(folder)) {
            try (var old = Files.newDirectoryStream(folder)) {
                for (Path file : old) {
                    Files.delete(file);
                }
            }
        }
        Files.createDirectories(folder);
        Files.writeString(this.valuesFile(minecraft), "# Capture " + this.scenarioName + ": "
                + this.scenario.get().description() + "\n", StandardCharsets.UTF_8);

        String world = WORLD_PREFIX + this.scenarioName;
        LevelStorageSource levels = minecraft.getLevelSource();
        if (levels.levelExists(world)) {
            try (LevelStorageSource.LevelStorageAccess access = levels.createAccess(world)) {
                access.deleteLevel();
            }
        }
        LevelSettings settings = new LevelSettings(world, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        LOGGER.info("Capture {}: creating the world \"{}\"", this.scenarioName, world);
        // The phase changes first: the world loads within this call (Minecraft.doWorldLoad waits for the integrated
        // server without advancing the client ticks, renderFrame(false)), and any later tick finds CREATING.
        this.enter(Phase.CREATING);
        minecraft.createWorldOpenFlows().createFreshLevel(world, settings, new WorldOptions(SEED, false, false),
                WorldPresets::createTestWorldDimensions, new TitleScreen());
    }

    private void entering(Minecraft minecraft) {
        if (minecraft.gui.screen() instanceof PauseScreen) {
            minecraft.gui.setScreen(null); // a window without focus pauses the game once in the world
        }
        boolean inWorld = minecraft.player != null && minecraft.level != null && minecraft.getSingleplayerServer() != null
                && minecraft.gui.screen() == null && minecraft.gui.overlay() == null;
        if (this.phase == Phase.CREATING) {
            if (inWorld) {
                this.enter(Phase.SETTLING);
            } else {
                this.limit("the world did not open", this.phaseTicks);
            }
        } else if (!inWorld) {
            throw new IllegalStateException("the world closed while settling");
        } else if (this.phaseTicks >= SETTLE_TICKS) {
            this.enter(Phase.SETUP);
        }
    }

    private void setup(Minecraft minecraft) {
        if (this.phaseTicks == 1) {
            // Every run first checks that the client draws each block and item of the mod with its own model and
            // textures; in the world, where the components of the items are bound (an item stack needs them).
            ModelCheck.Result models = ModelCheck.run(minecraft);
            if (!models.problems().isEmpty()) {
                throw new IllegalStateException("model check (" + models.summary() + "): " + String.join("; ", models.problems()));
            }
            LOGGER.info("Capture {}: model check passed ({})", this.scenarioName, models.summary());
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> {
                try {
                    // Without output: an operator would see the answers of the commands in the chat of the captures.
                    var source = server.createCommandSourceStack().withSuppressedOutput();
                    server.getCommands().performPrefixedCommand(source, "time set noon");
                    server.getCommands().performPrefixedCommand(source, "time pause");
                    server.getCommands().performPrefixedCommand(source, "weather clear");
                    // The test dimensions of the run (WorldPresets.createTestWorldDimensions) are not those of
                    // vanilla: their lifecycle is experimental, and opening the world again asks to confirm a
                    // warning (WorldOpenFlows.openWorldCheckWorldStemCompatibility, BackupConfirmScreen). A player
                    // confirms it once, then NeoForge saves it in level.dat (confirmedExperimentalSettings): the run
                    // confirms it here, so that the step Restart opens the world as such a player does.
                    if (server.getWorldData() instanceof PrimaryLevelData data) {
                        data.withConfirmedWarning(true);
                    }
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    BlockPos at = player.blockPosition();
                    player.teleportTo(player.level(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, Set.<Relative>of(),
                            Direction.NORTH.toYRot(), 0f, true);
                    this.stand.set(at);
                } catch (RuntimeException e) {
                    this.serverError.set("setup on the server: " + e);
                }
            });
            return;
        }
        this.limit("the player was not placed", this.phaseTicks);
        if (this.stand.get() != null && this.phaseTicks >= STABLE_TICKS) {
            LOGGER.info("Capture {}: in the world, player at {}", this.scenarioName, this.stand.get().toShortString());
            this.standing = this.stand.get();
            this.enter(Phase.STEPS);
        }
    }

    // ---- Steps ----

    private void steps(Minecraft minecraft) throws IOException {
        minecraft.options.keyShift.setDown(this.sneaking);
        minecraft.gui.toastManager().clear(); // no notification over the captures (tutorial, recipes, advancements)
        if (this.expectedScreen == ExpectedScreen.NONE && minecraft.gui.screen() instanceof PauseScreen) {
            minecraft.gui.setScreen(null);
        }
        if (this.inventory != null) {
            this.pointAtHeldItem(minecraft); // the window may have changed size or GUI scale since the last tick
        }
        List<Step> steps = this.scenario.orElseThrow().steps();
        if (this.step >= steps.size()) {
            CaptureScenario scenario = this.scenario.orElseThrow();
            long expected = steps.stream().filter(Step.Shot.class::isInstance).count()
                    * scenario.languages().size() * scenario.sizes().size() * scenario.guiScales().size();
            if (this.screenshots != expected) {
                throw new IllegalStateException(this.screenshots + " screenshots taken, " + expected + " expected");
            }
            this.finish(minecraft, "passed " + this.scenarioName + ": " + this.screenshots + " screenshots in "
                    + minecraft.gameDirectory.toPath().relativize(this.folder(minecraft)));
            return;
        }
        Step current = steps.get(this.step);
        this.stepTicks++;
        int allowed = switch (current) {
            case Step.Wait wait -> wait.ticks();
            case Step.Silence silence -> silence.ticks();
            default -> 0;
        };
        this.limit("step " + (this.step + 1) + " (" + current + ") did not end; " + this.observed(minecraft),
                this.stepTicks - allowed);
        boolean done = switch (current) {
            case Step.BuildScene build -> this.build(minecraft, build);
            case Step.ClearScene clear -> this.clear(minecraft);
            case Step.Aim aim -> this.aim(minecraft, aim);
            case Step.View view -> this.view(minecraft, view);
            case Step.Orbit orbit -> this.orbit(minecraft, orbit);
            case Step.HideHud hide -> this.hideHud(minecraft, hide);
            case Step.Hold hold -> this.hold(minecraft, hold);
            case Step.Use use -> this.use(minecraft);
            case Step.HoverHeldItem hover -> this.hoverHeldItem(minecraft);
            case Step.OpenChat open -> this.openChat(minecraft);
            case Step.CloseScreen close -> this.closeScreen(minecraft);
            case Step.Command command -> this.command(minecraft, command);
            case Step.GameMode mode -> this.gameMode(minecraft, mode);
            case Step.MessageSymbols symbols -> this.messageSymbols(minecraft, symbols);
            case Step.TerminalAdvice advice -> this.booleanOption(minecraft, "terminalAdvice", advice.enabled());
            case Step.Wait wait -> this.stepTicks >= wait.ticks();
            case Step.Silence silence -> this.silence(silence);
            case Step.RemoveCable remove -> this.removeCable(minecraft, remove);
            case Step.Sprint sprint -> this.sprint(minecraft, sprint);
            case Step.Restart restart -> this.restart(minecraft);
            case Step.OpenTerminal open -> this.openTerminal(minecraft, open);
            case Step.Type type -> this.type(minecraft, type);
            case Step.Expect expect -> this.expect(minecraft, expect);
            case Step.Press press -> this.press(minecraft, press);
            case Step.Scroll scroll -> this.scroll(minecraft, scroll);
            case Step.Shot shot -> this.shot(minecraft, shot);
        };
        if (done) {
            LOGGER.info("Capture {}: step {} done: {}", this.scenarioName, this.step + 1, current);
            this.step++;
            this.stepTicks = 0;
            this.stepStarted = false;
        }
    }

    private boolean build(Minecraft minecraft, Step.BuildScene build) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.scene.set(null);
            IntegratedServer server = minecraft.getSingleplayerServer();
            BlockPos origin = this.stand.get().relative(Direction.NORTH, TrialScenes.DISTANCE_FROM_PLAYER);
            server.execute(() -> {
                try {
                    this.scene.set(TrialScenes.build(server.overworld(), build.scene(), origin, Direction.NORTH));
                } catch (RuntimeException e) {
                    this.serverError.set("scene " + build.scene().id() + ": " + e);
                }
            });
            return false;
        }
        TrialScenes.Result result = this.scene.get();
        if (result != null && result.refusal().isPresent()) {
            throw new IllegalStateException("scene " + build.scene().id() + " refused: " + result.refusal().get());
        }
        return result != null && this.stepTicks >= SETTLE_TICKS;
    }

    private boolean aim(Minecraft minecraft, Step.Aim aim) {
        TrialScenes.Result result = this.scene.get();
        if (result == null || aim.device() >= result.devices().size()) {
            throw new IllegalStateException("no device " + aim.device() + " in the last scene");
        }
        BlockPos device = result.devices().get(aim.device());
        BlockPos at = device.relative(Direction.SOUTH, 2);
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.giveReachBack(minecraft);
            this.stableTicks = 0;
            this.sneaking = aim.sneaking();
            this.aimed = device;
            this.standing = at;
            this.face(minecraft, at, device, aim.sneaking());
            return false;
        }
        this.stableTicks = this.aimHolds(minecraft) ? this.stableTicks + 1 : 0;
        return this.stableTicks >= STABLE_TICKS;
    }

    /** The player at {@code at}, facing the centre of the shape of {@code device} (on the server, which tells the client). */
    private void face(Minecraft minecraft, BlockPos at, BlockPos device, boolean sneaking) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            // The centre of the shape of the block, not of the cube: a box is 3/16 of a block high.
            VoxelShape shape = player.level().getBlockState(device).getShape(player.level(), device);
            Vec3 target = shape.isEmpty() ? Vec3.atCenterOf(device) : shape.bounds().getCenter().add(Vec3.atLowerCornerOf(device));
            this.faceTarget(player, at, target, sneaking);
        });
    }

    /** The player at {@code at}, standing, facing this point; on the server thread. */
    private void faceTarget(ServerPlayer player, BlockPos at, Vec3 target) {
        this.faceTarget(player, at, target, false);
    }

    private void faceTarget(ServerPlayer player, BlockPos at, Vec3 target, boolean sneaking) {
        double eyeY = at.getY() + player.getEyeHeight(sneaking ? Pose.CROUCHING : Pose.STANDING);
        double dx = target.x - (at.getX() + 0.5);
        double dy = target.y - eyeY;
        double dz = target.z - (at.getZ() + 0.5);
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
        player.teleportTo(player.level(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, Set.<Relative>of(), yaw, pitch, true);
    }

    private boolean view(Minecraft minecraft, Step.View view) {
        TrialScenes.Result result = this.scene.get();
        if (result == null || Math.max(view.from(), view.target()) >= result.devices().size()) {
            throw new IllegalStateException("no device " + Math.max(view.from(), view.target()) + " in the last scene");
        }
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.sneaking = false;
            this.aimed = null;
            this.standing = result.devices().get(view.from()).east(view.east()).above(view.up()).south(view.south());
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                player.getAbilities().flying = true; // creative: the player stays where the view is
                player.onUpdateAbilities();
            });
            this.face(minecraft, this.standing, result.devices().get(view.target()), false);
            return false;
        }
        boolean there = minecraft.player != null && minecraft.player.blockPosition().equals(this.standing);
        return there && this.stepTicks >= STABLE_TICKS;
    }

    private boolean orbit(Minecraft minecraft, Step.Orbit orbit) {
        TrialScenes.Result result = this.scene.get();
        if (result == null || orbit.device() >= result.devices().size()) {
            throw new IllegalStateException("no device " + orbit.device() + " in the last scene");
        }
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.sneaking = false;
            this.aimed = null;
            this.orbitFeet.set(null);
            BlockPos device = result.devices().get(orbit.device());
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                VoxelShape shape = player.level().getBlockState(device).getShape(player.level(), device);
                Vec3 centre = (shape.isEmpty() ? Vec3.atCenterOf(device) : shape.bounds().getCenter().add(Vec3.atLowerCornerOf(device)))
                        .add(orbit.east(), 0, 0);
                AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
                this.normalReach.compareAndSet(null, reach.getBaseValue());
                reach.setBaseValue(0); // nothing aimed: no outline, no status panel over the view
                // From the south (+z), turned clockwise seen from above: south, west, north, east for 0, 90, 180, 270.
                double around = Math.toRadians(orbit.around());
                double above = Math.toRadians(orbit.above());
                double horizontal = orbit.distance() * Math.cos(above);
                Vec3 eye = centre.add(-Math.sin(around) * horizontal, orbit.distance() * Math.sin(above), Math.cos(around) * horizontal);
                Vec3 look = centre.subtract(eye);
                float yaw = (float) (Mth.atan2(look.z, look.x) * Mth.RAD_TO_DEG) - 90f;
                float pitch = (float) -(Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * Mth.RAD_TO_DEG);
                Vec3 feet = eye.subtract(0, player.getEyeHeight(Pose.STANDING), 0);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(player.level(), feet.x, feet.y, feet.z, Set.<Relative>of(), yaw, pitch, true);
                this.orbitFeet.set(feet);
            });
            return false;
        }
        Vec3 feet = this.orbitFeet.get();
        boolean there = feet != null && minecraft.player != null && minecraft.player.position().distanceTo(feet) < 0.01;
        if (there) {
            this.standing = minecraft.player.blockPosition();
        }
        return there && this.stepTicks >= STABLE_TICKS;
    }

    /** Gives the player back the block reach that a {@link Step.Orbit} set to 0, before an aim. */
    private void giveReachBack(Minecraft minecraft) {
        Double reach = this.normalReach.getAndSet(null);
        if (reach != null) {
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> server.getPlayerList().getPlayers().getFirst()
                    .getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(reach));
        }
    }

    private boolean hideHud(Minecraft minecraft, Step.HideHud hide) {
        if (minecraft.gui.hud.isHidden() != hide.hidden()) {
            minecraft.gui.hud.toggle();
        }
        return this.stepTicks >= STABLE_TICKS;
    }

    private boolean clear(Minecraft minecraft) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            TrialScenes.Result result = this.scene.get();
            if (result == null) {
                throw new IllegalStateException("no scene to clear");
            }
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> TrialScenes.remove(server.overworld(), result.devices()));
            this.aimed = null;
            return false;
        }
        return this.stepTicks >= STABLE_TICKS;
    }

    private boolean hold(Minecraft minecraft, Step.Hold hold) {
        ItemStack stack = hold.item() == null ? ItemStack.EMPTY : new ItemStack(hold.item().get());
        if (!this.stepStarted) {
            this.stepStarted = true;
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> server.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND, stack.copy()));
            return false;
        }
        boolean held = minecraft.player != null && ItemStack.isSameItem(minecraft.player.getMainHandItem(), stack)
                && minecraft.player.getMainHandItem().isEmpty() == stack.isEmpty();
        return held && this.stepTicks >= STABLE_TICKS;
    }

    private boolean use(Minecraft minecraft) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            if (minecraft.gui.screen() != null) {
                throw new IllegalStateException("a click of the use key with a screen open");
            }
            KeyMapping.click(minecraft.options.keyUse.getKey());
            return false;
        }
        return this.stepTicks >= STABLE_TICKS;
    }

    private boolean hoverHeldItem(Minecraft minecraft) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.inventory = new CaptureInventoryScreen(minecraft.player);
            this.expectedScreen = ExpectedScreen.INVENTORY;
            minecraft.gui.setScreen(this.inventory);
            return false;
        }
        return this.screenHolds(minecraft) && this.stepTicks >= STABLE_TICKS;
    }

    /** Moves the mouse over the selected slot of the hotbar, as the operating system would report it. */
    private void pointAtHeldItem(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.gui.screen() != this.inventory) {
            return;
        }
        Slot slot = this.heldSlot(minecraft);
        Window window = minecraft.getWindow();
        double x = this.inventory.slotCentreX(slot) * window.getScreenWidth() / window.getGuiScaledWidth();
        double y = this.inventory.slotCentreY(slot) * window.getScreenHeight() / window.getGuiScaledHeight();
        if (minecraft.mouseHandler.xpos() != x || minecraft.mouseHandler.ypos() != y) {
            minecraft.mouseHandler.onMove(window.handle(), x, y, x - minecraft.mouseHandler.xpos(), y - minecraft.mouseHandler.ypos());
        }
    }

    /** The slot of the inventory menu for the selected slot of the hotbar (hotbar slots come after the 36 others). */
    private Slot heldSlot(Minecraft minecraft) {
        return this.inventory.getMenu().slots.get(36 + minecraft.player.getInventory().getSelectedSlot());
    }

    private boolean openChat(Minecraft minecraft) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.expectedScreen = ExpectedScreen.CHAT;
            minecraft.gui.setScreen(new ChatScreen("", false));
            return false;
        }
        return this.screenHolds(minecraft) && this.stepTicks >= STABLE_TICKS;
    }

    private boolean closeScreen(Minecraft minecraft) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.expectedScreen = ExpectedScreen.NONE;
            this.inventory = null;
            minecraft.gui.setScreen(null);
            return false;
        }
        return minecraft.gui.screen() == null && this.stepTicks >= STABLE_TICKS;
    }

    private boolean command(Minecraft minecraft, Step.Command command) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            minecraft.player.connection.sendCommand(command.command());
            return false;
        }
        return this.stepTicks >= 2 * STABLE_TICKS;
    }

    private boolean gameMode(Minecraft minecraft, Step.GameMode mode) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> server.getPlayerList().getPlayers().getFirst().setGameMode(mode.mode()));
            return false;
        }
        return minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == mode.mode() && this.stepTicks >= STABLE_TICKS;
    }

    /** Edits the line {@code messageSymbols = ...} of the client options file of the mod, which the game reads again. */
    private boolean messageSymbols(Minecraft minecraft, Step.MessageSymbols symbols) throws IOException {
        return this.booleanOption(minecraft, "messageSymbols", symbols.enabled());
    }

    /**
     * Edits the line {@code name = true|false} of the client options file of the mod, as a player would; the game
     * watches the file and reads it again. The whole file is put back at the end of the run.
     */
    private boolean booleanOption(Minecraft minecraft, String name, boolean value) throws IOException {
        if (!this.stepStarted) {
            this.stepStarted = true;
            Path file = minecraft.gameDirectory.toPath().resolve(MOD_CLIENT_CONFIG);
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException(MOD_CLIENT_CONFIG + " not found");
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            String line = "\\b" + name + "\\s*=\\s*(true|false)";
            if (!content.matches("(?s).*" + line + ".*")) {
                throw new IllegalStateException("no " + name + " option in " + MOD_CLIENT_CONFIG);
            }
            Files.writeString(file, content.replaceAll(line, name + " = " + value), StandardCharsets.UTF_8);
            return false;
        }
        return this.stepTicks >= 4 * STABLE_TICKS;
    }

    /** Aims at a computer (the keyboard of an open laptop), clicks the use key, waits for its terminal screen. */
    private boolean openTerminal(Minecraft minecraft, Step.OpenTerminal open) {
        TerminalScenes.requireDevice(this.scene.get(), open.device());
        BlockPos device = this.scene.get().devices().get(open.device());
        BlockPos at = device.relative(Direction.SOUTH, 2);
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.giveReachBack(minecraft);
            this.terminalClicked = false;
            this.stableTicks = 0;
            this.sneaking = false;
            this.aimed = null;
            this.standing = at;
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                this.faceTarget(player, at, TerminalScenes.clickPoint(player.level().getBlockState(device), device));
            });
            return false;
        }
        if (!this.terminalClicked) {
            boolean onDevice = this.aimedBlock(minecraft).filter(device::equals).isPresent() && minecraft.gui.screen() == null;
            this.stableTicks = onDevice ? this.stableTicks + 1 : 0;
            if (this.stableTicks >= STABLE_TICKS) {
                KeyMapping.click(minecraft.options.keyUse.getKey());
                this.terminalClicked = true;
                this.stableTicks = 0;
            }
            return false;
        }
        boolean shown = minecraft.gui.screen() instanceof TerminalScreen terminal && terminal.device().equals(device);
        this.stableTicks = shown ? this.stableTicks + 1 : 0;
        if (this.stableTicks >= STABLE_TICKS) {
            this.expectedScreen = ExpectedScreen.TERMINAL;
            return true;
        }
        return false;
    }

    private boolean type(Minecraft minecraft, Step.Type type) {
        if (!this.stepStarted) {
            this.typedAt = -1;
        }
        Optional<Map<Integer, String>> addresses = this.addressesFor(minecraft, type.text());
        if (addresses.isEmpty()) {
            return false;
        }
        if (this.typedAt < 0) {
            TerminalScreen terminal = this.terminal(minecraft);
            withAddresses(type.text(), addresses.get(), false).codePoints()
                    .forEach(codepoint -> terminal.charTyped(new CharacterEvent(codepoint)));
            this.typedAt = this.stepTicks;
            return false;
        }
        return this.stepTicks >= this.typedAt + 2;
    }

    /** Counts the rows shown by the terminal that match, and fails the run if the count is not the one expected. */
    private boolean expect(Minecraft minecraft, Step.Expect expect) {
        Optional<Map<Integer, String>> addresses = this.addressesFor(minecraft, expect.regex());
        if (addresses.isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile(withAddresses(expect.regex(), addresses.get(), true));
        List<String> rows = this.terminal(minecraft).layout().visibleRows();
        long found = rows.stream().filter(row -> pattern.matcher(row.stripTrailing()).matches()).count();
        if (found != expect.count()) {
            throw new IllegalStateException("expected " + expect.count() + " rows matching \"" + pattern + "\" in the terminal, found "
                    + found + ": " + rows);
        }
        return true;
    }

    /**
     * The addresses named by {@code {address:N}} in this text, asked to the server at the first tick of the step: empty
     * until they arrive. A computer without a DHCP lease fails the run.
     */
    private Optional<Map<Integer, String>> addressesFor(Minecraft minecraft, String text) {
        if (this.stepStarted) {
            return Optional.ofNullable(this.addresses.get());
        }
        this.stepStarted = true;
        this.addresses.set(null);
        Set<Integer> wanted = ADDRESS_OF.matcher(text).results()
                .map(match -> Integer.parseInt(match.group(1))).collect(Collectors.toSet());
        if (wanted.isEmpty()) {
            this.addresses.set(Map.of());
            return Optional.empty();
        }
        TrialScenes.Result result = this.scene.get();
        wanted.forEach(device -> TerminalScenes.requireDevice(result, device));
        IntegratedServer server = minecraft.getSingleplayerServer();
        server.execute(() -> {
            try {
                Map<Integer, String> found = new HashMap<>();
                for (int device : wanted) {
                    DeviceId id = NetworkDevices.idOf(server.overworld(), result.devices().get(device));
                    Optional<String> address = NetworkManager.of(server).logic(id)
                            .filter(HostLogic.class::isInstance)
                            .flatMap(logic -> ((HostLogic) logic).dhcpClient(0).lease())
                            .map(lease -> lease.address().toString());
                    if (address.isEmpty()) {
                        this.serverError.set("device " + device + " of the scene has no DHCP lease");
                        return;
                    }
                    found.put(device, address.get());
                }
                this.addresses.set(Map.copyOf(found));
            } catch (RuntimeException e) {
                this.serverError.set("addresses of the scene: " + e);
            }
        });
        return Optional.empty();
    }

    /** The text with each {@code {address:N}} replaced by the address, quoted for a regular expression or not. */
    private static String withAddresses(String text, Map<Integer, String> addresses, boolean quoted) {
        return ADDRESS_OF.matcher(text).replaceAll(match -> {
            String address = addresses.get(Integer.parseInt(match.group(1)));
            return Matcher.quoteReplacement(quoted ? Pattern.quote(address) : address);
        });
    }

    /** A key to the terminal; Esc and the end of the shell close it, and the screen expected follows. */
    private boolean press(Minecraft minecraft, Step.Press press) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            Step.TerminalKey key = press.key();
            this.terminal(minecraft).keyPressed(new KeyEvent(key.key, key.keycode, key.modifiers));
            if (key == Step.TerminalKey.ESCAPE) {
                this.expectedScreen = ExpectedScreen.NONE;
            }
            return false;
        }
        if (this.expectedScreen == ExpectedScreen.TERMINAL && !(minecraft.gui.screen() instanceof TerminalScreen)) {
            this.expectedScreen = ExpectedScreen.NONE; // the server closed it: the shell ended
        }
        return this.stepTicks >= STABLE_TICKS;
    }

    /** Wheel notches, one event each, through the handler the operating system calls; the terminal must be open. */
    private boolean scroll(Minecraft minecraft, Step.Scroll scroll) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.terminal(minecraft);
            long handle = minecraft.getWindow().handle();
            for (int i = 0; i < Math.abs(scroll.notches()); i++) {
                minecraft.mouseHandler.onScroll(handle, 0.0, Math.signum(scroll.notches()));
            }
            return false;
        }
        return this.stepTicks >= STABLE_TICKS;
    }

    private TerminalScreen terminal(Minecraft minecraft) {
        if (minecraft.gui.screen() instanceof TerminalScreen terminal) {
            return terminal;
        }
        throw new IllegalStateException("no terminal open (screen " + minecraft.gui.screen() + ")");
    }

    private boolean silence(Step.Silence silence) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.silenceMark = this.chat.size();
            return false;
        }
        if (this.chat.size() > this.silenceMark) {
            throw new IllegalStateException("a line reached the chat after " + this.stepTicks + " of " + silence.ticks()
                    + " ticks of silence: " + this.chat.subList(this.silenceMark, this.chat.size()));
        }
        return this.stepTicks >= silence.ticks();
    }

    private boolean removeCable(Minecraft minecraft, Step.RemoveCable remove) {
        TrialScenes.Result result = this.scene.get();
        if (result == null || Math.max(remove.from(), remove.to()) >= result.devices().size()) {
            throw new IllegalStateException("no device " + Math.max(remove.from(), remove.to()) + " in the last scene");
        }
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.cableRemoved.set(false);
            BlockPos fromPos = result.devices().get(remove.from());
            BlockPos toPos = result.devices().get(remove.to());
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> {
                try {
                    NetworkManager network = NetworkManager.of(server);
                    DeviceId from = NetworkDevices.idOf(server.overworld(), fromPos);
                    DeviceId to = NetworkDevices.idOf(server.overworld(), toPos);
                    Optional<Link> link = network.linksOf(from).stream()
                            .filter(candidate -> candidate.a().device().equals(to) || candidate.b().device().equals(to))
                            .findFirst();
                    if (link.isEmpty()) {
                        this.serverError.set("no cable between devices " + remove.from() + " and " + remove.to());
                    } else if (network.disconnect(link.get().a()).isPresent()) {
                        this.cableRemoved.set(true);
                    } else {
                        this.serverError.set("the engine did not remove the cable " + link.get());
                    }
                } catch (RuntimeException e) {
                    this.serverError.set("cable removal on the server: " + e);
                }
            });
            return false;
        }
        return this.cableRemoved.get() && this.stepTicks >= STABLE_TICKS;
    }

    private boolean sprint(Minecraft minecraft, Step.Sprint sprint) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.sprintOver.set(false);
            this.sprintQueried.set(false);
            server.execute(() -> server.tickRateManager().requestGameToSprint(sprint.ticks()));
            return false;
        }
        // Asked on the server thread, after the request (its tasks run in order): never a state read across threads.
        if (!this.sprintOver.get() && this.sprintQueried.compareAndSet(false, true)) {
            server.execute(() -> {
                this.sprintOver.set(!server.tickRateManager().isSprinting());
                this.sprintQueried.set(false);
            });
        }
        return this.sprintOver.get() && this.stepTicks >= STABLE_TICKS;
    }

    /**
     * Quits to the title screen and opens the world again. The panel event is posted only when the panel changes: the
     * player looks up first, so that the run sees the panel hide, and a panel after the reopening is drawn again.
     */
    private boolean restart(Minecraft minecraft) {
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.restarting = Restarting.LOOKING_AWAY;
            this.aimed = null;
            this.sneaking = false;
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(),
                        player.getYRot(), -90f, true);
            });
            return false;
        }
        String world = WORLD_PREFIX + this.scenarioName;
        switch (this.restarting) {
            case LOOKING_AWAY -> {
                boolean hidden = this.panel != null && this.panel.device().isEmpty() && this.aimedBlock(minecraft).isEmpty();
                if (hidden && this.stepTicks >= STABLE_TICKS) {
                    LOGGER.info("Capture {}: saving and quitting to the title screen", this.scenarioName);
                    // The stage changes first: the call saves and stops the integrated server before it returns.
                    this.restarting = Restarting.REOPENING;
                    minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
                }
            }
            case REOPENING -> {
                if (minecraft.gui.screen() instanceof TitleScreen && minecraft.level == null) {
                    LOGGER.info("Capture {}: opening \"{}\" again", this.scenarioName, world);
                    this.restarting = Restarting.ENTERING;
                    this.enteredTicks = -1;
                    minecraft.createWorldOpenFlows().openWorld(world,
                            () -> this.serverError.set("opening \"" + world + "\" again was cancelled"));
                }
            }
            case ENTERING -> {
                boolean inWorld = minecraft.player != null && minecraft.level != null
                        && minecraft.getSingleplayerServer() != null && minecraft.gui.screen() == null
                        && minecraft.gui.overlay() == null;
                if (!inWorld) {
                    this.enteredTicks = -1; // a screen of the game (warning, backup) is reported by the time limit
                } else if (++this.enteredTicks >= SETTLE_TICKS) {
                    LOGGER.info("Capture {}: in the world again", this.scenarioName);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * True while the last aim still holds, as observed on the client: the crosshair on the device, the posture asked,
     * the panel of that device drawn. True when no aim was asked yet.
     */
    private boolean aimHolds(Minecraft minecraft) {
        if (this.aimed == null) {
            return true;
        }
        boolean posture = minecraft.player != null && minecraft.player.isShiftKeyDown() == this.sneaking;
        boolean drawn = this.panel != null && this.panel.device().filter(this.aimed::equals).isPresent();
        return this.aimedBlock(minecraft).filter(this.aimed::equals).isPresent() && posture && drawn;
    }

    /** True while the screen expected is the one open, and, without a screen, while the last aim still holds. */
    private boolean screenHolds(Minecraft minecraft) {
        Screen screen = minecraft.gui.screen();
        return switch (this.expectedScreen) {
            case NONE -> screen == null && this.aimHolds(minecraft);
            case CHAT -> screen instanceof ChatScreen;
            case TERMINAL -> screen instanceof TerminalScreen;
            case INVENTORY -> screen == this.inventory && minecraft.player != null
                    && this.inventory.hovered().filter(slot -> slot == this.heldSlot(minecraft) && slot.hasItem()).isPresent();
        };
    }

    /** What the client shows now, for the message of a step that does not end. */
    private String observed(Minecraft minecraft) {
        return "crosshair on " + this.aimedBlock(minecraft).map(BlockPos::toShortString).orElse("no block")
                + ", " + (minecraft.player != null && minecraft.player.isShiftKeyDown() ? "sneaking" : "standing")
                + ", panel " + (this.panel == null || this.panel.device().isEmpty() ? "none"
                        : "of " + this.panel.device().get().toShortString())
                + (this.aimed == null ? "" : ", device aimed at " + this.aimed.toShortString())
                + ", screen " + (minecraft.gui.screen() == null ? "none" : minecraft.gui.screen().getClass().getSimpleName());
    }

    /** The block under the crosshair, if there is one. */
    private Optional<BlockPos> aimedBlock(Minecraft minecraft) {
        return minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                ? Optional.of(hit.getBlockPos()) : Optional.empty();
    }

    /** One screenshot per combination: languages first (each change reloads the resources), then sizes, then scales. */
    private boolean shot(Minecraft minecraft, Step.Shot shot) throws IOException {
        CaptureScenario scenario = this.scenario.orElseThrow();
        int total = scenario.languages().size() * scenario.sizes().size() * scenario.guiScales().size();
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.combination = 0;
            this.apply(minecraft, scenario);
            return false;
        }
        String language = this.language(scenario);
        Size size = this.size(scenario);
        int guiScale = this.guiScale(scenario);
        String file = shot.name() + "-" + size + "-" + (guiScale == 0 ? "auto" : "scale" + guiScale) + "-" + language + ".png";
        Window window = minecraft.getWindow();
        if (this.waitingForSave) {
            if (!this.saved.get()) {
                return false;
            }
            this.waitingForSave = false;
            Path image = this.folder(minecraft).resolve(file);
            if (!Files.isRegularFile(image) || Files.size(image) == 0) {
                throw new IllegalStateException("screenshot " + file + " not written");
            }
            this.screenshots++;
            this.writeValues(minecraft, file, language);
            this.checkTerminalFrame(minecraft, file);
            this.combination++;
            if (this.combination >= total) {
                this.chatWritten = this.chat.size();
                return true;
            }
            this.stepTicks = 0;
            this.apply(minecraft, scenario);
            return false;
        }
        boolean reloaded = this.reload == null || (this.reload.isDone() && minecraft.gui.overlay() == null);
        boolean ready = reloaded && window.getWidth() == size.width() && window.getHeight() == size.height()
                && minecraft.options.guiScale().get() == guiScale
                && minecraft.getLanguageManager().getSelected().equals(language)
                && this.screenHolds(minecraft);
        if (this.useTicks >= 0) {
            // A click was made for this screenshot: the server answers within a few ticks.
            if (++this.useTicks < USE_ANSWER_TICKS) {
                return false;
            }
            this.useTicks = -1;
        } else {
            this.stableTicks = ready ? this.stableTicks + 1 : 0;
            if (this.stableTicks < STABLE_TICKS) {
                return false;
            }
            if (shot.useFirst()) {
                KeyMapping.click(minecraft.options.keyUse.getKey()); // the message of the action bar fades after 3 s
                this.useTicks = 0;
                return false;
            }
        }
        this.saved.set(false);
        this.waitingForSave = true;
        Screenshot.grab(minecraft.gameDirectory, this.scenarioName + "/" + file, minecraft.gameRenderer.mainRenderTarget(), 1,
                message -> this.saved.set(true));
        return false;
    }

    private String language(CaptureScenario scenario) {
        return scenario.languages().get(this.combination / (scenario.sizes().size() * scenario.guiScales().size()));
    }

    private Size size(CaptureScenario scenario) {
        return scenario.sizes().get(this.combination / scenario.guiScales().size() % scenario.sizes().size());
    }

    private int guiScale(CaptureScenario scenario) {
        return scenario.guiScales().get(this.combination % scenario.guiScales().size());
    }

    /** Asks for the combination: the language (reloads the resources if it changes), the window size, the GUI scale. */
    private void apply(Minecraft minecraft, CaptureScenario scenario) {
        this.stableTicks = 0;
        String language = this.language(scenario);
        if (!minecraft.getLanguageManager().getSelected().equals(language)) {
            minecraft.getLanguageManager().setSelected(language);
            minecraft.options.languageCode = language;
            this.reload = minecraft.reloadResourcePacks();
        }
        Size size = this.size(scenario);
        Window window = minecraft.getWindow();
        float density = window.getPixelDensity();
        int width = Math.round(size.width() / density);
        int height = Math.round(size.height() / density);
        if (window.getScreenWidth() != width || window.getScreenHeight() != height) {
            window.setWindowed(width, height);
        }
        minecraft.options.guiScale().set(this.guiScale(scenario));
    }

    private void writeValues(Minecraft minecraft, String file, String language) throws IOException {
        Window window = minecraft.getWindow();
        StringBuilder line = new StringBuilder(file)
                .append(" | language ").append(language)
                .append(" | window ").append(window.getScreenWidth()).append('x').append(window.getScreenHeight()).append(" pt")
                .append(" | framebuffer ").append(window.getWidth()).append('x').append(window.getHeight())
                .append(" | GUI scale ").append(window.getGuiScale())
                .append(minecraft.options.guiScale().get() == 0 ? " (auto)" : " (set)")
                .append(" | GUI ").append(window.getGuiScaledWidth()).append('x').append(window.getGuiScaledHeight())
                .append(" | game time ").append(minecraft.level == null ? "none" : String.valueOf(minecraft.level.getGameTime()))
                .append(" | ").append(minecraft.player != null && minecraft.player.isShiftKeyDown() ? "sneaking" : "standing")
                .append(" | crosshair on ").append(this.aimedBlock(minecraft).map(BlockPos::toShortString).orElse("no block"));
        if (this.panel == null || this.panel.device().isEmpty()) {
            line.append(" | panel: none");
        } else {
            line.append(" | panel at ").append(this.panel.device().get().toShortString()).append(": ").append(this.panel.name());
            this.panel.lines().forEach(text -> line.append(" / ").append(text));
        }
        line.append(" | screen ").append(minecraft.gui.screen() == null ? "none" : minecraft.gui.screen().getClass().getSimpleName());
        if (minecraft.gui.screen() instanceof TerminalScreen terminal) {
            TerminalScreen.Layout grid = terminal.layout();
            line.append(" | terminal ").append(grid.columns()).append('x').append(grid.rows())
                    .append(", text size ").append(grid.scale()).append(grid.automatic() ? " (auto)" : " (set)")
                    .append(", scrolled up ").append(grid.scrollUp())
                    .append(" | frame ").append(grid.frame().frame()).append(" in a GUI of ")
                    .append(grid.guiWidth()).append('x').append(grid.guiHeight())
                    .append(", title ").append(grid.frame().title())
                    .append(", text area ").append(grid.frame().text())
                    .append(" | rows:");
            grid.visibleRows().forEach(row -> line.append(" ⏎ ").append(row));
        }
        if (this.inventory != null && minecraft.gui.screen() == this.inventory) {
            line.append(" | tooltip:");
            Screen.getTooltipFromItem(minecraft, this.heldSlot(minecraft).getItem())
                    .forEach(text -> line.append(" / ").append(text.getString()));
        }
        if (this.actionBar != null && this.clientTicks - this.actionBarTick <= ACTION_BAR_TICKS) {
            line.append(" | action bar: ").append(this.actionBar);
        }
        line.append(" | chat since the previous shot: ").append(this.chat.subList(this.chatWritten, this.chat.size()));
        line.append('\n');
        Files.writeString(this.valuesFile(minecraft), line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        LOGGER.info("Capture {}: {}", this.scenarioName, line.toString().strip());
    }

    /**
     * A terminal open fails the run when its frame was drawn for another GUI size than the window's, or leaves it, or
     * when its title (accents included) or its text area crosses the border ({@link TerminalLayout#frameProblem}).
     */
    private void checkTerminalFrame(Minecraft minecraft, String file) {
        if (minecraft.gui.screen() instanceof TerminalScreen terminal) {
            TerminalScreen.Layout grid = terminal.layout();
            Window window = minecraft.getWindow();
            int width = window.getGuiScaledWidth();
            int height = window.getGuiScaledHeight();
            if (grid.guiWidth() != width || grid.guiHeight() != height) {
                throw new IllegalStateException(file + ": the terminal was drawn for a GUI of " + grid.guiWidth() + "x"
                        + grid.guiHeight() + ", the window has " + width + "x" + height);
            }
            TerminalLayout.frameProblem(grid.frame(), width, height).ifPresent(problem -> {
                throw new IllegalStateException(file + ": " + problem);
            });
        }
    }

    // ---- Settings and end ----

    private void saveSettings(Minecraft minecraft) {
        Window window = minecraft.getWindow();
        this.originalWidth = window.getScreenWidth();
        this.originalHeight = window.getScreenHeight();
        this.originalGuiScale = minecraft.options.guiScale().get();
        this.originalLanguage = minecraft.getLanguageManager().getSelected();
        this.originalPauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        this.originalTutorialStep = minecraft.options.tutorialStep;
        this.originalHudHidden = minecraft.gui.hud.isHidden();
        Path modConfig = minecraft.gameDirectory.toPath().resolve(MOD_CLIENT_CONFIG);
        try {
            this.originalModConfig = Files.isRegularFile(modConfig) ? Files.readString(modConfig, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + MOD_CLIENT_CONFIG, e);
        }
        this.settingsSaved = true;
        minecraft.options.pauseOnLostFocus = false;
        // The hints of the tutorial ("Move with ...") are drawn over the top right corner, where they hide the panel.
        minecraft.getTutorial().setStep(TutorialSteps.NONE);
    }

    /** Puts back what the run changed; never throws, the end of the run must happen. */
    private void restoreSettings(Minecraft minecraft) {
        if (!this.settingsSaved) {
            return;
        }
        this.settingsSaved = false;
        try {
            minecraft.options.keyShift.setDown(false);
            minecraft.options.pauseOnLostFocus = this.originalPauseOnLostFocus;
            minecraft.options.tutorialStep = this.originalTutorialStep;
            if (minecraft.gui.hud.isHidden() != this.originalHudHidden) {
                minecraft.gui.hud.toggle();
            }
            if (this.originalModConfig != null) {
                Files.writeString(minecraft.gameDirectory.toPath().resolve(MOD_CLIENT_CONFIG), this.originalModConfig,
                        StandardCharsets.UTF_8);
                LOGGER.info("Capture {}: {} put back", this.scenarioName, MOD_CLIENT_CONFIG);
            }
            minecraft.options.guiScale().set(this.originalGuiScale);
            minecraft.options.languageCode = this.originalLanguage;
            minecraft.getLanguageManager().setSelected(this.originalLanguage);
            minecraft.getWindow().setWindowed(this.originalWidth, this.originalHeight);
            minecraft.options.save();
            LOGGER.info("Capture {}: window {}x{} pt, GUI scale option {}, language {} put back", this.scenarioName,
                    this.originalWidth, this.originalHeight, this.originalGuiScale, this.originalLanguage);
        } catch (RuntimeException | IOException e) {
            LOGGER.error("Capture {}: could not put the settings back", this.scenarioName, e);
        }
    }

    /** Puts the settings back, writes the result read by Gradle, leaves the world and quits the game. */
    private void finish(Minecraft minecraft, String result) {
        this.phase = Phase.FINISHED;
        this.restoreSettings(minecraft);
        Path file = minecraft.gameDirectory.toPath().resolve("capture-result.txt");
        try {
            Files.writeString(file, result + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Capture {}: could not write {}", this.scenarioName, file, e);
        }
        if (result.startsWith("passed")) {
            LOGGER.info("Capture {}: {}", this.scenarioName, result);
        } else {
            LOGGER.error("Capture {}: {}", this.scenarioName, result);
        }
        try {
            if (minecraft.level != null) {
                minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
            }
        } finally {
            minecraft.stop();
        }
    }

    private Path folder(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("screenshots").resolve(this.scenarioName);
    }

    private Path valuesFile(Minecraft minecraft) {
        return this.folder(minecraft).resolve("values.txt");
    }
}
