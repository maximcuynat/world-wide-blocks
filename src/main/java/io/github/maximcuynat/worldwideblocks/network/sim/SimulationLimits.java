package io.github.maximcuynat.worldwideblocks.network.sim;

/**
 * Safeguards of the simulation, so a badly cabled network never brings the server down.
 *
 * @param maxDeliveriesPerTick frames delivered at most per tick; the next ones wait for the following tick
 * @param maxQueued            frames queued at most; beyond that, new ones are lost
 * @param reportIntervalTicks  minimum delay between two reports of the same problem, applied by the
 *                             message module (the simulation itself reports every tick where it sees the problem)
 */
public record SimulationLimits(int maxDeliveriesPerTick, int maxQueued, long reportIntervalTicks) {
    /** Values approved by Max on 2026-11-17: 10,000 frames per tick, 50,000 queued, one report per minute. */
    public static final SimulationLimits DEFAULT = new SimulationLimits(10_000, 50_000, 60L * 20L);

    public SimulationLimits {
        if (maxDeliveriesPerTick <= 0 || maxQueued <= 0 || reportIntervalTicks < 0) {
            throw new IllegalArgumentException("Invalid limits");
        }
    }
}
