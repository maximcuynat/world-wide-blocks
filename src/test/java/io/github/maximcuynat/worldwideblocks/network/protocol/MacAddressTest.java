package io.github.maximcuynat.worldwideblocks.network.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

class MacAddressTest {

    @Test
    void sEcritEnSixOctetsHexadecimauxMinuscules() {
        MacAddress mac = new MacAddress(0x02_4F_A0_00_1B_7CL);

        assertEquals("02:4f:a0:00:1b:7c", mac.toString());
        assertEquals(0x02, mac.octet(0));
        assertEquals(0x7C, mac.octet(5));
    }

    @Test
    void seRelitDepuisSonTexte() {
        MacAddress mac = new MacAddress(0x02_4F_A0_00_1B_7CL);

        assertEquals(Optional.of(mac), MacAddress.parse(mac.toString()));
        assertEquals(Optional.of(mac), MacAddress.parse("02:4F:A0:00:1B:7C"));
    }

    @Test
    void refuseUnTexteMalForme() {
        assertTrue(MacAddress.parse("02:4f:a0:00:1b").isEmpty());
        assertTrue(MacAddress.parse("02:4f:a0:00:1b:7c:00").isEmpty());
        assertTrue(MacAddress.parse("02:4f:a0:00:1b:7g").isEmpty());
        assertTrue(MacAddress.parse("2:4f:a0:00:1b:7c").isEmpty());
        assertTrue(MacAddress.parse("").isEmpty());
    }

    @Test
    void refuseUneValeurDePlusDe48Bits() {
        assertThrows(IllegalArgumentException.class, () -> new MacAddress(1L << 48));
        assertThrows(IllegalArgumentException.class, () -> new MacAddress(-1L));
    }

    @Test
    void laDiffusionEstUneAdresseDeGroupe() {
        assertEquals("ff:ff:ff:ff:ff:ff", MacAddress.BROADCAST.toString());
        assertTrue(MacAddress.BROADCAST.isBroadcast());
        assertTrue(MacAddress.BROADCAST.isMulticast());
    }

    @Test
    void uneAdresseTireeAuHasardCommencePar02() {
        SplittableRandom random = new SplittableRandom(1);
        for (int i = 0; i < 1000; i++) {
            MacAddress mac = MacAddress.randomLocal(random);

            assertEquals(0x02, mac.octet(0));
            assertTrue(mac.isLocallyAdministered());
            assertFalse(mac.isMulticast());
            assertFalse(mac.isBroadcast());
        }
    }
}
