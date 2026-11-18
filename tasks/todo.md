# NetCraft : suivi

## Bloquant pour la publication

- [ ] **Renommer le mod avant de rendre le dépôt public.** « Netcraft » est le nom d'une société britannique (netcraft.com) qui travaille dans le même domaine que le mod (réseau, sécurité internet) : risque de conflit de nom. Max cherche un autre nom ; le renommage (nom affiché, `mod_id`, package `dev.netcraft`, ressources, dépôt GitHub, docs) fera l'objet d'une session dédiée. Rien ne sera rendu public, ni publié sur CurseForge ou Modrinth, avant. (décidé le 2026-11-18)

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
- [ ] 8. `docs/securite-depot.md` : réglages GitHub à faire par Max, chemins de l'interface, ordre des opérations, signature SSH des commits et des tags.
- [ ] 9. `CLAUDE.md` : hooks, script de release, modèle de branches. Revue de la session ci-dessous.

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
- [ ] PC portable (`laptop`) : code et ressources écrits le 2026-11-17 (modèles ouvert et fermé, blockstate, item, loot table, tag pioche, traductions). Texture provisoire générée par script. À vérifier : `./gradlew build` puis en jeu (inclinaison de l'écran dans les 4 orientations, clic droit ouvre/ferme, nom et icône dans l'onglet, drop à la pioche en survie, aucune erreur `netcraft:laptop` dans le log)

## Plan par lots

Le découpage complet (lots, équipements, dépendances) est dans `docs/plan-lots.md`. Statut : proposé le 2026-11-17, en attente de validation par Max. Aucun code au-delà du lot 0 avant validation.

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
  - [ ] Lot F (interface HUD) : vérifier le rendu sur plusieurs tailles de fenêtre ; en 854 x 480, l'infobulle du cordon passe à la ligne
  - [x] Module des messages, avant le 1b-2 : audit et conception validés par Max le 2026-11-18 (`docs/messages.md`), 7 étapes codées et commitées le même jour après des builds verts, vérification à l'œil faite le même jour (résultat dans `docs/plan-lots.md`). Étapes :
    - [x] 1. Noyau en Java pur (`message/`) : niveaux, usages, canaux, valeurs, routage, anti-répétition, message envoyé et écouteur ; tests unitaires du routage et de l'anti-répétition. Aucun changement visible.
    - [x] 2. Transport et affichage : `MessagePayload` (canal, niveau, clé, valeurs), `MessageCenter` porté par les données du serveur, affichage décidé par le client (symbole ou mot selon le réglage `messageSymbols`), messages du cordon migrés, tests en jeu du cordon par l'écouteur. `TestPlayer` capte encore le chat pour le test de boucle, jusqu'à l'étape 4.
    - [x] 3. Textes : vouvoiement, distances entières, doublon et `result.ok` retirés, format commun d'un appareil, infobulle du cordon, test des fichiers de langue (mêmes clés, longueurs maximales).
    - [x] 4. Boucle et saturation : un événement par réseau et par tick, limite par minute dans le module, oubli au changement du réseau, message à l'appareil le plus proche du joueur, saturation aux opérateurs et au propriétaire en solo ; tests du moteur, du module et en jeu (une seule série de messages pour deux box).
    - [x] 5. Log en anglais et nettoyage : traces utiles passées par le module (DEBUG, anglais), `TrialLog` et `graphForTrial` retirés, exceptions en anglais.
    - [x] 6. Panneau d'état : lignes construites en Java pur (compact, détaillé accroupi, ligne « Accroupi : détails »), le client ne fait que dessiner ; tests unitaires.
    - [x] 7. Documents : `CLAUDE.md` (log en anglais, règles du module, données séparées du dessin), état dans `docs/plan-lots.md`, procédure de vérification à l'œil.
- [ ] Lot 1b-2 (DHCP) : architecture validée avec le 1b-1, pas commencé. Ne pas oublier l'état INIT-REBOOT (bail du poste sauvegardé).

## Tests multijoueur (à faire plus tard, non prioritaire)

En attendant, garder la séparation client et serveur stricte dans le code (voir les règles du `CLAUDE.md`).

- [ ] À la fin du lot 1 : serveur dédié avec un seul client sur le même PC.
  1. `./gradlew runServer` (la première fois, accepter l'EULA dans `run/eula.txt` puis relancer).
  2. Dans un autre terminal, `./gradlew runClient`, puis Multijoueur, connexion à `localhost`.
  3. Dérouler la procédure du lot 1 dans `docs/plan-lots.md` : branchement, câble visible, indicateur d'état, débranchement, adresses et `ping` (lots 1b et 1c).
  4. Arrêter puis relancer le serveur : les liens tiennent.
  5. Vérifier dans les deux logs l'absence d'erreur, en particulier aucune classe client chargée par le serveur.
- [ ] Vers la fin du lot 3 : deux joueurs.
  1. Deux clients sur le même serveur (deux machines, ou deux instances avec des pseudos différents).
  2. Filtrage par chunks : un câble posé loin du second joueur ne lui parvient pas ; il apparaît quand il s'en approche et disparaît quand il s'éloigne, sauf si l'autre bout reste dans sa zone.
  3. Un joueur branche ou débranche : l'autre voit le câble apparaître ou disparaître et l'indicateur changer.
  4. Changement de dimension (Nether) : aller-retour, les câbles de l'Overworld réapparaissent au retour et aucun câble d'une autre dimension n'est affiché.

## Idées en vrac (dites par Max, non triées, non validées)

Vision : un Packet Tracer en blocs, fidèle au réel, complet, pour tout le monde et pour les passionnés de réseau.

- Postes : PC à poser et à câbler
- Baies et armoires, serveurs en rack, datacenter
- Switch réaliste (exemple : 32 ports = 32 vrais ports), firewall
- Configuration des équipements comme en vrai
- Câbles, rails de guidage des câbles
- Extérieur : poteaux réseau, câbles aériens, points de distribution
- Alimentation électrique compatible avec les autres mods (RF)
- Très loin : un OS, des comptes utilisateurs, un annuaire type AD

## Faits vérifiés le 2026-11-17

- Dernière version de Minecraft : 26.3 (2026-09-15), Java 25. La 1.21.1 demande Java 21.
- NeoForge : builds 26.3.0.x disponibles, modèles officiels MDK-26.3-ModDevGradle et MDK-1.21.1-ModDevGradle.
- Builds NeoForge sur Modrinth :
  - 1.21.1 : Mekanism, Create, AE2, CC:Tweaked, Powah, Energized Power
  - 26.3 : AE2, CC:Tweaked, Powah, Energized Power (pas Mekanism, pas Create)
- Immersive Engineering s'arrête à 1.21.1, Thermal Expansion à 1.20.1 (Forge seulement).
- NeoForge 21.9 (Minecraft 1.21.9) a refondu les API de transfert (énergie, items, fluides) et FML : un portage 1.21.1 vers 26.x touche la couche énergie.
- Machine : Java 17 Homebrew (insuffisant), VS Code présent, IntelliJ absent.
