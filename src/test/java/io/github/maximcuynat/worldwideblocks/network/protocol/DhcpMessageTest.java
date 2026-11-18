package io.github.maximcuynat.worldwideblocks.network.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

/** The layers that carry DHCP, and the fields of each message (RFC 2131, tables 3 and 5). */
class DhcpMessageTest {
    private static final MacAddress PC = new MacAddress(0x02_00_00_00_00_01L);
    private static final MacAddress BOX = new MacAddress(0x02_00_00_00_00_02L);
    private static final Ipv4Address SERVER = Ipv4Address.of(192, 168, 1, 1);
    private static final Ipv4Address MASK = Ipv4Address.of(255, 255, 255, 0);
    private static final Ipv4Address OFFERED = Ipv4Address.of(192, 168, 1, 10);
    private static final DhcpOptions LEASE = DhcpOptions.lease(SERVER, MASK, SERVER, SERVER, 1200);

    @Test
    void theCodesAreTheOfficialValues() {
        // RFC 2132, 9.6
        assertEquals(1, DhcpMessageType.DISCOVER.code());
        assertEquals(2, DhcpMessageType.OFFER.code());
        assertEquals(3, DhcpMessageType.REQUEST.code());
        assertEquals(4, DhcpMessageType.DECLINE.code());
        assertEquals(5, DhcpMessageType.ACK.code());
        assertEquals(6, DhcpMessageType.NAK.code());
        assertEquals(7, DhcpMessageType.RELEASE.code());
        assertEquals(8, DhcpMessageType.INFORM.code());
        assertEquals(Optional.of(DhcpMessageType.ACK), DhcpMessageType.byCode(5));
        assertTrue(DhcpMessageType.byCode(9).isEmpty());
        // RFC 768 in the IANA registry, RFC 2131 4.1
        assertEquals(17, IpProtocol.UDP.value());
        assertEquals(Optional.of(IpProtocol.UDP), IpProtocol.byValue(17));
        assertEquals(67, DhcpMessage.SERVER_PORT);
        assertEquals(68, DhcpMessage.CLIENT_PORT);
    }

    @Test
    void onlyOfferAckAndNakComeFromAServer() {
        for (DhcpMessageType type : DhcpMessageType.values()) {
            boolean expected = type == DhcpMessageType.OFFER || type == DhcpMessageType.ACK || type == DhcpMessageType.NAK;
            assertEquals(expected, type.fromServer(), type.name());
        }
    }

    @Test
    void aDiscoverTravelsInUdpInIpv4InABroadcastFrame() {
        DhcpMessage discover = DhcpMessage.discover(42, PC);
        Ipv4Packet packet = new Ipv4Packet(Ipv4Address.ANY, Ipv4Address.BROADCAST,
                new UdpDatagram(DhcpMessage.CLIENT_PORT, DhcpMessage.SERVER_PORT, discover));
        EthernetFrame frame = new EthernetFrame(PC, MacAddress.BROADCAST, packet);

        assertEquals(EtherType.IPV4, frame.type());
        assertEquals(IpProtocol.UDP, packet.protocol());
        assertTrue(frame.isBroadcast());
        UdpDatagram udp = (UdpDatagram) ((Ipv4Packet) frame.payload()).payload();
        assertEquals(68, udp.sourcePort());
        assertEquals(67, udp.destinationPort());
        assertEquals(discover, udp.payload());
    }

    @Test
    void theClientMessagesCarryTheFieldsOfTable5() {
        DhcpMessage discover = DhcpMessage.discover(1, PC);
        assertEquals(DhcpMessageType.DISCOVER, discover.type());
        assertEquals(DhcpOptions.NONE, discover.options());
        assertTrue(discover.clientAddress().isAny());

        // After SELECTING: server identifier and requested address, ciaddr zero.
        DhcpMessage selected = DhcpMessage.requestSelected(2, PC, SERVER, OFFERED);
        assertEquals(DhcpMessageType.REQUEST, selected.type());
        assertEquals(Optional.of(SERVER), selected.options().serverId());
        assertEquals(Optional.of(OFFERED), selected.options().requestedAddress());
        assertTrue(selected.clientAddress().isAny());

        // INIT-REBOOT: requested address, no server identifier, ciaddr zero.
        DhcpMessage previous = DhcpMessage.requestPrevious(3, PC, OFFERED);
        assertEquals(Optional.of(OFFERED), previous.options().requestedAddress());
        assertTrue(previous.options().serverId().isEmpty());
        assertTrue(previous.clientAddress().isAny());

        // RENEWING and REBINDING: ciaddr, neither server identifier nor requested address.
        DhcpMessage extension = DhcpMessage.requestExtension(4, PC, OFFERED);
        assertEquals(OFFERED, extension.clientAddress());
        assertTrue(extension.options().serverId().isEmpty());
        assertTrue(extension.options().requestedAddress().isEmpty());

        for (DhcpMessage message : new DhcpMessage[] {discover, selected, previous, extension}) {
            assertFalse(message.broadcast());
            assertEquals(PC, message.clientMac());
        }
    }

    @Test
    void theServerRepliesCopyTheTransactionAndTheClientOfTheRequest() {
        DhcpMessage discover = DhcpMessage.discover(77, PC);
        DhcpMessage offer = DhcpMessage.offer(discover, OFFERED, LEASE);

        assertEquals(DhcpMessageType.OFFER, offer.type());
        assertEquals(77, offer.xid());
        assertEquals(PC, offer.clientMac());
        assertEquals(OFFERED, offer.yourAddress());
        assertTrue(offer.clientAddress().isAny());
        assertEquals(Optional.of(SERVER), offer.options().serverId());
        assertEquals(Optional.of(MASK), offer.options().subnetMask());
        assertEquals(Optional.of(SERVER), offer.options().router());
        assertEquals(Optional.of(SERVER), offer.options().dnsServer());
        assertEquals(OptionalLong.of(1200), offer.options().leaseSeconds());

        DhcpMessage renewal = DhcpMessage.requestExtension(78, PC, OFFERED);
        DhcpMessage ack = DhcpMessage.ack(renewal, OFFERED, LEASE);
        assertEquals(78, ack.xid());
        assertEquals(OFFERED, ack.clientAddress(), "ciaddr copied from the request (table 3)");
        assertEquals(OFFERED, ack.yourAddress());

        DhcpMessage nak = DhcpMessage.nak(DhcpMessage.requestPrevious(79, PC, OFFERED), SERVER);
        assertEquals(DhcpMessageType.NAK, nak.type());
        assertEquals(79, nak.xid());
        assertTrue(nak.yourAddress().isAny());
        assertEquals(DhcpOptions.server(SERVER), nak.options());
    }

    @Test
    void aServerMessageWithoutServerIdentifierIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new DhcpMessage(DhcpMessageType.ACK, 1, false,
                Ipv4Address.ANY, OFFERED, PC, DhcpOptions.NONE));
    }

    @Test
    void aServerDoesNotReplyToAServerMessage() {
        DhcpMessage offer = DhcpMessage.offer(DhcpMessage.discover(1, PC), OFFERED, LEASE);

        assertThrows(IllegalArgumentException.class, () -> DhcpMessage.ack(offer, OFFERED, LEASE));
    }

    @Test
    void inconsistentLayersAreRefused() {
        DhcpMessage discover = DhcpMessage.discover(1, PC);

        assertThrows(IllegalArgumentException.class, () -> new DhcpMessage(DhcpMessageType.DISCOVER, 1, false,
                Ipv4Address.ANY, Ipv4Address.ANY, MacAddress.BROADCAST, DhcpOptions.NONE));
        assertThrows(IllegalArgumentException.class, () -> new UdpDatagram(68, 65536, discover));
        assertThrows(IllegalArgumentException.class, () -> new UdpDatagram(-1, 67, discover));
        assertThrows(IllegalArgumentException.class, () -> new Ipv4Packet(Ipv4Address.BROADCAST, Ipv4Address.BROADCAST,
                new UdpDatagram(68, 67, discover)));
        assertThrows(IllegalArgumentException.class, () -> DhcpOptions.lease(SERVER, MASK, SERVER, SERVER, 1L << 32));
        assertThrows(IllegalArgumentException.class, () -> new EthernetFrame(BOX, PC, EtherType.ARP,
                new Ipv4Packet(SERVER, OFFERED, new UdpDatagram(67, 68, discover))));
    }
}
