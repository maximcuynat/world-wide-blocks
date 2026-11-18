package io.github.maximcuynat.worldwideblocks.network;

import java.util.Optional;

/**
 * Behavior of a device in the simulation. It is fixed for a block type and saved
 * with the device, so the simulation runs without loading the world.
 */
public enum DeviceRole {
    /** Computer: a network card that sends and receives for itself (PC tower, laptop). */
    HOST("host"),
    /** Internet box: a switch on its local network ports, and its own services on the local point (DHCP server). */
    BOX("box");

    private final String serializedName;

    DeviceRole(String serializedName) {
        this.serializedName = serializedName;
    }

    /** Name stored in the save: never change it once published. */
    public String serializedName() {
        return this.serializedName;
    }

    /**
     * True for a device with its own service on the network (the DHCP server of the box): it has, besides the
     * MAC of each port, a MAC for this service.
     */
    public boolean hasService() {
        return this == BOX;
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
