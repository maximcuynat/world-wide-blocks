package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Optional;

/**
 * Type of an Ethernet frame: it says which protocol reads its content. The values are the
 * official values of the IANA "IEEE 802 Numbers" registry (https://www.iana.org/assignments/ieee-802-numbers),
 * checked on 2026-11-17.
 */
public enum EtherType {
    /** IPv4 (IANA registry, RFC 9542). Used from lot 1b-2 (DHCP). */
    IPV4(0x0800),
    /** ARP, address resolution (IANA registry, RFC 9542). Used in lot 1c. */
    ARP(0x0806),
    /**
     * "IEEE Std 802 Local Experimental", reserved for experiments. It is only used by the presence frame,
     * scaffolding of lot 1b-1 to remove in lot 1c (see {@link PresencePayload}).
     */
    LOCAL_EXPERIMENTAL(0x88B5);

    private final int value;

    EtherType(int value) {
        this.value = value;
    }

    /** 16-bit value written in the frame header. */
    public int value() {
        return this.value;
    }

    public static Optional<EtherType> byValue(int value) {
        for (EtherType type : values()) {
            if (type.value == value) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
