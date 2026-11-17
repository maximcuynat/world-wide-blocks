package dev.netcraft.network;

/**
 * Désigne un chunk (colonne de 16 x 16 blocs) d'une dimension. Java pur, comme {@link DeviceId}.
 */
public record ChunkKey(String dimension, int x, int z) {

    public static ChunkKey of(DeviceId device) {
        // Décalage arithmétique : arrondit vers le bas, y compris pour les coordonnées négatives.
        return new ChunkKey(device.dimension(), device.x() >> 4, device.z() >> 4);
    }
}
