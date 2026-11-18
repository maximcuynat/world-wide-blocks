package io.github.maximcuynat.worldwideblocks.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.message.MessageHub;
import io.github.maximcuynat.worldwideblocks.message.server.Diagnostics;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServerState;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpStore;
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
 * <p>It also saves what the devices remember of DHCP ({@link DhcpStore}), and holds the simulation and the
 * state of the message module, which are not saved: they live and die with the server, with no static state.
 */
public final class NetworkSavedData extends SavedData {
    private static final Codec<NetworkSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            NetworkCodecs.NODE.listOf().fieldOf("nodes").forGetter(NetworkSavedData::savedNodes),
            NetworkCodecs.LINK.listOf().fieldOf("links").forGetter(data -> List.copyOf(data.graph.links())),
            // Since lot 1b-2: absent from older worlds.
            NetworkCodecs.DHCP_SERVER.listOf().optionalFieldOf("dhcp_servers", List.of()).forGetter(NetworkSavedData::savedDhcpServers),
            NetworkCodecs.DHCP_CLIENT.listOf().optionalFieldOf("dhcp_clients", List.of()).forGetter(NetworkSavedData::savedDhcpClients)
    ).apply(i, NetworkSavedData::new));

    /** Public for the game tests, which reread the file through a fresh storage. */
    public static final SavedDataType<NetworkSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "network"), () -> new NetworkSavedData(), CODEC);

    private final NetworkGraph graph = new NetworkGraph();
    private final DhcpStore dhcp = new DhcpStore();
    private final NetworkSimulation simulation = new NetworkSimulation(this.graph, this.dhcp);
    private final MessageHub messageHub = new MessageHub(this.simulation.limits().reportIntervalTicks());

    private NetworkSavedData() {
    }

    private NetworkSavedData(List<SavedNode> nodes, List<Link> links, List<SavedDhcpServer> dhcpServers,
            List<SavedDhcpClient> dhcpClients) {
        boolean macsGenerated = false;
        for (SavedNode saved : nodes) {
            macsGenerated |= !this.graph.restoreNode(saved.node(), saved.macs(), saved.serviceMac());
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
        int dhcpRestored = 0;
        for (SavedDhcpServer saved : dhcpServers) {
            // Only for a box still in the graph: a memory without its device is not reread.
            if (this.graph.node(saved.box()).filter(node -> node.role().hasService()).isPresent()) {
                this.dhcp.restoreServer(saved.box(), saved.enabled(), saved.bindings());
                dhcpRestored++;
            }
        }
        int leasesRestored = 0;
        for (SavedDhcpClient saved : dhcpClients) {
            // Only for a port of a computer still in the graph.
            if (this.graph.node(saved.port().device())
                    .filter(node -> node.role() == DeviceRole.HOST && node.hasPort(saved.port().index())).isPresent()) {
                this.dhcp.restoreClient(saved.port(), saved.lease());
                leasesRestored++;
            }
        }
        Diagnostics.debug("Network loaded: {} devices, {} cables, {} DHCP servers, {} computer leases, MAC addresses {}",
                nodes.size(), this.graph.links().size(), dhcpRestored, leasesRestored, macsGenerated
                        ? "generated for at least one device (world from before lot 1b or 1b-2, or invalid addresses)"
                        : "all taken from the save");
    }

    private List<SavedNode> savedNodes() {
        return this.graph.nodes().stream()
                .map(node -> new SavedNode(node, this.graph.macsOf(node.id()), this.graph.serviceMacOf(node.id())))
                .toList();
    }

    private List<SavedDhcpServer> savedDhcpServers() {
        List<SavedDhcpServer> saved = new ArrayList<>();
        for (Map.Entry<DeviceId, DhcpServerState> server : this.dhcp.servers().entrySet()) {
            saved.add(new SavedDhcpServer(server.getKey(), server.getValue().enabled(), server.getValue().bindings()));
        }
        return saved;
    }

    private List<SavedDhcpClient> savedDhcpClients() {
        List<SavedDhcpClient> saved = new ArrayList<>();
        this.dhcp.clients().forEach((port, lease) -> saved.add(new SavedDhcpClient(port, lease)));
        return saved;
    }

    NetworkGraph graph() {
        return this.graph;
    }

    DhcpStore dhcp() {
        return this.dhcp;
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
