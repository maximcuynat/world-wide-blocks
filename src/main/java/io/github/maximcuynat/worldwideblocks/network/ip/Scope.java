package io.github.maximcuynat.worldwideblocks.network.ip;

/**
 * Scope of an address or a route, as Linux names it ({@code rtnl_rtscope_n2a} of iproute2): how far the address
 * is valid, or how far the destination of the route lies.
 */
public enum Scope {
    /** Valid everywhere ({@code RT_SCOPE_UNIVERSE}); {@code ip route} does not write it, {@code ip addr} does. */
    GLOBAL("global"),
    /** Valid on the link of the interface: a directly connected network. */
    LINK("link"),
    /** Valid on this machine only: the loopback. */
    HOST("host");

    private final String text;

    Scope(String text) {
        this.text = text;
    }

    /** Name written by iproute2. */
    public String text() {
        return this.text;
    }
}
