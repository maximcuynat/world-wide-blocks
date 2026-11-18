package io.github.maximcuynat.worldwideblocks.network.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpFrames;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServer;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServerConfig;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpStore;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessageType;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.protocol.PresencePayload;
import io.github.maximcuynat.worldwideblocks.network.switching.MacTable;

/** The DHCP server of the box, plugged into the local point of its switch. */
class BoxLogicTest {
    private static final DeviceId BOX = new DeviceId("minecraft:overworld", 0, 64, 0);
    private static final MacAddress SERVICE = new MacAddress(0x02_00_00_00_00_FFL);
    private static final MacAddress PC = new MacAddress(0x02_00_00_00_00_01L);
    private static final MacAddress OTHER_PC = new MacAddress(0x02_00_00_00_00_02L);

    private record Sent(int port, EthernetFrame frame) {
    }

    /** Context of a box whose ports 0 to 3 are switched; records what the box sends or relays. */
    private final class FakeContext implements DeviceContext {
        @Override
        public long now() {
            return 0;
        }

        @Override
        public DeviceId device() {
            return BOX;
        }

        @Override
        public MacAddress mac(int port) {
            return new MacAddress(0x02_00_00_00_01_00L + port);
        }

        @Override
        public MacAddress serviceMac() {
            return SERVICE;
        }

        @Override
        public boolean isLinked(int port) {
            return true;
        }

        @Override
        public void send(int port, EthernetFrame frame) {
            BoxLogicTest.this.sent.add(new Sent(port, frame));
        }

        @Override
        public void relay(int port, EthernetFrame frame) {
            BoxLogicTest.this.relayed.add(new Sent(port, frame));
        }

        @Override
        public void schedule(long delayTicks, Object key) {
        }

        @Override
        public void report(SimulationEvent event) {
            BoxLogicTest.this.events.add(event);
        }
    }

    private final List<SimulationEvent> events = new ArrayList<>();

    private final List<Sent> sent = new ArrayList<>();
    private final List<Sent> relayed = new ArrayList<>();
    private final FakeContext context = new FakeContext();
    private DhcpStore store;
    private BoxLogic box;

    @BeforeEach
    void setUp() {
        this.store = new DhcpStore();
        this.box = new BoxLogic(List.of(0, 1, 2, 3), new DhcpServer(DhcpServerConfig.INTERNET_BOX, this.store.server(BOX)));
    }

    private static EthernetFrame discover(MacAddress client) {
        return DhcpFrames.fromClient(client, Ipv4Address.ANY, MacAddress.BROADCAST, Ipv4Address.BROADCAST,
                DhcpMessage.discover(9, client));
    }

    @Test
    void aDiscoverIsRelayedToTheOtherPortsAndAnsweredByTheServerThroughTheIngressPort() {
        this.box.onFrame(this.context, 1, discover(PC));

        assertEquals(List.of(0, 2, 3), this.relayed.stream().map(Sent::port).toList(), "broadcast relayed as before");
        assertEquals(1, this.sent.size());
        Sent offer = this.sent.getFirst();
        assertEquals(1, offer.port(), "the switch learned the client behind port 1");
        assertEquals(SERVICE, offer.frame().source());
        assertEquals(PC, offer.frame().destination());
        DhcpMessage message = DhcpFrames.toClient(offer.frame()).orElseThrow();
        assertEquals(DhcpMessageType.OFFER, message.type());
        assertEquals(9, message.xid());
        assertEquals(Ipv4Address.of(192, 168, 1, 10), message.yourAddress());
    }

    @Test
    void theServerIsNotShownAmongTheLearnedMacs() {
        this.box.onFrame(this.context, 1, discover(PC));

        assertEquals(List.of(PC), this.box.learnedMacs().stream().map(MacTable.Entry::mac).toList());
    }

    @Test
    void aUnicastFrameForAnotherDeviceDoesNotReachTheServer() {
        this.box.onFrame(this.context, 2, new EthernetFrame(OTHER_PC, MacAddress.BROADCAST, PresencePayload.INSTANCE));
        this.relayed.clear();
        EthernetFrame toOther = DhcpFrames.fromClient(PC, Ipv4Address.ANY, OTHER_PC, Ipv4Address.BROADCAST,
                DhcpMessage.discover(9, PC));

        this.box.onFrame(this.context, 1, toOther);

        assertEquals(List.of(2), this.relayed.stream().map(Sent::port).toList());
        assertTrue(this.sent.isEmpty());
    }

    @Test
    void aPresenceFrameReachesTheServerWhichIgnoresIt() {
        this.box.onFrame(this.context, 1, new EthernetFrame(PC, MacAddress.BROADCAST, PresencePayload.INSTANCE));

        assertTrue(this.sent.isEmpty());
        assertEquals(3, this.relayed.size());
    }

    @Test
    void aDisabledServerAnswersNothingButTheBoxStillSwitches() {
        this.store.server(BOX).setEnabled(false);

        this.box.onFrame(this.context, 1, discover(PC));

        assertTrue(this.sent.isEmpty());
        assertEquals(3, this.relayed.size());
    }
}
