package io.github.maximcuynat.worldwideblocks.network.ip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;

/** A machine for the tests of a stack: a clock driven by the test, its timers, and what it puts on the wire. */
public final class FakeMachine implements Ipv4Stack.Context {
    /** A frame put on the wire. */
    public record Sent(long at, NetInterface out, EthernetFrame frame) {
    }

    private record Due(long at, long order, Object key) {
    }

    private final Ipv4Stack stack;
    private final PriorityQueue<Due> timers = new PriorityQueue<>(Comparator.comparingLong(Due::at).thenComparingLong(Due::order));
    private final List<Sent> sent = new ArrayList<>();
    private long now;
    private long order;

    public FakeMachine(Ipv4Stack stack) {
        this.stack = stack;
    }

    @Override
    public long now() {
        return this.now;
    }

    @Override
    public void send(NetInterface out, EthernetFrame frame) {
        this.sent.add(new Sent(this.now, out, frame));
    }

    @Override
    public void schedule(long delayTicks, Object key) {
        if (delayTicks <= 0) {
            throw new IllegalArgumentException("Non-positive delay: " + delayTicks);
        }
        this.timers.add(new Due(this.now + delayTicks, this.order++, key));
    }

    /** Fires every timer due up to {@code time} included, in order, then sets the clock to it. */
    public void runUntil(long time) {
        while (!this.timers.isEmpty() && this.timers.peek().at() <= time) {
            Due due = this.timers.poll();
            this.now = due.at();
            this.stack.onTimer(this, due.key());
        }
        this.now = time;
    }

    public List<Sent> sent() {
        return this.sent;
    }

    /** Forgets what was sent so far. */
    public void clearSent() {
        this.sent.clear();
    }
}
