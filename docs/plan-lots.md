# NetCraft : plan par lots

Établi le 2026-11-17, révisé le même jour après relecture. Statut : proposé, en attente de validation. Lots codés : 0 et 1a (voir l'état de chaque lot).

## Principes

- Chaque lot ou sous-lot est petit et jouable à la fin : on peut ouvrir le jeu et constater que quelque chose de nouveau fonctionne.
- L'ordre suit le chemin réel d'une connexion : le poste, le logement, la rue, le quartier, le FAI, puis internet.
- Les équipements sont génériques : aucune marque, aucun modèle commercial.
- Pendant les lots, le retour visuel reste minimal : un indicateur d'état lisible (connecté ou non, adresse obtenue ou non). Les voyants animés, textures allumées et sons sont regroupés dans le lot de finition.
- Une ligne par bloc ou par item à créer. « Bloc » se pose dans le monde, « item » se tient en main.
- La simulation fait circuler de vrais échanges de protocoles (principe révisé le 2026-11-17, avant le lot 1b ; il remplace celui de la simulation abstraite) : de vraies trames Ethernet circulent dans le moteur, puis de vrais messages DHCP (lot 1b), ARP et ICMP (lot 1c). Une trame est livrée instantanément : le serveur la dépose dans une file d'attente qu'il traite à chaque tick, sans temps de trajet bloc par bloc le long du câble. Une limite de trames traitées par tick protège le serveur, en particulier en multijoueur et quand des équipements sont reliés en boucle.

## Périmètre

- Hors périmètre : le réseau câblé coaxial (modem câble, tête de réseau CMTS, noeud optique, amplificateurs). Il reste dans l'inventaire des équipements à titre de référence, mais aucun lot ne le prévoit.
- Le cuivre reste dans le plan comme technologie historique : Orange a transmis en 2022 à l'Arcep son plan de fermeture du réseau cuivre [14].

## Décisions à trancher

| Décision | Avant le lot | Proposition | Statut |
|---|---|---|---|
| Rôle de l'écran, du clavier et de la souris | 1c | Le terminal s'ouvre directement sur la tour ; les périphériques du lot 2c restent décoratifs et ne conditionnent pas l'accès. Alternative : les rendre obligatoires, ce qui changerait le comportement de la tour au lot 2c | À valider |
| Batterie du portable | É | Le portable fonctionne sur batterie quand il n'est pas branché et se recharge sur une prise | À valider |
| Électricité obligatoire ou non | É | Option de configuration « alimentation requise », activée par défaut ; désactivée, tous les appareils fonctionnent sans courant | À valider |
| Échelle des équipements en baie | 6a | Échelle par famille : baies à l'échelle du joueur, petits équipements agrandis. Avec le facteur 2,4 de la tour PC, une baie 42U ferait environ 4,8 blocs de haut | À valider |
| Cordon posé au sol séparé par un mur | F | Aucune proposition pour l'instant : que faire quand un mur sépare les deux appareils d'un cordon rendu au sol (voir le lot F) | À trancher |

Les dimensions réelles de chaque équipement sont dans `docs/equipements-reseau.md`.

Ce document décrit la conception. Les tâches de chaque lot sont des tickets GitHub, une milestone par lot ; la feuille de route des versions (0.1.0 pour le lot 1, 0.2.0 pour l'électricité et le lot 2, etc.) et l'organisation sont dans `docs/project-management.md`.

## Vue d'ensemble

| Lot | Thème | Ce qui marche à la fin |
|---|---|---|
| 0 | Postes | Deux postes posables (fait, portable à tester en jeu) |
| 1a | Câble et branchement | Deux postes reliés par un câble affichent « connecté » (fait) |
| 1b | La box attribue les adresses | Chaque poste branché à la box reçoit une adresse |
| 1c | Écran et commande ping | Depuis un poste, `ping` vers l'autre poste, qui répond |
| 1d | Switch de bureau | Plus de postes que la box n'a de ports |
| É | Électricité | Un appareil non alimenté est éteint |
| 2a | Wi-Fi | Le portable se connecte sans câble |
| 2b | Câblage fixe du logement | Les câbles passent par des prises murales et des goulottes |
| 2c | Périphériques et services locaux | Un fichier partagé et une impression en réseau |
| 2d | Autres appareils du logement | Téléphone, caméra et télévision sur le réseau |
| 3a | Arrivée fibre | La box est reliée à une prise optique |
| 3b | Arrivée cuivre | La box est reliée à une prise téléphonique |
| 4a | La rue en aérien | Une ligne tirée de poteau en poteau jusqu'à la maison |
| 4b | La rue en souterrain | La même ligne par des chambres et des fourreaux |
| 5a | Le quartier en fibre | Plusieurs maisons partent d'une même armoire de rue |
| 5b | Le quartier en cuivre | Idem pour les lignes cuivre |
| 6a | La baie | Des équipements se montent dans une baie, à un emplacement précis |
| 6b | Le local du FAI | Les lignes du quartier arrivent sur l'équipement de l'opérateur |
| 6c | Le service du FAI | La box reçoit une adresse publique, deux abonnés se joignent |
| 7a | Routage entre opérateurs | Deux abonnés de FAI différents se joignent |
| 7b | Serveurs et noms de domaine | Un nom se résout, un serveur répond |
| 7c | Le maillage d'internet | Un lien coupé, le trafic passe par un autre chemin |
| 8a | Baies et brassage d'entreprise | Un étage câblé proprement jusqu'à une baie |
| 8b | Switch configurable | Deux réseaux séparés sur un même switch |
| 8c | Routeur d'entreprise et pare-feu | Un flux autorisé, un flux bloqué |
| 8d | Serveurs et stockage | Un service hébergé sur un serveur en baie |
| 8e | Énergie de salle | Une coupure de courant sans arrêt des serveurs |
| 8f | Refroidissement | Une salle qui chauffe si la climatisation s'arrête |
| 8g | Système et comptes | On ouvre une session avec un compte de l'annuaire |
| 9a | Outils de pose et de test | Un câble se fabrique et se teste |
| 9b | Réseau mobile | Un appareil se connecte par une antenne |
| 9c | Satellite et faisceau hertzien | Un site isolé raccordé sans câble |
| 9d | Mode survie | Tous les équipements se fabriquent |
| F | Finition | Voyants, textures allumées, sons, modèles soignés |

## Lot 0 : postes

| Type | Équipement | Rôle | État |
|---|---|---|---|
| Bloc | Tour PC | Poste de travail fixe, un port réseau | Fait |
| Bloc | PC portable | Poste mobile, un port réseau, écran qui s'ouvre et se ferme | Écrit, à tester en jeu |

## Lot 1 : réseau local filaire

C'est le lot fondateur : il construit le moteur de simulation sur lequel tous les autres s'appuient (ports, liens, adresses, échange de messages).

### 1a : câble et branchement

| Type | Équipement | Rôle |
|---|---|---|
| Item | Cordon Ethernet | Relie deux ports réseau : un clic sur un appareil, un clic sur l'autre |
| Mécanique | Port réseau | Chaque appareil a un nombre fixe de ports ; un port ne reçoit qu'un seul câble |
| Mécanique | Indicateur d'état | En visant un appareil, on lit pour chaque port : libre ou connecté |

Jouable à la fin : deux postes reliés directement par un cordon affichent « connecté », et « libre » dès qu'on retire le cordon.

Choix proposés :

- Le cordon est un lien tendu visible entre deux appareils, pas une suite de blocs. Le passage propre des câbles arrive au lot 2b.
- Longueur maximale de 100 blocs, comme les 100 m d'un lien Ethernet cuivre réel.

Contrainte technique à tenir dès ce sous-lot : un lien doit continuer d'exister quand la zone du monde où il se trouve n'est pas chargée. Les lots 4 à 7 relient des sites éloignés, et le réseau ne doit pas tomber quand le joueur s'éloigne.

#### État du lot 1a

Codé le 2026-11-17 sur la branche `developement`. Build et 34 tests unitaires au vert, panneau d'état compris. Essais en jeu du même jour : branchement et câble visible jugés bons par Max ; d'après le log, le lien posé avant le redémarrage est toujours listé par `/netcraft liens` après relance du jeu, et encore listé depuis x ≈ 380, zone non chargée. Panneau d'état et consommation du cordon en survie validés par Max. Ajouté ensuite à sa demande et validé en jeu : retirer un seul bout du câble main vide, puis le débrancher de l'autre appareil pour récupérer le cordon (étapes 5 et 10). Lot 1a terminé.

| Type | Équipement | État |
|---|---|---|
| Item | Cordon Ethernet | Fait, texture provisoire |
| Mécanique | Port réseau | Fait : 1 port pour la tour et pour le portable |
| Mécanique | Indicateur d'état | Fait : panneau en haut au centre, un voyant par port |
| Rendu | Câble visible | Fait : ligne bleue légèrement affaissée, provisoire |

Comportement retenu :

- Clic droit avec le cordon sur un appareil, puis sur un autre : relie le premier port libre de chacun. Le cordon brille tant que le branchement est en attente. Au second clic, le serveur revérifie que la première extrémité existe toujours, est dans la même dimension et a son port libre.
- Accroupi + clic droit main vide sur un appareil branché : on retire le bout du câble de cet appareil et on le garde en main, l'autre bout reste branché (une ligne relie l'appareil à la main). Clic droit sur un autre appareil : le câble y est rebranché, sans consommer de nouveau cordon. Clic droit sur l'appareil où l'autre bout est branché, ou accroupi + clic dans le vide : le câble est débranché de l'autre bout aussi et le cordon reste libre en main.
- Accroupi + clic droit sur un appareil, cordon en main : débranche les deux bouts et range le cordon. Accroupi + clic droit dans le vide : annule un branchement commencé.
- Refusés : plus de 100 blocs en ligne droite entre les centres des deux blocs, deux dimensions différentes, le même appareil aux deux bouts, un port déjà occupé.
- Survie : le cordon est consommé au branchement. Il n'est rendu (au débranchement, en retirant un bout ou en tombant quand on casse un appareil) que si le lien en avait consommé un ; un lien posé en créatif ne rend rien en survie.
- Les appareils refusent les pistons : le moteur les identifie par leur position.

Socle technique pour les lots suivants :

- Le moteur (`dev.netcraft.network`) est un graphe d'appareils et de câbles en Java pur, testé sans lancer le jeu. Au lot 1a, il ne fait voyager aucun message ; depuis le lot 1b, de vraies trames y circulent (voir les principes).
- Il est sauvegardé dans le stockage global du serveur (`data/netcraft/network.dat` du monde), indépendant des chunks chargés. Aucune donnée réseau n'est en mémoire statique : en solo, deux mondes ne se mélangent pas.
- Chaque appareil connu du moteur garde son nombre de ports. C'est là que s'ajouteront l'adresse matérielle des ports (1b) et l'état de chaque appareil.
- Chaque lien retient son support (`copper_ethernet`, 100 blocs au plus) : la fibre et la ligne téléphonique s'y ajouteront.
- Toute la logique tourne sur le serveur. Un joueur ne reçoit que les câbles dont une extrémité est dans un chunk que son client a reçu, pour ne pas révéler l'emplacement d'appareils hors de vue.
- Commande `/netcraft liens` (opérateurs) : liste les liens tels que le serveur les connaît, même quand leur zone n'est pas chargée.

Limites connues, à reprendre plus tard :

- Le choix du port se fait automatiquement (premier port libre). Il faudra choisir le port visé quand les appareils en auront plusieurs (box au lot 1b).
- Un appareil retiré sans que le jeu le signale (par exemple un `/setblock` sans mise à jour) laisse un lien orphelin. Il est nettoyé à la prochaine pose ou au prochain clic sur cette position.
- Le panneau d'état occupe la même place que les barres de boss.
- La distance est affichée avec un point décimal quelle que soit la langue.

Procédure de test en jeu :

1. `./gradlew runClient`, monde « Netcraft » en créatif. Le cordon est dans l'onglet NetCraft.
2. Poser une tour PC et un PC portable à une vingtaine de blocs l'un de l'autre. En visant chacun, le panneau en haut au centre affiche « Port 1 : libre » avec un voyant gris.
3. Cordon en main, clic droit sur la tour (message « Extrémité 1 : Tour PC, port 1… », le cordon brille), puis clic droit sur le portable (message « Connecté… », l'écran du portable ne bascule pas). Un câble bleu relie les deux appareils ; en visant chacun, le panneau affiche « Port 1 : connecté » avec un voyant vert. `/netcraft liens` liste le lien.
4. Accroupi + clic droit sur l'un des deux avec le cordon : message « port 1 débranché », le câble disparaît, les deux repassent à « libre ». Rebrancher.
5. Accroupi + clic droit main vide sur le portable : le bout est retiré et reste en main, une ligne relie la tour à la main. Clic droit sur un troisième appareil : le câble relie la tour à ce nouvel appareil. Recommencer, puis clic droit sur la tour : le câble en est débranché, le cordon reste libre en main et la tour repasse à « libre ».
6. S'éloigner : `/tp ~400 ~ ~`. Taper `/netcraft liens` : le lien est toujours listé alors que sa zone n'est plus chargée. Revenir : `/tp ~-400 ~ ~`. Le câble est de nouveau visible et les deux appareils affichent « connecté ».
7. Sauvegarder et redémarrer : quitter vers le menu, fermer le jeu, relancer `./gradlew runClient`, rouvrir le monde. Le câble est visible, les deux appareils affichent « connecté » et `/netcraft liens` liste le lien.
8. Refus : relier un appareil à plus de 100 blocs (« Trop loin… »), puis un appareil dont le port est occupé (« Aucun port libre… »).
9. Casse : casser la tour branchée. Le câble disparaît et le portable repasse à « libre ».
10. Survie (`/gamemode survival`, avec un seul cordon dans l'inventaire) : brancher consomme le cordon. Main vide, accroupi + clic droit sur un appareil : le bout revient en main ; clic droit sur l'autre appareil : le cordon est libre en main, prêt à resservir. Casser un appareil relié en créatif ne fait tomber aucun cordon.

### 1b : la box attribue une adresse à chaque poste

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Box internet | Coeur du réseau du logement : 4 ports réseau local, relie les appareils entre eux et leur distribue les adresses |
| Mécanique | Adresse matérielle | Chaque port réseau a un identifiant unique (adresse MAC) |
| Mécanique | Commutation | La box transmet les messages d'un port à l'autre en retenant quel appareil est derrière quel port ; réutilisée telle quelle par les switchs |
| Mécanique | Attribution d'adresse | Un poste branché demande une adresse, la box lui en prête une (DHCP) avec le masque, la passerelle et le serveur de noms |

Jouable à la fin : chaque poste branché à la box affiche son adresse dans l'indicateur d'état ; débranché, il la perd.

La box a aussi un port d'arrivée de ligne, inactif jusqu'au lot 3.

Le lot est coupé en deux sous-lots (architecture validée par Max le 2026-11-17) :

- **1b-1 : box, adresses MAC et commutation.** Jouable : deux postes branchés à la box, la box affiche quelle MAC est derrière quel port.
- **1b-2 : DHCP.** Jouable : chaque poste obtient son adresse par un vrai échange DHCP et l'affiche avec l'état de son bail.

Architecture retenue :

- Trames en couches (`network/protocol/`) : une trame Ethernet porte une MAC source, une MAC destination (ou la diffusion), un type et un contenu typé. IPv4 (0x0800), puis UDP et DHCP, ARP (type officiel 0x0806) et ICMP s'y ajoutent chacun comme une variante de la couche du dessous.
- Une MAC par port, premier octet `02` (adresse administrée localement), unique dans le monde et sauvegardée ; un appareil cassé puis reposé en reçoit de nouvelles. Chaque port a un type : Ethernet ou arrivée de ligne. Le cordon Ethernet prend le premier port réseau local libre ; le choix du port viendra au lot 3.
- Simulation (`network/sim/`) : file de livraison traitée à chaque tick du serveur, minuteries, point d'écoute sur chaque trame entrante ou sortante (pour un futur analyseur), horloge sur le temps de jeu. Un changement survenu entre deux ticks (branchement, débranchement, appareil posé ou retiré) est appliqué au début du tick suivant, avec le temps de ce tick : ses trames, minuteries et signalements partent du tick où ses trames sont livrées (corrigé le 2026-11-18, voir le test en jeu de la boucle). Elle tourne même quand les chunks ne sont pas chargés.
- Commutation (`network/switching/`) : apprentissage des MAC sur les trames reçues, diffusion si la destination est inconnue ou adressée à tous, expiration des entrées après 300 s de jeu (valeur recommandée par IEEE 802.1D), oubli au tick suivant quand le câble est retiré. La box est ce switch plus un point local, sur lequel se branchera son propre service (DHCP au 1b-2) ; le switch de bureau du lot 1d sera le même switch, sans point local.
- Protection contre les boucles, en attendant le STP du lot 8b : une trame diffusée qui revient à un switch qu'elle a déjà traversé est supprimée et signalée (log, et message aux joueurs à moins de 32 blocs, au plus un par minute et par appareil). Filet de sécurité : 10 000 trames traitées par tick au plus, 50 000 en file.
- Trame de présence : **échafaudage, à retirer au lot 1c** quand le DHCP et l'ARP produiront du vrai trafic. Un poste diffuse une trame de type 0x88B5 (type expérimental IEEE) au branchement de son câble puis toutes les 30 s, pour que la box apprenne sa MAC avant que le DHCP existe.
- 1b-2 : client DHCP du poste selon la RFC 2131 (DISCOVER, OFFER, REQUEST, ACK avec un identifiant de transaction ; renouvellement à 50 % du bail, puis à 87,5 % en diffusion). Un poste qui avait une adresse avant un redémarrage du serveur redemande directement son ancienne adresse (état INIT-REBOOT) : son bail est donc sauvegardé. Plage 192.168.1.10 à 192.168.1.200 en /24, box en 192.168.1.1 (passerelle et serveur de noms), bail d'un jour de jeu (24 000 ticks), la box se souvient de l'adresse prêtée à chaque MAC. Deux serveurs DHCP sur le même réseau : l'indicateur signale le conflit.

#### État du lot 1b-1

Codé du 2026-11-17 au 2026-11-18 sur la branche `developement`, validé par Max le 2026-11-18. Tests unitaires et tests en jeu automatiques au vert, vérification à l'œil faite (panneau d'état, câbles dessinés, messages).

| Type | Équipement | État |
|---|---|---|
| Bloc | Box internet | Fait : 4 ports réseau local et 1 arrivée de ligne inactive, modèle 7 x 7 x 3 px, texture provisoire |
| Mécanique | Adresse matérielle | Fait : une MAC par port, sauvegardée, compatible avec les mondes du lot 1a |
| Mécanique | Commutation | Fait : apprentissage, diffusion, expiration à 300 s, oubli au retrait du câble, protection contre les boucles |
| Mécanique | Indicateur d'état | Fait : MAC du poste ; pour la box, la MAC apprise derrière chaque port |

Limites connues, à reprendre plus tard :

- Le câble part du centre du dessus de l'appareil, pas du port où il est branché.
- Messages (log en deux langues, deux messages pour une boucle) et traces `[ESSAI 1b-1]` : repris par le module des messages, ci-dessous.

#### Module des messages (avant le 1b-2)

Audit, conception et règles dans `docs/messages.md` (validés par Max le 2026-11-18). Codé le 2026-11-18 en 6 étapes, un commit chacune après un build vert (`./gradlew build test` : 123 tests unitaires ; `./gradlew runGameTestServer` : 17 tests en jeu). **Vérification à l'œil faite par Max le 2026-11-18** (jeu en français, monde « Essais messages » en superflat, aucun plantage) ; rendu validé comme base de la future interface HUD (lot F).

Résultat de la vérification : la colonne « Vu » est prouvée par les captures (`run/screenshots`) et le log ; la dernière colonne a été vue à l'œil par Max pendant la même session, sans capture, et validée sur son témoignage le 2026-11-18. Tous les points sont bons.

| Point | Vu (capture ou log) | Validé sur le témoignage de Max |
|---|---|---|
| 1. Infobulle et barre d'action | Infobulle : 3 lignes grises (la deuxième passe à la ligne dans une fenêtre de 854 x 480) ; « ✖ Box internet : aucun port libre. » en rouge | |
| 2. Panneau de la box | Compact « 3/4 ports connectés » et « Accroupi : détails » ; détaillé : ports 1 à 3 avec la MAC en entier, port 4 libre, port 5 (ligne) inactif ; postes compacts « Connecté » | Panneau détaillé d'un poste (port et MAC) |
| 3. 2 MAC sur un port (« chaine ») | Box de gauche : « Port 1 : connecté, 02:… (+1) » | Box de droite détaillée |
| 4. Portable au bout des 99 blocs | Portable « Connecté », câble arrivant au portable | Câble sur toute sa longueur, panneau détaillé du portable |
| 5. Message de boucle | Un seul message jaune « ⚠ Boucle réseau près de Box internet [x, y, z]… », de nouveau un seul plus tard ; une ligne « Network loop » en anglais par message | Écart d'une minute de jeu (jeu mis en pause entre les deux, prouvé par le test en jeu) ; arrêt des messages après le retrait d'un câble (aucun câble retiré) |
| 6. Textes français | Tous les textes vus sont en français et au vouvoiement ; symboles ✖ et ⚠ bien rendus | `/netcraft liens` (non lancé) ; rendu de ✔ et ℹ ; repli `messageSymbols = false` (non essayé) |

| Sujet | État |
|---|---|
| Canaux | Fait : le canal découle de l'usage et du niveau (geste : barre d'action ; événement : chat ; détail : log DEBUG) |
| Niveaux | Fait : information, succès, avertissement, erreur ; symbole (ℹ ✔ ⚠ ✖) ou mot (réglage `messageSymbols` du client), couleur constante |
| Anti-répétition | Fait : un problème une fois par minute ; une boucle vue par deux box donne un seul message ; la limite d'un réseau est oubliée quand ce réseau change |
| Textes | Fait : clés et valeurs seulement, vouvoiement, distances entières, longueurs vérifiées par un test, infobulle du cordon |
| Log | Fait : entièrement en anglais ; traces `[ESSAI 1b-1]` retirées, les utiles passées par `Diagnostics` (DEBUG) |
| Panneau d'état | Fait : compact par défaut, détaillé accroupi ; contenu en Java pur (`StatusPanel`), le client ne fait que dessiner |
| Réseau saturé | Fait : message aux opérateurs et au propriétaire en solo ; destinataires sans test automatique (il faudrait saturer le serveur de tests) |

Données séparées du dessin : le serveur envoie clé, valeurs et niveau ; le client décide seul de l'apparence. La future interface HUD (lot F) remplacera `client/ClientMessages` et `client/PortStatusLayer` sans toucher au reste.

Méthode de test (depuis le lot 1b-1) : la logique est prouvée par des tests automatiques ; les essais à la main ne servent plus qu'à ce qui demande des yeux. La sauvegarde de Max n'est jamais modifiée.

1. `./gradlew build test` : compilation (jeu de sources `gametest` compris) et tests unitaires du moteur, en Java pur.
2. `./gradlew runGameTestServer` : tests en jeu automatiques, sans fenêtre, dans un monde jetable (`run-gametest/`, ignoré par git). Le résultat est en fin de `run-gametest/logs/latest.log` (« All N required tests passed »). Ils couvrent : cordon dont le départ est cassé ou détruit par une explosion (créatif et survie, compte des cordons), changement de dimension et déconnexion pendant un branchement en attente, ports pleins, expiration des MAC (bornes exactes), sauvegarde et relecture du réseau, boucle signalée par un seul message par minute pour deux box (écart exact de 1 200 ticks) et limite oubliée seulement pour le réseau qui change (lot à part, car ils touchent les limites globales du moteur), construction de chaque décor d'essai, refus de `/netcraft test` hors d'un monde d'essai. Les messages se vérifient par l'écouteur du module (canal, clé, valeurs, destinataire), sans passer par le chat d'un joueur.
3. Vérification à l'œil avec `/netcraft test <décor>` : `./gradlew runClient`, monde créatif dont le nom commence par « Essais » (la commande est refusée ailleurs ; elle n'existe qu'en développement et seulement pour les opérateurs). Se placer face à un terrain dégagé : le décor est construit 3 blocs devant soi, tourné vers soi, et refusé si la zone n'est pas vide. Viser chaque appareil avant la capture (F2) pour afficher son panneau, debout (compact) puis accroupi (détaillé). `/netcraft test effacer` retire le dernier décor.

| Décor | À regarder |
|---|---|
| `box` | Panneau compact : « 0/4 ports connectés » ; accroupi : ports 1 à 4 libres, port 5 (ligne) inactif ; façade tournée vers le joueur |
| `box_postes` | Câbles vers 3 postes ; box compacte « 3/4 ports connectés », accroupi : 3 ports connectés avec la MAC apprise, port 4 libre ; poste compact « Connecté », accroupi : son port et sa MAC |
| `chaine` | Chaque box montre son poste et ceux de l'autre box ; accroupi, port 1 de la box de gauche : une MAC suivie de « (+1) » |
| `boucle` | Un seul message de boucle dans le chat (pas un par box), de nouveau un seul une minute de jeu plus tard |
| `ports_pleins` | Cordon en main, clic droit sur la box : « ✖ Box internet : aucun port libre. » en rouge au-dessus de la barre d'objets |
| `distance` | Câble de 99 blocs dessiné sur toute sa longueur ; les deux bouts « Connecté » (aller jusqu'au second bout pour le viser) |

Les captures vont dans `run/screenshots`, les messages dans `run/logs/latest.log`, les traces de diagnostic dans `run/logs/debug.log` (en anglais).

#### Vérification à l'œil du module des messages (jeu en français)

Préparation : `./gradlew runClient`, puis Options, Langue : Français. Ouvrir (ou créer) un monde créatif dont le nom commence par « Essais », se placer face à un terrain dégagé. Prendre une capture (F2) à chaque point marqué 📷.

1. **Infobulle et barre d'action.** Prendre un cordon Ethernet dans l'onglet NetCraft et le survoler dans l'inventaire : 3 lignes grises (« Clic droit sur deux appareils : les relier. », etc.) 📷. `/netcraft test ports_pleins`, cordon en main, clic droit sur la box : « ✖ Box internet : aucun port libre. » en rouge au-dessus de la barre d'objets 📷. `/netcraft test effacer`.
2. **Panneau compact puis détaillé (box).** `/netcraft test box_postes`. Viser la box debout : « Box internet », « 3/4 ports connectés » avec un voyant vert, « Accroupi : détails » en gris 📷. S'accroupir en la visant : « Port 1 : connecté, 02:… » (MAC en entier) pour les ports 1 à 3, « Port 4 : libre », « Port 5 (ligne) : inactif » 📷. Viser un poste : « Connecté », puis accroupi « Port 1 : connecté » et « MAC 02:… » 📷. `/netcraft test effacer`.
3. **2 MAC sur le port 1 de la box de gauche.** `/netcraft test chaine`. Viser la box de gauche accroupi : « Port 1 : connecté, 02:… (+1) » 📷. La box de droite, accroupi : port 1 avec une seule MAC (le poste de gauche), ports 2 et 3 avec la MAC de leur poste. `/netcraft test effacer`.
4. **Portable au bout du câble de 99 blocs.** `/netcraft test distance`. Viser la tour : « Connecté ». Marcher jusqu'au portable, 99 blocs devant : le câble est dessiné tout du long ; viser le portable : « Connecté », puis accroupi son port et sa MAC 📷. `/netcraft test effacer`.
5. **Message de boucle, unique et lisible.** `/netcraft test boucle`, rester à côté (moins de 32 blocs). Dans le chat : un seul message, « ⚠ Boucle réseau près de Box internet [x, y, z]. Retirez un câble de la boucle. » en jaune 📷. Une minute plus tard : de nouveau un seul message, pas deux. Retirer un des deux câbles entre les box (main vide, accroupi + clic droit sur la box de gauche) : plus aucun message. Dans `run/logs/latest.log`, une ligne en anglais « Network loop: … » par message. `/netcraft test effacer`.
6. **Textes français et repli des symboles.** Tout ce qui précède est en français et au vouvoiement ; `/netcraft liens` écrit « Box internet [x, y, z] (minecraft:overworld) » et des longueurs entières. Si un symbole (ℹ ✔ ⚠ ✖) s'affiche mal : dans `run/config/netcraft-client.toml`, mettre `messageSymbols = false` (relu sans relancer le jeu) et refaire le point 1 : « Erreur : Box internet : aucun port libre. ». Remettre `true` ensuite.

À rapporter : les captures, et tout texte coupé, débordant du panneau, mal traduit ou dont le symbole s'affiche mal.

### 1c : écran sur le poste et commande ping

| Type | Équipement | Rôle |
|---|---|---|
| Écran | Terminal du poste | Un clic droit sur la tour ou sur le portable ouvert affiche une invite de commandes |
| Commande | `ip addr` | Affiche l'adresse du poste |
| Commande | `ping <adresse>` | Envoie une requête à un autre appareil et affiche sa réponse ou l'absence de réponse |
| Mécanique | Résolution d'adresse | Avant d'envoyer, le poste demande sur le réseau quelle adresse matérielle correspond à l'adresse visée (ARP) |

Jouable à la fin : depuis un poste, `ping` vers l'autre poste, et il répond. C'est le critère de réussite du lot 1.

Conséquence sur le portable : le clic droit ouvrira le terminal, et l'écran s'ouvrira ou se fermera en cliquant sur la partie écran du modèle. Accroupi + clic droit main vide est déjà pris par le débranchement d'un bout de câble (décidé au lot 1a, le 2026-11-17).

Choix proposé : les commandes suivent la convention Linux (`ip addr`, `ping`, `traceroute`, `nslookup`), qui est aussi la plus proche des équipements réseau.

### 1d : switch de bureau

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Switch de bureau 5 ports | Multiplie les prises réseau, sans configuration |
| Bloc | Switch de bureau 8 ports | Idem, avec plus de ports |

Jouable à la fin : six postes ou plus sur une même box, tous joignables par `ping`.

## Lot É : électricité

Lot court, placé juste après le lot 1 pour ne pas avoir à reprendre des dizaines de blocs plus tard.

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Tableau électrique | Source d'énergie du logement ; accepte aussi l'énergie d'autres mods (RF) |
| Item | Câble électrique | Relie le tableau aux prises |
| Bloc | Prise électrique murale | Point d'alimentation |
| Item | Cordon d'alimentation | Relie un appareil à une prise |
| Bloc | Multiprise | Plusieurs appareils sur une seule prise |
| Mécanique | Consommation | Chaque appareil consomme une puissance ; non alimenté, il est éteint et ne répond plus |
| Mécanique | Batterie du portable | Le portable continue de fonctionner débranché, tant que sa batterie n'est pas vide (voir « Décisions à trancher ») |
| Réglage | Option « alimentation requise » | Permet de jouer sans gérer l'électricité (voir « Décisions à trancher ») |

Jouable à la fin : on débranche la box, tout le réseau du logement tombe.

Choix proposé : le mod devant fonctionner seul, le tableau électrique fournit lui-même de l'énergie. Le brancher sur un autre mod reste possible mais jamais obligatoire.

## Lot 2 : réseau local complet

### 2a : Wi-Fi

| Type | Équipement | Rôle |
|---|---|---|
| Mécanique | Wi-Fi de la box | La box diffuse un réseau sans fil avec un nom et un mot de passe, sur une portée limitée |
| Mécanique | Carte Wi-Fi du portable | Le portable liste les réseaux à portée et s'y connecte |
| Bloc | Point d'accès Wi-Fi | Étend la couverture, relié à la box par un cordon |
| Bloc | Répéteur Wi-Fi | Étend la couverture sans câble, branché sur une prise électrique |

Jouable à la fin : le portable, sans câble, fait un `ping` vers la tour.

### 2b : câblage fixe du logement

| Type | Équipement | Rôle |
|---|---|---|
| Item | Câble Ethernet d'installation | Câble posé à demeure entre le coffret et les prises |
| Bloc | Prise murale RJ45 | Point de branchement dans une pièce |
| Bloc | Coffret de communication | Tableau où arrivent tous les câbles du logement, avec la box |
| Bloc | Goulotte | Guide les câbles le long des murs |

Jouable à la fin : un poste branché sur la prise d'une chambre joint la box rangée dans le coffret.

### 2c : périphériques et services locaux

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Écran | Affichage de la tour |
| Bloc | Clavier | Saisie de la tour |
| Bloc | Souris | Pointage de la tour |
| Bloc | Serveur de stockage (NAS) | Partage de fichiers sur le réseau local |
| Bloc | Imprimante réseau | Impression depuis n'importe quel poste |

Jouable à la fin : un fichier déposé depuis un poste est lu depuis un autre.

Le rôle de l'écran, du clavier et de la souris dépend de la décision prise avant le lot 1c (voir « Décisions à trancher »).

### 2d : autres appareils du logement

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Téléphone IP | Téléphonie par le réseau |
| Bloc | Caméra IP | Vidéosurveillance par le réseau |
| Bloc | Télévision connectée | Client réseau du salon |
| Bloc | Adaptateur CPL | Fait passer le réseau par le circuit électrique (dépend du lot É) |
| Bloc | Injecteur PoE | Alimente un appareil par son câble réseau (dépend du lot É) |

Jouable à la fin : chaque nouvel appareil obtient une adresse et répond au `ping`.

## Lot 3 : arrivée chez l'abonné

Pour que ce lot soit jouable avant que la rue existe, un bloc de test fournit la ligne.

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Ligne opérateur de test | Bloc du mode créatif qui simule l'opérateur en amont ; remplacé par la vraie chaîne au fil des lots 4 à 6 |

### 3a : arrivée fibre

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Prise terminale optique | Point d'arrivée de la fibre dans le logement, passif |
| Item | Jarretière optique | Cordon fibre court entre la prise optique et le boîtier fibre |
| Bloc | Boîtier fibre (ONT) | Convertit le signal optique en Ethernet pour la box |
| Item | Câble de branchement optique | Fibre qui relie la prise optique à l'extérieur |

Jouable à la fin : la box affiche « ligne raccordée ».

### 3b : arrivée cuivre

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Prise téléphonique | Point d'arrivée de la ligne cuivre dans le logement |
| Item | Cordon téléphonique | Relie la box à la prise téléphonique |
| Item | Câble de branchement cuivre | Paire de cuivre qui relie la prise à l'extérieur |
| Mécanique | Débit selon la distance | Sur cuivre, le débit baisse avec la longueur de la ligne |

Jouable à la fin : la box affiche « ligne raccordée » par le cuivre, avec un débit qui dépend de la longueur.

## Lot 4 : la rue

### 4a : en aérien

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Poteau | Support des câbles aériens, haut de plusieurs blocs |
| Item | Câble aérien optique | Câble à plusieurs fibres tendu de poteau en poteau |
| Item | Câble aérien cuivre | Câble à plusieurs paires tendu de poteau en poteau |
| Bloc | Boîtier de branchement optique | Dernier boîtier fibre avant les logements, fixé sur poteau ou en façade |
| Bloc | Point de concentration cuivre | Dernier boîtier cuivre avant les logements, fixé sur poteau ou en façade |

Jouable à la fin : une maison raccordée par une ligne tirée sur trois poteaux.

### 4b : en souterrain

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Chambre de tirage | Regard sous trottoir où l'on tire et raccorde les câbles |
| Bloc | Fourreau | Conduit enterré entre deux chambres |
| Item | Câble souterrain optique | Câble fibre tiré dans les fourreaux |
| Bloc | Boîtier d'épissures | Jonction étanche entre deux câbles, dans une chambre |

Jouable à la fin : une maison raccordée par une ligne enterrée passant par deux chambres.

## Lot 5 : le quartier

### 5a : en fibre

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Armoire de rue fibre | Point où les fibres des logements rejoignent celles des opérateurs |
| Item | Coupleur optique | Partage une fibre d'opérateur entre plusieurs abonnés, par exemple 1 vers 32 ; se place dans l'armoire |
| Item | Câble de distribution optique | Câble de forte capacité entre l'armoire et les boîtiers de branchement |
| Item | Câble de transport optique | Relie l'armoire de rue au local de l'opérateur |

Jouable à la fin : plusieurs maisons raccordées à une même armoire, sur une seule fibre côté opérateur.

### 5b : en cuivre

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Armoire de rue cuivre (sous-répartiteur) | Point de raccordement d'une zone au réseau principal de l'opérateur |
| Item | Câble de transport cuivre | Câble de nombreuses paires entre l'armoire et le local de l'opérateur |

Jouable à la fin : plusieurs maisons raccordées en cuivre à une même armoire.

## Lot 6 : le FAI

### 6a : la baie

Sous-lot à part, car il introduit une mécanique nouvelle : le montage d'équipements dans des emplacements numérotés.

Prérequis : avoir tranché l'échelle des équipements en baie (voir « Décisions à trancher »).

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Baie 19 pouces | Armoire qui reçoit les équipements rackables |
| Mécanique | Emplacements en U | Chaque équipement occupe un nombre d'unités ; on choisit son emplacement dans la baie |
| Item | Équipement rackable de test | Boîtier vide de 1U pour valider le montage |

Jouable à la fin : on monte et on démonte un équipement à l'emplacement voulu.

### 6b : le local technique

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Tiroir optique | Terminaison des fibres en baie, pour le brassage |
| Bloc | Équipement de tête de ligne fibre (OLT) | Équipement actif de l'opérateur ; chaque port dessert un groupe d'abonnés par les coupleurs |
| Bloc | Répartiteur cuivre | Brassage des paires de cuivre à l'arrivée dans le central |
| Bloc | Équipement de tête de ligne cuivre (DSLAM) | Équivalent de l'OLT pour les lignes cuivre |

Jouable à la fin : le bloc « ligne opérateur de test » n'est plus nécessaire, la vraie chaîne va de la box au local de l'opérateur.

### 6c : le service du FAI

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Routeur d'agrégation des abonnés | Regroupe les sessions des box et leur attribue une adresse publique |
| Mécanique | Traduction d'adresses dans la box | La box partage son adresse publique entre les postes du logement (NAT) |
| Commande | `traceroute <adresse>` | Affiche les équipements traversés jusqu'à la destination |

Jouable à la fin : depuis un poste, `ping` vers l'adresse publique de la box d'un autre abonné du même FAI, qui répond. Un poste derrière une autre box n'est pas joignable directement par son adresse privée, comme dans la réalité.

## Lot 7 : internet

### 7a : routage entre opérateurs

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Routeur d'opérateur | Achemine les paquets entre réseaux selon une table de routage |
| Item | Fibre longue distance | Lien entre deux sites éloignés |
| Écran | Console du routeur | Configuration des interfaces et des routes en ligne de commande |

Jouable à la fin : `ping` vers l'adresse publique de la box d'un abonné d'un autre FAI, qui répond.

### 7b : serveurs et noms de domaine

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Serveur rackable | Machine qui héberge des services, 1U en baie |
| Service | Serveur DNS | Traduit un nom de domaine en adresse |
| Service | Serveur web | Répond à une requête par une page |
| Commande | `nslookup <nom>` | Interroge le DNS |

Jouable à la fin : `ping` vers un nom plutôt que vers une adresse.

### 7c : le maillage

| Type | Équipement | Rôle |
|---|---|---|
| Mécanique | Routage dynamique | Les routeurs s'échangent leurs routes et s'adaptent aux pannes |
| Bloc | Point d'échange internet | Site neutre où plusieurs opérateurs s'interconnectent |

Jouable à la fin : on coupe un lien entre deux opérateurs, le trafic reprend par un autre chemin.

## Lot 8 : entreprise et datacenter

Lots éloignés : les listes sont complètes, mais le découpage sera revu en y arrivant.

### 8a : baies et brassage d'entreprise

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Coffret mural | Petite baie fixée au mur pour un étage ou un petit site |
| Bloc | Panneau de brassage cuivre | Terminaison en baie des câbles venant des prises murales |
| Item | Cordon de brassage | Cordon court entre le panneau et le switch |
| Bloc | Passe-câbles | Guide les cordons entre deux équipements |
| Bloc | Panneau obturateur | Bouche un emplacement vide de la baie |
| Bloc | Chemin de câbles | Supporte les câbles au plafond entre les baies et les pièces |

Jouable à la fin : un étage câblé des prises murales jusqu'au switch en baie.

### 8b : switch configurable

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Switch d'accès 24 ports | Raccorde les postes d'un étage, 1U |
| Bloc | Switch d'accès 48 ports | Idem, plus dense, 1U |
| Bloc | Switch de distribution | Relie les switchs d'accès entre eux |
| Item | Module optique | S'insère dans un port du switch pour le relier en fibre |
| Écran | Console du switch | Configuration en ligne de commande |
| Mécanique | Réseaux virtuels (VLAN) | Sépare plusieurs réseaux sur un même switch |
| Item | Câble console | Relie un poste au port de configuration d'un équipement |

Jouable à la fin : deux groupes de postes sur un même switch, isolés l'un de l'autre.

### 8c : routeur d'entreprise et pare-feu

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Routeur d'entreprise | Relie le site à internet et aux autres sites |
| Bloc | Pare-feu | Filtre le trafic entre zones selon des règles |
| Mécanique | Règles de filtrage | Autoriser ou bloquer selon la source, la destination et le service |
| Mécanique | Tunnel entre sites (VPN) | Relie deux sites comme s'ils étaient sur le même réseau |

Jouable à la fin : un flux autorisé passe, un flux interdit est bloqué.

### 8d : serveurs et stockage

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Serveur 1U | Serveur plat d'usage général |
| Bloc | Serveur 2U | Serveur avec plus de disques |
| Bloc | Serveur 4U | Serveur de calcul ou de stockage |
| Bloc | Serveur tour | Serveur pour un site sans baie |
| Bloc | Baie de disques | Extension de stockage |
| Bloc | Console écran-clavier rackable | Écran et clavier escamotables pour intervenir sur un serveur |

Jouable à la fin : un service hébergé sur un serveur en baie, joint depuis un poste.

### 8e : énergie de salle

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Bandeau de prises de baie | Distribue le courant aux équipements de la baie |
| Bloc | Onduleur rackable | Maintient l'alimentation pendant une coupure |
| Bloc | Armoire de batteries | Prolonge l'autonomie de l'onduleur |
| Bloc | Groupe électrogène | Secours de longue durée |
| Mécanique | Double alimentation | Un équipement branché sur deux sources survit à la perte de l'une |

Jouable à la fin : on coupe le courant, les serveurs restent allumés le temps de l'autonomie.

### 8f : refroidissement

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Armoire de climatisation | Refroidit la salle |
| Bloc | Dalle de faux plancher | Plancher technique sous lequel passent l'air et les câbles |
| Bloc | Dalle perforée | Laisse monter l'air froid devant les baies |
| Bloc | Sonde de température | Mesure la température de la salle |
| Mécanique | Chaleur | Les équipements chauffent ; trop chauds, ils s'arrêtent |

Jouable à la fin : sans climatisation, la salle chauffe et les serveurs s'arrêtent.

### 8g : système et comptes

| Type | Équipement | Rôle |
|---|---|---|
| Mécanique | Système d'exploitation | Fichiers, programmes et sessions sur les postes et serveurs |
| Mécanique | Comptes utilisateurs | Ouverture de session par identifiant et mot de passe |
| Service | Annuaire | Serveur qui centralise les comptes et les droits |

Jouable à la fin : on ouvre une session sur un poste avec un compte défini sur le serveur d'annuaire.

## Lot 9 : réseaux sans fil étendus, outils et survie

Lots éloignés : les listes sont complètes, mais le découpage sera revu en y arrivant.

### 9a : outils de pose et de test

| Type | Équipement | Rôle |
|---|---|---|
| Item | Bobine de câble | Câble brut à couper à la longueur voulue |
| Item | Connecteur RJ45 | Se pose au bout d'un câble |
| Item | Pince à sertir | Fixe le connecteur sur le câble |
| Item | Testeur de câble | Vérifie qu'un câble est correctement monté |
| Item | Soudeuse fibre | Raccorde deux fibres |
| Item | Réflectomètre | Localise une coupure sur une fibre |

Jouable à la fin : un cordon fabriqué à la main, testé, puis utilisé.

### 9b : réseau mobile

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Pylône | Support des antennes |
| Bloc | Antenne de téléphonie mobile | Couvre un secteur autour du pylône |
| Bloc | Armoire de site radio | Équipements et énergie au pied du pylône |
| Bloc | Routeur mobile | Donne un accès internet par le réseau mobile |
| Item | Téléphone mobile | Client du réseau mobile |

Jouable à la fin : un logement sans ligne fixe accède à internet par l'antenne.

### 9c : satellite et faisceau hertzien

| Type | Équipement | Rôle |
|---|---|---|
| Bloc | Antenne satellite d'abonné | Accès internet par satellite |
| Bloc | Station terrienne | Passerelle entre les satellites et le réseau terrestre |
| Bloc | Antenne de faisceau hertzien | Liaison radio entre deux sites en vue directe |

Jouable à la fin : un site isolé relié au réseau sans aucun câble.

### 9d : mode survie

| Type | Équipement | Rôle |
|---|---|---|
| Mécanique | Recettes de fabrication | Chaque équipement se fabrique à partir de ressources |
| Item | Composants intermédiaires | Circuit, boîtier, câble brut |

Jouable à la fin : tout le mod est accessible sans le mode créatif.

## Lot F : finition

| Contenu | Détail |
|---|---|
| Voyants | Voyants de lien et d'activité par port, animés |
| Textures | Écrans et façades allumés ou éteints selon l'état |
| Sons | Ventilateurs, clics de branchement, bips |
| Modèles | Reprise des modèles et textures provisoires |
| Cordon au sol | Le cordon est rendu posé au sol au lieu d'une ligne tendue (idée notée le 2026-11-17, non validée) |
| Interface HUD | HUD plus professionnel et plus soigné pour le panneau d'état et les messages (demandé par Max le 2026-11-18) |

Interface HUD : le rendu actuel (panneau compact ou détaillé, messages avec symbole et couleur) est **validé par Max le 2026-11-18 comme base** ; rien n'est à changer avant le lot F. Le travail futur porte sur l'apparence : un HUD plus professionnel et plus soigné, avec une vérification du rendu sur plusieurs tailles de fenêtre (en 854 x 480, la deuxième ligne de l'infobulle du cordon passe à la ligne). Le module des messages (voir `docs/messages.md`) sépare les données du dessin : le serveur n'envoie que des clés, des valeurs et un niveau, les lignes du panneau sont construites en Java pur (`StatusPanel`), et seul le client décide de l'apparence. La nouvelle interface remplace donc le dessin (`client/ClientMessages`, `client/PortStatusLayer`) sans toucher au moteur, aux paquets ni aux textes.

Cordon au sol, idée à préciser en arrivant au lot :

- Pas de simulation physique : le tracé est calculé une fois. Le câble suit le relief entre les deux appareils, avec un léger affaissement entre deux appuis.
- Le tracé n'est recalculé que lorsqu'un bloc change sur le trajet.
- Point à trancher à ce moment-là : que faire si un mur sépare les deux appareils (voir « Décisions à trancher »).
- Le passage le long des murs et sur les chemins de câbles reste prévu aux lots 2b (goulottes) et 8a (chemins de câbles).

## Dépendances

- Le lot 1a est requis par tout le reste.
- 1c dépend de 1b : sans adresse, pas de `ping`.
- Le lot É est requis par l'adaptateur CPL, l'injecteur PoE et l'énergie de salle (8e).
- Le lot 3 a besoin du bloc de test tant que les lots 4 à 6 ne sont pas faits.
- La baie 19 pouces (6a) est requise par 6b, 7b et tout le lot 8.
- La décision d'échelle des équipements en baie doit être prise avant 6a.
- Les outils (9a) peuvent être avancés si l'on veut fabriquer les câbles plus tôt.

## Fonctionnement réel : ce qui a été vérifié

| Point | Vérification | Source |
|---|---|---|
| Attribution d'adresse en quatre étapes (découverte, offre, demande, accusé) | Lu | [1] |
| Chaîne fibre : équipement de tête de ligne chez l'opérateur, coupleurs passifs, boîtier chez l'abonné | Lu | [2] |
| Un port d'OLT dessert environ 32 ou 64 abonnés par les coupleurs ; portée d'environ 20 km à 32 abonnés | Lu | [2] |
| En France, les opérateurs se raccordent au réseau partagé par un point de mutualisation : boîtier en pied d'immeuble ou armoire de rue | Lu | [3], [4] |
| Le routeur d'agrégation se trouve à la bordure du coeur de réseau du FAI et regroupe les sessions des abonnés | Lu | [5] |
| Les coupleurs se trouvent à la transition entre la fibre de l'opérateur et les fibres des abonnés | Lu | [3] |
| Le point de mutualisation de zone, en rue, regroupe 360 à 1 000 lignes | Lu | [4] |
| L'OLT se trouve au noeud de raccordement optique et y joue le rôle du DSLAM | Lu | [6] |
| Cuivre : la boucle locale va du répartiteur de l'opérateur à la prise de l'abonné | Lu | [7] |
| Cuivre : le répartiteur est dans le central ; le sous-répartiteur raccorde une zone au réseau principal | Lu | [8] |
| Les adresses privées (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) ne sont pas routées sur internet : un poste derrière une box n'est joignable de l'extérieur que par la traduction d'adresses (NAT) | Extrait | [9], [10] |
| Cuivre : le dernier boîtier avant le logement est le point de concentration (PC), en façade ou sur poteau ; le câble de distribution, commun à plusieurs abonnés, arrive en amont | Extrait | [11], [12] |
| Cuivre : la ligne remonte du point de concentration au sous-répartiteur, puis au répartiteur | Extrait | [13] |

« Lu » : page ouverte et lue. « Extrait » : vu dans un résultat de moteur de recherche, page non ouverte en entier.

Les sources lues sont surtout Wikipedia : c'est suffisant pour le fonctionnement général. Pour les points propres à la France (PM, PBO, NRO, PC), lire en priorité avant les lots 4 et 5 les documents repérés dans l'inventaire : le guide CREDO d'Acome et le recueil de spécifications de l'Arcep pour les zones moins denses.

## Sources

1. Wikipedia, « Dynamic Host Configuration Protocol » : https://en.wikipedia.org/wiki/Dynamic_Host_Configuration_Protocol
2. Wikipedia, « Passive optical network » : https://en.wikipedia.org/wiki/Passive_optical_network
3. Wikipedia, « Réseau FTTH » : https://fr.wikipedia.org/wiki/R%C3%A9seau_FTTH
4. Wikipedia, « Point de mutualisation » : https://fr.wikipedia.org/wiki/Point_de_mutualisation
5. Wikipedia, « Broadband remote access server » : https://en.wikipedia.org/wiki/Broadband_remote_access_server
6. Wikipedia, « Noeud de raccordement optique » : https://fr.wikipedia.org/wiki/N%C5%93ud_de_raccordement_optique
7. Wikipedia, « Boucle locale » : https://fr.wikipedia.org/wiki/Boucle_locale
8. Wikipedia, « Répartiteur téléphonique » : https://fr.wikipedia.org/wiki/R%C3%A9partiteur_t%C3%A9l%C3%A9phonique
9. IETF, RFC 1918, « Address Allocation for Private Internets » : https://www.rfc-editor.org/rfc/rfc1918.txt
10. Wikipedia, « Private network » : https://www.wikipedia.org/wiki/Private_network
11. OVHcloud, « La desserte interne » : https://docs.ovhcloud.com/fr/guides/web-cloud/internet/internet-access/la-desserte-interne
12. Arcep, avis du comité d'experts cuivre DEX150505, VDSL2 au PC : https://arcep.fr/fileadmin/reprise/dossiers/degroup/avis-DEX150505-VDSL2_au_PC-050515.pdf
13. Orange, définition et classes des PLP : https://developer.orange.com/ope-contents/channels/87afd7365baec589/offers/7QKqBnDZ5utv021d/products/sAxl9K40i4n0a0sS/contents/faKUIj4gm2fYD5G2
14. Arcep, rapport 2026, fiche fermeture du réseau cuivre : https://www.arcep.fr/fileadmin/user_upload/espace_collectivites/rapport-TC-2026/RA2026-Tome02-fermeture-reseau-cuivre.pdf
