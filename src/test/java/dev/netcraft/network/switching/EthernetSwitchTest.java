package dev.netcraft.network.switching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.netcraft.network.protocol.EthernetFrame;
import dev.netcraft.network.protocol.MacAddress;
import dev.netcraft.network.protocol.PresencePayload;

class EthernetSwitchTest {
    private static final long DELAI = 100;
    private static final MacAddress A = new MacAddress(0x02_00_00_00_00_0AL);
    private static final MacAddress B = new MacAddress(0x02_00_00_00_00_0BL);
    private static final MacAddress C = new MacAddress(0x02_00_00_00_00_0CL);

    /** Les 4 ports réseau local de la box, sans point local. */
    private final EthernetSwitch box = new EthernetSwitch(List.of(0, 1, 2, 3), false, DELAI);

    private static EthernetFrame trame(MacAddress source, MacAddress destination) {
        return new EthernetFrame(source, destination, PresencePayload.INSTANCE);
    }

    @Test
    void diffuseSurTousLesAutresPortsUneTrameAdresseeATous() {
        assertEquals(List.of(0, 2, 3), box.receive(1, trame(A, MacAddress.BROADCAST), 0));
    }

    @Test
    void diffuseUneTrameDontLaDestinationEstInconnue() {
        assertEquals(List.of(1, 2, 3), box.receive(0, trame(A, B), 0));
    }

    @Test
    void relaieVersLePortAppris() {
        box.receive(2, trame(B, MacAddress.BROADCAST), 0);

        assertEquals(List.of(2), box.receive(0, trame(A, B), 1));
    }

    @Test
    void apprendLaSourceDeChaqueTrameRecue() {
        box.receive(3, trame(C, MacAddress.BROADCAST), 0);

        assertEquals(List.of(new MacTable.Entry(C, 3, 0)), box.entries());
    }

    @Test
    void filtreUneTrameDontLaDestinationEstDuMemeCote() {
        // A et B derrière le même port (un autre switch entre eux) : rien à relayer.
        box.receive(1, trame(B, MacAddress.BROADCAST), 0);

        assertEquals(List.of(), box.receive(1, trame(A, B), 1));
    }

    @Test
    void diffuseDeNouveauQuandLEntreeAExpire() {
        box.receive(2, trame(B, MacAddress.BROADCAST), 0);

        assertEquals(List.of(1, 2, 3), box.receive(0, trame(A, B), DELAI));
    }

    @Test
    void diffuseDeNouveauQuandLeCableDuPortEstRetire() {
        box.receive(2, trame(B, MacAddress.BROADCAST), 0);

        box.linkDown(2);

        assertEquals(List.of(1, 2, 3), box.receive(0, trame(A, B), 1));
    }

    @Test
    void lePointLocalRecoitLesDiffusionsEtPeutEmettre() {
        EthernetSwitch avecService = new EthernetSwitch(List.of(0, 1), true, DELAI);

        assertEquals(List.of(1, EthernetSwitch.LOCAL_PORT), avecService.receive(0, trame(A, MacAddress.BROADCAST), 0));
        assertEquals(List.of(0, 1), avecService.receive(EthernetSwitch.LOCAL_PORT, trame(B, MacAddress.BROADCAST), 0));
        assertEquals(List.of(EthernetSwitch.LOCAL_PORT), avecService.receive(0, trame(A, B), 1));
    }

    @Test
    void refuseUnPortNonCommute() {
        // Le port 4 de la box est l'arrivée de ligne : il ne fait pas partie du switch.
        assertThrows(IllegalArgumentException.class, () -> box.receive(4, trame(A, B), 0));
    }
}
