package io.github.maximcuynat.worldwideblocks.network;

import java.util.Optional;

/**
 * Physical medium of a link. Ethernet copper is the only one for now; fiber,
 * the phone line and the other cables will be added here.
 */
public enum LinkMedium {
    /** Copper Ethernet cord: 100 m at most, like a real link (1 block = 1 m). */
    COPPER_ETHERNET("copper_ethernet", 100.0, PortKind.ETHERNET);

    private final String serializedName;
    private final double maxLength;
    private final PortKind portKind;

    LinkMedium(String serializedName, double maxLength, PortKind portKind) {
        this.serializedName = serializedName;
        this.maxLength = maxLength;
        this.portKind = portKind;
    }

    /** Name stored in the save: never change it once published. */
    public String serializedName() {
        return this.serializedName;
    }

    /** Maximum length of the link, in blocks. */
    public double maxLength() {
        return this.maxLength;
    }

    /** True if this cable plugs into a port of this kind. */
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
