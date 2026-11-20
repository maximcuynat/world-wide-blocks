package io.github.maximcuynat.worldwideblocks.network.dhcp;

import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessageType;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Packet;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.protocol.UdpDatagram;

/** Puts DHCP messages into frames and takes them out, with the addresses of RFC 2131, 4.1. */
public final class DhcpFrames {

    private DhcpFrames() {
    }

    /** The client message carried by this frame, if it is one: UDP to port 67. */
    public static Optional<DhcpMessage> toServer(EthernetFrame frame) {
        return dhcp(frame, DhcpMessage.SERVER_PORT).filter(message -> !message.type().fromServer());
    }

    /** The server message carried by this frame, if it is one: UDP to port 68. */
    public static Optional<DhcpMessage> toClient(EthernetFrame frame) {
        return dhcp(frame, DhcpMessage.CLIENT_PORT).filter(message -> message.type().fromServer());
    }

    /**
     * Frame of a client, from port 68 to port 67.
     *
     * @param source      the client's address, {@link Ipv4Address#ANY} before it has one (RFC 2131, 4.1)
     * @param destination {@link Ipv4Address#BROADCAST} with {@link MacAddress#BROADCAST}, or the server
     */
    public static EthernetFrame fromClient(MacAddress clientMac, Ipv4Address source, MacAddress destinationMac,
            Ipv4Address destination, DhcpMessage message) {
        return new EthernetFrame(clientMac, destinationMac, packetFromClient(source, destination, message));
    }

    /** The IPv4 packet of a client message, from port 68 to port 67. */
    public static Ipv4Packet packetFromClient(Ipv4Address source, Ipv4Address destination, DhcpMessage message) {
        return new Ipv4Packet(source, destination, new UdpDatagram(DhcpMessage.CLIENT_PORT, DhcpMessage.SERVER_PORT, message));
    }

    /**
     * Frame of a server, from port 67 to port 68, addressed as RFC 2131 says (4.1, without relay agent):
     * a NAK is broadcast; otherwise to {@code ciaddr} if the client has filled it in; otherwise broadcast if
     * the client asked for it; otherwise to the client's hardware address and {@code yiaddr}. There is no ARP
     * before lot 1c: the hardware address of a unicast reply is always that of the client ({@code chaddr}).
     */
    public static EthernetFrame fromServer(MacAddress serverMac, Ipv4Address serverAddress, DhcpMessage reply) {
        MacAddress destinationMac;
        Ipv4Address destination;
        if (reply.type() == DhcpMessageType.NAK || reply.broadcast()) {
            destinationMac = MacAddress.BROADCAST;
            destination = Ipv4Address.BROADCAST;
        } else if (!reply.clientAddress().isAny()) {
            destinationMac = reply.clientMac();
            destination = reply.clientAddress();
        } else {
            destinationMac = reply.clientMac();
            destination = reply.yourAddress();
        }
        return new EthernetFrame(serverMac, destinationMac, new Ipv4Packet(serverAddress, destination,
                new UdpDatagram(DhcpMessage.SERVER_PORT, DhcpMessage.CLIENT_PORT, reply)));
    }

    private static Optional<DhcpMessage> dhcp(EthernetFrame frame, int port) {
        if (frame.payload() instanceof Ipv4Packet packet && packet.payload() instanceof UdpDatagram udp
                && udp.destinationPort() == port && udp.payload() instanceof DhcpMessage message) {
            return Optional.of(message);
        }
        return Optional.empty();
    }
}
