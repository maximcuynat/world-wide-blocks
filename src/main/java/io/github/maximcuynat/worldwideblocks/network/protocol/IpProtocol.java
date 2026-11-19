package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Optional;

/**
 * Protocol carried by an IPv4 packet (the "Protocol" field of its header). The values are the
 * official values of the IANA "Assigned Internet Protocol Numbers" registry
 * (https://www.iana.org/assignments/protocol-numbers).
 */
public enum IpProtocol {
    /** ICMP, RFC 792. Carries the echo of {@code ping} and the errors of lot 1c. */
    ICMP(1),
    /** UDP, RFC 768. Carries DHCP from lot 1b-2. */
    UDP(17);

    private final int value;

    IpProtocol(int value) {
        this.value = value;
    }

    /** 8-bit value written in the packet header. */
    public int value() {
        return this.value;
    }

    public static Optional<IpProtocol> byValue(int value) {
        for (IpProtocol protocol : values()) {
            if (protocol.value == value) {
                return Optional.of(protocol);
            }
        }
        return Optional.empty();
    }
}
