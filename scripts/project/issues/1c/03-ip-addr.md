Title: ip commands: addr, link, route, neigh
Labels: type: feature, area: interface, priority: medium
Milestone: lot 1c-1

The `ip` commands show what really exists on the machine, in the format of iproute2. A working subset: other subcommands and options get the error of the real tool.

- [x] `ip addr` (`ip a`, `ip addr show eth0`): interfaces, MAC address, IPv4 address and mask, state of the link, lease time left
- [x] `ip link`: physical state of the interfaces (`LOWER_UP` or `NO-CARRIER`)
- [x] `ip route`: default route and route of the local network, from the DHCP lease
- [x] `ip neigh`: the ARP cache and its states
- [ ] Outputs compared with a real machine before writing their tests
- [x] Unit tests of the outputs, in plain Java
