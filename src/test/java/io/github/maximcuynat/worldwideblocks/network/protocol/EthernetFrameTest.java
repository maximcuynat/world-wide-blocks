package io.github.maximcuynat.worldwideblocks.network.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class EthernetFrameTest {
    private static final MacAddress POSTE = new MacAddress(0x02_00_00_00_00_01L);
    private static final MacAddress AUTRE = new MacAddress(0x02_00_00_00_00_02L);

    @Test
    void lesTypesSontLesValeursOfficielles() {
        assertEquals(0x0800, EtherType.IPV4.value());
        assertEquals(0x0806, EtherType.ARP.value());
        assertEquals(0x88B5, EtherType.LOCAL_EXPERIMENTAL.value());
        assertEquals(Optional.of(EtherType.ARP), EtherType.byValue(0x0806));
        assertTrue(EtherType.byValue(0x1234).isEmpty());
    }

    @Test
    void laTrameDePresenceUtiliseLeTypeExperimental() {
        EthernetFrame frame = new EthernetFrame(POSTE, MacAddress.BROADCAST, PresencePayload.INSTANCE);

        assertEquals(EtherType.LOCAL_EXPERIMENTAL, frame.type());
        assertTrue(frame.isBroadcast());
    }

    @Test
    void uneTrameUnicastNEstPasDiffusee() {
        assertFalse(new EthernetFrame(POSTE, AUTRE, PresencePayload.INSTANCE).isBroadcast());
    }

    @Test
    void refuseUnTypeIncoherentAvecLeContenu() {
        assertThrows(IllegalArgumentException.class,
                () -> new EthernetFrame(POSTE, AUTRE, EtherType.ARP, PresencePayload.INSTANCE));
    }

    @Test
    void refuseUneSourceDeGroupe() {
        assertThrows(IllegalArgumentException.class,
                () -> new EthernetFrame(MacAddress.BROADCAST, AUTRE, PresencePayload.INSTANCE));
    }
}
