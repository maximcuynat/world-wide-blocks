# World Wide Blocks

**World Wide Blocks is a Minecraft mod for NeoForge to learn how the internet works by building it, block by block: a computer networking simulation where you wire real computers with real cables, watch real Ethernet frames travel between them, and follow a connection from your house all the way to the internet.**

<!-- Placeholder: hero animation (GIF or WebP, about 800 px wide, under 5 MB), shown here once recorded.
     Suggested shot: a player plugs an Ethernet cable from a PC tower into the internet box, then looks at the
     box and its status panel shows the computer's MAC address. File: docs/media/hero.gif -->

> **This is `developement`, the development branch**: its code is work in progress between two versions. The released versions, with their jar files, are on the [Releases](../../releases) page.

> **Alpha.** The mod is in early development: content changes between versions and a new version may break existing worlds. Back up your worlds before updating.

## What makes it different

Most "network" mods move items, fluids or energy between machines. World Wide Blocks simulates **how computer networks really work**, and follows the **complete physical path of a connection**:

1. the computer on your desk and its network port;
2. the cable, the wall socket and the internet box of the house;
3. the subscriber line, out to the street cabinet and the neighbourhood;
4. the internet service provider's building and its racks;
5. the links between operators, up to a server on the other side of the internet.

Every step is built from blocks you place and wire yourself, with real-world sizes and limits: a copper Ethernet cable stops at 100 blocks, like the 100 metres of a real one. What travels through the cables is what travels through real networks: Ethernet frames today, then DHCP, ARP and ping, then routing between networks. The server decides everything; the client only shows it.

The idea: a network lab you can walk through, like a "Packet Tracer made of blocks", for players who want to understand the internet and for network enthusiasts who want a faithful sandbox.

## What works today

- **Computers**: PC tower and laptop, each with one Ethernet port.
- **Internet box**: 4 local network ports and one line input; it learns MAC addresses and switches frames like a real switch.
- **Ethernet cable**: right-click two devices to connect them (100 blocks at most); sneak and click a device to unplug.
- **Status panel** when looking at a device: ports, MAC address, MAC addresses learned by the box. More details while sneaking.
- **Network loop detection**, reported to nearby players.
- English and French.

<!-- Placeholder: screenshot of a small home network (PC tower, laptop, internet box, cables), 1280 × 720 PNG.
     File: docs/media/home-network.png -->
<!-- Placeholder: screenshot of the detailed status panel of the internet box (sneaking), 1280 × 720 PNG.
     File: docs/media/status-panel.png -->

## Roadmap

| Version | Content |
|---|---|
| 0.1.0 Wired home network | PCs, Ethernet cable, internet box, DHCP, terminal and ping, desktop switches |
| 0.2.0 Power and home network | Electricity, Wi-Fi, fixed cabling, peripherals and local services |
| 0.3.0 Subscriber line | Fibre and copper line inputs of the box |
| 0.4.0 Street and neighbourhood | Aerial and underground lines, street cabinets |
| 0.5.0 ISP | Rack, ISP premises, public address |
| 0.6.0 Internet | Routing between operators, servers and domain names |
| 0.7.0 Enterprise and datacenter | Configurable switch, router and firewall, servers, power and cooling |
| 0.8.0 Long-range wireless, tools, survival | Mobile network, satellite and radio links, crafting in survival |
| 1.0.0 | Finishing: lights, textures, sounds, polished models and HUD |

Details: [`docs/project-management.md`](docs/project-management.md) (versions and milestones) and [`docs/lot-plan.md`](docs/lot-plan.md) (design of each step).

## Installation

| | Version |
|---|---|
| Minecraft | 26.3 |
| NeoForge | 26.3.0.103 or later |
| Java | 25 (bundled with the official Minecraft 26.3 launcher) |

1. Install NeoForge for Minecraft 26.3 ([neoforged.net](https://neoforged.net/)).
2. Download `worldwideblocks-26.3-<version>.jar` from the [GitHub releases](../../releases).
3. Put it in the `mods` folder of your instance and start the game.

The mod must be installed on the server and on every client. It works alone: no other mod is required.

Commands (operators): `/worldwideblocks links` lists the network links known to the server; `/wwb` is a short alias.

## Release channels

| Channel | Example version | For whom |
|---|---|---|
| alpha | `0.1.0-alpha.1` | Testing: incomplete features, worlds may become incompatible |
| beta | `0.1.0-beta.1` | Feature-complete, being stabilised |
| release | `0.1.0` | Stable release of the 0.x line |

Versions follow [Semantic Versioning](https://semver.org/). While the version starts with 0, a minor version (0.**2**.0) may break existing worlds. The published file carries the Minecraft version: `worldwideblocks-26.3-0.1.0-alpha.1.jar`. Changes for each version are listed in [`CHANGELOG.md`](CHANGELOG.md).

Versions are published on GitHub; CurseForge and Modrinth will come later.

## Building

Requirements: Git and JDK 17 or later to run Gradle. The JDK 25 needed to compile is downloaded automatically by Gradle on the first build.

```bash
git clone https://github.com/maximcuynat/world-wide-blocks.git
cd world-wide-blocks
./gradlew build
```

The jar is in `build/libs/`.

## Tests

```bash
./gradlew build test         # build and unit tests of the network engine (plain Java, no game launch)
./gradlew runGameTestServer  # automated in-game tests, in a throwaway world (run-gametest/)
./gradlew runClient          # development client
```

## Contributing, reporting a vulnerability

- Contributing: see [`CONTRIBUTING.md`](CONTRIBUTING.md) (commit convention, branches, Git hooks, tests, releases).
- Security vulnerability: do not open a public issue, see [`SECURITY.md`](SECURITY.md).

Development is AI-assisted (Claude Code, by Anthropic): the assistant's instructions are in [`CLAUDE.md`](CLAUDE.md), session notes and lessons learned in [`tasks/`](tasks/).

## License

Copyright © 2026 maximcuynat.

World Wide Blocks is free software under the [GNU LGPL v3.0 only](COPYING.LESSER) (`LGPL-3.0-only`), which supplements the [GNU GPL v3.0](COPYING). You may use it, include it in a modpack and modify it; a modified version of the mod that is distributed must stay under the same license, with its source code.

Files taken from the official NeoForge template (MDK) remain under the MIT license: see [`TEMPLATE_LICENSE.txt`](TEMPLATE_LICENSE.txt).

Minecraft is a trademark of Mojang Studios. World Wide Blocks is not affiliated with or endorsed by Mojang or Microsoft.
