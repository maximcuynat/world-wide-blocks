package io.github.maximcuynat.worldwideblocks.os;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import io.github.maximcuynat.worldwideblocks.os.shell.Shell;

/**
 * A console of the machine: a shell, the command running in the foreground, and the lines written so far, kept
 * up to {@link #MAX_LINES}. It lives in server memory while the machine exists; nobody needs to watch it for its
 * commands to run.
 *
 * <p>While a command runs, the console belongs to it: a typed line is not read (no type-ahead in lot 1c-1).
 *
 * <p>Ctrl+C is echoed {@code ^C} at the end of the current line, as the terminal driver of Linux does
 * ({@code ECHOCTL}), then the command is interrupted. A command that handles it ends the line itself ({@code ping}
 * starts its statistics with a line break); after a command killed by it (exit status 130), the shell ends the
 * line. Both seen on Debian 13 in a pseudo-terminal ({@code src/test/resources/linux-real-outputs.txt}, "ping
 * interrupted in a terminal").
 *
 * <p>Advice of the game ({@link ConsoleLine.Advice}) is kept apart from the output and added after the command, only
 * if it failed.
 */
public final class ConsoleSession {
    /** Lines kept, the oldest dropped first. */
    public static final int MAX_LINES = 1000;
    /** Exit status of a command killed by Ctrl+C (SIGINT): 128 + 2. */
    public static final int KILLED_BY_INTERRUPT = 130;

    private final OperatingSystem system;
    private final Shell shell;
    private final ArrayDeque<ConsoleLine> lines = new ArrayDeque<>();
    /** Text written on the current line, not ended yet ({@code ^C}). */
    private final StringBuilder current = new StringBuilder();
    private final TerminalOutput output = new TerminalOutput() {
        @Override
        public void line(String text) {
            ConsoleSession.this.write(text);
        }

        @Override
        public void advise(String key, String... values) {
            ConsoleSession.this.advice = new ConsoleLine.Advice(key, List.of(values));
        }
    };
    private OptionalInt foreground = OptionalInt.empty();
    private ConsoleLine.Advice advice;
    private boolean interrupted;
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
        this.advice = null;
        this.interrupted = false;
        OptionalInt started = this.shell.execute(machine, line, this.output, this::ended);
        if (started.isPresent() && this.system.processes().isRunning(started.getAsInt())) {
            this.foreground = started;
        }
    }

    /** Ctrl+C: echoed, then interrupts the command in the foreground, if there is one. */
    public void interrupt(MachineContext machine) {
        if (this.commandRunning()) {
            this.current.append("^C");
            this.interrupted = true;
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

    /** The output written so far, line by line, the current line last if it holds something; without the advice. */
    public List<String> lines() {
        List<String> texts = new ArrayList<>(this.lines.size() + 1);
        for (ConsoleLine line : this.lines) {
            if (line instanceof ConsoleLine.Output output) {
                texts.add(output.text());
            }
        }
        if (!this.current.isEmpty()) {
            texts.add(this.current.toString());
        }
        return texts;
    }

    /** Every line, output and advice, the current line last if it holds something. */
    public List<ConsoleLine> entries() {
        List<ConsoleLine> entries = new ArrayList<>(this.lines);
        if (!this.current.isEmpty()) {
            entries.add(new ConsoleLine.Output(this.current.toString()));
        }
        return entries;
    }

    private void ended(int status) {
        this.lastStatus = status;
        this.foreground = OptionalInt.empty();
        if (this.interrupted && status == KILLED_BY_INTERRUPT && !this.current.isEmpty()) {
            this.write(""); // bash ends the line of the ^C after a command killed by SIGINT
        }
        this.interrupted = false;
        if (this.advice != null && status != 0) {
            this.add(this.advice);
        }
        this.advice = null;
    }

    private void write(String line) {
        this.add(new ConsoleLine.Output(this.current + line));
        this.current.setLength(0);
    }

    private void add(ConsoleLine line) {
        this.lines.addLast(line);
        while (this.lines.size() > MAX_LINES) {
            this.lines.removeFirst();
        }
    }
}
