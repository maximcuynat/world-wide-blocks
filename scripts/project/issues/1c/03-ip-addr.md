Title: ip commands: addr, link, route, neigh
Labels: type: feature, area: interface, priority: medium
Milestone: lot 1c-1

The `ip` commands show what really exists on the machine, in the format of iproute2. A working subset: other subcommands and options get the error of the real tool.

- [x] `ip addr` (`ip a`, `ip addr show eth0`): interfaces, MAC address, IPv4 address and mask, state of the link, lease time left
- [x] `ip link`: physical state of the interfaces (`LOWER_UP` or `NO-CARRIER`)
- [x] `ip route`: default route and route of the local network, from the DHCP lease
- [x] `ip neigh`: the ARP cache and its states
- [x] Outputs compared with a real machine before writing their tests, for what the reference machine shows (a Debian 13 container: `lo`, a fixed address, `ip link`, `ip route`, `ip neigh`, captured before the tests were written)
- [x] Unit tests of the outputs, in plain Java

Reported to lot 1c-2 (issue file 1c-2/02, "dhclient command"): the lines that no real machine has shown yet, written from the sources of iproute2 and of the kernel: `dynamic` and `valid_lft` of an address under a DHCP lease, `NO-CARRIER` of an unplugged interface, and `qdisc fq_codel` of a physical interface (the veth of the container shows `noqueue`).
