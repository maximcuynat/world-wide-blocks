package io.github.maximcuynat.worldwideblocks.network.ip;

import java.util.Objects;

import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;

/**
 * A block of IPv4 addresses: a network address and the length of its prefix ({@code 192.168.1.0/24}).
 * {@code 0.0.0.0/0} holds every address: it is the destination of the default route.
 *
 * @param network first address of the block; its bits beyond the prefix are zero
 * @param length  number of bits of the prefix, from 0 to 32
 */
public record Ipv4Prefix(Ipv4Address network, int length) {
    /** Every address: destination of the default route. */
    public static final Ipv4Prefix DEFAULT = new Ipv4Prefix(Ipv4Address.ANY, 0);

    public Ipv4Prefix {
        Objects.requireNonNull(network, "network");
        if (length < 0 || length > 32) {
            throw new IllegalArgumentException("Prefix length out of range: " + length);
        }
        if ((network.value() & ~maskBits(length)) != 0) {
            throw new IllegalArgumentException("Host bits set in " + network + "/" + length);
        }
    }

    /** The block of {@code address} for this prefix length (host bits cleared). */
    public static Ipv4Prefix of(Ipv4Address address, int length) {
        if (length < 0 || length > 32) {
            throw new IllegalArgumentException("Prefix length out of range: " + length);
        }
        return new Ipv4Prefix(new Ipv4Address(address.value() & maskBits(length)), length);
    }

    /** True if the ones of {@code mask} are contiguous from the left: a network mask. */
    public static boolean isMask(Ipv4Address mask) {
        return maskBits(Integer.bitCount(mask.value())) == mask.value();
    }

    /**
     * Length of the prefix of a network mask ({@code 255.255.255.0}: 24).
     *
     * @throws IllegalArgumentException if the ones of the mask are not contiguous
     */
    public static int lengthOf(Ipv4Address mask) {
        if (!isMask(mask)) {
            throw new IllegalArgumentException("Not a network mask: " + mask);
        }
        return Integer.bitCount(mask.value());
    }

    public boolean contains(Ipv4Address address) {
        return (address.value() & maskBits(this.length)) == this.network.value();
    }

    public Ipv4Address mask() {
        return new Ipv4Address(maskBits(this.length));
    }

    /** Last address of the block: its broadcast address. */
    public Ipv4Address broadcast() {
        return new Ipv4Address(this.network.value() | ~maskBits(this.length));
    }

    private static int maskBits(int length) {
        return length == 0 ? 0 : -1 << (32 - length);
    }

    /** {@code 192.168.1.0/24}, as {@code ip route} writes it (it writes {@code default} for {@link #DEFAULT}). */
    @Override
    public String toString() {
        return this.network + "/" + this.length;
    }
}
