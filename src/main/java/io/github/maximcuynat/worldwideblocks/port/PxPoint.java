package io.github.maximcuynat.worldwideblocks.port;

/** A point of a block, in model pixels (0 to 16 on each axis), as in the models and the device sheets. */
public record PxPoint(double x, double y, double z) {
    /** Size of a block in model pixels. */
    public static final double BLOCK = 16.0;

    /** This point after the device is turned by {@code quarters} quarter turns clockwise seen from above ({@link PortSide#turned}). */
    public PxPoint turned(int quarters) {
        double px = this.x;
        double pz = this.z;
        for (int i = 0; i < Math.floorMod(quarters, 4); i++) {
            double next = BLOCK - pz;
            pz = px;
            px = next;
        }
        return new PxPoint(px, this.y, pz);
    }

    /** This point moved by {@code distance} pixels along the outward normal of {@code side}. */
    public PxPoint moved(PortSide side, double distance) {
        return new PxPoint(this.x + side.x() * distance, this.y + side.y() * distance, this.z + side.z() * distance);
    }
}
