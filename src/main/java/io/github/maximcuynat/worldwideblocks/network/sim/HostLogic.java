package io.github.maximcuynat.worldwideblocks.network.sim;

import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.protocol.PresencePayload;

/**
 * Poste (tour PC, portable) : une carte réseau par port, qui ne garde que les trames qui lui sont
 * adressées ou diffusées à tous.
 *
 * <p>Lot 1b-1 : le poste n'a encore aucun protocole. Il diffuse une trame de présence au branchement
 * de son câble puis toutes les 30 s, pour qu'un switch apprenne sa MAC. <b>Échafaudage à retirer au
 * lot 1c</b>, quand le DHCP (1b-2) et l'ARP (1c) produiront du vrai trafic.
 */
public final class HostLogic implements DeviceLogic {
    /** 30 s de jeu entre deux trames de présence, bien moins que les 300 s d'expiration des switchs. */
    public static final long PRESENCE_INTERVAL_TICKS = 30L * 20L;

    /** Minuterie de présence ; la génération périmée d'un câble retiré puis rebranché est ignorée. */
    private record PresenceTimer(int port, long generation) {
    }

    private final long[] generation;

    public HostLogic(int portCount) {
        this.generation = new long[portCount];
    }

    @Override
    public void onLinkUp(DeviceContext context, int port) {
        this.generation[port]++;
        this.announce(context, port);
    }

    @Override
    public void onLinkDown(DeviceContext context, int port) {
        this.generation[port]++;
    }

    @Override
    public void onTimer(DeviceContext context, Object key) {
        if (key instanceof PresenceTimer timer && timer.generation() == this.generation[timer.port()]
                && context.isLinked(timer.port())) {
            this.announce(context, timer.port());
        }
    }

    @Override
    public void onFrame(DeviceContext context, int port, EthernetFrame frame) {
        MacAddress destination = frame.destination();
        if (!destination.isBroadcast() && !destination.equals(context.mac(port))) {
            return; // pas pour cette carte réseau
        }
        // Lot 1b-2 : la trame sera remise à la pile IP du poste (client DHCP).
    }

    private void announce(DeviceContext context, int port) {
        context.send(port, new EthernetFrame(context.mac(port), MacAddress.BROADCAST, PresencePayload.INSTANCE));
        context.schedule(PRESENCE_INTERVAL_TICKS, new PresenceTimer(port, this.generation[port]));
    }
}
