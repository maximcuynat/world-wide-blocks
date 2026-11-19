Title: ICMP echo and the ping command
Labels: type: feature, area: protocols, priority: high
Milestone: lot 1c-1

`ping <address>` sends real echo requests to another device and shows its replies, or the real reason why none comes. This is the success criterion of lot 1. Design in `docs/lot-plan.md`, lot 1c-1.

- [x] ICMP echo request and reply (RFC 792) in IPv4
- [x] `ping <address>`: one request per second until Ctrl+C, then the statistics block; `ping -c n <address>` stops after `n` requests
- [ ] The terminal belongs to the command while it runs; Ctrl+C stops it with any keyboard layout; the command goes on when the screen is hidden
- [x] Round-trip time from the network latency model: latency carried by each frame, added by each cable (length) and each device crossed (processing time of its type); delivery stays instant; documented values with their sources, easy to change; deterministic results
- [x] Game time for the interval between requests, the timeout and the elapsed time of the statistics
- [x] Output of the situation really met, checked on a real machine: no address or no route, absent machine on the local network (ARP fails, `Destination Host Unreachable`), destination outside the local network; a timeout prints nothing, as on Linux
- [ ] Translated advice line after a failure, shown apart from the output, turned off by a client setting
- [x] Unit tests
