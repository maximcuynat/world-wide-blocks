package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Objects;

/**
 * Trame Ethernet : la seule unité qui circule sur un câble. Les couches supérieures (IP, UDP, DHCP,
 * ARP, ICMP) voyagent dans son contenu.
 *
 * <p>Immuable : un switch qui la relaie, sur un ou plusieurs ports, transmet le même objet.
 *
 * @param source      MAC de l'émetteur, toujours une adresse unicast
 * @param destination MAC du destinataire, ou {@link MacAddress#BROADCAST} pour tous
 * @param type        type de la trame ; il doit être celui du contenu
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

    /** Trame dont le type est déduit de son contenu. */
    public EthernetFrame(MacAddress source, MacAddress destination, EthernetPayload payload) {
        this(source, destination, payload.etherType(), payload);
    }

    /** Destinée à tous les appareils du réseau. */
    public boolean isBroadcast() {
        return this.destination.isBroadcast();
    }
}
