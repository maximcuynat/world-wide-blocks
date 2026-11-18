Title: ICMP echo and the ping command
Labels: type: feature, area: protocols, priority: high
Milestone: lot 1c

`ping <address>` sends echo requests to another device and shows its replies, or their absence. This is the success criterion of lot 1.

- [ ] ICMP echo request and reply (RFC 792) in IPv4
- [ ] `ping <address>`: a few requests, round-trip time in game ticks, summary line, timeout when nothing answers
- [ ] Unit tests
