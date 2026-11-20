package io.github.maximcuynat.worldwideblocks.network.ip;

import java.util.Objects;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;

/**
 * A route of the main table: where to send the packets of a destination.
 *
 * @param gateway         next router, or empty for a directly connected network
 * @param device          name of the interface the packets leave through
 * @param preferredSource source address of the packets sent through this route ({@code src})
 */
public record Route(Ipv4Prefix destination, Optional<Ipv4Address> gateway, String device, RouteProtocol protocol,
        Scope scope, Optional<Ipv4Address> preferredSource) {

    public Route {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(gateway, "gateway");
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(preferredSource, "preferredSource");
    }
}
