Title: DHCP server in the internet box
Labels: type: feature, area: protocols, priority: high
Milestone: lot 1b-2

The box answers DHCP requests on its local point (the switch of lot 1b-1 plus its own service).

- [ ] Pool 192.168.1.10 to 192.168.1.200 in /24; the box is 192.168.1.1 (gateway and DNS server)
- [ ] Lease of one game day (24,000 ticks)
- [ ] The box remembers the address lent to each MAC and gives it back to the same device
- [ ] Leases saved with the network, and expired leases freed
- [ ] Unit tests: OFFER and ACK, renewal, pool exhausted, same MAC gets the same address

Design: `docs/lot-plan.md`, lot 1b.
