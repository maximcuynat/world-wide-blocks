package io.github.maximcuynat.worldwideblocks.network;

import java.util.Optional;

/**
 * Behavior of a device in the simulation. It is fixed for a block type and saved
 * with the device, so the simulation runs without loading the world.
 */
public enum DeviceRole {
    /** Computer: a network card that sends and receives for itself (PC tower, laptop). */
    HOST("host"),
    /** Internet box: a switch on its local network ports, then its services (DHCP in lot 1b-2). */
    BOX("box");

    private final String serializedName;

    DeviceRole(String serializedName) {
        this.serializedName = serializedName;
    }

    /** Name stored in the save: never change it once published. */
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
