package io.github.maximcuynat.worldwideblocks.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.os.ConsoleSession;
import io.github.maximcuynat.worldwideblocks.os.MachineContext;
import io.github.maximcuynat.worldwideblocks.os.OperatingSystem;
import io.github.maximcuynat.worldwideblocks.os.command.CommandRegistry;

/**
 * The view of a terminal screen, rebuilt from the updates the server sends: a snapshot when the screen opens, then
 * the changes, as {@code TerminalServer} sends them. It must hold what the console holds.
 */
class TerminalViewTest {
    private final List<TerminalUpdate> sent = new ArrayList<>();
    private OperatingSystem system;

    private final MachineContext machine = new MachineContext() {
        @Override
        public long now() {
            return 0;
        }

        @Override
        public void schedule(long delayTicks, Object key) {
        }

        @Override
        public Ipv4Stack.Context network() {
            return new Ipv4Stack.Context() {
                @Override
                public long now() {
                    return 0;
                }

                @Override
                public void send(NetInterface out, EthernetFrame frame) {
                }

                @Override
                public void schedule(long delayTicks, Object key) {
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
        this.system = new OperatingSystem(new Ipv4Stack(), CommandRegistry.standard());
        this.system.setHostName("laptop-3f2a");
    }

    private TerminalView open() {
        ConsoleSession console = this.system.openLocalConsole();
        this.sent.clear();
        this.sent.addAll(TerminalUpdate.of(console.snapshot()));
        this.system.setConsoleListener((session, change) -> this.sent.add(TerminalUpdate.of(change)));
        TerminalView view = new TerminalView();
        return this.catchUp(view);
    }

    private TerminalView catchUp(TerminalView view) {
        this.sent.forEach(view::apply);
        this.sent.clear();
        ConsoleSession.Snapshot expected = this.system.localConsole().snapshot();
        assertEquals(expected.id(), view.session());
        assertEquals(expected.prompt(), view.prompt());
        assertEquals(expected.lines(), view.lines());
        assertEquals(expected.screenStart(), view.screenStart());
        assertEquals(expected.current(), view.current());
        assertEquals(expected.running(), view.running());
        assertEquals(expected.history(), view.history());
        assertEquals(expected.ended(), view.ended());
        return view;
    }

    @Test
    void aScreenOpenedOnAConsoleInUseShowsItAsItIs() {
        ConsoleSession console = this.system.localConsole();
        for (int i = 0; i < ConsoleSession.MAX_LINES; i++) {
            console.submit(this.machine, "hostname");
        }
        console.clearScreen();
        console.submit(this.machine, "help");

        TerminalView view = this.open();

        assertEquals(List.of("hostname", "help"), view.history());
        assertTrue(view.screenStart() > 0);
    }

    @Test
    void aScreenFollowsTheConsoleUntilTheShellEnds() {
        TerminalView view = this.open();
        ConsoleSession console = this.system.localConsole();

        console.submit(this.machine, "hostname");
        console.interrupt(this.machine, "hostn");
        console.submit(this.machine, "clear -x");
        console.submit(this.machine, "history -c");
        this.catchUp(view);
        console.submit(this.machine, "clear");
        console.endOfInput();
        this.catchUp(view);
        assertTrue(view.ended());

        TerminalView next = this.open();
        assertEquals(view.session() + 1, next.session(), "a new shell on a clear screen");
        assertTrue(next.lines().isEmpty());
    }
}
