package io.github.maximcuynat.worldwideblocks.client;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.network.sync.ClientLink;
import net.minecraft.core.BlockPos;

/**
 * Client-side copy of the cables the player can see. It is only used for display: the server
 * empties it on arrival in a dimension, then keeps it up to date link by link. It is emptied on
 * disconnection. Receiving the same cable twice has no effect: everything is indexed by its identifier.
 */
public final class ClientLinkCache {
    private final Map<UUID, ClientLink> links = new LinkedHashMap<>();
    private final Map<Port, ClientLink> linksByPort = new HashMap<>();

    private record Port(BlockPos pos, int index) {
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
