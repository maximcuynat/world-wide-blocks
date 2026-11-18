package io.github.maximcuynat.worldwideblocks.network;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpBinding;

/**
 * The memory of a box's DHCP server as written in the save (field {@code dhcp_servers}, since lot 1b-2).
 *
 * @param enabled  the "DHCP server enabled" setting
 * @param bindings what the server remembers of each client
 */
record SavedDhcpServer(DeviceId box, boolean enabled, List<DhcpBinding> bindings) {

    SavedDhcpServer {
        bindings = List.copyOf(bindings);
    }
}
