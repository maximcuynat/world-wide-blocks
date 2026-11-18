package io.github.maximcuynat.worldwideblocks.network.protocol;

/**
 * Content of a UDP datagram. UDP does not say what it carries: the receiver knows it from the
 * destination port (67 and 68 for DHCP). Each application protocol carried by UDP is a variant of it.
 */
public sealed interface UdpPayload permits DhcpMessage {
}
