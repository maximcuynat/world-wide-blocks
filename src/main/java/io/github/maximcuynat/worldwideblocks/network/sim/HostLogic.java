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
import io.github.maximcuynat.worldwideblocks.os.HostName;
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
 * <p>It sends only real traffic: DHCP, ARP, and what its commands send. A computer with an address that runs
 * nothing is silent until the renewal of its lease at T1 (600 s), so a switch forgets its MAC after 300 s and
 * learns it again at its next frame, as with a real machine (the presence frame of lot 1b-1, which kept it, was
 * removed by issue #15).
 */
public final class HostLogic implements DeviceLogic {
    /** Timer of the DHCP client of a port. */
    private record DhcpTimer(int port, DhcpClient.Timer timer) {
    }

    /** Timer of the operating system or of its IP stack. */
    private record SystemTimer(Object key) {
    }

    /** Block identifier of the computer, for its host name. */
    private final String deviceType;
    private final DhcpClient[] dhcp;
    private final Ipv4Stack stack = new Ipv4Stack();
    private final OperatingSystem system = new OperatingSystem(this.stack, CommandRegistry.standard());
    /** Interface of each port, created at the first call, which knows the MACs. */
    private NetInterface[] interfaces;

    /**
     * @param deviceType block identifier of the computer, for its host name ({@link HostName})
     * @param memories   saved lease of each port, which its DHCP client asks for again after a server restart
     */
    public HostLogic(String deviceType, int portCount, RandomGenerator random, IntFunction<DhcpClient.Memory> memories) {
        this.deviceType = deviceType;
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
        this.dhcp[port].linkUp(this.port(context, port));
    }

    @Override
    public void onLinkDown(DeviceContext context, int port) {
        this.stack.setCarrier(this.interfaces(context)[port], false);
        this.dhcp[port].linkDown();
    }

    @Override
    public void onTimer(DeviceContext context, Object key) {
        if (key instanceof DhcpTimer timer && context.isLinked(timer.port())) {
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
    public void onConsoleInterrupt(DeviceContext context, String typed) {
        this.interfaces(context);
        this.system.localConsole().interrupt(new Machine(context), typed);
    }

    @Override
    public void onConsoleEndOfInput(DeviceContext context) {
        this.interfaces(context);
        this.system.localConsole().endOfInput();
    }

    @Override
    public void onConsoleClear(DeviceContext context) {
        this.interfaces(context);
        this.system.localConsole().clearScreen();
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
            if (this.interfaces.length > 0) {
                this.system.setHostName(HostName.of(this.deviceType, context.mac(0)));
            }
        }
        return this.interfaces;
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
