package dev.netcraft.network;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.minecraft.server.MinecraftServer;

/**
 * Point d'entrée unique du moteur réseau côté serveur. Toute modification du réseau passe par ici,
 * pour que la sauvegarde soit marquée à jour (et, plus tard, que les clients soient prévenus).
 *
 * <p>Aucun état statique : l'objet est une façade légère sur les données du serveur en cours,
 * obtenue par {@link #of(MinecraftServer)}. Les données disparaissent avec le serveur, si bien
 * qu'en solo deux mondes ne se mélangent jamais.
 */
public final class NetworkManager {
    private final NetworkSavedData data;

    private NetworkManager(NetworkSavedData data) {
        this.data = data;
    }

    public static NetworkManager of(MinecraftServer server) {
        return new NetworkManager(server.getDataStorage().computeIfAbsent(NetworkSavedData.TYPE));
    }

    // ---- Lecture ----

    public Optional<NetworkNode> node(DeviceId id) {
        return this.graph().node(id);
    }

    public Collection<Link> links() {
        return this.graph().links();
    }

    public Optional<Link> linkAt(PortRef port) {
        return this.graph().linkAt(port);
    }

    public List<Link> linksOf(DeviceId device) {
        return this.graph().linksOf(device);
    }

    public Optional<PortRef> firstFreePort(DeviceId device) {
        return this.graph().firstFreePort(device);
    }

    // ---- Modification ----

    /** Enregistre ou met à jour un appareil ; renvoie les câbles devenus invalides et retirés. */
    public List<Link> registerDevice(NetworkNode node) {
        List<Link> removed = this.graph().putNode(node);
        this.data.setDirty();
        return removed;
    }

    public ConnectOutcome connect(PortRef a, PortRef b, LinkMedium medium, boolean cableConsumed) {
        ConnectOutcome outcome = this.graph().connect(a, b, medium, cableConsumed);
        if (outcome.isSuccess()) {
            this.data.setDirty();
        }
        return outcome;
    }

    public Optional<Link> disconnect(PortRef port) {
        Optional<Link> removed = this.graph().disconnect(port);
        if (removed.isPresent()) {
            this.data.setDirty();
        }
        return removed;
    }

    /** Retire l'appareil et ses câbles ; renvoie les câbles retirés. */
    public List<Link> removeDevice(DeviceId device) {
        boolean known = this.graph().node(device).isPresent();
        List<Link> removed = this.graph().removeDevice(device);
        if (known || !removed.isEmpty()) {
            this.data.setDirty();
        }
        return removed;
    }

    private NetworkGraph graph() {
        return this.data.graph();
    }
}
