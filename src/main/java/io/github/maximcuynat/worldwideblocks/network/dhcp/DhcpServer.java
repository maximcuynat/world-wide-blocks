package io.github.maximcuynat.worldwideblocks.network.dhcp;

import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpOptions;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * DHCP server (RFC 2131, 4.3): answers the messages of the clients with the addresses of its pool. It
 * decides only; the box puts the replies in frames ({@link DhcpFrames}) and sends them.
 *
 * <p>Its memory ({@link DhcpServerState}) is saved: after a server restart, the box knows its clients again.
 * Expired leases are freed lazily: an expired binding is reused when the pool has no free address left.
 *
 * <p>Plain Java, no Minecraft class.
 */
public final class DhcpServer {
    private final DhcpServerConfig config;
    private final DhcpServerState state;

    public DhcpServer(DhcpServerConfig config, DhcpServerState state) {
        this.config = config;
        this.state = state;
    }

    public DhcpServerConfig config() {
        return this.config;
    }

    public DhcpServerState state() {
        return this.state;
    }

    /**
     * Handles a client message.
     *
     * @return the reply, or empty when the server stays silent (disabled, pool full, request for another
     *         server, unknown client after INIT-REBOOT)
     */
    public Optional<DhcpMessage> handle(DhcpMessage message, long now) {
        if (!this.state.enabled()) {
            return Optional.empty();
        }
        return switch (message.type()) {
            case DISCOVER -> this.discover(message, now);
            case REQUEST -> this.request(message, now);
            // DECLINE, RELEASE and INFORM are not sent by the simulated clients (no ARP probe before lot 1c).
            default -> Optional.empty();
        };
    }

    // ---- DISCOVER (RFC 2131, 4.3.1) ----

    private Optional<DhcpMessage> discover(DhcpMessage discover, long now) {
        MacAddress client = discover.clientMac();
        Optional<DhcpBinding> current = this.state.bindingOf(client);
        if (current.isPresent() && current.get().leasedAt(now)) {
            // The client already holds a lease: offer it again as it is.
            return Optional.of(DhcpMessage.offer(discover, current.get().address(), this.leaseOptions()));
        }
        Optional<Ipv4Address> address = current.map(DhcpBinding::address).or(() -> this.freeAddress(now));
        if (address.isEmpty()) {
            return Optional.empty(); // pool full: no offer
        }
        // "the server SHOULD NOT reuse the selected network address before the client responds" (4.3.1)
        this.state.put(new DhcpBinding(client, address.get(), now + this.config.offerHoldTicks(), now, true));
        return Optional.of(DhcpMessage.offer(discover, address.get(), this.leaseOptions()));
    }

    /**
     * An address for a client the server does not know (RFC 2131, 4.3.1, last case: "a new address allocated
     * from the server's pool"): first an address that nobody holds, otherwise the address of the binding that
     * expired the longest ago (2.2: "the least recently assigned address"), whose client then loses its memory.
     */
    private Optional<Ipv4Address> freeAddress(long now) {
        for (int i = 0; i < this.config.poolSize(); i++) {
            Ipv4Address candidate = this.config.poolAddress(i);
            if (this.state.bindingAt(candidate).isEmpty()) {
                return Optional.of(candidate);
            }
        }
        DhcpBinding oldest = null;
        for (DhcpBinding binding : this.state.bindings()) {
            if (!binding.activeAt(now) && this.config.inPool(binding.address())
                    && (oldest == null || binding.assignedAt() < oldest.assignedAt())) {
                oldest = binding;
            }
        }
        return Optional.ofNullable(oldest).map(DhcpBinding::address);
    }

    // ---- REQUEST (RFC 2131, 4.3.2) ----

    private Optional<DhcpMessage> request(DhcpMessage request, long now) {
        Optional<Ipv4Address> serverId = request.options().serverId();
        if (serverId.isPresent()) {
            return this.selecting(request, serverId.get(), now);
        }
        if (request.clientAddress().isAny()) {
            return this.initReboot(request, now);
        }
        return this.extension(request, now);
    }

    /** "If the DHCPREQUEST message contains a 'server identifier' option, the message is in response to a DHCPOFFER". */
    private Optional<DhcpMessage> selecting(DhcpMessage request, Ipv4Address serverId, long now) {
        MacAddress client = request.clientMac();
        Optional<DhcpBinding> binding = this.state.bindingOf(client);
        if (!serverId.equals(this.config.serverAddress())) {
            // Another server was chosen: "notification that the client has declined that server's offer" (3.1).
            // The offered address is freed; the client keeps its memory as long as nobody needs the address.
            binding.filter(b -> b.offered() && b.activeAt(now)).ifPresent(b ->
                    this.state.put(new DhcpBinding(client, b.address(), now, b.assignedAt(), true)));
            return Optional.empty();
        }
        Optional<Ipv4Address> requested = request.options().requestedAddress();
        if (binding.isPresent() && requested.isPresent() && binding.get().address().equals(requested.get())) {
            return Optional.of(this.acknowledge(request, binding.get().address(), now));
        }
        // "If the selected server is unable to satisfy the DHCPREQUEST message [...] the server SHOULD respond with a DHCPNAK" (3.1)
        return Optional.of(this.refuse(request));
    }

    /** INIT-REBOOT: no server identifier, {@code ciaddr} zero, the previous address requested. */
    private Optional<DhcpMessage> initReboot(DhcpMessage request, long now) {
        Optional<Ipv4Address> requested = request.options().requestedAddress();
        if (requested.isEmpty()) {
            return Optional.empty(); // malformed (RFC 2131, table 5): ignored
        }
        if (!requested.get().sameNetwork(this.config.serverAddress(), this.config.subnetMask())) {
            return Optional.of(this.refuse(request)); // "on the wrong network": DHCPNAK
        }
        Optional<DhcpBinding> binding = this.state.bindingOf(request.clientMac());
        if (binding.isEmpty()) {
            return Optional.empty(); // "If the DHCP server has no record of this client, then it MUST remain silent"
        }
        if (!binding.get().address().equals(requested.get())) {
            return Optional.of(this.refuse(request)); // "the client's notion of its IP address is" not correct: DHCPNAK
        }
        return Optional.of(this.acknowledge(request, requested.get(), now));
    }

    /**
     * RENEWING (unicast) or REBINDING (broadcast): {@code ciaddr} filled in. The server extends the lease of the
     * address it lent to this client; it refuses any other address ("SHOULD check 'ciaddr' for correctness").
     */
    private Optional<DhcpMessage> extension(DhcpMessage request, long now) {
        Optional<DhcpBinding> binding = this.state.bindingOf(request.clientMac());
        if (binding.isPresent() && binding.get().address().equals(request.clientAddress())) {
            return Optional.of(this.acknowledge(request, request.clientAddress(), now));
        }
        return Optional.of(this.refuse(request));
    }

    private DhcpMessage acknowledge(DhcpMessage request, Ipv4Address address, long now) {
        this.state.put(new DhcpBinding(request.clientMac(), address, now + this.config.leaseTicks(), now, false));
        return DhcpMessage.ack(request, address, this.leaseOptions());
    }

    private DhcpMessage refuse(DhcpMessage request) {
        return DhcpMessage.nak(request, this.config.serverAddress());
    }

    private DhcpOptions leaseOptions() {
        return DhcpOptions.lease(this.config.serverAddress(), this.config.subnetMask(), this.config.router(),
                this.config.dnsServer(), this.config.leaseSeconds());
    }
}
