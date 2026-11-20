package io.github.maximcuynat.worldwideblocks.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.maximcuynat.worldwideblocks.os.ConsoleLine;
import io.github.maximcuynat.worldwideblocks.os.ConsoleSession;
import io.github.maximcuynat.worldwideblocks.os.shell.Prompt;

/**
 * What the server sends a terminal screen, in order. A viewer starts from {@link Reset}, then the lines and the
 * history of the snapshot, then {@link ScreenStart}, then every change of the console as it happens
 * ({@link ConsoleSession.Change}). The snapshot is sent as updates, so that no packet holds the whole console.
 */
public sealed interface TerminalUpdate {

    /** A new view: everything before is forgotten. */
    record Reset(int session, Prompt prompt, String current, boolean running) implements TerminalUpdate {
        public Reset {
            Objects.requireNonNull(prompt, "prompt");
            Objects.requireNonNull(current, "current");
        }
    }

    record Line(ConsoleLine line) implements TerminalUpdate {
        public Line {
            Objects.requireNonNull(line, "line");
        }
    }

    record Current(String text) implements TerminalUpdate {
        public Current {
            Objects.requireNonNull(text, "text");
        }
    }

    record Cleared(boolean scrollbackKept) implements TerminalUpdate {
    }

    /** Index of the first line on the screen, after the lines of a snapshot. */
    record ScreenStart(int index) implements TerminalUpdate {
    }

    record Running(boolean running) implements TerminalUpdate {
    }

    record History(String line) implements TerminalUpdate {
        public History {
            Objects.requireNonNull(line, "line");
        }
    }

    record HistoryCleared() implements TerminalUpdate {
    }

    record Ended(int status) implements TerminalUpdate {
    }

    /** The updates that draw this snapshot on a new view. */
    static List<TerminalUpdate> of(ConsoleSession.Snapshot snapshot) {
        List<TerminalUpdate> updates = new ArrayList<>(snapshot.lines().size() + snapshot.history().size() + 3);
        updates.add(new Reset(snapshot.id(), snapshot.prompt(), snapshot.current(), snapshot.running()));
        snapshot.lines().forEach(line -> updates.add(new Line(line)));
        snapshot.history().forEach(line -> updates.add(new History(line)));
        updates.add(new ScreenStart(snapshot.screenStart()));
        if (snapshot.ended()) {
            updates.add(new Ended(0));
        }
        return updates;
    }

    /** The update that carries this change of the console. */
    static TerminalUpdate of(ConsoleSession.Change change) {
        return switch (change) {
            case ConsoleSession.Change.Added added -> new Line(added.line());
            case ConsoleSession.Change.Current current -> new Current(current.text());
            case ConsoleSession.Change.Cleared cleared -> new Cleared(cleared.scrollbackKept());
            case ConsoleSession.Change.Running running -> new Running(running.running());
            case ConsoleSession.Change.HistoryAdded added -> new History(added.line());
            case ConsoleSession.Change.HistoryCleared cleared -> new HistoryCleared();
            case ConsoleSession.Change.Ended ended -> new Ended(ended.status());
        };
    }
}
