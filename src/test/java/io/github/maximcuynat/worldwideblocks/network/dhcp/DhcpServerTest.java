package io.github.maximcuynat.worldwideblocks.network.dhcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessageType;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/** The DHCP server of the box, alone (RFC 2131, 4.3). */
class DhcpServerTest {
    private static final DeviceId BOX = new DeviceId("minecraft:overworld", 0, 64, 0);
    private static final Ipv4Address SERVER = Ipv4Address.of(192, 168, 1, 1);
    private static final Ipv4Address MASK = Ipv4Address.of(255, 255, 255, 0);
    private static final Ipv4Address FIRST = Ipv4Address.of(192, 168, 1, 10);
    private static final Ipv4Address SECOND = Ipv4Address.of(192, 168, 1, 11);
    private static final MacAddress A = new MacAddress(0x02_00_00_00_00_0AL);
    private static final MacAddress B = new MacAddress(0x02_00_00_00_00_0BL);
    private static final MacAddress C = new MacAddress(0x02_00_00_00_00_0CL);
    private static final long LEASE = 24_000L;

    /** Same as the box, with a pool of two addresses. */
    private static final DhcpServerConfig TWO_ADDRESSES = new DhcpServerConfig(SERVER, MASK, SERVER, SERVER, FIRST, SECOND,
            1_200L, 1_200L);

    private DhcpStore store;
    private DhcpServer server;

    @BeforeEach
    void setUp() {
        this.use(DhcpServerConfig.INTERNET_BOX);
    }

    private void use(DhcpServerConfig config) {
        this.store = new DhcpStore();
        this.server = new DhcpServer(config, this.store.server(BOX));
    }

    private Optional<DhcpMessage> discover(MacAddress client, long now) {
        return this.server.handle(DhcpMessage.discover(1, client), now);
    }

    /** DISCOVER then REQUEST of the offer: the address leased. */
    private Ipv4Address lease(MacAddress client, long now) {
        Ipv4Address offered = this.discover(client, now).orElseThrow().yourAddress();
        DhcpMessage ack = this.server.handle(DhcpMessage.requestSelected(2, client, SERVER, offered), now).orElseThrow();
        assertEquals(DhcpMessageType.ACK, ack.type());
        return ack.yourAddress();
    }

    private DhcpBinding binding(MacAddress client) {
        return this.server.state().bindingOf(client).orElseThrow();
    }

    @Test
    void theBoxLendsFrom10To200In24WithItselfAsGatewayAndNameServerForOneDay() {
        DhcpServerConfig box = DhcpServerConfig.INTERNET_BOX;

        assertEquals(SERVER, box.serverAddress());
        assertEquals(MASK, box.subnetMask());
        assertEquals(SERVER, box.router());
        assertEquals(SERVER, box.dnsServer());
        assertEquals(FIRST, box.poolAddress(0));
        assertEquals(Ipv4Address.of(192, 168, 1, 200), box.poolAddress(box.poolSize() - 1));
        assertEquals(191, box.poolSize());
        assertEquals(LEASE, box.leaseTicks());
        assertEquals(1_200L, box.offerHoldTicks());
    }

    @Test
    void aDiscoverGetsTheFirstFreeAddressWithTheConfiguration() {
        DhcpMessage offer = this.discover(A, 0).orElseThrow();

        assertEquals(DhcpMessageType.OFFER, offer.type());
        assertEquals(1, offer.xid());
        assertEquals(A, offer.clientMac());
        assertEquals(FIRST, offer.yourAddress());
        assertEquals(Optional.of(SERVER), offer.options().serverId());
        assertEquals(Optional.of(MASK), offer.options().subnetMask());
        assertEquals(Optional.of(SERVER), offer.options().router());
        assertEquals(Optional.of(SERVER), offer.options().dnsServer());
        assertEquals(OptionalLong.of(1_200L), offer.options().leaseSeconds());
        assertTrue(binding(A).offered(), "the offered address is recorded (4.3.1)");
    }

    @Test
    void twoClientsGetTwoAddresses() {
        assertEquals(FIRST, this.lease(A, 0));
        assertEquals(SECOND, this.lease(B, 0));
    }

    @Test
    void theRequestOfTheOfferGetsAnAckAndALeaseOfOneGameDay() {
        this.lease(A, 100);

        DhcpBinding lease = binding(A);
        assertFalse(lease.offered());
        assertEquals(100 + LEASE, lease.expiresAt());
        assertTrue(lease.leasedAt(100 + LEASE - 1));
        assertFalse(lease.leasedAt(100 + LEASE), "the lease ends at its expiry tick");
        assertEquals(List.of(lease), this.server.state().leasesAt(100));
        assertEquals(List.of(), this.server.state().leasesAt(100 + LEASE));
    }

    @Test
    void theSameMacGetsTheSameAddressBackEvenAfterItsLeaseExpired() {
        this.lease(B, 0);
        Ipv4Address first = this.lease(A, 0);

        assertEquals(first, this.discover(A, 10).orElseThrow().yourAddress(), "while the lease runs");
        assertEquals(first, this.lease(A, 5 * LEASE), "long after it expired");
    }

    @Test
    void anOfferedAddressIsHeld60SecondsThenGivenToAnother() {
        this.use(TWO_ADDRESSES);
        this.discover(A, 0); // FIRST offered, never requested
        this.lease(B, 0);    // SECOND

        assertTrue(this.discover(C, 1_199).isEmpty(), "still held for A");
        assertEquals(FIRST, this.discover(C, 1_200).orElseThrow().yourAddress());
        assertTrue(this.server.state().bindingOf(A).isEmpty(), "A loses its memory to C");
    }

    @Test
    void aRequestForAnotherServerFreesTheOfferButTheClientKeepsItsAddressIfNobodyTakesIt() {
        this.use(TWO_ADDRESSES);
        this.discover(A, 0);
        Ipv4Address other = Ipv4Address.of(192, 168, 1, 254);

        assertTrue(this.server.handle(DhcpMessage.requestSelected(2, A, other, FIRST), 10).isEmpty());
        assertFalse(binding(A).activeAt(10), "offer released (3.1: the client declined it)");
        this.lease(B, 10);
        assertEquals(FIRST, this.lease(C, 10), "the released address is free for another");
    }

    @Test
    void aRequestOfAnAddressThatWasNotOfferedGetsANak() {
        this.discover(A, 0);

        DhcpMessage reply = this.server.handle(DhcpMessage.requestSelected(2, A, SERVER, SECOND), 0).orElseThrow();
        assertEquals(DhcpMessageType.NAK, reply.type());
        assertEquals(2, reply.xid());
        assertTrue(this.server.handle(DhcpMessage.requestSelected(3, B, SERVER, FIRST), 0)
                .filter(nak -> nak.type() == DhcpMessageType.NAK).isPresent(), "client without an offer");
    }

    @Test
    void initRebootGetsTheKnownAddressBack() {
        Ipv4Address address = this.lease(A, 0);

        DhcpMessage ack = this.server.handle(DhcpMessage.requestPrevious(5, A, address), 30_000).orElseThrow();
        assertEquals(DhcpMessageType.ACK, ack.type());
        assertEquals(address, ack.yourAddress());
        assertEquals(30_000 + LEASE, binding(A).expiresAt());
    }

    @Test
    void initRebootOfAnotherAddressOrOnAnotherNetworkGetsANak() {
        this.lease(A, 0);

        assertEquals(DhcpMessageType.NAK,
                this.server.handle(DhcpMessage.requestPrevious(5, A, SECOND), 0).orElseThrow().type());
        assertEquals(DhcpMessageType.NAK,
                this.server.handle(DhcpMessage.requestPrevious(6, B, Ipv4Address.of(10, 0, 0, 7)), 0).orElseThrow().type(),
                "wrong network, even for an unknown client");
    }

    @Test
    void initRebootOfAnUnknownClientGetsNoAnswer() {
        assertTrue(this.server.handle(DhcpMessage.requestPrevious(5, A, FIRST), 0).isEmpty(),
                "4.3.2: no record of this client: MUST remain silent");
    }

    @Test
    void aRenewalExtendsTheLeaseFromItsDate() {
        Ipv4Address address = this.lease(A, 0);

        DhcpMessage ack = this.server.handle(DhcpMessage.requestExtension(7, A, address), 12_000).orElseThrow();
        assertEquals(DhcpMessageType.ACK, ack.type());
        assertEquals(address, ack.clientAddress());
        assertEquals(12_000 + LEASE, binding(A).expiresAt());
    }

    @Test
    void aRenewalOfAnAddressThatIsNotTheClientsGetsANak() {
        this.lease(A, 0);

        assertEquals(DhcpMessageType.NAK, this.server.handle(DhcpMessage.requestExtension(7, A, SECOND), 0).orElseThrow().type());
        assertEquals(DhcpMessageType.NAK, this.server.handle(DhcpMessage.requestExtension(8, B, FIRST), 0).orElseThrow().type());
        assertEquals(FIRST, binding(A).address(), "A keeps its lease");
    }

    @Test
    void aFullPoolGivesNoOffer() {
        this.use(TWO_ADDRESSES);
        this.lease(A, 0);
        this.lease(B, 0);

        assertTrue(this.discover(C, LEASE - 1).isEmpty());
    }

    @Test
    void aFullPoolReusesTheLeastRecentlyAssignedExpiredAddress() {
        this.use(TWO_ADDRESSES);
        Ipv4Address ofA = this.lease(A, 0);
        this.lease(B, 100);

        assertEquals(ofA, this.discover(C, 100 + LEASE).orElseThrow().yourAddress());
        assertTrue(this.server.state().bindingOf(A).isEmpty());
        assertTrue(this.server.state().bindingOf(B).isPresent());
    }

    @Test
    void theWholePoolOfTheBoxCanBeLent() {
        for (int i = 0; i < 191; i++) {
            assertEquals(Ipv4Address.of(192, 168, 1, 10 + i), this.lease(new MacAddress(0x02_00_00_00_10_00L + i), 0));
        }
        assertTrue(this.discover(C, 0).isEmpty());
    }

    @Test
    void aDisabledServerAnswersNothingAndKeepsItsMemory() {
        Ipv4Address address = this.lease(A, 0);
        this.server.state().setEnabled(false);

        assertTrue(this.discover(B, 0).isEmpty());
        assertTrue(this.server.handle(DhcpMessage.requestExtension(7, A, address), 10).isEmpty());
        assertEquals(address, binding(A).address());

        this.server.state().setEnabled(true);
        assertEquals(DhcpMessageType.ACK, this.server.handle(DhcpMessage.requestExtension(8, A, address), 20).orElseThrow().type());
    }

    @Test
    void everyChangeIsReportedToTheStoreAndARemovedBoxForgetsEverything() {
        this.store.consumeChanged();
        this.lease(A, 0);
        assertTrue(this.store.consumeChanged());
        assertFalse(this.store.consumeChanged());

        this.server.state().setEnabled(false);
        assertTrue(this.store.consumeChanged());

        this.store.forget(BOX);
        assertTrue(this.store.consumeChanged());
        assertTrue(this.store.server(BOX).bindings().isEmpty(), "a new box starts from zero");
        assertTrue(this.store.server(BOX).enabled());
    }
}
