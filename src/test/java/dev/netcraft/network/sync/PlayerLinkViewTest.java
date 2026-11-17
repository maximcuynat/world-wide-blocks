package dev.netcraft.network.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.netcraft.network.ChunkKey;
import dev.netcraft.network.DeviceId;
import dev.netcraft.network.Link;
import dev.netcraft.network.LinkMedium;
import dev.netcraft.network.NetworkGraph;
import dev.netcraft.network.NetworkNode;
import dev.netcraft.network.PortRef;

class PlayerLinkViewTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    private NetworkGraph graph;
    private PlayerLinkView view;

    @BeforeEach
    void setUp() {
        this.graph = new NetworkGraph();
        this.view = new PlayerLinkView();
        this.view.enterDimension(OVERWORLD);
    }

    private PortRef device(String dimension, int x, int z) {
        DeviceId id = new DeviceId(dimension, x, 64, z);
        this.graph.putNode(new NetworkNode(id, "netcraft:pc_tower", 1));
        return new PortRef(id, 0);
    }

    private Link link(PortRef a, PortRef b) {
        return this.graph.connect(a, b, LinkMedium.COPPER_ETHERNET, false).link().orElseThrow();
    }

    private static ChunkKey chunk(int x, int z) {
        return new ChunkKey(OVERWORLD, x, z);
    }

    private List<Link> sent(ChunkKey chunk) {
        return this.view.chunkSent(chunk, this.graph.linksTouching(chunk));
    }

    private List<UUID> unwatched(ChunkKey chunk) {
        return this.view.chunkUnwatched(chunk, this.graph.linksTouching(chunk));
    }

    // Câble entre le chunk (0, 0) et le chunk (2, 0) : 40 blocs.
    private Link crossChunkLink() {
        return link(device(OVERWORLD, 5, 5), device(OVERWORLD, 45, 5));
    }

    @Test
    void chunkKeyArrondiVersLeBasPourLesCoordonneesNegatives() {
        assertEquals(chunk(-1, -1), ChunkKey.of(new DeviceId(OVERWORLD, -1, 64, -16)));
        assertEquals(chunk(-2, 0), ChunkKey.of(new DeviceId(OVERWORLD, -17, 64, 15)));
    }

    @Test
    void linksTouchingTrouveLeCableParSesDeuxExtremitesEtLOublieQuandIlEstRetire() {
        Link cable = crossChunkLink();

        assertEquals(List.of(cable), List.copyOf(graph.linksTouching(chunk(0, 0))));
        assertEquals(List.of(cable), List.copyOf(graph.linksTouching(chunk(2, 0))));
        assertTrue(graph.linksTouching(chunk(1, 0)).isEmpty());

        graph.disconnect(cable.a());
        assertTrue(graph.linksTouching(chunk(0, 0)).isEmpty());
        assertTrue(graph.linksTouching(chunk(2, 0)).isEmpty());
    }

    @Test
    void unChunkRecuEnvoieSesCablesUneSeuleFoisMemeParLesDeuxExtremites() {
        Link cable = crossChunkLink();

        assertEquals(List.of(cable), sent(chunk(0, 0)));
        assertTrue(sent(chunk(2, 0)).isEmpty(), "déjà connu par l'autre extrémité");
        assertTrue(view.knows(cable.id()));
    }

    @Test
    void unChunkSansCableNEnvoieRien() {
        crossChunkLink();

        assertTrue(sent(chunk(1, 0)).isEmpty());
    }

    @Test
    void auDechargementLeCableResteSiLAutreExtremiteEstEncoreSuivie() {
        Link cable = crossChunkLink();
        sent(chunk(0, 0));
        sent(chunk(2, 0));

        assertTrue(unwatched(chunk(0, 0)).isEmpty());
        assertTrue(view.knows(cable.id()));
    }

    @Test
    void auDechargementLeCableEstOublieQuandPlusAucuneExtremiteNEstSuivie() {
        Link cable = crossChunkLink();
        sent(chunk(0, 0));
        sent(chunk(2, 0));
        unwatched(chunk(0, 0));

        assertEquals(List.of(cable.id()), unwatched(chunk(2, 0)));
        assertFalse(view.knows(cable.id()));
    }

    @Test
    void unCableDansUnSeulChunkEstOublieAvecLui() {
        Link cable = link(device(OVERWORLD, 1, 1), device(OVERWORLD, 9, 9));
        sent(chunk(0, 0));

        assertEquals(List.of(cable.id()), unwatched(chunk(0, 0)));
    }

    @Test
    void unTeleportQuiRetireLesDeuxChunksALaSuiteOublieLeCable() {
        // Cas où la vue de suivi de Minecraft serait encore l'ancienne pendant les deux retraits.
        Link cable = crossChunkLink();
        sent(chunk(0, 0));
        sent(chunk(2, 0));

        List<UUID> forgotten = new ArrayList<>(unwatched(chunk(2, 0)));
        forgotten.addAll(unwatched(chunk(0, 0)));

        assertEquals(List.of(cable.id()), forgotten);
    }

    @Test
    void unChunkJamaisRecuNeFaitRienOublier() {
        Link cable = crossChunkLink();
        sent(chunk(0, 0));

        assertTrue(unwatched(chunk(2, 0)).isEmpty());
        assertTrue(view.knows(cable.id()));
    }

    @Test
    void unCableBrancheNEstEnvoyeQueSiUneExtremiteEstSuivie() {
        sent(chunk(0, 0));
        Link visible = link(device(OVERWORLD, 2, 2), device(OVERWORLD, 60, 2));
        Link horsDeVue = link(device(OVERWORLD, 200, 200), device(OVERWORLD, 230, 200));

        assertTrue(view.linkAdded(visible));
        assertFalse(view.linkAdded(visible), "pas de second envoi");
        assertFalse(view.linkAdded(horsDeVue));
    }

    @Test
    void unCableRetireNEstSignaleQueSiLeClientLeConnaissait() {
        Link cable = crossChunkLink();
        Link inconnu = link(device(OVERWORLD, 300, 300), device(OVERWORLD, 310, 300));
        sent(chunk(0, 0));

        assertTrue(view.linkRemoved(cable.id()));
        assertFalse(view.linkRemoved(cable.id()));
        assertFalse(view.linkRemoved(inconnu.id()));
    }

    @Test
    void changerDeDimensionVideLaVue() {
        Link cable = crossChunkLink();
        sent(chunk(0, 0));

        assertFalse(view.enterDimension(OVERWORLD), "même dimension : rien à oublier");
        assertTrue(view.enterDimension(NETHER));
        assertFalse(view.knows(cable.id()));
        assertFalse(view.linkAdded(cable), "le chunk de l'Overworld n'est plus suivi");
    }

    @Test
    void unChunkDUneAutreDimensionEstIgnore() {
        PortRef a = device(NETHER, 5, 5);
        PortRef b = device(NETHER, 10, 5);
        link(a, b);
        ChunkKey netherChunk = new ChunkKey(NETHER, 0, 0);
        Collection<Link> touching = graph.linksTouching(netherChunk);

        assertTrue(view.chunkSent(netherChunk, touching).isEmpty());
    }

    @Test
    void unNouveauJoueurDoitToujoursRepartirDeZero() {
        PlayerLinkView neuf = new PlayerLinkView();

        assertTrue(neuf.enterDimension(OVERWORLD));
    }
}
