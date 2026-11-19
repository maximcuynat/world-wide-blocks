package io.github.maximcuynat.worldwideblocks.os;

import java.util.ArrayDeque;
import java.util.List;
import java.util.OptionalInt;

import io.github.maximcuynat.worldwideblocks.os.shell.Shell;

/**
 * A console of the machine: a shell, the command running in the foreground, and the lines written so far, kept
 * up to {@link #MAX_LINES}. It lives in server memory while the machine exists; nobody needs to watch it for its
 * commands to run.
 *
 * <p>While a command runs, the console belongs to it: a typed line is not read (no type-ahead in lot 1c-1).
 */
public final class ConsoleSession {
    /** Lines kept, the oldest dropped first. */
    public static final int MAX_LINES = 1000;

    private final OperatingSystem system;
    private final Shell shell;
    private final ArrayDeque<String> lines = new ArrayDeque<>();
    private OptionalInt foreground = OptionalInt.empty();
    private int lastStatus;

    ConsoleSession(OperatingSystem system) {
        this.system = system;
        this.shell = new Shell(system);
    }

    /** A line typed then Enter. */
    public void submit(MachineContext machine, String line) {
        if (this.commandRunning()) {
            return;
        }
        OptionalInt started = this.shell.execute(machine, line, this::write, status -> {
            this.lastStatus = status;
            this.foreground = OptionalInt.empty();
        });
        if (started.isPresent() && this.system.processes().isRunning(started.getAsInt())) {
            this.foreground = started;
        }
    }

    /** Ctrl+C: interrupts the command in the foreground, if there is one. */
    public void interrupt(MachineContext machine) {
        if (this.foreground.isPresent()) {
            this.system.processes().interrupt(machine, this.foreground.getAsInt());
        }
    }

    public boolean commandRunning() {
        return this.foreground.isPresent() && this.system.processes().isRunning(this.foreground.getAsInt());
    }

    /** Exit status of the last command ({@code $?}). */
    public int lastStatus() {
        return this.lastStatus;
    }

    public List<String> lines() {
        return List.copyOf(this.lines);
    }

    private void write(String line) {
        this.lines.addLast(line);
        while (this.lines.size() > MAX_LINES) {
            this.lines.removeFirst();
        }
    }
}
