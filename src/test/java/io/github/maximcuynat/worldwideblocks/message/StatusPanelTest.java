package io.github.maximcuynat.worldwideblocks.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.message.StatusPanel.DeviceStatus;
import io.github.maximcuynat.worldwideblocks.message.StatusPanel.Light;
import io.github.maximcuynat.worldwideblocks.message.StatusPanel.Line;
import io.github.maximcuynat.worldwideblocks.network.PortKind;
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
        return StatusPanel.lines(BOX, connected::contains, BOX_STATUS, detailed);
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
                HINT), StatusPanel.lines(BOX, port -> false, null, false));
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
                StatusPanel.lines(TOWER, port -> true, status, false));
        assertEquals(List.of(new Line(Light.FREE, "hud.worldwideblocks.not_connected", List.of()), HINT),
                StatusPanel.lines(TOWER, port -> false, status, false));
    }

    @Test
    void theDetailedComputerShowsItsPortAndItsMac() {
        DeviceStatus status = new DeviceStatus(false, List.of(A), Map.of());

        assertEquals(List.of(
                new Line(Light.CONNECTED, "hud.worldwideblocks.port.connected", List.of(MessageArg.number(1))),
                new Line(Light.NONE, "hud.worldwideblocks.mac", List.of(MessageArg.text(A.toString())))),
                StatusPanel.lines(TOWER, port -> true, status, true));
    }

    @Test
    void withoutServerStateTheDetailedPanelStillShowsThePorts() {
        assertEquals(List.of(new Line(Light.FREE, "hud.worldwideblocks.port.free", List.of(MessageArg.number(1)))),
                StatusPanel.lines(TOWER, port -> false, null, true));
    }
}
