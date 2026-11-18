Title: IPv4, UDP and DHCP message layers
Labels: type: feature, area: protocols, priority: high
Milestone: lot 1b-2

Add the layers that carry DHCP inside Ethernet frames, each one as a variant of the layer below (`network/protocol/`), like the Ethernet layer of lot 1b-1.

- [ ] IPv4 packet in an Ethernet frame (EtherType 0x0800): source, destination, protocol
- [ ] UDP datagram in an IPv4 packet: source and destination ports (DHCP: 67 server, 68 client)
- [ ] DHCP message (RFC 2131): DISCOVER, OFFER, REQUEST, ACK, NAK, transaction id, client MAC, offered address, options (subnet mask, router, DNS server, lease time, server id)
- [ ] Unit tests, in plain Java

Design: `docs/lot-plan.md`, lot 1b, "Chosen architecture".
