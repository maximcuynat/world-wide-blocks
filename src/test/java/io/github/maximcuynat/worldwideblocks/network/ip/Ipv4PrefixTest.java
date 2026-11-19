package io.github.maximcuynat.worldwideblocks.network.ip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;

class Ipv4PrefixTest {

    @Test
    void theNetworkOfAnAddressClearsItsHostBits() {
        Ipv4Prefix prefix = Ipv4Prefix.of(Ipv4Address.of(192, 168, 1, 10), 24);

        assertEquals(Ipv4Address.of(192, 168, 1, 0), prefix.network());
        assertEquals("192.168.1.0/24", prefix.toString());
        assertEquals(Ipv4Address.of(255, 255, 255, 0), prefix.mask());
        assertEquals(Ipv4Address.of(192, 168, 1, 255), prefix.broadcast());
    }

    @Test
    void aPrefixHoldsTheAddressesOfItsBlockOnly() {
        Ipv4Prefix prefix = Ipv4Prefix.of(Ipv4Address.of(192, 168, 1, 0), 24);

        assertTrue(prefix.contains(Ipv4Address.of(192, 168, 1, 1)));
        assertTrue(prefix.contains(Ipv4Address.of(192, 168, 1, 255)));
        assertFalse(prefix.contains(Ipv4Address.of(192, 168, 2, 1)));
        assertTrue(Ipv4Prefix.DEFAULT.contains(Ipv4Address.of(8, 8, 8, 8)));
    }

    @Test
    void theLengthOfAMaskCountsItsOnes() {
        assertEquals(24, Ipv4Prefix.lengthOf(Ipv4Address.of(255, 255, 255, 0)));
        assertEquals(0, Ipv4Prefix.lengthOf(Ipv4Address.ANY));
        assertEquals(32, Ipv4Prefix.lengthOf(Ipv4Address.BROADCAST));
        assertThrows(IllegalArgumentException.class, () -> Ipv4Prefix.lengthOf(Ipv4Address.of(255, 0, 255, 0)));
    }

    @Test
    void aPrefixWithHostBitsIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Ipv4Prefix(Ipv4Address.of(192, 168, 1, 10), 24));
        assertThrows(IllegalArgumentException.class, () -> Ipv4Prefix.of(Ipv4Address.ANY, 33));
    }
}
