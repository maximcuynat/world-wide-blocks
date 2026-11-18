package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Locale;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Adresse matérielle Ethernet (MAC) sur 48 bits, rangée dans les 48 bits de poids faible de {@code value}.
 * Le premier octet est celui de poids fort : c'est lui qu'on écrit en premier ({@code 02:…}).
 *
 * <p>Java pur, sans classe Minecraft.
 */
public record MacAddress(long value) {
    private static final long MASK = 0xFFFF_FFFF_FFFFL;

    /** Adresse de diffusion : la trame est destinée à tous les appareils du réseau. */
    public static final MacAddress BROADCAST = new MacAddress(MASK);

    /** Premier octet des adresses générées : bit « administrée localement » levé, bit multicast à zéro. */
    private static final long LOCAL_PREFIX = 0x02L << 40;

    public MacAddress {
        if ((value & ~MASK) != 0) {
            throw new IllegalArgumentException("MAC address out of 48 bits: " + Long.toHexString(value));
        }
    }

    /**
     * Adresse unicast administrée localement, tirée au hasard : premier octet {@code 02}, les cinq
     * autres aléatoires. Elle ne peut pas entrer en conflit avec une adresse attribuée par un fabricant.
     */
    public static MacAddress randomLocal(RandomGenerator random) {
        return new MacAddress(LOCAL_PREFIX | (random.nextLong() & 0xFF_FFFF_FFFFL));
    }

    /** Lit une adresse écrite sous la forme {@code 02:4f:a0:00:1b:7c} (majuscules acceptées). */
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

    /** Octet numéro {@code index}, de 0 (le premier écrit) à 5. */
    public int octet(int index) {
        if (index < 0 || index > 5) {
            throw new IndexOutOfBoundsException(index);
        }
        return (int) (this.value >>> (8 * (5 - index))) & 0xFF;
    }

    public boolean isBroadcast() {
        return this.value == MASK;
    }

    /** Adresse de groupe (bit de poids faible du premier octet) ; la diffusion en est un cas particulier. */
    public boolean isMulticast() {
        return (this.octet(0) & 0x01) != 0;
    }

    /** Adresse administrée localement (deuxième bit du premier octet), et non attribuée par un fabricant. */
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
