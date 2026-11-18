package io.github.maximcuynat.worldwideblocks.network.dhcp;

/**
 * States of a DHCP client (RFC 2131, 4.4, figure 5), plus {@link #UNPLUGGED} when no cable is connected.
 * The serialized name is sent to the client for the status panel: never change it once published.
 */
public enum DhcpClientState {
    /** No cable: no address, no exchange. */
    UNPLUGGED,
    /** Starting point: about to look for a server. */
    INIT,
    /** DISCOVER sent, waiting for offers. */
    SELECTING,
    /** Offer chosen, REQUEST sent, waiting for the acknowledgement. */
    REQUESTING,
    /** Address obtained, lease running. */
    BOUND,
    /** Half of the lease (T1) passed: REQUEST sent to the server that lent the address. */
    RENEWING,
    /** 87.5% of the lease (T2) passed: REQUEST broadcast to any server. */
    REBINDING,
    /** Starting with an address remembered from before a server restart. */
    INIT_REBOOT,
    /** REQUEST of the remembered address broadcast, waiting for the acknowledgement. */
    REBOOTING;

    /** True in the states where the client uses an address. */
    public boolean hasAddress() {
        return this == BOUND || this == RENEWING || this == REBINDING;
    }
}
