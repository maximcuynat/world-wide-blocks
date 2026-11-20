Title: Detect IP address conflicts
Labels: type: idea

Idea from the maintainer (lot 1c-1): two devices with the same IPv4 address on one network are noticed, as on a real machine. Before using an address, a host checks with ARP probes that nobody holds it (RFC 5227, IPv4 Address Conflict Detection: probes with a sender address of 0.0.0.0, then an announcement; written from memory, to verify); a DHCP client that finds the offered address taken declines it (DHCPDECLINE, RFC 2131 4.4.1, to verify). Today the engine does neither: `docs/lot-plan.md` lists "no DECLINE, RELEASE or ARP probe of the offered address" among the accepted deviations of lot 1b-2.

Seen in the `chain` scene of lot 1c-1: both boxes are 192.168.1.1, and a computer behind the second box resolves that address to the nearer box.

To check before designing it: which part of a real Linux system does the probing (from memory, to verify: not the kernel, but the DHCP client or the network manager, depending on the distribution), and what the user sees then (log line, state of the address in `ip addr`).
