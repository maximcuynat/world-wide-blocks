# NetCraft

[Français](#français) · [English](#english)

---

## Français

NetCraft est un mod Minecraft (NeoForge) de simulation de réseau informatique réaliste : un « Packet Tracer en blocs ». On pose des postes, on les câble, et de vraies trames Ethernet circulent entre eux. Viendront ensuite DHCP, ARP, ICMP, puis tout le chemin d'une connexion, du poste jusqu'à internet.

> **Version alpha.** Le mod est en début de développement : le contenu change d'une version à l'autre et une nouvelle version peut casser les mondes existants. Faites une sauvegarde avant de mettre à jour.

### Ce qui fonctionne aujourd'hui

- **Postes** : tour PC et PC portable, chacun avec un port Ethernet.
- **Box internet** : 4 ports réseau local et une arrivée de ligne ; elle apprend les adresses MAC et commute les trames comme un vrai switch.
- **Cordon Ethernet** : clic droit sur deux appareils pour les relier (100 blocs au plus, comme les 100 m d'un câble cuivre réel) ; accroupi et clic sur un appareil pour débrancher.
- **Panneau d'état** en regardant un appareil : ports, adresse MAC, MAC apprises par la box. Plus de détails en restant accroupi.
- **Détection des boucles** réseau, signalées aux joueurs proches.
- Interface en français et en anglais.

Le plan complet par lots est dans [`docs/plan-lots.md`](docs/plan-lots.md).

### Installation

| | Version |
|---|---|
| Minecraft | 26.3 |
| NeoForge | 26.3.0.103 ou plus récent |
| Java | 25 (fourni par le lanceur officiel de Minecraft 26.3) |

1. Installez NeoForge pour Minecraft 26.3 ([neoforged.net](https://neoforged.net/)).
2. Téléchargez le fichier `netcraft-26.3-<version>.jar` dans les [releases GitHub](../../releases).
3. Placez-le dans le dossier `mods` de votre instance, puis lancez le jeu.

Le mod doit être installé sur le serveur et sur chaque client.

### Canaux de version

| Canal | Exemple de version | Pour qui |
|---|---|---|
| alpha | `0.1.0-alpha.1` | Essais : fonctionnalités incomplètes, mondes parfois incompatibles |
| beta | `0.1.0-beta.1` | Fonctionnalités complètes, en cours de stabilisation |
| release | `0.1.0` | Version stable du canal 0.x |

Les versions suivent le [versionnage sémantique](https://semver.org/lang/fr/). Tant que la version commence par 0, une version mineure (0.**2**.0) peut casser les mondes existants. Le fichier publié porte la version de Minecraft : `netcraft-26.3-0.1.0-alpha.1.jar`. Les modifications de chaque version sont dans [`CHANGELOG.md`](CHANGELOG.md).

Les versions sont publiées sur GitHub ; la publication sur CurseForge et Modrinth viendra plus tard.

### Compiler

Prérequis : Git et un JDK 17 ou plus récent pour lancer Gradle. Le JDK 25 nécessaire à la compilation est téléchargé automatiquement par Gradle au premier build.

```bash
git clone https://github.com/maximcuynat/netcraft.git
cd netcraft
./gradlew build
```

Le jar se trouve dans `build/libs/`.

### Tests

```bash
./gradlew build test         # compilation et tests unitaires du moteur réseau (Java pur, sans lancer le jeu)
./gradlew runGameTestServer  # tests en jeu automatiques, dans un monde jetable (run-gametest/)
./gradlew runClient          # client de développement
```

### Contribuer, signaler une faille

- Contribuer : voir [`CONTRIBUTING.md`](CONTRIBUTING.md) (convention des commits, branches, hooks Git, tests).
- Faille de sécurité : ne pas ouvrir d'issue publique, voir [`SECURITY.md`](SECURITY.md).

Le développement est assisté par IA (Claude Code, d'Anthropic) : les consignes données à l'assistant sont dans [`CLAUDE.md`](CLAUDE.md), le suivi et les leçons dans [`tasks/`](tasks/).

### Licence

Copyright © 2026 maximcuynat.

NetCraft est un logiciel libre sous licence [GNU LGPL v3.0 seulement](COPYING.LESSER) (`LGPL-3.0-only`), qui complète la [GNU GPL v3.0](COPYING). Vous pouvez l'utiliser, l'inclure dans un modpack et le modifier ; une version modifiée du mod qui est distribuée doit rester sous la même licence, avec son code source.

Les fichiers repris du modèle officiel NeoForge (MDK) restent sous licence MIT : voir [`TEMPLATE_LICENSE.txt`](TEMPLATE_LICENSE.txt).

Minecraft est une marque de Mojang Studios. NetCraft n'est ni affilié ni approuvé par Mojang ou Microsoft.

---

## English

NetCraft is a Minecraft mod (NeoForge) that simulates realistic computer networks: a "Packet Tracer made of blocks". You place computers, wire them up, and real Ethernet frames travel between them. DHCP, ARP and ICMP come next, then the whole path of a connection, from the computer to the internet.

> **Alpha version.** The mod is in early development: content changes between versions and a new version may break existing worlds. Back up your worlds before updating.

The source code, comments and design documents are written in French; the game is available in English and French.

### What works today

- **Computers**: PC tower and laptop, each with one Ethernet port.
- **Internet box**: 4 local network ports and one line input; it learns MAC addresses and switches frames like a real switch.
- **Ethernet cable**: right-click two devices to connect them (100 blocks at most, like the 100 m of a real copper cable); sneak and click a device to unplug.
- **Status panel** when looking at a device: ports, MAC address, MAC addresses learned by the box. More details while sneaking.
- **Network loop detection**, reported to nearby players.
- English and French interface.

The full plan (in French) is in [`docs/plan-lots.md`](docs/plan-lots.md).

### Installation

| | Version |
|---|---|
| Minecraft | 26.3 |
| NeoForge | 26.3.0.103 or later |
| Java | 25 (bundled with the official Minecraft 26.3 launcher) |

1. Install NeoForge for Minecraft 26.3 ([neoforged.net](https://neoforged.net/)).
2. Download `netcraft-26.3-<version>.jar` from the [GitHub releases](../../releases).
3. Put it in the `mods` folder of your instance and start the game.

The mod must be installed on the server and on every client.

### Release channels

| Channel | Example version | For whom |
|---|---|---|
| alpha | `0.1.0-alpha.1` | Testing: incomplete features, worlds may become incompatible |
| beta | `0.1.0-beta.1` | Feature-complete, being stabilised |
| release | `0.1.0` | Stable release of the 0.x line |

Versions follow [Semantic Versioning](https://semver.org/). While the version starts with 0, a minor version (0.**2**.0) may break existing worlds. The published file carries the Minecraft version: `netcraft-26.3-0.1.0-alpha.1.jar`. Changes for each version are listed in [`CHANGELOG.md`](CHANGELOG.md) (in French).

Versions are published on GitHub; CurseForge and Modrinth will come later.

### Building

Requirements: Git and JDK 17 or later to run Gradle. The JDK 25 needed to compile is downloaded automatically by Gradle on the first build.

```bash
git clone https://github.com/maximcuynat/netcraft.git
cd netcraft
./gradlew build
```

The jar is in `build/libs/`.

### Tests

```bash
./gradlew build test         # build and unit tests of the network engine (plain Java, no game launch)
./gradlew runGameTestServer  # automated in-game tests, in a throwaway world (run-gametest/)
./gradlew runClient          # development client
```

### Contributing, reporting a vulnerability

- Contributing: see [`CONTRIBUTING.md`](CONTRIBUTING.md) (in French: commit convention, branches, Git hooks, tests).
- Security vulnerability: do not open a public issue, see [`SECURITY.md`](SECURITY.md).

Development is AI-assisted (Claude Code, by Anthropic): the assistant's instructions are in [`CLAUDE.md`](CLAUDE.md), tracking and lessons learned in [`tasks/`](tasks/).

### License

Copyright © 2026 maximcuynat.

NetCraft is free software under the [GNU LGPL v3.0 only](COPYING.LESSER) (`LGPL-3.0-only`), which supplements the [GNU GPL v3.0](COPYING). You may use it, include it in a modpack and modify it; a modified version of the mod that is distributed must stay under the same license, with its source code.

Files taken from the official NeoForge template (MDK) remain under the MIT license: see [`TEMPLATE_LICENSE.txt`](TEMPLATE_LICENSE.txt).

Minecraft is a trademark of Mojang Studios. NetCraft is not affiliated with or endorsed by Mojang or Microsoft.
