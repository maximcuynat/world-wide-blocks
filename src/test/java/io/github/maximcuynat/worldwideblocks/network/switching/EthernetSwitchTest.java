package io.github.maximcuynat.worldwideblocks.network.switching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.protocol.ArpPacket;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

class EthernetSwitchTest {
    private static final long DELAY = 100;
    private static final MacAddress A = new MacAddress(0x02_00_00_00_00_0AL);
    private static final MacAddress B = new MacAddress(0x02_00_00_00_00_0BL);
    private static final MacAddress C = new MacAddress(0x02_00_00_00_00_0CL);

    /** The 4 local network ports of the box, without a local port. */
    private final EthernetSwitch box = new EthernetSwitch(List.of(0, 1, 2, 3), false, DELAY);

    /** A frame of this source: an ARP request, whatever its content (the switch reads only the addresses). */
    private static EthernetFrame frame(MacAddress source, MacAddress destination) {
        return new EthernetFrame(source, destination,
                ArpPacket.request(source, Ipv4Address.of(192, 168, 1, 10), Ipv4Address.of(192, 168, 1, 20)));
    }

    @Test
    void floodsAFrameAddressedToEveryoneOnAllTheOtherPorts() {
        assertEquals(List.of(0, 2, 3), box.receive(1, frame(A, MacAddress.BROADCAST), 0));
    }

    @Test
    void floodsAFrameWhoseDestinationIsUnknown() {
        assertEquals(List.of(1, 2, 3), box.receive(0, frame(A, B), 0));
    }

    @Test
    void relaysToTheLearnedPort() {
        box.receive(2, frame(B, MacAddress.BROADCAST), 0);

        assertEquals(List.of(2), box.receive(0, frame(A, B), 1));
    }

    @Test
    void learnsTheSourceOfEachReceivedFrame() {
        box.receive(3, frame(C, MacAddress.BROADCAST), 0);

        assertEquals(List.of(new MacTable.Entry(C, 3, 0)), box.entries());
    }

    @Test
    void filtersAFrameWhoseDestinationIsOnTheSameSide() {
        // A and B behind the same port (another switch between them): nothing to relay.
        box.receive(1, frame(B, MacAddress.BROADCAST), 0);

        assertEquals(List.of(), box.receive(1, frame(A, B), 1));
    }

    @Test
    void floodsAgainWhenTheEntryHasExpired() {
        box.receive(2, frame(B, MacAddress.BROADCAST), 0);

        assertEquals(List.of(1, 2, 3), box.receive(0, frame(A, B), DELAY));
    }

    @Test
    void floodsAgainWhenTheCableOfThePortIsRemoved() {
        box.receive(2, frame(B, MacAddress.BROADCAST), 0);

        box.linkDown(2);

        assertEquals(List.of(1, 2, 3), box.receive(0, frame(A, B), 1));
    }

    @Test
    void theLocalPortReceivesBroadcastsAndCanSend() {
        EthernetSwitch withService = new EthernetSwitch(List.of(0, 1), true, DELAY);

        assertEquals(List.of(1, EthernetSwitch.LOCAL_PORT), withService.receive(0, frame(A, MacAddress.BROADCAST), 0));
        assertEquals(List.of(0, 1), withService.receive(EthernetSwitch.LOCAL_PORT, frame(B, MacAddress.BROADCAST), 0));
        assertEquals(List.of(EthernetSwitch.LOCAL_PORT), withService.receive(0, frame(A, B), 1));
    }

    @Test
    void rejectsAPortThatIsNotSwitched() {
        // Port 4 of the box is the line input: it is not part of the switch.
        assertThrows(IllegalArgumentException.class, () -> box.receive(4, frame(A, B), 0));
    }
}
