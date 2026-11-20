package io.github.maximcuynat.worldwideblocks.network.ip;

/**
 * Who installed a route ({@code rtm_protocol}, names of {@code /etc/iproute2/rt_protos}). {@code ip route} writes
 * it unless it is {@link #BOOT} (iproute2 {@code ip/iproute.c}: {@code r->rtm_protocol != RTPROT_BOOT || show_details}).
 */
public enum RouteProtocol {
    /**
     * Added by a program that gives no protocol: {@code ip route add} without {@code proto}, as Debian's
     * {@code dhclient-script} adds the default route ("If the routing protocol ID is not given, ip assumes protocol
     * boot", ip-route(8)).
     */
    BOOT("boot"),
    /** Added by the kernel with an address: the route of the network of that address. */
    KERNEL("kernel");

    private final String text;

    RouteProtocol(String text) {
        this.text = text;
    }

    public String text() {
        return this.text;
    }
}
