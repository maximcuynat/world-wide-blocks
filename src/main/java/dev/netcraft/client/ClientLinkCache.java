package dev.netcraft.client;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.netcraft.network.sync.ClientLink;
import net.minecraft.core.BlockPos;

/**
 * Copie, côté client, des câbles de la dimension où se trouve le joueur. Elle ne sert qu'à
 * l'affichage : le serveur la remplace à la connexion et au changement de dimension, puis la tient
 * à jour lien par lien. Elle est vidée à la déconnexion.
 */
public final class ClientLinkCache {
    private final Map<UUID, ClientLink> links = new LinkedHashMap<>();
    private final Map<Port, ClientLink> linksByPort = new HashMap<>();

    private record Port(BlockPos pos, int index) {
    }

    public void replaceAll(List<ClientLink> newLinks) {
        this.clear();
        newLinks.forEach(this::add);
    }

    public void add(ClientLink link) {
        this.remove(link.id());
        this.links.put(link.id(), link);
        this.linksByPort.put(new Port(link.a(), link.portA()), link);
        this.linksByPort.put(new Port(link.b(), link.portB()), link);
    }

    public void remove(UUID id) {
        ClientLink link = this.links.remove(id);
        if (link != null) {
            this.linksByPort.remove(new Port(link.a(), link.portA()));
            this.linksByPort.remove(new Port(link.b(), link.portB()));
        }
    }

    public void clear() {
        this.links.clear();
        this.linksByPort.clear();
    }

    public boolean isConnected(BlockPos pos, int port) {
        return this.linksByPort.containsKey(new Port(pos, port));
    }

    public Collection<ClientLink> links() {
        return Collections.unmodifiableCollection(this.links.values());
    }
}
