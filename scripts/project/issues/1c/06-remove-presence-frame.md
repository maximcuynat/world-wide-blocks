Title: Remove the presence frame scaffolding
Labels: type: chore, area: protocols, priority: medium
Milestone: lot 1c-1

The presence frame (EtherType 0x88B5, sent when the cable is plugged and every 30 s) was scaffolding so that the box learns the MAC of the PCs before DHCP existed. With DHCP and ARP producing real traffic, it goes.

- [ ] Frame type and its timers removed
- [ ] Tests of MAC learning still green with DHCP and ARP traffic only
- [ ] `docs/lot-plan.md` updated
- [ ] Capture scenarios of lots 1b-1 and 1b-2 (issue #52) take the MACs learned by the box within 300 s of building the scene; the addresses lent stay shown (accepted by Max at the start of lot 1c-1)
