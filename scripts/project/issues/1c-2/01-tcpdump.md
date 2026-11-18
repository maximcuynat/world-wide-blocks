Title: tcpdump command on the frame tap
Labels: type: feature, area: interface, priority: high
Milestone: lot 1c-2

`tcpdump` shows one line per frame that really goes through the interface of the machine, read from `FrameTap`. Design in `docs/lot-plan.md`, lot 1c-2.

- [ ] Only the frames of the machine's own interface: incoming, outgoing, and the broadcasts it receives; never the traffic of other machines
- [ ] Output in the real format of tcpdump, with the information that really exists in the engine (ARP, IPv4, ICMP, UDP, DHCP); timestamp from the tick plus the latency carried by the frame
- [ ] Runs until Ctrl+C, then the summary line of the real tool; goes on when the screen is hidden
- [ ] Options `-i eth0`, `-n` (accepted, same output while there is no DNS), `-e` (MAC addresses), `-c n`
- [ ] Filters with a closed grammar that really filters the frames: `arp`, `icmp`, `udp`, `port <n>`, `host <address>`; anything else is refused with an error
- [ ] Outputs compared with a real machine before writing their tests
- [ ] Unit tests of the formatting and of the filters, in plain Java
