# NetCraft : suivi

Notes de session seulement. Les tâches sont des tickets GitHub, rangés par milestone (un lot, une version) : `scripts/dev status` et `docs/project-management.md`. Les tâches ouvertes et les idées qui étaient ici sont devenues des tickets le 2026-11-18 (`scripts/project/issues/`, créés par `scripts/dev project sync`).

## Bloquant pour la publication

- [ ] **Renommer le mod avant de rendre le dépôt public.** « Netcraft » est le nom d'une société britannique (netcraft.com) qui travaille dans le même domaine que le mod (réseau, sécurité internet) : risque de conflit de nom. Max cherche un autre nom ; le renommage (nom affiché, `mod_id`, package `dev.netcraft`, ressources, dépôt GitHub, docs) fera l'objet d'une session dédiée. Rien ne sera rendu public, ni publié sur CurseForge ou Modrinth, avant. (décidé le 2026-11-18)

## À faire dans la session de renommage

- [ ] Traduire en anglais l'existant : `docs/plan-lots.md`, `docs/messages.md`, `docs/equipements-reseau.md`, `docs/securite-depot.md`, `tasks/`, `CLAUDE.md`, le code et ses commentaires, les messages des hooks. Objectif de Max : un dépôt entièrement en anglais. Depuis la session B, tout ce qui est nouveau est déjà en anglais (commits, changelog, modèles, labels, tickets, nouveaux documents). (décidé le 2026-11-18)

## Session B : gestion de projet sur GitHub et commandes assistées (2026-11-18)

Conception validée par Max le 2026-11-18 : `developement` branche par défaut (« Closes # » ne marche que vers elle), branches `lot/<id>`, `release/v<version>` et `hotfix/<version>`, tag signé posé sur le commit de fusion de `main` par `release publish`, feuille de route 0.1.0 à 1.0.0 dans l'ordre de `plan-lots.md`, milestones de 0.1.0 et 0.2.0 seulement, reste de ce fichier transformé en tickets (idées comprises, renommage exclu), notes de 0.1.0-alpha.1 réécrites à la main (relecture et modification prévues dans `release publish`). Rien n'est créé sur GitHub par Claude : Max lance les scripts.

Un commit par sujet (en anglais), testé dans un clone jetable avec de faux `gradlew` et `gh` :

- [x] 1. `cliff.toml` en anglais, section « Breaks existing worlds » en tête, `CHANGELOG.md` régénéré. Essayé sur un historique avec tags sur les commits de fusion de `main` : un commit de lot daté d'avant une version mais fusionné après tombe bien dans la version suivante, `docs!:` a sa section. Trouvé au passage : sans `--github-repo`, les liens de comparaison du changelog restaient vides (`release.sh` de la session A) ; `scripts/dev` passe le dépôt lu dans la remote.
- [x] 2. Hook avant push : sortie de Gradle dans `build/pre-push.log`, 40 dernières lignes en cas d'échec, code dans `scripts/lib/checks.sh` (5 cas essayés).
- [x] 3. Modèles GitHub : ticket de bug, ticket d'idée (`config.yml` sans ticket vide), pull request avec « Closes # » et la case « Breaks existing worlds ».
- [x] 4. `scripts/dev` : socle, `status`, `lot start`, `lot finish`.
- [x] 5. `scripts/dev` : `release prepare`, `release publish`, `hotfix start` ; `scripts/release.sh` retiré.
- [x] 6. `scripts/dev project sync` et ses données : 16 labels, 10 milestones, 28 tickets (7 + 7 + 3 pour les lots 1b-2, 1c, 1d ; 4 tâches de ce fichier ; 7 idées).
- [x] 7. `CONTRIBUTING.md` réécrit en anglais, `docs/project-management.md`, `docs/securite-depot.md` (section 4 et tableau), phrase en tête du `README.md`.
- [x] 8. `CLAUDE.md` (langue, branches, outil, tickets), ce fichier réduit aux notes de session, renvoi dans `docs/plan-lots.md`.

Revue de la session B (2026-11-18) :

- Vérifié seulement dans des clones jetables, avec un faux `gradlew`, un faux `gh` (état en JSON, `--jq` passé au vrai `jq`), une clé de signature SSH jetable et un pseudo-terminal pour répondre aux questions : chaque commande en essai à blanc (aucun changement git, aucune écriture GitHub), sans terminal (réponse non), réponse non, échec des vérifications, réponse oui ; cas vide (aucun tag, ticket, milestone ni pull request) ; cycle complet alpha, beta (gel, `--allow-features`), release (milestone de version fermée), correctifs 0.1.1 à 0.1.3 (changement cassant refusé, report avec commit de fusion, conflit sur `CHANGELOG.md` gardé côté `developement`, autre conflit arrêté) ; `project sync` relancé : aucune écriture au second passage.
- Pas vérifié : le vrai `gh` (API des milestones, renommage des labels, `capture` de gojq dans `--jq`), les automatismes du tableau Projects, la signature avec la clé de Max, le build Gradle dans un worktree neuf (durée du premier build), Gradle tout court (aucun code du mod touché). Premier essai réel de chaque commande par Max avec `--dry-run`.
- Choix faits par Claude, signalés à Max : couleurs et descriptions des labels, label `type: idea` en plus des cinq types demandés, option `--allow-features`, notes de version gardées dans `build/release/` entre l'essai à blanc et la vraie publication, release toujours créée en brouillon, correctif possible seulement tant que la version à corriger est la dernière sur `main`, URL de clonage remplacée par un exemple dans `CONTRIBUTING.md` (renommage à venir).
- Reste à faire par Max, dans l'ordre : réglages GitHub de `docs/project-management.md` (branche par défaut, fusions, tableau Projects et ses automatismes), puis `scripts/dev project sync --dry-run` et `scripts/dev project sync`, puis `scripts/dev status`.
- Follow-up (2026-11-18, done by the maintainer): first push of the rewritten pre-push hook green (real build, unit and game tests); default branch `developement` (`gh repo edit --default-branch`); merge commits only, automatic branch deletion (`gh repo edit`), default merge message checked; "Roadmap" board (#4) linked with `gh project link 4 --owner <login>` (`--owner @me` refused), Status field and workflows set in the interface; `scripts/dev project sync`: 54 changes, issues #1 to #28, second run "everything is up to date". Commands that ask for an answer must run in a real terminal. Docs updated accordingly; `CLAUDE.md` rewritten in English for this organisation. Session B closed.

## Session A : dépôt propre, sécurisé et outillé (2026-11-18)

Audit de l'historique fait et validé par Max le 2026-11-18 : aucun secret (gitleaks, 57 commits), auteurs avec l'adresse masquée de GitHub, aucun fichier indésirable jamais commité, aucun chemin du poste. `CLAUDE.md`, `tasks/` et les mentions de Max restent publics. Aucun changement au code du mod, pas de GitHub Actions.

Décisions de Max : licence LGPL-3.0, titulaire « maximcuynat » ; version SemVer `0.1.0-alpha.1` dans `gradle.properties` (seule source), canal déduit du suffixe (alpha, beta, rien = release), tag `v0.1.0-alpha.1`, jar `netcraft-26.3-0.1.0-alpha.1.jar` ; `main` ne reçoit que les versions publiées, par une pull request depuis `developement` fusionnée avec un commit de fusion ; le tag 0.1.0-alpha.1 pourra devenir une pré-version GitHub, CurseForge et Modrinth attendront la fin du lot 1 ; hook avant push : tests sautés seulement si tous les fichiers modifiés sont des `.md` ou dans `docs/` ; gitleaks absent : avertissement.

Un commit par sujet, après le build vert de Max (Gradle ne tourne pas dans le bac à sable de Claude) :

- [x] 1. Wrapper Gradle régénéré en 9.2.1 avec l'empreinte de l'archive (`distributionSha256Sum`). Le jar actuel est un wrapper officiel 8.14.x, authentique mais périmé. Commande lancée par Max, puis contrôle : SHA-256 du jar = `423cb469…` (wrapper officiel 9.2.1), `build test` vert (Max, 2026-11-18). Fins de ligne fixées dans `.gitattributes` (`gradlew` en LF, `*.bat` en CRLF, `*.jar` binaire). L'empreinte n'est contrôlée qu'au téléchargement de la distribution : la distribution 9.2.1 déjà en cache n'a pas été revérifiée.
- [x] 2. `.gitignore` complété (`.env`, logs, captures, rapports de plantage, fichiers macOS et éditeurs) ; `git ls-files -ci --exclude-standard` reste vide.
- [x] 3. Licence LGPL-3.0 : `COPYING.LESSER` (LGPL) et `COPYING` (GPL, à laquelle la LGPL renvoie), `mod_license=LGPL-3.0-only`. `TEMPLATE_LICENSE.txt` (MIT du MDK) conservé. Build vert, `license="LGPL-3.0-only"` dans le jar (Max, 2026-11-18). Hooks installés et gitleaks vérifié chez Max.
- [x] 4. Version `0.1.0-alpha.1` et nom du jar avec la version de Minecraft. Build, tests en jeu verts (FML accepte la version) et `netcraft-26.3-0.1.0-alpha.1.jar` produit, `version="0.1.0-alpha.1"` dans le jar (Max, 2026-11-18).
- [x] 5. Hooks versionnés (`.githooks/`) et `scripts/install-hooks.sh` : messages de commit (rejoués sur les 57 sujets existants), gitleaks avant commit, build et tests en jeu avant push (sauf `.md` et `docs/` seuls ; arbre propre exigé). Hook avant push testé dans un clone jetable avec un faux `gradlew` (11 scénarios). Le hook avant commit bloque aussi quand gitleaks signale une erreur : en essai, gitleaks répondait « no leaks found » alors que git avait échoué. Installés et vérifiés chez Max.
- [x] 6. Releases : `cliff.toml` (Keep a Changelog en français : Ajouté, Modifié, Corrigé), `CHANGELOG.md`, `scripts/release.sh` (mode `--dry-run`, commit et tag signés, confirmation avant tout envoi, release GitHub en brouillon). Déroulé complet essayé dans un clone jetable avec de faux `gradlew` et `gh` (refus, envoi atomique, pull request, garde-fous de `--publish`, brouillon en pré-version, pas de terminal = non). Premier vrai essai : `scripts/release.sh --dry-run 0.1.0-alpha.1` par Max.
- [x] 7. `README.md` (français et anglais), `SECURITY.md` (signalement privé par GitHub), `CONTRIBUTING.md` (commits, branches, hooks, tests, releases).
- [x] 8. `docs/securite-depot.md` : réglages GitHub à faire par Max, chemins de l'interface, ordre des opérations, signature SSH des commits et des tags.
- [x] 9. `CLAUDE.md` : hooks, script de release, modèle de branches. Revue de la session ci-dessous.

Revue de la session A (2026-11-18) :

- Fait : audit, 10 commits sur `developement` (identité masquée, non signés), tous après un build vert de Max ou sans effet sur le build (hooks, docs, outillage de release).
- Vérifié pour de vrai : wrapper officiel 9.2.1, build et tests, licence et version dans le jar, tests en jeu avec `0.1.0-alpha.1`, hook gitleaks chez Max. Vérifié seulement dans des clones jetables : hook avant push (faux `gradlew`), `scripts/release.sh` (faux `gradlew` et `gh`, signature avec une clé jetable). Le premier push de Max sera le premier vrai passage du hook avant push.
- Reste à faire par Max : `scripts/release.sh --dry-run 0.1.0-alpha.1` (premier vrai essai du script), signature SSH puis réglages GitHub dans l'ordre de `docs/securite-depot.md`, choix 3.8 (signature des commits de Claude) avant d'exiger des commits signés sur `main`.
- Bloquant avant de rendre le dépôt public : renommage du mod (session dédiée).
- Choix faits par Claude et signalés à Max : `LGPL-3.0-only` plutôt que `-or-later`, fichiers `COPYING` et `COPYING.LESSER` au lieu d'un `LICENSE` (vérifier le badge de licence GitHub après le push), engagements de `SECURITY.md` (dernière version seulement, sans délai garanti) et parcours fork puis pull request de `CONTRIBUTING.md`.
- Gestion de projet GitHub (labels, jalons, tableau) : session B.

## Étape 1 : environnement de dev

- [x] Choisir le loader et la version de Minecraft : NeoForge 26.3.0.103 sur Minecraft 26.3 (le mod doit d'abord fonctionner seul)
- [x] JDK 25 : téléchargé automatiquement par Gradle au premier build (toolchain foojay), le Java 17 Homebrew suffit pour lancer Gradle 9.2.1
- [x] Générer le projet depuis le modèle officiel MDK-26.3-ModDevGradle
- [x] Renseigner l'identité du mod (id `netcraft`, package `dev.netcraft`, nom NetCraft, version 0.1.0)
- [x] Initialiser git et pousser sur le dépôt privé https://github.com/maximcuynat/netcraft (commit 41eadcb, lancé par Max)
- [x] `./gradlew build` passe : BUILD SUCCESSFUL en 3 min 53 s, `build/libs/netcraft-0.1.0.jar` produit (lancé par Max, Gradle ne peut pas ouvrir de socket local dans le bac à sable de Claude)
- [x] Le jeu se lance via `./gradlew runClient` (confirmé par Max le 2026-11-17). Monde de test : « Netcraft », créatif, superflat Redstone Ready. Présence de NetCraft dans l'écran Mods non confirmée explicitement.

## Blocs

- [x] Tour PC (`pc_tower`) : bloc, modèle, texture
- Ticket : PC portable (`laptop`), code et ressources écrits le 2026-11-17, vérification en jeu à faire (`scripts/project/issues/0.1.0/01-laptop-in-game.md`).

## Plan par lots

Le découpage complet (lots, équipements, dépendances) et son statut de validation sont dans `docs/plan-lots.md` ; feuille de route des versions dans `docs/project-management.md`.

Inventaire et dimensions réelles : `docs/equipements-reseau.md`.

- [x] Lot 1a (câble et branchement) : codé et validé en jeu le 2026-11-17 (procédure dans `docs/plan-lots.md`).
- [x] Principe « vrais échanges de protocoles » dans `docs/plan-lots.md` et `CLAUDE.md` (commit 132fbc6).
- [x] Lot 1b-1 (box, MAC, commutation) : validé par Max le 2026-11-18 (état et méthode de test dans `docs/plan-lots.md`, lot 1b). Étapes, un commit chacune :
  - [x] Ports typés (Ethernet, ligne) et MAC par port dans le graphe, sauvegarde compatible avec les mondes du lot 1a, tests (53 tests au vert)
  - [x] Couche trames : `MacAddress`, `EtherType`, `EthernetFrame`, contenu typé, tests
  - [x] Commutation réutilisable : table d'apprentissage avec expiration, décision de transfert, tests (75 tests au vert)
  - [x] Simulation : file de livraison, minuteries, point d'écoute, détection de boucle et limite par tick, logique du poste et de la box, rôle des appareils, tests (87 tests au vert)
  - [x] Branchement Minecraft : tick serveur, événements de câble, message de boucle
  - [x] Bloc box internet (modèle 7 x 7 x 3 px, texture provisoire, ressources)
  - [x] Indicateur : MAC du poste, MAC apprises par la box (paquet d'état), build vert, essai en jeu à faire
  - [x] Bug du cordon : l'attente restait accrochée à un point de départ cassé (relevé par Max le 2026-11-17). Reproduit par `PendingCableTest`, corrigé (annulation aussi au changement de dimension)
  - [x] Retirer les traces temporaires `[ESSAI 1b-1]` (`debug/TrialLog` et ses appels) : fait à l'étape 5 du module des messages ; les traces utiles passent par `Diagnostics` (DEBUG, anglais)
  - [x] Build de contrôle de c178668 et 5014267 (commités sans build, voir `tasks/lessons.md`) : vert sur 5014267 le 2026-11-18
  - [x] Tests en jeu automatiques et décors d'essai : conception validée par Max le 2026-11-18. Un commit par étape, seulement après un build vert :
    - [x] Jeu de sources `src/gametest/` exclu du jar, branché sur les runs ; run `gameTestServer` dans `run-gametest/` (ignoré par git) ; `./gradlew build` compile aussi ce jeu de sources
    - [x] Structure vide 16 x 4 x 16, joueur de test connecté (sur le modèle de `GameTestPlayer` de NeoForge, messages captés), nettoyage des appareils de chaque test à sa fin, réussie ou non
    - [x] Tests du lot commun (verts le 2026-11-18) : départ détruit (casse et explosion, créatif et survie, compte des cordons), changement de dimension, déconnexion et reconnexion, expiration des MAC, sauvegarde et relecture
    - [x] Lot à part (environnement dédié) pour ce qui touche les limites globales du moteur : boucle et limite d'un signalement par minute
    - [x] Horloge du moteur (demandé par Max le 2026-11-18) : un changement survenu entre deux ticks prenait le temps du tick précédent, la boucle était signalée toutes les 90 s au lieu de 60. Changements appliqués au début du tick suivant, tests unitaires de l'horloge, tests en jeu de la boucle (écart exact) et de l'expiration (bornes exactes) resserrés
    - [x] `/netcraft test <scénario>` et `/netcraft test effacer` (décors testés en jeu, vérification à l'œil faite par Max le 2026-11-18) : développement seulement (jeu de sources `gametest`), refus hors d'un monde dont le nom commence par « Essais », refus si la zone n'est pas vide
    - [x] Test automatique du refus hors d'un monde « Essais » (règle du nom et vraie commande), second poste sur la box de droite du décor « chaine » (2 MAC sur le port 1)
  - [x] Procédure de test en jeu (ce qui demande des yeux) et état du lot dans `docs/plan-lots.md`
  - [x] Vérification à l'œil du module des messages : faite par Max le 2026-11-18 (jeu en français, monde « Essais messages »), aucun plantage, rendu validé comme base du futur HUD (lot F). Tableau des 6 points dans `docs/plan-lots.md`.
  - [x] Points sans capture (panneau détaillé d'un poste et du portable, box de droite du décor « chaine », câble de 99 blocs sur toute sa longueur, arrêt des messages de boucle après le retrait d'un câble, `/netcraft liens`, rendu de ✔ et ℹ, repli `messageSymbols = false`) : vus à l'œil par Max pendant la même session, validés sur son témoignage le 2026-11-18
  - Ticket : lot F (interface HUD), rendu sur plusieurs tailles de fenêtre (`scripts/project/issues/backlog/02-hud-window-sizes.md`).
  - [x] Module des messages, avant le 1b-2 : audit et conception validés par Max le 2026-11-18 (`docs/messages.md`), 7 étapes codées et commitées le même jour après des builds verts, vérification à l'œil faite le même jour (résultat dans `docs/plan-lots.md`). Étapes :
    - [x] 1. Noyau en Java pur (`message/`) : niveaux, usages, canaux, valeurs, routage, anti-répétition, message envoyé et écouteur ; tests unitaires du routage et de l'anti-répétition. Aucun changement visible.
    - [x] 2. Transport et affichage : `MessagePayload` (canal, niveau, clé, valeurs), `MessageCenter` porté par les données du serveur, affichage décidé par le client (symbole ou mot selon le réglage `messageSymbols`), messages du cordon migrés, tests en jeu du cordon par l'écouteur. `TestPlayer` capte encore le chat pour le test de boucle, jusqu'à l'étape 4.
    - [x] 3. Textes : vouvoiement, distances entières, doublon et `result.ok` retirés, format commun d'un appareil, infobulle du cordon, test des fichiers de langue (mêmes clés, longueurs maximales).
    - [x] 4. Boucle et saturation : un événement par réseau et par tick, limite par minute dans le module, oubli au changement du réseau, message à l'appareil le plus proche du joueur, saturation aux opérateurs et au propriétaire en solo ; tests du moteur, du module et en jeu (une seule série de messages pour deux box).
    - [x] 5. Log en anglais et nettoyage : traces utiles passées par le module (DEBUG, anglais), `TrialLog` et `graphForTrial` retirés, exceptions en anglais.
    - [x] 6. Panneau d'état : lignes construites en Java pur (compact, détaillé accroupi, ligne « Accroupi : détails »), le client ne fait que dessiner ; tests unitaires.
    - [x] 7. Documents : `CLAUDE.md` (log en anglais, règles du module, données séparées du dessin), état dans `docs/plan-lots.md`, procédure de vérification à l'œil.
- Lot 1b-2 (DHCP) : architecture validée avec le 1b-1, pas commencé. Tâches : tickets de la milestone « Lot 1b-2: DHCP » (INIT-REBOOT compris).

## Tests multijoueur

Devenus des tickets le 2026-11-18 : fin du lot 1 (milestone « v0.1.0 », `scripts/project/issues/0.1.0/02-multiplayer-dedicated-server.md`) et deux joueurs vers la fin du lot 3 (`scripts/project/issues/backlog/01-multiplayer-two-players.md`). En attendant, garder la séparation client et serveur stricte dans le code (voir les règles du `CLAUDE.md`).

## Idées en vrac

Devenues des tickets `type: idea` le 2026-11-18 (`scripts/project/issues/ideas/`). Vision de Max : un Packet Tracer en blocs, fidèle au réel, complet, pour tout le monde et pour les passionnés de réseau. « Postes : PC à poser et à câbler » est fait (lots 0 et 1a) et n'a pas de ticket.

## Faits vérifiés le 2026-11-17

- Dernière version de Minecraft : 26.3 (2026-09-15), Java 25. La 1.21.1 demande Java 21.
- NeoForge : builds 26.3.0.x disponibles, modèles officiels MDK-26.3-ModDevGradle et MDK-1.21.1-ModDevGradle.
- Builds NeoForge sur Modrinth :
  - 1.21.1 : Mekanism, Create, AE2, CC:Tweaked, Powah, Energized Power
  - 26.3 : AE2, CC:Tweaked, Powah, Energized Power (pas Mekanism, pas Create)
- Immersive Engineering s'arrête à 1.21.1, Thermal Expansion à 1.20.1 (Forge seulement).
- NeoForge 21.9 (Minecraft 1.21.9) a refondu les API de transfert (énergie, items, fluides) et FML : un portage 1.21.1 vers 26.x touche la couche énergie.
- Machine : Java 17 Homebrew (insuffisant), VS Code présent, IntelliJ absent.
