# Listing texts

Texts that present the mod outside the repository: the GitHub repository card, and the project pages on Modrinth and CurseForge. Nothing here is published by a script: the maintainer applies or pastes these texts by hand, in the order of [`going-public.md`](going-public.md). Written on 2026-11-18; limits and category lists were read the same day (sources at the end).

Keep these texts in line with what the mod does **today**: the description says what works, the roadmap says what comes next. Update this file when a lot adds something worth saying.

## GitHub repository

### Description

249 characters (the target is under 350; the part about the internet is the roadmap, said as such):

```text
A Minecraft mod for NeoForge to learn how the internet works by building it, block by block: wire computers to an internet box, watch real Ethernet frames travel, then follow a connection from the house to the internet as the mod grows. Early alpha.
```

### Topics

GitHub rules: lowercase letters, numbers and hyphens, 50 characters at most per topic, 20 topics at most. Sixteen topics, from the most searched to the most specific:

```text
minecraft, minecraft-mod, neoforge, neoforge-mod, networking, computer-networking, network-simulation, internet, ethernet, education, educational, learn-networking, simulation, java, minecraft-26, world-wide-blocks
```

Left out on purpose: `packet-tracer` and other product names (the mod has no link with them), and protocol topics (`dhcp`, `arp`, `icmp`) until the lots that add them are released.

### Command

Run from the clone, after the rename of the repository (`going-public.md`, step 2). `gh repo edit` applies to the repository of the current directory. `--add-topic` adds topics and leaves the existing ones in place. The comma-separated list below was not run **(to verify)**: if gh refuses it, repeat `--add-topic` once per topic.

```bash
gh repo edit \
  --description "A Minecraft mod for NeoForge to learn how the internet works by building it, block by block: wire computers to an internet box, watch real Ethernet frames travel, then follow a connection from the house to the internet as the mod grows. Early alpha." \
  --add-topic minecraft,minecraft-mod,neoforge,neoforge-mod,networking,computer-networking,network-simulation,internet,ethernet,education,educational,learn-networking,simulation,java,minecraft-26,world-wide-blocks
# check
gh repo view --json description,repositoryTopics --jq '{description, topics: [.repositoryTopics[].name]}'
```

Options checked with `gh repo edit --help` (gh 2.103.3). The check command was not run: its field names (`repositoryTopics`) are **to verify** with `gh repo view --json` without a field, which lists them.

### Social preview image

The image shown when a link to the repository is shared (Slack, Discord, social networks). The maintainer adds it by hand: repository **Settings** > **General** > **Social preview** > **Edit** > **Upload an image**.

| | Value |
|---|---|
| Size | **1280 × 640 pixels** (GitHub's recommendation for the best display; at least 640 × 320) |
| Format | PNG, JPG or GIF, under 1 MB. PNG with transparency is accepted, but a plain background is safer: some platforms show transparency on white, others on black |
| Content (suggestion) | An in-game screenshot of a wired setup (computers, internet box, cables), with "World Wide Blocks" written large and one line under it, for example "Learn how the internet works, block by block". Keep text and important details away from the edges: some platforms crop the image |

## Modrinth and CurseForge project page

Draft only: no project exists yet on either platform. The address planned on both is `world-wide-blocks`, checked free on 2026-11-18 (Modrinth through its API, CurseForge by the maintainer in a browser).

### Title

```text
World Wide Blocks
```

### Summary

Short text shown in search results. The length limits of the summary on Modrinth and CurseForge were not found in their documentation: **to verify** in the creation form. This one is 141 characters:

```text
Learn how the internet works by building it: wire computers, watch real Ethernet frames travel, and soon follow a connection to the internet.
```

### Description

Markdown, accepted by both platforms. Replace the image placeholders by uploaded images (both platforms host a gallery).

```markdown
# World Wide Blocks

**Learn how the internet works by building it, block by block.**

World Wide Blocks is a NeoForge mod that turns computer networking into something you can place, wire and break. Every device is a block, every cable is a real link, and what travels through them is what travels through real networks: Ethernet frames today, then DHCP, ARP and ping.

![A computer and a laptop wired to an internet box](IMAGE: screenshot of a small home network)

## What makes it different

Most "network" mods move items or energy between machines. World Wide Blocks follows the **complete physical path of a connection**: from the computer on your desk, through the cable and the internet box of the house, out to the street cabinet, the provider's building, and across the internet to a server. Each step is built from blocks you place yourself, with real-world sizes and limits (a copper Ethernet cable stops at 100 blocks, like the 100 metres of a real one).

## What works today (alpha)

- **Computers**: PC tower and laptop, each with an Ethernet port.
- **Internet box**: 4 local ports and a line input. It learns MAC addresses and switches frames like a real switch.
- **Ethernet cable**: right-click two devices to connect them; sneak and click a device to unplug.
- **Status panel**: look at a device to see its ports, its MAC address and the addresses the box learned.
- **Loop detection**: a network loop is reported to the players nearby.
- English and French.

## What comes next

DHCP and IP addresses, a terminal with ping, desktop switches, then power, Wi-Fi, the subscriber line, the street, the provider and the internet itself. The full roadmap is on GitHub.

## Good to know

- **Alpha**: content changes between versions and a new version may break existing worlds. Back up your worlds before updating.
- Install it on the server **and** on every client.
- Works alone: no other mod required.

Source code, roadmap and bug reports: [GitHub](https://github.com/maximcuynat/world-wide-blocks).
```

### Categories and settings

| Setting | Modrinth | CurseForge |
|---|---|---|
| Project type | Mod | Mod (Minecraft > Mods) |
| Categories | `technology` (primary), `utility` | **Technology** (main category), **Education** |
| Loader | NeoForge | NeoForge |
| Game version | 26.3 | 26.3 |
| Environment | Client and server: required on both | (no such field: say it in the description) |
| License | LGPL-3.0-only | LGPL-3.0 (choose "GNU Lesser General Public License version 3", **to verify** in the list) |
| Links | Source: GitHub repository; Issues: GitHub issues | Source and Issues: same |

Modrinth has no "education" category for mods; the closest are `technology` and `utility`. CurseForge has an **Education** category, worth using: it is where people looking for "learn networking" will look. How many additional categories each platform accepts is **to verify** in the creation form.

## Sources

Read on 2026-11-18:

- GitHub topics (format and limit of 20): https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/classifying-your-repository-with-topics
- GitHub social preview (formats, 1 MB, 640 × 320 minimum, 1280 × 640 recommended, transparency): https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/customizing-your-repositorys-social-media-preview
- Modrinth mod categories: https://api.modrinth.com/v2/tag/category
- CurseForge mod categories: https://www.curseforge.com/minecraft/search?class=mc-mods
- `gh repo edit --help`, gh 2.103.3
