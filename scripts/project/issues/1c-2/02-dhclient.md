Title: dhclient command
Labels: type: feature, area: protocols, priority: medium
Milestone: lot 1c-2

`dhclient` acts on the real DHCP client of the interface. Design in `docs/lot-plan.md`, lot 1c-2.

- [ ] `dhclient -v eth0`: runs the real DHCP exchange and shows it (`DHCPDISCOVER`, `DHCPOFFER`, `DHCPREQUEST`, `DHCPACK`, then the bound address)
- [ ] `dhclient -r eth0`: releases the lease for real (DHCPRELEASE to the server) and removes the address and the routes of the interface
- [ ] Outputs compared with a real machine before writing their tests: the assistant runs a script in Docker (a dnsmasq server, a Debian client with `dhclient`) that captures `dhclient -v`, `dhclient -r`, and `ip addr` and `ip route` under a DHCP lease, which lot 1c-1 took from the sources (`dynamic`, `valid_lft`, no `proto dhcp`); `ping` through the box and without a route were already captured on a real machine at lot 1c-1 (#14)
- [ ] Unit tests
