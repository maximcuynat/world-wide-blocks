package io.github.maximcuynat.worldwideblocks.capture;

import io.github.maximcuynat.worldwideblocks.client.StatusPanelShownEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Capture runs of World Wide Blocks ({@code docs/automated-checks.md}): a development-only client mod, loaded by
 * the {@code clientCapture} run alone ({@code build.gradle}), never in the jar nor on a dedicated server. It runs the
 * scenario named by the system property {@value #SCENARIO_PROPERTY} ({@code -Pscenario=}), then quits the game.
 */
@Mod(value = WorldWideBlocksCapture.MODID, dist = Dist.CLIENT)
public final class WorldWideBlocksCapture {
    public static final String MODID = "worldwideblocks_capture";
    /** Set by the {@code clientCapture} run; absent anywhere else, where this mod does nothing. */
    public static final String SCENARIO_PROPERTY = "worldwideblocks.capture.scenario";

    public WorldWideBlocksCapture() {
        String scenario = System.getProperty(SCENARIO_PROPERTY);
        if (scenario == null) {
            return;
        }
        CaptureRun run = new CaptureRun(scenario.strip());
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, run::onClientTick);
        NeoForge.EVENT_BUS.addListener(StatusPanelShownEvent.class, run::onPanel);
        NeoForge.EVENT_BUS.addListener(ClientChatReceivedEvent.System.class, run::onChat);
    }
}
