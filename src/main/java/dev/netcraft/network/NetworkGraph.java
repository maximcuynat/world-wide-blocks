package dev.netcraft.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Graphe du réseau : les appareils (noeuds) et les câbles (liens) qui relient leurs ports.
 *
 * <p>C'est le coeur du moteur. Il est en Java pur, sans aucune classe Minecraft, pour être testé
 * sans lancer le jeu. Il ne sait rien des chunks : un lien existe tant qu'on ne l'a pas retiré.
 * La simulation reste abstraite : les lots suivants parcourront ce graphe pour savoir si un
 * chemin existe, sans faire voyager de messages tick par tick.
 *
 * <p>Pas thread-safe : n'y accéder que depuis le thread du serveur.
 */
public final class NetworkGraph {
    private final Map<DeviceId, NetworkNode> nodes = new LinkedHashMap<>();
    private final Map<UUID, Link> links = new LinkedHashMap<>();
    private final Map<PortRef, Link> linksByPort = new HashMap<>();
    private final Map<ChunkKey, Set<Link>> linksByChunk = new HashMap<>();

    // ---- Lecture ----

    public Optional<NetworkNode> node(DeviceId id) {
        return Optional.ofNullable(this.nodes.get(id));
    }

    public Collection<NetworkNode> nodes() {
        return Collections.unmodifiableCollection(this.nodes.values());
    }

    public Collection<Link> links() {
        return Collections.unmodifiableCollection(this.links.values());
    }

    public Optional<Link> linkAt(PortRef port) {
        return Optional.ofNullable(this.linksByPort.get(port));
    }

    public List<Link> linksOf(DeviceId device) {
        List<Link> result = new ArrayList<>();
        for (Link link : this.links.values()) {
            if (link.involves(device)) {
                result.add(link);
            }
        }
        return result;
    }

    /** Câbles dont au moins une extrémité se trouve dans ce chunk. */
    public Collection<Link> linksTouching(ChunkKey chunk) {
        Set<Link> found = this.linksByChunk.get(chunk);
        return found == null ? List.of() : Collections.unmodifiableCollection(found);
    }

    /** Premier port sans câble de l'appareil, s'il en reste un. */
    public Optional<PortRef> firstFreePort(DeviceId device) {
        NetworkNode node = this.nodes.get(device);
        if (node == null) {
            return Optional.empty();
        }
        for (int i = 0; i < node.portCount(); i++) {
            PortRef port = node.port(i);
            if (!this.linksByPort.containsKey(port)) {
                return Optional.of(port);
            }
        }
        return Optional.empty();
    }

    // ---- Modification ----

    /**
     * Enregistre un appareil, ou le met à jour.
     *
     * @return les liens retirés : si l'appareil enregistré à cette position a changé (bloc remplacé
     *         sans que son retrait ait été signalé), ses anciens câbles ne tiennent plus
     */
    public List<Link> putNode(NetworkNode node) {
        NetworkNode previous = this.nodes.put(node.id(), node);
        if (previous == null || previous.equals(node)) {
            return List.of();
        }
        return this.removeLinksOf(node.id());
    }

    /**
     * Branche un câble entre deux ports. Les vérifications se font dans cet ordre : appareils et
     * ports existants, appareils distincts, même dimension, distance, ports libres.
     */
    public ConnectOutcome connect(PortRef a, PortRef b, LinkMedium medium, boolean cableConsumed) {
        ConnectResult check = this.check(a, b, medium);
        if (check != ConnectResult.OK) {
            return ConnectOutcome.failure(check);
        }
        Link link = new Link(UUID.randomUUID(), a, b, medium, cableConsumed);
        this.add(link);
        return ConnectOutcome.success(link);
    }

    /** Retire le câble branché sur ce port, s'il y en a un. */
    public Optional<Link> disconnect(PortRef port) {
        Link link = this.linksByPort.get(port);
        if (link == null) {
            return Optional.empty();
        }
        this.remove(link);
        return Optional.of(link);
    }

    /** Retire l'appareil et tous ses câbles. */
    public List<Link> removeDevice(DeviceId device) {
        this.nodes.remove(device);
        return this.removeLinksOf(device);
    }

    /**
     * Remet en place un lien relu depuis la sauvegarde, sans en créer de nouveau.
     *
     * @return faux si le lien est incohérent avec le graphe (appareil inconnu, port occupé…) et a été ignoré
     */
    public boolean restoreLink(Link link) {
        if (this.links.containsKey(link.id()) || this.check(link.a(), link.b(), link.medium()) != ConnectResult.OK) {
            return false;
        }
        this.add(link);
        return true;
    }

    // ---- Interne ----

    private ConnectResult check(PortRef a, PortRef b, LinkMedium medium) {
        NetworkNode nodeA = this.nodes.get(a.device());
        NetworkNode nodeB = this.nodes.get(b.device());
        if (nodeA == null || nodeB == null || !nodeA.hasPort(a.index()) || !nodeB.hasPort(b.index())) {
            return ConnectResult.NOT_A_DEVICE;
        }
        if (a.device().equals(b.device())) {
            return ConnectResult.SAME_DEVICE;
        }
        if (!a.device().sameDimension(b.device())) {
            return ConnectResult.OTHER_DIMENSION;
        }
        if (a.device().distanceTo(b.device()) > medium.maxLength()) {
            return ConnectResult.TOO_FAR;
        }
        if (this.linksByPort.containsKey(a) || this.linksByPort.containsKey(b)) {
            return ConnectResult.PORT_IN_USE;
        }
        return ConnectResult.OK;
    }

    private List<Link> removeLinksOf(DeviceId device) {
        List<Link> removed = this.linksOf(device);
        removed.forEach(this::remove);
        return removed;
    }

    private void add(Link link) {
        this.links.put(link.id(), link);
        this.linksByPort.put(link.a(), link);
        this.linksByPort.put(link.b(), link);
        this.linksByChunk.computeIfAbsent(ChunkKey.of(link.a().device()), k -> new LinkedHashSet<>()).add(link);
        this.linksByChunk.computeIfAbsent(ChunkKey.of(link.b().device()), k -> new LinkedHashSet<>()).add(link);
    }

    private void remove(Link link) {
        this.links.remove(link.id());
        this.linksByPort.remove(link.a());
        this.linksByPort.remove(link.b());
        this.unindexChunk(ChunkKey.of(link.a().device()), link);
        this.unindexChunk(ChunkKey.of(link.b().device()), link);
    }

    private void unindexChunk(ChunkKey chunk, Link link) {
        Set<Link> inChunk = this.linksByChunk.get(chunk);
        if (inChunk != null && inChunk.remove(link) && inChunk.isEmpty()) {
            this.linksByChunk.remove(chunk);
        }
    }
}
