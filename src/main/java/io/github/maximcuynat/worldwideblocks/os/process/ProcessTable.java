package io.github.maximcuynat.worldwideblocks.os.process;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.IntConsumer;

import io.github.maximcuynat.worldwideblocks.os.MachineContext;
import io.github.maximcuynat.worldwideblocks.os.OperatingSystem;
import io.github.maximcuynat.worldwideblocks.os.TerminalOutput;

/**
 * The processes running on a machine. Each call into a process is guarded: an exception ends that process
 * (exit status 1) and is reported to the machine ({@link MachineContext#commandFailed}), never thrown into the
 * simulation tick.
 *
 * <p>A cap limits the running processes of a machine ({@link #MAX_PROCESSES}). Process numbers are never reused,
 * so a timer of an ended process cannot reach another one.
 */
public final class ProcessTable {
    /** Processes running at once on one machine, at most. */
    public static final int MAX_PROCESSES = 8;

    /** Timer of a process, carried by the machine's scheduler. */
    public record Timer(int pid, Object key) {
    }

    private final OperatingSystem system;
    private final Map<Integer, Entry> running = new LinkedHashMap<>();
    private int nextPid = 1;

    public ProcessTable(OperatingSystem system) {
        this.system = system;
    }

    /**
     * Starts a process.
     *
     * @param onExit told of the exit status when the process ends
     * @return its number, or empty if the cap is reached (the process does not start)
     */
    public OptionalInt start(MachineContext machine, Process process, List<String> arguments, TerminalOutput out,
            IntConsumer onExit) {
        if (this.running.size() >= MAX_PROCESSES) {
            return OptionalInt.empty();
        }
        int pid = this.nextPid++;
        Entry entry = new Entry(pid, Objects.requireNonNull(process, "process"), List.copyOf(arguments), out, onExit);
        this.running.put(pid, entry);
        entry.call(machine, () -> process.start(entry.context(machine)));
        return OptionalInt.of(pid);
    }

    public void onTimer(MachineContext machine, Timer timer) {
        Entry entry = this.running.get(timer.pid());
        if (entry != null) {
            entry.call(machine, () -> entry.process.onTimer(entry.context(machine), timer.key()));
        }
    }

    /** Ctrl+C to this process. */
    public void interrupt(MachineContext machine, int pid) {
        Entry entry = this.running.get(pid);
        if (entry != null) {
            entry.call(machine, () -> entry.process.interrupt(entry.context(machine)));
        }
    }

    public boolean isRunning(int pid) {
        return this.running.containsKey(pid);
    }

    public int count() {
        return this.running.size();
    }

    private final class Entry {
        private final int pid;
        private final Process process;
        private final List<String> arguments;
        private final TerminalOutput out;
        private final IntConsumer onExit;

        private Entry(int pid, Process process, List<String> arguments, TerminalOutput out, IntConsumer onExit) {
            this.pid = pid;
            this.process = process;
            this.arguments = arguments;
            this.out = out;
            this.onExit = onExit;
        }

        private void call(MachineContext machine, Runnable call) {
            try {
                call.run();
            } catch (RuntimeException error) {
                machine.commandFailed(this.arguments.getFirst(), error);
                this.exit(1);
            }
        }

        private void exit(int status) {
            if (ProcessTable.this.running.remove(this.pid) != null) {
                this.onExit.accept(status);
            }
        }

        private ProcessContext context(MachineContext machine) {
            Entry entry = this;
            return new ProcessContext() {
                @Override
                public List<String> arguments() {
                    return entry.arguments;
                }

                @Override
                public TerminalOutput out() {
                    return entry.out;
                }

                @Override
                public OperatingSystem system() {
                    return ProcessTable.this.system;
                }

                @Override
                public MachineContext machine() {
                    return machine;
                }

                @Override
                public void schedule(long delayTicks, Object key) {
                    machine.schedule(delayTicks, new Timer(entry.pid, key));
                }

                @Override
                public void exit(int status) {
                    entry.exit(status);
                }
            };
        }
    }
}
