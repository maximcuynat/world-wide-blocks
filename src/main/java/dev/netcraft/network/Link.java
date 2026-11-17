package dev.netcraft.network;

import java.util.UUID;

/**
 * Un câble branché entre deux ports.
 *
 * @param cableConsumed vrai si un cordon a été retiré de l'inventaire au branchement. Seul un lien
 *                      qui a consommé un cordon en rend un quand on le débranche, sinon brancher en
 *                      créatif puis casser en survie fabriquerait des cordons gratuits.
 */
public record Link(UUID id, PortRef a, PortRef b, LinkMedium medium, boolean cableConsumed) {

    public boolean involves(DeviceId device) {
        return this.a.device().equals(device) || this.b.device().equals(device);
    }

    /** Extrémité opposée à {@code port}. */
    public PortRef otherEnd(PortRef port) {
        if (this.a.equals(port)) {
            return this.b;
        }
        if (this.b.equals(port)) {
            return this.a;
        }
        throw new IllegalArgumentException("Le port " + port + " n'appartient pas au lien " + this.id);
    }

    public double length() {
        return this.a.device().distanceTo(this.b.device());
    }
}
