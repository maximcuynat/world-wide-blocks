# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projet

NetCraft est un mod Minecraft (NeoForge) de simulation de réseau informatique réaliste, « un Packet Tracer en blocs ». Lots faits : 0 (tour PC, PC portable), 1a (cordon Ethernet, ports, indicateur d'état, moteur réseau), 1b-1 (box internet, adresses MAC, commutation) et le module des messages.

À lire en début de session :
- `docs/plan-lots.md` : plan par lots, état de chaque lot, décisions à trancher, procédures de test en jeu ;
- `docs/equipements-reseau.md` : inventaire des équipements et dimensions réelles ;
- `docs/messages.md` : audit et règles du module des messages ;
- `tasks/todo.md` (suivi, idées non validées) et `tasks/lessons.md` (erreurs à ne pas refaire).

Code, commentaires, messages de commit et textes en français ; identifiants (classes, registres, ressources) en anglais. Les deux langues `fr_fr.json` et `en_us.json` sont maintenues (même jeu de clés, vérifié par `LangFilesTest`). Exception : tout ce qui peut finir dans le log (lignes de log, messages d'exception) est en anglais, comme le reste des logs de Minecraft.

## Versions

- Minecraft 26.3, NeoForge 26.3.0.103, plugin `net.neoforged.moddev` 2.0.148, Gradle 9.2.1, Java 25 (téléchargé par la toolchain).
- Versions et identité du mod dans `gradle.properties`, injectées dans `neoforge.mods.toml` : rien en dur dans le template.
- Version du mod : SemVer en 0.x, `mod_version` dans `gradle.properties` seule source de vérité (`0.1.0-alpha.1` ; canal déduit du suffixe : alpha, beta, rien = release). Jar `netcraft-<minecraft>-<version>.jar`. Licence LGPL-3.0-only (`COPYING.LESSER`, `COPYING`).
- Les API ont beaucoup bougé depuis la 1.21 (rendu, interface, noms de constantes). Vérifier chaque nom de classe, de méthode, d'enum et chaque package d'import dans les sources : Minecraft dans `build/moddev/artifacts/minecraft-patched-*-sources.jar`, NeoForge dans le jar `-sources` de maven.neoforged.net.

## Commandes

```bash
./gradlew build test   # compile, puis tests JUnit du moteur (Java pur, sans lancer le jeu)
./gradlew runClient    # client de dev, monde de test « Netcraft » (créatif, superflat)
./gradlew runServer    # serveur dédié sans interface
./gradlew runGameTestServer  # tests en jeu automatiques (src/gametest), monde jetable run-gametest/
```

Outillage du dépôt (détails dans `CONTRIBUTING.md`, réglages GitHub dans `docs/securite-depot.md`) :

```bash
scripts/install-hooks.sh                   # hooks versionnés de .githooks/ : convention des commits, gitleaks, build et tests avant push
scripts/release.sh --dry-run <version>     # release locale : essai à blanc, puis préparation et --publish (lancés par Max)
git-cliff -o CHANGELOG.md                  # changelog généré depuis les commits (cliff.toml), jamais écrit à la main
```

## Façon de travailler

- Toujours répondre à Max en français.
- Branche `developement` uniquement. Rien vers `main` sans l'accord de Max.
- Petits commits, un par étape logique.
- Messages de commit en Conventional Commits, vérifiés par le hook `commit-msg` ; les types `feat`, `fix`, `refactor` et `perf` alimentent le changelog des joueurs.
- `main` ne reçoit que les versions publiées, par une pull request de release depuis `developement` fusionnée avec un commit de fusion (`scripts/release.sh`). Ne jamais publier de release ni rendre le dépôt public : c'est Max qui le fait.
- Le bac à sable ne lit ni `~/.gitconfig` ni `~/.ssh` : préfixer les commandes git par `GIT_CONFIG_GLOBAL=/dev/null XDG_CONFIG_HOME=$TMPDIR/xdg` (sinon gitleaks voit l'avertissement de git et bloque le commit) et commiter avec l'identité masquée passée explicitement (`git -c user.name=maximcuynat -c user.email=93578199+maximcuynat@users.noreply.github.com commit …`), jamais une identité déduite du poste. Ces commits ne sont pas signés (voir `docs/securite-depot.md`, section 3.8).
- Le bac à sable de Claude Code ne peut pas lancer Gradle : c'est Max qui lance les builds et les essais en jeu (préfixe `!`) et rapporte le résultat. Ne jamais annoncer un build ou un essai comme réussi sans ce retour.

## Architecture du moteur réseau

- `network/` : `NetworkGraph` (appareils, câbles, index par port et par chunk), les records `DeviceId`, `PortRef`, `Link`, `NetworkNode`, `ChunkKey`, et `sync/PlayerLinkView`. Tout cela est en Java pur, testé dans `src/test`.
- `NetworkSavedData` sauvegarde le graphe dans le stockage global du serveur (`data/netcraft/network.dat`), indépendamment des chunks chargés.
- `NetworkManager.of(server)` est une façade sans état sur ces données ; `NetworkDevices` fait le lien entre le monde (niveaux, positions, blocs) et le moteur.
- `block/NetworkDeviceBlock` : base des appareils (nombre de ports fixe, enregistrement à la pose, retrait des câbles à la casse, refus des pistons).
- `item/EthernetCableItem` : branchement, débranchement, bout tenu en main.
- `network/sync/` : paquets vers le client. `client/` : cache en lecture seule, panneau d'état, rendu des câbles.

Règles à ne jamais casser :
- Le serveur décide, le client affiche. Aucune logique réseau côté client, et aucune classe de `client/` chargée sur un serveur dédié.
- Aucun état statique : les données vivent dans le serveur en cours (stockage global, attachement par joueur) ou dans l'instance client.
- La logique réseau reste en Java pur et testable, sans classe Minecraft.
- Toute modification du réseau passe par `NetworkManager`, qui marque la sauvegarde et prévient les clients.
- Un joueur ne reçoit que les câbles dont une extrémité est dans un chunk que son client a reçu (`PlayerLinkView`).
- Vrais échanges de protocoles : de vraies trames Ethernet (puis DHCP, ARP, ICMP) circulent dans le moteur. Elles sont livrées instantanément par une file d'attente que le serveur traite à chaque tick, sans temps de trajet bloc par bloc, et le nombre de trames traitées par tick est limité.

## Messages aux joueurs

Tout message passe par le module des messages (`message/`, règles complètes dans `docs/messages.md`) :

- Côté serveur, `MessageCenter.of(server)` : `gesture(joueur, message)` pour la réponse à un geste, `event(clé du problème, …)` pour un événement réseau. L'appelant donne un niveau (`MessageLevel`), une clé de langue et des valeurs (`MessageArg`) ; le canal découle de l'usage et du niveau (`MessageRouting`), jamais de l'appelant.
- Le serveur n'envoie jamais de texte rédigé ni de mise en forme : clé, valeurs et niveau seulement (`MessagePayload`). Le client décide seul de l'apparence (`client/ClientMessages`, `client/PortStatusLayer`), pour qu'une future interface HUD remplace le dessin sans toucher au reste.
- Un même problème n'est signalé qu'une fois par minute, quel que soit le nombre d'appareils qui le voient ; la limite d'un réseau est oubliée quand ce réseau change.
- Le contenu du panneau d'état est construit en Java pur (`StatusPanel`) : compact par défaut, détaillé quand le joueur est accroupi.
- Le détail technique va dans le log, en anglais, au niveau DEBUG (`Diagnostics`).
- Toute nouvelle clé de langue des familles vérifiées doit avoir ses valeurs d'exemple dans `LangFilesTest` (longueurs : 80 caractères pour la barre d'action, 45 pour le panneau et l'infobulle, 90 pour le chat).
- Tests : `NetworkScene.messages()` capte chaque message envoyé (canal, clé, valeurs, destinataire) ; ne pas passer par le chat d'un joueur.

## Ajouter un bloc

Un bloc touche plusieurs fichiers au même identifiant `<id>`, écrits à la main (le datagen n'est pas utilisé) :
- Java : classe dans `block/` (étendre `NetworkDeviceBlock` pour un appareil réseau), `ModBlocks`, `ModItems`, `ModCreativeTabs`. Tout nouveau registre s'ajoute dans le constructeur de `NetCraft`.
- `assets/netcraft/` : `blockstates/<id>.json`, `models/block/<id>.json`, `textures/block/<id>.png`, `items/<id>.json` (obligatoire depuis 1.21.4), clés `block.netcraft.<id>` dans les deux fichiers de langue.
- `data/` : `netcraft/loot_table/blocks/<id>.json` et l'identifiant dans `minecraft/tags/block/mineable/pickaxe.json`.

Les `.bbmodel` peuvent rester à côté des ressources : ils sont exclus du jar.
