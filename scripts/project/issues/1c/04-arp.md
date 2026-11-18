Title: Address resolution (ARP)
Labels: type: feature, area: protocols, priority: high
Milestone: lot 1c-1

Before sending, a PC asks the network which hardware address matches the IP address it targets.

- [ ] ARP request (broadcast) and reply, EtherType 0x0806 (RFC 826)
- [ ] ARP cache per device with expiry
- [ ] The box answers for its own address (192.168.1.1)
- [ ] Unit tests
