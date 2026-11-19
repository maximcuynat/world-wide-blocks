package io.github.maximcuynat.worldwideblocks.os.process;

/**
 * A running command. It is never a blocking call: the simulation advances it, through its start, its timers
 * and, from later steps, the frames it receives. It ends by calling {@link ProcessContext#exit}.
 */
public interface Process {

    void start(ProcessContext context);

    /** A timer asked with {@link ProcessContext#schedule} is due. */
    default void onTimer(ProcessContext context, Object key) {
    }

    /** Ctrl+C: by default the process ends at once, with the status of a process killed by SIGINT (128 + 2). */
    default void interrupt(ProcessContext context) {
        context.exit(130);
    }
}
