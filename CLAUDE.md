# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projet

NetCraft est un mod Minecraft (NeoForge) de simulation de réseau informatique réaliste : postes, baies, switchs, firewalls, câblage, alimentation. La vision est « un Packet Tracer en blocs ». Le projet démarre : seuls des blocs décoratifs existent pour l'instant (Tour PC, PC portable en cours). La feuille de route, les idées non validées et les faits vérifiés sur l'écosystème sont dans `tasks/todo.md`, à lire en début de session.

Le code, les commentaires, les messages de commit et les noms d'éléments de modèles sont en français. Les identifiants (classes, registres, ressources) sont en anglais.

## Versions

- Minecraft 26.3, NeoForge 26.3.0.103, plugin `net.neoforged.moddev` 2.0.148, Gradle 9.2.1 (wrapper).
- Java 25 pour la compilation, téléchargé par la toolchain Gradle. Un JDK 17 suffit pour lancer Gradle lui-même.
- Les versions et l'identité du mod (`mod_id`, `mod_version`, etc.) se changent dans `gradle.properties`. Elles sont injectées dans `src/main/templates/META-INF/neoforge.mods.toml` par la tâche `generateModMetadata` : ne pas écrire de valeurs en dur dans ce template.
- Minecraft 26.x n'est pas obfusqué et les API NeoForge ont beaucoup bougé depuis la 1.21.1 (transferts d'énergie, items, fluides et FML refondus en 21.9). Les tutoriels et exemples 1.20/1.21 sont souvent faux pour cette version : vérifier les signatures dans les sources décompilées ou la doc NeoForge à jour avant d'écrire du code d'API.

## Commandes

```bash
./gradlew build            # compile et produit build/libs/netcraft-<version>.jar
./gradlew runClient        # lance le client de dev (monde de test : « Netcraft », créatif, superflat)
./gradlew runServer        # serveur dédié, sans GUI
./gradlew runGameTestServer  # exécute les GameTests du namespace netcraft puis s'arrête
./gradlew runData          # datagen vers src/generated/resources
```

Il n'y a aucun test (ni JUnit ni GameTest) ni linter configuré pour l'instant. La vérification d'un changement passe par `./gradlew build` puis un contrôle en jeu avec `runClient`.

Dans le bac à sable de Claude Code, Gradle ne peut pas ouvrir de socket local : `./gradlew ...` échoue. Demander à l'utilisateur de lancer la commande lui-même (préfixe `!` dans le prompt) et de rapporter le résultat.

## Architecture

- `dev.netcraft.NetCraft` : point d'entrée `@Mod`. Son constructeur attache chaque `DeferredRegister` au bus d'événements du mod. Tout nouveau registre doit y être ajouté.
- `dev.netcraft.registry` : un fichier par type de registre (`ModBlocks`, `ModItems`, `ModCreativeTabs`), classes `final` à constructeur privé qui exposent des champs statiques `Deferred*`.
- `dev.netcraft.block` : une classe par bloc. Les blocs actuels étendent `HorizontalDirectionalBlock`, s'orientent face au joueur à la pose et définissent leurs `VoxelShape` une seule fois pour une orientation de référence, puis les font tourner avec `Shapes.rotateHorizontal` ou `Shapes.rotateHorizontalAxis`.

### Ajouter un bloc

Un bloc touche plusieurs fichiers qui doivent tous utiliser le même identifiant. Les ressources sont écrites à la main dans `src/main/resources` (le datagen est configuré mais non utilisé).

Java :
1. classe dans `block/`
2. `ModBlocks` : `BLOCKS.registerBlock(...)` (avec `noOcclusion()` pour un modèle qui ne remplit pas le cube)
3. `ModItems` : `ITEMS.registerSimpleBlockItem(...)`
4. `ModCreativeTabs` : `output.accept(...)` dans l'onglet `netcraft`

Ressources (`<id>` = identifiant du bloc) :
- `assets/netcraft/blockstates/<id>.json` : une variante par valeur de propriété (rotation `y` par `facing`)
- `assets/netcraft/models/block/<id>.json` et `assets/netcraft/textures/block/<id>.png`
- `assets/netcraft/items/<id>.json` : définition d'item côté client, obligatoire en plus du modèle (format introduit en 1.21.4)
- `assets/netcraft/lang/en_us.json` et `fr_fr.json` : clé `block.netcraft.<id>`, les deux langues sont maintenues
- `data/netcraft/loot_table/blocks/<id>.json` : sans elle le bloc ne lâche rien
- `data/minecraft/tags/block/mineable/pickaxe.json` : ajouter l'identifiant

Les fichiers de projet Blockbench (`.bbmodel`) peuvent rester à côté des ressources : ils sont exclus du jar par `build.gradle`.
