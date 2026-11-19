package io.github.maximcuynat.worldwideblocks.network.dhcp;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Prefix;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessageType;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpOptions;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * DHCP client of one network port of a computer: the state machine of RFC 2131 (4.4, figure 5).
 *
 * <ul>
 * <li>Cable connected: INIT, DISCOVER broadcast, SELECTING. The first offer is chosen (4.4.1 allows it:
 * "e.g., the first DHCPOFFER message"), REQUEST broadcast with the server identifier, REQUESTING; the
 * acknowledgement gives the lease, BOUND; a NAK sends it back to INIT.</li>
 * <li>At T1 (half of the lease): REQUEST to the server alone, RENEWING; at T2 (87.5%): REQUEST broadcast,
 * REBINDING; at the end of the lease without acknowledgement, the address is lost: INIT.</li>
 * <li>Cable unplugged: the address is lost at once (decision of lot 1b-2).</li>
 * <li>Cable already connected when the server starts, with a lease still running in the saved memory:
 * INIT-REBOOT, REQUEST of that address broadcast without server identifier, REBOOTING (3.2, 4.4.2). ACK: BOUND;
 * NAK: INIT; no answer after 60 s: INIT (decision of lot 1b-2; 4.4.2 also allows keeping the address).</li>
 * </ul>
 *
 * <p>Retransmissions, without the random +/-1 s of RFC 2131 4.1 (decision of lot 1b-2: exact timings):
 * DISCOVER after 4, 8, 16, 32 and 64 s, then every 64 s; REQUEST of REQUESTING and REBOOTING after 4, 8 and 16 s, then back
 * to INIT 32 s after the last one, 60 s in all ("for a total delay of 60 seconds", 3.1); in RENEWING and
 * REBINDING, "one-half of the remaining time until T2 [...] and one-half of the remaining lease time [...],
 * down to a minimum of 60 seconds" (4.4.5).
 *
 * <p>The transaction id ({@code xid}) is drawn at random for the first transaction, then increases by one
 * for each new transaction (4.1); a retransmission keeps it. A reply whose xid is not the current one is
 * ignored (4.4.1, 4.4.5).
 *
 * <p>Two DHCP servers on the same network: both boxes are 192.168.1.1, so their server identifiers are the same.
 * The client tells them apart by the source MAC of their frames: when two different servers answer the same
 * transaction, it reports the conflict once ({@link Port#conflict}). It keeps the first offer, and ignores a second
 * acknowledgement once bound (figure 5).
 *
 * <p>A single timer is pending at a time; each change of state makes the previous one stale.
 *
 * <p>Plain Java, no Minecraft class.
 */
public final class DhcpClient {
    static final long SECOND = DhcpServerConfig.TICKS_PER_SECOND;
    /** First wait before a retransmission (4.1). */
    static final long FIRST_RETRY = 4 * SECOND;
    /** Longest wait between two DISCOVER (4.1: "up to a maximum of 64 seconds"). */
    static final long MAX_RETRY = 64 * SECOND;
    /** Waits after each REQUEST of REQUESTING: 4, 8, 16 then 32 s before giving up, 60 s in all (3.1). */
    static final int REQUEST_SENDS = 4;
    /** Shortest wait between two REQUEST of RENEWING or REBINDING (4.4.5). */
    static final long MIN_RENEWAL_RETRY = 60 * SECOND;

    /** What the client needs from the computer: time, its port's MAC, sending and timers. */
    public interface Port {
        long now();

        MacAddress mac();

        void send(EthernetFrame frame);

        /** Calls {@link DhcpClient#onTimer} with {@code timer} in {@code delayTicks} ticks. */
        void schedule(long delayTicks, Timer timer);

        /** Two DHCP servers answered the same transaction: their MAC addresses, in the order they answered. */
        void conflict(List<MacAddress> servers);
    }

    /**
     * Where the client keeps its lease across a server restart: saved with the network ({@link DhcpStore}).
     * The client writes it at each change of its lease.
     */
    public interface Memory {
        /** A memory that keeps nothing: the client always starts with a DISCOVER. */
        Memory NONE = new Memory() {
            @Override
            public Optional<DhcpLease> recall() {
                return Optional.empty();
            }

            @Override
            public void remember(Optional<DhcpLease> lease) {
            }
        };

        Optional<DhcpLease> recall();

        void remember(Optional<DhcpLease> lease);
    }

    /**
     * Told of each change of the lease in use, to configure the interface: the computer applies it to its IP stack
     * ({@link DhcpClientScript}), as {@code dhclient} runs its script.
     */
    @FunctionalInterface
    public interface LeaseListener {
        /** A listener that does nothing. */
        LeaseListener NONE = lease -> {
        };

        void leaseChanged(Optional<DhcpLease> lease);
    }

    /** Timer of the client; only the latest one is valid. */
    public record Timer(long epoch) {
    }

    /** The offer chosen in SELECTING. */
    private record Offer(Ipv4Address address, Ipv4Address serverId) {
    }

    private final Memory memory;
    private final LeaseListener listener;
    private DhcpClientState state = DhcpClientState.UNPLUGGED;
    private int xid;
    /** Sends already made in the current transaction. */
    private int sends;
    private Offer offer;
    private DhcpLease lease;
    /** Address asked for again in REBOOTING. */
    private Ipv4Address previous;
    /** Servers that answered the current transaction, by the source MAC of their frames. */
    private final Set<MacAddress> answering = new LinkedHashSet<>();
    private long epoch;

    /** Client whose lease is not saved. */
    public DhcpClient(RandomGenerator random) {
        this(random, Memory.NONE);
    }

    public DhcpClient(RandomGenerator random, Memory memory) {
        this(random, memory, LeaseListener.NONE);
    }

    public DhcpClient(RandomGenerator random, Memory memory, LeaseListener listener) {
        this.xid = random.nextInt();
        this.memory = memory;
        this.listener = listener;
    }

    // ---- Reading ----

    public DhcpClientState state() {
        return this.state;
    }

    /** The lease in use; empty when the client has no address. */
    public Optional<DhcpLease> lease() {
        return this.state.hasAddress() ? Optional.ofNullable(this.lease) : Optional.empty();
    }

    // ---- Events ----

    /**
     * The cable is connected: the client asks again for the address it remembers, if its lease still runs (only
     * after a server restart: unplugging makes it forget), otherwise it looks for a server.
     */
    public void linkUp(Port port) {
        Optional<DhcpLease> remembered = this.memory.recall().filter(lease -> lease.expiresAt() > port.now());
        if (remembered.isPresent()) {
            this.reboot(port, remembered.get().address());
        } else {
            this.init(port);
        }
    }

    /** The cable is removed: the address is lost, nothing more is sent. */
    public void linkDown() {
        this.setLease(null);
        this.offer = null;
        this.state = DhcpClientState.UNPLUGGED;
        this.epoch++;
    }

    /** A message from a server reached this port. */
    public void receive(Port port, MacAddress serverMac, DhcpMessage message) {
        if (this.state == DhcpClientState.UNPLUGGED || !message.clientMac().equals(port.mac()) || message.xid() != this.xid) {
            return;
        }
        if (this.answering.add(serverMac) && this.answering.size() == 2) {
            port.conflict(List.copyOf(this.answering));
        }
        switch (this.state) {
            case SELECTING -> {
                if (message.type() == DhcpMessageType.OFFER) {
                    this.select(port, message);
                }
            }
            case REQUESTING, REBOOTING, RENEWING, REBINDING -> {
                if (message.type() == DhcpMessageType.ACK) {
                    this.bind(port, serverMac, message);
                } else if (message.type() == DhcpMessageType.NAK) {
                    this.init(port); // "DHCPNAK/Restart", "Halt network" (figure 5)
                }
            }
            default -> {
                // BOUND: "DHCPOFFER, DHCPACK, DHCPNAK/Discard" (figure 5).
            }
        }
    }

    public void onTimer(Port port, Timer timer) {
        if (timer.epoch() != this.epoch) {
            return; // stale: the state changed since
        }
        long now = port.now();
        switch (this.state) {
            case SELECTING -> this.sendDiscover(port);
            case REQUESTING, REBOOTING -> {
                if (this.sends >= REQUEST_SENDS) {
                    this.init(port);
                } else if (this.state == DhcpClientState.REQUESTING) {
                    this.sendSelectedRequest(port);
                } else {
                    this.sendPreviousRequest(port);
                }
            }
            case BOUND, RENEWING, REBINDING -> {
                if (now >= this.lease.expiresAt()) {
                    this.init(port); // "Lease expired/Halt network" (figure 5)
                } else if (now >= this.lease.rebindAt() && this.state != DhcpClientState.REBINDING) {
                    this.rebind(port);
                } else if (this.state == DhcpClientState.BOUND) {
                    this.renew(port);
                } else {
                    this.sendExtension(port, this.state == DhcpClientState.REBINDING);
                }
            }
            default -> {
            }
        }
    }

    // ---- Transitions ----

    /** INIT: the address is lost, a new transaction starts with a DISCOVER. */
    private void init(Port port) {
        this.setLease(null);
        this.offer = null;
        this.previous = null;
        this.state = DhcpClientState.SELECTING;
        this.newTransaction();
        this.sendDiscover(port);
    }

    /** INIT-REBOOT then REBOOTING: the remembered address is asked for again; it is not used until the ACK. */
    private void reboot(Port port, Ipv4Address address) {
        this.state = DhcpClientState.INIT_REBOOT;
        this.lease = null;
        this.previous = address;
        this.newTransaction();
        this.state = DhcpClientState.REBOOTING;
        this.sendPreviousRequest(port);
    }

    private void select(Port port, DhcpMessage offer) {
        Optional<Ipv4Address> serverId = offer.options().serverId();
        if (serverId.isEmpty() || offer.yourAddress().isAny()) {
            return;
        }
        this.offer = new Offer(offer.yourAddress(), serverId.get());
        this.state = DhcpClientState.REQUESTING;
        // Same xid as the offer (table 5): the transaction goes on.
        this.sends = 0;
        this.sendSelectedRequest(port);
    }

    private void bind(Port port, MacAddress serverMac, DhcpMessage ack) {
        DhcpOptions options = ack.options();
        if (ack.yourAddress().isAny() || options.serverId().isEmpty() || options.subnetMask().isEmpty()
                || options.router().isEmpty() || options.dnsServer().isEmpty() || options.leaseSeconds().isEmpty()
                || options.leaseSeconds().getAsLong() <= 0 || !Ipv4Prefix.isMask(options.subnetMask().get())) {
            return; // incomplete acknowledgement, or a mask whose ones are not contiguous: ignored
        }
        long now = port.now();
        this.setLease(new DhcpLease(ack.yourAddress(), options.subnetMask().get(), options.router().get(),
                options.dnsServer().get(), options.serverId().get(), serverMac, now,
                now + options.leaseSeconds().getAsLong() * SECOND));
        this.offer = null;
        this.previous = null;
        this.state = DhcpClientState.BOUND;
        this.epoch++;
        port.schedule(this.lease.renewAt() - now, new Timer(this.epoch));
    }

    /** T1: the client asks the server that lent the address to extend it. */
    private void renew(Port port) {
        this.state = DhcpClientState.RENEWING;
        this.newTransaction();
        this.sendExtension(port, false);
    }

    /** T2: the client asks any server. */
    private void rebind(Port port) {
        this.state = DhcpClientState.REBINDING;
        this.newTransaction();
        this.sendExtension(port, true);
    }

    /** The lease in use changes: the memory and the interface follow it. */
    private void setLease(DhcpLease lease) {
        this.lease = lease;
        this.memory.remember(Optional.ofNullable(lease));
        this.listener.leaseChanged(Optional.ofNullable(lease));
    }

    private void newTransaction() {
        this.xid++;
        this.sends = 0;
        this.answering.clear();
    }

    // ---- Sending ----

    private void sendDiscover(Port port) {
        port.send(DhcpFrames.fromClient(port.mac(), Ipv4Address.ANY, MacAddress.BROADCAST, Ipv4Address.BROADCAST,
                DhcpMessage.discover(this.xid, port.mac())));
        long wait = Math.min(FIRST_RETRY << Math.min(this.sends, 4), MAX_RETRY);
        this.sends++;
        this.retryIn(port, wait);
    }

    private void sendSelectedRequest(Port port) {
        port.send(DhcpFrames.fromClient(port.mac(), Ipv4Address.ANY, MacAddress.BROADCAST, Ipv4Address.BROADCAST,
                DhcpMessage.requestSelected(this.xid, port.mac(), this.offer.serverId(), this.offer.address())));
        long wait = FIRST_RETRY << this.sends;
        this.sends++;
        this.retryIn(port, wait);
    }

    /** REQUEST of INIT-REBOOT: broadcast, the remembered address requested, no server identifier, {@code ciaddr} zero. */
    private void sendPreviousRequest(Port port) {
        port.send(DhcpFrames.fromClient(port.mac(), Ipv4Address.ANY, MacAddress.BROADCAST, Ipv4Address.BROADCAST,
                DhcpMessage.requestPrevious(this.xid, port.mac(), this.previous)));
        long wait = FIRST_RETRY << this.sends;
        this.sends++;
        this.retryIn(port, wait);
    }

    /**
     * REQUEST with {@code ciaddr}: to the server alone in RENEWING ("This message will be unicast"), broadcast in
     * REBINDING ("MUST be broadcast to the 0xffffffff IP broadcast address"), RFC 2131 4.3.2. The next attempt
     * waits half the time left before T2 (or before the end of the lease), 60 s at least, and never beyond it.
     */
    private void sendExtension(Port port, boolean broadcast) {
        MacAddress destinationMac = broadcast ? MacAddress.BROADCAST : this.lease.serverMac();
        Ipv4Address destination = broadcast ? Ipv4Address.BROADCAST : this.lease.serverId();
        port.send(DhcpFrames.fromClient(port.mac(), this.lease.address(), destinationMac, destination,
                DhcpMessage.requestExtension(this.xid, port.mac(), this.lease.address())));
        this.sends++;
        long now = port.now();
        long deadline = broadcast ? this.lease.expiresAt() : this.lease.rebindAt();
        long next = Math.min(now + Math.max((deadline - now) / 2, MIN_RENEWAL_RETRY), deadline);
        this.retryIn(port, next - now);
    }

    private void retryIn(Port port, long delayTicks) {
        this.epoch++;
        port.schedule(delayTicks, new Timer(this.epoch));
    }
}
