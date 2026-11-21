package io.github.maximcuynat.worldwideblocks.port;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.PortKind;

/**
 * The ports of a device, for a front facing north, in model pixels ({@code docs/cable-design.md}, section 3): the single
 * source of truth for the server (which port was clicked) and for the client (outline, panel, attachment, plug). The
 * values come from the device sheets ({@code design/blockbench/<id>.md}, "Port layout"). A device turned by its
 * {@code FACING} uses {@link #turned}, as the blockstate turns its model.
 *
 * <p>Plain Java, no Minecraft class.
 *
 * @param body  the boxes of the shape of the block (selection and collision), on which the aim zones lie
 * @param ports the ports, in the order of the engine (index 0 first)
 */
public record PortLayout(List<PxBox> body, List<Port> ports) {
    /**
     * How far from the plane of a zone a clicked point may be, in pixels: the click lands on the shape of the block,
     * computed in floating point.
     */
    public static final double FACE_TOLERANCE = 0.05;
    /** Tolerance within the plane of a zone, in pixels. */
    public static final double EDGE_TOLERANCE = 1.0e-6;

    public PortLayout {
        body = List.copyOf(body);
        ports = List.copyOf(ports);
        for (int i = 0; i < ports.size(); i++) {
            if (ports.get(i).index() != i) {
                throw new IllegalArgumentException("Port " + i + " has the index " + ports.get(i).index());
            }
        }
    }

    /**
     * A rectangle of a face, part of an aim zone.
     *
     * @param box a box flat along the axis of the side, in the plane of that face of the shape
     */
    public record Area(PortSide side, PxBox box) {
        public Area turned(int quarters) {
            return new Area(this.side.turned(quarters), this.box.turned(quarters));
        }

        /** True if a point clicked on this side lies in this rectangle. */
        public boolean contains(PortSide clicked, PxPoint point) {
            if (clicked != this.side) {
                return false;
            }
            double tx = this.side.x() != 0 ? FACE_TOLERANCE : EDGE_TOLERANCE;
            double ty = this.side.y() != 0 ? FACE_TOLERANCE : EDGE_TOLERANCE;
            double tz = this.side.z() != 0 ? FACE_TOLERANCE : EDGE_TOLERANCE;
            return point.x() >= this.box.minX() - tx && point.x() <= this.box.maxX() + tx
                    && point.y() >= this.box.minY() - ty && point.y() <= this.box.maxY() + ty
                    && point.z() >= this.box.minZ() - tz && point.z() <= this.box.maxZ() + tz;
        }
    }

    /**
     * A port.
     *
     * @param index     number of the port in the engine, starting at 0
     * @param connector its connector (RJ45, line input)
     * @param face      the face that carries it; the plug goes in along the opposite of its outward normal
     * @param zone      its aim zone: the rectangles of the faces where a click chooses this port
     * @param hole      the hole of the port (flat for a painted port, on the back of a device)
     * @param mouth     where the plug goes in: the centre of the hole in the plane of the face
     */
    public record Port(int index, Connector connector, PortSide face, List<Area> zone, PxBox hole, PxPoint mouth) {
        public Port {
            zone = List.copyOf(zone);
        }

        public Port turned(int quarters) {
            return new Port(this.index, this.connector, this.face.turned(quarters),
                    this.zone.stream().map(area -> area.turned(quarters)).toList(),
                    this.hole.turned(quarters), this.mouth.turned(quarters));
        }

        /** Outward direction of the port: a plug leaves it this way. */
        public PortSide outward() {
            return this.face;
        }

        /** Number shown to the player, starting at 1. */
        public int displayNumber() {
            return this.index + 1;
        }
    }

    /** This layout for a device turned by {@code quarters} quarter turns clockwise seen from above (0 for a front facing north). */
    public PortLayout turned(int quarters) {
        if (Math.floorMod(quarters, 4) == 0) {
            return this;
        }
        return new PortLayout(this.body.stream().map(box -> box.turned(quarters)).toList(),
                this.ports.stream().map(port -> port.turned(quarters)).toList());
    }

    /**
     * The port whose aim zone holds a point clicked on a face of the device, in the frame of this layout (turn the
     * layout, not the point); empty when the click falls outside every zone.
     */
    public Optional<Port> portAt(PortSide clicked, PxPoint point) {
        for (Port port : this.ports) {
            for (Area area : port.zone()) {
                if (area.contains(clicked, point)) {
                    return Optional.of(port);
                }
            }
        }
        return Optional.empty();
    }

    /** The kinds of the ports, in engine order, as the block declares them ({@code PORTS}). */
    public List<PortKind> kinds() {
        List<PortKind> kinds = new ArrayList<>();
        for (Port port : this.ports) {
            kinds.add(port.connector().kind());
        }
        return List.copyOf(kinds);
    }
}
