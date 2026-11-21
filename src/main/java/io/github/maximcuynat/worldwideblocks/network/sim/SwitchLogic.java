package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.Latency;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.switching.EthernetSwitch;
import io.github.maximcuynat.worldwideblocks.network.switching.MacTable;

/**
 * Desktop switch (lot 1d-1): the switching of the box ({@link EthernetSwitch}) on all its ports, without a local point:
 * it has no service of its own, no address, and sends nothing by itself. A frame crossing it takes
 * {@link Latency#SWITCH_NANOS}.
 */
public final class SwitchLogic implements DeviceLogic {
    private record PurgeTimer() {
    }

    private final EthernetSwitch lan;

    /** @param ports numbers of the switched ports: all the ports of the switch */
    public SwitchLogic(List<Integer> ports) {
        this.lan = new EthernetSwitch(ports, false, MacTable.DEFAULT_AGING_TICKS);
    }

    @Override
    public void onStart(DeviceContext context) {
        context.schedule(BoxLogic.PURGE_INTERVAL_TICKS, new PurgeTimer());
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
            return;
        }
        for (int egress : this.lan.receive(port, frame, context.now())) {
            context.relay(egress, frame);
        }
    }

    @Override
    public void onTimer(DeviceContext context, Object key) {
        if (key instanceof PurgeTimer) {
            this.lan.purgeExpired(context.now());
            context.schedule(BoxLogic.PURGE_INTERVAL_TICKS, key);
        }
    }

    @Override
    public boolean bridges() {
        return true;
    }

    @Override
    public long processingNanos() {
        return Latency.SWITCH_NANOS;
    }

    @Override
    public List<MacTable.Entry> learnedMacs() {
        return this.lan.entries();
    }
}
