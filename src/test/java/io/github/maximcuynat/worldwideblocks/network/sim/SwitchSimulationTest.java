package io.github.maximcuynat.worldwideblocks.network.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.DeviceRole;
import io.github.maximcuynat.worldwideblocks.network.LinkMedium;
import io.github.maximcuynat.worldwideblocks.network.NetworkGraph;
import io.github.maximcuynat.worldwideblocks.network.NetworkNode;
import io.github.maximcuynat.worldwideblocks.network.PortKind;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpStore;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.switching.MacTable;
import io.github.maximcuynat.worldwideblocks.os.ConsoleSession;

/**
 * A desktop switch in a chain box, switch, computers (issue #17), through the real engine: the computers behind the
 * switch get their address from the box, the switch learns each computer behind its port and the box behind the port
 * that goes to it, a computer behind the switch pings a computer of the box, and unplugging a port makes the switch
 * forget what it learned there. The switch itself has no address and sends nothing.
 */
class SwitchSimulationTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final List<PortKind> BOX_PORTS =
            List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE);

    private NetworkGraph graph;
    private NetworkSimulation sim;
    private DeviceId box;
    private DeviceId switch5;
    /** Computers on ports 2 to 5 of the switch (engine ports 1 to 4). */
    private final List<DeviceId> behindSwitch = new ArrayList<>();
    /** Computer on port 2 of the box. */
    private DeviceId onBox;
    private long now;

    @BeforeEach
    void setUp() {
        this.graph = new NetworkGraph(new SplittableRandom(5));
        this.sim = new NetworkSimulation(this.graph, new DhcpStore(), SimulationLimits.DEFAULT);
        NetworkNode boxNode = new NetworkNode(new DeviceId(OVERWORLD, 0, 64, 0), "worldwideblocks:internet_box", DeviceRole.BOX, BOX_PORTS);
        NetworkNode switchNode = new NetworkNode(new DeviceId(OVERWORLD, 4, 64, 0), "worldwideblocks:desktop_switch_5",
                DeviceRole.SWITCH, PortKind.ethernet(5));
        this.graph.putNode(boxNode);
        this.graph.putNode(switchNode);
        this.box = boxNode.id();
        this.switch5 = switchNode.id();
        this.graph.connect(new PortRef(this.box, 0), new PortRef(this.switch5, 0), LinkMedium.COPPER_ETHERNET, true);
        for (int port = 1; port <= 4; port++) {
            NetworkNode host = new NetworkNode(new DeviceId(OVERWORLD, 4 + port, 64, 3), "worldwideblocks:pc_tower", 1);
            this.graph.putNode(host);
            this.behindSwitch.add(host.id());
            this.graph.connect(new PortRef(this.switch5, port), new PortRef(host.id(), 0), LinkMedium.COPPER_ETHERNET, true);
        }
        NetworkNode laptop = new NetworkNode(new DeviceId(OVERWORLD, -3, 64, 0), "worldwideblocks:laptop", 1);
        this.graph.putNode(laptop);
        this.onBox = laptop.id();
        this.graph.connect(new PortRef(this.box, 1), new PortRef(this.onBox, 0), LinkMedium.COPPER_ETHERNET, true);
        this.tickUntil(40);
    }

    private void tickUntil(long time) {
        while (this.now < time) {
            this.sim.tick(++this.now);
        }
    }

    private HostLogic host(DeviceId device) {
        return (HostLogic) this.sim.logic(device).orElseThrow();
    }

    private Ipv4Address address(DeviceId device) {
        Ipv4Stack stack = this.host(device).system().stack();
        return stack.primaryAddress(stack.interfaceNamed("eth0").orElseThrow()).orElseThrow();
    }

    private List<MacTable.Entry> learned() {
        return this.sim.logic(this.switch5).orElseThrow().learnedMacs();
    }

    @Test
    void theSwitchIsABridgeWithoutService() {
        DeviceLogic logic = this.sim.logic(this.switch5).orElseThrow();
        assertTrue(logic instanceof SwitchLogic);
        assertTrue(logic.bridges());
        assertFalse(DeviceRole.SWITCH.hasService());
        assertEquals(5, this.graph.macsOf(this.switch5).size(), "one MAC per port, none for a service");
    }

    @Test
    void computersBehindTheSwitchGetTheirAddressFromTheBox() {
        Set<Ipv4Address> addresses = new HashSet<>();
        for (DeviceId host : this.behindSwitch) {
            Ipv4Address address = this.address(host);
            assertTrue(address.toString().startsWith("192.168.1."), "address of a computer behind the switch: " + address);
            addresses.add(address);
        }
        addresses.add(this.address(this.onBox));
        assertEquals(5, addresses.size(), "five different addresses");
    }

    @Test
    void theSwitchLearnsEachComputerBehindItsPort() {
        for (int port = 1; port <= 4; port++) {
            MacAddress mac = this.graph.macsOf(this.behindSwitch.get(port - 1)).getFirst();
            int expected = port;
            assertTrue(this.learned().stream().anyMatch(entry -> entry.mac().equals(mac) && entry.port() == expected),
                    "computer of port " + (port + 1) + " learned behind it: " + this.learned());
        }
        // The DHCP server of the box answered: its MAC is behind port 1 of the switch, with the computer of the box.
        assertTrue(this.learned().stream().filter(entry -> entry.port() == 0).count() >= 1, "the box learned behind port 1");
    }

    @Test
    void aComputerBehindTheSwitchPingsAComputerOfTheBox() {
        ConsoleSession console = this.host(this.behindSwitch.get(2)).system().localConsole();
        int before = console.lines().size();
        this.sim.consoleLine(this.behindSwitch.get(2), "ping -c 1 " + this.address(this.onBox));
        this.tickUntil(this.now + 60);
        List<String> lines = console.lines().subList(before, console.lines().size());
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("64 bytes from " + this.address(this.onBox) + ": icmp_seq=1 ttl=64")),
                "reply received: " + lines);
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("1 packets transmitted, 1 received, 0% packet loss")), "statistics: " + lines);
    }

    @Test
    void unpluggingAPortForgetsWhatWasLearnedBehindIt() {
        MacAddress mac = this.graph.macsOf(this.behindSwitch.getFirst()).getFirst();
        this.graph.disconnect(new PortRef(this.switch5, 1)).ifPresent(this.sim::linkDown);
        this.tickUntil(this.now + 1);
        assertFalse(this.learned().stream().anyMatch(entry -> entry.mac().equals(mac)), "computer still learned: " + this.learned());
    }
}
