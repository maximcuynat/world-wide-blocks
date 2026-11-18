package io.github.maximcuynat.worldwideblocks.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * Network graph: the devices (nodes) and the cables (links) that connect their ports.
 *
 * <p>This is the core of the engine. It is plain Java, with no Minecraft class, to be tested
 * without launching the game. It knows nothing about chunks: a link exists until it is removed.
 * It describes the hardware (devices, ports, MAC addresses, cables); the frames traveling through it
 * are the simulation's business.
 *
 * <p>Each port gets a MAC address when its device is first registered. It stays with it
 * as long as the device is unchanged; a device removed then placed again, or replaced, gets new ones.
 * A device with its own service ({@link DeviceRole#hasService()}) also gets a MAC for that service.
 *
 * <p>Not thread-safe: access it only from the server thread.
 */
public final class NetworkGraph {
    private final Map<DeviceId, NetworkNode> nodes = new LinkedHashMap<>();
    private final Map<UUID, Link> links = new LinkedHashMap<>();
    private final Map<PortRef, Link> linksByPort = new HashMap<>();
    private final Map<ChunkKey, Set<Link>> linksByChunk = new HashMap<>();
    private final Map<DeviceId, List<MacAddress>> macs = new HashMap<>();
    private final Map<DeviceId, MacAddress> serviceMacs = new HashMap<>();
    private final Set<MacAddress> usedMacs = new HashSet<>();
    private final RandomGenerator random;

    public NetworkGraph() {
        this(RandomGenerator.getDefault());
    }

    /** @param random source of the generated MAC addresses; a fixed seed makes tests reproducible */
    public NetworkGraph(RandomGenerator random) {
        this.random = random;
    }

    // ---- Reading ----

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

    /**
     * Designates the device's network: the set of devices linked to it by cables, itself
     * included, represented by the smallest of their identifiers ({@link DeviceId#ORDER}). Two
     * devices of the same network give the same key. An unknown device forms a network of its own.
     * Walks port by port, without scanning all the cables.
     */
    public DeviceId networkKey(DeviceId device) {
        DeviceId smallest = device;
        Set<DeviceId> seen = new HashSet<>();
        Deque<DeviceId> todo = new ArrayDeque<>();
        seen.add(device);
        todo.add(device);
        while (!todo.isEmpty()) {
            DeviceId current = todo.poll();
            if (DeviceId.ORDER.compare(current, smallest) < 0) {
                smallest = current;
            }
            NetworkNode node = this.nodes.get(current);
            if (node == null) {
                continue;
            }
            for (int i = 0; i < node.portCount(); i++) {
                Link link = this.linksByPort.get(node.port(i));
                if (link != null) {
                    DeviceId other = link.otherEnd(node.port(i)).device();
                    if (seen.add(other)) {
                        todo.add(other);
                    }
                }
            }
        }
        return smallest;
    }

    /** Cables with at least one end in this chunk. */
    public Collection<Link> linksTouching(ChunkKey chunk) {
        Set<Link> found = this.linksByChunk.get(chunk);
        return found == null ? List.of() : Collections.unmodifiableCollection(found);
    }

    /** First port without a cable on the device that accepts this medium, if one is left. */
    public Optional<PortRef> firstFreePort(DeviceId device, LinkMedium medium) {
        NetworkNode node = this.nodes.get(device);
        if (node == null) {
            return Optional.empty();
        }
        for (int i = 0; i < node.portCount(); i++) {
            PortRef port = node.port(i);
            if (medium.accepts(node.portKind(i)) && !this.linksByPort.containsKey(port)) {
                return Optional.of(port);
            }
        }
        return Optional.empty();
    }

    /** MAC addresses of the device's ports, in port order; empty if the device is unknown. */
    public List<MacAddress> macsOf(DeviceId device) {
        return this.macs.getOrDefault(device, List.of());
    }

    public Optional<MacAddress> macOf(PortRef port) {
        List<MacAddress> deviceMacs = this.macsOf(port.device());
        return port.index() >= 0 && port.index() < deviceMacs.size() ? Optional.of(deviceMacs.get(port.index())) : Optional.empty();
    }

    /** MAC of the device's own service (the DHCP server of a box); empty for a device without a service. */
    public Optional<MacAddress> serviceMacOf(DeviceId device) {
        return Optional.ofNullable(this.serviceMacs.get(device));
    }

    /** The device whose service has this MAC (the box of a DHCP server), if it is still in the graph. */
    public Optional<DeviceId> deviceWithServiceMac(MacAddress mac) {
        for (Map.Entry<DeviceId, MacAddress> entry : this.serviceMacs.entrySet()) {
            if (entry.getValue().equals(mac)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    // ---- Modification ----

    /**
     * Registers a device, or updates it.
     *
     * @return the removed links: if the device registered at this position changed (block replaced
     *         without its removal having been reported), its old cables no longer hold
     */
    public List<Link> putNode(NetworkNode node) {
        NetworkNode previous = this.nodes.put(node.id(), node);
        if (previous != null && previous.equals(node)) {
            return List.of();
        }
        List<Link> removed = previous == null ? List.of() : this.removeLinksOf(node.id());
        // New device, or replaced device: new addresses.
        this.releaseMacs(node.id());
        this.macs.put(node.id(), this.generateMacs(node.portCount()));
        if (node.role().hasService()) {
            this.serviceMacs.put(node.id(), this.generateMacs(1).getFirst());
        }
        return removed;
    }

    /**
     * Restores a device reread from the save, with its MAC addresses. If they are missing
     * (world saved before lot 1b, or before lot 1b-2 for the service MAC) or do not fit (count, duplicate,
     * group address), new ones are generated. The port MACs and the service MAC are checked separately:
     * a box of a lot 1b-1 world keeps its port MACs and only gets a new service MAC.
     *
     * @param savedServiceMac MAC of the device's service, if it has one and it was saved
     * @return true if all the saved addresses were taken over as they are, false if one was generated
     */
    public boolean restoreNode(NetworkNode node, List<MacAddress> savedMacs, Optional<MacAddress> savedServiceMac) {
        this.removeDevice(node.id());
        this.nodes.put(node.id(), node);
        boolean usable = savedMacs.size() == node.portCount()
                && savedMacs.stream().distinct().count() == savedMacs.size()
                && savedMacs.stream().noneMatch(mac -> mac.isMulticast() || this.usedMacs.contains(mac));
        List<MacAddress> assigned = usable ? List.copyOf(savedMacs) : this.generateMacs(node.portCount());
        this.usedMacs.addAll(assigned);
        this.macs.put(node.id(), assigned);
        if (!node.role().hasService()) {
            return usable;
        }
        boolean serviceUsable = savedServiceMac.filter(mac -> !mac.isMulticast() && !this.usedMacs.contains(mac)).isPresent();
        MacAddress service = serviceUsable ? savedServiceMac.get() : this.generateMacs(1).getFirst();
        this.usedMacs.add(service);
        this.serviceMacs.put(node.id(), service);
        return usable && serviceUsable;
    }

    /**
     * Connects a cable between two ports. The checks are made in this order: existing devices and
     * ports, ports compatible with the medium, distinct devices, same dimension,
     * distance, free ports.
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

    /** Removes the cable connected to this port, if there is one. */
    public Optional<Link> disconnect(PortRef port) {
        Link link = this.linksByPort.get(port);
        if (link == null) {
            return Optional.empty();
        }
        this.remove(link);
        return Optional.of(link);
    }

    /** Removes the device and all its cables. */
    public List<Link> removeDevice(DeviceId device) {
        this.nodes.remove(device);
        this.releaseMacs(device);
        return this.removeLinksOf(device);
    }

    /**
     * Restores a link reread from the save, without creating a new one.
     *
     * @return false if the link is inconsistent with the graph (unknown device, port in use…) and was ignored
     */
    public boolean restoreLink(Link link) {
        if (this.links.containsKey(link.id()) || this.check(link.a(), link.b(), link.medium()) != ConnectResult.OK) {
            return false;
        }
        this.add(link);
        return true;
    }

    // ---- Internal ----

    private ConnectResult check(PortRef a, PortRef b, LinkMedium medium) {
        NetworkNode nodeA = this.nodes.get(a.device());
        NetworkNode nodeB = this.nodes.get(b.device());
        if (nodeA == null || nodeB == null || !nodeA.hasPort(a.index()) || !nodeB.hasPort(b.index())) {
            return ConnectResult.NOT_A_DEVICE;
        }
        if (!medium.accepts(nodeA.portKind(a.index())) || !medium.accepts(nodeB.portKind(b.index()))) {
            return ConnectResult.INCOMPATIBLE_PORT;
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

    /** Fresh addresses, unique in the whole graph, and reserved. */
    private List<MacAddress> generateMacs(int count) {
        List<MacAddress> generated = new ArrayList<>(count);
        while (generated.size() < count) {
            MacAddress mac = MacAddress.randomLocal(this.random);
            if (this.usedMacs.add(mac)) {
                generated.add(mac);
            }
        }
        return List.copyOf(generated);
    }

    private void releaseMacs(DeviceId device) {
        List<MacAddress> released = this.macs.remove(device);
        if (released != null) {
            this.usedMacs.removeAll(released);
        }
        MacAddress service = this.serviceMacs.remove(device);
        if (service != null) {
            this.usedMacs.remove(service);
        }
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
