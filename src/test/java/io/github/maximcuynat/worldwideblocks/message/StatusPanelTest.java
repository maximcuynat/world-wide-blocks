package io.github.maximcuynat.worldwideblocks.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.message.StatusPanel.DeviceStatus;
import io.github.maximcuynat.worldwideblocks.message.StatusPanel.Light;
import io.github.maximcuynat.worldwideblocks.message.StatusPanel.Line;
import io.github.maximcuynat.worldwideblocks.network.PortKind;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClientState;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

class StatusPanelTest {
    private static final List<PortKind> BOX = List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE);
    private static final List<PortKind> TOWER = List.of(PortKind.ETHERNET);
    private static final MacAddress A = new MacAddress(0x02_00_00_00_00_0AL);
    private static final MacAddress B = new MacAddress(0x02_00_00_00_00_0BL);
    private static final MacAddress C = new MacAddress(0x02_00_00_00_00_0CL);

    private static final Line HINT = new Line(Light.NONE, "hud.worldwideblocks.sneak_for_details", List.of());

    /** Box with ports 1, 2 and 3 connected: 2 MACs behind port 1, one behind port 2, none yet behind port 3. */
    private static final DeviceStatus BOX_STATUS = new DeviceStatus(true, List.of(), Map.of(0, List.of(A, B), 1, List.of(C)));

    private static List<Line> box(boolean detailed) {
        Set<Integer> connected = Set.of(0, 1, 2);
        return StatusPanel.lines(BOX, connected::contains, BOX_STATUS, detailed, 0);
    }

    @Test
    void theCompactBoxSummarizesItsLocalNetworkPorts() {
        assertEquals(List.of(
                new Line(Light.CONNECTED, "hud.worldwideblocks.ports_connected", List.of(MessageArg.number(3), MessageArg.number(4))),
                HINT), box(false));
    }

    @Test
    void aBoxWithoutCableShowsTheFreeLight() {
        assertEquals(List.of(
                new Line(Light.FREE, "hud.worldwideblocks.ports_connected", List.of(MessageArg.number(0), MessageArg.number(4))),
                HINT), StatusPanel.lines(BOX, port -> false, null, false, 0));
    }

    @Test
    void theDetailedBoxShowsEachPortWithItsMacs() {
        assertEquals(List.of(
                new Line(Light.CONNECTED, "hud.worldwideblocks.port.connected_learned_more",
                        List.of(MessageArg.number(1), MessageArg.text(A.toString()), MessageArg.number(1))),
                new Line(Light.CONNECTED, "hud.worldwideblocks.port.connected_learned", List.of(MessageArg.number(2), MessageArg.text(C.toString()))),
                new Line(Light.CONNECTED, "hud.worldwideblocks.port.connected", List.of(MessageArg.number(3))),
                new Line(Light.FREE, "hud.worldwideblocks.port.free", List.of(MessageArg.number(4))),
                new Line(Light.INACTIVE, "hud.worldwideblocks.port.line", List.of(MessageArg.number(5)))), box(true));
    }

    @Test
    void theCompactComputerTellsWhetherItIsConnected() {
        DeviceStatus status = new DeviceStatus(false, List.of(A), Map.of());

        assertEquals(List.of(new Line(Light.CONNECTED, "hud.worldwideblocks.connected", List.of()), HINT),
                StatusPanel.lines(TOWER, port -> true, status, false, 0));
        assertEquals(List.of(new Line(Light.FREE, "hud.worldwideblocks.not_connected", List.of()), HINT),
                StatusPanel.lines(TOWER, port -> false, status, false, 0));
    }

    @Test
    void theDetailedComputerShowsItsPortAndItsMac() {
        DeviceStatus status = new DeviceStatus(false, List.of(A), Map.of());

        assertEquals(List.of(
                new Line(Light.CONNECTED, "hud.worldwideblocks.port.connected", List.of(MessageArg.number(1))),
                new Line(Light.NONE, "hud.worldwideblocks.mac", List.of(MessageArg.text(A.toString())))),
                StatusPanel.lines(TOWER, port -> true, status, true, 0));
    }

    @Test
    void withoutServerStateTheDetailedPanelStillShowsThePorts() {
        assertEquals(List.of(new Line(Light.FREE, "hud.worldwideblocks.port.free", List.of(MessageArg.number(1)))),
                StatusPanel.lines(TOWER, port -> false, null, true, 0));
    }

    // ---- DHCP (lot 1b-2) ----

    private static final Ipv4Address ADDRESS = Ipv4Address.of(192, 168, 1, 10);
    private static final Ipv4Address MASK = Ipv4Address.of(255, 255, 255, 0);
    private static final Ipv4Address BOX_ADDRESS = Ipv4Address.of(192, 168, 1, 1);

    private static DeviceStatus tower(DhcpClientState state, boolean withAddress, long expiresAt) {
        Optional<StatusPanel.Address> address = withAddress
                ? Optional.of(new StatusPanel.Address(ADDRESS, MASK, BOX_ADDRESS, BOX_ADDRESS, expiresAt)) : Optional.empty();
        return new DeviceStatus(false, List.of(A), Map.of(), Map.of(0, new StatusPanel.PortAddress(state, address)), Optional.empty());
    }

    private static MessageArg state(String name) {
        return MessageArg.key("hud.worldwideblocks.dhcp.state." + name);
    }

    @Test
    void theCompactComputerShowsItsAddressAndTheStateOfItsClient() {
        assertEquals(List.of(new Line(Light.CONNECTED, "hud.worldwideblocks.dhcp.compact_address",
                        List.of(MessageArg.text("192.168.1.10"), state("bound"))), HINT),
                StatusPanel.lines(TOWER, port -> true, tower(DhcpClientState.BOUND, true, 24_000), false, 0));
        assertEquals(List.of(new Line(Light.CONNECTED, "hud.worldwideblocks.dhcp.compact_address",
                        List.of(MessageArg.text("192.168.1.10"), state("renewing"))), HINT),
                StatusPanel.lines(TOWER, port -> true, tower(DhcpClientState.RENEWING, true, 24_000), false, 12_000));
    }

    @Test
    void aComputerStillLookingForAServerShowsNoAddressWithTheWaitingLight() {
        assertEquals(List.of(new Line(Light.WAITING, "hud.worldwideblocks.dhcp.compact_no_address", List.of(state("selecting"))), HINT),
                StatusPanel.lines(TOWER, port -> true, tower(DhcpClientState.SELECTING, false, 0), false, 0));
        assertEquals(List.of(new Line(Light.WAITING, "hud.worldwideblocks.dhcp.compact_no_address", List.of(state("init_reboot"))), HINT),
                StatusPanel.lines(TOWER, port -> true, tower(DhcpClientState.INIT_REBOOT, false, 0), false, 0));
    }

    @Test
    void anUnpluggedComputerIsNotConnectedWhateverItsLastDhcpState() {
        assertEquals(List.of(new Line(Light.FREE, "hud.worldwideblocks.not_connected", List.of()), HINT),
                StatusPanel.lines(TOWER, port -> false, tower(DhcpClientState.BOUND, true, 24_000), false, 0));
    }

    @Test
    void theDetailedComputerShowsMacAddressMaskGatewayNameServerTimeLeftAndState() {
        assertEquals(List.of(
                new Line(Light.CONNECTED, "hud.worldwideblocks.port.connected", List.of(MessageArg.number(1))),
                new Line(Light.NONE, "hud.worldwideblocks.mac", List.of(MessageArg.text(A.toString()))),
                new Line(Light.NONE, "hud.worldwideblocks.dhcp.address", List.of(MessageArg.text("192.168.1.10"), MessageArg.text("255.255.255.0"))),
                new Line(Light.NONE, "hud.worldwideblocks.dhcp.gateway", List.of(MessageArg.text("192.168.1.1"))),
                new Line(Light.NONE, "hud.worldwideblocks.dhcp.dns", List.of(MessageArg.text("192.168.1.1"))),
                new Line(Light.NONE, "hud.worldwideblocks.dhcp.lease", List.of(MessageArg.text("18:42"))),
                new Line(Light.NONE, "hud.worldwideblocks.dhcp.state", List.of(state("bound")))),
                // 24,000 - 1,560 = 22,440 ticks = 1,122 s = 18 min 42 s
                StatusPanel.lines(TOWER, port -> true, tower(DhcpClientState.BOUND, true, 24_000), true, 1_560));
    }

    @Test
    void theDetailedComputerWithoutAddressOnlyShowsTheStateOfItsClient() {
        assertEquals(List.of(
                new Line(Light.CONNECTED, "hud.worldwideblocks.port.connected", List.of(MessageArg.number(1))),
                new Line(Light.NONE, "hud.worldwideblocks.mac", List.of(MessageArg.text(A.toString()))),
                new Line(Light.NONE, "hud.worldwideblocks.dhcp.state", List.of(state("rebooting")))),
                StatusPanel.lines(TOWER, port -> true, tower(DhcpClientState.REBOOTING, false, 0), true, 0));
    }

    @Test
    void theTimeLeftIsInRealMinutesAndSecondsAndNeverNegative() {
        assertEquals("20:00", StatusPanel.remaining(24_000));
        assertEquals("0:01", StatusPanel.remaining(1), "the started second counts");
        assertEquals("0:00", StatusPanel.remaining(0));
        assertEquals("0:00", StatusPanel.remaining(-40), "client clock slightly ahead");
        assertEquals("1:05:00", StatusPanel.remaining(65 * 60 * 20));
    }

    private static List<Line> boxWithLeases(int count, boolean enabled) {
        List<StatusPanel.Lent> leases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            leases.add(new StatusPanel.Lent(Ipv4Address.of(192, 168, 1, 10 + i), new MacAddress(0x02_00_00_00_01_00L + i)));
        }
        DeviceStatus status = new DeviceStatus(true, List.of(), Map.of(), Map.of(), Optional.of(new StatusPanel.ServerStatus(enabled, leases)));
        List<Line> lines = StatusPanel.lines(BOX, port -> false, status, true, 0);
        return lines.subList(5, lines.size()); // after the five port lines
    }

    @Test
    void theDetailedBoxListsTheAddressesItLent() {
        assertEquals(List.of(
                new Line(Light.NONE, "hud.worldwideblocks.dhcp.leases", List.of(MessageArg.number(2))),
                new Line(Light.NONE, "hud.worldwideblocks.dhcp.lease_line", List.of(MessageArg.text("192.168.1.10"), MessageArg.text("02:00:00:00:01:00"))),
                new Line(Light.NONE, "hud.worldwideblocks.dhcp.lease_line", List.of(MessageArg.text("192.168.1.11"), MessageArg.text("02:00:00:00:01:01")))),
                boxWithLeases(2, true));
    }

    @Test
    void theDetailedBoxShowsFourLeasesAtMostThenCountsTheOthers() {
        List<Line> lines = boxWithLeases(6, true);

        assertEquals(1 + StatusPanel.MAX_LEASE_LINES + 1, lines.size());
        assertEquals(new Line(Light.NONE, "hud.worldwideblocks.dhcp.leases", List.of(MessageArg.number(6))), lines.getFirst());
        assertEquals(new Line(Light.NONE, "hud.worldwideblocks.dhcp.leases_more", List.of(MessageArg.number(2))), lines.getLast());
    }

    @Test
    void aBoxWhoseServerIsOffSaysSo() {
        assertEquals(List.of(new Line(Light.NONE, "hud.worldwideblocks.dhcp.server_off", List.of())), boxWithLeases(3, false));
    }

    @Test
    void theCompactBoxIsUnchangedByItsServer() {
        DeviceStatus status = new DeviceStatus(true, List.of(), Map.of(), Map.of(), Optional.of(new StatusPanel.ServerStatus(true, List.of())));

        assertEquals(List.of(
                new Line(Light.FREE, "hud.worldwideblocks.ports_connected", List.of(MessageArg.number(0), MessageArg.number(4))),
                HINT), StatusPanel.lines(BOX, port -> false, status, false, 0));
    }
}
