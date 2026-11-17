package dev.netcraft.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.netcraft.message.StatusPanel.DeviceStatus;
import dev.netcraft.message.StatusPanel.Light;
import dev.netcraft.message.StatusPanel.Line;
import dev.netcraft.network.PortKind;
import dev.netcraft.network.protocol.MacAddress;

class StatusPanelTest {
    private static final List<PortKind> BOX = List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE);
    private static final List<PortKind> TOWER = List.of(PortKind.ETHERNET);
    private static final MacAddress A = new MacAddress(0x02_00_00_00_00_0AL);
    private static final MacAddress B = new MacAddress(0x02_00_00_00_00_0BL);
    private static final MacAddress C = new MacAddress(0x02_00_00_00_00_0CL);

    private static final Line HINT = new Line(Light.NONE, "hud.netcraft.sneak_for_details", List.of());

    /** Box aux ports 1, 2 et 3 connectés : 2 MAC derrière le port 1, une derrière le port 2, aucune encore derrière le port 3. */
    private static final DeviceStatus BOX_STATUS = new DeviceStatus(true, List.of(), Map.of(0, List.of(A, B), 1, List.of(C)));

    private static List<Line> box(boolean detailed) {
        Set<Integer> connected = Set.of(0, 1, 2);
        return StatusPanel.lines(BOX, connected::contains, BOX_STATUS, detailed);
    }

    @Test
    void laBoxCompacteResumeSesPortsReseauLocal() {
        assertEquals(List.of(
                new Line(Light.CONNECTED, "hud.netcraft.ports_connected", List.of(MessageArg.number(3), MessageArg.number(4))),
                HINT), box(false));
    }

    @Test
    void laBoxSansCableEstAuVoyantLibre() {
        assertEquals(List.of(
                new Line(Light.FREE, "hud.netcraft.ports_connected", List.of(MessageArg.number(0), MessageArg.number(4))),
                HINT), StatusPanel.lines(BOX, port -> false, null, false));
    }

    @Test
    void laBoxDetailleeMontreChaquePortAvecSesMac() {
        assertEquals(List.of(
                new Line(Light.CONNECTED, "hud.netcraft.port.connected_learned_more",
                        List.of(MessageArg.number(1), MessageArg.text(A.toString()), MessageArg.number(1))),
                new Line(Light.CONNECTED, "hud.netcraft.port.connected_learned", List.of(MessageArg.number(2), MessageArg.text(C.toString()))),
                new Line(Light.CONNECTED, "hud.netcraft.port.connected", List.of(MessageArg.number(3))),
                new Line(Light.FREE, "hud.netcraft.port.free", List.of(MessageArg.number(4))),
                new Line(Light.INACTIVE, "hud.netcraft.port.line", List.of(MessageArg.number(5)))), box(true));
    }

    @Test
    void lePosteCompactDitSIlEstConnecte() {
        DeviceStatus status = new DeviceStatus(false, List.of(A), Map.of());

        assertEquals(List.of(new Line(Light.CONNECTED, "hud.netcraft.connected", List.of()), HINT),
                StatusPanel.lines(TOWER, port -> true, status, false));
        assertEquals(List.of(new Line(Light.FREE, "hud.netcraft.not_connected", List.of()), HINT),
                StatusPanel.lines(TOWER, port -> false, status, false));
    }

    @Test
    void lePosteDetailleMontreSonPortEtSaMac() {
        DeviceStatus status = new DeviceStatus(false, List.of(A), Map.of());

        assertEquals(List.of(
                new Line(Light.CONNECTED, "hud.netcraft.port.connected", List.of(MessageArg.number(1))),
                new Line(Light.NONE, "hud.netcraft.mac", List.of(MessageArg.text(A.toString())))),
                StatusPanel.lines(TOWER, port -> true, status, true));
    }

    @Test
    void sansEtatDuServeurLePanneauDetailleMontreQuandMemeLesPorts() {
        assertEquals(List.of(new Line(Light.FREE, "hud.netcraft.port.free", List.of(MessageArg.number(1)))),
                StatusPanel.lines(TOWER, port -> false, null, true));
    }
}
