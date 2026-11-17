package dev.netcraft.network;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.netcraft.NetCraft;
import dev.netcraft.network.sim.NetworkSimulation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persistance du graphe réseau, dans le stockage global du serveur ({@code MinecraftServer#getDataStorage}),
 * fichier {@code data/netcraft/network.dat} du monde.
 *
 * <p>Ce stockage ne dépend d'aucun chunk : un lien existe même quand ses deux extrémités sont
 * déchargées. Il appartient à l'instance du serveur : en solo, chaque monde ouvert a le sien.
 *
 * <p>Il porte aussi la simulation, qui n'est pas sauvegardée : elle vit et meurt avec le serveur,
 * sans aucun état statique.
 */
public final class NetworkSavedData extends SavedData {
    private static final Codec<NetworkSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            NetworkCodecs.NODE.listOf().fieldOf("nodes").forGetter(NetworkSavedData::savedNodes),
            NetworkCodecs.LINK.listOf().fieldOf("links").forGetter(data -> List.copyOf(data.graph.links()))
    ).apply(i, NetworkSavedData::new));

    static final SavedDataType<NetworkSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(NetCraft.MODID, "network"), () -> new NetworkSavedData(), CODEC);

    private final NetworkGraph graph = new NetworkGraph();
    private final NetworkSimulation simulation = new NetworkSimulation(this.graph);

    private NetworkSavedData() {
    }

    private NetworkSavedData(List<SavedNode> nodes, List<Link> links) {
        boolean macsGenerated = false;
        for (SavedNode saved : nodes) {
            macsGenerated |= !this.graph.restoreNode(saved.node(), saved.macs());
        }
        if (macsGenerated) {
            // Monde d'avant le lot 1b : on écrit tout de suite les adresses générées, pour qu'elles ne changent plus.
            this.setDirty();
        }
        for (Link link : links) {
            if (!this.graph.restoreLink(link)) {
                NetCraft.LOGGER.warn("Lien réseau incohérent ignoré au chargement : {}", link);
            }
        }
    }

    private List<SavedNode> savedNodes() {
        return this.graph.nodes().stream()
                .map(node -> new SavedNode(node, this.graph.macsOf(node.id())))
                .toList();
    }

    NetworkGraph graph() {
        return this.graph;
    }

    NetworkSimulation simulation() {
        return this.simulation;
    }
}
