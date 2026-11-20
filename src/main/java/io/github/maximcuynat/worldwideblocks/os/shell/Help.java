package io.github.maximcuynat.worldwideblocks.os.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.github.maximcuynat.worldwideblocks.os.TerminalOutput;
import io.github.maximcuynat.worldwideblocks.os.command.CommandRegistry;

/**
 * The builtin {@code help}, translated ({@code docs/lot-plan.md}, lot 1c-1): the commands of this machine, their
 * syntax and how they differ from the real tools. Its lines are lines of the game ({@link TerminalOutput#game}), keys
 * of the family {@code terminal.worldwideblocks.help.}; only its errors are the literal English of bash.
 *
 * <p>As in bash, a word given to {@code help} selects every topic that starts with it ({@code help hi} shows
 * {@code history}).
 */
public final class Help {
    /** Prefix of the keys of {@code help}. */
    public static final String KEYS = "terminal.worldwideblocks.help.";
    /** Builtins of the shell, always there. */
    private static final List<String> BUILTINS = List.of("exit", "help", "history");
    /** Topics with their number of detail lines ({@code terminal.worldwideblocks.help.<topic>.<n>}, from 1). */
    public static final Map<String, Integer> TOPICS = new TreeMap<>(Map.of(
            "clear", 2,
            "exit", 2,
            "help", 1,
            "history", 2,
            "hostname", 2,
            "ip", 3,
            "ping", 2));
    /** Lines of the shortcuts of the terminal after the list ({@code terminal.worldwideblocks.help.keys.<n>}). */
    public static final int KEY_LINES = 2;
    /** Width of the column of the command names in the list. */
    private static final int NAME_COLUMN = 10;

    private Help() {
    }

    /**
     * Runs {@code help} with its words.
     *
     * @return its exit status
     */
    static int run(List<String> words, CommandRegistry commands, TerminalOutput out) {
        List<String> topics = topics(commands);
        List<String> patterns = words.subList(1, words.size());
        if (patterns.isEmpty()) {
            out.game(KEYS + "intro");
            for (String topic : topics) {
                out.game(KEYS + "summary." + topic, pad(topic));
            }
            for (int i = 1; i <= KEY_LINES; i++) {
                out.game(KEYS + "keys." + i);
            }
            return 0;
        }
        String first = patterns.getFirst();
        if (first.length() > 1 && first.startsWith("-")) {
            out.line("bash: help: " + first + ": invalid option");
            out.line("help: usage: help [command ...]");
            return Shell.USAGE;
        }
        List<String> shown = new ArrayList<>();
        for (String pattern : patterns) {
            for (String topic : topics) {
                if (topic.startsWith(pattern) && !shown.contains(topic)) {
                    shown.add(topic);
                    for (int i = 1; i <= TOPICS.get(topic); i++) {
                        out.game(KEYS + topic + "." + i);
                    }
                }
            }
        }
        if (shown.isEmpty()) {
            String pattern = patterns.getLast();
            out.line("bash: help: no help topics match `" + pattern + "'.  Try `help help' or `man -k " + pattern
                    + "' or `info " + pattern + "'.");
            return 1;
        }
        return 0;
    }

    /** The topics of this machine: the builtins and the commands of its registry that have a help, sorted. */
    static List<String> topics(CommandRegistry commands) {
        List<String> topics = new ArrayList<>();
        for (String topic : TOPICS.keySet()) {
            if (BUILTINS.contains(topic) || commands.names().contains(topic)) {
                topics.add(topic);
            }
        }
        return topics;
    }

    private static String pad(String name) {
        return name + " ".repeat(Math.max(1, NAME_COLUMN - name.length()));
    }
}
