package io.github.maximcuynat.worldwideblocks.network;

import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpLease;

/**
 * The lease of a computer port as written in the save (field {@code dhcp_clients}, since lot 1b-2): after a
 * server restart, the computer asks for that address again (INIT-REBOOT).
 */
record SavedDhcpClient(PortRef port, DhcpLease lease) {
}
