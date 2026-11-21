package io.github.maximcuynat.worldwideblocks.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.PortKind;
import io.github.maximcuynat.worldwideblocks.port.PortLayout.Area;
import io.github.maximcuynat.worldwideblocks.port.PortLayout.Port;

/**
 * The port layouts of the devices ({@code docs/cable-design.md}, section 3): ports, zones on the shape and disjoint, holes
 * inside their zone, rotation in the four orientations. That the zones also lie on the real shape of each block, and that
 * the layouts have as many ports as the {@code PORTS} of the blocks, is checked by the game tests ({@code PortLayoutTests}),
 * where the blocks can be loaded.
 */
class PortLayoutTest {
    private static final double EPS = 1.0e-9;

    private static final Map<String, PortLayout> LAYOUTS = Map.of(
            "pc_tower", PortLayouts.PC_TOWER,
            "laptop", PortLayouts.LAPTOP,
            "internet_box", PortLayouts.INTERNET_BOX,
            "desktop_switch_5", PortLayouts.DESKTOP_SWITCH_5,
            "desktop_switch_8", PortLayouts.DESKTOP_SWITCH_8);

    @Test
    void portsAndConnectors() {
        assertEquals(List.of(PortKind.ETHERNET), PortLayouts.PC_TOWER.kinds());
        assertEquals(List.of(PortKind.ETHERNET), PortLayouts.LAPTOP.kinds());
        assertEquals(List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE),
                PortLayouts.INTERNET_BOX.kinds());
        assertEquals(PortKind.ethernet(5), PortLayouts.DESKTOP_SWITCH_5.kinds());
        assertEquals(PortKind.ethernet(8), PortLayouts.DESKTOP_SWITCH_8.kinds());
    }

    @Test
    void zonesLieOnAFaceOfTheBody() {
        forEachTurn((name, layout) -> {
            for (Port port : layout.ports()) {
                for (Area area : port.zone()) {
                    assertTrue(layout.body().stream().anyMatch(body -> onFace(body, area)),
                            name + ": zone of port " + port.displayNumber() + " not on the " + area.side() + " face of the body: " + area);
                }
            }
        });
    }

    @Test
    void zonesAreDisjoint() {
        forEachTurn((name, layout) -> {
            List<Port> ports = layout.ports();
            for (int i = 0; i < ports.size(); i++) {
                for (int j = i + 1; j < ports.size(); j++) {
                    for (Area a : ports.get(i).zone()) {
                        for (Area b : ports.get(j).zone()) {
                            assertFalse(a.side() == b.side() && a.box().overlaps(b.box(), EPS),
                                    name + ": zones of ports " + (i + 1) + " and " + (j + 1) + " overlap: " + a + ", " + b);
                        }
                    }
                }
            }
        });
    }

    @Test
    void eachHoleIsInsideItsZoneAndItsMouthOnTheFace() {
        forEachTurn((name, layout) -> {
            for (Port port : layout.ports()) {
                PxBox flat = flatten(port.hole(), port.face(), port.zone().stream()
                        .filter(area -> area.side() == port.face()).findFirst().orElseThrow().box());
                assertTrue(port.zone().stream().anyMatch(area -> area.side() == port.face() && area.box().contains(flat, EPS)),
                        name + ": hole of port " + port.displayNumber() + " outside its zone");
                assertTrue(port.hole().contains(port.mouth(), EPS), name + ": mouth of port " + port.displayNumber() + " outside its hole");
                assertEquals(faceCoordinate(port.hole(), port.face()), coordinate(port.mouth(), port.face()), EPS,
                        name + ": mouth of port " + port.displayNumber() + " not on the outer face of its hole");
                PxPoint centre = port.hole().centre();
                assertEquals(coordinateAcross(centre, port.face()), coordinateAcross(port.mouth(), port.face()), EPS);
                assertEquals(centre.y(), port.mouth().y(), EPS);
            }
        });
    }

    @Test
    void holesAreTheSingleRj45Size() {
        LAYOUTS.forEach((name, layout) -> {
            for (Port port : layout.ports()) {
                if (port.connector() == Connector.RJ45) {
                    PxBox hole = port.hole();
                    double width = port.face().x() != 0 ? hole.maxZ() - hole.minZ() : hole.maxX() - hole.minX();
                    assertEquals(PortLayouts.RJ45, width, EPS, name + ": width of the hole of port " + port.displayNumber());
                    assertEquals(PortLayouts.RJ45, hole.maxY() - hole.minY(), EPS, name + ": height of the hole of port " + port.displayNumber());
                }
            }
        });
    }

    /** The mouth of LAN 1 of the box, written by hand for each orientation (blockstate {@code y} 0, 90, 180, 270). */
    @Test
    void boxPortOneTurnsWithTheBlock() {
        PortLayout box = PortLayouts.INTERNET_BOX;
        assertPort(box.turned(0).ports().get(0), PortSide.NORTH, 9.5, 1.75, 6.75);
        // Front east (x = 16 - 6.75); for a viewer to the east, the left is the south.
        assertPort(box.turned(1).ports().get(0), PortSide.EAST, 9.25, 1.75, 9.5);
        // Front south; for a viewer to the south, the left is the west.
        assertPort(box.turned(2).ports().get(0), PortSide.SOUTH, 6.5, 1.75, 9.25);
        // Front west; for a viewer to the west, the left is the north.
        assertPort(box.turned(3).ports().get(0), PortSide.WEST, 6.75, 1.75, 6.5);
        // The line input, at the back.
        assertPort(box.turned(1).ports().get(4), PortSide.WEST, 6.5, 2.0, 6.5);
    }

    @Test
    void laptopPortTurnsWithTheBlock() {
        assertPort(PortLayouts.LAPTOP.ports().get(0), PortSide.EAST, 15.0, 0.5, 10.75);
        assertPort(PortLayouts.LAPTOP.turned(1).ports().get(0), PortSide.SOUTH, 16 - 10.75, 0.5, 15.0);
        assertPort(PortLayouts.LAPTOP.turned(2).ports().get(0), PortSide.WEST, 1.0, 0.5, 16 - 10.75);
        assertPort(PortLayouts.LAPTOP.turned(3).ports().get(0), PortSide.NORTH, 10.75, 0.5, 1.0);
    }

    @Test
    void fourTurnsGiveTheLayoutBack() {
        LAYOUTS.forEach((name, layout) -> {
            PortLayout back = layout.turned(1).turned(1).turned(1).turned(1);
            for (int i = 0; i < layout.ports().size(); i++) {
                Port expected = layout.ports().get(i);
                Port actual = back.ports().get(i);
                assertEquals(expected.face(), actual.face());
                assertClose(expected.mouth(), actual.mouth(), name);
            }
            assertEquals(layout, layout.turned(4));
        });
    }

    /** A click in the middle of each rectangle of a zone chooses its port, in the four orientations. */
    @Test
    void aClickInAZoneChoosesItsPort() {
        forEachTurn((name, layout) -> {
            for (Port port : layout.ports()) {
                for (Area area : port.zone()) {
                    Optional<Port> aimed = layout.portAt(area.side(), area.box().centre());
                    assertEquals(Optional.of(port.index()), aimed.map(Port::index), name + ": click in the middle of " + area);
                }
            }
        });
    }

    @Test
    void aClickElsewhereChoosesNoPort() {
        // The top of the box: no zone there.
        assertEquals(Optional.empty(), PortLayouts.INTERNET_BOX.portAt(PortSide.UP, new PxPoint(8, 7, 8)));
        // The front of the box above the band of the ports.
        assertEquals(Optional.empty(), PortLayouts.INTERNET_BOX.portAt(PortSide.NORTH, new PxPoint(8, 5, 6.5)));
        // A point of a zone, but clicked on another face.
        assertEquals(Optional.empty(), PortLayouts.DESKTOP_SWITCH_5.portAt(PortSide.EAST, new PxPoint(8, 0.5, 6.5)));
        // The middle of the block, as the old game tests click: not on the top of a switch (1 px high).
        assertEquals(Optional.empty(), PortLayouts.DESKTOP_SWITCH_5.portAt(PortSide.UP, new PxPoint(8, 8, 8)));
        // The screen of the laptop.
        assertEquals(Optional.empty(), PortLayouts.LAPTOP.portAt(PortSide.UP, new PxPoint(8, 1, 8)));
    }

    /** On a switch, the top of the zones runs to the back: a click near the back edge above port 2 still chooses port 2. */
    @Test
    void theZoneOfASwitchGoesOverItsTop() {
        Port port = PortLayouts.DESKTOP_SWITCH_5.ports().get(1);
        double x = port.mouth().x();
        assertEquals(Optional.of(1), PortLayouts.DESKTOP_SWITCH_5.portAt(PortSide.UP, new PxPoint(x, 1.0, 9.4)).map(Port::index));
        assertEquals(Optional.of(1), PortLayouts.DESKTOP_SWITCH_5.portAt(PortSide.NORTH, new PxPoint(x, 0.95, 6.5)).map(Port::index));
        // Halfway between ports 4 and 5 of the 8-port switch: the middle of the post between the two groups.
        assertEquals(Optional.of(3), PortLayouts.DESKTOP_SWITCH_8.portAt(PortSide.NORTH, new PxPoint(8.01, 0.5, 6.0)).map(Port::index));
        assertEquals(Optional.of(4), PortLayouts.DESKTOP_SWITCH_8.portAt(PortSide.NORTH, new PxPoint(7.99, 0.5, 6.0)).map(Port::index));
    }

    @Test
    void sidesTurnClockwise() {
        assertEquals(PortSide.EAST, PortSide.NORTH.turned(1));
        assertEquals(PortSide.NORTH, PortSide.WEST.turned(1));
        assertEquals(PortSide.WEST, PortSide.NORTH.turned(-1));
        assertEquals(PortSide.UP, PortSide.UP.turned(3));
        for (PortSide side : List.of(PortSide.NORTH, PortSide.EAST, PortSide.SOUTH, PortSide.WEST)) {
            assertEquals(side, PortSide.NORTH.turned(side.quartersFromNorth()));
        }
    }

    private interface LayoutCheck {
        void check(String name, PortLayout layout);
    }

    private static void forEachTurn(LayoutCheck check) {
        LAYOUTS.forEach((name, layout) -> {
            for (int quarters = 0; quarters < 4; quarters++) {
                check.check(name + " turned " + 90 * quarters, layout.turned(quarters));
            }
        });
    }

    /** The area is inside the body box and on its face of that side. */
    private static boolean onFace(PxBox body, Area area) {
        return body.contains(area.box(), EPS)
                && Math.abs(faceCoordinate(area.box(), area.side()) - faceCoordinate(body, area.side())) < EPS;
    }

    /** The coordinate of the face of a box on that side (the max for a positive normal, the min otherwise). */
    private static double faceCoordinate(PxBox box, PortSide side) {
        return switch (side) {
            case NORTH -> box.minZ();
            case SOUTH -> box.maxZ();
            case WEST -> box.minX();
            case EAST -> box.maxX();
            case DOWN -> box.minY();
            case UP -> box.maxY();
        };
    }

    private static double coordinate(PxPoint point, PortSide side) {
        return side.x() != 0 ? point.x() : side.y() != 0 ? point.y() : point.z();
    }

    /** The horizontal coordinate across a horizontal side: z for east and west, x for north and south. */
    private static double coordinateAcross(PxPoint point, PortSide side) {
        return side.x() != 0 ? point.z() : point.x();
    }

    /** The hole squashed onto the plane of a zone of its face. */
    private static PxBox flatten(PxBox hole, PortSide face, PxBox plane) {
        return switch (face) {
            case NORTH, SOUTH -> new PxBox(hole.minX(), hole.minY(), plane.minZ(), hole.maxX(), hole.maxY(), plane.maxZ());
            case EAST, WEST -> new PxBox(plane.minX(), hole.minY(), hole.minZ(), plane.maxX(), hole.maxY(), hole.maxZ());
            case UP, DOWN -> new PxBox(hole.minX(), plane.minY(), hole.minZ(), hole.maxX(), plane.maxY(), hole.maxZ());
        };
    }

    private static void assertPort(Port port, PortSide face, double x, double y, double z) {
        assertEquals(face, port.face());
        assertClose(new PxPoint(x, y, z), port.mouth(), "mouth of port " + port.displayNumber());
    }

    private static void assertClose(PxPoint expected, PxPoint actual, String what) {
        assertEquals(expected.x(), actual.x(), EPS, what + " x");
        assertEquals(expected.y(), actual.y(), EPS, what + " y");
        assertEquals(expected.z(), actual.z(), EPS, what + " z");
    }
}
