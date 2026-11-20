package io.github.maximcuynat.worldwideblocks.terminal;

import java.util.ArrayList;
import java.util.List;

import io.github.maximcuynat.worldwideblocks.os.ConsoleLine;
import io.github.maximcuynat.worldwideblocks.os.ConsoleSession;
import io.github.maximcuynat.worldwideblocks.os.shell.Prompt;
import io.github.maximcuynat.worldwideblocks.os.shell.ShellHistory;

/**
 * The console as a terminal screen holds it, rebuilt from the updates of the server: the same lines, screen start,
 * current line, prompt, history and state as the console of the machine. Plain Java: the screen only draws it.
 */
public final class TerminalView {
    private int session = -1;
    private Prompt prompt = new Prompt(Prompt.USER, "localhost", Prompt.HOME);
    private final List<ConsoleLine> lines = new ArrayList<>();
    private int screenStart;
    private String current = "";
    private boolean running;
    private final List<String> history = new ArrayList<>();
    private boolean ended;

    public void apply(TerminalUpdate update) {
        switch (update) {
            case TerminalUpdate.Reset reset -> {
                this.session = reset.session();
                this.prompt = reset.prompt();
                this.lines.clear();
                this.screenStart = 0;
                this.current = reset.current();
                this.running = reset.running();
                this.history.clear();
                this.ended = false;
            }
            case TerminalUpdate.Line line -> {
                this.lines.add(line.line());
                while (this.lines.size() > ConsoleSession.MAX_LINES) {
                    this.lines.removeFirst();
                    this.screenStart = Math.max(0, this.screenStart - 1);
                }
            }
            case TerminalUpdate.Current text -> this.current = text.text();
            case TerminalUpdate.Cleared cleared -> {
                if (cleared.scrollbackKept()) {
                    this.screenStart = this.lines.size();
                } else {
                    this.lines.clear();
                    this.screenStart = 0;
                }
            }
            case TerminalUpdate.ScreenStart start -> this.screenStart = Math.min(start.index(), this.lines.size());
            case TerminalUpdate.Running state -> this.running = state.running();
            case TerminalUpdate.History added -> {
                this.history.add(added.line());
                while (this.history.size() > ShellHistory.MAX_ENTRIES) {
                    this.history.removeFirst();
                }
            }
            case TerminalUpdate.HistoryCleared cleared -> this.history.clear();
            case TerminalUpdate.Ended ended -> this.ended = true;
        }
    }

    public int session() {
        return this.session;
    }

    public Prompt prompt() {
        return this.prompt;
    }

    public List<ConsoleLine> lines() {
        return List.copyOf(this.lines);
    }

    public int screenStart() {
        return this.screenStart;
    }

    public String current() {
        return this.current;
    }

    public boolean running() {
        return this.running;
    }

    public List<String> history() {
        return List.copyOf(this.history);
    }

    public boolean ended() {
        return this.ended;
    }
}
