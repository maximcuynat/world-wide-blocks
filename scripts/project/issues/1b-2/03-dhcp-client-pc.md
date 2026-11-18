Title: DHCP client of the PCs
Labels: type: feature, area: protocols, priority: high
Milestone: lot 1b-2

A PC plugged into the box gets its address through a real DHCP exchange (RFC 2131), and loses it when unplugged.

- [ ] DISCOVER, OFFER, REQUEST, ACK with a transaction id, retries when nothing answers
- [ ] Renewal at 50% of the lease (unicast to the server), then at 87.5% (broadcast)
- [ ] Address dropped when the cable is unplugged or the lease expires
- [ ] Unit tests on the simulation timers

Design: `docs/lot-plan.md`, lot 1b.
