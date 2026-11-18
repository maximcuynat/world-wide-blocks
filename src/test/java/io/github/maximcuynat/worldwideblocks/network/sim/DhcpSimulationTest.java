package io.github.maximcuynat.worldwideblocks.network.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.DeviceRole;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.LinkMedium;
import io.github.maximcuynat.worldwideblocks.network.NetworkGraph;
import io.github.maximcuynat.worldwideblocks.network.NetworkNode;
import io.github.maximcuynat.worldwideblocks.network.PortKind;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClient;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClientState;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpFrames;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpLease;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpStore;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessageType;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/** Computers and boxes exchanging real DHCP frames through the simulation. */
class DhcpSimulationTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final List<PortKind> BOX_PORTS =
            List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE);
    private static final long LEASE = 24_000L;

    private record Sent(long now, PortRef port, MacAddress source, DhcpMessageType type) {
    }

    private NetworkGraph graph;
    private DhcpStore store;
    private NetworkSimulation sim;
    private final List<Sent> dhcpSent = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.graph = new NetworkGraph(new SplittableRandom(11));
        this.store = new DhcpStore();
        this.sim = new NetworkSimulation(this.graph, this.store, SimulationLimits.DEFAULT);
        this.sim.setTap((now, port, direction, frame) -> {
            if (direction == FrameDirection.SENT) {
                DhcpFrames.toServer(frame).or(() -> DhcpFrames.toClient(frame))
                        .ifPresent(message -> this.dhcpSent.add(new Sent(now, port, frame.source(), message.type())));
            }
        });
    }

    private DeviceId host(int x) {
        NetworkNode node = new NetworkNode(new DeviceId(OVERWORLD, x, 64, 0), "worldwideblocks:pc_tower", 1);
        this.graph.putNode(node);
        this.sim.deviceAdded(node);
        return node.id();
    }

    private DeviceId box(int x) {
        NetworkNode node = new NetworkNode(new DeviceId(OVERWORLD, x, 64, 0), "worldwideblocks:internet_box", DeviceRole.BOX, BOX_PORTS);
        this.graph.putNode(node);
        this.sim.deviceAdded(node);
        return node.id();
    }

    private void plug(DeviceId a, int portA, DeviceId b, int portB) {
        Link link = this.graph.connect(new PortRef(a, portA), new PortRef(b, portB), LinkMedium.COPPER_ETHERNET, true)
                .link().orElseThrow();
        this.sim.linkUp(link);
    }

    private void unplug(DeviceId device, int port) {
        this.sim.linkDown(this.graph.disconnect(new PortRef(device, port)).orElseThrow());
    }

    private DhcpClient client(DeviceId host) {
        return ((HostLogic) this.sim.logic(host).orElseThrow()).dhcpClient(0);
    }

    private Optional<Ipv4Address> address(DeviceId host) {
        return this.client(host).lease().map(DhcpLease::address);
    }

    private void tickUntil(long from, long to) {
        for (long t = from; t <= to; t++) {
            this.sim.tick(t);
        }
    }

    private List<Long> sentBy(DeviceId device, DhcpMessageType type) {
        return this.dhcpSent.stream().filter(s -> s.port().device().equals(device) && s.type() == type)
                .map(Sent::now).toList();
    }

    @Test
    void aComputerPluggedIntoTheBoxGetsItsAddressInTheSameTick() {
        DeviceId box = box(0);
        DeviceId tower = host(5);
        plug(box, 0, tower, 0);

        sim.tick(0);

        assertEquals(DhcpClientState.BOUND, client(tower).state());
        DhcpLease lease = client(tower).lease().orElseThrow();
        assertEquals(Ipv4Address.of(192, 168, 1, 10), lease.address());
        assertEquals(Ipv4Address.of(255, 255, 255, 0), lease.subnetMask());
        assertEquals(Ipv4Address.of(192, 168, 1, 1), lease.router());
        assertEquals(Ipv4Address.of(192, 168, 1, 1), lease.dnsServer());
        assertEquals(graph.serviceMacOf(box).orElseThrow(), lease.serverMac());
        assertEquals(LEASE, lease.expiresAt());
        // The four messages of RFC 2131, 3.1, in order.
        assertEquals(List.of(DhcpMessageType.DISCOVER, DhcpMessageType.OFFER, DhcpMessageType.REQUEST, DhcpMessageType.ACK),
                dhcpSent.stream().filter(s -> s.port().device().equals(tower) || s.port().device().equals(box))
                        .map(Sent::type).distinct().toList());
        assertEquals(1, store.server(box).leasesAt(0).size());
    }

    @Test
    void twoComputersGetTwoAddresses() {
        DeviceId box = box(0);
        DeviceId tower = host(5);
        DeviceId laptop = host(10);
        plug(box, 0, tower, 0);
        plug(box, 1, laptop, 0);

        sim.tick(0);

        assertEquals(Optional.of(Ipv4Address.of(192, 168, 1, 10)), address(tower));
        assertEquals(Optional.of(Ipv4Address.of(192, 168, 1, 11)), address(laptop));
    }

    @Test
    void unpluggedTheComputerLosesItsAddressAndGetsTheSameOneBackWhenPluggedAgain() {
        DeviceId box = box(0);
        DeviceId tower = host(5);
        DeviceId laptop = host(10);
        plug(box, 0, tower, 0);
        sim.tick(0);
        Ipv4Address first = address(tower).orElseThrow();

        unplug(box, 0);
        sim.tick(1);
        assertTrue(address(tower).isEmpty());
        assertEquals(DhcpClientState.UNPLUGGED, client(tower).state());

        plug(box, 1, laptop, 0); // the laptop does not take the tower's address
        sim.tick(2);
        plug(box, 2, tower, 0);
        sim.tick(3);
        assertEquals(Optional.of(first), address(tower));
        assertTrue(!address(laptop).orElseThrow().equals(first));
    }

    @Test
    void theRenewalHappensAtHalfOfTheLeaseAndExtendsIt() {
        DeviceId box = box(0);
        DeviceId tower = host(5);
        plug(box, 0, tower, 0);

        tickUntil(0, LEASE / 2);

        assertEquals(List.of(0L, LEASE / 2), sentBy(tower, DhcpMessageType.REQUEST), "REQUEST at 0, renewal at T1");
        assertEquals(List.of(0L, LEASE / 2), sentBy(box, DhcpMessageType.ACK));
        assertEquals(DhcpClientState.BOUND, client(tower).state());
        assertEquals(LEASE / 2 + LEASE, client(tower).lease().orElseThrow().expiresAt());
        assertEquals(Optional.of(Ipv4Address.of(192, 168, 1, 10)), address(tower));
    }

    @Test
    void withAServerThatStopsAnsweringTheAddressIsLostAtTheEndOfTheLeaseExactly() {
        DeviceId box = box(0);
        DeviceId tower = host(5);
        plug(box, 0, tower, 0);
        sim.tick(0);
        store.server(box).setEnabled(false);

        tickUntil(1, LEASE - 1);
        assertTrue(address(tower).isPresent());
        assertEquals(DhcpClientState.REBINDING, client(tower).state());

        sim.tick(LEASE);
        assertTrue(address(tower).isEmpty());
        assertEquals(DhcpClientState.SELECTING, client(tower).state());
        assertEquals(List.of(0L, LEASE), sentBy(tower, DhcpMessageType.DISCOVER));
    }

    @Test
    void withoutABoxTwoComputersStayWithoutAddressAndKeepLooking() {
        DeviceId tower = host(0);
        DeviceId laptop = host(5);
        plug(tower, 0, laptop, 0);

        tickUntil(0, 5_040);

        assertTrue(address(tower).isEmpty());
        assertEquals(DhcpClientState.SELECTING, client(tower).state());
        assertEquals(List.of(0L, 80L, 240L, 560L, 1_200L, 2_480L, 3_760L, 5_040L), sentBy(tower, DhcpMessageType.DISCOVER));
    }

    @Test
    void aComputerPluggedOnASecondBoxWithItsServerDisabledGetsItsAddressFromTheFirst() {
        DeviceId first = box(0);
        DeviceId second = box(20);
        DeviceId tower = host(25);
        plug(first, 0, second, 0);
        plug(second, 1, tower, 0);
        store.server(second).setEnabled(false);

        sim.tick(0);

        assertEquals(graph.serviceMacOf(first).orElseThrow(), client(tower).lease().orElseThrow().serverMac());
        MacAddress secondServer = graph.serviceMacOf(second).orElseThrow();
        assertTrue(dhcpSent.stream().noneMatch(s -> s.source().equals(secondServer)), "the second box relays, it does not answer");
    }
}
