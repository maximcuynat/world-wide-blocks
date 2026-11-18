package io.github.maximcuynat.worldwideblocks.network.protocol;

/**
 * Presence frame: a computer broadcasts it when its cable is connected then every 30 s, so
 * a switch learns its MAC while the computer has no other traffic yet.
 *
 * <p><b>Scaffolding of lot 1b-1, to remove in lot 1c</b>, when DHCP and ARP produce real
 * traffic. It imitates no real protocol: it uses the type reserved for experiments
 * ({@link EtherType#LOCAL_EXPERIMENTAL}) and carries nothing.
 */
public record PresencePayload() implements EthernetPayload {
    public static final PresencePayload INSTANCE = new PresencePayload();

    @Override
    public EtherType etherType() {
        return EtherType.LOCAL_EXPERIMENTAL;
    }
}
