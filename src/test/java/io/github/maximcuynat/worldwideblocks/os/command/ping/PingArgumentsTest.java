package io.github.maximcuynat.worldwideblocks.os.command.ping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.ip.FakeMachine;
import io.github.maximcuynat.worldwideblocks.network.ip.InterfaceAddress;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Prefix;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.ip.Route;
import io.github.maximcuynat.worldwideblocks.network.ip.RouteProtocol;
import io.github.maximcuynat.worldwideblocks.network.ip.Scope;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.os.ConsoleSession;
import io.github.maximcuynat.worldwideblocks.os.MachineContext;
import io.github.maximcuynat.worldwideblocks.os.OperatingSystem;
import io.github.maximcuynat.worldwideblocks.os.command.CommandRegistry;

/**
 * The arguments of {@code ping} and the errors before the first request, against the real outputs of Debian 13
 * ({@code src/test/resources/linux-real-outputs.txt}). The usage that follows an invalid option is written for the
 * mod: only its first line, the error of getopt, is compared.
 */
class PingArgumentsTest {
    private Ipv4Stack stack;
    private FakeMachine network;
    private final List<Object> scheduled = new ArrayList<>();
    private ConsoleSession console;

    private final MachineContext machine = new MachineContext() {
        @Override
        public long now() {
            return PingArgumentsTest.this.network.now();
        }

        @Override
        public void schedule(long delayTicks, Object key) {
            PingArgumentsTest.this.scheduled.add(key);
        }

        @Override
        public Ipv4Stack.Context network() {
            return PingArgumentsTest.this.network;
        }

        @Override
        public void commandFailed(String command, RuntimeException error) {
            throw error;
        }
    };

    @BeforeEach
    void setUp() {
        this.stack = new Ipv4Stack();
        NetInterface eth0 = this.stack.addEthernet("eth0", 0, new MacAddress(0x02_00_00_00_00_10L), true);
        this.stack.addAddress(eth0, new InterfaceAddress(Ipv4Address.of(192, 168, 1, 10), 24,
                Optional.of(Ipv4Address.of(192, 168, 1, 255)), Scope.GLOBAL, InterfaceAddress.FOREVER));
        this.network = new FakeMachine(this.stack);
        this.console = new OperatingSystem(this.stack, CommandRegistry.standard()).localConsole();
    }

    private List<String> run(String line) {
        this.console.submit(this.machine, line);
        return this.console.lines();
    }

    @Test
    void withoutDestination() {
        assertEquals(PingOutputTest.reference("ping no destination"), this.run("ping"));
        assertEquals(2, this.console.lastStatus());
    }

    @Test
    void aNameIsNotResolvedWithoutNameService() {
        assertEquals(PingOutputTest.reference("ping name"), this.run("ping foo"));
        assertEquals(2, this.console.lastStatus());
    }

    @Test
    void aCountOfZeroIsOutOfRange() {
        assertEquals(PingOutputTest.reference("ping count zero"), this.run("ping -c 0 192.168.1.1"));
        assertEquals(1, this.console.lastStatus());
    }

    @Test
    void aCountThatIsNotANumber() {
        assertEquals(PingOutputTest.reference("ping count abc"), this.run("ping -c abc 192.168.1.1"));
        assertEquals(1, this.console.lastStatus());
    }

    @Test
    void anUnknownOptionGetsTheErrorOfGetoptThenTheUsage() {
        List<String> lines = this.run("ping -z 192.168.1.1");

        assertEquals(PingOutputTest.reference("ping unknown option"), lines.subList(0, 1));
        assertEquals(PingCommand.USAGE, lines.subList(1, lines.size()));
        assertEquals(2, this.console.lastStatus());
    }

    @Test
    void theBroadcastAddressIsRefused() {
        assertEquals(PingOutputTest.reference("ping broadcast"), this.run("ping -c 1 192.168.1.255"));
        assertEquals(2, this.console.lastStatus());
    }

    @Test
    void anAddressWithoutRouteIsRefusedBeforeTheHeader() {
        assertEquals(PingOutputTest.reference("ping no route"), this.run("ping -c 1 10.9.9.9"));
        assertEquals(2, this.console.lastStatus());
    }

    @Test
    void theCountMaySticktoTheOptionAndOptionsMayFollowTheDestination() {
        this.run("ping 192.168.1.10 -c1");

        assertEquals("PING 192.168.1.10 (192.168.1.10) 56(84) bytes of data.", this.console.lines().getFirst());
        assertEquals("1 packets transmitted, 1 received, 0% packet loss, time 0ms", this.console.lines().get(4));
        assertEquals(0, this.console.lastStatus());
    }

    @Test
    void aCountTooLargeOverflows() {
        assertEquals(PingOutputTest.reference("ping count too large"), this.run("ping -c 99999999999999999999 192.168.1.1"));
        assertEquals(1, this.console.lastStatus());
    }

    @Test
    void theLimitedBroadcastIsRefusedOnlyWithARoute() {
        assertEquals(PingOutputTest.reference("ping limited broadcast without default route"),
                this.run("ping -c 1 255.255.255.255"));
        this.stack.addRoute(new Route(Ipv4Prefix.DEFAULT, Optional.of(Ipv4Address.of(192, 168, 1, 1)), "eth0",
                RouteProtocol.BOOT, Scope.GLOBAL, Optional.empty()));
        int before = this.console.lines().size();

        List<String> lines = this.run("ping -c 1 255.255.255.255");

        assertEquals(PingOutputTest.reference("ping limited broadcast"), lines.subList(before, lines.size()));
    }

    @Test
    void helpGivesTheUsageWithStatusTwo() {
        assertEquals(PingCommand.USAGE, this.run("ping -h"));
        assertEquals(2, this.console.lastStatus());
    }

    @Test
    void aMissingCountAndTwoDestinationsGetTheUsage() {
        List<String> missing = this.run("ping 192.168.1.1 -c");
        assertEquals(PingOutputTest.reference("ping missing count"), missing.subList(0, 1));
        assertEquals(PingCommand.USAGE, missing.subList(1, missing.size()));
        assertEquals(2, this.console.lastStatus());

        int before = this.console.lines().size();
        List<String> two = this.run("ping 192.168.1.1 192.168.1.2");
        assertEquals(PingCommand.USAGE, two.subList(before, two.size()));
        assertTrue(this.scheduled.isEmpty(), "nothing was sent");
    }
}
