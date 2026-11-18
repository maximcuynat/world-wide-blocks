Title: Game tests and check procedure of the network diagnostics
Labels: type: test, area: engine, priority: medium
Milestone: lot 1c-2

Playable at the end of lot 1c-2: `tcpdump` runs on one computer while another `ping`s it, and the ARP request, its reply, then the ICMP echoes scroll by.

- [ ] Game test: a capture on one PC sees the ARP exchange and the echoes of a `ping` from another PC
- [ ] Game test: once the box has learned their MAC addresses, a capture does not see a unicast exchange between two other PCs
- [ ] Game test: `dhclient -r` then `dhclient -v` gives the interface its address back
- [ ] Check procedure of lot 1c-2 in `docs/lot-plan.md` (terminal display, checked with `./gradlew runClient`)
