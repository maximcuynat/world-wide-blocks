package io.github.maximcuynat.worldwideblocks.port;

import io.github.maximcuynat.worldwideblocks.network.PortKind;

/**
 * The connector of a port, which decides the plug drawn in it ({@code docs/cable-design.md}, section 4). A single size
 * for every RJ45 port: 2 x 2 texels (0.5 px).
 */
public enum Connector {
    /** RJ45 jack, 2 x 2 texels. */
    RJ45(PortKind.ETHERNET),
    /** Line input of the box: no cable plugs into it before lot 3, no plug is drawn. */
    LINE(PortKind.LINE);

    private final PortKind kind;

    Connector(PortKind kind) {
        this.kind = kind;
    }

    /** The kind of the port in the network engine. */
    public PortKind kind() {
        return this.kind;
    }
}
