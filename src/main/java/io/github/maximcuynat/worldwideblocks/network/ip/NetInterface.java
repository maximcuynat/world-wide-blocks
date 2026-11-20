package io.github.maximcuynat.worldwideblocks.network.ip;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * A network interface of a machine: the loopback {@code lo}, or an Ethernet card on a port of the device
 * ({@code eth0}). Interfaces are always administratively up: no command of lot 1c-1 takes them down.
 */
public final class NetInterface {
    /** What the interface is. */
    public enum Kind {
        LOOPBACK, ETHERNET
    }

    /** Port of the device of a virtual interface: the loopback has none. */
    public static final int NO_PORT = -1;

    private final int index;
    private final String name;
    private final Kind kind;
    private final int port;
    private final MacAddress mac;
    private final int mtu;
    private boolean carrier;
    private final List<InterfaceAddress> addresses = new ArrayList<>();

    NetInterface(int index, String name, Kind kind, int port, MacAddress mac, int mtu, boolean carrier) {
        this.index = index;
        this.name = Objects.requireNonNull(name, "name");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.port = port;
        this.mac = Objects.requireNonNull(mac, "mac");
        this.mtu = mtu;
        this.carrier = carrier;
    }

    /** Interface number ({@code ifindex}), from 1, in the order the interfaces were created. */
    public int index() {
        return this.index;
    }

    public String name() {
        return this.name;
    }

    public Kind kind() {
        return this.kind;
    }

    /** Port of the device behind this interface, or {@link #NO_PORT}. */
    public int port() {
        return this.port;
    }

    public MacAddress mac() {
        return this.mac;
    }

    public int mtu() {
        return this.mtu;
    }

    /** True when the link is up: a cable is plugged into the port (always true for the loopback). */
    public boolean carrier() {
        return this.carrier;
    }

    void setCarrier(boolean carrier) {
        this.carrier = carrier;
    }

    /** IPv4 addresses, in the order they were added. */
    public List<InterfaceAddress> addresses() {
        return List.copyOf(this.addresses);
    }

    List<InterfaceAddress> mutableAddresses() {
        return this.addresses;
    }
}
