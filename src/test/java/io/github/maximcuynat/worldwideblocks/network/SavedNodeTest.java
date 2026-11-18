package io.github.maximcuynat.worldwideblocks.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

class SavedNodeTest {
    private static final DeviceId ID = new DeviceId("minecraft:overworld", 0, 64, 0);

    @Test
    void unMondeDAvantLeLot1bNAQueDesPostesAPortsEthernetEtAucuneMac() {
        SavedNode saved = SavedNode.decode(ID, "worldwideblocks:pc_tower", Optional.empty(), 1, Optional.empty(), List.of());

        assertEquals(DeviceRole.HOST, saved.node().role());
        assertEquals(List.of(PortKind.ETHERNET), saved.node().ports());
        assertEquals(List.of(), saved.macs());
    }

    @Test
    void leRoleEtLesTypesDePortEcritsFontFoi() {
        List<PortKind> kinds = List.of(PortKind.ETHERNET, PortKind.LINE);
        MacAddress mac = MacAddress.parse("02:00:00:00:00:01").orElseThrow();

        SavedNode saved = SavedNode.decode(ID, "worldwideblocks:internet_box", Optional.of(DeviceRole.BOX), 2, Optional.of(kinds), List.of(mac, mac));

        assertEquals(DeviceRole.BOX, saved.node().role());
        assertEquals(kinds, saved.node().ports());
        assertEquals(List.of(mac, mac), saved.macs());
    }
}
