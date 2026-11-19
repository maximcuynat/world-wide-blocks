package io.github.maximcuynat.worldwideblocks.os.process;

import io.github.maximcuynat.worldwideblocks.network.ip.IcmpSocket;

/**
 * A running command. It is never a blocking call: the simulation advances it, through its start, its timers
 * and, from later steps, the frames it receives. It ends by calling {@link ProcessContext#exit}.
 */
public interface Process {

    void start(ProcessContext context);

    /** A timer asked with {@link ProcessContext#schedule} is due. */
    default void onTimer(ProcessContext context, Object key) {
    }

    /** A message reached one of the sockets of the process ({@link ProcessContext#openPingSocket}). */
    default void onReceive(ProcessContext context, IcmpSocket socket, IcmpSocket.Received message) {
    }

    /** Ctrl+C: by default the process ends at once, with the status of a process killed by SIGINT (128 + 2). */
    default void interrupt(ProcessContext context) {
        context.exit(130);
    }
}
