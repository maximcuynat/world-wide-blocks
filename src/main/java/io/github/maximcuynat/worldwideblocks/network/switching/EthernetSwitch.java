package io.github.maximcuynat.worldwideblocks.network.switching;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;

/**
 * Transparent Ethernet switching (IEEE 802.1D bridge without STP): it learns the source MAC of
 * each received frame, relays a frame to the port where its destination is, and broadcasts it on
 * all the other ports when the destination is unknown or the frame is addressed to all.
 *
 * <p>Reused as is by the box (lot 1b) and by the desk switches (lot 1d). The ports
 * are the port numbers of the device that holds it. A device that has its own service on the
 * network (the box and its DHCP server in lot 1b-2, a managed switch in lot 8b) plugs it in through
 * the local point {@link #LOCAL_PORT}, like an internal host; a desk switch has none.
 *
 * <p>It only decides: sending the frames and the loop protection are the business
 * of the simulation. Plain Java, no Minecraft class.
 */
public final class EthernetSwitch {
    /** Number of the local point: never a real port number, which starts at 0. */
    public static final int LOCAL_PORT = -1;

    private final List<Integer> ports;
    private final boolean localPort;
    private final MacTable table;

    /**
     * @param ports     numbers of the switched ports, in broadcast order
     * @param localPort true if the device has a service plugged into the local point
     */
    public EthernetSwitch(List<Integer> ports, boolean localPort, long agingTicks) {
        for (int port : ports) {
            if (port < 0) {
                throw new IllegalArgumentException("Negative port number: " + port);
            }
        }
        this.ports = List.copyOf(ports);
        this.localPort = localPort;
        this.table = new MacTable(agingTicks);
    }

    /**
     * A frame arrives through {@code ingress} (a switched port or {@link #LOCAL_PORT}): the switch learns
     * its source, then returns the ports to relay it through. An empty list means the frame
     * is filtered (its destination is behind the very port it came from).
     */
    public List<Integer> receive(int ingress, EthernetFrame frame, long now) {
        if (ingress != LOCAL_PORT && !this.ports.contains(ingress)) {
            throw new IllegalArgumentException("Port " + ingress + " is not switched");
        }
        this.table.learn(frame.source(), ingress, now);
        if (!frame.destination().isMulticast()) {
            OptionalInt known = this.table.lookup(frame.destination(), now);
            if (known.isPresent()) {
                return known.getAsInt() == ingress ? List.of() : List.of(known.getAsInt());
            }
        }
        return this.floodFrom(ingress);
    }

    /** The cable of this port was just removed: everything learned behind it is forgotten. */
    public void linkDown(int port) {
        this.table.flushPort(port);
    }

    /** To call regularly: removes the expired entries. */
    public void purgeExpired(long now) {
        this.table.purgeExpired(now);
    }

    public boolean switches(int port) {
        return this.ports.contains(port);
    }

    /** Learning table, read access (status panel, future analyzer). */
    public List<MacTable.Entry> entries() {
        return this.table.entries();
    }

    private List<Integer> floodFrom(int ingress) {
        List<Integer> egress = new ArrayList<>(this.ports.size() + 1);
        for (int port : this.ports) {
            if (port != ingress) {
                egress.add(port);
            }
        }
        if (this.localPort && ingress != LOCAL_PORT) {
            egress.add(LOCAL_PORT);
        }
        return egress;
    }
}
