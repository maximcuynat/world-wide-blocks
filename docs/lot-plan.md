# World Wide Blocks: lot plan

Written on 2026-11-17, revised the same day after review; revised again on 2026-11-18 (lot E renamed 1e, lot 6c split in two, tool-crafting decision added; TCP lot 1f, shared console and terminal decisions added); revised on 2026-11-19 (decisions of lot 1c taken with Max, lot 1c split into 1c-1 and 1c-2; findings of the terminal prototype applied to lot 1c-1). Status: proposed, awaiting approval. Lots coded: 0 and 1a (see the status of each lot).

## Principles

- Each lot or sub-lot is small and playable at the end: you can open the game and see that something new works.
- The order follows the real path of a connection: the computer, the home, the street, the neighbourhood, the ISP, then the internet.
- Equipment is generic: no brand, no commercial model.
- During the lots, visual feedback stays minimal: one readable status indicator (connected or not, address obtained or not). Animated lights, lit textures and sounds are grouped in the finishing lot.
- One line per block or item to create. A "block" is placed in the world, an "item" is held in hand.
- The simulation carries real protocol exchanges (principle revised on 2026-11-17, before lot 1b; it replaces the abstract simulation): real Ethernet frames travel through the engine, then real DHCP messages (lot 1b), ARP and ICMP (lot 1c). A frame is delivered instantly: the server puts it in a queue that it processes at each tick, with no block-by-block travel time along the cable. A limit of frames processed per tick protects the server, in particular in multiplayer and when devices are connected in a loop.

## Scope

- Out of scope: the coaxial cable network (cable modem, CMTS head-end, optical node, amplifiers). It stays in the equipment inventory for reference, but no lot covers it.
- Copper stays in the plan as a historical technology: in 2022, Orange sent Arcep its plan to close the copper network [14].

## Open decisions

| Decision | Before lot | Proposal | Status |
|---|---|---|---|
| Laptop battery | 1e | The laptop runs on battery when it is not plugged in and recharges on a socket | To approve |
| Electricity mandatory or not | 1e | "Power required" configuration option, enabled by default; when disabled, all devices work without power | To approve |
| Making cables by hand | 2b | The tools of lot 9a (spool, connector, crimping tool, tester) are brought forward just before 2b, so that the fixed home cabling uses cables cut to the wanted length. Alternative: keep them at 9a and use only finished cords until then. Either way, the length of a cable then lives in the item beside its type and its colour: the type is the item, colour and length are two data components (`docs/cable-design.md`, section 7) | To decide |
| Colour of a cord | 1d-2 | One item per type of cable, the colour a property of the item (eight colours, `design/palette.json`, blue by default); changed by combining the cord with a dye in the crafting table, as leather armour; one creative entry per colour; a right-click with a dye on a port with a cable recolours that cable (the colour becomes a property of the link, saved and sent to clients); an unplugged cable gives back an item of its colour; tooltip and status panel name the colour; a colour carries no rule of the game (`docs/cable-design.md`, section 7) | Proposed by Max on 2026-11-20 |
| Types of cables | 1e, 3a, 3b, 8b | Each type (copper RJ45, telephone RJ11, fibre, power cord of 1e, console cable of 8b) is a distinct item, recognised by its plug and its thickness, never by its colour; fibres take by default the standard colours of real cords (yellow single-mode, orange or aqua multimode, to check) (`docs/cable-design.md`, sections 4 and 7) | Proposed by Max on 2026-11-20 |
| Step between cables sharing a path | 1d-2 | 0.5 px, a gap of one cable width (preview `cable_lanes`; 0.25 px makes them touch) (`docs/cable-design.md`, section 5) | To approve |
| Scale of rack equipment | 6a | Scale per family: racks at player scale, small equipment enlarged. With the factor 2.4 of the PC tower, a 42U rack would be about 4.8 blocks high | To approve |

## Decisions taken

Decisions of lot 1c, taken with Max on 2026-11-19 in a dedicated session (details in the sections of lots 1c-1 and 1c-2); the last five rows were taken at the start of lot 1c-1, the same day.

| Decision | Lot | Decided | Why |
|---|---|---|---|
| Role of the screen, keyboard and mouse (issue #10) | 1c-1 | The terminal opens directly on the tower and on the laptop, with the same console. Peripherals are not required by default; a connected screen will give access to the graphical mode when it exists; a server option may later require peripherals | A console is a view of a machine that lives on the server, and lot 1c-1 must stay playable without peripherals |
| Laptop gestures | 1c-1 | A right-click on the screen part opens or closes the lid; a right-click on the keyboard part of the open laptop opens the terminal; the item keeps its open or closed state in the inventory and is placed in that state | Everything is built into a laptop, so each part of the model gets its own gesture |
| Language of the terminal | 1c-1 | The output of the commands is literal English, produced by the real processing of the simulated system; help, advice and game messages are translated through language keys, shown apart from the output and can be turned off (`docs/messages.md`, "Terminal") | The player reads what a real Linux shows, and the teaching stays accessible in both languages |
| Keyboard | 1c-1 | The terminal follows the player's keyboard layout, for typed characters and for shortcuts; any typed character is accepted and shown if the font has it; commands follow the Linux conventions | The computer of the game has no keyboard layout of its own |
| Monospaced font | 1c-1 | Monocraft v4.2.1 (SIL OFL 1.1), TrueType, its licence text shipped next to the font file; size 9 gives a 6 GUI pixel cell, as the game font. Confirmed by Max on 2026-11-19 after the prototype (issue #30). Rejected: the game font as it is (proportional, columns do not align), the game font drawn cell by cell (wide glyphs touch their neighbours, narrow letters stand alone in their cell), Spleen (the 5x8 size has no French accents, the 6x12 size has the same 6 px cell, no TrueType file: no gain in density) | Keep the look of the game while the columns stay aligned |
| Appearance of the terminal | 1c-1 | Chosen by Max on 2026-11-19 from the captures of the prototype: dark game popup panel with a white title, grey text (AAAAAA) on black with the game's text shadow, game colours as the ANSI palette (never the dark blue 0000AA for important text), prompt `user@laptop-3f2a:~$` with `user@host` in green 55FF55 and `~` in blue 5555FF, blinking block cursor, rows of 12 px; a text size of its own, by default the largest that gives at least 80 columns (see lot 1c-1) | It looks like Minecraft and behaves like Linux |
| Two players at the same computer | 1c-1 | One player at the keyboard at a time ("computer busy"). One local console per machine, kept in server memory while the machine exists: closing the screen hides it and running commands go on. Read-only viewing later (backlog) | A computer has one screen and one keyboard, and it keeps running when nobody is in front of it |
| `ping` without a count | 1c-1 | Continuous until Ctrl+C, as on Linux; `-c n` for a fixed count. "Ctrl+C: stop" in the title bar while a command runs; after a failure, a translated advice line, marked as a game message, that a client setting turns off | Learn the real conventions, with help kept out of the output of the tool |
| Time shown by `ping` | 1c-1 | A latency computed from the path actually taken: cable lengths, devices crossed, processing time of each type of device. Delivery stays instant in the engine; the values are documented with their sources and easy to change; results are deterministic (jitter maybe later). Game time stays for intervals, timeouts, elapsed time and leases | Replies are delivered in the same tick, so converted ticks would show 0 ms; a computed latency makes the built network measurable (replaces the proposal "1 tick = 50 ms") |
| Frame capture (`tcpdump`) | 1c-2 | In lot 1c-2, between 1c-1 and 1d-1 | Lot 1c-1 keeps its success criterion, and capture arrives before switches, to show unicast and broadcast |
| Scope of lots 1c-1 and 1c-2 | 1c-1, 1c-2 | See the two lots. A working subset of the real tools: what exists really works, everything else returns a consistent error | No command shows a prepared result |
| Format of `ip addr` and `ip route` under DHCP | 1c-1 | The machine behaves like Debian with ifupdown and `dhclient`: `default via 192.168.1.1 dev eth0 ` and `192.168.1.0/24 dev eth0 proto kernel scope link src 192.168.1.10 `, without `proto dhcp` nor metric; the address is `dynamic`, with `valid_lft` and `preferred_lft` equal to the time left on the lease. Read in the sources, not run: Debian's `dhclient-script.linux` adds the default route without `proto` (so `boot`), and iproute2 hides `proto boot` without `-d` (`ip/iproute.c`); it prints `dynamic` for an address that is not permanent (`ip/ipaddress.c`) | The format is that of the real reference output, and lot 1c-2 brings `dhclient` itself. A Docker run (dnsmasq and `dhclient`) will confirm it at lot 1c-2 |
| Reply of the box to a `ping` outside the local network | 1c-1 | `Destination Net Unreachable` (ICMP type 3, code 0) sent by the box, whose routing table only holds its local network until lot 6c; no rate limit on ICMP errors. RFC 1812, 4.3.3.1: a router with "no routes at all (including no default route)" to the destination "MUST generate a Destination Unreachable, Code 0 (Network Unreachable) ICMP message"; Linux does the same when it forwards (`net/ipv4/route.c`, `ip_error`). What a real consumer box does with its line down was not found | The answer comes from the real state of the box, and changes by itself when the box gets a route (lot 6c) |
| Laptop with its lid closed | 1c-1 | It stays on the network (keeps its address, answers `ping`) until lot 1e; at lot 1e, closing the lid could put it to sleep, as most real laptops do by default (from memory, to verify) | Lot 1e brings power states |
| "Computer busy" | 1c-1 | Answer to a gesture, through `MessageCenter.gesture`, as an error (`docs/messages.md`, "Terminal") | Same channel as the other gestures |
| Presence frame removed | 1c-1 | Accepted: without it, a computer that sends nothing leaves the box's table after 300 s and comes back at its next frame (at the latest its DHCP renewal, at 600 s); the panel of the box stops showing its MAC for that time, while the addresses lent stay shown | This is what a real switch does |
| Working name of the operating system | (announcement) | "Blocknix" is a working name, in the documentation only: generic names in the code, nothing in the game or its resources. Before any announcement: trademark databases (classes 9 and 42: INPI, EUIPO, WIPO, USPTO), mod platforms (CurseForge, Modrinth), GitHub and the web, checked the same day, and availability of the useful names | The name can change without touching the architecture |
| Choice of the exact port | 1d-1 | Decided by Max on 2026-11-20 (`docs/cable-design.md`, section 3): the player aims at the port. With a cord in hand, the aimed port is outlined and the status panel shows "Port 3: free" before the click. The face that carries the ports is split into one aim zone per port, up to halfway to its neighbours; on a flat device the zone goes on over the top face, where the port LEDs are. Anywhere else on the device, the first free port (first occupied port to unplug or take an end off). All the gestures follow the aimed port | Natural gesture, the same two clicks; the zones keep the 0.5 px ports of a switch easy to hit. Menu and scroll wheel set aside (a menu may come back for the dense switches of 8b) |
| Cable attached to its port | 1d-1 | Decided by Max on 2026-11-20 (`docs/cable-design.md`, section 4): the cable leaves from the mouth of its port, given by a port layout per device, with an RJ45 plug pushed into it; plugs drawn by the client from the cables it knows, not from block states; cable 0.25 px thick instead of a line. Check in the 26.3 sources how to draw a small model in the cable pass before coding | One source of truth (the cables known to the client); `ATTACH_HEIGHT` disappears |
| Tower and laptop before lot 1d-1 | 1d-1 | Decided by Max on 2026-11-20: the tower and the laptop are redrawn on the design guide, with the position of their ports, before lot 1d-1 (issue #56) | Step 1 needs the position of every port; the laptop has none in its current model |
| Cable on the ground: drawn, not placed in the world | 1d-2 | Decided by Max on 2026-11-20 (`docs/cable-design.md`, section 5): each client computes the route from the terrain and recomputes it when a block changes on it; nothing in the world, nothing saved, no cost on the server; the player walks over it. Wall sockets, clips, trunking and trays (2b, 8a) are real blocks that become fixed points of the route | A single copy of the network (the engine), the server decides and the client displays |
| Cord laid on the ground, separated by a wall | 1d-2 | Decided by Max on 2026-11-20: the route goes around the obstacle in a corridor around the straight line (through a door), climbs up to one block; with no route on the ground, the cable hangs in the air as today, until the wall sockets of 2b | The clean answer to a wall is a wall socket |
| Placement of the cable steps | 1d-1, 1d-2, 2b, 8a | Decided by Max on 2026-11-20 (`docs/cable-design.md`, section 1), each step playable alone: 1. port chosen by aiming and cable attached to its port with its plug, lot 1d-1 (lot 1d renamed 1d-1); 2. thick cable laid on the ground, new lot 1d-2, in version 0.2.0 so as not to delay 0.1.0; 3. RJ45 wall sockets and cable fixed to the wall, 2b; 4. trunking in 2b, cable trays in 8a | Cables improve step by step without delaying 0.1.0 |
| Visual design of the cables | 1d-1, 1d-2 | Decided by Max on 2026-11-20 from the previews of `design/tools/cables.py` (`docs/cable-design.md`): one RJ45 size on every device, 2 x 2 texels (the box and the tower go from 3 x 3 to 2 x 2); the plug with its clear body and a long boot in the colour of the cord; a two-tone outline of the aimed port (light inside, dark outside), not a status colour; eight cord colours, a family of their own (`design/palette.json`, key `cords`); the cord icon with a taller plug; the plug standing out behind the tower and the laptop accepted | A real RJ45 has the same size everywhere; the outline shows on light and dark devices without contradicting the lights of the status panel |

History of the decision on issue #10: during the interview, Max first answered that the tower would have no terminal for now, that its terminal would need a screen, keyboard and mouse plugged in, and that the tower would later be crafted from components. On review of the summary, he confirmed the principle written above (terminal on the tower from lot 1c-1, peripherals optional by default). The tower crafted from components stays an idea of its own (idea issue "PC tower built from components"), independent of this decision.

The real dimensions of each piece of equipment are in `docs/network-equipment.md`.

This document describes the design. The tasks of each lot are GitHub issues, one milestone per lot; the roadmap of versions (0.1.0 for lot 1, 0.2.0 for electricity and lot 2, etc.) and the organisation are in `docs/project-management.md`.

## Overview

| Lot | Theme | What works at the end |
|---|---|---|
| 0 | Computers | Two computers can be placed (done, laptop to test in game) |
| 1a | Cable and connection | Two computers linked by a cable show "connected" (done) |
| 1b | The box assigns addresses | Each computer connected to the box receives an address |
| 1c-1 | Terminal and ping command | From a computer, `ping` to another computer, which answers |
| 1c-2 | Network diagnostics | `tcpdump` shows the ARP and ICMP frames of a `ping` |
| 1d-1 | Desktop switch | More computers than the box has ports; the player chooses the port by aiming at it, the cable leaves from the port with its plug |
| 1d-2 | Cable on the ground | Cables come down from the ports, lie on the floor and go around walls; the player walks over them (version 0.2.0) |
| 1e | Electricity | A device without power is off |
| 1f | TCP | A connection opens between two computers, carries a message and closes |
| 2a | Wi-Fi | The laptop connects without a cable |
| 2b | Fixed home cabling | Cables go through wall sockets and trunking |
| 2c | Peripherals and local services | A shared file and a network print |
| 2d | Other devices in the home | Phone, camera and television on the network |
| 3a | Fibre arrival | The box is connected to an optical socket |
| 3b | Copper arrival | The box is connected to a telephone socket |
| 4a | The street, overhead | A line strung from pole to pole up to the house |
| 4b | The street, underground | The same line through chambers and ducts |
| 5a | The neighbourhood in fibre | Several houses start from the same street cabinet |
| 5b | The neighbourhood in copper | Same for copper lines |
| 6a | The rack | Equipment is mounted in a rack, at a precise position |
| 6b | The ISP site | The neighbourhood lines arrive on the operator's equipment |
| 6c-1 | Public address | The box receives a public address from the aggregation router |
| 6c-2 | Address translation | Two subscribers reach each other; a private address cannot be reached directly |
| 7a | Routing between operators | Two subscribers of different ISPs reach each other |
| 7b | Servers and domain names | A name resolves, a server answers |
| 7c | The mesh of the internet | One link cut, the traffic takes another path |
| 8a | Enterprise racks and patching | A floor cabled cleanly up to a rack |
| 8b | Configurable switch | Two separate networks on the same switch |
| 8c | Enterprise router and firewall | One flow allowed, one flow blocked |
| 8d | Servers and storage | A service hosted on a rack server |
| 8e | Room power | A power cut without stopping the servers |
| 8f | Cooling | A room that heats up if the air conditioning stops |
| 8g | System and accounts | A session is opened with an account from the directory |
| 9a | Cabling and test tools | A cable is made and tested |
| 9b | Mobile network | A device connects through an antenna |
| 9c | Satellite and microwave link | An isolated site connected without a cable |
| 9d | Survival mode | All equipment can be crafted |
| F | Finishing | Lights, lit textures, sounds, polished models |

## Lot 0: computers

| Type | Equipment | Role | Status |
|---|---|---|---|
| Block | PC Tower | Fixed workstation, one network port | Done |
| Block | Laptop | Mobile computer, one network port, screen that opens and closes | Written, to test in game |

## Lot 1: wired local network

This is the founding lot: it builds the simulation engine on which all the others rely (ports, links, addresses, message exchange).

### 1a: cable and connection

| Type | Equipment | Role |
|---|---|---|
| Item | Ethernet Cable | Links two network ports: one click on a device, one click on the other |
| Mechanic | Network port | Each device has a fixed number of ports; a port takes only one cable |
| Mechanic | Status indicator | When aiming at a device, you read for each port: free or connected |

Playable at the end: two computers linked directly by a cord show "connected", and "free" as soon as the cord is removed.

Proposed choices:

- The cord is a visible stretched link between two devices, not a series of blocks. Clean cable routing comes at lot 2b.
- Maximum length of 100 blocks, like the 100 m of a real copper Ethernet link.

Technical constraint to keep from this sub-lot on: a link must keep existing when the area of the world where it lies is not loaded. Lots 4 to 7 connect distant sites, and the network must not go down when the player walks away.

#### Status of lot 1a

Coded on 2026-11-17 on the `developement` branch. Build and 34 unit tests green, status panel included. In-game trials of the same day: connection and visible cable judged good by Max; according to the log, the link placed before the restart is still listed by `/worldwideblocks links` after the game is relaunched, and still listed from x ≈ 380, an unloaded area. Status panel and cord consumption in survival approved by Max. Added afterwards at his request and approved in game: removing a single end of the cable with an empty hand, then unplugging it from the other device to get the cord back (steps 5 and 10). Lot 1a finished.

| Type | Equipment | Status |
|---|---|---|
| Item | Ethernet Cable | Done, provisional texture |
| Mechanic | Network port | Done: 1 port for the tower and for the laptop |
| Mechanic | Status indicator | Done: panel at the top centre, one light per port |
| Rendering | Visible cable | Done: slightly sagging blue line, provisional |

Chosen behaviour:

- Right-click with the cord on a device, then on another: links the first free port of each. The cord glows while the connection is pending. At the second click, the server checks again that the first end still exists, is in the same dimension and has its port free.
- Sneak + right-click with an empty hand on a connected device: removes the cable end from that device and keeps it in hand, the other end stays connected (a line links the device to the hand). Right-click on another device: the cable is plugged in there, without consuming a new cord. Right-click on the device where the other end is connected, or sneak + click in the air: the cable is unplugged from the other end too and the cord stays free in hand.
- Sneak + right-click on a device, cord in hand: unplugs both ends and stores the cord. Sneak + right-click in the air: cancels a connection that was started.
- Refused: more than 100 blocks in a straight line between the centres of the two blocks, two different dimensions, the same device at both ends, a port already in use.
- Survival: the cord is consumed on connection. It is given back (on unplugging, when removing an end, or when dropped on breaking a device) only if the link had consumed one; a link placed in creative gives nothing back in survival.
- Devices refuse pistons: the engine identifies them by their position.

Technical base for the next lots:

- The engine (`io.github.maximcuynat.worldwideblocks.network`) is a graph of devices and cables in plain Java, tested without launching the game. At lot 1a, it carries no message; since lot 1b, real frames travel through it (see the principles).
- It is saved in the server's global storage (`data/worldwideblocks/network.dat` of the world), independent of loaded chunks. No network data is in static memory: in singleplayer, two worlds do not mix.
- Each device known to the engine keeps its number of ports. This is where the hardware address of the ports (1b) and the state of each device will be added.
- Each link keeps its medium (`copper_ethernet`, 100 blocks at most): fibre and the telephone line will be added to it.
- All the logic runs on the server. A player only receives the cables with one end in a chunk their client has received, so as not to reveal the location of devices out of sight.
- Command `/worldwideblocks links` (operators): lists the links as the server knows them, even when their area is not loaded.

Known limits, to take up later:

- The port is chosen automatically (first free port). The target port will have to be chosen when devices have several (box at lot 1b).
- A device removed without the game reporting it (for example a `/setblock` without an update) leaves an orphan link. It is cleaned at the next placement or the next click on that position.
- The status panel takes the same place as the boss bars.
- The distance is shown with a decimal point whatever the language.

In-game test procedure:

1. `./gradlew runClient`, a creative world. The cord is in the World Wide Blocks tab.
2. Place a PC tower and a laptop about twenty blocks apart. When aiming at each, the panel at the top centre shows "Port 1: free" with a grey light.
3. Cord in hand, right-click on the tower (message "PC Tower, port 1: right-click the second device.", the cord glows), then right-click on the laptop (message "Connected: …", the laptop screen does not toggle). A blue cable links the two devices; when aiming at each, the panel shows "Port 1: connected" with a green light. `/worldwideblocks links` lists the link.
4. Sneak + right-click on one of the two with the cord: message "port 1 unplugged", the cable disappears, both go back to "free". Reconnect.
5. Sneak + right-click with an empty hand on the laptop: the end is removed and stays in hand, a line links the tower to the hand. Right-click on a third device: the cable links the tower to this new device. Do it again, then right-click on the tower: the cable is unplugged from it, the cord stays free in hand and the tower goes back to "free".
6. Walk away: `/tp ~400 ~ ~`. Type `/worldwideblocks links`: the link is still listed although its area is no longer loaded. Come back: `/tp ~-400 ~ ~`. The cable is visible again and both devices show "connected".
7. Save and restart: quit to the menu, close the game, relaunch `./gradlew runClient`, reopen the world. The cable is visible, both devices show "connected" and `/worldwideblocks links` lists the link.
8. Refusals: connect a device more than 100 blocks away ("Too far: …"), then a device whose port is in use ("no free port").
9. Breaking: break the connected tower. The cable disappears and the laptop goes back to "free".
10. Survival (`/gamemode survival`, with a single cord in the inventory): connecting consumes the cord. Empty hand, sneak + right-click on a device: the end comes back in hand; right-click on the other device: the cord is free in hand, ready to be used again. Breaking a device linked in creative drops no cord.

### 1b: the box assigns an address to each computer

| Type | Equipment | Role |
|---|---|---|
| Block | Internet Box | Heart of the home network: 4 local network ports, links the devices together and hands out their addresses |
| Mechanic | Hardware address | Each network port has a unique identifier (MAC address) |
| Mechanic | Switching | The box passes messages from one port to another, remembering which device is behind which port; reused as is by the switches |
| Mechanic | Address assignment | A connected computer asks for an address, the box lends it one (DHCP) with the mask, the gateway and the name server |

Playable at the end: each computer connected to the box shows its address in the status indicator; unplugged, it loses it.

The box also has a line input port, inactive until lot 3.

The lot is split in two sub-lots (architecture approved by Max on 2026-11-17):

- **1b-1: box, MAC addresses and switching.** Playable: two computers connected to the box, the box shows which MAC is behind which port.
- **1b-2: DHCP.** Playable: each computer obtains its address through a real DHCP exchange and shows it with the state of its lease.

Chosen architecture:

- Layered frames (`network/protocol/`): an Ethernet frame carries a source MAC, a destination MAC (or broadcast), a type and a typed content. IPv4 (0x0800), then UDP and DHCP, ARP (official type 0x0806) and ICMP are each added as a variant of the layer below.
- One MAC per port, first byte `02` (locally administered address), unique in the world and saved; a device that is broken then placed again gets new ones. Each port has a type: Ethernet or line input. The Ethernet cord takes the first free local network port; how to choose the exact port is an open decision, to take before lot 1d-1 (see "Open decisions").
- Simulation (`network/sim/`): delivery queue processed at each server tick, timers, a tap on each incoming or outgoing frame (for a future analyzer), clock on game time. A change that happens between two ticks (connection, unplugging, device placed or removed) is applied at the start of the next tick, with the time of that tick: its frames, timers and reports start from the tick where its frames are delivered (fixed on 2026-11-18, see the in-game test of the loop). It runs even when chunks are not loaded.
- Switching (`network/switching/`): learning of MACs on received frames, broadcast if the destination is unknown or addressed to all, entries expire after 300 game seconds (value recommended by IEEE 802.1D), forgotten at the next tick when the cable is removed. The box is this switch plus a local point, on which its own service will be plugged (DHCP at 1b-2); the desktop switch of lot 1d-1 will be the same switch, without a local point.
- Loop protection, until the STP of lot 8b: a broadcast frame that comes back to a switch it has already crossed is dropped and reported (log, and a message to players within 32 blocks, at most one per minute and per device). Safety net: 10,000 frames processed per tick at most, 50,000 in the queue.
- Presence frame: scaffolding of lot 1b-1, **removed at lot 1c-1** (issue #15) once DHCP and ARP produced real traffic. A computer broadcast a frame of type 0x88B5 (IEEE experimental type) when its cable was connected, then every 30 s, so that the box learned its MAC before DHCP existed. Since then a computer sends only real traffic, and the box learns its MAC from its DHCP exchange.
- 1b-2: DHCP client of the computer following RFC 2131 (DISCOVER, OFFER, REQUEST, ACK with a transaction identifier; renewal at 50% of the lease, then at 87.5% by broadcast). A computer that had an address before a server restart directly asks for its old address again (INIT-REBOOT state): its lease is therefore saved. Range 192.168.1.10 to 192.168.1.200 in /24, box at 192.168.1.1 (gateway and name server), lease of one game day (24,000 ticks), the box remembers the address lent to each MAC. Two DHCP servers on the same network: the indicator reports the conflict.

#### Status of lot 1b-1

Coded from 2026-11-17 to 2026-11-18 on the `developement` branch, approved by Max on 2026-11-18. Unit tests and automatic game tests green, check by eye done (status panel, drawn cables, messages).

| Type | Equipment | Status |
|---|---|---|
| Block | Internet Box | Done: 4 local network ports and 1 inactive line input, 7 x 7 x 3 px model, provisional texture |
| Mechanic | Hardware address | Done: one MAC per port, saved, compatible with the worlds of lot 1a |
| Mechanic | Switching | Done: learning, broadcast, expiry at 300 s, forgotten when the cable is removed, loop protection |
| Mechanic | Status indicator | Done: MAC of the computer; for the box, the MAC learned behind each port |

Known limits, to take up later:

- The cable starts from the centre of the top of the device, not from the port where it is connected.
- Messages (log in two languages, two messages for one loop) and the temporary trial log lines: taken up by the message module, below.

#### Message module (before 1b-2)

Audit, design and rules in `docs/messages.md` (approved by Max on 2026-11-18). Coded on 2026-11-18 in 6 steps, one commit each after a green build (`./gradlew build test`: 123 unit tests; `./gradlew runGameTestServer`: 17 game tests). **Check by eye done by Max on 2026-11-18** (game in French, "Trials messages" world in superflat, no crash); rendering approved as the base of the future HUD interface (lot F).

Result of the check: the "Seen" column is proved by the screenshots (`run/screenshots`) and the log; the last column was seen by eye by Max during the same session, without a screenshot, and approved on his word on 2026-11-18. All points are good.

| Point | Seen (screenshot or log) | Approved on Max's word |
|---|---|---|
| 1. Tooltip and action bar | Tooltip: 3 grey lines (the second wraps in an 854 x 480 window); "✖ Internet Box: no free port." in red | |
| 2. Box panel | Compact "3/4 ports connected" and "Sneak: details"; detailed: ports 1 to 3 with the full MAC, port 4 free, port 5 (line) inactive; computers compact "Connected" | Detailed panel of a computer (port and MAC) |
| 3. 2 MACs on one port ("chain") | Left box: "Port 1: connected, 02:… (+1)" | Right box detailed |
| 4. Laptop at the end of 99 blocks | Laptop "Connected", cable reaching the laptop | Cable over its whole length, detailed panel of the laptop |
| 5. Loop message | A single yellow message "⚠ Network loop near Internet Box [x, y, z]…", again a single one later; one "Network loop" line in English per message | One game minute apart (game paused between the two, proved by the game test); messages stop after a cable is removed (no cable removed) |
| 6. French texts | All the texts seen are in French and use formal address; symbols ✖ and ⚠ rendered well | `/worldwideblocks links` (not run); rendering of ✔ and ℹ; fallback `messageSymbols = false` (not tried) |

| Subject | Status |
|---|---|
| Channels | Done: the channel follows from the use and the level (gesture: action bar; event: chat; detail: DEBUG log) |
| Levels | Done: information, success, warning, error; symbol (ℹ ✔ ⚠ ✖) or word (client setting `messageSymbols`), constant colour |
| Throttling | Done: a problem once per minute; a loop seen by two boxes gives a single message; the limit of a network is forgotten when that network changes |
| Texts | Done: keys and values only, the French texts use formal address, whole-number distances, lengths checked by a test, cord tooltip |
| Log | Done: entirely in English; temporary trial log lines removed, the useful ones moved to `Diagnostics` (DEBUG) |
| Status panel | Done: compact by default, detailed when sneaking; content in plain Java (`StatusPanel`), the client only draws |
| Saturated network | Done: message to operators and to the owner in singleplayer; recipients without an automatic test (the test server would have to be saturated) |

Data separated from drawing: the server sends key, values and level; the client alone decides the look. The future HUD interface (lot F) will replace `client/ClientMessages` and `client/PortStatusLayer` without touching the rest.

Test method (since lot 1b-1): the logic is proved by automatic tests; manual trials are only used for what needs eyes. Max's save is never modified.

1. `./gradlew build test`: compilation (`gametest` source set included) and unit tests of the engine, in plain Java.
2. `./gradlew runGameTestServer`: automatic game tests, without a window, in a throwaway world (`run-gametest/`, ignored by git). The result is at the end of `run-gametest/logs/latest.log` ("All N required tests passed"). They cover: cord whose start is broken or destroyed by an explosion (creative and survival, cord count), dimension change and disconnection during a pending connection, full ports, MAC expiry (exact bounds), saving and reloading the network, loop reported by a single message per minute for two boxes (exact gap of 1,200 ticks) and limit forgotten only for the network that changes (separate lot, because they touch the global limits of the engine), construction of each trial scene, refusal of `/worldwideblocks test` outside a trial world. Messages are checked through the module's listener (channel, key, values, recipient), without going through a player's chat.
3. Check by eye: a capture run per scene (`docs/automated-checks.md`), `./gradlew runClientCapture -Pscenario=<name>`. The run creates its own trial world in `run-capture/`, builds the scene, aims at each device standing then sneaking, takes the screenshots in English and in French, at 854 x 480 and 1920 x 1080, writes what it read next to them and quits; the assistant reads `run-capture/screenshots/<name>/` and `values.txt`. The scenarios are in `src/capture` (`CaptureScenarios`).

| Scenario | What it shows |
|---|---|
| `box` | Compact panel: "0/4 ports connected"; sneaking: ports 1 to 4 free, port 5 (line) inactive; front facing the player |
| `box_hosts` | Box compact "3/4 ports connected", sneaking: 3 ports connected with the learned MAC, port 4 free; computer compact, then sneaking: its port and its MAC; `/worldwideblocks links` in the chat |
| `chain` | Sneaking, port 1 of the left box: one MAC followed by "(+1)"; the right box, one MAC per port and its DHCP server off; no conflict message |
| `loop` | A single loop message in the chat, again a single one one game minute later; one cable between the boxes removed, then no message for a minute and a half (the run fails if one comes) |
| `full_ports` | Cord in hand, right-click on the box: "✖ Internet Box: no free port." in red above the hotbar, then "Error: …" with `messageSymbols = false` |
| `distance` | The 99-block cable seen from above each end, both ends with their panel |
| `tooltip` | The tooltip of the cord in the inventory |

`/worldwideblocks test <scene>` (short alias `/wwb`) stays for trying a scene by hand in `./gradlew runClient`, in a creative world whose name starts with "Trials" (refused elsewhere; development only, operators only).

Since issue #15 (lot 1c-1), without the presence frame: the MACs learned by a box (`box_hosts`, `chain`) are shown during the first 300 game seconds after the scene is built, then again for 300 s after each DHCP renewal of the computer (every 600 s); the capture runs take their shots within the first minute. The addresses lent by the box stay shown.

#### Check by eye of the message module

Capture runs, in English and in French (`./gradlew runClientCapture -Pscenario=<name>`):

| Point | Scenario |
|---|---|
| 1. Tooltip and action bar | `tooltip` (3 grey lines), `full_ports` ("✖ Internet Box: no free port." in red) |
| 2. Compact then detailed panel | `box_hosts` |
| 3. 2 MACs on port 1 of the left box | `chain` |
| 4. Laptop at the end of the 99-block cable | `distance` |
| 5. Loop message, single and readable, stopped by removing a cable | `loop` |
| 6. French texts, `/worldwideblocks links`, symbol fallback | All of them in French; `box_hosts` (links); `full_ports` (`messageSymbols = false`, put back by the run) |

To report: any text cut off, overflowing the panel, badly translated, or whose symbol is displayed badly.

#### Status of lot 1b-2

Coded on 2026-11-18 on the `lot/1b-2` branch (issues #3 to #9), in 7 steps, one commit each after a green build (`./gradlew build test` and `./gradlew runGameTestServer`: 30 game tests). Protocol checked in the text of RFC 2131 and RFC 2132. Check by eye done by Max on 2026-11-18, below (points 9 and 10 rest on automatic tests).

| Type | Equipment | Status |
|---|---|---|
| Mechanic | Protocol layers | Done: IPv4 packet in the Ethernet frame (0x0800), UDP datagram (protocol 17), DHCP message (ports 67 and 68), each a variant of the layer below |
| Mechanic | DHCP server of the box | Done: on the local point of the box's switch, with its own saved MAC; pool 192.168.1.10 to .200 in /24, gateway and name server 192.168.1.1, lease of one game day; same address for the same MAC; expired leases reused, the oldest first, when the pool is full |
| Mechanic | DHCP client of the computers | Done: DISCOVER, OFFER, REQUEST, ACK and NAK with a transaction id; renewal at T1 to the server alone, at T2 by broadcast; address lost at the end of the lease or when the cable is unplugged |
| Mechanic | Restart | Done: leases of the box and of the computers saved in `network.dat` (optional fields, lot 1b-1 worlds still load); a computer still plugged in asks for its address again (INIT-REBOOT) |
| Mechanic | Two DHCP servers | Done: detected by the computer (two answers from different MACs, both boxes being 192.168.1.1); chat warning to the players within 32 blocks, once per minute per network |
| Setting | "DHCP server enabled" | Done, saved, on by default, without interface: used by the `chain`, `loop` and `dhcp_server_off` scenes and by the game tests |
| Mechanic | Status indicator | Done: address and DHCP state of a computer, details when sneaking (MAC, mask, gateway, name server, time left on the lease), addresses lent by a box (four lines at most) |

Deviations from RFC 2131, approved by Max on 2026-11-18:

- No random +/-1 s on retransmissions (4.1), for exact timings: DISCOVER after 4, 8, 16, 32 and 64 s, then every 64 s; REQUEST after 4, 8 and 16 s, then back to INIT 60 s after the first one (3.1).
- INIT-REBOOT without an answer goes back to INIT (4.4.2 also allows keeping the address).
- The renewal is unicast to the MAC of the server taken from its acknowledgement: no ARP before lot 1c. Since lot 1c-1 (issue #13), the computer sends it through its IP stack, which finds the server's MAC by ARP, as `dhclient` does through the kernel.
- No DECLINE, RELEASE or ARP probe of the offered address (4.4.1).
- Lease time in seconds in the message (1,200 s), 24,000 ticks in the engine; an offered address is held 60 s.

Presence frame: removed at lot 1c-1 (issue #15), as accepted by Max at the start of that lot. A computer with an address is silent until T1 (600 s), while a switch forgets a MAC after 300 s: the MACs learned by a box are shown during 300 s after each frame of the computer.

Automatic tests: unit tests of the layers, of the server, of the client at the exact tick (renewal, rebinding, expiry, retransmissions, INIT-REBOOT, conflict), of the simulation with boxes and computers, of the panel and of the language files. Game tests `dhcp_*`: address obtained, lost when unplugged, same address when plugged again, renewal at T1, expiry at the exact tick (server turned off), computers without a box, conflict of two boxes, restart through `network.dat` read back (INIT-REBOOT), full pool; plus the build of each new scene.

#### Check by eye of lot 1b-2 (game in French)

**Done by Max on 2026-11-18** (game in French, "Trials DHCP" world, screenshots `run/screenshots` from 17.09.29 to 17.16.23, log `run/logs/latest.log`), analysed by Claude. No text cut off, overflowing or badly translated; the waiting light is the expected yellow-orange. Points 9 and 10 were not seen by eye: they rest on the automatic tests named below.

| Point | Result | Proof |
|---|---|---|
| 1. Address of the computers | Passed | 17.09.29 compact "192.168.1.10 (DHCP : obtenue)", green light; 17.09.40 detailed (MAC, address and mask, gateway, DNS server, "Bail : encore 19:14", state); 17.09.44 box "Adresses prêtées : 3" and three lines |
| 2. Unplugged, plugged again | Passed | Log 17:10:01 cable disconnected, end in hand; 17.10.06 "Non connecté"; log 17:10:14 cable connected; 17.10.23 same address 192.168.1.10 |
| 3. Renewal | Passed | 17.10.23 "19:52"; `/tick sprint 600s` from 17:10:39 to 17:10:57; 17.11.07 "19:25", state "obtenue" (without renewal, about 9:25) |
| 4. Chain, one server | Passed for what is shown | No conflict message (17.16.23, and the only conflict line of the log belongs to `dhcp_conflict`). Not captured: the addresses of the computers and the right box's "Serveur DHCP : arrêté", covered by the unit test `DhcpSimulationTest.aComputerPluggedOnASecondBoxWithItsServerDisabledGetsItsAddressFromTheFirst` (no game test) and the panel test `StatusPanelTest.aBoxWhoseServerIsOffSaysSo`; the panel line itself is seen at point 8 |
| 5. No box | Passed for what is shown | 17.12.10 and 17.12.11: tower "Pas d'adresse (DHCP : recherche)", yellow-orange light. Not captured: the laptop, covered by the game test `dhcp_no_box` |
| 6. Conflict | Passed for what is shown | 17.12.30 and log line 294: a single message "⚠ Deux serveurs DHCP sur le réseau de Box internet [0, -60, -2]. N'en gardez qu'un."; log line 293 names both boxes. Not captured: the address of the computer, covered by the game test `dhcp_conflict_of_two_boxes` |
| 7. Full pool | Passed | 17.15.06, 17.15.07 computer searching; 17.15.15 "Adresses prêtées : 191", four lines, "+187 autres" |
| 8. Server off | Passed | 17.15.56 "Serveur DHCP : arrêté"; 17.16.01, 17.16.02 computer without address |
| 9. Restart | Not seen by eye | Game test `dhcp_restart_asks_for_the_same_address` (network.dat read back, INIT-REBOOT, same address); the client display after a real quit and reopen is not proved |
| 10. Loop | Not seen by eye | Game tests `loop_reported_once_per_minute` and `loop_forgotten_only_for_changed_network` (one loop message per minute); "no conflict message" in the `loop` scene is proved only at engine level, by the unit tests `NetworkSimulationTest.twoBoxesInALoop…` and `twoLoopsInTwoNetworks…` (second server off: a single loop event, no conflict) |

Capture runs for the next checks, in English and in French (`./gradlew runClientCapture -Pscenario=<name>`):

| Point | Scenario | What it shows |
|---|---|---|
| 1. Address of the computers | `dhcp_addresses` | A computer compact ("192.168.1.1x (DHCP: bound)", green light) then detailed (MAC, address and mask, gateway, DNS server, time left on the lease, state); the box detailed, with the addresses it lent |
| 2. Unplugged, plugged again | `dhcp_addresses` | Empty hand, sneak and right-click on the computer: "Not connected"; right-click with the end of its cable: the same address again |
| 3. Renewal | `dhcp_renewal` | Time left about 20:00, then 600 s of game at once (`/tick sprint`): about 20:00 again, not 10:00 |
| 4. Chain, one server | `chain` | The left box lends 3 addresses, the right box "DHCP server: off", its computers with an address, no conflict message |
| 5. No box | `dhcp_no_box` | Both computers "No address (DHCP: selecting)", yellow light |
| 6. Conflict | `dhcp_conflict` | A single "⚠ Two DHCP servers on the network of …" in the chat; the computer keeps an address; the English log line in `run-capture/logs/latest.log` names both boxes |
| 7. Full pool | `dhcp_pool_full` | The computer without address; the box "Addresses lent: 191", four lines, "+187 more" |
| 8. Server off | `dhcp_server_off` | The box "DHCP server: off"; the computer without address |
| 9. Restart | `dhcp_restart` | Each computer's address, then save and quit to the title screen, open the world again: the same addresses, state "bound" |
| 10. Loop | `loop` | One loop message per minute, no conflict message, none once a cable of the loop is removed |

To report: any text cut off, overflowing the panel, badly translated, or a time left that does not count down.

### 1c-1: terminal and ping command

| Type | Equipment | Role |
|---|---|---|
| Screen | Computer terminal | A right-click on the tower, or on the keyboard part of the open laptop, shows the console of the machine |
| Command | `ping [-c n] <address>` | Sends real ICMP echo requests and shows the replies, or the real reason why none comes |
| Command | `ip addr`, `ip link`, `ip route`, `ip neigh` | Show the interfaces, the state of the link, the routes and the ARP cache that really exist on the machine |
| Command | `hostname`, `help`, `clear`, `history`, `exit` | Base of the shell; `help` is translated |
| Mechanic | Address resolution | Before sending, the computer asks on the network which hardware address matches the target address (ARP) |
| Mechanic | Network latency | Computed from the path actually taken by the frames |

Playable at the end: from a computer (tower or laptop), `ping` to another computer, and it answers; to an absent address, the real error. This is the success criterion of lot 1.

Decisions of 2026-11-19: see "Decisions taken". The terminal is an interface to the simulated system, never a set of prepared results: each command reads or changes the real state of the machine and of the network.

**Gestures.**

- Laptop: a right-click on the screen part of the model opens or closes the lid; a right-click on the keyboard part of the open laptop opens the terminal. The item keeps its open or closed state in the inventory and is placed in that state. Sneak + right-click with an empty hand stays the unplugging of a cable end (decided at lot 1a, on 2026-11-17), and a right-click with a cord in hand stays the connection of a cable.
- Tower: a right-click opens the terminal, as a chest opens: with any item in hand except the cord, which connects a cable; sneaking to place a block against it or to unplug a cable end (decided by Max on 2026-11-20, what Minecraft players know). No screen, keyboard or mouse is needed (issue #10).
- A laptop with its lid closed stays on the network (keeps its address, answers `ping`) until lot 1e (see "Decisions taken").

**Machine, console and session.**

- The computer is an object of the server: its interfaces, its host name, its account, its processes. It runs whether a player is in front of it or not.
- A console is a view: a grid of characters on the client and a stream of keys, linked on the server side to a session that runs a command interpreter. The consoles planned later reuse it with another interpreter or another way in: the router console (7a), the switch console and its console cable (8b), the screen and keyboard console of the rack (8d), SSH sessions. The design allows several sessions per machine; lot 1c-1 opens only one, the local console.
- One player at the keyboard at a time; another player who tries gets "computer busy" (translated message). The keyboard is freed when the player walks away beyond the distance of use, disconnects or changes dimension, as with the containers of the game.
- The local console lives in server memory while the machine exists. Esc hides the screen: the session and its running commands go on, and the player finds the screen as it was. `exit` (or Ctrl+D on an empty line) ends the shell session, not the other processes of the machine; the next opening starts a new shell on a clear screen. A server restart loses the session, like the memory of a real computer that restarts; the saved disk comes with the operating system lots.
- First packet from client to server of the mod: size of a line, rate and session (player, distance) are checked again by the server on each packet.
- A command is a process advanced by the server tick, never a blocking call: it reacts to keys, received frames and timers. An error in a command ends its process, never the tick. Each command is a class registered in a registry: a later lot adds commands without changing the terminal. A cap limits the running processes per machine.

**Appearance.**

Chosen by Max on 2026-11-19 from the captures of the prototype (issue #30, findings in the issue).

- Black opaque screen inside the dark popup panel of the game (sprite `popup/background`, nine-slice), as if the text were shown on the screen of the computer. Title bar in white: name of the machine, how to close, and "Ctrl+C: stop" while a command runs; the title stays inside the border of the panel, with room above it for the accents of the capitals (É), checked at each shot of the capture runs.
- Font Monocraft (see "Decisions taken"). The 16 text colours of Minecraft as the ANSI palette (0 black, 1 dark_red, 2 dark_green, 3 gold, 4 dark_blue, 5 dark_purple, 6 dark_aqua, 7 gray, 8 dark_gray, 9 red, 10 green, 11 yellow, 12 blue, 13 light_purple, 14 aqua, 15 white); grey text (AAAAAA) on black by default, with the game's text shadow. Never the dark blue 0000AA for an important text: it reads badly on black. Blinking block cursor.
- Prompt `user@laptop-3f2a:~$ `, with `user@laptop-3f2a` in green 55FF55 and `~` in blue 5555FF: the `PS1` of a Debian 13 account (`/etc/skel/.bashrc`) writes them in bold green `01;32` and bold blue `01;34`, seen on 2026-11-20 (the earlier comparison with Ubuntu was from memory). The account `user` and the host name are data of the machine, never text fixed in the screen: the host name is the type of the machine and the last four hex digits of the MAC of its first port: a laptop is named `laptop-3f2a`, a tower `tower-…`; it will change through `hostname` and `/etc/hostname` when the operating system has files.
- Rows of 12 px: with 10 px, the accents of capitals (`É È Ê Ô`) reach into the row above; with 11 px, they touch it.
- The terminal has its own text size, independent of the GUI scale of the game: by default the largest that gives at least 80 columns, changed in a client option and in the terminal with Ctrl+= and Ctrl+- (`=` and `-` move between keyboard layouts: read them from the symbol of the layout, as Ctrl+C). At the GUI scale of the game, 80 columns are not reached at 854 x 480 (63 columns in the frame of the prototype) nor at 1920 x 1080 in automatic scale (70 columns in the chosen frame), and the `lo` line of `ip addr` is 91 characters long.
- Number of columns and rows from the window size and the text size, never a fixed 80 x 25, recomputed on every resize; long lines wrapped at the column by the client, mid-word, as by a real terminal emulator.
- No scissor in the screen: `GuiGraphicsExtractor.enableScissor` crashed the prototype when the window shrank (`GuiRenderer.enableScissor` throws on a height of 0); text is cut by width instead.
- Scrollback with the mouse wheel and Shift+Page Up / Page Down (Shift+Fn+Up / Down on a MacBook), with a cap on kept lines; command history with the Up and Down arrows.
- The screen does not pause the game in single player (`Screen.isPauseScreen` returns false).
- Keys: the player's keyboard layout, for typed characters and for the shortcuts Ctrl+C, Ctrl+L and Ctrl+D (the letters of the layout, not the physical key positions: `KeyEvent.keycode`, not the scancode). Text comes only from the typed characters (`charTyped`): a dead key and the letter after it never reach `keyPressed`, and the composition goes through the preedit (`^` then `e` gives `ê`). The real Ctrl key, also on macOS (not Cmd). Alt+letter is never a shortcut: Option+c is `ç` on macOS. Any typed character is accepted and shown if the font has it. Esc closes the screen; to revisit when a text editor arrives, since programs use it.

**Commands.** A working subset of the real tools, Linux convention (iproute2, iputils, bash): what is supported really works; any other command or option returns the error of the real tool, in the same format (`bash: foo: command not found`), possibly followed by a translated advice line pointing to `help`.

| Command | Content |
|---|---|
| `ping [-c n] <address>` | One request per second until Ctrl+C, or `n` requests; one line per reply (`icmp_seq`, `ttl`, `time`), then the statistics block. The terminal belongs to the command while it runs (no background jobs in 1c-1); a `ping` goes on when the screen is hidden |
| `ip addr` (`ip a`, `ip addr show eth0`) | Interfaces, MAC, IPv4 address and mask, state of the link, lease time left: an address from DHCP is `dynamic`, its `valid_lft` counts down in real seconds, consistent with the status panel. No `inet6` line: the mod has no IPv6 (idea for later) |
| `ip link` | Physical state of the interfaces (`LOWER_UP` or `NO-CARRIER`): "no cable" is not "no address" |
| `ip route` | From the DHCP lease, in the format of `dhclient` on Debian: `default via 192.168.1.1 dev eth0 ` and `192.168.1.0/24 dev eth0 proto kernel scope link src 192.168.1.10 ` (trailing space included) |
| `ip neigh` | The ARP cache and its states |
| `hostname` | The name of the machine |
| `help`, `help <command>` | Translated: the commands of this machine, their syntax, the differences with the real tool |
| `clear` (Ctrl+L), `history`, `exit` (Ctrl+D) | Base gestures of a terminal |

`ping` shows the situation really met. Cases covered, each checked on a real machine (Debian 13 in Docker, iputils 3:20240905-3, run on 2026-11-19, two network namespaces joined by a veth, the router forwarding without default route; outputs in `src/test/resources/linux-real-outputs.txt`): no address or no route (`ping: connect: Network is unreachable`, exit status 2); absent machine on the local network, ARP fails (`From <own address> icmp_seq=1 Destination Host Unreachable`); destination outside the local network, through the box, which has no route beyond its local network before lot 6c: the box answers `From 192.168.1.1 icmp_seq=1 Destination Net Unreachable` (the form without a colon: iputils prints the errors of its own requests from the error queue of its socket, `ping4_receive_error_msg`), and the statistics count it in `+N errors`. A timeout prints nothing, as on Linux; the statistics say `100% packet loss`. A request sent after the route is lost (cable removed during a `ping`) prints nothing either and is counted as sent: seen on the real machine, and what the code of iputils does (`hard_local_error` reports `sendmsg` only when its error queue holds something). In a continuous `ping` to an absent address, the first run after the network namespace was created lost some `Destination Host Unreachable` lines (three first runs out of three; every later run showed all of them): not explained, the engine shows every error.

Reference of the output tests: real outputs of `ip addr`, `ip link`, `ip route`, `ip neigh` and `ping` (to the gateway, to an absent address) on Debian, iproute2 and iputils in Docker, in `src/test/resources/linux-real-outputs.txt`. The mod keeps their format, trailing spaces included, and adapts what differs on the simulated machine: only `lo` and `eth0`, `eth0` without `@if…` nor `link-netnsid`, MTU 1500, addresses 192.168.1.x, MACs `02:…`.

Pedagogical help stays apart from the output of the tools: translated, marked as a message of the game, shown after the command ends and only after a failure, and turned off by a client setting (`docs/messages.md`, "Terminal").

**Network latency.** The time shown by `ping` (and later by `tcpdump` and `traceroute`) comes from a latency carried by each frame and added at each step of its real path: length of each cable crossed, processing time of each device according to its type. Delivery stays instant in the engine (a frame and its replies are delivered in the same tick, `NetworkSimulation`). The values come from documented sources, cited next to them, and are parameters that change without touching the engine. Results are deterministic; jitter can come later if it brings something. Game time (1 tick = 50 ms) stays for what is not a round trip: the interval between two requests, timeouts, the elapsed time of the statistics, DHCP leases.

**IP stack.** Lot 1c-1 lays down the IPv4 stack per interface, with services bound to a port (idea issue "Services API shared by the network engine and the computer OS"): ARP and ICMP need a real IP layer on the computers, and the DHCP client and server of lot 1b-2 move onto it. The rest of that idea (generic saved memory per service, start and stop) waits for the next service (DNS, lot 7b).

Done at step 1 of the lot (issue #12): the box's computer has a stack with 192.168.1.1/24, its DHCP server bound to UDP port 67. The DHCP client of the computers is not bound to UDP port 68: like ISC `dhclient`, which reads through a packet socket on Linux (`PF_PACKET`, `common/lpf.c`), it reads the frames of its interface before the IP layer, since an offer is addressed to an address the interface does not have yet. Each change of its lease configures the interface as Debian's `dhclient-script` does (address, broadcast, lifetimes, default route). Known difference: unplugging removes the address at once (decision of lot 1b-2), where a real `dhclient` may keep it (to check at lot 1c-2).

Done at step 2 (issue #13): ARP (RFC 826) on every stack, computers and box's computer, with the ARP cache of Linux (`ip neigh`): states `INCOMPLETE`, `REACHABLE`, `STALE`, `DELAY`, `PROBE`, `FAILED`; three broadcast requests one second apart, then `FAILED` after 3 s, the waiting packets reported lost; `REACHABLE` for 30 s after a confirmation (Linux draws between 15 and 45 s; fixed here for deterministic results); 5 s of `DELAY` then three unicast probes; the first answer kept for one second (`locktime`); entries forgotten when the cable is removed. Read in `net/core/neighbour.c` and `net/ipv4/arp.c`. Consequence in the `chain` scene: both boxes are 192.168.1.1, so a computer behind the second box (server off) resolves 192.168.1.1 to that nearer box and its renewal at T1 gets no answer; the broadcast at T2 reaches the first box and the lease is extended, as with two real routers sharing one address.

Done at step 3 (issue #14): ICMP (RFC 792) echo and destination unreachable on every stack; the box forwards, and answers `Destination Net Unreachable` (code 0) for a destination without route; an echo of a computer that ARP cannot deliver comes back to its ping socket as `Destination Host Unreachable` from its own address, as Linux does; its own addresses and 127.0.0.0/8 answer through the loopback. `ping [-c n] <address>` follows the main loop of iputils, read in its sources: on each wake-up the request due leaves before what arrived is read (so the errors of an absent machine come by three every 3 s with `pipe 4`, as on the real machine); with `-c n` it ends when replies and errors reach `n`, otherwise after twice the longest round trip (at least 1 s), or 10 s without reply; the `time` of the statistics runs to the last request sent; exit status 0 with a reply, 1 without, 2 for an error before the first request. Ctrl+C is echoed `^C` by the console, as the terminal driver does. Advice keys `terminal.worldwideblocks.advice.ping.*` (160 characters at most, two rows of 80 columns), shown after a failure only. Differences: only `-c` (other options answered as invalid), a destination not written `a.b.c.d` gets the error of a resolver without answer (`Temporary failure in name resolution`, no name service before lot 7b), several destinations get the usage, the usage text is written for the mod.

Latency of step 3 (class `Latency`, sources next to each value): 5 ns per block of cable (Fluke: Cat5e "a bit less than 5 nS per meter"), 2 µs to cross the switch of a box (Netgear GS108X datasheet, "1Gbps: <2µs"), 50 µs for a computer, or the computer of the box, to receive a frame (calibrated on the real round trips between two Linux namespaces, 0.04 to 0.2 ms once the first request of a run is left out (0.35 to 0.5 ms), and on a published 0.144 ms through a gigabit switch; no measure of a consumer box was found), 7 µs through the loopback (real `time=0.014 ms` to 127.0.0.1). A round trip to a neighbour already resolved is 104 µs (`time=0.104 ms`); the first request also carries its ARP round trip (0.208 ms). The round trip is the difference of the latencies carried within the tick: the game time spent waiting is not counted.

Done at step 5a (issue #11, plain Java): the base of the shell, compared with real sessions of bash 5.2.37, hostname 3.25 and ncurses 6.5 on Debian 13 (account made by `useradd -m`, host `laptop-3f2a`, in a pseudo-terminal, 2026-11-20; sections "bash" of `src/test/resources/linux-real-outputs.txt`). `exit [n]` writes `exit` first and ends with `n` or the last status (a word that is not a number: status 2; two words: status 1 and the shell goes on); Ctrl+D on an empty line writes `exit` on the next line; Ctrl+C at the prompt leaves `text^C` on its line and sets the status 130; Ctrl+L clears the screen and keeps the lines above for scrolling, `clear` clears both, `clear -x` the screen only. `history [-c] [n]` numbers on 5 columns; the history belongs to the account and is shared by the shells of the machine, with the rules of the Debian `.bashrc` (`HISTCONTROL=ignoreboth`: no repeat of the previous line, no line starting with a space; `HISTSIZE=1000`). `hostname` writes the host name and refuses a new one (`you must be root to change the host name`, status 1). `help` lists the commands of the machine and details one, in lines of the game (`docs/messages.md`, section 3). After `exit` or Ctrl+D, the next opening starts a new shell on a clear screen; the console tells each change (line added, current line, clear, command running, history, end) for the screen of step 5b. Differences: usages written for the mod (`hostname` is GPL; `clear` lists only `-x`, `history` only `-c` and `n`), the other options of these tools answered as unknown; no `echo`, `true`, `false` (no shell scripting in this lot).

Done at step 5b (issue #11, server): a right-click on the tower opens its terminal; on the laptop, a click on the keyboard of the open laptop opens it and a click anywhere else moves the lid (`LaptopPart`: the keyboard is the part 1 pixel high, the lid covers it when closed). As for the containers of the game, the click opens the terminal with any item in hand except the cord, which connects, and sneaking keeps unplugging a cable end. A closed laptop drops a closed laptop (`copy_state` into the block state component of the item; its item model selects the closed model, to be seen in a capture at step 5c) and is placed closed; an open one drops a plain laptop, which stacks with new ones. One player at the keyboard (`Keyboards`): another one gets "computer busy" (`message.worldwideblocks.terminal.busy`, through `MessageCenter.gesture`, as an error). The keyboard is freed by Esc, when the player walks away beyond the reach of the game plus 4 blocks (as for containers), leaves the server, dies, changes dimension, when the computer is removed or its lid closed, and when the shell ends (the screen closes; the next opening starts a new shell). Packets: the server sends the console as updates (`TerminalUpdate`: a reset, the lines and history of the snapshot, then each change), 64 per packet, each tick after the simulation; the client sends the keys (`TerminalInputPayload`: line, Ctrl+C with the text typed, Ctrl+D, Ctrl+L, Esc), each checked again: player at this keyboard, session shown, within reach, at most 256 characters without control character, 20 packets at once then one per tick. Refused packets are dropped, logged at DEBUG (`Diagnostics`). Until step 5c, the client keeps the console (`TerminalView`) without drawing it.

Done at step 5c (issue #11, client): the terminal screen (`TerminalScreen`), drawn from the console the server sends (`TerminalView`): the dark popup panel of the game with the name of the machine in white, "Esc: close" and, while a command runs, "Ctrl+C: stop" (translated); Monocraft v4.2.1 with its OFL licence next to the font file, the game font as a fallback for the characters it lacks; output in grey on black with the shadow of the game, the prompt in green and blue, the lines of `help` in white, the advice in yellow after the symbol or word of its level (client setting `terminalAdvice`); a blinking block cursor; rows of 12 font pixels. Its own text size (`TerminalLayout`), in framebuffer pixels per pixel of the font, drawn through a scale of the pose: by default the largest that gives at least 80 columns (3 at 1920 x 1080, 1 at 854 x 480), changed by the client setting `terminalTextSize` and in the terminal with Ctrl+= and Ctrl+- (Ctrl+0: automatic). The line is edited in the screen (left, right, home, end, backspace, delete) and sent with Enter; Up and Down recall the history of the account; the wheel and Shift+Page Up / Page Down scroll; the screen does not pause the game and uses no scissor. Capture scenario `terminal_screen`.

Done at step 5d (issue #11, proofs of the gestures and of the wheel): game tests `terminal_tower_opens_as_a_chest` (a block or a stick in hand opens the terminal and places nothing; sneaking with a block places it against the tower; the cord in hand starts a cable) and `terminal_laptop_sneak_unplugs` (sneak + right-click with an empty hand on the keyboard of the open laptop takes off the end of its cable, without terminal nor lid); capture step `Scroll` (wheel notches through `MouseHandler.onScroll`) and key Shift+Page Down in `terminal_screen`, shots `wheel_down` and `wheel_up`.

Done at step 6 (issue #15): the presence frame is removed (`PresencePayload`, the type 0x88B5, the presence timers of the computers). A computer sends only real traffic: DHCP, ARP and what its commands send; a computer with an address that runs nothing is silent until T1, and the box forgets its MAC 300 s after its last frame (unit test `aComputerWithAnAddressIsSilentUntilItsRenewalAndTheBoxForgetsItMeanwhile`). The loop game tests and the capture scenario `loop` get their traffic from a continuous `ping` to an absent address of the local network, which broadcasts an ARP request every second (unit test `aContinuousPingToAnAbsentAddressBroadcastsArpWithoutEnd`); `loop` shows it in the shot `ping_running`.

Done at step 7 (issue #16): game tests `ping_between_two_computers` (two towers on the box, `ping -c 3` from one to the other: three replies in order, no loss, status 0) and `ping_absent_address` (`ping -c 3` to an address nobody has: three `Destination Host Unreachable` from the computer itself, `+3 errors`, status 1); capture scenario `terminal`.

#### Status of lot 1c-1

Coded from 2026-11-19 to 2026-11-20 on the `lot/1c-1` branch (issues #10 to #16 and #52, after the prototype #30), in 7 steps, one commit each after a green build. `developement` merged into the branch before closing it (design of the devices, issue #69). Final checks on 2026-11-20: clean build with 382 unit tests, 50 game tests, the 18 capture scenarios (17 passed, `failure_check` failed on purpose and put the settings back); the status panels of 13 scenarios identical to the runs made before the merge.

| Type | Equipment | Status |
|---|---|---|
| Interface | Terminal screen | Done: on the tower and on the keyboard of the open laptop, one console per machine kept by the server, one player at the keyboard, packets checked by the server, Monocraft, its own text size, scrollback, history, the player's keyboard layout |
| Mechanic | IPv4 stack of the computers | Done: `lo` and `eth0` configured by the DHCP lease, routes, UDP demultiplexing; the box has 192.168.1.1 |
| Mechanic | ARP | Done: RFC 826, neighbour table with the states and delays of Linux, the box answers for 192.168.1.1 |
| Mechanic | ICMP and latency | Done: echo and destination unreachable (RFC 792); latency carried by each frame (cables, devices); the box answers Net Unreachable outside its network |
| Interface | Commands | Done: `ip addr`, `ip link`, `ip route`, `ip neigh`, `ping [-c n]`, `hostname`, `help`, `clear`, `history`, `exit`; output compared with Debian 13, except the lines reported to lot 1c-2 (`dynamic`, `valid_lft`, `NO-CARRIER`, `qdisc fq_codel`) |
| Mechanic | Presence frame | Removed: a computer sends only real traffic |
| Tooling | Capture runs | Done: permanent scenarios for every check by eye, checks of the frame of the terminal and of its rows (`Expect`), addresses read from the leases |

#### Check by eye of lot 1c-1

Capture runs, in English and in French, at 854 x 480 and 1920 x 1080 (`./gradlew runClientCapture -Pscenario=<name>`); the assistant reads the screenshots and `values.txt` and reports. Only the keystrokes through the keyboard layouts of the operating system stay by hand (they cannot be injected): done by Max on 2026-11-20, AZERTY and QWERTY on macOS, all as expected (step 5c).

| Point | Scenario | What it shows |
|---|---|---|
| 1. Address of the computer | `terminal` | `ip a`: `lo`, and `eth0` with the address lent by the box (`dynamic`, lifetimes) |
| 2. Ping the box | `terminal` | `ping -c 3 192.168.1.1`: three replies, statistics without loss |
| 3. Ping another computer | `terminal` | `ping -c 3` to the laptop: three replies; the goal of the lot |
| 4. Absent address | `terminal` | `ping -c 3 192.168.1.250`: three `Destination Host Unreachable` from the tower itself, statistics with `+3 errors`, then the advice of the game, translated |
| 5. Terminal screen | `terminal_screen` | Prompt, `help` translated, `ip a` wrapped, text size (Ctrl+=), a running `ping` stopped by Ctrl+C, Ctrl+L, scrolling (Shift+Page Up / Page Down, wheel), history (Up), Esc then reopening, `exit`, the laptop with accents typed; automatic GUI scale and scale 2 |
| 6. Advice turned off | `terminal_advice` | The advice after a failed `ping`, then hidden by the client option `terminalAdvice` |
| 7. Loop without the presence frame | `loop` | One loop message, a `ping` in the terminal (`ping_running`), one more message a minute later, none once a cable is removed |


**Throwaway prototype**: done on 2026-11-19 (issue #30, closed, findings in the issue; branch `proto/terminal-screen`, never merged): keyboard input (SDL3) with the AZERTY and QWERTY layouts, dead keys, shortcuts and arrows reaching the screen, Monocraft against the game font, character grid at 854 x 480 and 1920 x 1080, frame and colours, a screen that does not pause the game in single player. Its automatic capture run (`runClientCapture`) becomes permanent in this lot (issue #52, `docs/automated-checks.md`).

Pushed back: completion with Tab and read-only viewing (backlog), fixed address with `ip addr add` (idea issue "Configure the devices like real ones", once the operating system has files), `traceroute` (6c), `nslookup` and `dig` (7b), `ss` and `nc` (1f), `uptime`, `reboot` and `poweroff` (1e), pipes, redirections, variables and background jobs (scripts), terminal text shown on the block in the world (F).

### 1c-2: network diagnostics

| Type | Equipment | Role |
|---|---|---|
| Command | `tcpdump [-i eth0] [-n] [-e] [-c n] [filter]` | One line per frame that really goes through the interface of the machine, until Ctrl+C or `n` frames |
| Command | `dhclient -v eth0`, `dhclient -r eth0` | Runs the real DHCP exchange again and shows it; releases the lease for real and removes the configuration of the interface |

Playable at the end: `tcpdump` runs on one computer while another `ping`s it; the ARP request, its reply, then the ICMP echoes scroll by.

- `tcpdump` reads the frames of `FrameTap`, on the interface of the machine only: its incoming and outgoing frames and the broadcasts it receives, never the traffic of other machines. This is what a real computer sees behind a box or a switch, and lot 1d-1 shows it.
- Output in the real format, with the information that really exists in the engine. The timestamp is the time of the tick plus the latency carried by the frame.
- Options `-n` (accepted; same output, there is no DNS yet), `-e` (MAC addresses, needed in lot 1d-1), `-c n`.
- Filters: a closed grammar that really filters the frames, `arp`, `icmp`, `udp`, `port <n>`, `host <address>`. Anything else is refused with an error; `and` and `or` come later if needed.
- `dhclient -v eth0` shows the real exchange (`DHCPDISCOVER`, `DHCPOFFER`, `DHCPREQUEST`, `DHCPACK`); `dhclient -r eth0` releases the lease and removes the address and the routes of the interface.

The order of lots 1c-1, 1c-2 and 1d-1 follows the learning: use the network, observe what travels on it, then see what a switch changes (unicast and broadcast).

### 1d-1: desktop switch

| Type | Equipment | Role |
|---|---|---|
| Block | 5-port desktop switch | Multiplies the network sockets, without configuration |
| Block | 8-port desktop switch | Same, with more ports |
| Mechanic | Port chosen by aiming | The aimed port is outlined and named by the status panel before the click; elsewhere on the device, the first free port |
| Rendering | Cable attached to its port | The cable leaves from the port, with an RJ45 plug pushed into it; thick cable instead of a line; the hanging curve stays until 1d-2 |

Playable at the end: six or more computers on the same box, all reachable by `ping`, each cable in the port the player chose.

Design of the port choice and of the attachment: `docs/cable-design.md`, sections 3 and 4 (decided on 2026-11-20). Before the lot: the tower and the laptop redrawn on the design guide with the position of their ports (issue #56).

#### Status of lot 1d-1

Coded on 2026-11-21 on the `lot/1d-1` branch (issues #61, #17, #18, #62, #63, #19), one commit per step after `./gradlew build test`, `./gradlew runGameTestServer` and the capture runs of the step, all run and read by the assistant during a session where Max was away; to be looked at by Max. The port layout of each device (`port/PortLayout`), the 5-port and 8-port desktop switches (role `switch`, the switching of the box without its local point), the port chosen by aiming (outline, panel line, the four gestures), the RJ45 plug in each port and the thick cable. Playable goal checked: the game test `ping_seven_computers_box_and_switch` (seven computers, three on the box and four behind a 5-port switch, each pings the next one) and the capture `seven_computers` (the scene, the box lending seven addresses, the switch learning a MAC behind each port, a `ping -c 3` from a tower behind the switch to a tower of the box: three replies, 0% loss, time 0.108 ms, the 0.104 ms of a computer of the box plus the 2 µs of the switch each way). Captures of the lot: `device_switch_5`, `device_switch_8`, `port_aim`, `plugs`, `seven_computers` (`docs/automated-checks.md`). The test operator line block (issue #31) was moved to lot 3b on 2026-11-21 (Max). Decisions validated by Max on 2026-11-21, with one change: the cable is never less than one pixel of the screen wide.

### 1d-2: cable on the ground

In version 0.2.0 (decided on 2026-11-20), so as not to delay 0.1.0.

| Type | Equipment | Role |
|---|---|---|
| Rendering | Cable on the ground | The cable comes down from the plug, follows the floor, climbs steps and goes around walls, up to the other device; drawn by the client, no collision |
| Rendering | Hanging fallback | With no route on the ground (wall without an opening, terrain not received), the cable hangs in the air as today |

Playable at the end: a room cabled with several computers, the cables lying side by side on the floor and going out through the door; the player walks over them.

Design: `docs/cable-design.md`, section 5. Steps 3 and 4 (wall sockets and cable fixed to the wall in 2b, trunking in 2b, cable trays in 8a) reuse its chain of spans (section 6).

## Lot 1e: electricity

Short lot, numbered in the 1 series because it is placed right after lot 1, so as not to have to rework dozens of blocks later. It is not part of the network simulation itself.

| Type | Equipment | Role |
|---|---|---|
| Block | Electrical panel | Energy source of the home; also accepts energy from other mods (RF) |
| Item | Electric cable | Links the panel to the sockets |
| Block | Wall power socket | Power point |
| Item | Power cord | Links a device to a socket |
| Block | Power strip | Several devices on one socket |
| Mechanic | Consumption | Each device consumes some power; without power, it is off and no longer answers |
| Mechanic | Laptop battery | The laptop keeps working unplugged, as long as its battery is not empty (see "Open decisions") |
| Setting | "Power required" option | Allows playing without managing electricity (see "Open decisions") |

Playable at the end: unplug the box, and the whole home network goes down.

Proposed choice: since the mod must work on its own, the electrical panel supplies energy itself. Plugging it into another mod stays possible but never mandatory.

## Lot 1f: TCP

Numbered in the 1 series like lot 1e: it completes the protocols of lot 1 (IPv4, UDP, DHCP, ARP, ICMP) and comes before lot 2c, whose file sharing relies on it, as do the web server (7b) and SSH (8d). Simplified version: no sliding window or congestion control at first.

| Type | Equipment | Role |
|---|---|---|
| Mechanic | Connection | Three-way handshake (SYN, SYN-ACK, ACK), then close (FIN) |
| Mechanic | Sequence and acknowledgement numbers | Each side counts what it sent and what it received |
| Mechanic | Simple retransmission | A segment not acknowledged in time is sent again |

Playable at the end: from the terminal, a connection opens between two computers, carries a message and closes; each step can be followed in the exchanged segments.

## Lot 2: complete local network

### 2a: Wi-Fi

| Type | Equipment | Role |
|---|---|---|
| Mechanic | Box Wi-Fi | The box broadcasts a wireless network with a name and a password, over a limited range |
| Mechanic | Laptop Wi-Fi card | The laptop lists the networks in range and connects to one |
| Block | Wi-Fi access point | Extends the coverage, connected to the box by a cord |
| Block | Wi-Fi repeater | Extends the coverage without a cable, plugged into a power socket |

Playable at the end: the laptop, without a cable, `ping`s the tower.

### 2b: fixed home cabling

| Type | Equipment | Role |
|---|---|---|
| Item | Installation Ethernet cable | Cable laid permanently between the enclosure and the sockets |
| Block | RJ45 wall socket | Connection point in a room |
| Block | Communications enclosure | Panel where all the cables of the home arrive, with the box |
| Block | Cable trunking | Guides the cables along the walls |
| Block | Wall clip (added on 2026-11-20) | Holds a cable along a wall; a fixed point of its route (`docs/cable-design.md`, section 6) |

Playable at the end: a computer connected to the socket of a bedroom reaches the box stored in the enclosure.

Cables (decided on 2026-11-20, `docs/cable-design.md`, section 6): the wall socket is a device with ports (aim and plugs of lot 1d-1); wall clips and trunking are real blocks that become fixed points of the route of a cable, stored in the link; the length of a cable becomes the length of its route.

Needs noted by Max on 2026-11-20, to design with the lot: a preview of the route before a cable through a wall is laid, then a way to find hidden cables again in offices, for example a view through the walls while holding a cable, each cable in the colour of its cord; later a detection tool of lot 9a could do the same (`docs/cable-design.md`, section 6).

### 2c: peripherals and local services

| Type | Equipment | Role |
|---|---|---|
| Block | Screen | Display of the tower |
| Block | Keyboard | Input for the tower |
| Block | Mouse | Pointing for the tower |
| Block | Storage server (NAS) | File sharing on the local network |
| Block | Network printer | Printing from any computer |

Playable at the end: a file dropped from one computer is read from another.

The screen, keyboard and mouse are not required to use the tower (decision of 2026-11-19 on issue #10, see "Decisions taken"): a connected screen will give access to the graphical mode when it exists, and a server option may require the peripherals. To design at this lot.

### 2d: other devices in the home

| Type | Equipment | Role |
|---|---|---|
| Block | IP phone | Telephony over the network |
| Block | IP camera | Video surveillance over the network |
| Block | Smart television | Network client of the living room |
| Block | Powerline adapter | Carries the network over the electrical circuit (depends on lot 1e) |
| Block | PoE injector | Powers a device through its network cable (depends on lot 1e) |

Playable at the end: each new device obtains an address and answers `ping`.

## Lot 3: arrival at the subscriber

So that this lot is playable before the street exists, a test block provides the line. It was brought forward to lot 1d-1, then moved to lot 3b on 2026-11-21 (Max, issue #31): its model is to approve in Blockbench and it connects to the line input of the box with the RJ11 telephone cord of lot 3b.

| Type | Equipment | Role |
|---|---|---|
| Block | Test operator line | Creative mode block that simulates the upstream operator; replaced by the real chain over lots 4 to 6 |

### 3a: fibre arrival

| Type | Equipment | Role |
|---|---|---|
| Block | Optical termination socket | Point where the fibre arrives in the home, passive |
| Item | Optical patch cord | Short fibre cord between the optical socket and the fibre box |
| Block | Fibre box (ONT) | Converts the optical signal to Ethernet for the box |
| Item | Optical drop cable | Fibre that links the optical socket to the outside |

Playable at the end: the box shows "line connected".

### 3b: copper arrival

| Type | Equipment | Role |
|---|---|---|
| Block | Telephone socket | Point where the copper line arrives in the home |
| Item | Telephone cord | Links the box to the telephone socket |
| Item | Copper drop cable | Copper pair that links the socket to the outside |
| Mechanic | Speed depending on distance | On copper, the speed drops with the length of the line |

Playable at the end: the box shows "line connected" over copper, with a speed that depends on the length.

## Lot 4: the street

### 4a: overhead

| Type | Equipment | Role |
|---|---|---|
| Block | Pole | Support for overhead cables, several blocks high |
| Item | Overhead optical cable | Multi-fibre cable strung from pole to pole |
| Item | Overhead copper cable | Multi-pair cable strung from pole to pole |
| Block | Optical connection box | Last fibre box before the homes, fixed on a pole or on a facade |
| Block | Copper concentration point | Last copper box before the homes, fixed on a pole or on a facade |

Playable at the end: a house connected by a line strung over three poles.

### 4b: underground

| Type | Equipment | Role |
|---|---|---|
| Block | Pull chamber | Manhole under the pavement where cables are pulled and connected |
| Block | Duct | Buried conduit between two chambers |
| Item | Underground optical cable | Fibre cable pulled through the ducts |
| Block | Splice closure | Waterproof junction between two cables, in a chamber |

Playable at the end: a house connected by a buried line passing through two chambers.

## Lot 5: the neighbourhood

### 5a: in fibre

| Type | Equipment | Role |
|---|---|---|
| Block | Fibre street cabinet | Point where the fibres of the homes meet those of the operators |
| Item | Optical splitter | Shares one operator fibre between several subscribers, for example 1 to 32; goes in the cabinet |
| Item | Optical distribution cable | High-capacity cable between the cabinet and the connection boxes |
| Item | Optical transport cable | Links the street cabinet to the operator's site |

Playable at the end: several houses connected to the same cabinet, on a single fibre on the operator side.

### 5b: in copper

| Type | Equipment | Role |
|---|---|---|
| Block | Copper street cabinet (sub-distribution frame) | Point where an area is connected to the operator's main network |
| Item | Copper transport cable | Cable with many pairs between the cabinet and the operator's site |

Playable at the end: several houses connected in copper to the same cabinet.

## Lot 6: the ISP

### 6a: the rack

Separate sub-lot, because it introduces a new mechanic: mounting equipment in numbered slots.

Prerequisite: the scale of rack equipment has been decided (see "Open decisions").

| Type | Equipment | Role |
|---|---|---|
| Block | 19-inch rack | Cabinet that receives rack-mountable equipment |
| Mechanic | U slots | Each piece of equipment takes a number of units; you choose its slot in the rack |
| Item | Test rack-mountable equipment | Empty 1U box to validate mounting |

Playable at the end: you mount and unmount a piece of equipment at the chosen slot.

### 6b: the technical room

| Type | Equipment | Role |
|---|---|---|
| Block | Optical patch tray | Termination of the fibres in the rack, for patching |
| Block | Fibre line head equipment (OLT) | Active equipment of the operator; each port serves a group of subscribers through the splitters |
| Block | Copper distribution frame | Patching of the copper pairs on arrival at the exchange |
| Block | Copper line head equipment (DSLAM) | Equivalent of the OLT for copper lines |

Playable at the end: the "test operator line" block is no longer needed, the real chain goes from the box to the operator's site.

### 6c: the ISP service

Split in two sub-lots: address translation (NAT) is the most structuring change of the plan after DHCP. It redefines addressing (private against public), adds a stateful translation to the box and conditions `traceroute`, so it is trialled on its own.

#### 6c-1: public address

| Type | Equipment | Role |
|---|---|---|
| Block | Subscriber aggregation router | Groups the sessions of the boxes and gives them a public address |

Playable at the end: each connected box obtains a public address from the aggregation router and shows it in the status indicator, while its computers keep their private addresses.

#### 6c-2: address translation and traceroute

| Type | Equipment | Role |
|---|---|---|
| Mechanic | Address translation in the box | The box shares its public address between the computers of the home (NAT) |
| Command | `traceroute <address>` | Shows the equipment crossed up to the destination |

Playable at the end: from a computer, `ping` to the public address of the box of another subscriber of the same ISP, which answers. A computer behind another box cannot be reached directly by its private address, as in reality.

## Lot 7: the internet

### 7a: routing between operators

| Type | Equipment | Role |
|---|---|---|
| Block | Operator router | Forwards packets between networks according to a routing table |
| Item | Long-distance fibre | Link between two distant sites |
| Screen | Router console | Command-line configuration of interfaces and routes |

Playable at the end: `ping` to the public address of the box of a subscriber of another ISP, which answers.

### 7b: servers and domain names

| Type | Equipment | Role |
|---|---|---|
| Block | Rack server | Machine that hosts services, 1U in a rack |
| Service | DNS server | Translates a domain name into an address |
| Service | Web server | Answers a request with a page |
| Command | `nslookup <name>` | Queries the DNS |

Playable at the end: `ping` to a name rather than to an address.

### 7c: the mesh

Dynamic routing is, with the DHCP of lot 1b and the NAT of 6c-2, one of the heaviest algorithmic pieces of the mod. Its cost moves towards the server in multiplayer: routing tables and re-advertised routes add traffic to the engine, so the safety limits of lot 1a (10,000 frames processed per tick, 50,000 in the queue) are to be reviewed when this lot is designed.

| Type | Equipment | Role |
|---|---|---|
| Mechanic | Dynamic routing | Routers exchange their routes and adapt to failures |
| Block | Internet exchange point | Neutral site where several operators interconnect |

Playable at the end: a link between two operators is cut, the traffic resumes through another path.

## Lot 8: enterprise and datacenter

Distant lots: the lists are complete, but the split will be reviewed when we get there.

### 8a: enterprise racks and patching

| Type | Equipment | Role |
|---|---|---|
| Block | Wall cabinet | Small rack fixed to the wall for a floor or a small site |
| Block | Copper patch panel | Termination in the rack of the cables coming from the wall sockets |
| Item | Patch cord | Short cord between the panel and the switch |
| Block | Cable guide | Guides the cords between two pieces of equipment |
| Block | Blanking panel | Closes an empty slot of the rack |
| Block | Cable tray | Supports the cables on the ceiling between the racks and the rooms; a carrier whose path becomes part of the route of the cables it holds (`docs/cable-design.md`, section 6) |

Playable at the end: a floor cabled from the wall sockets up to the switch in the rack.

### 8b: configurable switch

| Type | Equipment | Role |
|---|---|---|
| Block | 24-port access switch | Connects the computers of a floor, 1U |
| Block | 48-port access switch | Same, denser, 1U |
| Block | Distribution switch | Links the access switches together |
| Item | Optical module | Inserts into a switch port to link it in fibre |
| Screen | Switch console | Command-line configuration |
| Mechanic | Virtual networks (VLAN) | Separates several networks on the same switch |
| Item | Console cable | Links a computer to the configuration port of a device |

Playable at the end: two groups of computers on the same switch, isolated from each other.

### 8c: enterprise router and firewall

| Type | Equipment | Role |
|---|---|---|
| Block | Enterprise router | Links the site to the internet and to the other sites |
| Block | Firewall | Filters the traffic between zones according to rules |
| Mechanic | Filtering rules | Allow or block according to the source, the destination and the service |
| Mechanic | Tunnel between sites (VPN) | Links two sites as if they were on the same network |

Playable at the end: an allowed flow goes through, a forbidden flow is blocked.

### 8d: servers and storage

| Type | Equipment | Role |
|---|---|---|
| Block | 1U server | General-purpose flat server |
| Block | 2U server | Server with more disks |
| Block | 4U server | Compute or storage server |
| Block | Tower server | Server for a site without a rack |
| Block | Disk array | Storage extension |
| Block | Rack-mountable screen and keyboard console | Retractable screen and keyboard to work on a server |

Playable at the end: a service hosted on a rack server, reached from a computer.

### 8e: room power

| Type | Equipment | Role |
|---|---|---|
| Block | Rack power strip | Distributes power to the equipment of the rack |
| Block | Rack-mountable UPS | Keeps the power on during a cut |
| Block | Battery cabinet | Extends the UPS autonomy |
| Block | Generator | Long-duration backup |
| Mechanic | Dual power supply | A device connected to two sources survives the loss of one |

Playable at the end: the power is cut, the servers stay on for the duration of the autonomy.

### 8f: cooling

| Type | Equipment | Role |
|---|---|---|
| Block | Air conditioning cabinet | Cools the room |
| Block | Raised floor tile | Technical floor under which the air and the cables pass |
| Block | Perforated tile | Lets cold air rise in front of the racks |
| Block | Temperature probe | Measures the temperature of the room |
| Mechanic | Heat | Equipment heats up; too hot, it stops |

Playable at the end: without air conditioning, the room heats up and the servers stop.

### 8g: system and accounts

| Type | Equipment | Role |
|---|---|---|
| Mechanic | Operating system | Files, programs and sessions on the computers and servers |
| Mechanic | User accounts | Login with an identifier and a password |
| Service | Directory | Server that centralises the accounts and the rights |

Playable at the end: a session is opened on a computer with an account defined on the directory server.

## Lot 9: wide wireless networks, tools and survival

Distant lots: the lists are complete, but the split will be reviewed when we get there.

### 9a: cabling and test tools

| Type | Equipment | Role |
|---|---|---|
| Item | Cable spool | Raw cable to cut to the wanted length |
| Item | RJ45 connector | Fitted at the end of a cable |
| Item | Crimping tool | Fixes the connector on the cable |
| Item | Cable tester | Checks that a cable is correctly assembled |
| Item | Fibre splicer | Joins two fibres |
| Item | Reflectometer | Locates a break on a fibre |

Playable at the end: a cord made by hand, tested, then used.

Idea noted by Max on 2026-11-20: a cable detector that shows where hidden cables run, like the view through the walls planned with the wall sockets of 2b (`docs/cable-design.md`, section 6).

### 9b: mobile network

| Type | Equipment | Role |
|---|---|---|
| Block | Mast | Support for the antennas |
| Block | Mobile phone antenna | Covers a sector around the mast |
| Block | Radio site cabinet | Equipment and power at the foot of the mast |
| Block | Mobile router | Gives internet access through the mobile network |
| Item | Mobile phone | Client of the mobile network |

Playable at the end: a home without a fixed line reaches the internet through the antenna.

### 9c: satellite and microwave link

| Type | Equipment | Role |
|---|---|---|
| Block | Subscriber satellite dish | Internet access by satellite |
| Block | Earth station | Gateway between the satellites and the terrestrial network |
| Block | Microwave link antenna | Radio link between two sites in direct line of sight |

Playable at the end: an isolated site linked to the network without any cable.

### 9d: survival mode

| Type | Equipment | Role |
|---|---|---|
| Mechanic | Crafting recipes | Each piece of equipment is crafted from resources |
| Item | Intermediate components | Circuit, case, raw cable |

Playable at the end: the whole mod is accessible without creative mode.

## Lot F: finishing

| Content | Detail |
|---|---|
| Lights | Link and activity lights per port, animated |
| Textures | Screens and fronts lit or off depending on the state |
| Sounds | Fans, connection clicks, beeps |
| Models | Rework of the remaining provisional models and textures, following `docs/design-guide.md`. The internet box, the PC tower and the laptop were redrawn on 2026-11-19 and 2026-11-20; `models/block/laptop_open.json`, used by no blockstate, was removed on 2026-11-20 |
| Cord on the ground | Moved to lot 1d-2 (decided on 2026-11-20, `docs/cable-design.md`); lot F keeps the polish: rounded bends, a little slack |
| HUD interface | More professional and more polished HUD for the status panel and the messages (requested by Max on 2026-11-18) |

HUD interface: the current rendering (compact or detailed panel, messages with symbol and colour) is **approved by Max on 2026-11-18 as a base**; nothing has to change before lot F. The future work is about the look: a more professional and more polished HUD, with a check of the rendering on several window sizes (in 854 x 480, the second line of the cord tooltip wraps). The message module (see `docs/messages.md`) separates data from drawing: the server only sends keys, values and a level, the panel lines are built in plain Java (`StatusPanel`), and only the client decides the look. The new interface therefore replaces the drawing (`client/ClientMessages`, `client/PortStatusLayer`) without touching the engine, the packets or the texts.

Cord on the ground, idea noted on 2026-11-17 (now designed for lot 1d-2 in `docs/cable-design.md`, decided on 2026-11-20):

- No physics simulation: the route is computed once. The cable follows the terrain between the two devices, with a slight sag between two supports.
- The route is recomputed only when a block changes on the path.
- Point to decide at that time: what to do if a wall separates the two devices (see "Open decisions").
- Routing along walls and on cable trays stays planned at lots 2b (trunking) and 8a (cable trays).

## Dependencies

- Lot 1a is required by everything else.
- Cables (decided on 2026-11-20, `docs/cable-design.md`): the port choice and the plugs of 1d-1 come before the cable on the ground of 1d-2; the wall sockets, wall clips and trunking of 2b, then the cable trays of 8a, reuse its chain of spans.
- 1c-1 depends on 1b: without an address, no `ping`. 1c-2 depends on 1c-1 and comes before 1d-1, whose switches it helps to understand.
- 1f (TCP) depends on 1c-1 (IP stack per interface) and is required by 2c (file sharing), 7b (web server) and 8d (SSH).
- Lot 1e is required by the powerline adapter, the PoE injector and room power (8e).
- Lot 3 needs the test block as long as lots 4 to 6 are not done.
- The 19-inch rack (6a) is required by 6b, 7b and the whole of lot 8.
- The decision on the scale of rack equipment must be taken before 6a.
- The decision on making cables by hand must be taken before 2b; if the tools of 9a are brought forward, lot 9a keeps their certifier and fibre tools and the crafting recipes move to 9d.

## Real-world operation: what was verified

| Point | Verification | Source |
|---|---|---|
| Address assignment in four steps (discover, offer, request, acknowledge) | Read | [1] |
| Fibre chain: line head equipment at the operator, passive splitters, box at the subscriber | Read | [2] |
| One OLT port serves about 32 or 64 subscribers through the splitters; range of about 20 km at 32 subscribers | Read | [2] |
| In France, operators connect to the shared network through a mutualisation point: box at the foot of a building or street cabinet | Read | [3], [4] |
| The aggregation router sits at the edge of the ISP core network and groups the subscribers' sessions | Read | [5] |
| The splitters sit at the transition between the operator's fibre and the subscribers' fibres | Read | [3] |
| The zone mutualisation point, in the street, groups 360 to 1,000 lines | Read | [4] |
| The OLT sits at the optical connection node and plays the role of the DSLAM there | Read | [6] |
| Copper: the local loop goes from the operator's distribution frame to the subscriber's socket | Read | [7] |
| Copper: the distribution frame is in the exchange; the sub-distribution frame connects an area to the main network | Read | [8] |
| Private addresses (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) are not routed on the internet: a computer behind a box can be reached from outside only through address translation (NAT) | Excerpt | [9], [10] |
| Copper: the last box before the home is the concentration point (PC), on a facade or pole; the distribution cable, shared by several subscribers, arrives upstream | Excerpt | [11], [12] |
| Copper: the line goes up from the concentration point to the sub-distribution frame, then to the distribution frame | Excerpt | [13] |

"Read": page opened and read. "Excerpt": seen in a search engine result, page not opened in full.

The sources read are mostly Wikipedia: this is enough for the general operation. For the points specific to France (PM, PBO, NRO, PC), read first, before lots 4 and 5, the documents identified in the inventory: the CREDO guide from Acome and the collection of specifications from Arcep for less dense areas.

## Sources

1. Wikipedia, "Dynamic Host Configuration Protocol": https://en.wikipedia.org/wiki/Dynamic_Host_Configuration_Protocol
2. Wikipedia, "Passive optical network": https://en.wikipedia.org/wiki/Passive_optical_network
3. Wikipedia (in French), "FTTH network": https://fr.wikipedia.org/wiki/R%C3%A9seau_FTTH
4. Wikipedia (in French), "Mutualisation point": https://fr.wikipedia.org/wiki/Point_de_mutualisation
5. Wikipedia, "Broadband remote access server": https://en.wikipedia.org/wiki/Broadband_remote_access_server
6. Wikipedia (in French), "Optical connection node": https://fr.wikipedia.org/wiki/N%C5%93ud_de_raccordement_optique
7. Wikipedia (in French), "Local loop": https://fr.wikipedia.org/wiki/Boucle_locale
8. Wikipedia (in French), "Telephone distribution frame": https://fr.wikipedia.org/wiki/R%C3%A9partiteur_t%C3%A9l%C3%A9phonique
9. IETF, RFC 1918, "Address Allocation for Private Internets": https://www.rfc-editor.org/rfc/rfc1918.txt
10. Wikipedia, "Private network": https://www.wikipedia.org/wiki/Private_network
11. OVHcloud (in French), "Internal service line": https://docs.ovhcloud.com/fr/guides/web-cloud/internet/internet-access/la-desserte-interne
12. Arcep, opinion of the copper expert committee DEX150505, VDSL2 at the PC: https://arcep.fr/fileadmin/reprise/dossiers/degroup/avis-DEX150505-VDSL2_au_PC-050515.pdf
13. Orange, definition and classes of PLP: https://developer.orange.com/ope-contents/channels/87afd7365baec589/offers/7QKqBnDZ5utv021d/products/sAxl9K40i4n0a0sS/contents/faKUIj4gm2fYD5G2
14. Arcep, 2026 report, copper network closure sheet: https://www.arcep.fr/fileadmin/user_upload/espace_collectivites/rapport-TC-2026/RA2026-Tome02-fermeture-reseau-cuivre.pdf
