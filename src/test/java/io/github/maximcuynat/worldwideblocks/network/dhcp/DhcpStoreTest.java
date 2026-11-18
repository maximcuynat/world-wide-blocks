package io.github.maximcuynat.worldwideblocks.network.dhcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/** What the devices remember of DHCP, saved with the network. */
class DhcpStoreTest {
    private static final DeviceId TOWER = new DeviceId("minecraft:overworld", 5, 64, 0);
    private static final DeviceId LAPTOP = new DeviceId("minecraft:overworld", 9, 64, 0);
    private static final Ipv4Address SERVER = Ipv4Address.of(192, 168, 1, 1);
    private static final DhcpLease LEASE = new DhcpLease(Ipv4Address.of(192, 168, 1, 10), Ipv4Address.of(255, 255, 255, 0),
            SERVER, SERVER, SERVER, new MacAddress(0x02_00_00_00_00_FFL), 0, 24_000);

    @Test
    void aComputerLeaseIsKeptPerPortAndEachChangeIsReported() {
        DhcpStore store = new DhcpStore();
        DhcpClient.Memory memory = store.client(new PortRef(TOWER, 0));

        memory.remember(Optional.of(LEASE));
        assertTrue(store.consumeChanged());
        assertEquals(Optional.of(LEASE), store.client(new PortRef(TOWER, 0)).recall(), "read again through a new handle");
        assertEquals(Map.of(new PortRef(TOWER, 0), LEASE), store.clients());

        memory.remember(Optional.of(LEASE));
        assertFalse(store.consumeChanged(), "same lease: nothing to save");

        memory.remember(Optional.empty());
        assertTrue(store.consumeChanged());
        assertTrue(store.clients().isEmpty());
    }

    @Test
    void aRemovedComputerForgetsItsLeaseAndOnlyItsOwn() {
        DhcpStore store = new DhcpStore();
        store.restoreClient(new PortRef(TOWER, 0), LEASE);
        store.restoreClient(new PortRef(LAPTOP, 0), LEASE);
        assertFalse(store.consumeChanged(), "rereading the save is not a change");

        store.forget(TOWER);

        assertTrue(store.consumeChanged());
        assertEquals(Map.of(new PortRef(LAPTOP, 0), LEASE), store.clients());
    }
}
