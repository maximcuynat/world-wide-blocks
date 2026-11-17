package dev.netcraft.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.netcraft.network.protocol.MacAddress;

class SavedNodeTest {
    private static final DeviceId ID = new DeviceId("minecraft:overworld", 0, 64, 0);

    @Test
    void unMondeDAvantLeLot1bNAQueDesPortsEthernetEtAucuneMac() {
        SavedNode saved = SavedNode.decode(ID, "netcraft:pc_tower", 1, Optional.empty(), List.of());

        assertEquals(List.of(PortKind.ETHERNET), saved.node().ports());
        assertEquals(List.of(), saved.macs());
    }

    @Test
    void lesTypesDePortEcritsFontFoi() {
        List<PortKind> kinds = List.of(PortKind.ETHERNET, PortKind.LINE);
        MacAddress mac = MacAddress.parse("02:00:00:00:00:01").orElseThrow();

        SavedNode saved = SavedNode.decode(ID, "netcraft:internet_box", 2, Optional.of(kinds), List.of(mac, mac));

        assertEquals(kinds, saved.node().ports());
        assertEquals(List.of(mac, mac), saved.macs());
    }
}
