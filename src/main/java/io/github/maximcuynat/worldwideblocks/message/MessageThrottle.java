package io.github.maximcuynat.worldwideblocks.message;

import java.util.HashMap;
import java.util.Map;

/**
 * Repeat limit for events: a same problem, designated by its key, is only passed on once
 * per interval, whatever the number of devices that see it. The time is that of the ticks of the
 * simulation, in game time.
 *
 * <p>The key of a problem tied to a network is forgotten when that network changes ({@link #forget(String)}):
 * the player who thinks they have fixed it learns at the next report, without waiting for the end of
 * the interval. The other networks are not touched.
 */
public final class MessageThrottle {
    private final long intervalTicks;
    private final Map<String, Long> lastSent = new HashMap<>();

    public MessageThrottle(long intervalTicks) {
        if (intervalTicks < 0) {
            throw new IllegalArgumentException("Negative interval: " + intervalTicks);
        }
        this.intervalTicks = intervalTicks;
    }

    /** True if the problem can be passed on now; if so, the instant is remembered. */
    public boolean allow(String problemKey, long now) {
        Long last = this.lastSent.get(problemKey);
        if (last != null && now - last < this.intervalTicks) {
            return false;
        }
        this.lastSent.put(problemKey, now);
        return true;
    }

    /** Forgets this problem: it can be passed on again right away. */
    public void forget(String problemKey) {
        this.lastSent.remove(problemKey);
    }
}
