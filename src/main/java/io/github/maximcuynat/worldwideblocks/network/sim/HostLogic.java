package io.github.maximcuynat.worldwideblocks.network.sim;

import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.protocol.PresencePayload;

/**
 * Computer (PC tower, laptop): a network card per port, which keeps only the frames addressed
 * to it or broadcast to all.
 *
 * <p>Lot 1b-1: the computer has no protocol yet. It broadcasts a presence frame when
 * its cable is connected then every 30 s, so a switch learns its MAC. <b>Scaffolding to remove in
 * lot 1c</b>, when DHCP (1b-2) and ARP (1c) produce real traffic.
 */
public final class HostLogic implements DeviceLogic {
    /** 30 game seconds between two presence frames, well below the 300 s expiry of switches. */
    public static final long PRESENCE_INTERVAL_TICKS = 30L * 20L;

    /** Presence timer; the stale generation of a cable removed then plugged back in is ignored. */
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
            return; // not for this network card
        }
        // Lot 1b-2: the frame will be handed to the computer's IP stack (DHCP client).
    }

    private void announce(DeviceContext context, int port) {
        context.send(port, new EthernetFrame(context.mac(port), MacAddress.BROADCAST, PresencePayload.INSTANCE));
        context.schedule(PRESENCE_INTERVAL_TICKS, new PresenceTimer(port, this.generation[port]));
    }
}
