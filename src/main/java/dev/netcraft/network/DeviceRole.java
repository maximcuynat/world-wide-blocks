package dev.netcraft.network;

import java.util.Optional;

/**
 * Comportement d'un appareil dans la simulation. Il est fixe pour un type de bloc et sauvegardé
 * avec l'appareil, pour que la simulation tourne sans charger le monde.
 */
public enum DeviceRole {
    /** Poste : une carte réseau qui émet et reçoit pour lui-même (tour PC, portable). */
    HOST("host"),
    /** Box internet : un switch sur ses ports réseau local, puis ses services (DHCP au lot 1b-2). */
    BOX("box");

    private final String serializedName;

    DeviceRole(String serializedName) {
        this.serializedName = serializedName;
    }

    /** Nom stocké dans la sauvegarde : ne jamais le changer une fois publié. */
    public String serializedName() {
        return this.serializedName;
    }

    public static Optional<DeviceRole> bySerializedName(String name) {
        for (DeviceRole role : values()) {
            if (role.serializedName.equals(name)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
}
