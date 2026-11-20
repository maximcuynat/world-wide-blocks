package io.github.maximcuynat.worldwideblocks.os;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import io.github.maximcuynat.worldwideblocks.os.shell.Prompt;
import io.github.maximcuynat.worldwideblocks.os.shell.Shell;

/**
 * A console of the machine: a shell, the command running in the foreground, and the lines written so far, kept
 * up to {@link #MAX_LINES}. It lives in server memory while the machine exists; nobody needs to watch it for its
 * commands to run. It ends with its shell ({@code exit}, Ctrl+D on an empty line): the machine then opens a new one,
 * on a clear screen ({@link OperatingSystem#openLocalConsole}).
 *
 * <p>While a command runs, the console belongs to it: a typed line is not read (no type-ahead in lot 1c-1).
 *
 * <p>Ctrl+C is echoed {@code ^C} at the end of the current line, as the terminal driver of Linux does
 * ({@code ECHOCTL}), then the command is interrupted. A command that handles it ends the line itself ({@code ping}
 * starts its statistics with a line break); after a command killed by it (exit status 130), the shell ends the
 * line. At the prompt, bash ends the line after {@code ^C} and sets the status 130. Seen on Debian 13 in a
 * pseudo-terminal ({@code src/test/resources/linux-real-outputs.txt}, "ping interrupted in a terminal" and the
 * sections "bash").
 *
 * <p>Advice of the game ({@link ConsoleLine.Advice}) is kept apart from the output and added after the command, only
 * if it failed.
 *
 * <p>Every change is told to the listener of the machine ({@link OperatingSystem#setConsoleListener}), in order: a
 * viewer that got a {@link #snapshot} and then every change sees what the console holds.
 */
public final class ConsoleSession {
    /** Lines kept, the oldest dropped first. */
    public static final int MAX_LINES = 1000;
    /** Exit status of a command killed by Ctrl+C (SIGINT): 128 + 2. */
    public static final int KILLED_BY_INTERRUPT = 130;

    /** A change of the console, told in the order it happens. */
    public sealed interface Change {
        /** A line was ended and added at the bottom. */
        record Added(ConsoleLine line) implements Change {
        }

        /** The line not ended yet now holds this text ({@code ^C} echoed). */
        record Current(String text) implements Change {
        }

        /** The screen was cleared; the lines above stay reachable by scrolling only if {@code scrollbackKept}. */
        record Cleared(boolean scrollbackKept) implements Change {
        }

        /** A command started or ended in the foreground: the prompt is shown only when none runs. */
        record Running(boolean running) implements Change {
        }

        /** A line was kept in the history of the account (Up arrow). */
        record HistoryAdded(String line) implements Change {
        }

        /** The history was emptied ({@code history -c}). */
        record HistoryCleared() implements Change {
        }

        /** The shell ended with this status: the console takes nothing more. */
        record Ended(int status) implements Change {
        }
    }

    /** Receives the changes of the consoles of a machine. */
    @FunctionalInterface
    public interface Listener {
        void changed(ConsoleSession session, Change change);
    }

    /**
     * What a viewer needs to draw the console from scratch.
     *
     * @param id          number of the session on its machine, new after each {@code exit}
     * @param lines       the lines ended, oldest first
     * @param screenStart index of the first line on the screen: the lines before it were cleared by Ctrl+L or
     *                    {@code clear -x} and are only reachable by scrolling
     * @param current     the line not ended yet
     * @param history     the history of the account, oldest first
     */
    public record Snapshot(int id, Prompt prompt, List<ConsoleLine> lines, int screenStart, String current,
            boolean running, List<String> history, boolean ended) {
        public Snapshot {
            lines = List.copyOf(lines);
            history = List.copyOf(history);
        }
    }

    private final OperatingSystem system;
    private final int id;
    private final Shell shell;
    private final ArrayDeque<ConsoleLine> lines = new ArrayDeque<>();
    /** Index in {@link #lines} of the first line on the screen. */
    private int screenStart;
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

        @Override
        public void game(String key, String... values) {
            ConsoleSession.this.add(new ConsoleLine.Game(key, List.of(values)));
        }

        @Override
        public void clear(boolean keepScrollback) {
            ConsoleSession.this.clearScreen(keepScrollback);
        }
    };
    private final Shell.Session shellSession = new Shell.Session() {
        @Override
        public int lastStatus() {
            return ConsoleSession.this.lastStatus;
        }

        @Override
        public void exit(int status) {
            ConsoleSession.this.end(status);
        }

        @Override
        public void historyCleared() {
            ConsoleSession.this.tell(new Change.HistoryCleared());
        }
    };
    private OptionalInt foreground = OptionalInt.empty();
    private ConsoleLine.Advice advice;
    private boolean interrupted;
    private int lastStatus;
    private boolean ended;

    ConsoleSession(OperatingSystem system, int id) {
        this.system = system;
        this.id = id;
        this.shell = new Shell(system);
    }

    /** Number of the session on its machine, new after each {@code exit}. */
    public int id() {
        return this.id;
    }

    /** The prompt of the shell now. */
    public Prompt prompt() {
        return new Prompt(Prompt.USER, this.system.hostName(), Prompt.HOME);
    }

    /** A line typed then Enter: echoed after the prompt, kept in the history, then run. */
    public void submit(MachineContext machine, String line) {
        if (this.ended || this.commandRunning()) {
            return;
        }
        this.add(new ConsoleLine.Input(this.prompt(), line));
        if (this.system.history().add(line)) {
            this.tell(new Change.HistoryAdded(line));
        }
        this.advice = null;
        this.interrupted = false;
        OptionalInt started = this.shell.execute(machine, line, this.output, this.shellSession, this::ended);
        if (!this.ended && started.isPresent() && this.system.processes().isRunning(started.getAsInt())) {
            this.foreground = started;
            this.tell(new Change.Running(true));
        }
    }

    /** Ctrl+C with no text typed: see {@link #interrupt(MachineContext, String)}. */
    public void interrupt(MachineContext machine) {
        this.interrupt(machine, "");
    }

    /**
     * Ctrl+C. While a command runs: echoed, then the command is interrupted. At the prompt: the text typed so far is
     * left on its line followed by {@code ^C}, and the status becomes 130, as bash does.
     *
     * @param typed the text typed at the prompt and not sent, ignored while a command runs
     */
    public void interrupt(MachineContext machine, String typed) {
        if (this.ended) {
            return;
        }
        if (this.commandRunning()) {
            this.current.append("^C");
            this.tell(new Change.Current(this.current.toString()));
            this.interrupted = true;
            this.system.processes().interrupt(machine, this.foreground.getAsInt());
        } else {
            this.add(new ConsoleLine.Input(this.prompt(), typed + "^C"));
            this.lastStatus = KILLED_BY_INTERRUPT;
        }
    }

    /**
     * Ctrl+D on an empty line at the prompt: bash writes {@code exit} on the next line and ends with the status of the
     * last command. Ignored while a command runs (no command of lot 1c-1 reads its input). Ctrl+D on a line with text
     * only rings the bell: the client handles it.
     */
    public void endOfInput() {
        if (this.ended || this.commandRunning()) {
            return;
        }
        this.add(new ConsoleLine.Input(this.prompt(), ""));
        this.write("exit");
        this.end(this.lastStatus);
    }

    /** Ctrl+L at the prompt: the screen is cleared, what was above stays reachable by scrolling. */
    public void clearScreen() {
        if (!this.ended && !this.commandRunning()) {
            this.clearScreen(true);
        }
    }

    public boolean commandRunning() {
        return this.foreground.isPresent() && this.system.processes().isRunning(this.foreground.getAsInt());
    }

    /** True once the shell has ended ({@code exit}, Ctrl+D): the console takes nothing more. */
    public boolean ended() {
        return this.ended;
    }

    /** Exit status of the last command ({@code $?}), or of the shell once it has ended. */
    public int lastStatus() {
        return this.lastStatus;
    }

    /** The output written so far, line by line, the current line last if it holds something; without the prompts and the lines of the game. */
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

    /** Every line, of every kind, the current line last if it holds something. */
    public List<ConsoleLine> entries() {
        List<ConsoleLine> entries = new ArrayList<>(this.lines);
        if (!this.current.isEmpty()) {
            entries.add(new ConsoleLine.Output(this.current.toString()));
        }
        return entries;
    }

    /** The whole console, for a viewer who opens it. */
    public Snapshot snapshot() {
        return new Snapshot(this.id, this.prompt(), List.copyOf(this.lines), this.screenStart, this.current.toString(),
                this.commandRunning(), this.system.history().entries(), this.ended);
    }

    private void ended(int status) {
        this.lastStatus = status;
        boolean wasRunning = this.foreground.isPresent();
        this.foreground = OptionalInt.empty();
        if (this.interrupted && status == KILLED_BY_INTERRUPT && !this.current.isEmpty()) {
            this.write(""); // bash ends the line of the ^C after a command killed by SIGINT
        }
        this.interrupted = false;
        if (this.advice != null && status != 0) {
            this.add(this.advice);
        }
        this.advice = null;
        if (wasRunning) {
            this.tell(new Change.Running(false));
        }
    }

    private void end(int status) {
        if (this.ended) {
            return;
        }
        this.lastStatus = status;
        this.ended = true;
        this.tell(new Change.Ended(status));
    }

    private void write(String line) {
        boolean hadCurrent = !this.current.isEmpty();
        ConsoleLine.Output output = new ConsoleLine.Output(this.current + line);
        this.current.setLength(0);
        if (hadCurrent) {
            this.tell(new Change.Current(""));
        }
        this.add(output);
    }

    private void add(ConsoleLine line) {
        this.lines.addLast(line);
        while (this.lines.size() > MAX_LINES) {
            this.lines.removeFirst();
            this.screenStart = Math.max(0, this.screenStart - 1);
        }
        this.tell(new Change.Added(line));
    }

    private void clearScreen(boolean keepScrollback) {
        if (keepScrollback) {
            this.screenStart = this.lines.size();
        } else {
            this.lines.clear();
            this.screenStart = 0;
        }
        this.tell(new Change.Cleared(keepScrollback));
    }

    private void tell(Change change) {
        this.system.consoleChanged(this, change);
    }
}
