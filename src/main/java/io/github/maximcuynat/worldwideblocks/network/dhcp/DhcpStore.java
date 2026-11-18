package io.github.maximcuynat.worldwideblocks.network.dhcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;

/**
 * What the devices remember of DHCP across a server restart, saved with the network: the memory of each
 * box's server. The behavior of the devices in the simulation is not saved, but it reads and writes here.
 *
 * <p>A device removed or replaced forgets everything: a broken box starts from zero.
 *
 * <p>Plain Java, no Minecraft class. Not thread-safe: access it only from the server thread.
 */
public final class DhcpStore {
    private final Map<DeviceId, DhcpServerState> servers = new LinkedHashMap<>();
    private boolean changed;

    /** Memory of the box's server, created empty (server enabled) the first time. */
    public DhcpServerState server(DeviceId box) {
        return this.servers.computeIfAbsent(box, id -> new DhcpServerState(this::markChanged));
    }

    /** Saved memories, by box. */
    public Map<DeviceId, DhcpServerState> servers() {
        return Collections.unmodifiableMap(this.servers);
    }

    /** Rereads the memory of a box's server from the save. */
    public void restoreServer(DeviceId box, boolean enabled, List<DhcpBinding> bindings) {
        this.server(box).restore(enabled, bindings);
    }

    /** The device was removed or replaced: what it remembered is lost. */
    public void forget(DeviceId device) {
        if (this.servers.remove(device) != null) {
            this.markChanged();
        }
    }

    /** True if something changed since the previous call: the network must then be saved. */
    public boolean consumeChanged() {
        boolean was = this.changed;
        this.changed = false;
        return was;
    }

    private void markChanged() {
        this.changed = true;
    }
}
