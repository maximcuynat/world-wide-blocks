package dev.netcraft.network.protocol;

import java.util.Optional;

/**
 * Type d'une trame Ethernet : il dit quel protocole lit son contenu. Les valeurs sont les valeurs
 * officielles du registre IANA « IEEE 802 Numbers » (https://www.iana.org/assignments/ieee-802-numbers),
 * vérifiées le 2026-11-17.
 */
public enum EtherType {
    /** IPv4 (registre IANA, RFC 9542). Utilisé à partir du lot 1b-2 (DHCP). */
    IPV4(0x0800),
    /** ARP, résolution d'adresse (registre IANA, RFC 9542). Utilisé au lot 1c. */
    ARP(0x0806),
    /**
     * « IEEE Std 802 Local Experimental », réservé aux essais. Il ne sert qu'à la trame de présence,
     * échafaudage du lot 1b-1 à retirer au lot 1c (voir {@link PresencePayload}).
     */
    LOCAL_EXPERIMENTAL(0x88B5);

    private final int value;

    EtherType(int value) {
        this.value = value;
    }

    /** Valeur sur 16 bits écrite dans l'en-tête de la trame. */
    public int value() {
        return this.value;
    }

    public static Optional<EtherType> byValue(int value) {
        for (EtherType type : values()) {
            if (type.value == value) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
