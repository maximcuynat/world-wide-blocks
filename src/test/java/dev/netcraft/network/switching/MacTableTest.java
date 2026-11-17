package dev.netcraft.network.switching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import dev.netcraft.network.protocol.MacAddress;

class MacTableTest {
    private static final long DELAI = 100;
    private static final MacAddress A = new MacAddress(0x02_00_00_00_00_0AL);
    private static final MacAddress B = new MacAddress(0x02_00_00_00_00_0BL);

    private final MacTable table = new MacTable(DELAI);

    @Test
    void leDelaiParDefautEst300SecondesDeJeu() {
        assertEquals(6000, MacTable.DEFAULT_AGING_TICKS);
    }

    @Test
    void retientLePortDUneMacSource() {
        table.learn(A, 2, 0);

        assertEquals(OptionalInt.of(2), table.lookup(A, 0));
        assertTrue(table.lookup(B, 0).isEmpty());
        assertEquals(List.of(new MacTable.Entry(A, 2, 0)), table.entries());
    }

    @Test
    void nApprendJamaisUneAdresseDeGroupe() {
        table.learn(MacAddress.BROADCAST, 0, 0);

        assertTrue(table.entries().isEmpty());
    }

    @Test
    void uneMacQuiChangeDePortEstDeplacee() {
        table.learn(A, 0, 0);
        table.learn(A, 3, 5);

        assertEquals(OptionalInt.of(3), table.lookup(A, 5));
        assertEquals(1, table.entries().size());
    }

    @Test
    void uneEntreeExpireApresLeDelaiSansEmission() {
        table.learn(A, 1, 0);

        assertEquals(OptionalInt.of(1), table.lookup(A, DELAI - 1));
        assertTrue(table.lookup(A, DELAI).isEmpty());

        table.purgeExpired(DELAI);
        assertTrue(table.entries().isEmpty());
    }

    @Test
    void chaqueEmissionRepousseLExpiration() {
        table.learn(A, 1, 0);
        table.learn(A, 1, 80);

        assertEquals(OptionalInt.of(1), table.lookup(A, 150));
        table.purgeExpired(150);
        assertEquals(1, table.entries().size());
    }

    @Test
    void retirerLeCableOublieLesMacDeCePortSeulement() {
        table.learn(A, 1, 0);
        table.learn(B, 2, 0);

        table.flushPort(1);

        assertTrue(table.lookup(A, 0).isEmpty());
        assertEquals(OptionalInt.of(2), table.lookup(B, 0));
    }
}
