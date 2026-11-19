Title: dhclient command
Labels: type: feature, area: protocols, priority: medium
Milestone: lot 1c-2

`dhclient` acts on the real DHCP client of the interface. Design in `docs/lot-plan.md`, lot 1c-2.

- [ ] `dhclient -v eth0`: runs the real DHCP exchange and shows it (`DHCPDISCOVER`, `DHCPOFFER`, `DHCPREQUEST`, `DHCPACK`, then the bound address)
- [ ] `dhclient -r eth0`: releases the lease for real (DHCPRELEASE to the server) and removes the address and the routes of the interface
- [ ] Outputs compared with a real machine before writing their tests: a script run by the maintainer in Docker (a dnsmasq server, a Debian client with `dhclient`, a forwarding box without default route) captures `dhclient -v`, `dhclient -r`, `ip addr` and `ip route` under DHCP, `ping` through the box (`Destination Net Unreachable`) and without a route (`ping: connect: Network is unreachable`); it also confirms the formats of lot 1c-1, taken from the sources
- [ ] Unit tests
