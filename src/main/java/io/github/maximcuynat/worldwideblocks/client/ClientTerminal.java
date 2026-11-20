package io.github.maximcuynat.worldwideblocks.client;

import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.sync.TerminalClosePayload;
import io.github.maximcuynat.worldwideblocks.network.sync.TerminalUpdatesPayload;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalUpdate;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalView;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * The terminal the server opened for this player: the computer and its console as the updates of the server build
 * it ({@link TerminalView}), drawn by {@link TerminalScreen}, which opens with the first update of a view and closes
 * when the server frees the keyboard.
 */
public final class ClientTerminal {
    private final WorldWideBlocksClientConfig config;
    private BlockPos device;
    private TerminalView view;

    ClientTerminal(WorldWideBlocksClientConfig config) {
        this.config = config;
    }

    void receive(TerminalUpdatesPayload payload) {
        boolean reset = !payload.updates().isEmpty() && payload.updates().getFirst() instanceof TerminalUpdate.Reset;
        if (reset || this.view == null || !payload.pos().equals(this.device)) {
            if (!reset) {
                return; // updates of a screen this client no longer shows
            }
            this.device = payload.pos();
            this.view = new TerminalView();
        }
        payload.updates().forEach(this.view::apply);
        Minecraft minecraft = Minecraft.getInstance();
        if (reset && !(minecraft.gui.screen() instanceof TerminalScreen screen && screen.device().equals(this.device))) {
            minecraft.gui.setScreen(new TerminalScreen(this, this.config, this.device));
        }
    }

    void close(TerminalClosePayload payload) {
        if (payload.pos().equals(this.device)) {
            if (Minecraft.getInstance().gui.screen() instanceof TerminalScreen screen && screen.device().equals(this.device)) {
                screen.closeFromServer();
            }
            this.clear();
        }
    }

    void clear() {
        this.device = null;
        this.view = null;
    }

    /** The computer whose terminal is open, and its console. */
    public Optional<TerminalView> view() {
        return Optional.ofNullable(this.view);
    }

    public Optional<BlockPos> device() {
        return Optional.ofNullable(this.device);
    }
}
