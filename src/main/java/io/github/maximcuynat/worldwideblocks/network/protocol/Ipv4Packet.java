package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Objects;

/**
 * IPv4 packet, carried by an Ethernet frame of type {@link EtherType#IPV4}. Only the fields the simulation
 * uses are kept: source, destination and protocol. The others (time to live, fragmentation, checksum)
 * come with the lots that need them, for example the time to live with routing.
 *
 * @param protocol protocol of the content; it must be that of the content
 */
public record Ipv4Packet(Ipv4Address source, Ipv4Address destination, IpProtocol protocol, Ipv4Payload payload)
        implements EthernetPayload {

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
    }

    /** Packet whose protocol is deduced from its content. */
    public Ipv4Packet(Ipv4Address source, Ipv4Address destination, Ipv4Payload payload) {
        this(source, destination, payload.protocol(), payload);
    }

    @Override
    public EtherType etherType() {
        return EtherType.IPV4;
    }
}
