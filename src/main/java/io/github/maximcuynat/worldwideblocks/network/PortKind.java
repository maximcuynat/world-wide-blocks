package io.github.maximcuynat.worldwideblocks.network;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Kind of a port: it decides which cables plug into it. A port's kind is fixed for a device
 * type and saved with it.
 */
public enum PortKind {
    /** RJ45 jack: takes the Ethernet cord. */
    ETHERNET("ethernet"),
    /** Line input of the box. No cable plugs into it before lot 3. */
    LINE("line");

    private final String serializedName;

    PortKind(String serializedName) {
        this.serializedName = serializedName;
    }

    /** Name stored in the save: never change it once published. */
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

    /** {@code count} Ethernet ports: the configuration of all devices saved before lot 1b. */
    public static List<PortKind> ethernet(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative port count: " + count);
        }
        return Collections.nCopies(count, ETHERNET);
    }
}
