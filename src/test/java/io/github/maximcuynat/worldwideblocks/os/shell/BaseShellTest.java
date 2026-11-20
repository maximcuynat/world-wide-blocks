package io.github.maximcuynat.worldwideblocks.os.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.os.ConsoleLine;
import io.github.maximcuynat.worldwideblocks.os.ConsoleSession;
import io.github.maximcuynat.worldwideblocks.os.MachineContext;
import io.github.maximcuynat.worldwideblocks.os.OperatingSystem;
import io.github.maximcuynat.worldwideblocks.os.command.CommandRegistry;
import io.github.maximcuynat.worldwideblocks.os.process.Process;
import io.github.maximcuynat.worldwideblocks.os.process.ProcessContext;

/**
 * The base shell against the real sessions of {@code src/test/resources/linux-real-outputs.txt}, sections "bash":
 * bash 5.2.37, hostname 3.25 and ncurses 6.5 on Debian 13, account {@code user} made by {@code useradd -m}, host
 * {@code laptop-3f2a}, in a pseudo-terminal (2026-11-20). Each section is replayed line by line: what follows the
 * prompt is typed, the rest must be what the console shows.
 *
 * <p>The sessions check statuses with {@code echo "[exit $?]"}; the mod has no {@code echo}: the test reads the status
 * of the console there and leaves those two lines out. A line ending with {@code ^C} is Ctrl+C typed after its text.
 * The real usages of {@code hostname}, {@code clear} and {@code history} are replaced by the usages written for the mod
 * (named in each test).
 */
class BaseShellTest {
    private static final String HOST = "laptop-3f2a";
    private static final String PROMPT = "user@" + HOST + ":~$ ";
    private static final String ECHO_STATUS = "echo \"[exit $?]\"";
    private static final String SHELL_ENDED = "[shell ended, status ";

    /** {@code false} of coreutils: ends with status 1. */
    private static final class False implements Process {
        @Override
        public void start(ProcessContext context) {
            context.exit(1);
        }
    }

    private OperatingSystem system;
    private ConsoleSession console;

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
        this.system = new OperatingSystem(new Ipv4Stack(), CommandRegistry.standard().register("false", False::new));
        this.system.setHostName(HOST);
        this.console = this.system.localConsole();
    }

    @ParameterizedTest
    @ValueSource(strings = {"hostname", "hostname set", "history", "history count", "exit", "exit after a failure",
            "exit 3", "exit abc", "exit too many arguments", "ctrl-c at the prompt", "ctrl-d on an empty line",
            "help unknown topic", "command not found"})
    void aSessionShowsWhatBashShows(String section) {
        List<String> expected = reference("bash " + section);
        if (section.equals("history count")) {
            expected = replace(expected, "history: usage: history [-c] [-d offset] [n] or history -anrw [filename] or history -ps arg [arg...]",
                    List.of("history: usage: history [-c] [n]"));
        }
        assertEquals(withoutStatusEchoes(expected), this.replay(expected));
    }

    @Test
    void hostnameWithAnUnknownOptionOrTwoNamesGetsItsErrorAndTheUsageOfTheMod() {
        List<String> modUsage = List.of("Usage: hostname                display host name");
        for (String section : List.of("hostname invalid option", "hostname two arguments")) {
            List<String> real = reference("bash " + section);
            int usageStart = indexStartingWith(real, "Usage: hostname");
            int usageEnd = indexStartingWith(real, PROMPT + ECHO_STATUS);
            List<String> expected = new ArrayList<>(real.subList(0, usageStart));
            expected.addAll(modUsage);
            expected.addAll(real.subList(usageEnd, real.size()));
            this.setUp();
            assertEquals(withoutStatusEchoes(expected), this.replay(expected), section);
        }
    }

    @Test
    void clearWithAnUnknownOptionOrAWordGetsItsErrorAndTheUsageWithoutTheUnsupportedOptions() {
        for (String section : List.of("clear invalid option", "clear extra argument")) {
            List<String> real = reference("bash " + section);
            List<String> expected = new ArrayList<>(real);
            expected.remove("  -T TERM     use this instead of $TERM");
            expected.remove("  -V          print curses-version");
            this.setUp();
            assertEquals(withoutStatusEchoes(expected), this.replay(expected), section);
        }
    }

    @Test
    void clearEmptiesTheScreenAndTheScrollback() {
        List<String> real = reference("bash clear");
        assertEquals("<ESC>[H<ESC>[2J<ESC>[3J" + PROMPT + ECHO_STATUS, real.get(3), "what the real clear writes");

        this.console.submit(this.machine, "hostname");
        this.console.submit(this.machine, "clear");

        assertTrue(this.console.entries().isEmpty(), "screen and scrollback cleared");
        assertEquals(0, this.console.lastStatus());
        assertEquals(0, this.console.snapshot().screenStart());
    }

    @Test
    void ctrlLAndClearXClearTheScreenOnlyAndKeepTheTextTyped() {
        List<String> real = reference("bash ctrl-l");
        assertEquals(PROMPT + "hostn<ESC>[H<ESC>[2J" + PROMPT + "hostname", real.get(2),
                "Ctrl+L: screen cleared without ESC[3J, the prompt and the text typed drawn again");

        this.console.submit(this.machine, "hostname");
        this.console.clearScreen();
        assertEquals(2, this.console.snapshot().screenStart(), "the 2 lines above stay reachable by scrolling");
        this.console.submit(this.machine, "clear -x");
        assertEquals(3, this.console.snapshot().screenStart(), "the line of clear -x goes above the screen too");
        assertEquals(3, this.console.entries().size());
    }

    @Test
    void ctrlDOnALineWithTextDoesNothingOnTheServer() {
        List<String> real = reference("bash ctrl-d on a line with text");
        assertTrue(real.getFirst().startsWith(PROMPT + "hostn<BEL>"), "readline rings the bell and keeps the line");
        // The client handles it: Ctrl+D with text is never sent. Nothing else to check here.
    }

    @Test
    void historyIsSharedByTheShellsOfTheMachine() {
        this.console.submit(this.machine, "hostname");
        this.console.submit(this.machine, "exit");
        assertTrue(this.console.ended());

        ConsoleSession next = this.system.openLocalConsole();
        next.submit(this.machine, "history");

        assertEquals(List.of("    1  hostname", "    2  exit", "    3  history"), next.lines());
        assertEquals(this.console.id() + 1, next.id());
        assertEquals(List.of(new ConsoleLine.Input(new Prompt("user", HOST, "~"), "history")), next.entries().subList(0, 1),
                "the new shell starts on a clear screen");
    }

    @Test
    void anEndedShellTakesNothingMore() {
        this.console.submit(this.machine, "exit 4");
        this.console.submit(this.machine, "hostname");
        this.console.interrupt(this.machine, "x");
        this.console.endOfInput();

        assertEquals(List.of("exit"), this.console.lines());
        assertEquals(4, this.console.lastStatus());
        assertEquals(this.console, this.system.localConsole(), "until the screen is opened again");
    }

    @Test
    void helpListsTheCommandsOfTheMachineAsLinesOfTheGame() {
        this.console.submit(this.machine, "help");

        List<String> keys = this.console.entries().stream().skip(1).map(line -> ((ConsoleLine.Game) line).key()).toList();
        assertEquals(List.of("terminal.worldwideblocks.help.intro",
                "terminal.worldwideblocks.help.summary.clear",
                "terminal.worldwideblocks.help.summary.exit",
                "terminal.worldwideblocks.help.summary.help",
                "terminal.worldwideblocks.help.summary.history",
                "terminal.worldwideblocks.help.summary.hostname",
                "terminal.worldwideblocks.help.summary.ip",
                "terminal.worldwideblocks.help.summary.ping",
                "terminal.worldwideblocks.help.keys.1",
                "terminal.worldwideblocks.help.keys.2"), keys);
        assertEquals(List.of("hostname  "), ((ConsoleLine.Game) this.console.entries().get(6)).values());
        assertTrue(this.console.lines().isEmpty(), "no literal output");
        assertEquals(0, this.console.lastStatus());
    }

    @Test
    void helpWithAWordShowsEveryTopicThatStartsWithIt() {
        this.console.submit(this.machine, "help h");

        List<String> keys = this.console.entries().stream().skip(1).map(line -> ((ConsoleLine.Game) line).key()).toList();
        assertEquals(List.of("terminal.worldwideblocks.help.help.1", "terminal.worldwideblocks.help.history.1",
                "terminal.worldwideblocks.help.history.2", "terminal.worldwideblocks.help.hostname.1",
                "terminal.worldwideblocks.help.hostname.2"), keys);
    }

    @Test
    void theHistoryCommandIsKeptBeforeItRunsAndIgnorebothApplies() {
        this.console.submit(this.machine, "hostname");
        this.console.submit(this.machine, "hostname");
        this.console.submit(this.machine, " hostname");
        assertEquals(List.of("    1  hostname"), this.system.history().listing(10));
        this.console.submit(this.machine, "history -c");
        this.console.submit(this.machine, "history");

        assertEquals(List.of(HOST, HOST, HOST, "    1  history"), this.console.lines());
    }

    // ---- Replay ----

    /** Types what follows each prompt of a real session and returns what the console shows, in the same form. */
    private List<String> replay(List<String> session) {
        List<String> shown = new ArrayList<>();
        for (int i = 0; i < session.size(); i++) {
            String line = session.get(i);
            boolean last = i == session.size() - 1;
            if (line.startsWith(SHELL_ENDED)) {
                break;
            }
            if (!line.startsWith(PROMPT) || last) {
                continue;
            }
            String typed = line.substring(PROMPT.length());
            if (typed.equals(ECHO_STATUS)) {
                String status = session.get(++i);
                assertEquals(status, "[exit " + this.console.lastStatus() + "]", "status at line " + i);
                continue;
            }
            if (typed.endsWith("^C")) {
                this.console.interrupt(this.machine, typed.substring(0, typed.length() - 2));
            } else if (typed.isEmpty() && i + 1 < session.size() && session.get(i + 1).equals("exit")) {
                this.console.endOfInput();
            } else {
                this.console.submit(this.machine, typed);
            }
        }
        for (ConsoleLine entry : this.console.entries()) {
            switch (entry) {
                case ConsoleLine.Input input -> shown.add(input.prompt().text() + input.text());
                case ConsoleLine.Output output -> shown.add(output.text());
                case ConsoleLine.Game game -> shown.add("[game " + game.key() + "]");
                case ConsoleLine.Advice advice -> {
                }
            }
        }
        if (this.console.ended()) {
            shown.add(SHELL_ENDED + this.console.lastStatus() + "]");
        } else {
            shown.add(this.console.prompt().text());
        }
        return shown;
    }

    /** The real session without its {@code echo "[exit $?]"} lines and their answers, which the replay leaves out. */
    private static List<String> withoutStatusEchoes(List<String> session) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < session.size(); i++) {
            if (session.get(i).equals(PROMPT + ECHO_STATUS)) {
                i++;
            } else {
                lines.add(session.get(i));
            }
        }
        return lines;
    }

    private static List<String> reference(String section) {
        try (InputStream in = BaseShellTest.class.getResourceAsStream("/linux-real-outputs.txt")) {
            List<String> all = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            int start = all.indexOf("=== " + section) + 1;
            assertTrue(start > 0, "section " + section);
            int end = start;
            while (end < all.size() && !all.get(end).startsWith("=== ")) {
                end++;
            }
            return all.subList(start, end);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> replace(List<String> lines, String old, List<String> replacement) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (line.equals(old)) {
                result.addAll(replacement);
            } else {
                result.add(line);
            }
        }
        return result;
    }

    private static int indexStartingWith(List<String> lines, String start) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(start)) {
                return i;
            }
        }
        throw new AssertionError("no line starting with " + start);
    }
}
