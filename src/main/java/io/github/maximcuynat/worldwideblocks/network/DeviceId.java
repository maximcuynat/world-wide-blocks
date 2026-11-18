package io.github.maximcuynat.worldwideblocks.network;

import java.util.Comparator;

/**
 * Identifies a network device by its position: dimension and block coordinates.
 * A device never moves (it refuses pistons), so its position is enough to identify it.
 *
 * <p>Plain Java, no Minecraft class: the engine stays testable without launching the game.
 *
 * @param dimension identifier of the dimension, for example {@code minecraft:overworld}
 */
public record DeviceId(String dimension, int x, int y, int z) {
    /** Total, stable order: dimension, then x, y, z. Used to designate a network by one of its devices. */
    public static final Comparator<DeviceId> ORDER = Comparator.comparing(DeviceId::dimension)
            .thenComparingInt(DeviceId::x)
            .thenComparingInt(DeviceId::y)
            .thenComparingInt(DeviceId::z);

    public boolean sameDimension(DeviceId other) {
        return this.dimension.equals(other.dimension);
    }

    /** Straight-line distance between the centers of the two blocks, in blocks. */
    public double distanceTo(DeviceId other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public String toString() {
        return this.dimension + " " + this.x + " " + this.y + " " + this.z;
    }
}
