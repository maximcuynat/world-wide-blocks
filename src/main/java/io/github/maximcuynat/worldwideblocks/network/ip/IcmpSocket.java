package io.github.maximcuynat.worldwideblocks.network.ip;

import java.util.ArrayDeque;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.protocol.IcmpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;

/**
 * A ping socket of the stack, as Linux opens one for {@code ping} ({@code socket(AF_INET, SOCK_DGRAM,
 * IPPROTO_ICMP)}): an identifier chosen by the stack, the echo replies carrying it, and the destination unreachable
 * errors about its own requests (what Linux hands over through the error queue of the socket, {@code IP_RECVERR}).
 *
 * <p>What arrives waits in the socket until its owner takes it ({@link #poll}); the operating system hands it over
 * at the end of each call into the machine.
 */
public final class IcmpSocket {
    /**
     * What reached the socket.
     *
     * @param from    the sender: the machine that answered, or the one that reported the error (this machine itself
     *                when its ARP requests got no answer)
     * @param ttl     time to live of the packet received
     * @param latency time since the start of the tick when it arrived, in nanoseconds ({@code Latency})
     */
    public record Received(Ipv4Address from, IcmpMessage message, int ttl, long latency) {
    }

    private final Ipv4Stack stack;
    private final int identifier;
    private final ArrayDeque<Received> inbox = new ArrayDeque<>();
    private boolean closed;

    IcmpSocket(Ipv4Stack stack, int identifier) {
        this.stack = stack;
        this.identifier = identifier;
    }

    /** Identifier of the echo requests of this socket. */
    public int identifier() {
        return this.identifier;
    }

    /** The oldest message waiting, removed from the socket. */
    public Optional<Received> poll() {
        return Optional.ofNullable(this.inbox.poll());
    }

    public boolean hasWaiting() {
        return !this.inbox.isEmpty();
    }

    /** The socket is released: nothing reaches it any more. */
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.inbox.clear();
            this.stack.release(this);
        }
    }

    public boolean closed() {
        return this.closed;
    }

    void deliver(Received received) {
        if (!this.closed) {
            this.inbox.add(received);
        }
    }
}
