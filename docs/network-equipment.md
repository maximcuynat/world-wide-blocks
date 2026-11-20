# Network equipment: from the home to the operator

Inventory of the equipment to model in World Wide Blocks, with their real dimensions when known. This document is used to size the blocks. It does not yet decide the development order or the in-game size of each object.

Written on 2026-11-17.

## How to read this document

Each dimension has a status, because they are not all equally reliable:

| Status | Meaning |
|---|---|
| **S** | Sourced: the page or datasheet was read, the source is numbered at the end of the document |
| **E** | Extracted: value seen in a search engine result, page not opened. To be confirmed |
| **M** | From memory: usual order of magnitude, no source consulted. To be verified before relying on it |
| [TO COMPLETE] | No reliable value found |

Dimensions are given in millimetres, in the order width x depth x height, unless stated otherwise.

A useful remark: in game, 1 pixel stands for 62.5 mm and the equipment is enlarged by about 2.4. An error of a few millimetres therefore changes nothing in the model. A dimension is only checked when the equipment is modelled, lot by lot, without trying to confirm everything in advance.

Limit of this research: the page reading tool was blocked during the session. Only some of the manufacturer datasheets could be opened, and many dimensions remain at status E or M. The list of equipment, however, is complete for a first inventory.

## Scale: from real life to the game

- In Minecraft, 1 block stands for 1 m and is divided into 16 px, which is 62.5 mm per pixel.
- At this exact scale, most equipment is unreadable: a 1U switch would be 7 px wide and 0.7 px high, a PC tower 3 px wide.
- The two blocks already made are therefore enlarged: the PC tower is 8 px wide for 210 mm in real life, a factor of about 2.4.
- Open decision before modelling the racks: a single enlarged scale (consistent but bulky), or one scale per family (racks at player scale, small equipment enlarged).
- Proposal: one scale per family. With the factor 2.4, a 42U rack of 1991 mm would be about 4.8 blocks high. The decision is tracked in `docs/lot-plan.md`, section "Open decisions".

## 1. Computers and terminals

| Equipment | Role | Real dimensions | Status |
|---|---|---|---|
| Desktop tower | Fixed workstation | ≈ 210 x 450 x 450 | M |
| Mini PC | Compact computer, often fixed behind the monitor | [TO COMPLETE] | |
| 15-inch laptop | Mobile computer, Wi-Fi and Ethernet | ≈ 360 x 240 x 20 closed | M |
| 24-inch monitor | Computer display | [TO COMPLETE] | |
| Keyboard, mouse | Input | [TO COMPLETE] | |
| Network printer | Printing shared on the network | [TO COMPLETE] | |
| Smartphone, tablet | Wi-Fi and mobile clients | [TO COMPLETE] | |
| Smart TV | Network client of the living room | [TO COMPLETE] | |
| Game console | Network client | [TO COMPLETE] | |
| IP phone | Telephony over the network, often powered by PoE | [TO COMPLETE] | |
| IP camera | Video surveillance, often powered by PoE | [TO COMPLETE] | |
| Smart home hub | Gateway of the connected objects | [TO COMPLETE] | |

## 2. Internet access at the subscriber's

| Equipment | Role | Real dimensions | Status |
|---|---|---|---|
| Home gateway (internet box, e.g. Livebox 6) | Modem, router, Wi-Fi and telephony in one case | 183 x 183 x 80 | E |
| Optical outlet (PTO, optical termination outlet, e.g. Acome PTO2803) | Arrival of the fibre in the home, passive | 100 x 80 x 27.5 | E |
| Optical network terminal (ONT, e.g. Fracarro ONTG-4GP2F-S) | Converts the operator's fibre into Ethernet | 205 x 110 x 30 | E |
| Phone socket, DSL filter | Arrival of the copper line | [TO COMPLETE] | |
| Cable modem | Access through the coaxial cable network (out of the plan's scope) | [TO COMPLETE] | |
| Mobile router (4G/5G, e.g. Netgear Nighthawk M6) | Internet access through the mobile network | 105 x 105 x 21.5 | E |
| Satellite dish (Starlink V5) | Subscriber's satellite terminal, 35 to 50 W on average | 384 x 306 x 34 | S [3] |
| Starlink V5 power supply | Powers the dish | 157 x 74 x 36 | S [3] |
| Starlink Mini router | Wi-Fi 6 router, 2 RJ45 ports at 1 Gbit/s | ≈ 138 x 83.6 x 27 (order of the dimensions uncertain) | S [3] |

## 3. Home and small office local network

| Equipment | Role | Real dimensions | Status |
|---|---|---|---|
| Wi-Fi router (e.g. TP-Link Archer AX55) | Routing, simple firewall, Wi-Fi | 261.1 x 134.5 x 41 | E |
| 5-port desktop switch | Multiplies the Ethernet sockets | ≈ 100 x 75 x 25, provisional value chosen for the model (100 x 100 x 25 on 2026-11-19, depth reduced on 2026-11-20), not verified | M |
| 8-port desktop switch (e.g. Netgear GS108) | Same, unmanaged | 158 x 102 x 27 | E |
| 16-port desktop switch | Same | [TO COMPLETE] | |
| Access point (e.g. Ubiquiti U6 Lite) | Broadcasts the Wi-Fi, ceiling-mounted, PoE, 12 W max | Ø 160 x 32.7 | E |
| Wi-Fi extender | Extends the coverage, plugs into a power socket | [TO COMPLETE] | |
| Mesh system | Several coordinated Wi-Fi nodes | [TO COMPLETE] | |
| Powerline adapter | Carries the network over the electrical circuit | [TO COMPLETE] | |
| PoE injector | Adds power to an Ethernet cable | [TO COMPLETE] | |
| RJ45 wall socket (Legrand Mosaic, 1 module) | Connection point in the wall | 22.5 x 45 on the front | E |
| Home network enclosure, grade 3 (Legrand 4 132 10) | Structured wiring panel (VDI) of the home, 6 RJ45 | 250 x 250 x 103.5 | E |
| 2-bay NAS (e.g. Synology DS223) | Shared storage, ≈ 17 W | 165 x 108 x 232.7 (order of the dimensions to be verified) | E |
| 4-bay NAS (e.g. Synology DS423) | Shared storage, ≈ 32 W | 184 x 168 x 230 | E |
| Home server | Services at home | [TO COMPLETE] | |
| Desktop UPS (e.g. APC BX700U-GR, 700 VA) | Keeps the power on during an outage | 115 x 256 x 200 | E |
| Power strip | Electrical distribution | [TO COMPLETE] | |

## 4. Racks, cabinets and rack accessories

The 19-inch rack standard (EIA-310) is the basis of all enterprise equipment. Its dimensions are reliable:

| Dimension | Value | Status |
|---|---|---|
| Width of the front panel of a device | 482.6 mm (19 inches) | S [1] |
| Opening between the posts | 450.85 mm (17.75 inches) | S [1] |
| Height of one unit, 1U | 44.45 mm (1.75 inch) | S [1] |
| Front height of an n-U device | 44.45 x n, minus 0.794 mm (43.66 mm for 1U) | S [1] |
| Horizontal spacing of the mounting holes | 465.14 mm | S [10] |
| Common server rack | 42U, 600 mm wide, ≈ 914 mm deep | S [1] |
| Example APC NetShelter SX AR3100, 42U | 600 x 1070 x 1991 | S [1] |

| Equipment | Role | Real dimensions | Status |
|---|---|---|---|
| 42U server rack (closed cabinet) | Closed cabinet for servers | 600 x 1070 x 1991 | S [1] |
| Wide network rack (e.g. Rittal TS 8) | Wider cabinet for patching | 800 x 600 x 2000 | E |
| 47U or 48U tall rack | Tall datacenter rack | [TO COMPLETE] | |
| 6U, 9U, 12U wall-mount cabinet | Small rack fixed to the wall | ≈ 600 x 450 x 500 for 9U | M |
| 2-post rack | Open frame for network equipment | [TO COMPLETE] | |
| 4-post open frame | Open frame for servers | [TO COMPLETE] | |
| 10-inch rack | Mini rack for home or small office, same unit height as the 19-inch | [TO COMPLETE] | |
| Open Rack (OCP, 21 inches) | Standard of very large datacenters, 600 wide on the outside, 547 module width | 600 x 1068 x 2286 for version 3 | S [1] for 547, E for the rest |
| 24-port copper patch panel | Termination of the cables in the rack, 1U | 482.6 x ≈ 100 x 44.45 | M |
| 48-port patch panel | Same, high-density 1U or 2U | [TO COMPLETE] | |
| Fibre enclosure | Termination of the fibres in the rack, 1U | [TO COMPLETE] | |
| Horizontal cable manager | Guides the cords between two devices, 1U or 2U | 482.6 x ≈ 80 x 44.45 | M |
| Vertical cable manager | Guides the cords along the height of the rack | [TO COMPLETE] | |
| Blanking panel | Closes the empty Us for the airflow | 482.6 x ≈ 10 x 44.45 per U | M |
| Shelf, rails | Equipment supports | [TO COMPLETE] | |

## 5. Enterprise network equipment

All rack-mountable devices have a 482.6 mm front panel and a body about 440 mm wide. Their height is a multiple of 44.45 mm. Only the depth varies.

| Equipment | Role | Real dimensions | Status |
|---|---|---|---|
| 24-port access switch (Cisco Catalyst 9300-24T) | Connects the computers of one floor, 350 W power supply | 445 x 409 x 44, 1U (449 deep with the power supply) | S [7] |
| 48-port access switch (Cisco Catalyst 9300-48T) | Same, denser, 7.45 kg, 350 W power supply | 445 x 449 x 44, 1U | S [7] |
| 48-port PoE switch (Cisco Catalyst 9300-48P) | Powers phones, access points and cameras over the cable, 1100 W power supply | 445 x 449 x 44, 1U | S [7] |
| High-power PoE switch (Cisco Catalyst 9300-48UXM) | 48 ports, 1900 W power supply | 445 x 485 x 44, 1U | S [7] |
| Distribution switch | Aggregates the access switches | 1U or 2U | M |
| Core switch | Backbone of the network | [TO COMPLETE] | |
| Modular chassis switch | Chassis with line cards and supervisors | [TO COMPLETE] | |
| Line card, supervisor | Modules of the chassis | [TO COMPLETE] | |
| Top-of-rack switch (e.g. Juniper QFX5120-48Y) | Switch at the top of each server rack, ≈ 250 W | 440.9 x 520.2 x 43.7, 1U | E |
| Spine switch, leaf switch | Two-level datacenter architecture | 1U or 2U | M |
| Enterprise router | Connects the site to the internet and to other sites | 1U or 2U | M |
| Core router | Very high speed routing | [TO COMPLETE] | |
| Firewall (Palo Alto PA-3410 to PA-3440) | Filters the traffic between zones, 133 to 155 W on average, 7 kg | 434.9 x 361.4 x 43.2, 1U | S [8] |
| Firewall (Fortinet FortiGate 600F) | Same, ≈ 169 W on average | 432 x 380 x 44.45, 1U | E |
| Load balancer | Spreads the requests between servers | 1U or 2U | M |
| VPN gateway | Encrypted tunnels between sites | [TO COMPLETE] | |
| IDS, IPS | Intrusion detection and blocking | [TO COMPLETE] | |
| Network TAP | Copies the traffic for analysis | [TO COMPLETE] | |
| Wireless controller | Manages the access points of the site | [TO COMPLETE] | |
| Ceiling access point (Cisco Catalyst 9130AXI) | Enterprise Wi-Fi, PoE, 25.5 to 30.5 W | 226 x 226 x 48 | E |
| Outdoor access point | Outdoor Wi-Fi, waterproof | [TO COMPLETE] | |
| Console server (Opengear CM8100) | Backup serial access to the equipment, 16 to 48 ports, under 15 W | 440 x 235 x 44, 1U | S [9] |
| KVM switch | Shares screen, keyboard and mouse between servers | 1U | M |
| Rack console | Retractable screen and keyboard | 1U | M |

## 6. Servers and storage

| Equipment | Role | Real dimensions | Status |
|---|---|---|---|
| 1U rack server (e.g. Dell PowerEdge R650) | Flat server, general use | 434 to 482 x 758 to 787 x 42.8 (sources differ) | E |
| 2U rack server | More disks and cards | 440 x ≈ 700 to 800 x 87 | M |
| 4U rack server | Computing, graphics cards, storage | 440 x ≈ 700 to 900 x 175 | M |
| Tower server | Server for a small site without a rack | [TO COMPLETE] | |
| Blade enclosure (e.g. HPE BladeSystem c7000) | Groups several blade servers | 447 x 813 x 442, 10U | E |
| Blade server | Server module of the enclosure | [TO COMPLETE] | |
| Disk shelf, JBOD (e.g. Dell PowerVault ME484) | Storage extension, 84 disks, 64 kg empty | 483 x 981 x 222, 5U | E |
| SAN array | Shared block storage | [TO COMPLETE] | |
| Rack NAS (e.g. Synology RS2423+) | Shared file storage, 12 disks, ≈ 120 W | 482 x 552 x 88, 2U | E |
| Tape library | Backup and archiving on tape | [TO COMPLETE] | |
| Backup appliance | Dedicated backup | [TO COMPLETE] | |

## 7. Power

| Equipment | Role | Real dimensions | Status |
|---|---|---|---|
| 0U vertical PDU | Power strip along the height of the rack | ≈ 55 x 45 x 1800 | M |
| 1U horizontal PDU | Power strip on the front | 482.6 x ≈ 45 x 44.45 | M |
| Basic, metered, switched PDU | Variants: simple, with metering, with per-outlet switching | Same formats | M |
| 3 kVA rack UPS (APC Smart-UPS SRT3000RMXLT) | Electrical backup in the rack, 2700 W, 31.3 kg | 432 x 635 x 85, 2U | E |
| 5 to 6 kVA rack UPS (Eaton 9SX) | Electrical backup, 4.5 to 5.4 kW | 440 x 684 x 130, 3U | E |
| Tower UPS | Backup for a small room | [TO COMPLETE] | |
| Room UPS | Backup for the whole room, several cabinets | [TO COMPLETE] | |
| Battery cabinet | Runtime of the UPS | [TO COMPLETE] | |
| Transfer switch (ATS and STS) | Switches between two power sources | 1U for a rack ATS | M |
| Electrical panel | Distribution and protection of the circuits | [TO COMPLETE] | |
| Busbar | Electrical distribution above the racks | [TO COMPLETE] | |
| Generator | Long-duration backup | [TO COMPLETE] | |

## 8. Cooling and room infrastructure

| Equipment | Role | Real dimensions | Status |
|---|---|---|---|
| Air conditioning cabinet, CRAC (Vertiv Liebert PX023) | Cools the room, 23 kW of cooling, 304 kg | 876 x 879 x 1970 | E |
| Chilled water air conditioning cabinet, CRAH | Same, connected to a chilled water network | [TO COMPLETE] | |
| In-row cooling | Cooling unit placed between the racks | [TO COMPLETE] | |
| Hot or cold aisle containment | Doors and roof that separate hot and cold air | [TO COMPLETE] | |
| Raised floor tile | Technical floor, cables and air underneath | 600 x 600 x 32 to 42 in Europe, 610 x 610 in the United States | E, S [1] for 610 |
| Perforated tile | Lets cold air through in front of the racks | 600 x 600 x 38 | E |
| Fire detection and suppression | Detectors, nozzles and gas cylinders | [TO COMPLETE] | |
| Access control | Badge reader, secured door | [TO COMPLETE] | |
| CCTV camera | Room surveillance | [TO COMPLETE] | |
| Temperature and humidity sensor | Room monitoring | [TO COMPLETE] | |

## 9. Cables

### Copper

| Cable | Speed and distance | Status |
|---|---|---|
| Cat 5e | 1 Gbit/s over 100 m, 100 MHz | E |
| Cat 6 | 1 Gbit/s over 100 m, 10 Gbit/s over about 55 m, 250 MHz | E |
| Cat 6A | 10 Gbit/s over 100 m, 500 MHz | E |
| Cat 7 | 600 MHz according to some sources, 1 GHz for Cat 7A. Conflicting values | E |
| Cat 8 | 25 and 40 Gbit/s over 30 m, 2 GHz | E |
| Shielding | Cat 5e, 6, 6A and 8.1 shielded: usually F/UTP. Cat 7, 7A and 8.2: S/FTP | S [2] |
| Outer diameter of the cables | [TO COMPLETE] | |
| Straight-through cable, crossover cable | T568A and T568B pinouts. [TO COMPLETE] | |
| RG-6 coaxial | 75 ohms, diameter 6.8 to 7 mm depending on the manufacturer | E |
| RG-59 coaxial | 75 ohms, diameter 6.1 to 6.15 mm | E |
| Telephone cable | [TO COMPLETE] | |
| Console cable | RJ45 rollover, RJ45 to DB9, USB. [TO COMPLETE] | |
| DAC twinax cable | Short link between SFP+ or QSFP ports. [TO COMPLETE] | |
| Power cord | C13/C14 and C19/C20. [TO COMPLETE] | |

### Optical fibre

| Fibre | Characteristics | Status |
|---|---|---|
| OM1 | Multimode, core 62.5 µm, orange jacket, 10 Gbit/s over about 33 m | E |
| OM2 | Multimode, core 50 µm, orange jacket, 10 Gbit/s over about 82 m | E |
| OM3 | Multimode, core 50 µm, aqua jacket, 10 Gbit/s over 300 m, 40 and 100 Gbit/s over 100 m | E |
| OM4 | Multimode, core 50 µm, aqua jacket (violet according to one source), 10 Gbit/s over 550 m, 40 and 100 Gbit/s over 150 m | E |
| OM5 | Multimode, core 50 µm, lime green jacket | E |
| OS1, OS2 | Single-mode, long distance. Yellow jacket by usage, not confirmed by a source | M |
| Fibre patch cord | Short cord between two devices. [TO COMPLETE] | |
| MPO/MTP trunk cable | 12 or 24 fibres per connector | E |
| Breakout cable | One multifiber connector to several single connectors. [TO COMPLETE] | |
| Outdoor fibre | Aerial, buried, armored. Number of fibres and diameters: [TO COMPLETE] | |

## 10. Connectors and modules

| Item | Characteristics | Status |
|---|---|---|
| RJ45 (8P8C) | Copper Ethernet connector. Dimensions: [TO COMPLETE] | |
| RJ11 | Telephone connector. [TO COMPLETE] | |
| LC | Fibre, 1.25 mm ferrule, small form factor | E |
| SC | Fibre, 2.5 mm ferrule, push-pull | E |
| ST | Fibre, 2.5 mm ferrule, bayonet | E |
| FC | Fibre, 2.5 mm ferrule, screw-on | E |
| MPO/MTP | Fibre, 4 to 24 fibres per connector | S [5] |
| APC polish | Angle of 8 degrees in general, green body or sleeve | S [5] |
| UPC polish | Blue by usage, not confirmed by a source read | E |
| F connector, BNC | Coaxial. [TO COMPLETE] | |
| Keystone jack | Snap-in jack for a panel or faceplate. [TO COMPLETE] | |
| SFP module | 1 Gbit/s, ≈ 45 x 13.7 x 8.6 | E |
| SFP+ module | 10 Gbit/s, same format as the SFP | E |
| SFP28 module | 25 Gbit/s, same format as the SFP | E |
| QSFP+ module | 40 Gbit/s, ≈ 52.4 x 18.35 x 8.5 | E |
| QSFP28 module | 100 Gbit/s, same format as the QSFP+ | E |
| QSFP-DD module | 400 Gbit/s and more, 8 lanes | E |
| GBIC module | Old 1 Gbit/s format. [TO COMPLETE] | |
| Module types | SR, LR, ER, BiDi, copper. [TO COMPLETE] | |

## 11. Cable routing

| Item | Role | Real dimensions | Status |
|---|---|---|---|
| Wire mesh cable tray | Supports the cables on the ceiling or under the floor | Height 50, width 150 to 600 (one manufacturer) | E |
| Perforated cable tray | Same, in sheet metal | Widths from 50 to 1000 | E |
| Cable ladder | Supports large bundles | Width 200 to 600, side rail 100 (one manufacturer) | E |
| Cable trunking | Closed surface-mounted path, wall or ceiling | [TO COMPLETE] | |
| Skirting trunking, molding | Discreet path at the bottom of a wall | [TO COMPLETE] | |
| Conduit, ribbed conduit | Protects the cables in walls and slabs | [TO COMPLETE] | |
| Underground duct | Protects the cables underground, PVC or HDPE | [TO COMPLETE] | |
| D-ring | Guides the cords in the rack | [TO COMPLETE] | |
| Service pole | Brings the cables down from the ceiling to the desk | [TO COMPLETE] | |
| Floor box | Sockets set into the floor | [TO COMPLETE] | |

## 12. Termination and connection

| Item | Role | Status |
|---|---|---|
| Splice closure | Protects the splices between two fibre cables | [TO COMPLETE] |
| Splice tray | Stores the splices in a closure or an enclosure | [TO COMPLETE] |
| Optical splitter | Shares one fibre between several subscribers, from 1:2 to 1:64 | [TO COMPLETE] |
| Attenuator | Reduces the signal power | [TO COMPLETE] |
| Junction box | Junction of copper cables | [TO COMPLETE] |

## 13. Tools

Potential game objects, without a dimension at this stage.

| Tool | Use |
|---|---|
| Crimping tool | Fits the RJ45 connectors |
| Punch-down tool | Connects the wires on a panel or a socket |
| Cable stripper | Removes the jacket |
| Cable tester | Checks continuity and pinout |
| Cable certifier | Validates a link against its category |
| Tone generator | Finds a cable in a bundle |
| Fusion splicer | Welds two fibres |
| Fibre cleaver | Cuts the fibre cleanly before splicing |
| OTDR | Locates a fault on a fibre |
| Optical power meter | Measures the received power |
| Visual fault locator | Finds a fibre or a break with red light |

## 14. Operator network: fibre to the home (FTTH)

Chain, from the core to the subscriber: optical connection node (NRO), then sharing point (PM, shared access point), then branching point (PBO), then optical outlet and ONT.

| Equipment | Role | Real dimensions | Status |
|---|---|---|---|
| Optical connection node (NRO) | Operator's building from which the fibres start | [TO COMPLETE] | |
| OLT (optical line terminal) | Active equipment of the NRO that serves the subscribers | [TO COMPLETE] | |
| ODF (optical distribution frame) | Patching of the fibres at the NRO | [TO COMPLETE] | |
| Street cabinet (sharing point, PM, e.g. Grolleau PM360 2x28U) | Distributes the operators' fibres to a neighborhood, passive | 1600 x 350 x 1625 | E |
| Street cabinet (Idea Optical iBER-1635 2x34U) | Same | 1886 x 368 x 1630 | E |
| Optical branching point (PBO) | Last closure before the home: facade, pole, pull box or landing | [TO COMPLETE] | |
| Splice protection closure (BPE) | Waterproof junction between cables | [TO COMPLETE] | |
| Riser, floor box | Vertical distribution in a building | [TO COMPLETE] | |
| PON splitter (GPON, XGS-PON) | Shares one fibre between several subscribers | [TO COMPLETE] | |
| Optical outlet (PTO) | See section 2 | 100 x 80 x 27.5 | E |

## 15. Operator network: copper and cable

The coaxial cable network (headend, optical node, amplifiers) is out of the scope of the lot plan: these lines are kept for reference.

| Equipment | Role | Status |
|---|---|---|
| Telephone exchange (NRA) | Building from which the copper lines start | [TO COMPLETE] |
| Main distribution frame | Patching of the copper pairs at the exchange | [TO COMPLETE] |
| DSLAM | Active equipment of ADSL and VDSL | [TO COMPLETE] |
| Cross-connect cabinet (street cabinet) | Distribution of the pairs in a neighborhood | [TO COMPLETE] |
| Concentration point (PC, copper distribution point) | Last copper closure before the subscriber, on a facade or a pole; the distribution cable shared by several subscribers arrives upstream | E |
| Headend, CMTS | Active equipment of the cable network | [TO COMPLETE] |
| Optical node, amplifier, tap | Elements of the coaxial network | [TO COMPLETE] |

## 16. Civil works and aerial network

| Equipment | Role | Real dimensions | Status |
|---|---|---|---|
| Pull box L0T | Sidewalk manhole to pull and connect the cables | ≈ 500 x 350 x 190 (diagram reading imprecise) | S [4] |
| Pull box L1T | Same, 24 duct entries, 30 kg | 650 x 500 x 562, inside 520 x 380 | S [4] |
| Pull box L2T | Long chamber, 36 duct entries, 50 kg | 1290 x 510 x 562, inside 1160 x 380 | S [4] |
| L3T, K1C, K2C chambers | Concrete chambers, larger | [TO COMPLETE] | |
| Manhole cover | Lid of a chamber | [TO COMPLETE] | |
| Duct, trench | Underground path of the cables | [TO COMPLETE] | |
| Telecom pole (wood, metal, composite) | Support of the aerial network | [TO COMPLETE] | |
| Shared utility pole | Common support for electricity and telecom | [TO COMPLETE] | |
| Aerial cable | Cable stretched between poles | [TO COMPLETE] | |
| Anchor clamp, crossarm | Fixing of the cables on the pole | [TO COMPLETE] | |
| Pole-mounted closure | PBO or splice closure at height | [TO COMPLETE] | |
| Cable slack loop | Reserve of cable coiled on the pole | [TO COMPLETE] | |
| Active street cabinet | Cabinet with powered equipment | [TO COMPLETE] | |
| Shelter, prefabricated building | Technical building set on site | [TO COMPLETE] | |
| Containerized datacenter | Complete room in a container | [TO COMPLETE] | |

## 17. Mobile network

| Equipment | Role | Real dimensions | Status |
|---|---|---|---|
| Monopole tower | Antenna support | Height 20 to 45 m (commercial range) | E |
| Lattice tower | Antenna support, several operators | Up to 100 m (commercial range) | E |
| 4G/5G panel antenna | Transmits to a sector | [TO COMPLETE] | |
| Massive MIMO active antenna | 5G antenna with built-in radio | [TO COMPLETE] | |
| Remote radio unit (RRU or RRH) | Radio moved close to the antenna | [TO COMPLETE] | |
| Baseband unit (BBU) | Signal processing at the foot of the site | [TO COMPLETE] | |
| Microwave link | Point-to-point radio link with a dish | [TO COMPLETE] | |
| Small cell | Small proximity antenna | [TO COMPLETE] | |
| Radio site cabinet | Power and equipment at the foot of the tower | [TO COMPLETE] | |

## 18. Long distance, satellite and interconnection

| Equipment | Role | Status |
|---|---|---|
| VSAT dish | Professional satellite link | [TO COMPLETE] |
| Ground station | Gateway between satellites and the terrestrial network | [TO COMPLETE] |
| Submarine cable, landing station | Intercontinental links | [TO COMPLETE] |
| Optical repeater, EDFA amplifier | Regenerates the signal over a long distance | [TO COMPLETE] |
| DWDM multiplexer, ROADM | Carries several wavelengths over one fibre | [TO COMPLETE] |
| Internet exchange point (IXP) | Place where networks interconnect | [TO COMPLETE] |
| Meet-me room | Patching room between operators | [TO COMPLETE] |
| Point of presence (PoP) | An operator's site in a city | [TO COMPLETE] |
| BNG, BRAS | Authenticates and aggregates subscribers | [TO COMPLETE] |
| CGNAT | Shares IPv4 addresses between subscribers | [TO COMPLETE] |

## Reference values useful for the game

### Power over Ethernet (PoE)

Useful for the mod's power system. Values read on Wikipedia [6], unless stated otherwise.

| Standard | Power delivered by the port | Power available at the device | Status |
|---|---|---|---|
| 802.3af, type 1 | 15.4 W | 12.95 W | S [6] |
| 802.3at, type 2 | 30 W | 25.5 W | S [6] |
| 802.3bt, type 3 | 60 W | 51 W | S [6] for 51 W, E for 60 W |
| 802.3bt, type 4 | 90 W | 71.3 W | S [6] |

### Ethernet over copper

| Standard | Speed | Cable and distance | Status |
|---|---|---|---|
| 10BASE-T | 10 Mbit/s | Cat 3 or better, 100 m | E |
| 100BASE-TX | 100 Mbit/s | Cat 5 or better, 100 m | E |
| 1000BASE-T | 1 Gbit/s | Cat 5 or better, 100 m | E |
| 10GBASE-T | 10 Gbit/s | 55 m on Cat 6, 100 m on Cat 6A | E |
| 25GBASE-T, 40GBASE-T | 25 and 40 Gbit/s | Cat 8, 30 m | E |

## Still to verify

- The lines at status E and M, only when the equipment concerned is modelled (see "How to read this document").
- The dimensions of the copper concentration point.
- The order of the dimensions (width, depth, height) for the NAS, the UPS and the Starlink Mini router.
- The values that conflict between sources: OM4 jacket colour, Cat 7 frequency, coaxial diameters.
- The dimensions of everything outdoors: poles, PBO, mobile antennas, concrete chambers.
- The power draw of switches and servers depends on the configuration: the values quoted are power supply ratings or manufacturer averages, not guarantees.
- In sections 5 to 8, the equipment without a quoted model (core switch, chassis, routers, PDU, batteries, containment) was not researched.

Documents spotted but not read, to be consulted first before lots 4 and 5 to complete the operator network: the Acome CREDO 2018 guide on access network components, the ARCEP specification compendium for less dense areas (version of 2026-06-29) and the Nexans "central office" catalog 2024.

## Sources

Pages read:

1. Wikipedia, "19-inch rack": https://en.wikipedia.org/wiki/19-inch_rack
2. Wikipedia, "Twisted pair": https://en.wikipedia.org/wiki/Twisted_pair
3. Starlink, V5 specification sheet: https://starlink.com/public-files/specification_sheet_starlink_V5.pdf
4. Groupe Cahors, dry networks catalog 2020, pages 4 to 6: https://www.groupe-cahors.com/sites/default/files/cahors-reseaux-secs-2020.pdf
5. Wikipedia, "Optical fiber connector": https://en.wikipedia.org/wiki/Optical_fiber_connector
6. Wikipedia, "Power over Ethernet": https://en.wikipedia.org/wiki/Power_over_Ethernet
7. Cisco, Catalyst 9300 datasheet: https://www.cisco.com/c/en/us/products/collateral/switches/catalyst-9300-series-switches/nb-06-cat9300-ser-data-sheet-cte-en.pdf
8. Palo Alto Networks, PA-3400 physical and electrical specifications: https://docs.paloaltonetworks.com/hardware/pa-3400-hardware-reference/pa-3400-series-specifications
9. Opengear, CM8100 datasheet: https://resources.opengear.com/cm/datasheets/cm8100/
10. Wikipedia, "Rack unit": https://en.wikipedia.org/wiki/Rack_unit

Search results, pages not opened (status E):

- TP-Link Archer AX55: https://www.tp-link.com/baltic/products/details/archer-ax55.html
- Netgear GS108: https://www.netgear.com/uk/business/wired/switches/unmanaged/gs108/
- Ubiquiti U6 Lite: https://cdn.panacompu.com/doc/pd/u6-lite-ds.pdf
- Netgear Nighthawk M6: https://kb.netgear.com/000065402/What-are-the-specifications-of-NETGEAR-Nighthawk-mobile-hotspots
- Livebox 6: https://www.lafibreoptique.fr/offre-fibre/orange/livebox-6
- Synology DS223: https://www.synology.com/en-eu/products/DS223
- Synology DS423: https://www.synology.com/en-eu/products/DS423
- APC BX700U-GR: https://gho.ie/product/details/apc/power-components-%26-protection/bx700u-gr/0280F011
- Legrand, communication enclosure 4 132 10: https://assets.legrand.com/pim/NP-FT-GT/S000126011FR-00.pdf
- Legrand, Mosaic RJ45 socket 076562: https://www.domomat.com/16482-legrand-076562-prise-rj45-lcs-prog-mosaic-blanc-categorie-6-ftp-1-module-legrand-076562.html
- Acome PTO2803: https://www.batiproduits.com/amp/fiche/produits/chambres-de-tirage-p381319340.html
- Fracarro, GPON ONT: https://fracarro.com/wp-content/uploads/product-downloadable-contents/datasheet/287794_ONTG-8GP-M_fr_ds.pdf
- Grolleau, PM360 2x28U cabinet: https://lafibre.info/images/pon/202105_grolleau_armoire_ftth_pm360_2x28u.pdf
- Idea Optical, iBER-1635 cabinet: https://lafibre.info/images/dsp/202411_idea-optical_iber_1635_outdoor_g2_2x34u.pdf
- Mobile phone tower types: https://conseils.hellopro.fr/quels-sont-les-types-de-pylones-de-telephonie-mobile-3984.html
- Ethernet cable categories: https://www.ampcom.com/blogs/industry-information/ethernet-cable-categories-cat5e-to-cat8-explained
- OM1 to OM5 multimode fibers: https://network-switch.com/blogs/networking/multimode-fiber-types-2026-guide
- Fiber connectors: https://www.layer23-switch.com/blog/fiber-optic-connector-types.html
- SFP and QSFP modules: https://www.router-switch.com/faq/sfp-sfp-sfp28-qsfp-qsfp28-guide.html
- Module dimensions: https://internet.watch.impress.co.jp/docs/column/nettech/1269068.html
- PoE standards: https://community.fs.com/article/understanding-poe-standards-and-poe-wattage.html
- RG-6 and RG-59 coaxials: https://media.iewc.com/SpecificationSheets/Southwire_coaxial_SS.pdf
- Cable trays: https://meandersquare.co.za/Tue-14-Sep-2021-11835.html
- Copper Ethernet standards: https://kb.wisc.edu/ns/7829
- Juniper QFX5120: https://www.juniper.net/us/en/products/switches/qfx-series/qfx5120-data-center-switches/specs.html
- Fortinet FortiGate 600F: https://www.shi.com/product/44900362/Fortinet-FortiGate-600F
- Cisco Catalyst 9130AXI: https://netmode.com/products/cisco-catalyst-c9130axi-ewc-b-series-with-embedded-wireless-controller-mobility-express-new
- Dell PowerEdge R650: https://www.racksolutions.com/news/blog/dell-poweredge-r650-specs-and-rack-compatibility/
- Dell PowerVault ME484: https://www.stikc.com/storage/dell-powervault-san-das/powervault-me5084.html
- Synology RS2423+: https://nasshops.net/home/205-274-synology-rs2423.html
- HPE BladeSystem c7000: https://www.manualslib.com/manual/642057/Hp-C-Class.html?page=43
- APC Smart-UPS SRT3000RMXLT: https://cdn.blueally.com/apcguard/datasheets/SRT3000RMXLT_datasheet.pdf
- Eaton 9SX: https://eaton.com/content/dam/eaton/products/backup-power-ups-surge-it-power-distribution/backup-power-ups/eaton-9sx-ups/eaton-9sx-5-11kva-ups-datasheet-en-us-east-asia.pdf
- Vertiv Liebert PX023: https://www.vertiv.com/en-us/products-catalog/thermal-management/room-cooling/px023-23kw/
- Rittal TS 8: https://www.rittal.com/pdf-creator/variant/com-en/8806750
- Raised floor tiles: https://source.thenbs.com/en-us/gb/product/beh600pbt-heavy-grade-porcelain-edge-banded-panel/deWJdesYthhtqkzvTneCzd/h5xPg6gCNF82zv4FWFMw64
- Open Rack version 3: https://www.opencompute.org/products/440/rittal-open-rack-v3-orv3
- Concentration point, OVHcloud: https://docs.ovhcloud.com/fr/guides/web-cloud/internet/internet-access/la-desserte-interne
- Concentration point, Arcep opinion DEX150505: https://arcep.fr/fileadmin/reprise/dossiers/degroup/avis-DEX150505-VDSL2_au_PC-050515.pdf
