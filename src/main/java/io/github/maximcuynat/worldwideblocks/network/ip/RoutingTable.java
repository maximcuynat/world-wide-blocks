package io.github.maximcuynat.worldwideblocks.network.ip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;

/**
 * The main routing table of a machine, the one {@code ip route} shows. The routes of the loopback live in the
 * local table of Linux and are not part of it.
 */
public final class RoutingTable {
    /**
     * Order of {@code ip route}: by destination address, as the kernel walks the keys of its table, so
     * {@code default} comes first, as in the real output of {@code src/test/resources/linux-real-outputs.txt}.
     * For two routes of the same address, the shorter prefix first: not checked on a real machine, no route of
     * lot 1c-1 is in that case.
     */
    private static final Comparator<Route> DUMP_ORDER = Comparator
            .comparingLong((Route route) -> Integer.toUnsignedLong(route.destination().network().value()))
            .thenComparingInt(route -> route.destination().length());

    private final List<Route> routes = new ArrayList<>();

    /** Adds a route; the same route twice is kept once. */
    void add(Route route) {
        if (!this.routes.contains(route)) {
            this.routes.add(route);
        }
    }

    /** Removes every route that leaves through this interface. */
    void removeDevice(String device) {
        this.routes.removeIf(route -> route.device().equals(device));
    }

    /** Routes in the order {@code ip route} writes them. */
    public List<Route> routes() {
        return this.routes.stream().sorted(DUMP_ORDER).toList();
    }

    /** The route of the longest prefix that holds {@code destination}, if there is one. */
    public Optional<Route> lookup(Ipv4Address destination) {
        return this.routes.stream().filter(route -> route.destination().contains(destination))
                .max(Comparator.comparingInt(route -> route.destination().length()));
    }
}
