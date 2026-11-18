package io.github.maximcuynat.worldwideblocks.network;

import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServer;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServerConfig;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpStore;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.sim.NetworkSimulation;
import net.minecraft.server.MinecraftServer;

/**
 * What the trial scenes and the game tests need from the engine that has no public entry point: the
 * "DHCP server enabled" setting of a box (no interface in lot 1b-2), and the simulation and DHCP memory of
 * network data reread from disk. It lives in the package of the engine, in the {@code gametest} source set,
 * which is absent from the jar: the mod itself gains no public method.
 */
public final class NetworkTestAccess {

    private NetworkTestAccess() {
    }

    /** Turns the DHCP server of a box on or off, as the future interface of the box will. */
    public static void setDhcpServerEnabled(MinecraftServer server, DeviceId box, boolean enabled) {
        NetworkManager network = NetworkManager.of(server);
        requireBox(network, box);
        network.dhcp().server(box).setEnabled(enabled);
    }

    /**
     * Lends the whole pool of the box to clients that do not exist (MACs {@code 02:ff:00:00:…}), through its
     * real server, at the given game time: the next computer finds no address left.
     *
     * @return the number of addresses lent
     */
    public static int fillDhcpPool(MinecraftServer server, DeviceId box, long now) {
        NetworkManager network = NetworkManager.of(server);
        requireBox(network, box);
        DhcpServerConfig config = DhcpServerConfig.INTERNET_BOX;
        DhcpServer dhcp = new DhcpServer(config, network.dhcp().server(box));
        int lent = 0;
        for (int i = 0; i < config.poolSize(); i++) {
            MacAddress client = new MacAddress(0x02_FF_00_00_00_00L + i);
            Optional<DhcpMessage> offer = dhcp.handle(DhcpMessage.discover(i, client), now);
            if (offer.isPresent()) {
                dhcp.handle(DhcpMessage.requestSelected(i, client, config.serverAddress(), offer.get().yourAddress()), now);
                lent++;
            }
        }
        return lent;
    }

    /** The simulation of network data reread from disk, as after a server restart. */
    public static NetworkSimulation simulation(NetworkSavedData data) {
        return data.simulation();
    }

    /** The DHCP memory of network data reread from disk. */
    public static DhcpStore dhcp(NetworkSavedData data) {
        return data.dhcp();
    }

    private static void requireBox(NetworkManager network, DeviceId box) {
        if (network.node(box).filter(node -> node.role().hasService()).isEmpty()) {
            throw new IllegalArgumentException("Not a box: " + box);
        }
    }
}
