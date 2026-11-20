package io.github.maximcuynat.worldwideblocks.client;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.Event;

/**
 * The status panel drawn on the screen changed: posted on {@code NeoForge.EVENT_BUS} by {@link PortStatusLayer} when
 * what it draws is no longer what it drew before, including when it disappears. The texts are those of the screen,
 * in the language of the game. Read by the capture runs ({@code src/capture}), which write them next to their
 * screenshots; a future HUD or narration can listen to it as well.
 *
 * <p>Why in the shipped client: the panel is a layer of the mod, and no event of the game carries what it draws, so
 * the capture mod cannot read it otherwise. Its cost when nobody listens: one comparison per frame of what is already
 * computed for drawing; the texts are built and the event posted only when the panel changes (device, lines,
 * loaded language), and it carries no state of the server.
 *
 * @param device the device aimed at, empty when no panel is drawn
 * @param name   name of the device as drawn, empty when no panel is drawn
 * @param lines  the lines under the name, each prefixed with the colour of its light ({@code [green] }), if it has one
 */
public final class StatusPanelShownEvent extends Event {
    private final Optional<BlockPos> device;
    private final String name;
    private final List<String> lines;

    StatusPanelShownEvent(Optional<BlockPos> device, String name, List<String> lines) {
        this.device = device;
        this.name = name;
        this.lines = List.copyOf(lines);
    }

    public Optional<BlockPos> device() {
        return this.device;
    }

    public String name() {
        return this.name;
    }

    public List<String> lines() {
        return this.lines;
    }
}
