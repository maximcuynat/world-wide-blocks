package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.List;

import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.Latency;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpFrames;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServer;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServerConfig;
import io.github.maximcuynat.worldwideblocks.network.ip.InterfaceAddress;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Prefix;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.ip.Scope;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.switching.EthernetSwitch;
import io.github.maximcuynat.worldwideblocks.network.switching.MacTable;

/**
 * Internet box: a switch on its local network ports, with a computer inside plugged into the switch's local point,
 * with its own MAC ({@link DeviceContext#serviceMac()}). That computer has an IPv4 stack with the address of the box,
 * 192.168.1.1/24, permanent, and its DHCP server bound to UDP port 67. That computer is the router of the network:
 * it forwards, and with no route beyond its local network until lot 6c, it answers a packet for another network with
 * a destination unreachable, code 0. The line input port is not part of the switch and stays inactive until lot 3.
 *
 * <p>Latency ({@link Latency}): a frame crossing the switch takes {@link Latency#SWITCH_NANOS}; a frame for the
 * computer inside also takes {@link Latency#COMPUTER_NANOS}, and its answers cross the switch again.
 */
public final class BoxLogic implements DeviceLogic {
    /**
     * Cleanup frequency of expired entries: once per game second. An expired entry
     * is no longer used for switching, but stays visible in the table until the next cleanup.
     */
    public static final long PURGE_INTERVAL_TICKS = 20L;

    private record PurgeTimer() {
    }

    /** Timer of the IP stack of the box's computer. */
    private record StackTimer(Object key) {
    }

    private final EthernetSwitch lan;
    private final DhcpServer dhcp;
    private final Ipv4Stack stack = new Ipv4Stack();
    /** Interface of the box's computer on the local point, created at the first call, which knows its MAC. */
    private NetInterface local;

    /**
     * @param lanPorts numbers of the local network ports
     * @param dhcp     the box's DHCP server, with its saved memory
     */
    public BoxLogic(List<Integer> lanPorts, DhcpServer dhcp) {
        this.lan = new EthernetSwitch(lanPorts, true, MacTable.DEFAULT_AGING_TICKS);
        this.dhcp = dhcp;
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
            if (egress == EthernetSwitch.LOCAL_PORT) {
                this.serve(context, frame);
            } else {
                context.relay(egress, frame);
            }
        }
    }

    /** A frame reached the local point: it goes to the box's computer, whose answers cross the switch again. */
    private void serve(DeviceContext context, EthernetFrame frame) {
        context.elapse(Latency.COMPUTER_NANOS + Latency.SWITCH_NANOS);
        this.stack.receive(new Local(context, this.lan), this.local(context), frame);
    }

    /** A frame crossing the switch. */
    @Override
    public long processingNanos() {
        return Latency.SWITCH_NANOS;
    }

    /**
     * Interface of the box's computer, with the address of the box: the server address of its DHCP configuration,
     * in the network of its mask.
     */
    private NetInterface local(DeviceContext context) {
        if (this.local == null) {
            this.local = this.stack.addEthernet("br0", NetInterface.NO_PORT, context.serviceMac(), true);
            this.stack.setForwarding(true);
            DhcpServerConfig config = this.dhcp.config();
            int length = Ipv4Prefix.lengthOf(config.subnetMask());
            this.stack.addAddress(this.local, new InterfaceAddress(config.serverAddress(), length,
                    Optional.of(Ipv4Prefix.of(config.serverAddress(), length).broadcast()), Scope.GLOBAL,
                    InterfaceAddress.FOREVER));
            this.stack.bindUdp(DhcpMessage.SERVER_PORT, (call, in, received, packet, datagram) -> DhcpFrames.toServer(received)
                    .flatMap(request -> this.dhcp.handle(request, call.now()))
                    .ifPresent(reply -> call.send(in, DhcpFrames.fromServer(in.mac(), config.serverAddress(), reply))));
        }
        return this.local;
    }

    /** The box's computer during a call: what it sends enters the switch through the local point. */
    private record Local(DeviceContext context, EthernetSwitch lan) implements Ipv4Stack.Context {
        @Override
        public long now() {
            return this.context.now();
        }

        @Override
        public void send(NetInterface out, EthernetFrame frame) {
            for (int egress : this.lan.receive(EthernetSwitch.LOCAL_PORT, frame, this.context.now())) {
                this.context.send(egress, frame);
            }
        }

        @Override
        public void schedule(long delayTicks, Object key) {
            this.context.schedule(delayTicks, new StackTimer(key));
        }

        @Override
        public long latency() {
            return this.context.latency();
        }

        @Override
        public void elapse(long nanos) {
            this.context.elapse(nanos);
        }
    }

    @Override
    public void onTimer(DeviceContext context, Object key) {
        if (key instanceof PurgeTimer) {
            this.lan.purgeExpired(context.now());
            context.schedule(PURGE_INTERVAL_TICKS, key);
        } else if (key instanceof StackTimer timer) {
            this.local(context);
            this.stack.onTimer(new Local(context, this.lan), timer.key());
        }
    }

    @Override
    public boolean bridges() {
        return true;
    }

    /** MACs learned behind the ports; the box's own server, behind the local point, is not one of them. */
    @Override
    public List<MacTable.Entry> learnedMacs() {
        return this.lan.entries().stream().filter(entry -> entry.port() != EthernetSwitch.LOCAL_PORT).toList();
    }

    /** The IPv4 stack of the box's computer. */
    public Ipv4Stack stack() {
        return this.stack;
    }

    /** The box's DHCP server (status panel). */
    public DhcpServer dhcpServer() {
        return this.dhcp;
    }
}
