package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Objects;

/**
 * UDP datagram (RFC 768), carried by an IPv4 packet: a source port, a destination port and a content.
 * The length and the checksum of the real header are not simulated.
 *
 * @param sourcePort      from 0 to 65535
 * @param destinationPort from 0 to 65535
 */
public record UdpDatagram(int sourcePort, int destinationPort, UdpPayload payload) implements Ipv4Payload {

    public UdpDatagram {
        checkPort(sourcePort);
        checkPort(destinationPort);
        Objects.requireNonNull(payload, "payload");
    }

    private static void checkPort(int port) {
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("UDP port out of range: " + port);
        }
    }

    @Override
    public IpProtocol protocol() {
        return IpProtocol.UDP;
    }
}
