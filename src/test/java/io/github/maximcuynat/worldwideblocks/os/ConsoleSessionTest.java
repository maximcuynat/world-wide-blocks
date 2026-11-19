package io.github.maximcuynat.worldwideblocks.os;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
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
import io.github.maximcuynat.worldwideblocks.os.process.ProcessTable;
import io.github.maximcuynat.worldwideblocks.os.shell.Shell;

/** The shell, the processes and the console, with a clock and timers driven by the test. */
class ConsoleSessionTest {

    /** A command of the test: writes one line per second, {@code n} times, then ends with status 0. */
    private static final class Count implements Process {
        @Override
        public void start(ProcessContext context) {
            context.out().line("start");
            context.schedule(20, 1);
        }

        @Override
        public void onTimer(ProcessContext context, Object key) {
            int n = (Integer) key;
            context.out().line("tick " + n);
            if (n == Integer.parseInt(context.arguments().get(1))) {
                context.exit(0);
            } else {
                context.schedule(20, n + 1);
            }
        }
    }

    /** A command of the test that fails at once. */
    private static final class Broken implements Process {
        @Override
        public void start(ProcessContext context) {
            throw new IllegalStateException("broken on purpose");
        }
    }

    private record Due(long at, Object key) {
    }

    private final PriorityQueue<Due> timers = new PriorityQueue<>(Comparator.comparingLong(Due::at));
    private final List<String> failures = new ArrayList<>();
    private long now;
    private OperatingSystem system;
    private ConsoleSession console;

    private final MachineContext machine = new MachineContext() {
        @Override
        public long now() {
            return ConsoleSessionTest.this.now;
        }

        @Override
        public void schedule(long delayTicks, Object key) {
            ConsoleSessionTest.this.timers.add(new Due(ConsoleSessionTest.this.now + delayTicks, key));
        }

        @Override
        public Ipv4Stack.Context network() {
            return new Ipv4Stack.Context() {
                @Override
                public long now() {
                    return ConsoleSessionTest.this.now;
                }

                @Override
                public void send(NetInterface out, EthernetFrame frame) {
                }

                @Override
                public void schedule(long delayTicks, Object key) {
                    ConsoleSessionTest.this.machine.schedule(delayTicks, key);
                }
            };
        }

        @Override
        public void commandFailed(String command, RuntimeException error) {
            ConsoleSessionTest.this.failures.add(command + ": " + error.getMessage());
        }
    };

    @BeforeEach
    void setUp() {
        CommandRegistry commands = CommandRegistry.standard().register("count", Count::new).register("broken", Broken::new);
        this.system = new OperatingSystem(new Ipv4Stack(), commands);
        this.console = this.system.localConsole();
    }

    private void runUntil(long time) {
        while (!this.timers.isEmpty() && this.timers.peek().at() <= time) {
            Due due = this.timers.poll();
            this.now = due.at();
            this.system.onTimer(this.machine, due.key());
        }
        this.now = time;
    }

    @Test
    void anUnknownCommandGetsTheMessageOfBash() {
        this.console.submit(this.machine, "foo bar");

        assertEquals(List.of("bash: foo: command not found"), this.console.lines());
        assertEquals(Shell.COMMAND_NOT_FOUND, this.console.lastStatus());
        assertFalse(this.console.commandRunning());
    }

    @Test
    void anEmptyLineRunsNothing() {
        this.console.submit(this.machine, "   \t ");

        assertTrue(this.console.lines().isEmpty());
    }

    @Test
    void wordsAreSplitOnSpacesAndTabs() {
        assertEquals(List.of("ip", "a", "show", "eth0"), Shell.words("  ip\ta  show   eth0 "));
    }

    @Test
    void aCommandRunsOverSeveralTicksAndHoldsTheConsole() {
        this.console.submit(this.machine, "count 3");
        assertTrue(this.console.commandRunning());

        this.console.submit(this.machine, "ip a");
        this.runUntil(59);
        assertEquals(List.of("start", "tick 1", "tick 2"), this.console.lines(), "the line typed while it runs is not read");

        this.runUntil(60);
        assertEquals(List.of("start", "tick 1", "tick 2", "tick 3"), this.console.lines());
        assertFalse(this.console.commandRunning());
        assertEquals(0, this.console.lastStatus());
        assertEquals(0, this.system.processes().count());
    }

    @Test
    void ctrlCEndsTheCommandAndItsTimers() {
        this.console.submit(this.machine, "count 5");
        this.runUntil(20);

        this.console.interrupt(this.machine);
        this.runUntil(200);

        assertEquals(List.of("start", "tick 1"), this.console.lines());
        assertEquals(130, this.console.lastStatus());
        assertFalse(this.console.commandRunning());
    }

    @Test
    void aCommandThatThrowsEndsWithoutStoppingAnything() {
        this.console.submit(this.machine, "broken");

        assertEquals(List.of("broken: broken on purpose"), this.failures);
        assertEquals(1, this.console.lastStatus());
        assertFalse(this.console.commandRunning());
        this.console.submit(this.machine, "foo");
        assertEquals(List.of("bash: foo: command not found"), this.console.lines(), "the console still answers");
    }

    @Test
    void theNumberOfProcessesOfAMachineIsCapped() {
        ProcessTable processes = this.system.processes();
        for (int i = 0; i < ProcessTable.MAX_PROCESSES; i++) {
            assertTrue(processes.start(this.machine, new Count(), List.of("count", "9"), line -> {
            }, status -> {
            }).isPresent());
        }

        OptionalInt refused = processes.start(this.machine, new Count(), List.of("count", "9"), line -> {
        }, status -> {
        });

        assertTrue(refused.isEmpty());
        assertEquals(ProcessTable.MAX_PROCESSES, processes.count());
    }

    @Test
    void theConsoleKeepsItsLastLinesOnly() {
        for (int i = 0; i < ConsoleSession.MAX_LINES + 5; i++) {
            this.console.submit(this.machine, "foo" + i);
        }

        assertEquals(ConsoleSession.MAX_LINES, this.console.lines().size());
        assertEquals("bash: foo5: command not found", this.console.lines().getFirst());
    }

    @Test
    void ipRunsOnTheStackOfTheMachine() {
        this.console.submit(this.machine, "ip link show lo");

        assertEquals(List.of("1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1000",
                "    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00"), this.console.lines());
        assertEquals(0, this.console.lastStatus());
    }
}
