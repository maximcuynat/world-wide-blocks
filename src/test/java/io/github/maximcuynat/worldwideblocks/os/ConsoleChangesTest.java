package io.github.maximcuynat.worldwideblocks.os;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.os.command.CommandRegistry;
import io.github.maximcuynat.worldwideblocks.os.process.Process;
import io.github.maximcuynat.worldwideblocks.os.process.ProcessContext;

/**
 * The changes told by a console: a viewer who takes a snapshot and then applies every change in order holds the
 * console as it is, whatever happens (lines dropped beyond the limit, {@code ^C} completed later, clear, a new shell).
 */
class ConsoleChangesTest {

    /** A command of the test: writes one line per second until Ctrl+C. */
    private static final class Forever implements Process {
        @Override
        public void start(ProcessContext context) {
            context.out().line("start");
            context.schedule(20, 1);
        }

        @Override
        public void onTimer(ProcessContext context, Object key) {
            context.out().line("tick " + key);
            context.schedule(20, (Integer) key + 1);
        }
    }

    /** What a viewer draws, rebuilt from a snapshot and the changes. */
    private static final class Viewer {
        int session;
        List<ConsoleLine> lines = new ArrayList<>();
        int screenStart;
        String current = "";
        boolean running;
        List<String> history = new ArrayList<>();
        boolean ended;

        Viewer(ConsoleSession.Snapshot snapshot) {
            this.session = snapshot.id();
            this.lines.addAll(snapshot.lines());
            this.screenStart = snapshot.screenStart();
            this.current = snapshot.current();
            this.running = snapshot.running();
            this.history.addAll(snapshot.history());
            this.ended = snapshot.ended();
        }

        void apply(ConsoleSession.Change change) {
            switch (change) {
                case ConsoleSession.Change.Added added -> {
                    this.lines.add(added.line());
                    while (this.lines.size() > ConsoleSession.MAX_LINES) {
                        this.lines.removeFirst();
                        this.screenStart = Math.max(0, this.screenStart - 1);
                    }
                }
                case ConsoleSession.Change.Current text -> this.current = text.text();
                case ConsoleSession.Change.Cleared cleared -> {
                    if (cleared.scrollbackKept()) {
                        this.screenStart = this.lines.size();
                    } else {
                        this.lines.clear();
                        this.screenStart = 0;
                    }
                }
                case ConsoleSession.Change.Running state -> this.running = state.running();
                case ConsoleSession.Change.HistoryAdded added -> {
                    this.history.add(added.line());
                    while (this.history.size() > 1000) {
                        this.history.removeFirst();
                    }
                }
                case ConsoleSession.Change.HistoryCleared cleared -> this.history.clear();
                case ConsoleSession.Change.Ended ended -> this.ended = true;
            }
        }

        void assertSame(ConsoleSession.Snapshot snapshot) {
            assertEquals(snapshot.id(), this.session);
            assertEquals(snapshot.lines(), this.lines);
            assertEquals(snapshot.screenStart(), this.screenStart);
            assertEquals(snapshot.current(), this.current);
            assertEquals(snapshot.running(), this.running);
            assertEquals(snapshot.history(), this.history);
            assertEquals(snapshot.ended(), this.ended);
        }
    }

    private record Due(long at, Object key) {
    }

    private final PriorityQueue<Due> timers = new PriorityQueue<>(Comparator.comparingLong(Due::at));
    private final List<ConsoleSession.Change> changes = new ArrayList<>();
    private long now;
    private OperatingSystem system;

    private final MachineContext machine = new MachineContext() {
        @Override
        public long now() {
            return ConsoleChangesTest.this.now;
        }

        @Override
        public void schedule(long delayTicks, Object key) {
            ConsoleChangesTest.this.timers.add(new Due(ConsoleChangesTest.this.now + delayTicks, key));
        }

        @Override
        public Ipv4Stack.Context network() {
            return new Ipv4Stack.Context() {
                @Override
                public long now() {
                    return ConsoleChangesTest.this.now;
                }

                @Override
                public void send(NetInterface out, EthernetFrame frame) {
                }

                @Override
                public void schedule(long delayTicks, Object key) {
                    ConsoleChangesTest.this.machine.schedule(delayTicks, key);
                }
            };
        }

        @Override
        public void commandFailed(String command, RuntimeException error) {
            throw error;
        }
    };

    @BeforeEach
    void setUp() {
        this.system = new OperatingSystem(new Ipv4Stack(), CommandRegistry.standard().register("forever", Forever::new));
        this.system.setHostName("tower-0007");
        this.system.setConsoleListener((session, change) -> this.changes.add(change));
    }

    private void runUntil(long time) {
        while (!this.timers.isEmpty() && this.timers.peek().at() <= time) {
            Due due = this.timers.poll();
            this.now = due.at();
            this.system.onTimer(this.machine, due.key());
        }
        this.now = time;
    }

    /** A viewer who joins now, then follows the changes. */
    private Viewer join() {
        Viewer viewer = new Viewer(this.system.localConsole().snapshot());
        this.changes.clear();
        return viewer;
    }

    private void catchUp(Viewer viewer) {
        this.changes.forEach(viewer::apply);
        this.changes.clear();
        viewer.assertSame(this.system.localConsole().snapshot());
    }

    @Test
    void aViewerFollowsACommandItsCtrlCAndTheEndOfItsLine() {
        ConsoleSession console = this.system.localConsole();
        console.submit(this.machine, "hostname");
        Viewer viewer = this.join();

        console.submit(this.machine, "forever");
        this.runUntil(40);
        catchUp(viewer);
        assertTrue(viewer.running);

        console.interrupt(this.machine);
        catchUp(viewer);
        console.interrupt(this.machine, "hostn");
        catchUp(viewer);
        assertFalse(viewer.running);
        assertEquals("tower-0007", ((ConsoleLine.Output) viewer.lines.get(1)).text());
    }

    @Test
    void aViewerFollowsClearsTheHistoryAndTheLimitOfLines() {
        ConsoleSession console = this.system.localConsole();
        Viewer viewer = this.join();

        for (int i = 0; i < ConsoleSession.MAX_LINES; i++) {
            console.submit(this.machine, "hostname");
        }
        console.clearScreen();
        console.submit(this.machine, "hostname");
        catchUp(viewer);
        console.submit(this.machine, "clear -x");
        console.submit(this.machine, "history -c");
        console.submit(this.machine, "foo");
        catchUp(viewer);
        console.submit(this.machine, "clear");
        catchUp(viewer);
        assertTrue(viewer.lines.isEmpty());
    }

    @Test
    void theEndOfTheShellIsToldAndTheNextOpeningIsANewSession() {
        ConsoleSession console = this.system.localConsole();
        Viewer viewer = this.join();

        console.endOfInput();
        catchUp(viewer);
        assertTrue(viewer.ended);
        assertEquals(0, console.lastStatus());

        ConsoleSession next = this.system.openLocalConsole();
        assertEquals(console.id() + 1, next.id());
        assertTrue(next.snapshot().lines().isEmpty());
        assertEquals(next, this.system.openLocalConsole(), "the same while it runs");
    }
}
