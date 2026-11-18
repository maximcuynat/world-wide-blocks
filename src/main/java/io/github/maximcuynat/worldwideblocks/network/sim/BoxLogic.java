package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpFrames;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServer;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.switching.EthernetSwitch;
import io.github.maximcuynat.worldwideblocks.network.switching.MacTable;

/**
 * Internet box: a switch on its local network ports, with its DHCP server plugged into the switch's
 * local point, like a computer inside the box with its own MAC ({@link DeviceContext#serviceMac()}). Its
 * line input port is not part of it and stays inactive until lot 3.
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
    private final DhcpServer dhcp;

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

    /** A frame reached the local point: the server keeps those addressed to its MAC or to all. */
    private void serve(DeviceContext context, EthernetFrame frame) {
        MacAddress own = context.serviceMac();
        if (!frame.isBroadcast() && !frame.destination().equals(own)) {
            return;
        }
        DhcpFrames.toServer(frame).flatMap(request -> this.dhcp.handle(request, context.now()))
                .ifPresent(reply -> this.sendFromService(context, reply));
    }

    /** The reply enters the switch through the local point, which sends it on to the right ports. */
    private void sendFromService(DeviceContext context, DhcpMessage reply) {
        EthernetFrame frame = DhcpFrames.fromServer(context.serviceMac(), this.dhcp.config().serverAddress(), reply);
        for (int egress : this.lan.receive(EthernetSwitch.LOCAL_PORT, frame, context.now())) {
            context.send(egress, frame);
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

    /** MACs learned behind the ports; the box's own server, behind the local point, is not one of them. */
    @Override
    public List<MacTable.Entry> learnedMacs() {
        return this.lan.entries().stream().filter(entry -> entry.port() != EthernetSwitch.LOCAL_PORT).toList();
    }

    /** The box's DHCP server (status panel). */
    public DhcpServer dhcpServer() {
        return this.dhcp;
    }
}
