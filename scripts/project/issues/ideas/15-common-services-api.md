Title: Services API shared by the network engine and the computer OS
Labels: type: idea

A single way to run network services (DHCP, DNS, web, NAT, directory) on any device: in the engine for the box, and in the future OS of the computers. Assessed on 2026-11-18 from the DHCP server of lot 1b-2, whose decision logic (`DhcpServer.handle`) is already independent of frames and of Minecraft. What it would need:

- an IP stack per interface, with services bound to a UDP or TCP port (67 DHCP, 53 DNS, 80 web), and a raw path for DHCP, which works before having an address;
- a generic service context (time, sending through the stack, timers, reports, memory), on the model of `DhcpClient.Port`;
- services bound to an interface rather than to a MAC: the box keeps its internal host on the switch's local point, a computer shares the address of its card;
- a generic saved memory per device and per service (typed sections with their codec), replacing `DhcpStore`, and a configuration per device;
- a life cycle (start, stop, state), generalising the "DHCP server enabled" setting.

Suggested moment: lot 1c, where ARP and ICMP require a real IP layer on the computers anyway.
