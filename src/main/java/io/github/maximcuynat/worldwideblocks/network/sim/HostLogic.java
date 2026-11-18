package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.List;
import java.util.function.IntFunction;
import java.util.random.RandomGenerator;

import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClient;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpFrames;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.protocol.PresencePayload;

/**
 * Computer (PC tower, laptop): a network card per port, which keeps only the frames addressed
 * to it or broadcast to all, and a DHCP client per port, which gets the address of that port.
 *
 * <p>It also broadcasts a presence frame when its cable is connected then every 30 s, so a switch
 * keeps its MAC: <b>scaffolding to remove in lot 1c</b> (issue #15). DHCP alone does not do it yet: a
 * computer with an address is silent until T1 (600 s), while a switch forgets a MAC after 300 s.
 */
public final class HostLogic implements DeviceLogic {
    /** 30 game seconds between two presence frames, well below the 300 s expiry of switches. */
    public static final long PRESENCE_INTERVAL_TICKS = 30L * 20L;

    /** Presence timer; the stale generation of a cable removed then plugged back in is ignored. */
    private record PresenceTimer(int port, long generation) {
    }

    /** Timer of the DHCP client of a port. */
    private record DhcpTimer(int port, DhcpClient.Timer timer) {
    }

    private final long[] generation;
    private final DhcpClient[] dhcp;

    /**
     * @param memories saved lease of each port, which its DHCP client asks for again after a server restart
     */
    public HostLogic(int portCount, RandomGenerator random, IntFunction<DhcpClient.Memory> memories) {
        this.generation = new long[portCount];
        this.dhcp = new DhcpClient[portCount];
        for (int i = 0; i < portCount; i++) {
            this.dhcp[i] = new DhcpClient(random, memories.apply(i));
        }
    }

    @Override
    public void onLinkUp(DeviceContext context, int port) {
        this.generation[port]++;
        this.announce(context, port);
        this.dhcp[port].linkUp(new Port(context, port));
    }

    @Override
    public void onLinkDown(DeviceContext context, int port) {
        this.generation[port]++;
        this.dhcp[port].linkDown();
    }

    @Override
    public void onTimer(DeviceContext context, Object key) {
        if (key instanceof PresenceTimer timer && timer.generation() == this.generation[timer.port()]
                && context.isLinked(timer.port())) {
            this.announce(context, timer.port());
        } else if (key instanceof DhcpTimer timer && context.isLinked(timer.port())) {
            this.dhcp[timer.port()].onTimer(new Port(context, timer.port()), timer.timer());
        }
    }

    @Override
    public void onFrame(DeviceContext context, int port, EthernetFrame frame) {
        MacAddress destination = frame.destination();
        if (!destination.isBroadcast() && !destination.equals(context.mac(port))) {
            return; // not for this network card
        }
        DhcpFrames.toClient(frame).ifPresent(message -> this.dhcp[port].receive(new Port(context, port), frame.source(), message));
    }

    /** DHCP client of a port (status panel). */
    public DhcpClient dhcpClient(int port) {
        return this.dhcp[port];
    }

    private void announce(DeviceContext context, int port) {
        context.send(port, new EthernetFrame(context.mac(port), MacAddress.BROADCAST, PresencePayload.INSTANCE));
        context.schedule(PRESENCE_INTERVAL_TICKS, new PresenceTimer(port, this.generation[port]));
    }

    /** A port of the computer, as its DHCP client sees it during the current call. */
    private record Port(DeviceContext context, int index) implements DhcpClient.Port {
        @Override
        public long now() {
            return this.context.now();
        }

        @Override
        public MacAddress mac() {
            return this.context.mac(this.index);
        }

        @Override
        public void send(EthernetFrame frame) {
            this.context.send(this.index, frame);
        }

        @Override
        public void schedule(long delayTicks, DhcpClient.Timer timer) {
            this.context.schedule(delayTicks, new DhcpTimer(this.index, timer));
        }

        @Override
        public void conflict(List<MacAddress> servers) {
            this.context.report(new SimulationEvent.DhcpConflict(this.context.device(), servers));
        }
    }
}
