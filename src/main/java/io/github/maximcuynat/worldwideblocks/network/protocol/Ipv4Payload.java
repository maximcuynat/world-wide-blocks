package io.github.maximcuynat.worldwideblocks.network.protocol;

/**
 * Content of an IPv4 packet. Each protocol carried by IP is a variant of it, which declares its
 * {@link IpProtocol}: {@link UdpDatagram} (lot 1b-2), then {@link IcmpMessage} (lot 1c).
 */
public sealed interface Ipv4Payload permits UdpDatagram, IcmpMessage {

    IpProtocol protocol();
}
