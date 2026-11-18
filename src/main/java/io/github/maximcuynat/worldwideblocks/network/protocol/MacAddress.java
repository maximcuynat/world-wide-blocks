package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Locale;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * 48-bit Ethernet hardware address (MAC), stored in the low 48 bits of {@code value}.
 * The first byte is the most significant one: it is the one written first ({@code 02:…}).
 *
 * <p>Plain Java, no Minecraft class.
 */
public record MacAddress(long value) {
    private static final long MASK = 0xFFFF_FFFF_FFFFL;

    /** Broadcast address: the frame is meant for all devices of the network. */
    public static final MacAddress BROADCAST = new MacAddress(MASK);

    /** First byte of generated addresses: "locally administered" bit set, multicast bit clear. */
    private static final long LOCAL_PREFIX = 0x02L << 40;

    public MacAddress {
        if ((value & ~MASK) != 0) {
            throw new IllegalArgumentException("MAC address out of 48 bits: " + Long.toHexString(value));
        }
    }

    /**
     * Locally administered unicast address, drawn at random: first byte {@code 02}, the other
     * five random. It cannot conflict with an address assigned by a manufacturer.
     */
    public static MacAddress randomLocal(RandomGenerator random) {
        return new MacAddress(LOCAL_PREFIX | (random.nextLong() & 0xFF_FFFF_FFFFL));
    }

    /** Reads an address written as {@code 02:4f:a0:00:1b:7c} (uppercase accepted). */
    public static Optional<MacAddress> parse(String text) {
        String[] parts = text.split(":", -1);
        if (parts.length != 6) {
            return Optional.empty();
        }
        long value = 0;
        for (String part : parts) {
            if (part.length() != 2) {
                return Optional.empty();
            }
            int high = Character.digit(part.charAt(0), 16);
            int low = Character.digit(part.charAt(1), 16);
            if (high < 0 || low < 0) {
                return Optional.empty();
            }
            value = value << 8 | high << 4 | low;
        }
        return Optional.of(new MacAddress(value));
    }

    /** Byte number {@code index}, from 0 (the first written) to 5. */
    public int octet(int index) {
        if (index < 0 || index > 5) {
            throw new IndexOutOfBoundsException(index);
        }
        return (int) (this.value >>> (8 * (5 - index))) & 0xFF;
    }

    public boolean isBroadcast() {
        return this.value == MASK;
    }

    /** Group address (low bit of the first byte); broadcast is a special case of it. */
    public boolean isMulticast() {
        return (this.octet(0) & 0x01) != 0;
    }

    /** Locally administered address (second bit of the first byte), not assigned by a manufacturer. */
    public boolean isLocallyAdministered() {
        return (this.octet(0) & 0x02) != 0;
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder(17);
        for (int i = 0; i < 6; i++) {
            if (i > 0) {
                text.append(':');
            }
            text.append(String.format(Locale.ROOT, "%02x", this.octet(i)));
        }
        return text.toString();
    }
}
