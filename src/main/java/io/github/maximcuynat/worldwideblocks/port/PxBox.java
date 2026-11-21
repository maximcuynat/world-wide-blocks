package io.github.maximcuynat.worldwideblocks.port;

/**
 * An axis-aligned box of a block, in model pixels. A box with a zero thickness on one axis is a rectangle on a face
 * (an aim zone, a painted port).
 */
public record PxBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public PxBox {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Inverted box: " + minX + ", " + minY + ", " + minZ + " to " + maxX + ", " + maxY + ", " + maxZ);
        }
    }

    /** This box after the device is turned by {@code quarters} quarter turns clockwise seen from above ({@link PortSide#turned}). */
    public PxBox turned(int quarters) {
        PxPoint a = new PxPoint(this.minX, this.minY, this.minZ).turned(quarters);
        PxPoint b = new PxPoint(this.maxX, this.maxY, this.maxZ).turned(quarters);
        return new PxBox(Math.min(a.x(), b.x()), this.minY, Math.min(a.z(), b.z()), Math.max(a.x(), b.x()), this.maxY, Math.max(a.z(), b.z()));
    }

    public PxPoint centre() {
        return new PxPoint((this.minX + this.maxX) / 2, (this.minY + this.maxY) / 2, (this.minZ + this.maxZ) / 2);
    }

    /** True if the point is inside the box, or within {@code tolerance} pixels of it on each axis. */
    public boolean contains(PxPoint point, double tolerance) {
        return point.x() >= this.minX - tolerance && point.x() <= this.maxX + tolerance
                && point.y() >= this.minY - tolerance && point.y() <= this.maxY + tolerance
                && point.z() >= this.minZ - tolerance && point.z() <= this.maxZ + tolerance;
    }

    /** True if the other box is inside this one, within {@code tolerance} pixels. */
    public boolean contains(PxBox other, double tolerance) {
        return other.minX >= this.minX - tolerance && other.maxX <= this.maxX + tolerance
                && other.minY >= this.minY - tolerance && other.maxY <= this.maxY + tolerance
                && other.minZ >= this.minZ - tolerance && other.maxZ <= this.maxZ + tolerance;
    }

    /** True if the two boxes share some volume, or some area for flat boxes in the same plane (touching edges do not count). */
    public boolean overlaps(PxBox other, double tolerance) {
        return overlap(this.minX, this.maxX, other.minX, other.maxX, tolerance)
                && overlap(this.minY, this.maxY, other.minY, other.maxY, tolerance)
                && overlap(this.minZ, this.maxZ, other.minZ, other.maxZ, tolerance);
    }

    /** Two intervals overlap by more than {@code tolerance}; two zero-length intervals at the same place overlap. */
    private static boolean overlap(double min1, double max1, double min2, double max2, double tolerance) {
        if (max1 - min1 <= tolerance || max2 - min2 <= tolerance) {
            return min1 <= max2 + tolerance && min2 <= max1 + tolerance;
        }
        return Math.min(max1, max2) - Math.max(min1, min2) > tolerance;
    }
}
