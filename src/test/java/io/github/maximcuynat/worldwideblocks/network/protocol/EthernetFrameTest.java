package io.github.maximcuynat.worldwideblocks.network.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class EthernetFrameTest {
    private static final MacAddress HOST = new MacAddress(0x02_00_00_00_00_01L);
    private static final MacAddress OTHER = new MacAddress(0x02_00_00_00_00_02L);
    /** Any payload: an ARP request of the host for 192.168.1.2. */
    private static final ArpPacket ARP_REQUEST =
            ArpPacket.request(HOST, Ipv4Address.of(192, 168, 1, 1), Ipv4Address.of(192, 168, 1, 2));

    @Test
    void theTypesAreTheOfficialValues() {
        assertEquals(0x0800, EtherType.IPV4.value());
        assertEquals(0x0806, EtherType.ARP.value());
        assertEquals(Optional.of(EtherType.ARP), EtherType.byValue(0x0806));
        assertTrue(EtherType.byValue(0x1234).isEmpty());
        assertTrue(EtherType.byValue(0x88B5).isEmpty(), "the experimental type of the presence frame is gone");
    }

    @Test
    void theTypeOfAFrameIsThatOfItsPayload() {
        EthernetFrame frame = new EthernetFrame(HOST, MacAddress.BROADCAST, ARP_REQUEST);

        assertEquals(EtherType.ARP, frame.type());
        assertTrue(frame.isBroadcast());
    }

    @Test
    void aUnicastFrameIsNotBroadcast() {
        assertFalse(new EthernetFrame(HOST, OTHER, ARP_REQUEST).isBroadcast());
    }

    @Test
    void rejectsATypeInconsistentWithThePayload() {
        assertThrows(IllegalArgumentException.class,
                () -> new EthernetFrame(HOST, OTHER, EtherType.IPV4, ARP_REQUEST));
    }

    @Test
    void rejectsAGroupSource() {
        assertThrows(IllegalArgumentException.class,
                () -> new EthernetFrame(MacAddress.BROADCAST, OTHER, ARP_REQUEST));
    }
}
