package io.github.maximcuynat.worldwideblocks.network.protocol;

/**
 * Contenu d'une trame Ethernet. Chaque protocole porté par Ethernet en est une variante, qui déclare
 * son {@link EtherType}. Les couches suivantes s'ajoutent ici : {@code Ipv4Packet} (lot 1b-2), qui
 * portera UDP puis DHCP, et {@code ArpPacket} (lot 1c).
 *
 * <p>Les contenus sont des records immuables : une trame relayée par un switch est le même objet.
 */
public sealed interface EthernetPayload permits PresencePayload {

    EtherType etherType();
}
