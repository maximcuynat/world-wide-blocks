package io.github.maximcuynat.worldwideblocks.port;

import java.util.ArrayList;
import java.util.List;

import io.github.maximcuynat.worldwideblocks.port.PortLayout.Area;
import io.github.maximcuynat.worldwideblocks.port.PortLayout.Port;

/**
 * The port layout of each device, for a front facing north, taken from its device sheet ({@code design/blockbench/<id>.md},
 * "Port layout"; {@code python3 design/tools/port_layout.py <id>} prints these values). Plain Java.
 *
 * <p>Aim zones ({@code docs/cable-design.md}, section 3): on the face that carries the ports, each port owns the strip
 * of the face up to halfway to its neighbours, the ports at the ends going to the edge of the face; on a flat device
 * (the desktop switches) the strip goes on over the whole depth of the top face. A device with a single port gets a
 * zone on the face of that port, where its outline is shown.
 */
public final class PortLayouts {
    /** Width of every RJ45 hole: 2 texels. */
    public static final double RJ45 = 0.5;

    /**
     * PC tower ({@code pc_tower.md}): one RJ45 port, painted, on an expansion card at the back, low; its zone is the
     * whole back face.
     */
    public static final PortLayout PC_TOWER = new PortLayout(
            List.of(new PxBox(4.5, 0.0, 0.5, 11.5, 15.0, 15.5)),
            List.of(new Port(0, Connector.RJ45, PortSide.SOUTH,
                    List.of(new Area(PortSide.SOUTH, new PxBox(4.5, 0.0, 15.5, 11.5, 15.0, 15.5))),
                    new PxBox(8.5, 4.25, 15.5, 9.0, 4.75, 15.5),
                    new PxPoint(8.75, 4.5, 15.5))));

    /**
     * Laptop ({@code laptop.md}): one RJ45 port, 0.5 px deep, on the left flank of the base for the seated user (east for
     * {@code FACING} north), the same with the lid open or closed; its zone is the flank of the base. The body is the
     * base, common to both states of the lid.
     */
    public static final PortLayout LAPTOP = new PortLayout(
            List.of(new PxBox(1.0, 0.0, 3.5, 15.0, 1.0, 12.5)),
            List.of(new Port(0, Connector.RJ45, PortSide.EAST,
                    List.of(new Area(PortSide.EAST, new PxBox(15.0, 0.0, 3.5, 15.0, 1.0, 12.5))),
                    new PxBox(14.5, 0.25, 10.5, 15.0, 0.75, 11.0),
                    new PxPoint(15.0, 0.5, 10.75))));

    /**
     * Internet box ({@code internet_box.md}): LAN 1 to 4 on the front, recessed 0.5 px behind the panel (z 6.75), the
     * zones on the plane of the glass band where the shape of the block starts (z 6.5), over the lower band of the front
     * (y 0.75 to 3.25); the line input (port 5) painted at the back, its zone on the left half of the back seen from
     * behind, up to the power socket.
     */
    public static final PortLayout INTERNET_BOX;

    /**
     * 5-port desktop switch ({@code desktop_switch_5.md}): ports 1 to 5 on the front (z 6.5), recessed 0.5 px; zones
     * over the whole front and the whole depth of the top.
     */
    public static final PortLayout DESKTOP_SWITCH_5 = flatSwitch(
            new PxBox(6.0, 0.0, 6.5, 10.0, 1.0, 9.5), 9.25, 8.5, 7.75, 7.0, 6.25);

    /**
     * 8-port desktop switch ({@code desktop_switch_8.md}): ports 1 to 8 on the front (z 6), in two groups of 4, recessed
     * 0.5 px; zones over the whole front and the whole depth of the top, ports 4 and 5 meeting in the middle of the post
     * between the groups.
     */
    public static final PortLayout DESKTOP_SWITCH_8 = flatSwitch(
            new PxBox(4.75, 0.0, 6.0, 11.25, 1.0, 10.0), 10.5, 9.75, 9.0, 8.25, 7.25, 6.5, 5.75, 5.0);

    static {
        PxBox body = new PxBox(4.5, 0.0, 6.5, 11.5, 7.0, 9.5);
        List<Port> ports = new ArrayList<>(frontRow(body, 6.75, 1.5, 0.75, 3.25, false, 9.25, 8.25, 7.25, 6.25));
        ports.add(new Port(4, Connector.LINE, PortSide.SOUTH,
                List.of(new Area(PortSide.SOUTH, new PxBox(4.5, 0.75, 9.5, 8.0, 3.25, 9.5))),
                new PxBox(6.25, 1.75, 9.5, 6.75, 2.25, 9.5),
                new PxPoint(6.5, 2.0, 9.5)));
        INTERNET_BOX = new PortLayout(List.of(body), ports);
    }

    private PortLayouts() {
    }

    /** A flat switch: its ports along the whole height of the front, recessed 0.5 px, the zones over the top too. */
    private static PortLayout flatSwitch(PxBox body, double... holeMinX) {
        return new PortLayout(List.of(body), frontRow(body, body.minZ(), 0.25, body.minY(), body.maxY(), true, holeMinX));
    }

    /**
     * RJ45 ports in a row on the north face of the body, numbered from the viewer's left (the east, the highest x),
     * recessed 0.5 px.
     *
     * @param mouthZ   plane of the mouths (the panel the holes are cut in)
     * @param holeMinY bottom of the holes
     * @param zoneMinY bottom of the zones on the front
     * @param zoneMaxY top of the zones on the front
     * @param overTop  the zones go on over the whole depth of the top face
     * @param holeMinX west edge of each hole, in port order
     */
    private static List<Port> frontRow(PxBox body, double mouthZ, double holeMinY, double zoneMinY, double zoneMaxY,
            boolean overTop, double... holeMinX) {
        List<Port> ports = new ArrayList<>();
        for (int i = 0; i < holeMinX.length; i++) {
            double maxX = i == 0 ? body.maxX() : (holeMinX[i - 1] + holeMinX[i] + RJ45) / 2;
            double minX = i == holeMinX.length - 1 ? body.minX() : (holeMinX[i] + holeMinX[i + 1] + RJ45) / 2;
            List<Area> zone = new ArrayList<>();
            zone.add(new Area(PortSide.NORTH, new PxBox(minX, zoneMinY, body.minZ(), maxX, zoneMaxY, body.minZ())));
            if (overTop) {
                zone.add(new Area(PortSide.UP, new PxBox(minX, body.maxY(), body.minZ(), maxX, body.maxY(), body.maxZ())));
            }
            double x = holeMinX[i];
            ports.add(new Port(i, Connector.RJ45, PortSide.NORTH, zone,
                    new PxBox(x, holeMinY, mouthZ, x + RJ45, holeMinY + RJ45, mouthZ + RJ45),
                    new PxPoint(x + RJ45 / 2, holeMinY + RJ45 / 2, mouthZ)));
        }
        return ports;
    }
}
