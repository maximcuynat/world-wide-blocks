package io.github.maximcuynat.worldwideblocks.capture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;

import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;

import io.github.maximcuynat.worldwideblocks.capture.CaptureScenario.Size;
import io.github.maximcuynat.worldwideblocks.capture.CaptureScenario.Step;
import io.github.maximcuynat.worldwideblocks.client.StatusPanelShownEvent;
import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
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

    // World, written by the server thread.
    private final AtomicReference<BlockPos> stand = new AtomicReference<>();
    private final AtomicReference<TrialScenes.Result> scene = new AtomicReference<>();
    private final AtomicReference<String> serverError = new AtomicReference<>();

    // Steps.
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

    /** The last panel drawn, as the client posted it. */
    private StatusPanelShownEvent panel;

    CaptureRun(String scenarioName) {
        this.scenarioName = scenarioName;
        this.scenario = CaptureScenarios.byName(scenarioName);
    }

    void onPanel(StatusPanelShownEvent event) {
        this.panel = event;
    }

    void onClientTick(ClientTickEvent.Post event) {
        if (this.phase == Phase.FINISHED) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
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
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> {
                try {
                    // Without output: an operator would see the answers of the commands in the chat of the captures.
                    var source = server.createCommandSourceStack().withSuppressedOutput();
                    server.getCommands().performPrefixedCommand(source, "time set noon");
                    server.getCommands().performPrefixedCommand(source, "time pause");
                    server.getCommands().performPrefixedCommand(source, "weather clear");
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
            this.enter(Phase.STEPS);
        }
    }

    // ---- Steps ----

    private void steps(Minecraft minecraft) throws IOException {
        minecraft.options.keyShift.setDown(this.sneaking);
        minecraft.gui.toastManager().clear(); // no notification over the captures (tutorial, recipes, advancements)
        if (minecraft.gui.screen() instanceof PauseScreen) {
            minecraft.gui.setScreen(null);
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
        this.limit("step " + (this.step + 1) + " (" + current + ") did not end; " + this.observed(minecraft), this.stepTicks);
        boolean done = switch (current) {
            case Step.BuildScene build -> this.build(minecraft, build);
            case Step.Aim aim -> this.aim(minecraft, aim);
            case Step.Wait wait -> this.stepTicks >= wait.ticks();
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
        BlockPos at = this.stand.get();
        if (!this.stepStarted) {
            this.stepStarted = true;
            this.stableTicks = 0;
            this.sneaking = aim.sneaking();
            this.aimed = device;
            IntegratedServer server = minecraft.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                // The centre of the shape of the block, not of the cube: a box is 3/16 of a block high.
                VoxelShape shape = player.level().getBlockState(device).getShape(player.level(), device);
                Vec3 target = shape.isEmpty() ? Vec3.atCenterOf(device) : shape.bounds().getCenter().add(Vec3.atLowerCornerOf(device));
                double eyeY = at.getY() + player.getEyeHeight(aim.sneaking() ? Pose.CROUCHING : Pose.STANDING);
                double dx = target.x - (at.getX() + 0.5);
                double dy = target.y - eyeY;
                double dz = target.z - (at.getZ() + 0.5);
                float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
                float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
                player.teleportTo(player.level(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, Set.<Relative>of(), yaw, pitch, true);
            });
            return false;
        }
        this.stableTicks = this.aimHolds(minecraft) ? this.stableTicks + 1 : 0;
        return this.stableTicks >= STABLE_TICKS;
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
            this.combination++;
            if (this.combination >= total) {
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
                && minecraft.gui.screen() == null
                && this.aimHolds(minecraft);
        this.stableTicks = ready ? this.stableTicks + 1 : 0;
        if (this.stableTicks >= STABLE_TICKS) {
            this.saved.set(false);
            this.waitingForSave = true;
            Screenshot.grab(minecraft.gameDirectory, this.scenarioName + "/" + file, minecraft.gameRenderer.mainRenderTarget(), 1,
                    message -> this.saved.set(true));
        }
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
                .append(" | ").append(minecraft.player != null && minecraft.player.isShiftKeyDown() ? "sneaking" : "standing")
                .append(" | crosshair on ").append(this.aimedBlock(minecraft).map(BlockPos::toShortString).orElse("no block"));
        if (this.panel == null || this.panel.device().isEmpty()) {
            line.append(" | panel: none");
        } else {
            line.append(" | panel at ").append(this.panel.device().get().toShortString()).append(": ").append(this.panel.name());
            this.panel.lines().forEach(text -> line.append(" / ").append(text));
        }
        line.append('\n');
        Files.writeString(this.valuesFile(minecraft), line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        LOGGER.info("Capture {}: {}", this.scenarioName, line.toString().strip());
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
            minecraft.options.guiScale().set(this.originalGuiScale);
            minecraft.options.languageCode = this.originalLanguage;
            minecraft.getLanguageManager().setSelected(this.originalLanguage);
            minecraft.getWindow().setWindowed(this.originalWidth, this.originalHeight);
            minecraft.options.save();
            LOGGER.info("Capture {}: window {}x{} pt, GUI scale option {}, language {} put back", this.scenarioName,
                    this.originalWidth, this.originalHeight, this.originalGuiScale, this.originalLanguage);
        } catch (RuntimeException e) {
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
