Title: Remove the presence frame scaffolding
Labels: type: chore, area: protocols, priority: medium
Milestone: lot 1c

The presence frame (EtherType 0x88B5, sent when the cable is plugged and every 30 s) was scaffolding so that the box learns the MAC of the PCs before DHCP existed. With DHCP and ARP producing real traffic, it goes.

- [ ] Frame type and its timers removed
- [ ] Tests of MAC learning still green with DHCP and ARP traffic only
- [ ] `docs/plan-lots.md` updated
