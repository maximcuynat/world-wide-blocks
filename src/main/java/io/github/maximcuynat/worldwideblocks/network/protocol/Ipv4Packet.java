package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Objects;

/**
 * IPv4 packet, carried by an Ethernet frame of type {@link EtherType#IPV4}. Only the fields the simulation
 * uses are kept: source, destination, protocol and time to live. The others (fragmentation, checksum) come
 * with the lots that need them.
 *
 * @param protocol protocol of the content; it must be that of the content
 * @param ttl      time to live (RFC 791), from 0 to 255; {@code ping} shows it on each reply
 */
public record Ipv4Packet(Ipv4Address source, Ipv4Address destination, IpProtocol protocol, int ttl, Ipv4Payload payload)
        implements EthernetPayload {
    /**
     * Time to live of the packets a machine sends: 64, the default of Linux ({@code net.ipv4.ip_default_ttl},
     * "Default: 64 (as recommended by RFC1700)", Documentation/networking/ip-sysctl.rst, read on 2026-11-19).
     */
    public static final int DEFAULT_TTL = 64;

    public Ipv4Packet {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(payload, "payload");
        if (protocol != payload.protocol()) {
            throw new IllegalArgumentException("Protocol " + protocol + " does not match payload " + payload.protocol());
        }
        if (source.isBroadcast()) {
            throw new IllegalArgumentException("A packet cannot be sent from the broadcast address");
        }
        if (ttl < 0 || ttl > 0xFF) {
            throw new IllegalArgumentException("Time to live out of range: " + ttl);
        }
    }

    /** Packet with the default time to live. */
    public Ipv4Packet(Ipv4Address source, Ipv4Address destination, IpProtocol protocol, Ipv4Payload payload) {
        this(source, destination, protocol, DEFAULT_TTL, payload);
    }

    /** Packet whose protocol is deduced from its content, with the default time to live. */
    public Ipv4Packet(Ipv4Address source, Ipv4Address destination, Ipv4Payload payload) {
        this(source, destination, payload.protocol(), DEFAULT_TTL, payload);
    }

    @Override
    public EtherType etherType() {
        return EtherType.IPV4;
    }
}
