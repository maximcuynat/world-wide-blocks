package io.github.maximcuynat.worldwideblocks.port;

/**
 * A face of a device, in the frame of its block: north is -z, east +x, south +z, west -x, as in the game. Plain Java,
 * so that the port layouts are tested without the game ({@code net.minecraft.core.Direction} is mapped to it on the
 * game side).
 */
public enum PortSide {
    NORTH(0, 0, -1),
    EAST(1, 0, 0),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0);

    private static final PortSide[] RING = {NORTH, EAST, SOUTH, WEST};

    private final int x;
    private final int y;
    private final int z;

    PortSide(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Outward normal, x component. */
    public int x() {
        return this.x;
    }

    /** Outward normal, y component. */
    public int y() {
        return this.y;
    }

    /** Outward normal, z component. */
    public int z() {
        return this.z;
    }

    /** True for a face of the sides (north, east, south, west). */
    public boolean horizontal() {
        return this.y == 0;
    }

    /**
     * This face after the device is turned by {@code quarters} quarter turns clockwise seen from above, as the {@code y}
     * of a blockstate turns a model: north goes east, east goes south. Top and bottom do not move.
     */
    public PortSide turned(int quarters) {
        if (!this.horizontal()) {
            return this;
        }
        int index = switch (this) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            default -> 3;
        };
        return RING[Math.floorMod(index + quarters, 4)];
    }

    /** Quarter turns clockwise from north to this horizontal face: 0 north, 1 east, 2 south, 3 west. */
    public int quartersFromNorth() {
        if (!this.horizontal()) {
            throw new IllegalStateException("Not a horizontal face: " + this);
        }
        return switch (this) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            default -> 3;
        };
    }
}
