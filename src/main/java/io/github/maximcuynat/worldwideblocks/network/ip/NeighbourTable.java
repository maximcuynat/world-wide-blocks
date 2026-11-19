package io.github.maximcuynat.worldwideblocks.network.ip;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.protocol.ArpPacket;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Packet;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * The ARP cache of a machine ({@code ip neigh}): which hardware address answers for each IPv4 neighbour, with the
 * states and timers of Linux. Read in {@code net/core/neighbour.c} ({@code neigh_timer_handler},
 * {@code __neigh_event_send}, {@code __neigh_update}) and {@code net/ipv4/arp.c} ({@code arp_tbl},
 * {@code arp_process}, {@code arp_netdev_event}) on 2026-11-19:
 *
 * <ul>
 * <li>A packet to an unknown neighbour: entry {@code INCOMPLETE}, an ARP request broadcast at once then every
 * second, three in all ({@code mcast_probes}); the packets wait in the entry. Without a reply, one second after the
 * third request: {@code FAILED}, and each waiting packet is reported lost (the sender gets "host unreachable").
 * The next packet starts again from {@code INCOMPLETE}.</li>
 * <li>A reply addressed to this machine: {@code REACHABLE}, the waiting packets leave. 30 s after the last
 * confirmation ({@code base_reachable_time}), the entry becomes {@code DELAY} if it was used in the last 5 s,
 * {@code STALE} otherwise.</li>
 * <li>A packet to a {@code STALE} neighbour leaves at once, and the entry becomes {@code DELAY}; 5 s later
 * ({@code delay_first_probe_time}) without confirmation, {@code PROBE}: three requests unicast to the known address,
 * one per second, then {@code FAILED}. A reply makes it {@code REACHABLE} again.</li>
 * <li>A request for one of the addresses of this machine is answered, and gives a {@code STALE} entry for the
 * asker. Another ARP packet only updates an entry that already exists ({@code arp_accept} off): {@code REACHABLE}
 * for a reply to this machine, {@code STALE} otherwise. Within one second of the last change ({@code locktime}),
 * a different hardware address is ignored: the first answer wins.</li>
 * <li>The cable removed: the entries of the interface are forgotten ({@code arp_evict_nocarrier}, on by default).</li>
 * </ul>
 *
 * <p>Differences, for deterministic results: Linux draws the reachable time between 15 and 45 s, here always 30 s;
 * Linux re-arms a timer due at the same instant 10 ms later, here a state changes on the tick its time is reached.
 * Entries are never collected: Linux does not collect below 128 entries ({@code gc_thresh1}). {@code ip neigh}
 * lists the entries in the order they were created (the kernel lists them in the order of its hash table).
 */
public final class NeighbourTable {
    /** Game ticks per second. */
    private static final long SECOND = 20L;
    /** {@code base_reachable_time} of {@code arp_tbl}: 30 s. */
    public static final long REACHABLE_TIME = 30 * SECOND;
    /** {@code delay_first_probe_time}: 5 s. */
    public static final long DELAY_PROBE_TIME = 5 * SECOND;
    /** {@code retrans_time}: 1 s between two requests. */
    public static final long RETRANS_TIME = SECOND;
    /** {@code locktime}: 1 s. */
    public static final long LOCKTIME = SECOND;
    /** Requests broadcast to resolve an address ({@code mcast_probes}). */
    public static final int MCAST_PROBES = 3;
    /** Requests unicast to confirm a known address ({@code ucast_probes}). */
    public static final int UCAST_PROBES = 3;
    /** Packets waiting for one address, at most ({@code unres_qlen}, "Default: 101" in ip-sysctl.rst). */
    public static final int QUEUE_LENGTH = 101;

    /** States of an entry, in the order {@code ip neigh} writes their names. */
    public enum State {
        INCOMPLETE, REACHABLE, STALE, DELAY, PROBE, FAILED;

        /** {@code NUD_VALID}: the hardware address is known. */
        boolean valid() {
            return this == REACHABLE || this == STALE || this == DELAY || this == PROBE;
        }

        /** {@code NUD_IN_TIMER}: the entry has a timer running. */
        boolean inTimer() {
            return this == INCOMPLETE || this == REACHABLE || this == DELAY || this == PROBE;
        }
    }

    /** What becomes of a waiting packet when its neighbour cannot be resolved. */
    @FunctionalInterface
    public interface Failure {
        /** A failure that reports nothing. */
        Failure NONE = packet -> {
        };

        void unreachable(Ipv4Packet packet);
    }

    /** An entry as {@code ip neigh} shows it. */
    public record Neighbour(Ipv4Address address, String device, Optional<MacAddress> mac, State state) {
    }

    /** Timer of an entry; only the latest of an entry is valid. */
    public record Timer(String device, Ipv4Address address, long generation) {
    }

    private record Key(String device, Ipv4Address address) {
    }

    private record Waiting(Ipv4Packet packet, Failure failure) {
    }

    private static final class Entry {
        private final NetInterface link;
        private final Ipv4Address address;
        private MacAddress mac;
        /** Null before the first resolution ({@code NUD_NONE}). */
        private State state;
        private long confirmed = Long.MIN_VALUE / 2;
        private long used;
        private long updated = Long.MIN_VALUE / 2;
        private int probes;
        private long generation;
        private final List<Waiting> waiting = new ArrayList<>();

        private Entry(NetInterface link, Ipv4Address address) {
            this.link = link;
            this.address = address;
        }
    }

    private final Ipv4Stack stack;
    private final Map<Key, Entry> entries = new LinkedHashMap<>();

    NeighbourTable(Ipv4Stack stack) {
        this.stack = stack;
    }

    /** The entries, in the order they were created; an entry never resolved is not listed. */
    public List<Neighbour> neighbours() {
        return this.entries.values().stream().filter(entry -> entry.state != null)
                .map(entry -> new Neighbour(entry.address, entry.link.name(),
                        entry.state.valid() ? Optional.of(entry.mac) : Optional.empty(), entry.state))
                .toList();
    }

    // ---- Sending ----

    /**
     * Sends a packet to a neighbour of the link of {@code out}: at once if its address is known, after resolution
     * otherwise ({@code neigh_resolve_output}).
     *
     * @param failure told of the packet if the neighbour cannot be resolved
     */
    void output(Ipv4Stack.Context context, NetInterface out, Ipv4Address neighbour, Ipv4Packet packet, Failure failure) {
        long now = context.now();
        Entry entry = this.entries.computeIfAbsent(new Key(out.name(), neighbour), key -> new Entry(out, neighbour));
        entry.used = now;
        if (entry.state == State.REACHABLE || entry.state == State.DELAY || entry.state == State.PROBE) {
            this.transmit(context, entry, packet);
            return;
        }
        if (entry.state == State.STALE) {
            entry.state = State.DELAY;
            entry.updated = now;
            this.arm(context, entry, now + DELAY_PROBE_TIME);
            this.transmit(context, entry, packet);
            return;
        }
        if (entry.state != State.INCOMPLETE) {
            // NUD_NONE or NUD_FAILED: resolution starts, the probes counted from the unicast ones.
            entry.state = State.INCOMPLETE;
            entry.updated = now;
            entry.probes = UCAST_PROBES;
            this.arm(context, entry, now + RETRANS_TIME);
            this.probe(context, entry);
        }
        if (entry.waiting.size() >= QUEUE_LENGTH) {
            entry.waiting.removeFirst(); // the oldest is dropped, without report (SKB_DROP_REASON_NEIGH_QUEUEFULL)
        }
        entry.waiting.add(new Waiting(packet, failure));
    }

    private void transmit(Ipv4Stack.Context context, Entry entry, Ipv4Packet packet) {
        context.send(entry.link, new EthernetFrame(entry.link.mac(), entry.mac, packet));
    }

    // ---- Timers ----

    void onTimer(Ipv4Stack.Context context, Timer timer) {
        Entry entry = this.entries.get(new Key(timer.device(), timer.address()));
        if (entry == null || entry.generation != timer.generation() || entry.state == null || !entry.state.inTimer()) {
            return; // stale timer
        }
        long now = context.now();
        long next = now + RETRANS_TIME;
        if (entry.state == State.REACHABLE) {
            if (now < entry.confirmed + REACHABLE_TIME) {
                next = entry.confirmed + REACHABLE_TIME;
            } else if (now <= entry.used + DELAY_PROBE_TIME) {
                entry.state = State.DELAY;
                entry.updated = now;
                next = now + DELAY_PROBE_TIME;
            } else {
                entry.state = State.STALE;
                entry.updated = now;
            }
        } else if (entry.state == State.DELAY) {
            if (now <= entry.confirmed + DELAY_PROBE_TIME) {
                entry.state = State.REACHABLE;
                entry.updated = now;
                next = entry.confirmed + REACHABLE_TIME;
            } else {
                entry.state = State.PROBE;
                entry.updated = now;
                entry.probes = 0;
            }
        }
        if ((entry.state == State.INCOMPLETE || entry.state == State.PROBE) && entry.probes >= maxProbes(entry)) {
            entry.state = State.FAILED;
            entry.updated = now;
            entry.generation++;
            this.invalidate(entry);
            return;
        }
        if (entry.state.inTimer()) {
            this.arm(context, entry, next);
        }
        if (entry.state == State.INCOMPLETE || entry.state == State.PROBE) {
            this.probe(context, entry);
        }
    }

    /** {@code neigh_max_probes}: unicast plus broadcast requests while resolving, unicast only while probing. */
    private static int maxProbes(Entry entry) {
        return UCAST_PROBES + (entry.state == State.PROBE ? 0 : MCAST_PROBES);
    }

    /**
     * {@code neigh_probe} then {@code arp_solicit}: the request is sent with the count of the probes before this one,
     * then the count goes up. Unicast to the known address while that count is below {@code ucast_probes} (the
     * probes of {@code PROBE}, counted from 0), broadcast beyond (those of {@code INCOMPLETE}, counted from
     * {@code ucast_probes}). The source address is that of the last packet waiting, if it belongs to this machine,
     * otherwise the address of the interface ({@code arp_announce} 0).
     */
    private void probe(Ipv4Stack.Context context, Entry entry) {
        boolean unicast = entry.probes < UCAST_PROBES && entry.mac != null;
        entry.probes++;
        Optional<Ipv4Address> source = Optional.ofNullable(entry.waiting.isEmpty() ? null : entry.waiting.getLast())
                .map(waiting -> waiting.packet().source()).filter(this.stack::isLocal)
                .or(() -> this.stack.primaryAddress(entry.link));
        if (source.isEmpty()) {
            return; // no address to ask from
        }
        ArpPacket request = ArpPacket.request(entry.link.mac(), source.get(), entry.address);
        context.send(entry.link, new EthernetFrame(entry.link.mac(), unicast ? entry.mac : MacAddress.BROADCAST, request));
    }

    private void arm(Ipv4Stack.Context context, Entry entry, long due) {
        entry.generation++;
        context.schedule(Math.max(1L, due - context.now()), new Timer(entry.link.name(), entry.address, entry.generation));
    }

    /** {@code neigh_invalidate}: each waiting packet is reported lost. */
    private void invalidate(Entry entry) {
        List<Waiting> lost = List.copyOf(entry.waiting);
        entry.waiting.clear();
        for (Waiting waiting : lost) {
            waiting.failure().unreachable(waiting.packet());
        }
    }

    // ---- Receiving ----

    /** {@code arp_process}: an ARP packet reached the interface {@code in}. */
    void receive(Ipv4Stack.Context context, NetInterface in, EthernetFrame frame, ArpPacket arp) {
        if (arp.senderIp().isAny() || arp.senderMac().isMulticast()) {
            return;
        }
        if (arp.operation() == ArpPacket.Operation.REQUEST && this.stack.isLocal(arp.targetIp())) {
            // neigh_event_ns: the asker becomes a STALE neighbour, its address overriding the known one.
            Entry asker = this.entries.computeIfAbsent(new Key(in.name(), arp.senderIp()), key -> new Entry(in, arp.senderIp()));
            this.update(context, asker, arp.senderMac(), State.STALE, true);
            context.send(in, new EthernetFrame(in.mac(), arp.senderMac(), ArpPacket.replyTo(arp, in.mac())));
            return;
        }
        Entry known = this.entries.get(new Key(in.name(), arp.senderIp()));
        if (known == null || known.state == null) {
            return;
        }
        long now = context.now();
        boolean override = now > known.updated + LOCKTIME;
        boolean toThisMachine = frame.destination().equals(in.mac());
        State state = arp.operation() == ArpPacket.Operation.REPLY && toThisMachine ? State.REACHABLE : State.STALE;
        this.update(context, known, arp.senderMac(), state, override);
    }

    /** {@code __neigh_update} with a hardware address, for the states that hold one. */
    private void update(Ipv4Stack.Context context, Entry entry, MacAddress mac, State wanted, boolean override) {
        long now = context.now();
        State old = entry.state;
        boolean oldValid = old != null && old.valid();
        State state = wanted;
        boolean sameMac = oldValid && mac.equals(entry.mac);
        if (state == State.REACHABLE) {
            entry.confirmed = now;
        }
        if (oldValid) {
            if (!sameMac && !override) {
                return; // a different address within the lock time: the known one stays
            }
            if (sameMac && state == State.STALE) {
                state = old; // valid, same address: a STALE update changes nothing
            }
        }
        if (state != old || !sameMac) {
            entry.updated = now;
        }
        if (state != old) {
            entry.state = state;
            if (state == State.REACHABLE) {
                this.arm(context, entry, now + REACHABLE_TIME);
            } else {
                entry.generation++; // STALE has no timer
            }
        }
        if (!sameMac) {
            entry.mac = mac;
            if (state != State.REACHABLE) {
                entry.confirmed = now - 2 * REACHABLE_TIME;
            }
        }
        if (!oldValid) {
            // The address is known now: the packets waiting for it leave (neigh_update_process_arp_queue).
            List<Waiting> ready = List.copyOf(entry.waiting);
            entry.waiting.clear();
            ready.forEach(waiting -> this.transmit(context, entry, waiting.packet()));
        }
    }

    /** The carrier of the interface is lost: its entries are forgotten, their waiting packets with them. */
    void flush(NetInterface link) {
        this.entries.values().removeIf(entry -> entry.link == link);
    }
}
