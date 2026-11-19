package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.List;
import java.util.function.IntFunction;
import java.util.random.RandomGenerator;

import io.github.maximcuynat.worldwideblocks.network.Latency;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClient;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClientScript;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpFrames;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.ip.NeighbourTable;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Packet;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.protocol.PresencePayload;
import io.github.maximcuynat.worldwideblocks.os.MachineContext;
import io.github.maximcuynat.worldwideblocks.os.OperatingSystem;
import io.github.maximcuynat.worldwideblocks.os.command.CommandRegistry;

/**
 * Computer (PC tower, laptop): an operating system with its IPv4 stack, an Ethernet interface per port
 * ({@code eth0}, {@code eth1}...), and a DHCP client per port, which gets the address of that interface.
 *
 * <p>The DHCP client reads the frames of its interface before the IP layer, like ISC {@code dhclient} through its
 * packet socket on Linux ({@code socket(PF_PACKET, SOCK_RAW, ...)}, {@code common/lpf.c}): an offer is addressed to
 * an address the interface does not have yet. Each change of its
 * lease configures the interface ({@link DhcpClientScript}).
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

    /** Timer of the operating system or of its IP stack. */
    private record SystemTimer(Object key) {
    }

    private final long[] generation;
    private final DhcpClient[] dhcp;
    private final Ipv4Stack stack = new Ipv4Stack();
    private final OperatingSystem system = new OperatingSystem(this.stack, CommandRegistry.standard());
    /** Interface of each port, created at the first call, which knows the MACs. */
    private NetInterface[] interfaces;

    /**
     * @param memories saved lease of each port, which its DHCP client asks for again after a server restart
     */
    public HostLogic(int portCount, RandomGenerator random, IntFunction<DhcpClient.Memory> memories) {
        this.generation = new long[portCount];
        this.dhcp = new DhcpClient[portCount];
        for (int i = 0; i < portCount; i++) {
            int port = i;
            this.dhcp[i] = new DhcpClient(random, memories.apply(i), lease -> {
                if (this.interfaces != null) {
                    DhcpClientScript.apply(this.stack, this.interfaces[port], lease);
                }
            });
        }
    }

    @Override
    public void onStart(DeviceContext context) {
        this.interfaces(context);
    }

    @Override
    public void onLinkUp(DeviceContext context, int port) {
        this.stack.setCarrier(this.interfaces(context)[port], true);
        this.generation[port]++;
        this.announce(context, port);
        this.dhcp[port].linkUp(this.port(context, port));
    }

    @Override
    public void onLinkDown(DeviceContext context, int port) {
        this.stack.setCarrier(this.interfaces(context)[port], false);
        this.generation[port]++;
        this.dhcp[port].linkDown();
    }

    @Override
    public void onTimer(DeviceContext context, Object key) {
        if (key instanceof PresenceTimer timer && timer.generation() == this.generation[timer.port()]
                && context.isLinked(timer.port())) {
            this.announce(context, timer.port());
        } else if (key instanceof DhcpTimer timer && context.isLinked(timer.port())) {
            this.dhcp[timer.port()].onTimer(this.port(context, timer.port()), timer.timer());
        } else if (key instanceof SystemTimer timer) {
            this.interfaces(context);
            this.system.onTimer(new Machine(context), timer.key());
        }
    }

    @Override
    public void onFrame(DeviceContext context, int port, EthernetFrame frame) {
        NetInterface in = this.interfaces(context)[port];
        if (!this.stack.keeps(in, frame)) {
            return; // not for this network card
        }
        DhcpFrames.toClient(frame).ifPresent(message -> this.dhcp[port].receive(this.port(context, port), frame.source(), message));
        this.system.receive(new Machine(context), in, frame);
    }

    @Override
    public void onConsoleLine(DeviceContext context, String line) {
        this.interfaces(context);
        this.system.localConsole().submit(new Machine(context), line);
    }

    @Override
    public void onConsoleInterrupt(DeviceContext context) {
        this.interfaces(context);
        this.system.localConsole().interrupt(new Machine(context));
    }

    /** A computer takes {@link Latency#COMPUTER_NANOS} to receive a frame. */
    @Override
    public long processingNanos() {
        return Latency.COMPUTER_NANOS;
    }

    /** DHCP client of a port (status panel). */
    public DhcpClient dhcpClient(int port) {
        return this.dhcp[port];
    }

    /** The operating system of the computer: its stack, its processes and its console. */
    public OperatingSystem system() {
        return this.system;
    }

    private NetInterface[] interfaces(DeviceContext context) {
        if (this.interfaces == null) {
            this.interfaces = new NetInterface[this.dhcp.length];
            for (int port = 0; port < this.dhcp.length; port++) {
                this.interfaces[port] = this.stack.addEthernet("eth" + port, port, context.mac(port), context.isLinked(port));
            }
        }
        return this.interfaces;
    }

    private void announce(DeviceContext context, int port) {
        context.send(port, new EthernetFrame(context.mac(port), MacAddress.BROADCAST, PresencePayload.INSTANCE));
        context.schedule(PRESENCE_INTERVAL_TICKS, new PresenceTimer(port, this.generation[port]));
    }

    /** The machine as its stack and its operating system see it during the current call. */
    private record Machine(DeviceContext context) implements Ipv4Stack.Context, MachineContext {
        @Override
        public long now() {
            return this.context.now();
        }

        @Override
        public void send(NetInterface out, EthernetFrame frame) {
            this.context.send(out.port(), frame);
        }

        @Override
        public void schedule(long delayTicks, Object key) {
            this.context.schedule(delayTicks, new SystemTimer(key));
        }

        @Override
        public long latency() {
            return this.context.latency();
        }

        @Override
        public void elapse(long nanos) {
            this.context.elapse(nanos);
        }

        @Override
        public Ipv4Stack.Context network() {
            return this;
        }

        @Override
        public void commandFailed(String command, RuntimeException error) {
            this.context.report(new SimulationEvent.CommandFailed(this.context.device(), command, error));
        }
    }

    /** A port of the computer, as its DHCP client sees it during the current call. */
    private Port port(DeviceContext context, int index) {
        return new Port(context, index, this.stack);
    }

    private record Port(DeviceContext context, int index, Ipv4Stack stack) implements DhcpClient.Port {
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

        /** Through the IP stack: routed, and the server found by ARP. */
        @Override
        public void sendUnicast(Ipv4Packet packet, MacAddress serverMac) {
            this.stack.send(new Machine(this.context), packet, NeighbourTable.Failure.NONE);
        }
    }
}
