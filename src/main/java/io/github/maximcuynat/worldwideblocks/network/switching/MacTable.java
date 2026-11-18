package io.github.maximcuynat.worldwideblocks.network.switching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Predicate;

import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * Learning table of a switch: for each source MAC seen, the port behind which it
 * is. An entry expires if its MAC has not sent since the expiry delay, and disappears
 * as soon as the cable of its port is removed.
 *
 * <p>Time is the game's, in ticks, supplied by the caller.
 *
 * <p>Plain Java, no Minecraft class. Not thread-safe.
 */
public final class MacTable {
    /** 300 s, expiry delay recommended by IEEE 802.1D, in game ticks (20 per second). */
    public static final long DEFAULT_AGING_TICKS = 300L * 20L;

    /** What the table knows about a MAC. */
    public record Entry(MacAddress mac, int port, long lastSeen) {
    }

    private final long agingTicks;
    private final Map<MacAddress, Entry> entries = new LinkedHashMap<>();

    public MacTable(long agingTicks) {
        if (agingTicks <= 0) {
            throw new IllegalArgumentException("Non-positive aging delay: " + agingTicks);
        }
        this.agingTicks = agingTicks;
    }

    /**
     * Remembers that {@code mac} just sent from {@code port}. A group address is never
     * learned: it does not designate a device.
     */
    public void learn(MacAddress mac, int port, long now) {
        if (mac.isMulticast()) {
            return;
        }
        this.entries.put(mac, new Entry(mac, port, now));
    }

    /** Port behind which {@code mac} is, if it is known and has not expired. */
    public OptionalInt lookup(MacAddress mac, long now) {
        Entry entry = this.entries.get(mac);
        return entry == null || this.isExpired(entry, now) ? OptionalInt.empty() : OptionalInt.of(entry.port());
    }

    /** Forgets everything learned behind this port (cable removed). */
    public void flushPort(int port) {
        this.removeIf(entry -> entry.port() == port);
    }

    /** Removes the expired entries. */
    public void purgeExpired(long now) {
        this.removeIf(entry -> this.isExpired(entry, now));
    }

    /** Entries still present, in learning order. They may include expired entries not yet purged. */
    public List<Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(this.entries.values()));
    }

    private boolean isExpired(Entry entry, long now) {
        return now - entry.lastSeen() >= this.agingTicks;
    }

    private void removeIf(Predicate<Entry> condition) {
        this.entries.values().removeIf(condition);
    }
}
