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

- [x] Lot 1a (câble et branchement) : codé et validé en jeu le 2026-11-17 (procédure dans `docs/plan-lots.md`). Prochain : lot 1b, pas commencé.

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
