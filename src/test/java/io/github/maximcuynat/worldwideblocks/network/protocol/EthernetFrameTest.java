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

    @Test
    void theTypesAreTheOfficialValues() {
        assertEquals(0x0800, EtherType.IPV4.value());
        assertEquals(0x0806, EtherType.ARP.value());
        assertEquals(0x88B5, EtherType.LOCAL_EXPERIMENTAL.value());
        assertEquals(Optional.of(EtherType.ARP), EtherType.byValue(0x0806));
        assertTrue(EtherType.byValue(0x1234).isEmpty());
    }

    @Test
    void thePresenceFrameUsesTheExperimentalType() {
        EthernetFrame frame = new EthernetFrame(HOST, MacAddress.BROADCAST, PresencePayload.INSTANCE);

        assertEquals(EtherType.LOCAL_EXPERIMENTAL, frame.type());
        assertTrue(frame.isBroadcast());
    }

    @Test
    void aUnicastFrameIsNotBroadcast() {
        assertFalse(new EthernetFrame(HOST, OTHER, PresencePayload.INSTANCE).isBroadcast());
    }

    @Test
    void rejectsATypeInconsistentWithThePayload() {
        assertThrows(IllegalArgumentException.class,
                () -> new EthernetFrame(HOST, OTHER, EtherType.ARP, PresencePayload.INSTANCE));
    }

    @Test
    void rejectsAGroupSource() {
        assertThrows(IllegalArgumentException.class,
                () -> new EthernetFrame(MacAddress.BROADCAST, OTHER, PresencePayload.INSTANCE));
    }
}
