package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Objects;

/**
 * ICMP message (RFC 792), carried by an IPv4 packet of protocol {@link IpProtocol#ICMP}. Only the messages of
 * lot 1c: the echo of {@code ping} and its reply, and destination unreachable. The checksum is not simulated.
 */
public sealed interface IcmpMessage extends Ipv4Payload {

    /** The "Type" field of the message. */
    int type();

    /** The "Code" field of the message. */
    int code();

    /** Length of the message, header included, in bytes. */
    int length();

    @Override
    default IpProtocol protocol() {
        return IpProtocol.ICMP;
    }

    /** True for a message that reports an error: no error is ever sent about it (RFC 1122, 3.2.2). */
    default boolean isError() {
        return false;
    }

    /**
     * Echo request (type 8) or echo reply (type 0). The reply carries the identifier, the sequence number and the
     * data of the request.
     *
     * @param identifier from 0 to 65535: the socket of the sender, which matches the replies to it
     * @param sequence   from 0 to 65535
     * @param dataLength bytes of data after the 8 bytes of the header ({@code ping} sends 56)
     */
    record Echo(boolean reply, int identifier, int sequence, int dataLength) implements IcmpMessage {
        /** Type of an echo request. */
        public static final int REQUEST_TYPE = 8;
        /** Type of an echo reply. */
        public static final int REPLY_TYPE = 0;

        public Echo {
            checkShort(identifier, "identifier");
            checkShort(sequence, "sequence");
            if (dataLength < 0 || dataLength > 0xFFFF - 28) {
                throw new IllegalArgumentException("Data length out of range: " + dataLength);
            }
        }

        public static Echo request(int identifier, int sequence, int dataLength) {
            return new Echo(false, identifier, sequence, dataLength);
        }

        /** The reply to this request. */
        public Echo answer() {
            return new Echo(true, this.identifier, this.sequence, this.dataLength);
        }

        @Override
        public int type() {
            return this.reply ? REPLY_TYPE : REQUEST_TYPE;
        }

        @Override
        public int code() {
            return 0;
        }

        @Override
        public int length() {
            return 8 + this.dataLength;
        }
    }

    /**
     * Destination unreachable (type 3), sent back to the source of a packet that cannot be delivered. RFC 792
     * quotes the header of the original packet and its first 64 bits of data, enough to find the echo it answers
     * (its identifier and sequence number); the whole packet is kept here.
     *
     * @param code     {@link #NET_UNREACHABLE} or {@link #HOST_UNREACHABLE}
     * @param original the packet that could not be delivered
     */
    record Unreachable(int code, Ipv4Packet original) implements IcmpMessage {
        /** Type of a destination unreachable. */
        public static final int TYPE = 3;
        /** Code 0: no route to the network of the destination (RFC 1812, 4.3.3.1). */
        public static final int NET_UNREACHABLE = 0;
        /** Code 1: the destination does not answer on its network (its address was not resolved by ARP). */
        public static final int HOST_UNREACHABLE = 1;

        public Unreachable {
            Objects.requireNonNull(original, "original");
            if (code < 0 || code > 15) {
                throw new IllegalArgumentException("Code out of range: " + code);
            }
        }

        @Override
        public int type() {
            return TYPE;
        }

        @Override
        public int length() {
            return 8 + 20 + 8; // header, then the quoted header of the original packet and 64 bits of its data
        }

        @Override
        public boolean isError() {
            return true;
        }
    }

    private static void checkShort(int value, String name) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException("ICMP " + name + " out of range: " + value);
        }
    }
}
