package io.github.maximcuynat.worldwideblocks.os.command.ip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClientScript;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpLease;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * Output of {@code ip}, compared with the real output of {@code src/test/resources/linux-real-outputs.txt} (Debian
 * in Docker). The only changes made to the real lines are those of the simulated machine, each named in
 * {@link #adapt}: what the reference cannot show (a lease, no cable) is checked against the format read in the
 * sources of iproute2 ({@link IpOutput}).
 */
class IpCommandTest {
    private static final MacAddress MAC = new MacAddress(0x02_00_00_00_3F_2AL);
    private static final Ipv4Address BOX = Ipv4Address.of(192, 168, 1, 1);
    private static final Ipv4Address ADDRESS = Ipv4Address.of(192, 168, 1, 10);
    private static final long LEASE_TICKS = 24_000L;

    private Ipv4Stack stack;
    private NetInterface eth0;

    @BeforeEach
    void setUp() {
        this.stack = new Ipv4Stack();
        this.eth0 = this.stack.addEthernet("eth0", 0, MAC, true);
    }

    private void bound(long obtainedAt) {
        DhcpClientScript.apply(this.stack, this.eth0, Optional.of(new DhcpLease(ADDRESS, Ipv4Address.of(255, 255, 255, 0),
                BOX, BOX, BOX, new MacAddress(0x02_00_00_00_00_FFL), obtainedAt, obtainedAt + LEASE_TICKS)));
    }

    private record Run(int status, List<String> lines) {
    }

    private Run ip(long now, String line) {
        List<String> lines = new ArrayList<>();
        int status = IpCommand.run(this.stack, Arrays.asList(line.split(" ")), now, lines::add);
        return new Run(status, lines);
    }

    /** The lines of a section {@code === name} of the reference file. */
    private static List<String> reference(String section) {
        try (InputStream in = IpCommandTest.class.getResourceAsStream("/linux-real-outputs.txt")) {
            List<String> all = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            int start = all.indexOf("=== " + section) + 1;
            assertTrue(start > 0, "section " + section);
            int end = start;
            while (end < all.size() && !all.get(end).startsWith("=== ")) {
                end++;
            }
            return all.subList(start, end);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A line of the reference, with the values of the simulated machine instead of those of the Docker container. */
    private static String adapt(String real) {
        return real
                // Interface 11 of the container, a veth with its peer (@if5215): eth0 is the second interface here.
                .replace("11: eth0@if5215:", "2: eth0:")
                // MTU of the Docker network; an Ethernet card has 1500.
                .replace("mtu 65535", "mtu 1500")
                // A veth has no queue; an Ethernet card has fq_codel and a queue of 1000 (IpOutput).
                .replace("qdisc noqueue state UP", "qdisc fq_codel state UP")
                .replace("group default $", "group default qlen 1000")
                .replace("76:85:9b:bb:95:bb", MAC.toString())
                // The network namespace of the container.
                .replace(" link-netnsid 0", "")
                .replace("172.17.0.0/16", "192.168.1.0/24")
                .replace("172.17.255.255", "192.168.1.255")
                .replace("172.17.0.2/16", "192.168.1.10/24")
                .replace("172.17.0.2", "192.168.1.10")
                .replace("172.17.0.1", "192.168.1.1");
    }

    private static List<String> adapt(List<String> real) {
        // "group default $" marks the end of a line: the veth has no queue length after it.
        return real.stream().map(line -> line.endsWith("group default ") ? line + "$" : line).map(IpCommandTest::adapt).toList();
    }

    // ---- Against the real output ----

    @Test
    void ipAddrShowsTheLoopbackAsTheRealOneWithoutIpv6() {
        List<String> real = reference("ip addr");
        // The mod has no IPv6: the two inet6 lines of lo are left out (decision of lot 1c-1).
        List<String> expected = List.of(real.get(0), real.get(1), real.get(2), real.get(3));

        assertEquals(expected, this.ip(0, "ip addr show lo").lines());
    }

    @Test
    void ipAddrShowsTheAddressOfTheLeaseInTheRealFormat() {
        this.bound(0);
        List<String> all = adapt(reference("ip addr"));
        List<String> real = all.subList(all.size() - 4, all.size());
        // From DHCP: "dynamic" before the label, and the lifetimes of the lease instead of "forever" (IpOutput).
        List<String> expected = List.of(real.get(0), real.get(1),
                real.get(2).replace("scope global eth0", "scope global dynamic eth0"),
                real.get(3).replace("forever", "1200sec"));

        Run run = this.ip(0, "ip a");

        assertEquals(0, run.status());
        assertEquals(expected, run.lines().subList(4, 8));
        assertEquals(8, run.lines().size());
    }

    @Test
    void ipLinkShowsTheInterfacesInTheRealFormat() {
        List<String> real = adapt(reference("ip link"));
        List<String> expected = List.of(real.get(0), real.get(1), real.get(real.size() - 2), real.get(real.size() - 1));

        assertEquals(expected, this.ip(0, "ip link").lines());
        assertEquals(expected, this.ip(0, "ip l show").lines());
    }

    @Test
    void ipRouteShowsTheRoutesOfTheLeaseInTheRealFormat() {
        this.bound(0);

        assertEquals(adapt(reference("ip route")), this.ip(0, "ip route").lines());
        assertEquals(adapt(reference("ip route")), this.ip(0, "ip r s dev eth0").lines());
    }

    // ---- What the reference does not show ----

    @Test
    void theLifetimeCountsDownInWholeSecondsRoundedUp() {
        this.bound(0);

        assertEquals("       valid_lft 1199sec preferred_lft 1199sec", this.ip(30, "ip a show eth0").lines().get(3));
        assertEquals("       valid_lft 1198sec preferred_lft 1198sec", this.ip(40, "ip a show eth0").lines().get(3));
    }

    @Test
    void withoutCableTheInterfaceHasNoCarrierAndNoAddress() {
        this.stack.setCarrier(this.eth0, false);

        assertEquals(List.of(
                "2: eth0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc fq_codel state DOWN group default qlen 1000",
                "    link/ether 02:00:00:00:3f:2a brd ff:ff:ff:ff:ff:ff"), this.ip(0, "ip addr show dev eth0").lines());
        assertEquals("2: eth0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc fq_codel state DOWN mode DEFAULT group default qlen 1000",
                this.ip(0, "ip link show eth0").lines().getFirst());
        assertEquals(List.of(), this.ip(0, "ip route").lines());
    }

    @Test
    void withMinus4AnInterfaceWithoutIpv4AddressIsLeftOut() {
        assertEquals(4, this.ip(0, "ip -4 a").lines().size(), "lo only");
    }

    // ---- Errors of the real tool ----

    @Test
    void anUnknownDeviceIsReported() {
        assertEquals(new Run(1, List.of("Device \"eth5\" does not exist.")), this.ip(0, "ip addr show eth5"));
        assertEquals(new Run(1, List.of("Device \"eth5\" does not exist.")), this.ip(0, "ip link show dev eth5"));
        assertEquals(new Run(1, List.of("Cannot find device \"eth5\"")), this.ip(0, "ip route show dev eth5"));
    }

    @Test
    void unknownObjectsCommandsAndOptionsGetTheMessagesOfIproute2() {
        assertEquals(new Run(1, List.of("Object \"foo\" is unknown, try \"ip help\".")), this.ip(0, "ip foo"));
        assertEquals(new Run(255, List.of("Command \"foo\" is unknown, try \"ip address help\".")), this.ip(0, "ip a foo"));
        assertEquals(new Run(255, List.of("Command \"foo\" is unknown, try \"ip link help\".")), this.ip(0, "ip link foo"));
        assertEquals(new Run(255, List.of("Command \"foo\" is unknown, try \"ip route help\".")), this.ip(0, "ip route foo"));
        assertEquals(new Run(255, List.of("Option \"-s\" is unknown, try \"ip -help\".")), this.ip(0, "ip -s a"));
        assertEquals(new Run(255, List.of("Command line is not complete. Try option \"help\"")), this.ip(0, "ip a show dev"));
        assertEquals(new Run(255, List.of("Error: either \"dev\" is duplicate, or \"lo\" is garbage.")),
                this.ip(0, "ip a show eth0 lo"));
        assertEquals(new Run(1, List.of("Error: any valid prefix is expected rather than \"foo\".")), this.ip(0, "ip r show foo"));
    }

    @Test
    void abbreviationsFollowTheOrderOfTheTableOfIproute2() {
        assertEquals(this.ip(0, "ip address").lines(), this.ip(0, "ip a").lines());
        assertEquals(this.ip(0, "ip link").lines(), this.ip(0, "ip l").lines(), "link comes before l2tp");
        assertEquals(this.ip(0, "ip route").lines(), this.ip(0, "ip r").lines(), "route comes before rule");
        assertEquals(List.of("Object \"ru\" is unknown, try \"ip help\"."), this.ip(0, "ip ru").lines(),
                "rule exists in iproute2 but not on this machine");
    }

    @Test
    void changesAreRefusedToAnAccountThatIsNotRoot() {
        assertEquals(new Run(2, List.of("RTNETLINK answers: Operation not permitted")),
                this.ip(0, "ip addr add 192.168.1.50/24 dev eth0"));
        assertEquals(new Run(2, List.of("RTNETLINK answers: Operation not permitted")), this.ip(0, "ip link set eth0 down"));
        assertEquals(new Run(2, List.of("RTNETLINK answers: Operation not permitted")), this.ip(0, "ip route del default"));
    }

    @Test
    void helpShowsTheUsageOfWhatThisMachineSupports() {
        Run run = this.ip(0, "ip");

        assertEquals(new Run(255, IpCommand.IP_USAGE), run);
        assertEquals(run, this.ip(0, "ip help"));
        assertEquals(run, this.ip(0, "ip -help"));
        assertEquals(new Run(255, IpCommand.ADDRESS_USAGE), this.ip(0, "ip a help"));
        assertEquals(new Run(255, IpCommand.ADDRESS_USAGE), this.ip(0, "ip a show help"));
        assertEquals(new Run(255, IpCommand.LINK_USAGE), this.ip(0, "ip link help"));
        assertEquals(new Run(255, IpCommand.ROUTE_USAGE), this.ip(0, "ip route help"));
    }
}
