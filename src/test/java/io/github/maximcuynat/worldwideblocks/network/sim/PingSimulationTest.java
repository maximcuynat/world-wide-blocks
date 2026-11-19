package io.github.maximcuynat.worldwideblocks.network.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.DeviceRole;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.LinkMedium;
import io.github.maximcuynat.worldwideblocks.network.NetworkGraph;
import io.github.maximcuynat.worldwideblocks.network.NetworkNode;
import io.github.maximcuynat.worldwideblocks.network.PortKind;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpStore;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.os.ConsoleLine;
import io.github.maximcuynat.worldwideblocks.os.ConsoleSession;

/**
 * {@code ping} on a box with two towers 2 blocks away, through the real engine: DHCP, ARP, ICMP, the latency of
 * the path, and the console. Each output is compared with a real run of Debian 13
 * ({@code src/test/resources/linux-real-outputs.txt}), with what differs on the simulated machine: its addresses,
 * and times computed by the latency model ({@code Latency}) and by game ticks instead of measured.
 *
 * <p>Round trip to a neighbour already resolved: cable 10 ns, switch of the box 2 µs, the computer that answers
 * 50 µs, back the same way, and 50 µs for the computer that receives the reply: 104 µs ({@code time=0.104 ms}). The
 * first request waits for ARP, which makes the same round trip first: 208 µs.
 */
class PingSimulationTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final List<PortKind> BOX_PORTS =
            List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE);
    private static final long SECOND = 20;

    private NetworkGraph graph;
    private NetworkSimulation sim;
    private DeviceId tower;
    private DeviceId other;
    private long now;

    @BeforeEach
    void setUp() {
        this.graph = new NetworkGraph(new SplittableRandom(11));
        this.sim = new NetworkSimulation(this.graph, new DhcpStore(), SimulationLimits.DEFAULT);
        NetworkNode boxNode = new NetworkNode(new DeviceId(OVERWORLD, 0, 64, 0), "worldwideblocks:internet_box", DeviceRole.BOX, BOX_PORTS);
        NetworkNode towerNode = new NetworkNode(new DeviceId(OVERWORLD, 2, 64, 0), "worldwideblocks:pc_tower", 1);
        NetworkNode otherNode = new NetworkNode(new DeviceId(OVERWORLD, -2, 64, 0), "worldwideblocks:pc_tower", 1);
        this.graph.putNode(boxNode);
        this.graph.putNode(towerNode);
        this.graph.putNode(otherNode);
        this.tower = towerNode.id();
        this.other = otherNode.id();
        this.graph.connect(new PortRef(boxNode.id(), 0), new PortRef(this.tower, 0), LinkMedium.COPPER_ETHERNET, true);
        this.graph.connect(new PortRef(boxNode.id(), 1), new PortRef(this.other, 0), LinkMedium.COPPER_ETHERNET, true);
        this.sim.tick(0); // both computers get their address in this tick
    }

    private void tickUntil(long time) {
        while (this.now < time) {
            this.sim.tick(++this.now);
        }
    }

    private ConsoleSession console() {
        return this.host(this.tower).system().localConsole();
    }

    private HostLogic host(DeviceId device) {
        return (HostLogic) this.sim.logic(device).orElseThrow();
    }

    private Ipv4Address address(DeviceId device) {
        Ipv4Stack stack = this.host(device).system().stack();
        return stack.primaryAddress(stack.interfaceNamed("eth0").orElseThrow()).orElseThrow();
    }

    /** Types a line at the next tick, lets {@code ticks} ticks run, and returns what the console wrote meanwhile. */
    private List<String> type(String line, long ticks) {
        int before = this.console().lines().size();
        this.sim.consoleLine(this.tower, line);
        this.tickUntil(this.now + 1 + ticks);
        List<String> lines = this.console().lines();
        return new ArrayList<>(lines.subList(before, lines.size()));
    }

    private List<ConsoleLine.Advice> advice() {
        return this.console().entries().stream().filter(ConsoleLine.Advice.class::isInstance)
                .map(ConsoleLine.Advice.class::cast).toList();
    }

    private static List<String> reference(String section) {
        try (InputStream in = PingSimulationTest.class.getResourceAsStream("/linux-real-outputs.txt")) {
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

    /** A real output with the addresses of this scene and the given times in place of the measured ones. */
    private static List<String> adapted(List<String> real, String fromAddress, String toAddress, String... times) {
        List<String> lines = new ArrayList<>();
        int next = 0;
        for (String line : real) {
            String adapted = line.replace(fromAddress, toAddress);
            if (adapted.contains("time=") && next < times.length) {
                adapted = adapted.replaceFirst("time=[0-9.]+ ms", "time=" + times[next++] + " ms");
            }
            lines.add(adapted);
        }
        return lines;
    }

    @Test
    void theBoxAnswersAndTheFirstRequestWaitsForArp() {
        List<String> lines = this.type("ping -c 3 192.168.1.1", 2 * SECOND);

        List<String> expected = new ArrayList<>(reference("ping router"));
        expected.set(1, expected.get(1).replace("0.507", "0.208"));
        expected.set(2, expected.get(2).replace("0.055", "0.104"));
        expected.set(3, expected.get(3).replace("0.049", "0.104"));
        expected.set(6, expected.get(6).replace("time 2043ms", "time 2000ms"));
        expected.set(7, "rtt min/avg/max/mdev = 0.104/0.138/0.208/0.049 ms");
        assertEquals(expected, lines);
        assertFalse(this.console().commandRunning(), "the third reply ends it in the tick of the third request");
        assertEquals(0, this.console().lastStatus());
        assertTrue(this.advice().isEmpty());
    }

    @Test
    void theOtherComputerAnswers() {
        String target = this.address(this.other).toString();

        List<String> lines = this.type("ping -c 2 " + target, SECOND);

        assertEquals(List.of(
                "PING " + target + " (" + target + ") 56(84) bytes of data.",
                "64 bytes from " + target + ": icmp_seq=1 ttl=64 time=0.208 ms",
                "64 bytes from " + target + ": icmp_seq=2 ttl=64 time=0.104 ms",
                "",
                "--- " + target + " ping statistics ---",
                "2 packets transmitted, 2 received, 0% packet loss, time 1000ms",
                "rtt min/avg/max/mdev = 0.104/0.156/0.208/0.052 ms"), lines);
    }

    @Test
    void aLongerCableTakesLonger() {
        NetworkNode farNode = new NetworkNode(new DeviceId(OVERWORLD, 100, 64, 0), "worldwideblocks:pc_tower", 1);
        this.graph.putNode(farNode);
        this.sim.deviceAdded(farNode);
        Link link = this.graph.connect(new PortRef(this.graph.nodes().stream()
                .filter(node -> node.role() == DeviceRole.BOX).findFirst().orElseThrow().id(), 2),
                new PortRef(farNode.id(), 0), LinkMedium.COPPER_ETHERNET, true).link().orElseThrow();
        this.sim.linkUp(link);
        this.tickUntil(this.now + 1);
        String target = this.address(farNode.id()).toString();

        List<String> lines = this.type("ping -c 2 " + target, SECOND);

        // 100 blocks of cable each way: 2 x 500 ns more than the 104.02 us of a neighbour 2 blocks away.
        assertEquals("64 bytes from " + target + ": icmp_seq=1 ttl=64 time=0.210 ms", lines.get(1));
        assertEquals("64 bytes from " + target + ": icmp_seq=2 ttl=64 time=0.105 ms", lines.get(2));
    }

    @Test
    void anAbsentAddressGetsHostUnreachableFromTheComputerItself() {
        String own = this.address(this.tower).toString();

        List<String> lines = this.type("ping -c 3 192.168.1.250", 3 * SECOND);

        List<String> expected = adapted(reference("ping absent"), "172.17.0.250", "192.168.1.250");
        expected = adapted(expected, "From 172.17.0.2 ", "From " + own + " ");
        expected.replaceAll(line -> line.replace("time 2028ms", "time 2000ms"));
        assertEquals(expected, lines);
        assertEquals(1, this.console().lastStatus());
        assertEquals(List.of(new ConsoleLine.Advice("terminal.worldwideblocks.advice.ping.host_unreachable",
                List.of("192.168.1.250"))), this.advice());
    }

    @Test
    void aRequestDueWhenErrorsArriveLeavesBeforeTheyAreRead() {
        String own = this.address(this.tower).toString();

        List<String> lines = this.type("ping -c 6 192.168.1.250", 6 * SECOND);

        List<String> expected = adapted(reference("ping absent count 6"), "192.168.1.10", own);
        expected.replaceAll(line -> line.replace("time 5096ms", "time 5000ms"));
        assertEquals(expected, lines, "errors by three every 3 s, pipe 4 as on Debian");
    }

    @Test
    void ctrlCEndsAContinuousPingWithItsStatistics() {
        String own = this.address(this.tower).toString();
        this.type("ping 192.168.1.250", 11 * SECOND + 2);

        this.sim.consoleInterrupt(this.tower);
        this.tickUntil(this.now + 1);

        List<String> real = adapted(reference("ping absent interrupted"), "192.168.1.10", own);
        List<String> expected = new ArrayList<>(real.subList(0, 10));
        expected.add("^C");
        expected.addAll(real.subList(11, real.size()));
        expected.replaceAll(line -> line.replace("time 11115ms", "time 11000ms"));
        assertEquals(expected, this.console().lines(), "^C ends its line, then the statistics follow");
        assertEquals(1, this.console().lastStatus());
    }

    @Test
    void ctrlCAfterRepliesEndsWithStatusZero() {
        this.type("ping 192.168.1.1", 2 * SECOND + 5);

        this.sim.consoleInterrupt(this.tower);
        this.tickUntil(this.now + 1);

        List<String> lines = this.console().lines();
        assertEquals(List.of("64 bytes from 192.168.1.1: icmp_seq=3 ttl=64 time=0.104 ms", "^C",
                "--- 192.168.1.1 ping statistics ---",
                "3 packets transmitted, 3 received, 0% packet loss, time 2000ms",
                "rtt min/avg/max/mdev = 0.104/0.138/0.208/0.049 ms"), lines.subList(lines.size() - 5, lines.size()));
        assertEquals(0, this.console().lastStatus());
        assertTrue(this.advice().isEmpty(), "no advice after a success");
    }

    @Test
    void outsideTheLocalNetworkTheBoxAnswersNetUnreachable() {
        List<String> lines = this.type("ping -c 3 10.9.9.9", 2 * SECOND);

        List<String> expected = new ArrayList<>(reference("ping outside"));
        expected.replaceAll(line -> line.replace("time 2012ms", "time 2000ms"));
        assertEquals(expected, lines);
        assertEquals(List.of(new ConsoleLine.Advice("terminal.worldwideblocks.advice.ping.net_unreachable",
                List.of("10.9.9.9"))), this.advice());
    }

    @Test
    void ownAddressAndLoopbackAnswerThroughLo() {
        String own = this.address(this.tower).toString();

        List<String> lines = this.type("ping -c 2 " + own, SECOND);

        assertEquals(adapted(reference("ping own address"), "192.168.1.10", own, "0.014", "0.014").subList(0, 6),
                lines.subList(0, 6));
        assertEquals("rtt min/avg/max/mdev = 0.014/0.014/0.014/0.000 ms", lines.get(6));
        assertEquals("64 bytes from 127.0.0.1: icmp_seq=1 ttl=64 time=0.014 ms",
                this.type("ping -c 1 127.0.0.1", 0).get(1));
    }

    @Test
    void withoutAddressThereIsNoRoute() {
        this.sim.linkDown(this.graph.disconnect(new PortRef(this.tower, 0)).orElseThrow());
        this.tickUntil(this.now + 1);

        assertEquals(reference("ping no route"), this.type("ping 192.168.1.1", 0));
        assertEquals(2, this.console().lastStatus());
        assertEquals(List.of(new ConsoleLine.Advice("terminal.worldwideblocks.advice.ping.no_route",
                List.of("192.168.1.1"))), this.advice());
    }

    @Test
    void aRequestWithoutRouteMeanwhileIsCountedAndPrintsNothing() {
        this.type("ping -c 4 192.168.1.1", SECOND);
        this.sim.linkDown(this.graph.disconnect(new PortRef(this.tower, 0)).orElseThrow());
        this.tickUntil(this.now + 2 * SECOND + LINGER_MARGIN);

        List<String> lines = this.console().lines();
        assertEquals(List.of("",
                "--- 192.168.1.1 ping statistics ---",
                "4 packets transmitted, 2 received, 50% packet loss, time 3000ms",
                "rtt min/avg/max/mdev = 0.104/0.156/0.208/0.052 ms"), lines.subList(lines.size() - 4, lines.size()),
                "as \"ping route lost\" on Debian: nothing for the two requests without route");
        assertFalse(this.console().commandRunning());
        assertEquals(0, this.console().lastStatus());
    }

    @Test
    void requestsWithoutReplyPrintNothingAndTheLastWaitIsOneSecond() {
        String target = this.address(this.other).toString();
        int before = this.console().lines().size();
        this.type("ping -c 4 " + target, SECOND);
        this.sim.linkDown(this.graph.disconnect(new PortRef(this.other, 0)).orElseThrow());

        this.tickUntil(this.now + 2 * SECOND + LINGER_MARGIN - 1);
        assertTrue(this.console().commandRunning(), "still waiting for the replies of requests 3 and 4");
        this.tickUntil(this.now + 1);

        List<String> lines = this.console().lines();
        List<String> expected = adapted(reference("ping timeout after replies"), "192.168.1.1", target, "0.208", "0.104");
        expected.replaceAll(line -> line.replace("time 3069ms", "time 3000ms")
                .replace("0.182/0.396/0.610/0.214", "0.104/0.156/0.208/0.052"));
        assertEquals(expected, lines.subList(before, lines.size()),
                "requests 3 and 4 leave and get nothing: no line for them, as on Debian");
        assertEquals(0, this.console().lastStatus());
    }

    @Test
    void aTimeoutPrintsNothing() {
        String target = this.address(this.other).toString();
        this.type("ping -c 1 " + target, 0); // the other computer is now known by ARP
        this.sim.linkDown(this.graph.disconnect(new PortRef(this.other, 0)).orElseThrow());
        this.tickUntil(this.now + 1);
        int before = this.console().lines().size();

        List<String> lines = this.type("ping -c 3 " + target, 2 * SECOND + LINGER_WITHOUT_REPLY);

        List<String> expected = adapted(reference("ping timeout"), "192.168.1.77", target);
        expected.replaceAll(line -> line.replace("time 2052ms", "time 2000ms"));
        assertEquals(expected, lines, "the requests leave unicast and are lost: nothing before the statistics");
        assertFalse(this.console().commandRunning(), "10 s of waiting after the last request, no reply having come");
        assertEquals(1, this.console().lastStatus());
        assertEquals(List.of(new ConsoleLine.Advice("terminal.worldwideblocks.advice.ping.no_reply", List.of(target))),
                this.advice().subList(this.advice().size() - 1, this.advice().size()));
        assertEquals(before + expected.size(), this.console().lines().size());
    }

    /** {@code MAXWAIT}: 10 s after the last request when no reply came. */
    private static final long LINGER_WITHOUT_REPLY = 10 * SECOND;

    /** After the last request, a run with replies waits twice the longest round trip, at least one second. */
    private static final long LINGER_MARGIN = SECOND;

    @Test
    void anInterruptForAComputerRemovedBeforeTheTickIsIgnored() {
        this.sim.consoleLine(this.tower, "ping 192.168.1.1");
        this.tickUntil(this.now + 1);
        this.sim.consoleInterrupt(this.tower);
        this.graph.removeDevice(this.tower).forEach(this.sim::linkDown);
        this.sim.deviceRemoved(this.tower);

        this.tickUntil(this.now + SECOND);

        assertTrue(this.sim.logic(this.tower).isEmpty());
    }
}
