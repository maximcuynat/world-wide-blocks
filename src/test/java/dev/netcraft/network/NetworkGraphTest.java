package dev.netcraft.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NetworkGraphTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";
    private static final LinkMedium CUIVRE = LinkMedium.COPPER_ETHERNET;

    private NetworkGraph graph;

    @BeforeEach
    void setUp() {
        this.graph = new NetworkGraph();
    }

    /** Enregistre un appareil à 1 port dans l'Overworld et renvoie son port 0. */
    private PortRef device(int x, int y, int z) {
        return this.device(OVERWORLD, x, y, z, 1);
    }

    private PortRef device(String dimension, int x, int y, int z, int ports) {
        DeviceId id = new DeviceId(dimension, x, y, z);
        this.graph.putNode(new NetworkNode(id, "netcraft:pc_tower", ports));
        return new PortRef(id, 0);
    }

    @Nested
    class Connect {

        @Test
        void okCreeLeLienEtOccupeLesDeuxPorts() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(10, 64, 0);

            ConnectOutcome outcome = graph.connect(a, b, CUIVRE, true);

            assertEquals(ConnectResult.OK, outcome.result());
            Link link = outcome.link().orElseThrow();
            assertEquals(a, link.a());
            assertEquals(b, link.b());
            assertTrue(link.cableConsumed());
            assertEquals(Optional.of(link), graph.linkAt(a));
            assertEquals(Optional.of(link), graph.linkAt(b));
            assertEquals(1, graph.links().size());
        }

        @Test
        void retientQuUnCordonNAPasEteConsomme() {
            Link link = graph.connect(device(0, 64, 0), device(1, 64, 0), CUIVRE, false).link().orElseThrow();

            assertFalse(link.cableConsumed());
        }

        @Test
        void notADeviceSiUneExtremiteEstInconnue() {
            PortRef a = device(0, 64, 0);
            PortRef inconnu = new PortRef(new DeviceId(OVERWORLD, 5, 64, 0), 0);

            assertEquals(ConnectResult.NOT_A_DEVICE, graph.connect(a, inconnu, CUIVRE, true).result());
            assertEquals(ConnectResult.NOT_A_DEVICE, graph.connect(inconnu, a, CUIVRE, true).result());
            assertTrue(graph.links().isEmpty());
        }

        @Test
        void notADeviceSiLePortNExistePas() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);

            assertEquals(ConnectResult.NOT_A_DEVICE, graph.connect(a, new PortRef(b.device(), 1), CUIVRE, true).result());
            assertEquals(ConnectResult.NOT_A_DEVICE, graph.connect(a, new PortRef(b.device(), -1), CUIVRE, true).result());
        }

        @Test
        void sameDeviceSiLesDeuxExtremitesSontLeMemeAppareil() {
            PortRef port0 = device(OVERWORLD, 0, 64, 0, 2);
            PortRef port1 = new PortRef(port0.device(), 1);

            assertEquals(ConnectResult.SAME_DEVICE, graph.connect(port0, port1, CUIVRE, true).result());
            assertEquals(ConnectResult.SAME_DEVICE, graph.connect(port0, port0, CUIVRE, true).result());
        }

        @Test
        void otherDimensionSiLesAppareilsSontDansDeuxDimensions() {
            PortRef a = device(OVERWORLD, 0, 64, 0, 1);
            PortRef b = device(NETHER, 0, 64, 0, 1);

            assertEquals(ConnectResult.OTHER_DIMENSION, graph.connect(a, b, CUIVRE, true).result());
        }

        @Test
        void tooFarAuDelaDeCentBlocs() {
            PortRef a = device(0, 64, 0);
            PortRef aCent = device(100, 64, 0);
            PortRef aCentUn = device(0, 64, 101);

            assertEquals(ConnectResult.TOO_FAR, graph.connect(a, aCentUn, CUIVRE, true).result());
            assertEquals(ConnectResult.OK, graph.connect(a, aCent, CUIVRE, true).result(), "100 blocs pile reste autorisé");
        }

        @Test
        void tooFarMesureEnLigneDroite() {
            // 60 x 60 x 60 : 103,9 blocs en diagonale, chaque axe est sous 100.
            assertEquals(ConnectResult.TOO_FAR, graph.connect(device(0, 0, 0), device(60, 60, 60), CUIVRE, true).result());
        }

        @Test
        void portInUseSiLUnDesDeuxPortsEstDejaBranche() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            PortRef c = device(10, 64, 0);
            graph.connect(a, b, CUIVRE, true);

            assertEquals(ConnectResult.PORT_IN_USE, graph.connect(a, c, CUIVRE, true).result());
            assertEquals(ConnectResult.PORT_IN_USE, graph.connect(c, b, CUIVRE, true).result());
            assertEquals(1, graph.links().size());
        }
    }

    @Nested
    class Disconnect {

        @Test
        void retireLeLienEtLibereLesDeuxPorts() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            Link link = graph.connect(a, b, CUIVRE, true).link().orElseThrow();

            assertEquals(Optional.of(link), graph.disconnect(b));
            assertTrue(graph.linkAt(a).isEmpty());
            assertTrue(graph.linkAt(b).isEmpty());
            assertTrue(graph.links().isEmpty());
        }

        @Test
        void renvoieVideSurUnPortLibre() {
            assertTrue(graph.disconnect(device(0, 64, 0)).isEmpty());
        }

        @Test
        void unPortLibereAccepteUnNouveauCable() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            PortRef c = device(10, 64, 0);
            graph.connect(a, b, CUIVRE, true);
            graph.disconnect(a);

            assertEquals(ConnectResult.OK, graph.connect(a, c, CUIVRE, true).result());
        }
    }

    @Nested
    class RemoveDevice {

        @Test
        void retireLAppareilEtSesCablesSansToucherAuxAutres() {
            PortRef hub = device(OVERWORLD, 0, 64, 0, 2);
            PortRef a = device(5, 64, 0);
            PortRef b = device(10, 64, 0);
            PortRef c = device(15, 64, 0);
            PortRef d = device(20, 64, 0);
            Link versA = graph.connect(hub, a, CUIVRE, true).link().orElseThrow();
            Link versB = graph.connect(new PortRef(hub.device(), 1), b, CUIVRE, false).link().orElseThrow();
            Link autre = graph.connect(c, d, CUIVRE, true).link().orElseThrow();

            List<Link> removed = graph.removeDevice(hub.device());

            assertEquals(2, removed.size());
            assertTrue(removed.contains(versA));
            assertTrue(removed.contains(versB));
            assertTrue(graph.node(hub.device()).isEmpty());
            assertTrue(graph.linkAt(a).isEmpty(), "le port d'en face est libéré");
            assertTrue(graph.linkAt(b).isEmpty());
            assertEquals(List.of(autre), List.copyOf(graph.links()));
        }

        @Test
        void unAppareilSansCableNeRenvoieRien() {
            PortRef a = device(0, 64, 0);

            assertTrue(graph.removeDevice(a.device()).isEmpty());
            assertTrue(graph.node(a.device()).isEmpty());
        }
    }

    @Nested
    class PutNode {

        @Test
        void reenregistrerLeMemeAppareilGardeSesCables() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            graph.connect(a, b, CUIVRE, true);

            assertTrue(graph.putNode(new NetworkNode(a.device(), "netcraft:pc_tower", 1)).isEmpty());
            assertEquals(1, graph.links().size());
        }

        @Test
        void unAppareilRemplaceSansRetraitSignalePerdSesCables() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            Link link = graph.connect(a, b, CUIVRE, true).link().orElseThrow();

            List<Link> removed = graph.putNode(new NetworkNode(a.device(), "netcraft:laptop", 1));

            assertEquals(List.of(link), removed);
            assertTrue(graph.linkAt(b).isEmpty());
        }
    }

    @Nested
    class FirstFreePort {

        @Test
        void donneLePremierPortSansCable() {
            PortRef port0 = device(OVERWORLD, 0, 64, 0, 3);
            graph.connect(port0, device(5, 64, 0), CUIVRE, true);

            assertEquals(Optional.of(new PortRef(port0.device(), 1)), graph.firstFreePort(port0.device()));
        }

        @Test
        void videQuandTousLesPortsSontOccupesOuLAppareilInconnu() {
            PortRef a = device(0, 64, 0);
            graph.connect(a, device(5, 64, 0), CUIVRE, true);

            assertTrue(graph.firstFreePort(a.device()).isEmpty());
            assertTrue(graph.firstFreePort(new DeviceId(OVERWORLD, 99, 64, 99)).isEmpty());
        }
    }

    @Nested
    class RestoreLink {

        @Test
        void remetEnPlaceUnLienCoherentAvecSonIdentifiant() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            Link saved = new Link(UUID.randomUUID(), a, b, CUIVRE, false);

            assertTrue(graph.restoreLink(saved));
            assertEquals(Optional.of(saved), graph.linkAt(a));
        }

        @Test
        void ignoreUnLienVersUnAppareilInconnuOuUnPortOccupe() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            PortRef c = device(10, 64, 0);
            PortRef inconnu = new PortRef(new DeviceId(OVERWORLD, 50, 64, 0), 0);
            graph.restoreLink(new Link(UUID.randomUUID(), a, b, CUIVRE, true));

            assertFalse(graph.restoreLink(new Link(UUID.randomUUID(), a, inconnu, CUIVRE, true)));
            assertFalse(graph.restoreLink(new Link(UUID.randomUUID(), c, b, CUIVRE, true)));
            assertEquals(1, graph.links().size());
        }
    }
}
