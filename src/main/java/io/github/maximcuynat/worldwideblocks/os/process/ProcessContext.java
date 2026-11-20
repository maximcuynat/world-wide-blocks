package io.github.maximcuynat.worldwideblocks.os.process;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.ip.IcmpSocket;

import io.github.maximcuynat.worldwideblocks.os.MachineContext;
import io.github.maximcuynat.worldwideblocks.os.OperatingSystem;
import io.github.maximcuynat.worldwideblocks.os.TerminalOutput;

/** What a process can do during one of its calls. Valid only during that call. */
public interface ProcessContext {

    /** The words of the command line, the name of the command first ({@code argv}). */
    List<String> arguments();

    TerminalOutput out();

    OperatingSystem system();

    MachineContext machine();

    /** Game time, in ticks. */
    default long now() {
        return this.machine().now();
    }

    /** Time spent since the start of the tick in this call, in nanoseconds ({@code Latency}). */
    default long latency() {
        return this.machine().network().latency();
    }

    /**
     * Opens a ping socket of the machine for this process ({@link Process#onReceive}); it is closed when the process
     * ends.
     */
    IcmpSocket openPingSocket();

    /** Calls {@link Process#onTimer} with this key in {@code delayTicks} ticks, if the process still runs. */
    void schedule(long delayTicks, Object key);

    /** Ends the process with this exit status. Nothing reaches it afterwards. */
    void exit(int status);
}
