package io.github.maximcuynat.worldwideblocks.network.dhcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpOptions;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Packet;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.protocol.PresencePayload;
import io.github.maximcuynat.worldwideblocks.network.protocol.UdpDatagram;

/** Addresses of the frames that carry DHCP (RFC 2131, 4.1). */
class DhcpFramesTest {
    private static final MacAddress PC = new MacAddress(0x02_00_00_00_00_01L);
    private static final MacAddress SERVICE = new MacAddress(0x02_00_00_00_00_02L);
    private static final Ipv4Address SERVER = Ipv4Address.of(192, 168, 1, 1);
    private static final Ipv4Address ADDRESS = Ipv4Address.of(192, 168, 1, 10);
    private static final DhcpOptions LEASE = DhcpOptions.lease(SERVER, Ipv4Address.of(255, 255, 255, 0), SERVER, SERVER, 1200);

    private static Ipv4Packet packet(EthernetFrame frame) {
        return (Ipv4Packet) frame.payload();
    }

    @Test
    void anOfferGoesToTheHardwareAddressOfTheClientAndToItsNewAddress() {
        EthernetFrame frame = DhcpFrames.fromServer(SERVICE, SERVER, DhcpMessage.offer(DhcpMessage.discover(1, PC), ADDRESS, LEASE));

        assertEquals(SERVICE, frame.source());
        assertEquals(PC, frame.destination());
        assertEquals(SERVER, packet(frame).source());
        assertEquals(ADDRESS, packet(frame).destination());
        UdpDatagram udp = (UdpDatagram) packet(frame).payload();
        assertEquals(67, udp.sourcePort());
        assertEquals(68, udp.destinationPort());
    }

    @Test
    void anAckOfARenewalGoesToCiaddr() {
        EthernetFrame frame = DhcpFrames.fromServer(SERVICE, SERVER,
                DhcpMessage.ack(DhcpMessage.requestExtension(1, PC, ADDRESS), ADDRESS, LEASE));

        assertEquals(PC, frame.destination());
        assertEquals(ADDRESS, packet(frame).destination());
    }

    @Test
    void aNakIsBroadcast() {
        EthernetFrame frame = DhcpFrames.fromServer(SERVICE, SERVER, DhcpMessage.nak(DhcpMessage.requestPrevious(1, PC, ADDRESS), SERVER));

        assertTrue(frame.isBroadcast());
        assertEquals(Ipv4Address.BROADCAST, packet(frame).destination());
    }

    @Test
    void eachSideOnlyReadsTheMessagesOfTheOther() {
        DhcpMessage discover = DhcpMessage.discover(1, PC);
        EthernetFrame fromClient = DhcpFrames.fromClient(PC, Ipv4Address.ANY, MacAddress.BROADCAST, Ipv4Address.BROADCAST, discover);
        EthernetFrame fromServer = DhcpFrames.fromServer(SERVICE, SERVER, DhcpMessage.offer(discover, ADDRESS, LEASE));

        assertEquals(Optional.of(discover), DhcpFrames.toServer(fromClient));
        assertTrue(DhcpFrames.toClient(fromClient).isEmpty());
        assertTrue(DhcpFrames.toServer(fromServer).isEmpty());
        assertEquals(DhcpMessage.offer(discover, ADDRESS, LEASE), DhcpFrames.toClient(fromServer).orElseThrow());
        assertTrue(DhcpFrames.toServer(new EthernetFrame(PC, MacAddress.BROADCAST, PresencePayload.INSTANCE)).isEmpty());
        assertTrue(DhcpFrames.toServer(new EthernetFrame(PC, MacAddress.BROADCAST, new Ipv4Packet(Ipv4Address.ANY,
                Ipv4Address.BROADCAST, new UdpDatagram(68, 1234, discover)))).isEmpty(), "other UDP port");
    }
}
