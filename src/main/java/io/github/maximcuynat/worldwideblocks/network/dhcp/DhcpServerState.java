package io.github.maximcuynat.worldwideblocks.network.dhcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * Memory of a DHCP server, saved with the network like the flash memory of a real box: whether the server is
 * enabled, and its bindings. A client has at most one binding, and an address belongs to at most one binding.
 *
 * <p>Every change is reported to the store ({@link DhcpStore}), which has the network saved.
 */
public final class DhcpServerState {
    private final Runnable changed;
    private final Map<MacAddress, DhcpBinding> byClient = new LinkedHashMap<>();
    private final Map<Ipv4Address, DhcpBinding> byAddress = new HashMap<>();
    private boolean enabled = true;

    DhcpServerState(Runnable changed) {
        this.changed = changed;
    }

    /**
     * The "DHCP server enabled" setting, on by default. A disabled server answers nothing and keeps its
     * bindings. Lot 1b-2 has no interface for it: the scenes and tests that need one server per network use it.
     */
    public boolean enabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            this.changed.run();
        }
    }

    public Optional<DhcpBinding> bindingOf(MacAddress client) {
        return Optional.ofNullable(this.byClient.get(client));
    }

    public Optional<DhcpBinding> bindingAt(Ipv4Address address) {
        return Optional.ofNullable(this.byAddress.get(address));
    }

    /** All bindings, in the order they were first made. */
    public List<DhcpBinding> bindings() {
        return List.copyOf(this.byClient.values());
    }

    /** The acknowledged leases still running, in address order (status panel). */
    public List<DhcpBinding> leasesAt(long now) {
        List<DhcpBinding> leases = new ArrayList<>();
        for (DhcpBinding binding : this.byClient.values()) {
            if (binding.leasedAt(now)) {
                leases.add(binding);
            }
        }
        leases.sort((a, b) -> Integer.compareUnsigned(a.address().value(), b.address().value()));
        return leases;
    }

    /** Records a binding; it replaces the client's previous one and whoever held the same address. */
    void put(DhcpBinding binding) {
        this.remove(binding.client());
        DhcpBinding holder = this.byAddress.get(binding.address());
        if (holder != null) {
            this.byClient.remove(holder.client());
        }
        this.byClient.put(binding.client(), binding);
        this.byAddress.put(binding.address(), binding);
        this.changed.run();
    }

    void remove(MacAddress client) {
        DhcpBinding previous = this.byClient.remove(client);
        if (previous != null) {
            this.byAddress.remove(previous.address());
            this.changed.run();
        }
    }

    /** Rereads the saved memory, without reporting a change. */
    void restore(boolean enabled, List<DhcpBinding> bindings) {
        this.enabled = enabled;
        for (DhcpBinding binding : bindings) {
            if (!this.byClient.containsKey(binding.client()) && !this.byAddress.containsKey(binding.address())) {
                this.byClient.put(binding.client(), binding);
                this.byAddress.put(binding.address(), binding);
            }
        }
    }
}
