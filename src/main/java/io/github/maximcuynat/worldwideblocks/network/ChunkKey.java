package io.github.maximcuynat.worldwideblocks.network;

/**
 * Identifies a chunk (a 16 x 16 block column) of a dimension. Plain Java, like {@link DeviceId}.
 */
public record ChunkKey(String dimension, int x, int z) {

    public static ChunkKey of(DeviceId device) {
        // Arithmetic shift: rounds down, including for negative coordinates.
        return new ChunkKey(device.dimension(), device.x() >> 4, device.z() >> 4);
    }
}
