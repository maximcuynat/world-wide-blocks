package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Optional;

/**
 * Type of a DHCP message, carried by option 53 (RFC 2132, 9.6, values checked on 2026-11-18). The
 * simulation uses DISCOVER, OFFER, REQUEST, ACK and NAK; DECLINE, RELEASE and INFORM are listed for
 * completeness and are not sent yet.
 */
public enum DhcpMessageType {
    DISCOVER(1, false),
    OFFER(2, true),
    REQUEST(3, false),
    DECLINE(4, false),
    ACK(5, true),
    NAK(6, true),
    RELEASE(7, false),
    INFORM(8, false);

    private final int code;
    private final boolean fromServer;

    DhcpMessageType(int code, boolean fromServer) {
        this.code = code;
        this.fromServer = fromServer;
    }

    /** Value of option 53. */
    public int code() {
        return this.code;
    }

    /**
     * True for a message sent by a server ("op" field BOOTREPLY), false for a message sent by a client
     * (BOOTREQUEST): RFC 2131, table 1 and section 3.
     */
    public boolean fromServer() {
        return this.fromServer;
    }

    public static Optional<DhcpMessageType> byCode(int code) {
        for (DhcpMessageType type : values()) {
            if (type.code == code) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
