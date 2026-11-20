package io.github.maximcuynat.worldwideblocks.network;

/**
 * The network latency model ({@code docs/lot-plan.md}, lot 1c-1, "Network latency"): the time a frame takes along
 * its real path, shown by {@code ping}. The engine delivers every frame at once, in the tick it is sent; each
 * delivery carries the time spent since the start of that tick, in nanoseconds. A cable adds its length times
 * {@link #CABLE_NANOS_PER_METRE}; a device adds its processing time when it receives a frame, and what it sends while
 * handling that frame leaves after it. Deterministic: no jitter.
 *
 * <p>Values read on 2026-11-19, sources next to each. Changing one changes every {@code ping} of the game and its
 * tests, nothing else.
 */
public final class Latency {
    /** Nanoseconds in one game tick (50 ms). */
    public static final long TICK_NANOS = 50_000_000L;

    /**
     * Propagation along a copper Ethernet cord: 5 ns per metre (one block). Fluke Networks, "Propagation delay"
     * (DTX CableAnalyzer knowledge base): "Typical propagation delay for category 5e UTP is a bit less than 5 nS per
     * meter (worst case allowed is 5.7 nS/m)". Datasheets of Cat5e and Cat6 cables give a velocity of propagation of
     * 0.64 to 0.72 c, that is 4.6 to 5.2 ns/m.
     */
    public static final long CABLE_NANOS_PER_METRE = 5L;

    /**
     * A frame crossing the switch of a box: 2 µs. Netgear GS108X datasheet (unmanaged gigabit switch, netgear.com),
     * "Latency (Max Connection Speed) ... 1Gbps: &lt;2µs"; the GS105v5 and GS108v4 datasheets give &lt;2.6 µs and
     * &lt;1.6 µs for 64-byte frames. No figure was found for the switch built into a consumer box.
     */
    public static final long SWITCH_NANOS = 2_000L;

    /**
     * A computer receiving a frame: 50 µs for its network card, its kernel and the program that reads it. Calibrated
     * on real round trips: between two Linux network namespaces joined by a veth (no cable, two receptions per round
     * trip), Debian 13 measured 0.04 to 0.2 ms, the first request of each run left out (0.35 to 0.5 ms, slowed down by
     * the first use of the socket and of the route) ({@code src/test/resources/linux-real-outputs.txt}); through a
     * gigabit switch, a measure of 0.144 ms on average for 56 bytes was published (Server Fault, "Network latency
     * 100mbit vs 1gbit", question 276651). The same value for the computer inside a box: no measure of a consumer box
     * was found.
     */
    public static final long COMPUTER_NANOS = 50_000L;

    /**
     * A packet a computer sends to itself, through its loopback: 7 µs. Debian 13 measured {@code time=0.014 ms} for a
     * round trip to 127.0.0.1, two receptions ({@code src/test/resources/linux-real-outputs.txt}, "ping loopback",
     * second request: the first one is slowed down by the first use of the socket).
     */
    public static final long LOOPBACK_NANOS = 7_000L;

    private Latency() {
    }

    /** Time to cross a cable of this length, in blocks. */
    public static long cable(double lengthInBlocks) {
        return Math.round(lengthInBlocks * CABLE_NANOS_PER_METRE);
    }
}
