package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Optional;

/**
 * 32-bit IPv4 address, stored in {@code value} (the first byte written is the most significant one:
 * {@code 192.168.1.1} is {@code 0xC0A80101}).
 *
 * <p>Plain Java, no Minecraft class.
 */
public record Ipv4Address(int value) {
    /** {@code 0.0.0.0}: "no address yet", the source of the messages of a DHCP client before it has one (RFC 2131, 4.1). */
    public static final Ipv4Address ANY = new Ipv4Address(0);

    /** {@code 255.255.255.255}: limited broadcast, to all devices of the local network. */
    public static final Ipv4Address BROADCAST = new Ipv4Address(0xFFFF_FFFF);

    /** Address written {@code a.b.c.d}; each byte from 0 to 255. */
    public static Ipv4Address of(int a, int b, int c, int d) {
        for (int octet : new int[] {a, b, c, d}) {
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Byte out of range: " + octet);
            }
        }
        return new Ipv4Address(a << 24 | b << 16 | c << 8 | d);
    }

    /** Reads an address written {@code 192.168.1.10}: four decimal numbers from 0 to 255, without a leading zero. */
    public static Optional<Ipv4Address> parse(String text) {
        String[] parts = text.split("\\.", -1);
        if (parts.length != 4) {
            return Optional.empty();
        }
        int value = 0;
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || (part.length() > 1 && part.charAt(0) == '0')
                    || !part.chars().allMatch(c -> c >= '0' && c <= '9')) {
                return Optional.empty();
            }
            int octet = Integer.parseInt(part);
            if (octet > 255) {
                return Optional.empty();
            }
            value = value << 8 | octet;
        }
        return Optional.of(new Ipv4Address(value));
    }

    /** Byte number {@code index}, from 0 (the first written) to 3. */
    public int octet(int index) {
        if (index < 0 || index > 3) {
            throw new IndexOutOfBoundsException(index);
        }
        return this.value >>> (8 * (3 - index)) & 0xFF;
    }

    /** The address that follows this one, or that comes {@code offset} addresses later. */
    public Ipv4Address plus(int offset) {
        return new Ipv4Address(this.value + offset);
    }

    /** Position of this address after {@code start} ({@code start} itself: 0), compared as unsigned numbers. */
    public long distanceFrom(Ipv4Address start) {
        return Integer.toUnsignedLong(this.value) - Integer.toUnsignedLong(start.value);
    }

    /** True if both addresses are in the same network for this mask. */
    public boolean sameNetwork(Ipv4Address other, Ipv4Address mask) {
        return (this.value & mask.value) == (other.value & mask.value);
    }

    public boolean isAny() {
        return this.value == 0;
    }

    public boolean isBroadcast() {
        return this.value == 0xFFFF_FFFF;
    }

    @Override
    public String toString() {
        return this.octet(0) + "." + this.octet(1) + "." + this.octet(2) + "." + this.octet(3);
    }
}
