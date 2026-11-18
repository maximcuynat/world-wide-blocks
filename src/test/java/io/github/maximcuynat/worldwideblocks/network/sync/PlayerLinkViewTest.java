package io.github.maximcuynat.worldwideblocks.network.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.ChunkKey;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.LinkMedium;
import io.github.maximcuynat.worldwideblocks.network.NetworkGraph;
import io.github.maximcuynat.worldwideblocks.network.NetworkNode;
import io.github.maximcuynat.worldwideblocks.network.PortRef;

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
        this.graph.putNode(new NetworkNode(id, "worldwideblocks:pc_tower", 1));
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

    // Cable between chunk (0, 0) and chunk (2, 0): 40 blocks.
    private Link crossChunkLink() {
        return link(device(OVERWORLD, 5, 5), device(OVERWORLD, 45, 5));
    }

    @Test
    void chunkKeyRoundsDownForNegativeCoordinates() {
        assertEquals(chunk(-1, -1), ChunkKey.of(new DeviceId(OVERWORLD, -1, 64, -16)));
        assertEquals(chunk(-2, 0), ChunkKey.of(new DeviceId(OVERWORLD, -17, 64, 15)));
    }

    @Test
    void linksTouchingFindsTheCableByItsTwoEndsAndForgetsItWhenRemoved() {
        Link cable = crossChunkLink();

        assertEquals(List.of(cable), List.copyOf(graph.linksTouching(chunk(0, 0))));
        assertEquals(List.of(cable), List.copyOf(graph.linksTouching(chunk(2, 0))));
        assertTrue(graph.linksTouching(chunk(1, 0)).isEmpty());

        graph.disconnect(cable.a());
        assertTrue(graph.linksTouching(chunk(0, 0)).isEmpty());
        assertTrue(graph.linksTouching(chunk(2, 0)).isEmpty());
    }

    @Test
    void aReceivedChunkSendsItsCablesOnlyOnceEvenThroughBothEnds() {
        Link cable = crossChunkLink();

        assertEquals(List.of(cable), sent(chunk(0, 0)));
        assertTrue(sent(chunk(2, 0)).isEmpty(), "already known through the other end");
        assertTrue(view.knows(cable.id()));
    }

    @Test
    void aChunkWithoutCableSendsNothing() {
        crossChunkLink();

        assertTrue(sent(chunk(1, 0)).isEmpty());
    }

    @Test
    void onUnloadTheCableStaysIfTheOtherEndIsStillTracked() {
        Link cable = crossChunkLink();
        sent(chunk(0, 0));
        sent(chunk(2, 0));

        assertTrue(unwatched(chunk(0, 0)).isEmpty());
        assertTrue(view.knows(cable.id()));
    }

    @Test
    void onUnloadTheCableIsForgottenWhenNoEndIsTrackedAnymore() {
        Link cable = crossChunkLink();
        sent(chunk(0, 0));
        sent(chunk(2, 0));
        unwatched(chunk(0, 0));

        assertEquals(List.of(cable.id()), unwatched(chunk(2, 0)));
        assertFalse(view.knows(cable.id()));
    }

    @Test
    void aCableInASingleChunkIsForgottenWithIt() {
        Link cable = link(device(OVERWORLD, 1, 1), device(OVERWORLD, 9, 9));
        sent(chunk(0, 0));

        assertEquals(List.of(cable.id()), unwatched(chunk(0, 0)));
    }

    @Test
    void aTeleportThatRemovesBothChunksOneAfterTheOtherForgetsTheCable() {
        // Case where Minecraft's tracking view would still be the old one during both removals.
        Link cable = crossChunkLink();
        sent(chunk(0, 0));
        sent(chunk(2, 0));

        List<UUID> forgotten = new ArrayList<>(unwatched(chunk(2, 0)));
        forgotten.addAll(unwatched(chunk(0, 0)));

        assertEquals(List.of(cable.id()), forgotten);
    }

    @Test
    void aChunkNeverReceivedMakesNothingForgotten() {
        Link cable = crossChunkLink();
        sent(chunk(0, 0));

        assertTrue(unwatched(chunk(2, 0)).isEmpty());
        assertTrue(view.knows(cable.id()));
    }

    @Test
    void aConnectedCableIsSentOnlyIfAnEndIsTracked() {
        sent(chunk(0, 0));
        Link visible = link(device(OVERWORLD, 2, 2), device(OVERWORLD, 60, 2));
        Link outOfView = link(device(OVERWORLD, 200, 200), device(OVERWORLD, 230, 200));

        assertTrue(view.linkAdded(visible));
        assertFalse(view.linkAdded(visible), "no second send");
        assertFalse(view.linkAdded(outOfView));
    }

    @Test
    void aRemovedCableIsReportedOnlyIfTheClientKnewIt() {
        Link cable = crossChunkLink();
        Link unknown = link(device(OVERWORLD, 300, 300), device(OVERWORLD, 310, 300));
        sent(chunk(0, 0));

        assertTrue(view.linkRemoved(cable.id()));
        assertFalse(view.linkRemoved(cable.id()));
        assertFalse(view.linkRemoved(unknown.id()));
    }

    @Test
    void changingDimensionEmptiesTheView() {
        Link cable = crossChunkLink();
        sent(chunk(0, 0));

        assertFalse(view.enterDimension(OVERWORLD), "same dimension: nothing to forget");
        assertTrue(view.enterDimension(NETHER));
        assertFalse(view.knows(cable.id()));
        assertFalse(view.linkAdded(cable), "the Overworld chunk is no longer tracked");
    }

    @Test
    void aChunkOfAnotherDimensionIsIgnored() {
        PortRef a = device(NETHER, 5, 5);
        PortRef b = device(NETHER, 10, 5);
        link(a, b);
        ChunkKey netherChunk = new ChunkKey(NETHER, 0, 0);
        Collection<Link> touching = graph.linksTouching(netherChunk);

        assertTrue(view.chunkSent(netherChunk, touching).isEmpty());
    }

    @Test
    void aNewPlayerMustAlwaysStartFromScratch() {
        PlayerLinkView fresh = new PlayerLinkView();

        assertTrue(fresh.enterDimension(OVERWORLD));
    }
}
