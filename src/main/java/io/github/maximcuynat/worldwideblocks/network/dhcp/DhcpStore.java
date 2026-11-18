package io.github.maximcuynat.worldwideblocks.network.dhcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.PortRef;

/**
 * What the devices remember of DHCP across a server restart, saved with the network: the memory of each
 * box's server, and the lease of each computer port, which it asks for again after a restart (INIT-REBOOT,
 * RFC 2131 3.2). The behavior of the devices in the simulation is not saved, but it reads and writes here.
 *
 * <p>A device removed or replaced forgets everything: a broken box starts from zero.
 *
 * <p>Plain Java, no Minecraft class. Not thread-safe: access it only from the server thread.
 */
public final class DhcpStore {
    private final Map<DeviceId, DhcpServerState> servers = new LinkedHashMap<>();
    private final Map<PortRef, DhcpLease> clients = new LinkedHashMap<>();
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

    /** What the DHCP client of this computer port remembers: its lease, read at start and kept up to date. */
    public DhcpClient.Memory client(PortRef port) {
        return new DhcpClient.Memory() {
            @Override
            public Optional<DhcpLease> recall() {
                return Optional.ofNullable(DhcpStore.this.clients.get(port));
            }

            @Override
            public void remember(Optional<DhcpLease> lease) {
                DhcpLease previous = lease.isPresent() ? DhcpStore.this.clients.put(port, lease.get()) : DhcpStore.this.clients.remove(port);
                if (!lease.equals(Optional.ofNullable(previous))) {
                    DhcpStore.this.markChanged();
                }
            }
        };
    }

    /** Saved leases, by computer port. */
    public Map<PortRef, DhcpLease> clients() {
        return Collections.unmodifiableMap(this.clients);
    }

    /** Rereads the lease of a computer port from the save. */
    public void restoreClient(PortRef port, DhcpLease lease) {
        this.clients.put(port, lease);
    }

    /** The device was removed or replaced: what it remembered is lost. */
    public void forget(DeviceId device) {
        boolean removed = this.servers.remove(device) != null;
        removed |= this.clients.keySet().removeIf(port -> port.device().equals(device));
        if (removed) {
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
