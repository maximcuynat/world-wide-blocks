package io.github.maximcuynat.worldwideblocks.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

class NetworkGraphTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";
    private static final LinkMedium COPPER = LinkMedium.COPPER_ETHERNET;

    private NetworkGraph graph;

    @BeforeEach
    void setUp() {
        this.graph = new NetworkGraph(new SplittableRandom(42));
    }

    /** Registers a 1-port device in the Overworld and returns its port 0. */
    private PortRef device(int x, int y, int z) {
        return this.device(OVERWORLD, x, y, z, 1);
    }

    private PortRef device(String dimension, int x, int y, int z, int ports) {
        DeviceId id = new DeviceId(dimension, x, y, z);
        this.graph.putNode(new NetworkNode(id, "worldwideblocks:pc_tower", ports));
        return new PortRef(id, 0);
    }

    @Nested
    class NetworkKey {

        @Test
        void aLoneDeviceIsItsOwnNetwork() {
            DeviceId alone = device(5, 64, 0).device();

            assertEquals(alone, graph.networkKey(alone));
            DeviceId unknown = new DeviceId(OVERWORLD, 99, 64, 0);
            assertEquals(unknown, graph.networkKey(unknown));
        }

        @Test
        void allLinkedDevicesGiveTheSameKeyTheSmallestId() {
            PortRef center = device(OVERWORLD, 10, 64, 0, 3);
            PortRef left = device(-4, 64, 0);
            PortRef right = device(20, 64, 0);
            graph.connect(new PortRef(center.device(), 0), left, COPPER, true);
            graph.connect(new PortRef(center.device(), 1), right, COPPER, true);
            DeviceId isolated = device(-50, 64, 0).device();

            for (PortRef port : List.of(center, left, right)) {
                assertEquals(left.device(), graph.networkKey(port.device()));
            }
            assertEquals(isolated, graph.networkKey(isolated));
        }

        @Test
        void anUnplugSplitsTheNetworkInTwo() {
            PortRef center = device(OVERWORLD, 10, 64, 0, 3);
            PortRef left = device(-4, 64, 0);
            PortRef right = device(20, 64, 0);
            graph.connect(new PortRef(center.device(), 0), left, COPPER, true);
            graph.connect(new PortRef(center.device(), 1), right, COPPER, true);

            graph.disconnect(left);

            assertEquals(left.device(), graph.networkKey(left.device()));
            assertEquals(center.device(), graph.networkKey(right.device()));
        }
    }

    @Nested
    class Connect {

        @Test
        void okCreatesTheLinkAndOccupiesBothPorts() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(10, 64, 0);

            ConnectOutcome outcome = graph.connect(a, b, COPPER, true);

            assertEquals(ConnectResult.OK, outcome.result());
            Link link = outcome.link().orElseThrow();
            assertEquals(a, link.a());
            assertEquals(b, link.b());
            assertTrue(link.cableConsumed());
            assertEquals(Optional.of(link), graph.linkAt(a));
            assertEquals(Optional.of(link), graph.linkAt(b));
            assertEquals(1, graph.links().size());
        }

        @Test
        void remembersThatNoCordWasConsumed() {
            Link link = graph.connect(device(0, 64, 0), device(1, 64, 0), COPPER, false).link().orElseThrow();

            assertFalse(link.cableConsumed());
        }

        @Test
        void notADeviceWhenAnEndIsUnknown() {
            PortRef a = device(0, 64, 0);
            PortRef unknown = new PortRef(new DeviceId(OVERWORLD, 5, 64, 0), 0);

            assertEquals(ConnectResult.NOT_A_DEVICE, graph.connect(a, unknown, COPPER, true).result());
            assertEquals(ConnectResult.NOT_A_DEVICE, graph.connect(unknown, a, COPPER, true).result());
            assertTrue(graph.links().isEmpty());
        }

        @Test
        void notADeviceWhenThePortDoesNotExist() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);

            assertEquals(ConnectResult.NOT_A_DEVICE, graph.connect(a, new PortRef(b.device(), 1), COPPER, true).result());
            assertEquals(ConnectResult.NOT_A_DEVICE, graph.connect(a, new PortRef(b.device(), -1), COPPER, true).result());
        }

        @Test
        void sameDeviceWhenBothEndsAreTheSameDevice() {
            PortRef port0 = device(OVERWORLD, 0, 64, 0, 2);
            PortRef port1 = new PortRef(port0.device(), 1);

            assertEquals(ConnectResult.SAME_DEVICE, graph.connect(port0, port1, COPPER, true).result());
            assertEquals(ConnectResult.SAME_DEVICE, graph.connect(port0, port0, COPPER, true).result());
        }

        @Test
        void otherDimensionWhenTheDevicesAreInTwoDimensions() {
            PortRef a = device(OVERWORLD, 0, 64, 0, 1);
            PortRef b = device(NETHER, 0, 64, 0, 1);

            assertEquals(ConnectResult.OTHER_DIMENSION, graph.connect(a, b, COPPER, true).result());
        }

        @Test
        void tooFarBeyondOneHundredBlocks() {
            PortRef a = device(0, 64, 0);
            PortRef atHundred = device(100, 64, 0);
            PortRef atHundredAndOne = device(0, 64, 101);

            assertEquals(ConnectResult.TOO_FAR, graph.connect(a, atHundredAndOne, COPPER, true).result());
            assertEquals(ConnectResult.OK, graph.connect(a, atHundred, COPPER, true).result(), "exactly 100 blocks stays allowed");
        }

        @Test
        void tooFarIsMeasuredInAStraightLine() {
            // 60 x 60 x 60: 103.9 blocks on the diagonal, every axis is under 100.
            assertEquals(ConnectResult.TOO_FAR, graph.connect(device(0, 0, 0), device(60, 60, 60), COPPER, true).result());
        }

        @Test
        void portInUseWhenOneOfTheTwoPortsIsAlreadyConnected() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            PortRef c = device(10, 64, 0);
            graph.connect(a, b, COPPER, true);

            assertEquals(ConnectResult.PORT_IN_USE, graph.connect(a, c, COPPER, true).result());
            assertEquals(ConnectResult.PORT_IN_USE, graph.connect(c, b, COPPER, true).result());
            assertEquals(1, graph.links().size());
        }
    }

    @Nested
    class Disconnect {

        @Test
        void removesTheLinkAndFreesBothPorts() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            Link link = graph.connect(a, b, COPPER, true).link().orElseThrow();

            assertEquals(Optional.of(link), graph.disconnect(b));
            assertTrue(graph.linkAt(a).isEmpty());
            assertTrue(graph.linkAt(b).isEmpty());
            assertTrue(graph.links().isEmpty());
        }

        @Test
        void returnsEmptyOnAFreePort() {
            assertTrue(graph.disconnect(device(0, 64, 0)).isEmpty());
        }

        @Test
        void aFreedPortAcceptsANewCable() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            PortRef c = device(10, 64, 0);
            graph.connect(a, b, COPPER, true);
            graph.disconnect(a);

            assertEquals(ConnectResult.OK, graph.connect(a, c, COPPER, true).result());
        }
    }

    @Nested
    class RemoveDevice {

        @Test
        void removesTheDeviceAndItsCablesWithoutTouchingTheOthers() {
            PortRef hub = device(OVERWORLD, 0, 64, 0, 2);
            PortRef a = device(5, 64, 0);
            PortRef b = device(10, 64, 0);
            PortRef c = device(15, 64, 0);
            PortRef d = device(20, 64, 0);
            Link toA = graph.connect(hub, a, COPPER, true).link().orElseThrow();
            Link toB = graph.connect(new PortRef(hub.device(), 1), b, COPPER, false).link().orElseThrow();
            Link other = graph.connect(c, d, COPPER, true).link().orElseThrow();

            List<Link> removed = graph.removeDevice(hub.device());

            assertEquals(2, removed.size());
            assertTrue(removed.contains(toA));
            assertTrue(removed.contains(toB));
            assertTrue(graph.node(hub.device()).isEmpty());
            assertTrue(graph.linkAt(a).isEmpty(), "the port on the other side is freed");
            assertTrue(graph.linkAt(b).isEmpty());
            assertEquals(List.of(other), List.copyOf(graph.links()));
        }

        @Test
        void aDeviceWithoutCableReturnsNothing() {
            PortRef a = device(0, 64, 0);

            assertTrue(graph.removeDevice(a.device()).isEmpty());
            assertTrue(graph.node(a.device()).isEmpty());
        }
    }

    @Nested
    class PutNode {

        @Test
        void registeringTheSameDeviceAgainKeepsItsCables() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            graph.connect(a, b, COPPER, true);

            assertTrue(graph.putNode(new NetworkNode(a.device(), "worldwideblocks:pc_tower", 1)).isEmpty());
            assertEquals(1, graph.links().size());
        }

        @Test
        void aDeviceReplacedWithoutRemovalLosesItsCables() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            Link link = graph.connect(a, b, COPPER, true).link().orElseThrow();

            List<Link> removed = graph.putNode(new NetworkNode(a.device(), "worldwideblocks:laptop", 1));

            assertEquals(List.of(link), removed);
            assertTrue(graph.linkAt(b).isEmpty());
        }
    }

    @Nested
    class FirstFreePort {

        @Test
        void givesTheFirstPortWithoutCable() {
            PortRef port0 = device(OVERWORLD, 0, 64, 0, 3);
            graph.connect(port0, device(5, 64, 0), COPPER, true);

            assertEquals(Optional.of(new PortRef(port0.device(), 1)), graph.firstFreePort(port0.device(), COPPER));
        }

        @Test
        void emptyWhenAllPortsAreTakenOrTheDeviceIsUnknown() {
            PortRef a = device(0, 64, 0);
            graph.connect(a, device(5, 64, 0), COPPER, true);

            assertTrue(graph.firstFreePort(a.device(), COPPER).isEmpty());
            assertTrue(graph.firstFreePort(new DeviceId(OVERWORLD, 99, 64, 99), COPPER).isEmpty());
        }
    }

    @Nested
    class RestoreLink {

        @Test
        void restoresAConsistentLinkWithItsId() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            Link saved = new Link(UUID.randomUUID(), a, b, COPPER, false);

            assertTrue(graph.restoreLink(saved));
            assertEquals(Optional.of(saved), graph.linkAt(a));
        }

        @Test
        void ignoresALinkToAnUnknownDeviceOrATakenPort() {
            PortRef a = device(0, 64, 0);
            PortRef b = device(5, 64, 0);
            PortRef c = device(10, 64, 0);
            PortRef unknown = new PortRef(new DeviceId(OVERWORLD, 50, 64, 0), 0);
            graph.restoreLink(new Link(UUID.randomUUID(), a, b, COPPER, true));

            assertFalse(graph.restoreLink(new Link(UUID.randomUUID(), a, unknown, COPPER, true)));
            assertFalse(graph.restoreLink(new Link(UUID.randomUUID(), c, b, COPPER, true)));
            assertEquals(1, graph.links().size());
        }
    }

    /** Box: 4 Ethernet sockets, then the line input. */
    private DeviceId box(int x, int y, int z) {
        DeviceId id = new DeviceId(OVERWORLD, x, y, z);
        graph.putNode(new NetworkNode(id, "worldwideblocks:internet_box", DeviceRole.BOX,
                List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE)));
        return id;
    }

    @Nested
    class PortTypes {

        @Test
        void theFirstFreePortSkipsTheLineInput() {
            DeviceId box = box(0, 64, 0);
            for (int i = 0; i < 4; i++) {
                assertEquals(Optional.of(new PortRef(box, i)), graph.firstFreePort(box, COPPER));
                graph.connect(new PortRef(box, i), device(10 + i, 64, 0), COPPER, true);
            }

            assertTrue(graph.firstFreePort(box, COPPER).isEmpty());
        }

        @Test
        void theEthernetCordIsRefusedOnTheLineInput() {
            DeviceId box = box(0, 64, 0);

            ConnectOutcome outcome = graph.connect(new PortRef(box, 4), device(5, 64, 0), COPPER, true);

            assertEquals(ConnectResult.INCOMPATIBLE_PORT, outcome.result());
            assertTrue(graph.links().isEmpty());
        }
    }

    @Nested
    class MacAddresses {

        @Test
        void oneMacPerPortAtFirstRegistrationLocallyAdministeredAndUnicast() {
            DeviceId box = box(0, 64, 0);

            List<MacAddress> macs = graph.macsOf(box);

            assertEquals(5, macs.size());
            assertEquals(5, new HashSet<>(macs).size());
            for (MacAddress mac : macs) {
                assertEquals(0x02, mac.octet(0));
                assertFalse(mac.isMulticast());
            }
            assertEquals(Optional.of(macs.get(2)), graph.macOf(new PortRef(box, 2)));
            assertTrue(graph.macOf(new PortRef(box, 5)).isEmpty());
        }

        @Test
        void registeringTheSameDeviceAgainKeepsItsMacs() {
            PortRef a = device(0, 64, 0);
            List<MacAddress> before = graph.macsOf(a.device());

            graph.putNode(new NetworkNode(a.device(), "worldwideblocks:pc_tower", 1));

            assertEquals(before, graph.macsOf(a.device()));
        }

        @Test
        void aDeviceRemovedThenPlacedAgainGetsNewMacs() {
            PortRef a = device(0, 64, 0);
            MacAddress before = graph.macOf(a).orElseThrow();

            graph.removeDevice(a.device());
            assertTrue(graph.macsOf(a.device()).isEmpty());
            device(0, 64, 0);

            assertNotEquals(before, graph.macOf(a).orElseThrow());
        }

        @Test
        void aReplacedDeviceGetsNewMacs() {
            PortRef a = device(0, 64, 0);
            MacAddress before = graph.macOf(a).orElseThrow();

            graph.putNode(new NetworkNode(a.device(), "worldwideblocks:laptop", 1));

            assertNotEquals(before, graph.macOf(a).orElseThrow());
        }

        @Test
        void aMacAlreadyTakenIsDrawnAgain() {
            // The random source returns the same value twice, then another one.
            long[] draws = {7L, 7L, 9L};
            int[] i = {0};
            RandomGenerator rigged = () -> draws[Math.min(i[0]++, draws.length - 1)];
            NetworkGraph g = new NetworkGraph(rigged);
            DeviceId a = new DeviceId(OVERWORLD, 0, 64, 0);
            DeviceId b = new DeviceId(OVERWORLD, 1, 64, 0);

            g.putNode(new NetworkNode(a, "worldwideblocks:pc_tower", 1));
            g.putNode(new NetworkNode(b, "worldwideblocks:pc_tower", 1));

            assertEquals(0x02_00_00_00_00_07L, g.macsOf(a).get(0).value());
            assertEquals(0x02_00_00_00_00_09L, g.macsOf(b).get(0).value());
        }

        @Test
        void uniqueMacsAcrossManyDevices() {
            Set<MacAddress> sightings = new HashSet<>();
            for (int x = 0; x < 200; x++) {
                DeviceId box = box(x, 64, 0);
                sightings.addAll(graph.macsOf(box));
            }
            assertEquals(200 * 5, sightings.size());
        }
    }

    @Nested
    class RestoreNode {
        private final DeviceId id = new DeviceId(OVERWORLD, 0, 64, 0);
        private final NetworkNode tower = new NetworkNode(this.id, "worldwideblocks:pc_tower", 1);

        @Test
        void takesTheSavedMacs() {
            MacAddress savedMac = MacAddress.parse("02:11:22:33:44:55").orElseThrow();

            assertTrue(graph.restoreNode(this.tower, List.of(savedMac)));
            assertEquals(List.of(savedMac), graph.macsOf(this.id));
            assertEquals(Optional.of(this.tower), graph.node(this.id));
        }

        @Test
        void generatesMacsForAWorldFromBeforeLot1b() {
            assertFalse(graph.restoreNode(this.tower, List.of()));
            assertEquals(1, graph.macsOf(this.id).size());
        }

        @Test
        void generatesMacsWhenTheSavedOnesAreUnusable() {
            MacAddress mac = MacAddress.parse("02:11:22:33:44:55").orElseThrow();
            graph.restoreNode(this.tower, List.of(mac));
            DeviceId other = new DeviceId(OVERWORLD, 5, 64, 0);

            // Duplicate of another device, broadcast address, wrong count.
            assertFalse(graph.restoreNode(new NetworkNode(other, "worldwideblocks:pc_tower", 1), List.of(mac)));
            assertNotEquals(mac, graph.macsOf(other).get(0));
            assertFalse(graph.restoreNode(new NetworkNode(other, "worldwideblocks:pc_tower", 1), List.of(MacAddress.BROADCAST)));
            assertFalse(graph.restoreNode(new NetworkNode(other, "worldwideblocks:pc_tower", 1), List.of(mac, mac)));
        }
    }
}
