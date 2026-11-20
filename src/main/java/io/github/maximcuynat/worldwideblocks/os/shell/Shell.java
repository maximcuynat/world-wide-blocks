package io.github.maximcuynat.worldwideblocks.os.shell;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.IntConsumer;

import io.github.maximcuynat.worldwideblocks.os.MachineContext;
import io.github.maximcuynat.worldwideblocks.os.OperatingSystem;
import io.github.maximcuynat.worldwideblocks.os.TerminalOutput;
import io.github.maximcuynat.worldwideblocks.os.process.Process;

/**
 * The command interpreter of a console, with the conventions of bash: a line is split into words, the first
 * names the command. Quotes, variables, pipes and redirections are not supported in lot 1c-1 ({@code docs/lot-plan.md},
 * "Pushed back"): a quote is an ordinary character.
 *
 * <p>The builtins of bash that act on the shell itself run here: {@code exit}, {@code history}, {@code help}. Their
 * messages and statuses are those of bash 5.2.37 on Debian 13, seen on 2026-11-20
 * ({@code src/test/resources/linux-real-outputs.txt}, sections "bash"); the usage of {@code history} is written for
 * the mod and names only what it supports. The other commands are processes of the registry.
 */
public final class Shell {
    /** Exit status of a command that does not exist (bash, {@code EX_NOTFOUND}). */
    public static final int COMMAND_NOT_FOUND = 127;
    /** Exit status when the process cannot start. */
    public static final int CANNOT_START = 126;
    /** Exit status of a builtin given a bad option ({@code EX_USAGE}). */
    public static final int USAGE = 2;

    /** What the builtins need from the console they run in. */
    public interface Session {
        /** {@code $?}. */
        int lastStatus();

        /** Ends the shell with this status. */
        void exit(int status);

        /** The history of the account was emptied. */
        void historyCleared();
    }

    private final OperatingSystem system;

    public Shell(OperatingSystem system) {
        this.system = system;
    }

    /** Words of a command line, split on spaces and tabs. */
    public static List<String> words(String line) {
        String trimmed = line.strip();
        return trimmed.isEmpty() ? List.of() : Arrays.asList(trimmed.split("[ \t]+"));
    }

    /**
     * Runs a command line.
     *
     * @param onExit told of the exit status of the command, possibly before this method returns
     * @return the number of the process still running in the foreground, or empty when the command already ended
     *         (or the line was empty: then {@code onExit} is not called)
     */
    public OptionalInt execute(MachineContext machine, String line, TerminalOutput out, Session session, IntConsumer onExit) {
        List<String> words = words(line);
        if (words.isEmpty()) {
            return OptionalInt.empty();
        }
        String name = words.getFirst();
        switch (name) {
            case "exit" -> {
                this.exit(words, out, session, onExit);
                return OptionalInt.empty();
            }
            case "history" -> {
                onExit.accept(this.history(words, out, session));
                return OptionalInt.empty();
            }
            case "help" -> {
                onExit.accept(Help.run(words, this.system.commands(), out));
                return OptionalInt.empty();
            }
            default -> {
            }
        }
        Optional<Process> process = this.system.commands().create(name);
        if (process.isEmpty()) {
            // bash, execute_cmd.c: notfound_str = "command not found", after the name of the shell and the command.
            out.line("bash: " + name + ": command not found");
            onExit.accept(COMMAND_NOT_FOUND);
            return OptionalInt.empty();
        }
        OptionalInt pid = this.system.processes().start(machine, process.get(), words, out, onExit);
        if (pid.isEmpty()) {
            // Cap of running processes reached. Wording of bash when fork fails, from memory, to check in
            // jobs.c (not reachable on 2026-11-19); out of reach in lot 1c-1, with one foreground command per machine.
            out.line("bash: fork: Resource temporarily unavailable");
            onExit.accept(CANNOT_START);
            return OptionalInt.empty();
        }
        return this.system.processes().isRunning(pid.getAsInt()) ? pid : OptionalInt.empty();
    }

    /**
     * {@code exit [n]}: an interactive bash writes {@code exit} first, then ends with {@code n} (modulo 256) or the
     * status of the last command. A word that is not a number ends it with status 2 after its message; two words or
     * more leave the shell running, with status 1.
     */
    private void exit(List<String> words, TerminalOutput out, Session session, IntConsumer onExit) {
        out.line("exit");
        if (words.size() > 2) {
            out.line("bash: exit: too many arguments");
            onExit.accept(1);
            return;
        }
        if (words.size() == 1) {
            session.exit(session.lastStatus());
            return;
        }
        String argument = words.get(1);
        OptionalLong status = number(argument);
        if (status.isEmpty()) {
            out.line("bash: exit: " + argument + ": numeric argument required");
            session.exit(USAGE);
        } else {
            session.exit((int) (status.getAsLong() & 0xFF));
        }
    }

    /** {@code history [-c] [n]}. */
    private int history(List<String> words, TerminalOutput out, Session session) {
        ShellHistory history = this.system.history();
        List<String> arguments = words.subList(1, words.size());
        if (!arguments.isEmpty() && arguments.getFirst().length() > 1 && arguments.getFirst().startsWith("-")) {
            String option = arguments.getFirst();
            if (option.equals("-c")) {
                history.clear();
                session.historyCleared();
                return 0;
            }
            out.line("bash: history: " + option + ": invalid option");
            out.line("history: usage: history [-c] [n]");
            return USAGE;
        }
        if (arguments.size() > 1) {
            out.line("bash: history: too many arguments");
            return 1;
        }
        int count = Integer.MAX_VALUE;
        if (arguments.size() == 1) {
            OptionalLong n = number(arguments.getFirst());
            if (n.isEmpty()) {
                out.line("bash: history: " + arguments.getFirst() + ": numeric argument required");
                return 1;
            }
            count = (int) Math.min(Integer.MAX_VALUE, n.getAsLong());
        }
        history.listing(count).forEach(out::line);
        return 0;
    }

    /** A whole number as bash reads it ({@code legal_number}): optional sign, digits only. */
    private static OptionalLong number(String word) {
        if (!word.matches("[+-]?[0-9]+")) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(word.startsWith("+") ? word.substring(1) : word));
        } catch (NumberFormatException e) {
            return OptionalLong.empty(); // beyond a 64-bit number: bash refuses it too
        }
    }
}
