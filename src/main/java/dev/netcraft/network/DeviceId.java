package dev.netcraft.network;

import java.util.Comparator;

/**
 * Identifie un appareil réseau par sa position : dimension et coordonnées du bloc.
 * Un appareil ne se déplace jamais (il refuse les pistons), sa position suffit donc à l'identifier.
 *
 * <p>Java pur, sans classe Minecraft : le moteur reste testable sans lancer le jeu.
 *
 * @param dimension identifiant de la dimension, par exemple {@code minecraft:overworld}
 */
public record DeviceId(String dimension, int x, int y, int z) {
    /** Ordre total et stable : dimension, puis x, y, z. Sert à désigner un réseau par un de ses appareils. */
    public static final Comparator<DeviceId> ORDER = Comparator.comparing(DeviceId::dimension)
            .thenComparingInt(DeviceId::x)
            .thenComparingInt(DeviceId::y)
            .thenComparingInt(DeviceId::z);

    public boolean sameDimension(DeviceId other) {
        return this.dimension.equals(other.dimension);
    }

    /** Distance en ligne droite entre les centres des deux blocs, en blocs. */
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
