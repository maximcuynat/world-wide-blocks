# NetCraft : suivi

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
  - [ ] Retirer les traces temporaires `[ESSAI 1b-1]` (`debug/TrialLog` et ses appels) une fois les essais du lot 1b-1 validés
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
  - [ ] À la prochaine vérification à l'œil (jeu en français) : portable au bout des 99 blocs, 2 MAC sur le port 1 du décor « chaine » (pendant le module des messages), textes du panneau et des messages
  - [ ] Module des messages (prompt séparé de Max), avant le 1b-2 : prévoir dès la conception un moyen pour les tests de capter les messages envoyés
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
