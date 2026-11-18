package io.github.maximcuynat.worldwideblocks.network.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class Ipv4AddressTest {
    private static final Ipv4Address MASK_24 = Ipv4Address.of(255, 255, 255, 0);

    @Test
    void theFirstByteWrittenIsTheMostSignificant() {
        Ipv4Address box = Ipv4Address.of(192, 168, 1, 1);

        assertEquals(0xC0A80101, box.value());
        assertEquals(192, box.octet(0));
        assertEquals(1, box.octet(3));
        assertEquals("192.168.1.1", box.toString());
    }

    @Test
    void highAddressesAreWrittenWithoutSign() {
        assertEquals("255.255.255.255", Ipv4Address.BROADCAST.toString());
        assertEquals("255.255.255.0", MASK_24.toString());
        assertEquals("0.0.0.0", Ipv4Address.ANY.toString());
        assertTrue(Ipv4Address.BROADCAST.isBroadcast());
        assertTrue(Ipv4Address.ANY.isAny());
        assertFalse(MASK_24.isBroadcast());
    }

    @Test
    void parseReadsWhatToStringWrites() {
        for (Ipv4Address address : new Ipv4Address[] {Ipv4Address.ANY, Ipv4Address.BROADCAST, MASK_24,
                Ipv4Address.of(192, 168, 1, 200), Ipv4Address.of(10, 0, 0, 7)}) {
            assertEquals(Optional.of(address), Ipv4Address.parse(address.toString()));
        }
    }

    @Test
    void parseRefusesWhatIsNotAnAddress() {
        for (String text : new String[] {"", "192.168.1", "192.168.1.1.1", "192.168.1.256", "192.168.01.1",
                "192.168.1.-1", "192.168..1", "a.b.c.d", "192.168.1.1 ", "1234.1.1.1"}) {
            assertTrue(Ipv4Address.parse(text).isEmpty(), text);
        }
    }

    @Test
    void ofRefusesAByteOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> Ipv4Address.of(256, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Ipv4Address.of(0, 0, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> MASK_24.octet(4));
    }

    @Test
    void plusAndDistanceCountAddresses() {
        Ipv4Address first = Ipv4Address.of(192, 168, 1, 10);

        assertEquals(Ipv4Address.of(192, 168, 1, 200), first.plus(190));
        assertEquals(190, first.plus(190).distanceFrom(first));
        assertEquals(Ipv4Address.of(192, 168, 2, 0), Ipv4Address.of(192, 168, 1, 255).plus(1));
        assertEquals(-1, Ipv4Address.of(192, 168, 1, 9).distanceFrom(first));
    }

    @Test
    void sameNetworkComparesTheBitsOfTheMask() {
        Ipv4Address box = Ipv4Address.of(192, 168, 1, 1);

        assertTrue(Ipv4Address.of(192, 168, 1, 200).sameNetwork(box, MASK_24));
        assertFalse(Ipv4Address.of(192, 168, 2, 10).sameNetwork(box, MASK_24));
    }
}
