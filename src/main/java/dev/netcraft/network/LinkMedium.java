package dev.netcraft.network;

import java.util.Optional;

/**
 * Support physique d'un lien. Le cuivre Ethernet est le seul pour l'instant ; la fibre,
 * la ligne téléphonique et les autres câbles s'ajouteront ici.
 */
public enum LinkMedium {
    /** Cordon Ethernet cuivre : 100 m au plus, comme un lien réel (1 bloc = 1 m). */
    COPPER_ETHERNET("copper_ethernet", 100.0);

    private final String serializedName;
    private final double maxLength;

    LinkMedium(String serializedName, double maxLength) {
        this.serializedName = serializedName;
        this.maxLength = maxLength;
    }

    /** Nom stocké dans la sauvegarde : ne jamais le changer une fois publié. */
    public String serializedName() {
        return this.serializedName;
    }

    /** Longueur maximale du lien, en blocs. */
    public double maxLength() {
        return this.maxLength;
    }

    public static Optional<LinkMedium> bySerializedName(String name) {
        for (LinkMedium medium : values()) {
            if (medium.serializedName.equals(name)) {
                return Optional.of(medium);
            }
        }
        return Optional.empty();
    }
}
