package io.github.maximcuynat.worldwideblocks.network;

import java.util.UUID;

/**
 * A cable connected between two ports.
 *
 * @param cableConsumed true if a cord was taken from the inventory when connecting. Only a link
 *                      that consumed a cord gives one back when it is unplugged, otherwise connecting in
 *                      creative then breaking in survival would make free cords.
 */
public record Link(UUID id, PortRef a, PortRef b, LinkMedium medium, boolean cableConsumed) {

    public boolean involves(DeviceId device) {
        return this.a.device().equals(device) || this.b.device().equals(device);
    }

    /** End opposite to {@code port}. */
    public PortRef otherEnd(PortRef port) {
        if (this.a.equals(port)) {
            return this.b;
        }
        if (this.b.equals(port)) {
            return this.a;
        }
        throw new IllegalArgumentException("Port " + port + " does not belong to link " + this.id);
    }

    public double length() {
        return this.a.device().distanceTo(this.b.device());
    }
}
