package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.NetworkGraph;
import io.github.maximcuynat.worldwideblocks.network.NetworkNode;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpStore;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * Moves real Ethernet frames between the devices of the graph.
 *
 * <p>A frame sent on a port is placed in a queue, then delivered to the other end of the
 * cable when the queue is processed, on every server tick: it arrives instantly, with no
 * travel time along the cable. Frames produced during this processing (switch relays,
 * replies) are delivered in the same tick, within the limit of {@link SimulationLimits#maxDeliveriesPerTick()}.
 *
 * <p>Loop protection, until the STP of lot 8b: each sent frame gets a
 * trace, shared by all its relayed copies. A trace exists only in the engine, not in the
 * frame. If a copy comes back to a switch the trace already crossed, there are two paths between
 * them: the copy is dropped and the loop reported. In a network without a loop, this never happens.
 *
 * <p>Clock: the simulation time is that of the tick being processed. A change of the
 * graph (connection, disconnection, device placed or removed) often happens between two ticks:
 * a player click is handled after the simulation tick. It is therefore queued and applied
 * at the start of the next tick, with the time of that tick. Thus its frames, its timers and its
 * reports start from the very tick where its frames are delivered. (Applied immediately with the time
 * of the previous tick, a presence sent on connection was delivered one tick after its date: the
 * following presences fell 1,199 ticks after the loop report instead of 1,200.)
 *
 * <p>The runtime state (device behavior, queue, timers) is not saved: at
 * server start, each device starts from zero and each existing cable is announced as
 * connected, as after a power cut. Only what the devices remember of DHCP is saved ({@link DhcpStore}),
 * like the memory of a real box.
 *
 * <p>Plain Java, no Minecraft class. Not thread-safe: access it only from the server thread.
 */
public final class NetworkSimulation {
    private final NetworkGraph graph;
    private final DhcpStore dhcp;
    private final SimulationLimits limits;
    private final Map<DeviceId, DeviceLogic> logics = new HashMap<>();
    private final ArrayDeque<Delivery> queue = new ArrayDeque<>();
    /** Graph changes that happened since the last tick, applied in order on the next tick. */
    private final ArrayDeque<Runnable> pendingChanges = new ArrayDeque<>();
    private final PriorityQueue<Timer> timers = new PriorityQueue<>(
            Comparator.comparingLong(Timer::due).thenComparingLong(Timer::sequence));
    private final List<SimulationEvent> events = new ArrayList<>();
    /** Ports where a looping frame came back during the current tick; grouped by network at the end of the tick. */
    private final Set<SimulationEvent.LoopPoint> loopPoints = new LinkedHashSet<>();
    private boolean saturatedThisTick;
    private FrameTap tap = FrameTap.NONE;
    private long now;
    private long timerSequence;
    private boolean started;

    /** A frame on its way to the other end of a cable. */
    private record Delivery(PortRef ingress, UUID linkId, EthernetFrame frame, Trace trace) {
    }

    /** One emission and all its relayed copies: the switches they have already crossed. */
    private static final class Trace {
        private final Set<DeviceId> visited = new HashSet<>();
    }

    private record Timer(long due, long sequence, DeviceId device, DeviceLogic logic, Object key) {
    }

    public NetworkSimulation(NetworkGraph graph, DhcpStore dhcp, SimulationLimits limits) {
        this.graph = graph;
        this.dhcp = dhcp;
        this.limits = limits;
    }

    public NetworkSimulation(NetworkGraph graph, DhcpStore dhcp) {
        this(graph, dhcp, SimulationLimits.DEFAULT);
    }

    /** Simulation whose DHCP memory is not saved (tests). */
    public NetworkSimulation(NetworkGraph graph, SimulationLimits limits) {
        this(graph, new DhcpStore(), limits);
    }

    public void setTap(FrameTap tap) {
        this.tap = tap;
    }

    // ---- Reading ----

    public SimulationLimits limits() {
        return this.limits;
    }

    public Optional<DeviceLogic> logic(DeviceId device) {
        return Optional.ofNullable(this.logics.get(device));
    }

    /** Frames waiting for delivery. */
    public int pendingFrames() {
        return this.queue.size();
    }

    /** Events that happened since the last call, to turn into messages. */
    public List<SimulationEvent> drainEvents() {
        List<SimulationEvent> drained = List.copyOf(this.events);
        this.events.clear();
        return drained;
    }

    // ---- Graph changes ----
    // Applied at the start of the next tick, with its time (see the class). Before the first tick, they
    // are ignored: startup rereads the whole graph.
    //
    // A pending change only applies to what still exists in the graph at the time of the
    // tick: a device or cable removed in the meantime is already replaced by its removal, also
    // pending. (A computer placed, cabled and removed in the same tick used to crash the server.)

    /** A device was just added, or replaced by another one (its old behavior is forgotten). */
    public void deviceAdded(NetworkNode node) {
        this.later(() -> {
            if (this.graph.node(node.id()).filter(node::equals).isPresent()) {
                this.start(node);
            }
        });
    }

    public void deviceRemoved(DeviceId device) {
        this.later(() -> {
            this.logics.remove(device);
        });
    }

    public void linkUp(Link link) {
        this.later(() -> this.notifyLink(link, true));
    }

    /** To call after the cable was removed from the graph. */
    public void linkDown(Link link) {
        this.later(() -> this.notifyLink(link, false));
    }

    private void later(Runnable change) {
        if (this.started) {
            this.pendingChanges.add(change);
        }
    }

    // ---- Tick ----

    /**
     * Advances the simulation to {@code now}: graph changes that happened since the previous
     * tick, expired timers, then frame delivery.
     *
     * @param now game time, in ticks; it must never go backwards
     */
    public void tick(long now) {
        this.now = now;
        if (!this.started) {
            this.started = true;
            this.graph.nodes().forEach(this::start);
            this.graph.links().forEach(link -> this.notifyLink(link, true));
        }
        while (!this.pendingChanges.isEmpty()) {
            this.pendingChanges.poll().run();
        }
        while (!this.timers.isEmpty() && this.timers.peek().due() <= now) {
            Timer timer = this.timers.poll();
            // A removed or replaced device has another behavior: its timers are stale.
            if (this.logics.get(timer.device()) == timer.logic()) {
                timer.logic().onTimer(new Context(timer.device(), timer.logic(), null), timer.key());
            }
        }
        int budget = this.limits.maxDeliveriesPerTick();
        while (budget > 0 && !this.queue.isEmpty()) {
            budget--;
            this.deliver(this.queue.poll());
        }
        this.flushReports();
    }

    /**
     * End of the tick: one event per network where a loop was seen, whatever the number of switches
     * that saw it, and at most one saturation event. The rate limit (once per
     * minute) is not here: it belongs to the message module, which decides whom to notify and when.
     */
    private void flushReports() {
        if (!this.loopPoints.isEmpty()) {
            Map<DeviceId, DeviceId> networkOf = new HashMap<>();
            Map<DeviceId, List<SimulationEvent.LoopPoint>> byNetwork = new TreeMap<>(DeviceId.ORDER);
            for (SimulationEvent.LoopPoint point : this.loopPoints) {
                DeviceId network = networkOf.computeIfAbsent(point.device(), this.graph::networkKey);
                byNetwork.computeIfAbsent(network, key -> new ArrayList<>()).add(point);
            }
            byNetwork.forEach((network, points) -> this.events.add(new SimulationEvent.LoopDetected(network, points)));
            this.loopPoints.clear();
        }
        if (this.saturatedThisTick) {
            this.events.add(new SimulationEvent.QueueSaturated());
            this.saturatedThisTick = false;
        }
    }

    // ---- Internal ----

    private void start(NetworkNode node) {
        DeviceLogic logic = DeviceLogics.create(node, this.dhcp);
        this.logics.put(node.id(), logic);
        logic.onStart(new Context(node.id(), logic, null));
    }

    private void notifyLink(Link link, boolean up) {
        if (up && !this.isInGraph(link)) {
            return; // unplugged since: its unplugging follows in the queue
        }
        for (PortRef end : List.of(link.a(), link.b())) {
            DeviceLogic logic = this.logics.get(end.device());
            if (logic == null || this.graph.node(end.device()).isEmpty()) {
                continue; // device removed since: its behavior is about to be forgotten
            }
            Context context = new Context(end.device(), logic, null);
            if (up) {
                logic.onLinkUp(context, end.index());
            } else {
                logic.onLinkDown(context, end.index());
            }
        }
    }

    private boolean isInGraph(Link link) {
        return this.graph.linkAt(link.a()).filter(current -> current.id().equals(link.id())).isPresent();
    }

    private void emit(PortRef egress, EthernetFrame frame, Trace trace) {
        this.tap.onFrame(this.now, egress, FrameDirection.SENT, frame);
        Optional<Link> link = this.graph.linkAt(egress);
        if (link.isEmpty()) {
            return; // no cable on this port: the frame is lost
        }
        if (this.queue.size() >= this.limits.maxQueued()) {
            this.saturatedThisTick = true;
            return;
        }
        this.queue.add(new Delivery(link.get().otherEnd(egress), link.get().id(), frame, trace));
    }

    private void deliver(Delivery delivery) {
        PortRef ingress = delivery.ingress();
        Optional<Link> link = this.graph.linkAt(ingress);
        if (link.isEmpty() || !link.get().id().equals(delivery.linkId())) {
            return; // cable removed (or replaced) before delivery
        }
        DeviceLogic logic = this.logics.get(ingress.device());
        if (logic == null) {
            return;
        }
        this.tap.onFrame(this.now, ingress, FrameDirection.RECEIVED, delivery.frame());
        if (logic.bridges() && !delivery.trace().visited.add(ingress.device())) {
            this.loopPoints.add(new SimulationEvent.LoopPoint(ingress.device(), ingress.index()));
            return;
        }
        logic.onFrame(new Context(ingress.device(), logic, delivery.trace()), ingress.index(), delivery.frame());
    }


    /** Context of a device during a call; {@code trace} is the current frame, if there is one. */
    private final class Context implements DeviceContext {
        private final DeviceId device;
        private final DeviceLogic logic;
        private final Trace trace;

        private Context(DeviceId device, DeviceLogic logic, Trace trace) {
            this.device = device;
            this.logic = logic;
            this.trace = trace;
        }

        @Override
        public long now() {
            return NetworkSimulation.this.now;
        }

        @Override
        public DeviceId device() {
            return this.device;
        }

        @Override
        public MacAddress mac(int port) {
            return graph.macOf(new PortRef(this.device, port))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown port " + port + " on " + this.device));
        }

        @Override
        public MacAddress serviceMac() {
            return graph.serviceMacOf(this.device)
                    .orElseThrow(() -> new IllegalStateException("No service on " + this.device));
        }

        @Override
        public boolean isLinked(int port) {
            return graph.linkAt(new PortRef(this.device, port)).isPresent();
        }

        @Override
        public void send(int port, EthernetFrame frame) {
            Trace fresh = new Trace();
            if (this.logic.bridges()) {
                fresh.visited.add(this.device);
            }
            emit(new PortRef(this.device, port), frame, fresh);
        }

        @Override
        public void relay(int port, EthernetFrame frame) {
            if (this.trace == null) {
                throw new IllegalStateException("No frame being relayed");
            }
            emit(new PortRef(this.device, port), frame, this.trace);
        }

        @Override
        public void report(SimulationEvent event) {
            events.add(event);
        }

        @Override
        public void schedule(long delayTicks, Object key) {
            if (delayTicks <= 0) {
                throw new IllegalArgumentException("Non-positive delay: " + delayTicks);
            }
            timers.add(new Timer(NetworkSimulation.this.now + delayTicks, timerSequence++, this.device, this.logic, key));
        }
    }
}
