package io.github.maximcuynat.worldwideblocks.terminal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Runtime state of the terminals of a server: who is at which keyboard, and the rate of the packets of each player.
 * Never saved: a server restart closes every screen, like the consoles it starts again. Plain Java.
 */
public final class TerminalHub {
    private final Keyboards keyboards = new Keyboards();
    private final Map<UUID, TokenBucket> rates = new HashMap<>();

    public Keyboards keyboards() {
        return this.keyboards;
    }

    /** True if a packet of this player may be read at this tick ({@link TerminalRules#BURST}, then one per tick). */
    public boolean allow(UUID player, long tick) {
        return this.rates.computeIfAbsent(player, id -> new TokenBucket(TerminalRules.BURST, tick)).take(tick);
    }

    /** The player left the server. */
    public void forget(UUID player) {
        this.rates.remove(player);
    }
}
