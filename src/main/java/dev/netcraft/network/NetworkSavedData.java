package dev.netcraft.network;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.netcraft.NetCraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persistance du graphe réseau, dans le stockage global du serveur ({@code MinecraftServer#getDataStorage}),
 * fichier {@code data/netcraft/network.dat} du monde.
 *
 * <p>Ce stockage ne dépend d'aucun chunk : un lien existe même quand ses deux extrémités sont
 * déchargées. Il appartient à l'instance du serveur : en solo, chaque monde ouvert a le sien.
 */
public final class NetworkSavedData extends SavedData {
    private static final Codec<NetworkSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            NetworkCodecs.NODE.listOf().fieldOf("nodes").forGetter(data -> List.copyOf(data.graph.nodes())),
            NetworkCodecs.LINK.listOf().fieldOf("links").forGetter(data -> List.copyOf(data.graph.links()))
    ).apply(i, NetworkSavedData::new));

    static final SavedDataType<NetworkSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(NetCraft.MODID, "network"), () -> new NetworkSavedData(), CODEC);

    private final NetworkGraph graph = new NetworkGraph();

    private NetworkSavedData() {
    }

    private NetworkSavedData(List<NetworkNode> nodes, List<Link> links) {
        nodes.forEach(this.graph::putNode);
        for (Link link : links) {
            if (!this.graph.restoreLink(link)) {
                NetCraft.LOGGER.warn("Lien réseau incohérent ignoré au chargement : {}", link);
            }
        }
    }

    NetworkGraph graph() {
        return this.graph;
    }
}
