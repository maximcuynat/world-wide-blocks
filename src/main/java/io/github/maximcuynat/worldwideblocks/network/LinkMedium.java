package io.github.maximcuynat.worldwideblocks.network;

import java.util.Optional;

/**
 * Support physique d'un lien. Le cuivre Ethernet est le seul pour l'instant ; la fibre,
 * la ligne téléphonique et les autres câbles s'ajouteront ici.
 */
public enum LinkMedium {
    /** Cordon Ethernet cuivre : 100 m au plus, comme un lien réel (1 bloc = 1 m). */
    COPPER_ETHERNET("copper_ethernet", 100.0, PortKind.ETHERNET);

    private final String serializedName;
    private final double maxLength;
    private final PortKind portKind;

    LinkMedium(String serializedName, double maxLength, PortKind portKind) {
        this.serializedName = serializedName;
        this.maxLength = maxLength;
        this.portKind = portKind;
    }

    /** Nom stocké dans la sauvegarde : ne jamais le changer une fois publié. */
    public String serializedName() {
        return this.serializedName;
    }

    /** Longueur maximale du lien, en blocs. */
    public double maxLength() {
        return this.maxLength;
    }

    /** Vrai si ce câble se branche sur un port de ce type. */
    public boolean accepts(PortKind kind) {
        return this.portKind == kind;
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
