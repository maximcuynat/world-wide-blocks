package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Objects;

/**
 * Ethernet frame: the only unit that travels on a cable. The upper layers (IP, UDP, DHCP,
 * ARP, ICMP) travel in its content.
 *
 * <p>Immutable: a switch that relays it, on one or several ports, passes on the same object.
 *
 * @param source      MAC of the sender, always a unicast address
 * @param destination MAC of the recipient, or {@link MacAddress#BROADCAST} for all
 * @param type        type of the frame; it must be that of the content
 */
public record EthernetFrame(MacAddress source, MacAddress destination, EtherType type, EthernetPayload payload) {

    public EthernetFrame {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(payload, "payload");
        if (source.isMulticast()) {
            throw new IllegalArgumentException("A frame cannot be sent from a group address: " + source);
        }
        if (type != payload.etherType()) {
            throw new IllegalArgumentException("Type " + type + " does not match payload " + payload.etherType());
        }
    }

    /** Frame whose type is deduced from its content. */
    public EthernetFrame(MacAddress source, MacAddress destination, EthernetPayload payload) {
        this(source, destination, payload.etherType(), payload);
    }

    /** Meant for all devices of the network. */
    public boolean isBroadcast() {
        return this.destination.isBroadcast();
    }
}
