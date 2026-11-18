package io.github.maximcuynat.worldwideblocks.network;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.message.MessageHub;
import io.github.maximcuynat.worldwideblocks.message.server.Diagnostics;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.sim.NetworkSimulation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persistence of the network graph, in the server's global storage ({@code MinecraftServer#getDataStorage}),
 * file {@code data/worldwideblocks/network.dat} of the world.
 *
 * <p>This storage depends on no chunk: a link exists even when both of its ends are
 * unloaded. It belongs to the server instance: in singleplayer, each opened world has its own.
 *
 * <p>It also holds the simulation and the state of the message module, which are not saved: they
 * live and die with the server, with no static state.
 */
public final class NetworkSavedData extends SavedData {
    private static final Codec<NetworkSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            NetworkCodecs.NODE.listOf().fieldOf("nodes").forGetter(NetworkSavedData::savedNodes),
            NetworkCodecs.LINK.listOf().fieldOf("links").forGetter(data -> List.copyOf(data.graph.links()))
    ).apply(i, NetworkSavedData::new));

    /** Public for the game tests, which reread the file through a fresh storage. */
    public static final SavedDataType<NetworkSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "network"), () -> new NetworkSavedData(), CODEC);

    private final NetworkGraph graph = new NetworkGraph();
    private final NetworkSimulation simulation = new NetworkSimulation(this.graph);
    private final MessageHub messageHub = new MessageHub(this.simulation.limits().reportIntervalTicks());

    private NetworkSavedData() {
    }

    private NetworkSavedData(List<SavedNode> nodes, List<Link> links) {
        boolean macsGenerated = false;
        for (SavedNode saved : nodes) {
            macsGenerated |= !this.graph.restoreNode(saved.node(), saved.macs());
        }
        if (macsGenerated) {
            // World from before lot 1b: write the generated addresses right away, so they do not change anymore.
            this.setDirty();
        }
        for (Link link : links) {
            if (!this.graph.restoreLink(link)) {
                WorldWideBlocks.LOGGER.warn("Inconsistent network link ignored on load: {}", link);
            }
        }
        Diagnostics.debug("Network loaded: {} devices, {} cables, MAC addresses {}", nodes.size(), this.graph.links().size(),
                macsGenerated ? "generated for at least one device (world from before lot 1b, or invalid addresses)" : "all taken from the save");
    }

    private List<SavedNode> savedNodes() {
        return this.graph.nodes().stream()
                .map(node -> new SavedNode(node, this.graph.macsOf(node.id())))
                .toList();
    }

    NetworkGraph graph() {
        return this.graph;
    }

    // ---- Read only, to compare data reread from disk (game tests) ----

    public Optional<NetworkNode> node(DeviceId id) {
        return this.graph.node(id);
    }

    public List<MacAddress> macsOf(DeviceId id) {
        return this.graph.macsOf(id);
    }

    public List<Link> linksOf(DeviceId id) {
        return this.graph.linksOf(id);
    }

    NetworkSimulation simulation() {
        return this.simulation;
    }

    /** Throttle and listeners of the message module (see {@code MessageCenter}). */
    public MessageHub messageHub() {
        return this.messageHub;
    }
}
