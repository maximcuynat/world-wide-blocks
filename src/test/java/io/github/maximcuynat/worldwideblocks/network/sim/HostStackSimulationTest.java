package io.github.maximcuynat.worldwideblocks.network.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
import io.github.maximcuynat.worldwideblocks.network.ip.InterfaceAddress;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.os.ConsoleSession;

/** The IPv4 stack of the computers and of the box, configured by real DHCP exchanges, read with ip in the console. */
class HostStackSimulationTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final List<PortKind> BOX_PORTS =
            List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE);

    private NetworkGraph graph;
    private NetworkSimulation sim;
    private DeviceId box;
    private DeviceId tower;
    private long now;

    @BeforeEach
    void setUp() {
        this.graph = new NetworkGraph(new SplittableRandom(11));
        this.sim = new NetworkSimulation(this.graph, SimulationLimits.DEFAULT);
        NetworkNode boxNode = new NetworkNode(new DeviceId(OVERWORLD, 0, 64, 0), "worldwideblocks:internet_box", DeviceRole.BOX, BOX_PORTS);
        NetworkNode towerNode = new NetworkNode(new DeviceId(OVERWORLD, 2, 64, 0), "worldwideblocks:pc_tower", 1);
        this.graph.putNode(boxNode);
        this.graph.putNode(towerNode);
        this.box = boxNode.id();
        this.tower = towerNode.id();
        this.graph.connect(new PortRef(this.box, 0), new PortRef(this.tower, 0), LinkMedium.COPPER_ETHERNET, true);
        this.sim.tick(0); // the DHCP exchange runs in this tick: the frames are delivered at once
    }

    private void tickUntil(long time) {
        while (this.now < time) {
            this.sim.tick(++this.now);
        }
    }

    private ConsoleSession console() {
        return ((HostLogic) this.sim.logic(this.tower).orElseThrow()).system().localConsole();
    }

    private List<String> type(String line) {
        int before = this.console().lines().size();
        this.sim.consoleLine(this.tower, line);
        this.tickUntil(this.now + 1);
        List<String> lines = this.console().lines();
        return lines.subList(before, lines.size());
    }

    @Test
    void theBoxHasItsPermanentAddressOnItsComputer() {
        Ipv4Stack stack = ((BoxLogic) this.sim.logic(this.box).orElseThrow()).stack();

        InterfaceAddress address = stack.interfaceNamed("br0").orElseThrow().addresses().getFirst();
        assertEquals(Ipv4Address.of(192, 168, 1, 1), address.address());
        assertTrue(address.permanent());
    }

    @Test
    void theLeaseGivesTheAddressAndTheRoutesOfEth0() {
        List<String> addr = this.type("ip a show eth0");
        List<String> route = this.type("ip r");

        assertTrue(addr.get(0).startsWith("2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_codel state UP"), addr.get(0));
        assertEquals("    inet 192.168.1.10/24 brd 192.168.1.255 scope global dynamic eth0", addr.get(2));
        // Lease of 1,200 s obtained at tick 0, read at tick 1: 1,199.95 s left, written 1200sec.
        assertEquals("       valid_lft 1200sec preferred_lft 1200sec", addr.get(3));
        assertEquals(List.of("default via 192.168.1.1 dev eth0 ",
                "192.168.1.0/24 dev eth0 proto kernel scope link src 192.168.1.10 "), route);
    }

    @Test
    void theLineIsReadAtTheNextTick() {
        this.sim.consoleLine(this.tower, "ip r");

        assertTrue(this.console().lines().isEmpty());
        this.tickUntil(1);
        assertEquals(2, this.console().lines().size());
    }

    @Test
    void unpluggedTheInterfaceLosesItsCarrierItsAddressAndItsRoutes() {
        this.sim.linkDown(this.graph.disconnect(new PortRef(this.tower, 0)).orElseThrow());
        this.tickUntil(1);

        assertEquals(List.of(
                "2: eth0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc fq_codel state DOWN group default qlen 1000"),
                this.type("ip a show eth0").subList(0, 1));
        assertEquals(2, this.type("ip a show eth0").size(), "no inet line");
        assertEquals(List.of(), this.type("ip route"));
    }

    @Test
    void theAddressComesBackWhenTheCableIsPluggedAgain() {
        this.sim.linkDown(this.graph.disconnect(new PortRef(this.tower, 0)).orElseThrow());
        this.tickUntil(1);
        Link link = this.graph.connect(new PortRef(this.box, 0), new PortRef(this.tower, 0), LinkMedium.COPPER_ETHERNET, true)
                .link().orElseThrow();
        this.sim.linkUp(link);
        this.tickUntil(2);

        assertEquals("    inet 192.168.1.10/24 brd 192.168.1.255 scope global dynamic eth0", this.type("ip a show eth0").get(2));
    }

    @Test
    void aLineForADeviceGoneBeforeTheTickIsIgnored() {
        this.graph.removeDevice(this.tower).forEach(this.sim::linkDown);
        this.sim.deviceRemoved(this.tower);
        this.sim.consoleLine(this.tower, "ip a");

        this.tickUntil(1);

        assertTrue(this.sim.logic(this.tower).isEmpty());
    }
}
