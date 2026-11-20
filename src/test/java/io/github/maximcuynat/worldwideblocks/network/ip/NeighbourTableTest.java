package io.github.maximcuynat.worldwideblocks.network.ip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpFrames;
import io.github.maximcuynat.worldwideblocks.network.ip.NeighbourTable.State;
import io.github.maximcuynat.worldwideblocks.network.protocol.ArpPacket;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Packet;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/** The ARP cache of a computer at 192.168.1.10/24, step by step, against the states and timers of Linux. */
class NeighbourTableTest {
    private static final long SECOND = 20;
    private static final MacAddress OWN = new MacAddress(0x02_00_00_00_00_10L);
    private static final MacAddress BOX = new MacAddress(0x02_00_00_00_00_01L);
    private static final MacAddress OTHER_BOX = new MacAddress(0x02_00_00_00_00_02L);
    private static final Ipv4Address ADDRESS = Ipv4Address.of(192, 168, 1, 10);
    private static final Ipv4Address GATEWAY = Ipv4Address.of(192, 168, 1, 1);

    private Ipv4Stack stack;
    private NetInterface eth0;
    private FakeMachine machine;
    private final List<Ipv4Packet> lost = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.stack = new Ipv4Stack();
        this.eth0 = this.stack.addEthernet("eth0", 0, OWN, true);
        this.stack.addAddress(this.eth0, new InterfaceAddress(ADDRESS, 24,
                Optional.of(Ipv4Address.of(192, 168, 1, 255)), Scope.GLOBAL, InterfaceAddress.FOREVER));
        this.machine = new FakeMachine(this.stack);
    }

    private Ipv4Packet packet(int n) {
        return DhcpFrames.packetFromClient(ADDRESS, GATEWAY, DhcpMessage.discover(n, OWN));
    }

    private void send(int n) {
        assertEquals(Ipv4Stack.Output.SENT, this.stack.send(this.machine, this.packet(n), this.lost::add));
    }

    private void reply(MacAddress from, MacAddress to) {
        ArpPacket arp = new ArpPacket(ArpPacket.Operation.REPLY, from, GATEWAY, OWN, ADDRESS);
        this.stack.receive(this.machine, this.eth0, new EthernetFrame(from, to, arp));
    }

    private Optional<NeighbourTable.Neighbour> gateway() {
        return this.stack.neighbours().neighbours().stream().filter(n -> n.address().equals(GATEWAY)).findFirst();
    }

    private State state() {
        return this.gateway().orElseThrow().state();
    }

    private List<FakeMachine.Sent> arpSent() {
        return this.machine.sent().stream().filter(s -> s.frame().payload() instanceof ArpPacket).toList();
    }

    private List<FakeMachine.Sent> packetsSent() {
        return this.machine.sent().stream().filter(s -> s.frame().payload() instanceof Ipv4Packet).toList();
    }

    /** Resolved at tick 0: REACHABLE, confirmed at 0. */
    private void resolvedAtZero() {
        this.send(1);
        this.reply(BOX, OWN);
        assertEquals(State.REACHABLE, this.state());
        this.machine.clearSent();
    }

    @Test
    void anUnknownNeighbourIsAskedForByABroadcastRequestAndThePacketWaits() {
        this.send(1);

        assertEquals(State.INCOMPLETE, this.state());
        assertEquals(Optional.empty(), this.gateway().orElseThrow().mac());
        FakeMachine.Sent request = this.arpSent().getFirst();
        assertEquals(MacAddress.BROADCAST, request.frame().destination());
        assertEquals(ArpPacket.request(OWN, ADDRESS, GATEWAY), request.frame().payload(), "zero target hardware address");
        assertTrue(this.packetsSent().isEmpty());
    }

    @Test
    void theReplyMakesItReachableAndTheWaitingPacketsLeave() {
        this.send(1);
        this.send(2);

        this.reply(BOX, OWN);

        assertEquals(State.REACHABLE, this.state());
        assertEquals(Optional.of(BOX), this.gateway().orElseThrow().mac());
        assertEquals(List.of(this.packet(1), this.packet(2)), this.packetsSent().stream().map(s -> s.frame().payload()).toList());
        assertTrue(this.packetsSent().stream().allMatch(s -> s.frame().destination().equals(BOX)));
    }

    @Test
    void withoutReplyThreeRequestsOneSecondApartThenFailedAndEachWaitingPacketReported() {
        this.send(1);
        this.machine.runUntil(SECOND);
        this.send(2);
        this.machine.runUntil(2 * SECOND);
        this.send(3);

        this.machine.runUntil(3 * SECOND - 1);
        assertEquals(State.INCOMPLETE, this.state());
        assertTrue(this.lost.isEmpty());
        this.machine.runUntil(3 * SECOND);

        assertEquals(List.of(0L, SECOND, 2 * SECOND), this.arpSent().stream().map(FakeMachine.Sent::at).toList());
        assertTrue(this.arpSent().stream().allMatch(s -> s.frame().isBroadcast()));
        assertEquals(State.FAILED, this.state());
        assertEquals(List.of(this.packet(1), this.packet(2), this.packet(3)), this.lost);
        this.machine.runUntil(10 * SECOND);
        assertEquals(3, this.arpSent().size(), "a failed entry asks nothing more by itself");
    }

    @Test
    void aPacketToAFailedNeighbourStartsTheResolutionAgain() {
        this.send(1);
        this.machine.runUntil(3 * SECOND);
        assertEquals(State.FAILED, this.state());

        this.send(2);

        assertEquals(State.INCOMPLETE, this.state());
        assertEquals(4, this.arpSent().size());
        this.machine.runUntil(6 * SECOND);
        assertEquals(List.of(this.packet(1), this.packet(2)), this.lost);
    }

    @Test
    void thirtySecondsAfterItsConfirmationAnUnusedEntryBecomesStale() {
        this.resolvedAtZero();

        this.machine.runUntil(30 * SECOND - 1);
        assertEquals(State.REACHABLE, this.state());
        this.machine.runUntil(30 * SECOND);

        assertEquals(State.STALE, this.state());
        assertEquals(Optional.of(BOX), this.gateway().orElseThrow().mac());
    }

    @Test
    void anEntryUsedInTheLastFiveSecondsGoesToDelayThenProbesByUnicast() {
        this.resolvedAtZero();
        this.machine.runUntil(26 * SECOND);
        this.send(2);

        this.machine.runUntil(30 * SECOND);
        assertEquals(State.DELAY, this.state());
        this.machine.runUntil(35 * SECOND);

        assertEquals(State.PROBE, this.state());
        FakeMachine.Sent probe = this.arpSent().getFirst();
        assertEquals(35 * SECOND, probe.at());
        assertEquals(BOX, probe.frame().destination(), "unicast to the known address");
    }

    @Test
    void aPacketToAStaleNeighbourLeavesAtOnceAndTheEntryWaitsFiveSecondsBeforeProbing() {
        this.resolvedAtZero();
        this.machine.runUntil(40 * SECOND);
        assertEquals(State.STALE, this.state());

        this.send(2);

        assertEquals(State.DELAY, this.state());
        assertEquals(1, this.packetsSent().size());
        assertTrue(this.arpSent().isEmpty());
        this.machine.runUntil(45 * SECOND);
        assertEquals(State.PROBE, this.state());
    }

    @Test
    void aProbedNeighbourThatNeverAnswersFailsAfterThreeUnicastRequests() {
        this.resolvedAtZero();
        this.machine.runUntil(40 * SECOND);
        this.send(2);

        this.machine.runUntil(48 * SECOND - 1);
        assertEquals(State.PROBE, this.state());
        this.machine.runUntil(48 * SECOND);

        assertEquals(List.of(45 * SECOND, 46 * SECOND, 47 * SECOND), this.arpSent().stream().map(FakeMachine.Sent::at).toList());
        assertTrue(this.arpSent().stream().noneMatch(s -> s.frame().isBroadcast()));
        assertEquals(State.FAILED, this.state());
        assertTrue(this.lost.isEmpty(), "the packet had left: nothing was waiting");
    }

    @Test
    void aReplyWhileProbingMakesItReachableAgain() {
        this.resolvedAtZero();
        this.machine.runUntil(40 * SECOND);
        this.send(2);
        this.machine.runUntil(45 * SECOND);

        this.reply(BOX, OWN);

        assertEquals(State.REACHABLE, this.state());
        this.machine.runUntil(60 * SECOND);
        assertEquals(1, this.arpSent().size(), "no more probes");
    }

    @Test
    void aRequestForThisMachineIsAnsweredAndTheAskerBecomesStale() {
        ArpPacket request = ArpPacket.request(BOX, GATEWAY, ADDRESS);

        this.stack.receive(this.machine, this.eth0, new EthernetFrame(BOX, MacAddress.BROADCAST, request));

        FakeMachine.Sent answer = this.machine.sent().getFirst();
        assertEquals(new EthernetFrame(OWN, BOX, new ArpPacket(ArpPacket.Operation.REPLY, OWN, ADDRESS, BOX, GATEWAY)),
                answer.frame());
        assertEquals(State.STALE, this.state());
        assertEquals(Optional.of(BOX), this.gateway().orElseThrow().mac());
    }

    @Test
    void aRequestForAnotherAddressIsNotAnsweredAndCreatesNothing() {
        ArpPacket request = ArpPacket.request(BOX, GATEWAY, Ipv4Address.of(192, 168, 1, 99));

        this.stack.receive(this.machine, this.eth0, new EthernetFrame(BOX, MacAddress.BROADCAST, request));

        assertTrue(this.machine.sent().isEmpty());
        assertTrue(this.stack.neighbours().neighbours().isEmpty());
    }

    @Test
    void aReplyNobodyAskedForCreatesNoEntry() {
        this.reply(BOX, OWN);

        assertTrue(this.stack.neighbours().neighbours().isEmpty());
    }

    @Test
    void twoDifferentAnswersInTheSameSecondKeepTheFirstThenALaterOneWins() {
        this.send(1);

        this.reply(BOX, OWN);
        this.reply(OTHER_BOX, OWN);
        assertEquals(Optional.of(BOX), this.gateway().orElseThrow().mac());

        this.machine.runUntil(SECOND + 1);
        this.reply(OTHER_BOX, OWN);
        assertEquals(Optional.of(OTHER_BOX), this.gateway().orElseThrow().mac());
        assertEquals(State.REACHABLE, this.state());
    }

    @Test
    void theEntriesOfAnInterfaceGoWithItsCarrier() {
        this.resolvedAtZero();

        this.stack.setCarrier(this.eth0, false);

        assertTrue(this.stack.neighbours().neighbours().isEmpty());
    }

    @Test
    void atMost101PacketsWaitTheOldestDroppedWithoutReport() {
        for (int i = 0; i < NeighbourTable.QUEUE_LENGTH + 2; i++) {
            this.send(i);
        }

        this.reply(BOX, OWN);

        assertEquals(NeighbourTable.QUEUE_LENGTH, this.packetsSent().size());
        assertEquals(this.packet(2), this.packetsSent().getFirst().frame().payload());
        assertTrue(this.lost.isEmpty());
    }

    @Test
    void aDestinationWithoutRouteIsNotSent() {
        Ipv4Packet far = DhcpFrames.packetFromClient(ADDRESS, Ipv4Address.of(8, 8, 8, 8), DhcpMessage.discover(1, OWN));

        assertEquals(Ipv4Stack.Output.NO_ROUTE, this.stack.send(this.machine, far, this.lost::add));
        assertTrue(this.machine.sent().isEmpty());
    }

    @Test
    void anArpPacketTypeMatchesItsContent() {
        ArpPacket request = ArpPacket.request(OWN, ADDRESS, GATEWAY);

        assertEquals(ArpPacket.UNKNOWN, request.targetMac());
        assertEquals(1, request.operation().value());
        assertEquals(2, ArpPacket.replyTo(request, BOX).operation().value());
        assertEquals(0x0806, new EthernetFrame(OWN, MacAddress.BROADCAST, request).type().value());
    }
}
