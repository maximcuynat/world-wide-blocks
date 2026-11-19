package io.github.maximcuynat.worldwideblocks.os.shell;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntConsumer;

import io.github.maximcuynat.worldwideblocks.os.MachineContext;
import io.github.maximcuynat.worldwideblocks.os.OperatingSystem;
import io.github.maximcuynat.worldwideblocks.os.TerminalOutput;
import io.github.maximcuynat.worldwideblocks.os.process.Process;

/**
 * The command interpreter of a console, with the conventions of bash: a line is split into words, the first
 * names the command. Quotes, variables, pipes and redirections are not supported in lot 1c-1 ({@code docs/lot-plan.md},
 * "Pushed back"): a quote is an ordinary character.
 */
public final class Shell {
    /** Exit status of a command that does not exist (bash, {@code EX_NOTFOUND}). */
    public static final int COMMAND_NOT_FOUND = 127;
    /** Exit status when the process cannot start. */
    public static final int CANNOT_START = 126;

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
    public OptionalInt execute(MachineContext machine, String line, TerminalOutput out, IntConsumer onExit) {
        List<String> words = words(line);
        if (words.isEmpty()) {
            return OptionalInt.empty();
        }
        String name = words.getFirst();
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
}
