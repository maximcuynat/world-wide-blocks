package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Optional;

/**
 * Type of an Ethernet frame: it says which protocol reads its content. The values are the
 * official values of the IANA "IEEE 802 Numbers" registry (https://www.iana.org/assignments/ieee-802-numbers),
 * checked on 2026-11-17.
 */
public enum EtherType {
    /** IPv4 (IANA registry, RFC 9542), see {@link Ipv4Packet}. Used from lot 1b-2 (DHCP). */
    IPV4(0x0800),
    /** ARP, address resolution (IANA registry, RFC 9542), see {@link ArpPacket}. Used from lot 1c-1. */
    ARP(0x0806);

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
