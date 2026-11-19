package io.github.maximcuynat.worldwideblocks.network.protocol;

/**
 * Content of an Ethernet frame. Each protocol carried by Ethernet is a variant of it, which declares
 * its {@link EtherType}: {@link Ipv4Packet} (lot 1b-2), which carries UDP then DHCP, and {@link ArpPacket}
 * (lot 1c).
 *
 * <p>The contents are immutable records: a frame relayed by a switch is the same object.
 */
public sealed interface EthernetPayload permits ArpPacket, Ipv4Packet, PresencePayload {

    EtherType etherType();
}
