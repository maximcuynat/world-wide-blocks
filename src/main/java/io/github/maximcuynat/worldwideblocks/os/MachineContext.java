package io.github.maximcuynat.worldwideblocks.os;

import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;

/**
 * What the operating system needs from the machine it runs on, during one call of the simulation. Valid only
 * during that call.
 */
public interface MachineContext {
    /** Game time, in ticks. */
    long now();

    /** Calls the operating system back with this key in {@code delayTicks} ticks ({@link OperatingSystem#onTimer}). */
    void schedule(long delayTicks, Object key);

    /** The network side of the machine, for the processes that send frames. */
    Ipv4Stack.Context network();

    /** A command threw an exception: its process is ended, the error goes to the log. */
    void commandFailed(String command, RuntimeException error);
}
