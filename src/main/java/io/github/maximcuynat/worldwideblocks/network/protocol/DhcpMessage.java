package io.github.maximcuynat.worldwideblocks.network.protocol;

import java.util.Objects;

/**
 * DHCP message (RFC 2131), carried by a UDP datagram from port 68 (client) to port 67 (server), or back
 * (RFC 2131, 4.1). Only the fields the simulation uses are kept (RFC 2131, table 1): {@code xid},
 * the broadcast flag, {@code ciaddr}, {@code yiaddr}, {@code chaddr}, and the options of
 * {@link DhcpOptions}. The "op" field follows from the type ({@link DhcpMessageType#fromServer()}).
 * There is no relay agent: {@code giaddr} is always zero, and {@code siaddr}, {@code sname} and
 * {@code file} (network boot) are not simulated.
 *
 * <p>The factories build each message with the fields required by RFC 2131, table 3 (server) and
 * table 5 (client); the constructor only checks what is true of every message.
 *
 * @param xid           transaction id, chosen by the client and copied by the server in its reply
 * @param broadcast     broadcast flag: the client asks for broadcast replies; never set by the simulated
 *                      clients, which receive unicast before having an address (RFC 2131, 4.1)
 * @param clientAddress {@code ciaddr}: the client's address, only when it already has one (renewal)
 * @param yourAddress   {@code yiaddr}: the address given to the client, in an offer or an acknowledgement
 * @param clientMac     {@code chaddr}: hardware address of the client
 */
public record DhcpMessage(DhcpMessageType type, int xid, boolean broadcast, Ipv4Address clientAddress,
        Ipv4Address yourAddress, MacAddress clientMac, DhcpOptions options) implements UdpPayload {
    /** UDP port of the servers (RFC 2131, 4.1). */
    public static final int SERVER_PORT = 67;
    /** UDP port of the clients (RFC 2131, 4.1). */
    public static final int CLIENT_PORT = 68;

    public DhcpMessage {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(clientAddress, "clientAddress");
        Objects.requireNonNull(yourAddress, "yourAddress");
        Objects.requireNonNull(clientMac, "clientMac");
        Objects.requireNonNull(options, "options");
        if (clientMac.isMulticast()) {
            throw new IllegalArgumentException("Client hardware address must be unicast: " + clientMac);
        }
        if (type.fromServer() && options.serverId().isEmpty()) {
            throw new IllegalArgumentException("A server message carries the server identifier (RFC 2131, table 3)");
        }
    }

    // ---- Client messages (RFC 2131, table 5) ----

    /** Looks for servers (INIT state). */
    public static DhcpMessage discover(int xid, MacAddress client) {
        return new DhcpMessage(DhcpMessageType.DISCOVER, xid, false, Ipv4Address.ANY, Ipv4Address.ANY, client, DhcpOptions.NONE);
    }

    /** Accepts the offer of a server (SELECTING state): its identifier and the offered address, {@code ciaddr} zero. */
    public static DhcpMessage requestSelected(int xid, MacAddress client, Ipv4Address serverId, Ipv4Address offered) {
        return new DhcpMessage(DhcpMessageType.REQUEST, xid, false, Ipv4Address.ANY, Ipv4Address.ANY, client,
                DhcpOptions.selected(serverId, offered));
    }

    /** Asks again for its previous address (INIT-REBOOT state): no server identifier, {@code ciaddr} zero. */
    public static DhcpMessage requestPrevious(int xid, MacAddress client, Ipv4Address previous) {
        return new DhcpMessage(DhcpMessageType.REQUEST, xid, false, Ipv4Address.ANY, Ipv4Address.ANY, client,
                DhcpOptions.requested(previous));
    }

    /**
     * Extends its lease (RENEWING and REBINDING states): {@code ciaddr} filled in, neither server identifier
     * nor requested address.
     */
    public static DhcpMessage requestExtension(int xid, MacAddress client, Ipv4Address current) {
        return new DhcpMessage(DhcpMessageType.REQUEST, xid, false, current, Ipv4Address.ANY, client, DhcpOptions.NONE);
    }

    // ---- Server messages (RFC 2131, table 3) ----

    /** Offers an address, with the configuration and the lease time. */
    public static DhcpMessage offer(DhcpMessage request, Ipv4Address offered, DhcpOptions lease) {
        return reply(DhcpMessageType.OFFER, request, Ipv4Address.ANY, offered, lease);
    }

    /** Confirms the address; {@code ciaddr} is copied from the request (RFC 2131, table 3). */
    public static DhcpMessage ack(DhcpMessage request, Ipv4Address address, DhcpOptions lease) {
        return reply(DhcpMessageType.ACK, request, request.clientAddress(), address, lease);
    }

    /** Refuses the request: the client must start again from the beginning. */
    public static DhcpMessage nak(DhcpMessage request, Ipv4Address serverId) {
        return reply(DhcpMessageType.NAK, request, Ipv4Address.ANY, Ipv4Address.ANY, DhcpOptions.server(serverId));
    }

    private static DhcpMessage reply(DhcpMessageType type, DhcpMessage request, Ipv4Address ciaddr, Ipv4Address yiaddr,
            DhcpOptions options) {
        if (request.type().fromServer()) {
            throw new IllegalArgumentException("Not a client message: " + request.type());
        }
        return new DhcpMessage(type, request.xid(), request.broadcast(), ciaddr, yiaddr, request.clientMac(), options);
    }
}
