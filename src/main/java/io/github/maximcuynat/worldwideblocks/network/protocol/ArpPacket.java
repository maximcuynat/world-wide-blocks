package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Objects;

/**
 * ARP packet (RFC 826), carried by an Ethernet frame of type {@link EtherType#ARP}: who has an IPv4 address, and
 * which hardware address answers for it. The fixed fields of an Ethernet and IPv4 packet are not simulated:
 * hardware type 1 (Ethernet), protocol type 0x0800 (IPv4), hardware address length 6, protocol address length 4.
 *
 * <p>A request is broadcast with a zero target hardware address, as Linux sends it ({@code arp_create},
 * {@code net/ipv4/arp.c}: the target hardware address is zeroed when none is given); a reply goes to the asker.
 *
 * @param senderMac hardware address of the sender ({@code ar$sha})
 * @param senderIp  protocol address of the sender ({@code ar$spa})
 * @param targetMac hardware address of the target ({@code ar$tha}), zero in a request
 * @param targetIp  protocol address of the target ({@code ar$tpa})
 */
public record ArpPacket(Operation operation, MacAddress senderMac, Ipv4Address senderIp, MacAddress targetMac,
        Ipv4Address targetIp) implements EthernetPayload {
    /** Hardware address of a request's target: unknown, all zeros. */
    public static final MacAddress UNKNOWN = new MacAddress(0);

    /** The operation field ({@code ar$op}), with its value from RFC 826. */
    public enum Operation {
        REQUEST(1), REPLY(2);

        private final int value;

        Operation(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    public ArpPacket {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(senderMac, "senderMac");
        Objects.requireNonNull(senderIp, "senderIp");
        Objects.requireNonNull(targetMac, "targetMac");
        Objects.requireNonNull(targetIp, "targetIp");
    }

    /** Who has {@code target}? Tell {@code senderIp}. */
    public static ArpPacket request(MacAddress senderMac, Ipv4Address senderIp, Ipv4Address target) {
        return new ArpPacket(Operation.REQUEST, senderMac, senderIp, UNKNOWN, target);
    }

    /** The answer to {@code request}: {@code ownIp} is at {@code ownMac}. */
    public static ArpPacket replyTo(ArpPacket request, MacAddress ownMac) {
        return new ArpPacket(Operation.REPLY, ownMac, request.targetIp(), request.senderMac(), request.senderIp());
    }

    @Override
    public EtherType etherType() {
        return EtherType.ARP;
    }
}
