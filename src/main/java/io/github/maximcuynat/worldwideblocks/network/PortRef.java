package io.github.maximcuynat.worldwideblocks.network;

/**
 * Désigne un port précis d'un appareil.
 *
 * @param index numéro du port, à partir de 0 (affiché à partir de 1 au joueur)
 */
public record PortRef(DeviceId device, int index) {

    /** Numéro du port tel qu'il est montré au joueur. */
    public int displayNumber() {
        return this.index + 1;
    }
}
