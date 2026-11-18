package io.github.maximcuynat.worldwideblocks.network;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Type d'un port : il décide quels câbles s'y branchent. Le type d'un port est fixe pour un type
 * d'appareil et sauvegardé avec lui.
 */
public enum PortKind {
    /** Prise RJ45 : reçoit le cordon Ethernet. */
    ETHERNET("ethernet"),
    /** Arrivée de ligne de la box. Aucun câble ne s'y branche avant le lot 3. */
    LINE("line");

    private final String serializedName;

    PortKind(String serializedName) {
        this.serializedName = serializedName;
    }

    /** Nom stocké dans la sauvegarde : ne jamais le changer une fois publié. */
    public String serializedName() {
        return this.serializedName;
    }

    public static Optional<PortKind> bySerializedName(String name) {
        for (PortKind kind : values()) {
            if (kind.serializedName.equals(name)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    /** {@code count} ports Ethernet : la configuration de tous les appareils sauvegardés avant le lot 1b. */
    public static List<PortKind> ethernet(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative port count: " + count);
        }
        return Collections.nCopies(count, ETHERNET);
    }
}
