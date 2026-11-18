package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.switching.EthernetSwitch;
import io.github.maximcuynat.worldwideblocks.network.switching.MacTable;

/**
 * Internet box: a switch on its local network ports. Its line input port is not part
 * of it and stays inactive until lot 3. In lot 1b-2, its DHCP server will plug into the
 * switch's local point.
 */
public final class BoxLogic implements DeviceLogic {
    /**
     * Cleanup frequency of expired entries: once per game second. An expired entry
     * is no longer used for switching, but stays visible in the table until the next cleanup.
     */
    public static final long PURGE_INTERVAL_TICKS = 20L;

    private record PurgeTimer() {
    }

    private final EthernetSwitch lan;

    /** @param lanPorts numbers of the local network ports */
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
            return; // line input: inactive until lot 3
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

    @Override
    public List<MacTable.Entry> learnedMacs() {
        return this.lan.entries();
    }
}
