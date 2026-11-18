package io.github.maximcuynat.worldwideblocks.network.dhcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessageType;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpOptions;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Packet;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/** The state machine of the DHCP client, alone, at the exact tick (RFC 2131, 4.4). */
class DhcpClientTest {
    private static final MacAddress PC = new MacAddress(0x02_00_00_00_00_01L);
    private static final MacAddress SERVICE = new MacAddress(0x02_00_00_00_00_FFL);
    private static final MacAddress OTHER_SERVICE = new MacAddress(0x02_00_00_00_00_EEL);
    private static final Ipv4Address SERVER = Ipv4Address.of(192, 168, 1, 1);
    private static final Ipv4Address MASK = Ipv4Address.of(255, 255, 255, 0);
    private static final Ipv4Address ADDRESS = Ipv4Address.of(192, 168, 1, 10);
    private static final DhcpOptions LEASE = DhcpOptions.lease(SERVER, MASK, SERVER, SERVER, 1_200L);

    private record Scheduled(long due, DhcpClient.Timer timer) {
    }

    /** A computer port: records what the client sends and the timer it asks for. */
    private final class FakePort implements DhcpClient.Port {
        long now;
        final List<EthernetFrame> sent = new ArrayList<>();
        final List<Long> sentAt = new ArrayList<>();
        Scheduled timer;

        @Override
        public long now() {
            return this.now;
        }

        @Override
        public MacAddress mac() {
            return PC;
        }

        @Override
        public void send(EthernetFrame frame) {
            this.sent.add(frame);
            this.sentAt.add(this.now);
        }

        @Override
        public void schedule(long delayTicks, DhcpClient.Timer timer) {
            assertTrue(delayTicks > 0, "delay " + delayTicks);
            this.timer = new Scheduled(this.now + delayTicks, timer);
        }

        /** Advances to the pending timer and fires it. */
        void fire() {
            Scheduled pending = this.timer;
            this.now = pending.due();
            client.onTimer(this, pending.timer());
        }

        DhcpMessage last() {
            return DhcpFrames.toServer(this.sent.getLast()).orElseThrow();
        }

        EthernetFrame lastFrame() {
            return this.sent.getLast();
        }
    }

    private DhcpClient client;
    private FakePort port;

    @BeforeEach
    void setUp() {
        this.client = new DhcpClient(new SplittableRandom(3));
        this.port = new FakePort();
    }

    private void reply(MacAddress from, DhcpMessage message) {
        this.client.receive(this.port, from, message);
    }

    /** Plugged at tick 0, offer and acknowledgement from the box: BOUND at tick 0. */
    private void bindAtZero() {
        this.client.linkUp(this.port);
        DhcpMessage discover = this.port.last();
        this.reply(SERVICE, DhcpMessage.offer(discover, ADDRESS, LEASE));
        this.reply(SERVICE, DhcpMessage.ack(this.port.last(), ADDRESS, LEASE));
        assertEquals(DhcpClientState.BOUND, this.client.state());
    }

    @Test
    void aConnectedCableStartsWithABroadcastDiscoverFromNoAddress() {
        assertEquals(DhcpClientState.UNPLUGGED, this.client.state());

        this.client.linkUp(this.port);

        assertEquals(DhcpClientState.SELECTING, this.client.state());
        EthernetFrame frame = this.port.lastFrame();
        assertTrue(frame.isBroadcast());
        assertEquals(PC, frame.source());
        Ipv4Packet packet = (Ipv4Packet) frame.payload();
        assertEquals(Ipv4Address.ANY, packet.source());
        assertEquals(Ipv4Address.BROADCAST, packet.destination());
        assertEquals(DhcpMessageType.DISCOVER, this.port.last().type());
        assertEquals(PC, this.port.last().clientMac());
        assertTrue(this.client.lease().isEmpty());
    }

    @Test
    void withoutAnswerTheDiscoverIsSentAgainAfter4_8_16_32_64ThenEvery64Seconds() {
        this.client.linkUp(this.port);
        int xid = this.port.last().xid();
        for (int i = 0; i < 7; i++) {
            this.port.fire();
        }

        assertEquals(List.of(0L, 80L, 240L, 560L, 1200L, 2480L, 3760L, 5040L), this.port.sentAt);
        assertTrue(this.port.sent.stream().allMatch(f -> DhcpFrames.toServer(f).orElseThrow().xid() == xid),
                "a retransmission keeps the xid");
        assertEquals(DhcpClientState.SELECTING, this.client.state());
    }

    @Test
    void theFirstOfferIsRequestedByBroadcastWithTheServerAndTheOfferedAddress() {
        this.client.linkUp(this.port);
        DhcpMessage discover = this.port.last();

        this.reply(SERVICE, DhcpMessage.offer(discover, ADDRESS, LEASE));

        assertEquals(DhcpClientState.REQUESTING, this.client.state());
        DhcpMessage request = this.port.last();
        assertEquals(DhcpMessage.requestSelected(discover.xid(), PC, SERVER, ADDRESS), request);
        assertTrue(this.port.lastFrame().isBroadcast());
        assertEquals(Ipv4Address.ANY, ((Ipv4Packet) this.port.lastFrame().payload()).source());
    }

    @Test
    void anOfferOfAnotherTransactionOrForAnotherClientIsIgnored() {
        this.client.linkUp(this.port);
        DhcpMessage discover = this.port.last();

        this.reply(SERVICE, DhcpMessage.offer(DhcpMessage.discover(discover.xid() + 1, PC), ADDRESS, LEASE));
        this.reply(SERVICE, DhcpMessage.offer(DhcpMessage.discover(discover.xid(), new MacAddress(0x02_00_00_00_00_09L)), ADDRESS, LEASE));
        this.reply(SERVICE, DhcpMessage.ack(DhcpMessage.discover(discover.xid(), PC), ADDRESS, LEASE));

        assertEquals(DhcpClientState.SELECTING, this.client.state());
        assertEquals(1, this.port.sent.size());
    }

    @Test
    void theAcknowledgementGivesTheLeaseAndTheRenewalIsDueAtHalfOfIt() {
        this.port.now = 100;
        this.client.linkUp(this.port);
        this.reply(SERVICE, DhcpMessage.offer(this.port.last(), ADDRESS, LEASE));
        this.reply(SERVICE, DhcpMessage.ack(this.port.last(), ADDRESS, LEASE));

        assertEquals(DhcpClientState.BOUND, this.client.state());
        DhcpLease lease = this.client.lease().orElseThrow();
        assertEquals(new DhcpLease(ADDRESS, MASK, SERVER, SERVER, SERVER, SERVICE, 100, 24_100), lease);
        assertEquals(12_100, lease.renewAt());
        assertEquals(21_100, lease.rebindAt());
        assertEquals(12_100, this.port.timer.due());
    }

    @Test
    void aSecondAcknowledgementOnceBoundIsIgnored() {
        this.bindAtZero();
        DhcpLease lease = this.client.lease().orElseThrow();

        this.reply(OTHER_SERVICE, DhcpMessage.ack(DhcpMessage.requestSelected(this.port.last().xid(), PC, SERVER, ADDRESS),
                Ipv4Address.of(192, 168, 1, 50), LEASE));

        assertEquals(Optional.of(lease), this.client.lease());
    }

    @Test
    void withoutAcknowledgementTheRequestIsSentAgainAfter4_8_16ThenTheClientStartsAgain60SecondsLater() {
        this.client.linkUp(this.port);
        int xid = this.port.last().xid();
        this.reply(SERVICE, DhcpMessage.offer(this.port.last(), ADDRESS, LEASE));
        for (int i = 0; i < 4; i++) {
            this.port.fire();
        }

        // DISCOVER at 0, REQUEST at 0, 80, 240, 560, then DISCOVER at 1200 (60 s after the first REQUEST).
        assertEquals(List.of(0L, 0L, 80L, 240L, 560L, 1200L), this.port.sentAt);
        assertEquals(DhcpMessageType.DISCOVER, this.port.last().type());
        assertEquals(xid + 1, this.port.last().xid(), "new transaction");
        assertEquals(DhcpClientState.SELECTING, this.client.state());
    }

    @Test
    void aNakWhileRequestingSendsTheClientBackToDiscover() {
        this.client.linkUp(this.port);
        this.reply(SERVICE, DhcpMessage.offer(this.port.last(), ADDRESS, LEASE));
        DhcpMessage request = this.port.last();

        this.reply(SERVICE, DhcpMessage.nak(request, SERVER));

        assertEquals(DhcpClientState.SELECTING, this.client.state());
        assertEquals(DhcpMessageType.DISCOVER, this.port.last().type());
    }

    @Test
    void atT1TheRenewalGoesToTheServerAloneWithTheAddressInCiaddr() {
        this.bindAtZero();
        int previousXid = this.port.last().xid();

        this.port.fire();

        assertEquals(12_000, this.port.now);
        assertEquals(DhcpClientState.RENEWING, this.client.state());
        DhcpMessage renewal = this.port.last();
        assertEquals(DhcpMessage.requestExtension(previousXid + 1, PC, ADDRESS), renewal);
        EthernetFrame frame = this.port.lastFrame();
        assertEquals(SERVICE, frame.destination(), "unicast to the server's MAC, taken from its ACK");
        assertEquals(ADDRESS, ((Ipv4Packet) frame.payload()).source());
        assertEquals(SERVER, ((Ipv4Packet) frame.payload()).destination());
        assertTrue(this.client.lease().isPresent(), "the address is still in use while renewing");
    }

    @Test
    void anAcknowledgedRenewalStartsANewLease() {
        this.bindAtZero();
        this.port.fire(); // T1 at 12,000

        this.reply(SERVICE, DhcpMessage.ack(this.port.last(), ADDRESS, LEASE));

        assertEquals(DhcpClientState.BOUND, this.client.state());
        assertEquals(12_000 + 24_000, this.client.lease().orElseThrow().expiresAt());
        assertEquals(24_000, this.port.timer.due(), "next T1");
    }

    @Test
    void withoutAnswerTheClientRenewsThenRebindsThenLosesItsAddressAtTheExactTicks() {
        this.bindAtZero();
        List<DhcpClientState> states = new ArrayList<>();
        List<Boolean> broadcast = new ArrayList<>();
        int sentBefore = this.port.sent.size();
        while (this.client.state() != DhcpClientState.SELECTING) {
            this.port.fire();
            states.add(this.client.state());
            broadcast.add(this.port.lastFrame().isBroadcast());
        }

        // RENEWING: half the time left before T2 (21,000), 60 s (1,200 ticks) at least, never beyond T2.
        // REBINDING: half the time left before the end of the lease (24,000), same rules.
        List<Long> sends = this.port.sentAt.subList(sentBefore, this.port.sentAt.size());
        assertEquals(List.of(12_000L, 16_500L, 18_750L, 19_950L, 21_000L, 22_500L, 23_700L, 24_000L), sends);
        assertEquals(List.of(DhcpClientState.RENEWING, DhcpClientState.RENEWING, DhcpClientState.RENEWING,
                DhcpClientState.RENEWING, DhcpClientState.REBINDING, DhcpClientState.REBINDING,
                DhcpClientState.REBINDING, DhcpClientState.SELECTING), states);
        assertEquals(List.of(false, false, false, false, true, true, true, true), broadcast);
        assertEquals(DhcpMessageType.DISCOVER, this.port.last().type(), "lease expired: back to INIT");
        assertTrue(this.client.lease().isEmpty());
    }

    @Test
    void theAddressIsUsedUntilTheTickBeforeTheEndOfTheLease() {
        this.bindAtZero();
        while (this.client.state() != DhcpClientState.SELECTING) {
            assertTrue(this.client.lease().isPresent(), "at tick " + this.port.now);
            this.port.fire();
        }
        assertEquals(24_000, this.port.now);
    }

    @Test
    void aNakWhileRenewingLosesTheAddress() {
        this.bindAtZero();
        this.port.fire();

        this.reply(SERVICE, DhcpMessage.nak(this.port.last(), SERVER));

        assertTrue(this.client.lease().isEmpty());
        assertEquals(DhcpClientState.SELECTING, this.client.state());
        assertEquals(DhcpMessageType.DISCOVER, this.port.last().type());
    }

    @Test
    void anUnpluggedCableLosesTheAddressAndStopsEverything() {
        this.bindAtZero();
        DhcpClient.Timer pending = this.port.timer.timer();
        int sent = this.port.sent.size();

        this.client.linkDown();
        this.port.now = 12_000;
        this.client.onTimer(this.port, pending);

        assertEquals(DhcpClientState.UNPLUGGED, this.client.state());
        assertTrue(this.client.lease().isEmpty());
        assertEquals(sent, this.port.sent.size(), "the stale timer sends nothing");
    }

    @Test
    void anIncompleteAcknowledgementIsIgnored() {
        this.client.linkUp(this.port);
        this.reply(SERVICE, DhcpMessage.offer(this.port.last(), ADDRESS, LEASE));

        this.reply(SERVICE, DhcpMessage.ack(this.port.last(), ADDRESS, DhcpOptions.server(SERVER)));

        assertEquals(DhcpClientState.REQUESTING, this.client.state());
    }
}
