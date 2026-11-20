package io.github.maximcuynat.worldwideblocks.terminal;

/** A rate limit counted in ticks: {@code capacity} packets at once, then one more each tick. */
public final class TokenBucket {
    private final int capacity;
    private double tokens;
    private long lastTick;

    public TokenBucket(int capacity, long now) {
        this.capacity = capacity;
        this.tokens = capacity;
        this.lastTick = now;
    }

    /** True if a packet may pass at this tick; it then takes one token. */
    public boolean take(long now) {
        if (now > this.lastTick) {
            this.tokens = Math.min(this.capacity, this.tokens + (now - this.lastTick));
            this.lastTick = now;
        }
        if (this.tokens < 1) {
            return false;
        }
        this.tokens--;
        return true;
    }
}
