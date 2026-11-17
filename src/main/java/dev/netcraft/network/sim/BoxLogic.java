package dev.netcraft.network.sim;

import java.util.List;

import dev.netcraft.network.protocol.EthernetFrame;
import dev.netcraft.network.switching.EthernetSwitch;
import dev.netcraft.network.switching.MacTable;

/**
 * Box internet : un switch sur ses ports réseau local. Son port d'arrivée de ligne n'en fait pas
 * partie et reste inactif jusqu'au lot 3. Au lot 1b-2, son serveur DHCP se branchera sur le point
 * local du switch.
 */
public final class BoxLogic implements DeviceLogic {
    /** Fréquence de nettoyage des entrées expirées : une fois par seconde de jeu. */
    static final long PURGE_INTERVAL_TICKS = 20L;

    private record PurgeTimer() {
    }

    private final EthernetSwitch lan;

    /** @param lanPorts numéros des ports réseau local */
    public BoxLogic(List<Integer> lanPorts) {
        this.lan = new EthernetSwitch(lanPorts, false, MacTable.DEFAULT_AGING_TICKS);
    }

    @Override
    public void onStart(DeviceContext context) {
        context.schedule(PURGE_INTERVAL_TICKS, new PurgeTimer());
    }

    @Override
    public void onLinkUp(DeviceContext context, int port) {
    }

    @Override
    public void onLinkDown(DeviceContext context, int port) {
        if (this.lan.switches(port)) {
            this.lan.linkDown(port);
        }
    }

    @Override
    public void onFrame(DeviceContext context, int port, EthernetFrame frame) {
        if (!this.lan.switches(port)) {
            return; // arrivée de ligne : inactive jusqu'au lot 3
        }
        for (int egress : this.lan.receive(port, frame, context.now())) {
            context.relay(egress, frame);
        }
    }

    @Override
    public void onTimer(DeviceContext context, Object key) {
        if (key instanceof PurgeTimer) {
            this.lan.purgeExpired(context.now());
            context.schedule(PURGE_INTERVAL_TICKS, key);
        }
    }

    @Override
    public boolean bridges() {
        return true;
    }

    /** MAC apprises derrière chaque port réseau local (indicateur). */
    public List<MacTable.Entry> learnedMacs() {
        return this.lan.entries();
    }

    /** Augmente à chaque changement visible des MAC apprises. */
    public long version() {
        return this.lan.version();
    }
}
