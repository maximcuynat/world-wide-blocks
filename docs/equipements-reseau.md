# Équipements réseau : du domicile à l'opérateur

Inventaire des équipements à modéliser dans World Wide Blocks, avec leurs dimensions réelles quand elles sont connues. Ce document sert à dimensionner les blocs. Il ne décide pas encore de l'ordre de développement ni de la taille en jeu de chaque objet.

Établi le 2026-11-17.

## Comment lire ce document

Chaque dimension porte un statut, parce que toutes n'ont pas le même niveau de fiabilité :

| Statut | Signification |
|---|---|
| **S** | Sourcé : la page ou la fiche a été lue, la source est numérotée en fin de document |
| **E** | Extrait : valeur vue dans un résultat de moteur de recherche, page non ouverte. À confirmer |
| **M** | De mémoire : ordre de grandeur usuel, sans source consultée. À vérifier avant de s'y fier |
| [À COMPLÉTER] | Aucune valeur fiable trouvée |

Les dimensions sont données en millimètres, dans l'ordre largeur x profondeur x hauteur, sauf mention contraire.

Précision utile : en jeu, 1 pixel représente 62,5 mm et les équipements sont agrandis d'environ 2,4. Une erreur de quelques millimètres ne change donc rien au modèle. On ne vérifie une cote qu'au moment de modéliser l'équipement, lot par lot, sans chercher à tout confirmer d'avance.

Limite de cette recherche : l'outil de lecture de pages était bloqué pendant la session. Une partie seulement des fiches constructeur a pu être ouverte, et beaucoup de dimensions restent en statut E ou M. La liste des équipements, elle, est complète pour un premier inventaire.

## Échelle : du réel au jeu

- Dans Minecraft, 1 bloc représente 1 m et se divise en 16 px, soit 62,5 mm par pixel.
- À cette échelle exacte, la plupart des équipements sont illisibles : un switch 1U ferait 7 px de large pour 0,7 px de haut, une tour PC 3 px de large.
- Les deux blocs déjà faits sont donc agrandis : la tour PC fait 8 px de large pour 210 mm réels, soit un facteur d'environ 2,4.
- Point à trancher avant de modéliser les baies : une échelle unique agrandie (cohérente mais encombrante), ou une échelle par famille (baies à l'échelle du joueur, petits équipements agrandis).
- Proposition : l'échelle par famille. Avec le facteur 2,4, une baie 42U de 1991 mm ferait environ 4,8 blocs de haut. La décision est suivie dans `docs/plan-lots.md`, section « Décisions à trancher ».

## 1. Postes et terminaux

| Équipement | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| PC tour | Desktop tower | Poste de travail fixe | ≈ 210 x 450 x 450 | M |
| Mini PC | Mini PC | Poste compact, souvent fixé derrière l'écran | [À COMPLÉTER] | |
| PC portable 15 pouces | Laptop | Poste mobile, Wi-Fi et Ethernet | ≈ 360 x 240 x 20 fermé | M |
| Écran 24 pouces | Monitor | Affichage du poste | [À COMPLÉTER] | |
| Clavier, souris | Keyboard, mouse | Saisie | [À COMPLÉTER] | |
| Imprimante réseau | Network printer | Impression partagée sur le réseau | [À COMPLÉTER] | |
| Smartphone, tablette | Smartphone, tablet | Clients Wi-Fi et mobiles | [À COMPLÉTER] | |
| TV connectée | Smart TV | Client réseau du salon | [À COMPLÉTER] | |
| Console de jeu | Game console | Client réseau | [À COMPLÉTER] | |
| Téléphone IP | IP phone | Téléphonie sur le réseau, souvent alimenté en PoE | [À COMPLÉTER] | |
| Caméra IP | IP camera | Vidéosurveillance, souvent alimentée en PoE | [À COMPLÉTER] | |
| Hub domotique | Smart home hub | Passerelle des objets connectés | [À COMPLÉTER] | |

## 2. Accès internet chez l'abonné

| Équipement | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| Box internet (ex. Livebox 6) | Home gateway | Modem, routeur, Wi-Fi et téléphonie en un boîtier | 183 x 183 x 80 | E |
| Prise terminale optique, PTO (ex. Acome PTO2803) | Optical outlet | Arrivée de la fibre dans le logement, passive | 100 x 80 x 27,5 | E |
| ONT fibre (ex. Fracarro ONTG-4GP2F-S) | Optical network terminal | Convertit la fibre de l'opérateur en Ethernet | 205 x 110 x 30 | E |
| Prise téléphonique, filtre DSL | Phone socket, DSL filter | Arrivée de la ligne cuivre | [À COMPLÉTER] | |
| Modem câble | Cable modem | Accès par le réseau câblé coaxial (hors périmètre du plan) | [À COMPLÉTER] | |
| Routeur 4G/5G (ex. Netgear Nighthawk M6) | Mobile router | Accès internet par le réseau mobile | 105 x 105 x 21,5 | E |
| Antenne satellite (Starlink V5) | Satellite dish | Terminal satellite de l'abonné, 35 à 50 W en moyenne | 384 x 306 x 34 | S [3] |
| Bloc d'alimentation Starlink V5 | Power supply | Alimente l'antenne | 157 x 74 x 36 | S [3] |
| Routeur Starlink Mini | Router Mini | Routeur Wi-Fi 6, 2 ports RJ45 1 Gbit/s | ≈ 138 x 83,6 x 27 (ordre des cotes incertain) | S [3] |

## 3. Réseau local domestique et petit bureau

| Équipement | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| Routeur Wi-Fi (ex. TP-Link Archer AX55) | Wi-Fi router | Routage, pare-feu simple, Wi-Fi | 261,1 x 134,5 x 41 | E |
| Switch de bureau 5 ports | 5-port desktop switch | Multiplie les prises Ethernet | [À COMPLÉTER] | |
| Switch de bureau 8 ports (ex. Netgear GS108) | 8-port desktop switch | Idem, non manageable | 158 x 102 x 27 | E |
| Switch de bureau 16 ports | 16-port desktop switch | Idem | [À COMPLÉTER] | |
| Point d'accès Wi-Fi (ex. Ubiquiti U6 Lite) | Access point | Diffuse le Wi-Fi, fixé au plafond, PoE, 12 W max | Ø 160 x 32,7 | E |
| Répéteur Wi-Fi | Wi-Fi extender | Étend la couverture, se branche sur une prise | [À COMPLÉTER] | |
| Système mesh | Mesh system | Plusieurs bornes Wi-Fi coordonnées | [À COMPLÉTER] | |
| Adaptateur CPL | Powerline adapter | Fait passer le réseau par le circuit électrique | [À COMPLÉTER] | |
| Injecteur PoE | PoE injector | Ajoute l'alimentation sur un câble Ethernet | [À COMPLÉTER] | |
| Prise murale RJ45 (Legrand Mosaic 1 module) | RJ45 wall outlet | Point de branchement dans le mur | 22,5 x 45 en façade | E |
| Coffret de communication grade 3 (Legrand 4 132 10) | Home network enclosure | Tableau VDI du logement, 6 RJ45 | 250 x 250 x 103,5 | E |
| NAS 2 baies (ex. Synology DS223) | 2-bay NAS | Stockage partagé, ≈ 17 W | 165 x 108 x 232,7 (ordre des cotes à vérifier) | E |
| NAS 4 baies (ex. Synology DS423) | 4-bay NAS | Stockage partagé, ≈ 32 W | 184 x 168 x 230 | E |
| Mini serveur | Home server | Services à domicile | [À COMPLÉTER] | |
| Onduleur domestique (ex. APC BX700U-GR, 700 VA) | Desktop UPS | Maintient l'alimentation en cas de coupure | 115 x 256 x 200 | E |
| Multiprise | Power strip | Distribution électrique | [À COMPLÉTER] | |

## 4. Baies, armoires et accessoires de baie

Le standard de la baie 19 pouces (EIA-310) est la base de tout le matériel d'entreprise. Ses cotes sont fiables :

| Cote | Valeur | Statut |
|---|---|---|
| Largeur de la face avant d'un équipement | 482,6 mm (19 pouces) | S [1] |
| Ouverture entre les montants | 450,85 mm (17,75 pouces) | S [1] |
| Hauteur d'une unité, 1U | 44,45 mm (1,75 pouce) | S [1] |
| Hauteur de façade d'un équipement de n U | 44,45 x n, moins 0,794 mm (43,66 mm pour 1U) | S [1] |
| Entraxe horizontal des trous de fixation | 465,14 mm | S [10] |
| Baie serveur courante | 42U, 600 mm de large, ≈ 914 mm de profondeur | S [1] |
| Exemple APC NetShelter SX AR3100, 42U | 600 x 1070 x 1991 | S [1] |

| Équipement | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| Baie serveur 42U | Server cabinet | Armoire fermée pour serveurs | 600 x 1070 x 1991 | S [1] |
| Baie réseau large (ex. Rittal TS 8) | Network cabinet | Armoire plus large pour le brassage | 800 x 600 x 2000 | E |
| Baie 47U ou 48U | Tall cabinet | Baie haute de datacenter | [À COMPLÉTER] | |
| Coffret mural 6U, 9U, 12U | Wall-mount cabinet | Petite baie fixée au mur | ≈ 600 x 450 x 500 pour 9U | M |
| Rack ouvert 2 montants | 2-post rack | Châssis ouvert pour équipements réseau | [À COMPLÉTER] | |
| Rack ouvert 4 montants | 4-post open frame | Châssis ouvert pour serveurs | [À COMPLÉTER] | |
| Rack 10 pouces | 10-inch rack | Mini baie pour domicile ou petit bureau, même hauteur d'unité que le 19 pouces | [À COMPLÉTER] | |
| Open Rack 21 pouces | Open Rack (OCP) | Standard des très grands datacenters, 600 de large à l'extérieur, modules de 547 | 600 x 1068 x 2286 pour la version 3 | S [1] pour 547, E pour le reste |
| Panneau de brassage cuivre 24 ports | Patch panel | Terminaison des câbles en baie, 1U | 482,6 x ≈ 100 x 44,45 | M |
| Panneau de brassage 48 ports | 48-port patch panel | Idem, 1U haute densité ou 2U | [À COMPLÉTER] | |
| Tiroir optique | Fiber enclosure | Terminaison des fibres en baie, 1U | [À COMPLÉTER] | |
| Passe-câbles horizontal | Horizontal cable manager | Guide les cordons entre deux équipements, 1U ou 2U | 482,6 x ≈ 80 x 44,45 | M |
| Passe-câbles vertical | Vertical cable manager | Guide les cordons sur la hauteur de la baie | [À COMPLÉTER] | |
| Panneau obturateur | Blanking panel | Bouche les U vides pour le flux d'air | 482,6 x ≈ 10 x 44,45 par U | M |
| Étagère, rails | Shelf, rails | Supports d'équipements | [À COMPLÉTER] | |

## 5. Équipements réseau d'entreprise

Tous les équipements rackables ont une façade de 482,6 mm et un corps d'environ 440 mm de large. Leur hauteur est un multiple de 44,45 mm. Seule la profondeur varie.

| Équipement | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| Switch d'accès 24 ports (Cisco Catalyst 9300-24T) | 24-port access switch | Raccorde les postes d'un étage, alimentation 350 W | 445 x 409 x 44, 1U (449 de profondeur avec l'alimentation) | S [7] |
| Switch d'accès 48 ports (Cisco Catalyst 9300-48T) | 48-port access switch | Idem, plus dense, 7,45 kg, alimentation 350 W | 445 x 449 x 44, 1U | S [7] |
| Switch PoE 48 ports (Cisco Catalyst 9300-48P) | PoE switch | Alimente téléphones, bornes et caméras par le câble, alimentation 1100 W | 445 x 449 x 44, 1U | S [7] |
| Switch PoE haute puissance (Cisco Catalyst 9300-48UXM) | High-power PoE switch | 48 ports, alimentation 1900 W | 445 x 485 x 44, 1U | S [7] |
| Switch de distribution | Distribution switch | Agrège les switchs d'accès | 1U ou 2U | M |
| Switch de coeur | Core switch | Dorsale du réseau | [À COMPLÉTER] | |
| Switch modulaire à châssis | Chassis switch | Châssis avec cartes de ligne et superviseurs | [À COMPLÉTER] | |
| Carte de ligne, superviseur | Line card, supervisor | Modules du châssis | [À COMPLÉTER] | |
| Switch top-of-rack (ex. Juniper QFX5120-48Y) | Top-of-rack switch | Switch en haut de chaque baie serveur, ≈ 250 W | 440,9 x 520,2 x 43,7, 1U | E |
| Switch spine, switch leaf | Spine, leaf switch | Architecture de datacenter à deux niveaux | 1U ou 2U | M |
| Routeur d'entreprise | Enterprise router | Relie le site à internet et aux autres sites | 1U ou 2U | M |
| Routeur de coeur | Core router | Routage à très haut débit | [À COMPLÉTER] | |
| Pare-feu (Palo Alto PA-3410 à PA-3440) | Firewall | Filtre le trafic entre zones, 133 à 155 W en moyenne, 7 kg | 434,9 x 361,4 x 43,2, 1U | S [8] |
| Pare-feu (Fortinet FortiGate 600F) | Firewall | Idem, ≈ 169 W en moyenne | 432 x 380 x 44,45, 1U | E |
| Équilibreur de charge | Load balancer | Répartit les requêtes entre serveurs | 1U ou 2U | M |
| Passerelle VPN | VPN gateway | Tunnels chiffrés entre sites | [À COMPLÉTER] | |
| IDS, IPS | IDS, IPS | Détection et blocage d'intrusion | [À COMPLÉTER] | |
| TAP réseau | Network TAP | Copie le trafic pour l'analyse | [À COMPLÉTER] | |
| Contrôleur Wi-Fi | Wireless controller | Pilote les bornes du site | [À COMPLÉTER] | |
| Borne Wi-Fi de plafond (Cisco Catalyst 9130AXI) | Ceiling access point | Wi-Fi d'entreprise, PoE, 25,5 à 30,5 W | 226 x 226 x 48 | E |
| Borne Wi-Fi extérieure | Outdoor access point | Wi-Fi extérieur, étanche | [À COMPLÉTER] | |
| Serveur de console (Opengear CM8100) | Console server | Accès série de secours aux équipements, 16 à 48 ports, moins de 15 W | 440 x 235 x 44, 1U | S [9] |
| Commutateur KVM | KVM switch | Partage écran, clavier et souris entre serveurs | 1U | M |
| Console KVM rackable | Rack console | Écran et clavier escamotables | 1U | M |

## 6. Serveurs et stockage

| Équipement | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| Serveur rack 1U (ex. Dell PowerEdge R650) | 1U rack server | Serveur plat, usage général | 434 à 482 x 758 à 787 x 42,8 (sources divergentes) | E |
| Serveur rack 2U | 2U rack server | Plus de disques et de cartes | 440 x ≈ 700 à 800 x 87 | M |
| Serveur rack 4U | 4U rack server | Calcul, cartes graphiques, stockage | 440 x ≈ 700 à 900 x 175 | M |
| Serveur tour | Tower server | Serveur pour petit site sans baie | [À COMPLÉTER] | |
| Châssis de lames (ex. HPE BladeSystem c7000) | Blade enclosure | Regroupe plusieurs serveurs lames | 447 x 813 x 442, 10U | E |
| Serveur lame | Blade server | Module serveur du châssis | [À COMPLÉTER] | |
| Baie de disques, JBOD (ex. Dell PowerVault ME484) | Disk shelf, JBOD | Extension de stockage, 84 disques, 64 kg à vide | 483 x 981 x 222, 5U | E |
| Baie SAN | SAN array | Stockage bloc partagé | [À COMPLÉTER] | |
| NAS rackable (ex. Synology RS2423+) | Rack NAS | Stockage fichier partagé, 12 disques, ≈ 120 W | 482 x 552 x 88, 2U | E |
| Bibliothèque de bandes | Tape library | Sauvegarde et archivage sur bande | [À COMPLÉTER] | |
| Appliance de sauvegarde | Backup appliance | Sauvegarde dédiée | [À COMPLÉTER] | |

## 7. Énergie

| Équipement | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| PDU vertical 0U | 0U vertical PDU | Bandeau de prises sur la hauteur de la baie | ≈ 55 x 45 x 1800 | M |
| PDU horizontal 1U | 1U horizontal PDU | Bandeau de prises en façade | 482,6 x ≈ 45 x 44,45 | M |
| PDU basique, mesuré, commuté | Basic, metered, switched PDU | Variantes : simple, avec mesure, avec coupure par prise | Mêmes formats | M |
| Onduleur rack 3 kVA (APC Smart-UPS SRT3000RMXLT) | Rack UPS | Secours électrique en baie, 2700 W, 31,3 kg | 432 x 635 x 85, 2U | E |
| Onduleur rack 5 à 6 kVA (Eaton 9SX) | Rack UPS | Secours électrique, 4,5 à 5,4 kW | 440 x 684 x 130, 3U | E |
| Onduleur tour | Tower UPS | Secours pour petite salle | [À COMPLÉTER] | |
| Onduleur de salle | Room UPS | Secours pour toute la salle, plusieurs armoires | [À COMPLÉTER] | |
| Armoire de batteries | Battery cabinet | Autonomie de l'onduleur | [À COMPLÉTER] | |
| Commutateur de transfert, ATS et STS | Transfer switch | Bascule entre deux sources électriques | 1U pour un ATS de baie | M |
| Tableau électrique | Electrical panel | Distribution et protection des circuits | [À COMPLÉTER] | |
| Canalisation préfabriquée | Busbar | Distribution électrique au-dessus des baies | [À COMPLÉTER] | |
| Groupe électrogène | Generator | Secours longue durée | [À COMPLÉTER] | |

## 8. Refroidissement et infrastructure de salle

| Équipement | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| Armoire de climatisation, CRAC (Vertiv Liebert PX023) | CRAC unit | Refroidit la salle, 23 kW de froid, 304 kg | 876 x 879 x 1970 | E |
| Armoire de climatisation à eau glacée, CRAH | CRAH unit | Idem, raccordée à un réseau d'eau glacée | [À COMPLÉTER] | |
| Climatisation en rangée | In-row cooling | Unité de froid intercalée entre les baies | [À COMPLÉTER] | |
| Confinement d'allée chaude ou froide | Aisle containment | Portes et toit qui séparent air chaud et air froid | [À COMPLÉTER] | |
| Dalle de faux plancher | Raised floor tile | Plancher technique, câbles et air en dessous | 600 x 600 x 32 à 42 en Europe, 610 x 610 aux États-Unis | E, S [1] pour 610 |
| Dalle perforée | Perforated tile | Laisse passer l'air froid devant les baies | 600 x 600 x 38 | E |
| Détection et extinction incendie | Fire detection, suppression | Détecteurs, buses et bouteilles de gaz | [À COMPLÉTER] | |
| Contrôle d'accès | Access control | Lecteur de badge, porte sécurisée | [À COMPLÉTER] | |
| Caméra de surveillance | CCTV camera | Surveillance de la salle | [À COMPLÉTER] | |
| Sonde de température et d'humidité | Environmental sensor | Supervision de la salle | [À COMPLÉTER] | |

## 9. Câbles

### Cuivre

| Câble | Débit et distance | Statut |
|---|---|---|
| Cat 5e | 1 Gbit/s sur 100 m, 100 MHz | E |
| Cat 6 | 1 Gbit/s sur 100 m, 10 Gbit/s sur environ 55 m, 250 MHz | E |
| Cat 6A | 10 Gbit/s sur 100 m, 500 MHz | E |
| Cat 7 | 600 MHz selon certaines sources, 1 GHz pour la Cat 7A. Valeurs divergentes | E |
| Cat 8 | 25 et 40 Gbit/s sur 30 m, 2 GHz | E |
| Blindage | Cat 5e, 6, 6A et 8.1 blindés : en général F/UTP. Cat 7, 7A et 8.2 : S/FTP | S [2] |
| Diamètre extérieur des câbles | [À COMPLÉTER] | |
| Câble droit, câble croisé | Brochages T568A et T568B. [À COMPLÉTER] | |
| Coaxial RG-6 | 75 ohms, diamètre 6,8 à 7 mm selon le fabricant | E |
| Coaxial RG-59 | 75 ohms, diamètre 6,1 à 6,15 mm | E |
| Câble téléphonique | [À COMPLÉTER] | |
| Câble console | Rollover RJ45, RJ45 vers DB9, USB. [À COMPLÉTER] | |
| Câble DAC twinax | Liaison courte entre ports SFP+ ou QSFP. [À COMPLÉTER] | |
| Cordon d'alimentation | C13/C14 et C19/C20. [À COMPLÉTER] | |

### Fibre optique

| Fibre | Caractéristiques | Statut |
|---|---|---|
| OM1 | Multimode, coeur 62,5 µm, gaine orange, 10 Gbit/s sur environ 33 m | E |
| OM2 | Multimode, coeur 50 µm, gaine orange, 10 Gbit/s sur environ 82 m | E |
| OM3 | Multimode, coeur 50 µm, gaine aqua, 10 Gbit/s sur 300 m, 40 et 100 Gbit/s sur 100 m | E |
| OM4 | Multimode, coeur 50 µm, gaine aqua (violet selon une source), 10 Gbit/s sur 550 m, 40 et 100 Gbit/s sur 150 m | E |
| OM5 | Multimode, coeur 50 µm, gaine vert citron | E |
| OS1, OS2 | Monomode, longue distance. Gaine jaune d'usage, non confirmée par une source | M |
| Jarretière optique | Cordon court entre deux équipements. [À COMPLÉTER] | |
| Câble trunk MPO/MTP | 12 ou 24 fibres par connecteur | E |
| Câble breakout | Un connecteur multifibre vers plusieurs connecteurs simples. [À COMPLÉTER] | |
| Fibre extérieure | Aérienne, enterrée, armée. Nombre de fibres et diamètres : [À COMPLÉTER] | |

## 10. Connecteurs et modules

| Élément | Caractéristiques | Statut |
|---|---|---|
| RJ45 (8P8C) | Connecteur Ethernet cuivre. Dimensions : [À COMPLÉTER] | |
| RJ11 | Connecteur téléphonique. [À COMPLÉTER] | |
| LC | Fibre, férule 1,25 mm, petit format | E |
| SC | Fibre, férule 2,5 mm, enfichable | E |
| ST | Fibre, férule 2,5 mm, baïonnette | E |
| FC | Fibre, férule 2,5 mm, à vis | E |
| MPO/MTP | Fibre, 4 à 24 fibres par connecteur | S [5] |
| Polissage APC | Angle de 8 degrés en général, corps ou manchon vert | S [5] |
| Polissage UPC | Bleu d'usage, non confirmé par une source lue | E |
| Connecteur F, BNC | Coaxial. [À COMPLÉTER] | |
| Noyau keystone | Prise encliquetable pour panneau ou plastron. [À COMPLÉTER] | |
| Module SFP | 1 Gbit/s, ≈ 45 x 13,7 x 8,6 | E |
| Module SFP+ | 10 Gbit/s, même format que le SFP | E |
| Module SFP28 | 25 Gbit/s, même format que le SFP | E |
| Module QSFP+ | 40 Gbit/s, ≈ 52,4 x 18,35 x 8,5 | E |
| Module QSFP28 | 100 Gbit/s, même format que le QSFP+ | E |
| Module QSFP-DD | 400 Gbit/s et plus, 8 voies | E |
| Module GBIC | Ancien format 1 Gbit/s. [À COMPLÉTER] | |
| Types de modules | SR, LR, ER, BiDi, cuivre. [À COMPLÉTER] | |

## 11. Cheminement des câbles

| Élément | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| Chemin de câbles en fil | Wire mesh cable tray | Supporte les câbles au plafond ou sous plancher | Hauteur 50, largeur 150 à 600 (un fabricant) | E |
| Chemin de câbles perforé | Perforated cable tray | Idem, en tôle | Largeurs de 50 à 1000 | E |
| Échelle à câbles | Cable ladder | Supporte les gros faisceaux | Largeur 200 à 600, flanc 100 (un fabricant) | E |
| Goulotte | Cable trunking | Chemin fermé en apparent, mur ou plafond | [À COMPLÉTER] | |
| Plinthe technique, moulure | Skirting trunking | Chemin discret en bas de mur | [À COMPLÉTER] | |
| Conduit, gaine annelée | Conduit | Protège les câbles dans les murs et dalles | [À COMPLÉTER] | |
| Fourreau enterré | Underground duct | Protège les câbles sous terre, PVC ou PEHD | [À COMPLÉTER] | |
| Anneau de guidage | D-ring | Guide les cordons en baie | [À COMPLÉTER] | |
| Colonne et perche de distribution | Service pole | Descend les câbles du plafond au bureau | [À COMPLÉTER] | |
| Boîte de sol | Floor box | Prises encastrées dans le sol | [À COMPLÉTER] | |

## 12. Terminaison et raccordement

| Élément | Nom anglais | Rôle | Statut |
|---|---|---|---|
| Boîtier d'épissurage | Splice closure | Protège les soudures entre deux câbles fibre | [À COMPLÉTER] |
| Cassette de soudure | Splice tray | Range les soudures dans un boîtier ou un tiroir | [À COMPLÉTER] |
| Coupleur optique passif | Optical splitter | Partage une fibre vers plusieurs abonnés, de 1:2 à 1:64 | [À COMPLÉTER] |
| Atténuateur optique | Attenuator | Réduit la puissance du signal | [À COMPLÉTER] |
| Boîtier de raccordement | Junction box | Jonction de câbles cuivre | [À COMPLÉTER] |

## 13. Outils

Objets potentiels du jeu, sans dimension à ce stade.

| Outil | Nom anglais | Usage |
|---|---|---|
| Pince à sertir | Crimping tool | Pose les connecteurs RJ45 |
| Outil d'impact | Punch-down tool | Raccorde les fils sur un panneau ou une prise |
| Dénudeur | Cable stripper | Retire la gaine |
| Testeur de câble | Cable tester | Vérifie la continuité et le brochage |
| Certificateur | Cable certifier | Valide un lien par rapport à sa catégorie |
| Générateur de tonalité | Tone generator | Retrouve un câble dans un faisceau |
| Soudeuse fibre | Fusion splicer | Soude deux fibres |
| Cliveuse | Fiber cleaver | Coupe la fibre proprement avant soudure |
| Réflectomètre | OTDR | Localise un défaut sur une fibre |
| Photomètre | Optical power meter | Mesure la puissance reçue |
| Stylo laser | Visual fault locator | Repère une fibre ou une cassure à la lumière rouge |

## 14. Réseau d'opérateur : fibre jusqu'à l'abonné

Chaîne, du coeur vers l'abonné : NRO, puis point de mutualisation, puis point de branchement, puis prise terminale et ONT.

| Équipement | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| Noeud de raccordement optique, NRO | Central office | Local de l'opérateur d'où partent les fibres | [À COMPLÉTER] | |
| OLT | Optical line terminal | Équipement actif du NRO qui dessert les abonnés | [À COMPLÉTER] | |
| Répartiteur optique, ODF | Optical distribution frame | Brassage des fibres au NRO | [À COMPLÉTER] | |
| Point de mutualisation, armoire de rue (Grolleau PM360 2x28U) | Street cabinet | Répartit les fibres des opérateurs vers un quartier, passif | 1600 x 350 x 1625 | E |
| Armoire de rue (Idea Optical iBER-1635 2x34U) | Street cabinet | Idem | 1886 x 368 x 1630 | E |
| Point de branchement optique, PBO | Distribution point | Dernier boîtier avant le logement : façade, poteau, chambre ou palier | [À COMPLÉTER] | |
| Boîtier de protection d'épissures, BPE | Splice closure | Jonction étanche entre câbles | [À COMPLÉTER] | |
| Colonne montante, boîtier d'étage | Riser, floor box | Distribution verticale en immeuble | [À COMPLÉTER] | |
| Coupleur GPON, XGS-PON | PON splitter | Partage une fibre entre plusieurs abonnés | [À COMPLÉTER] | |
| Prise terminale optique, PTO | Optical outlet | Voir section 2 | 100 x 80 x 27,5 | E |

## 15. Réseau d'opérateur : cuivre et câble

Le réseau câblé coaxial (tête de réseau, noeud optique, amplificateurs) est hors périmètre du plan des lots : ces lignes sont gardées pour référence.

| Équipement | Nom anglais | Rôle | Statut |
|---|---|---|---|
| Central téléphonique, NRA | Telephone exchange | Local d'où partent les lignes cuivre | [À COMPLÉTER] |
| Répartiteur général | Main distribution frame | Brassage des paires cuivre au central | [À COMPLÉTER] |
| DSLAM | DSLAM | Équipement actif de l'ADSL et du VDSL | [À COMPLÉTER] |
| Sous-répartiteur, armoire de rue | Cross-connect cabinet | Répartition des paires dans un quartier | [À COMPLÉTER] |
| Point de concentration, PC | Distribution point | Dernier boîtier cuivre avant l'abonné, en façade ou sur poteau ; le câble de distribution commun à plusieurs abonnés arrive en amont | E |
| Tête de réseau, CMTS | Headend, CMTS | Équipement actif du réseau câblé | [À COMPLÉTER] |
| Noeud optique, amplificateur, dérivateur | Optical node, amplifier, tap | Éléments du réseau coaxial | [À COMPLÉTER] |

## 16. Génie civil et réseau aérien

| Équipement | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| Chambre de tirage L0T | Pull box L0T | Regard sous trottoir pour tirer et raccorder les câbles | ≈ 500 x 350 x 190 (lecture de schéma imprécise) | S [4] |
| Chambre de tirage L1T | Pull box L1T | Idem, 24 entrées de gaine, 30 kg | 650 x 500 x 562, intérieur 520 x 380 | S [4] |
| Chambre de tirage L2T | Pull box L2T | Chambre longue, 36 entrées de gaine, 50 kg | 1290 x 510 x 562, intérieur 1160 x 380 | S [4] |
| Chambres L3T, K1C, K2C | Concrete chambers | Chambres en béton, plus grandes | [À COMPLÉTER] | |
| Tampon | Manhole cover | Couvercle de chambre | [À COMPLÉTER] | |
| Fourreau, tranchée | Duct, trench | Passage enterré des câbles | [À COMPLÉTER] | |
| Poteau télécom bois, métal, composite | Telecom pole | Support du réseau aérien | [À COMPLÉTER] | |
| Poteau électrique partagé | Shared utility pole | Support commun électricité et télécom | [À COMPLÉTER] | |
| Câble aérien | Aerial cable | Câble tendu entre poteaux | [À COMPLÉTER] | |
| Pince d'ancrage, traverse | Anchor clamp, crossarm | Fixation des câbles sur le poteau | [À COMPLÉTER] | |
| Boîtier sur poteau | Pole-mounted closure | PBO ou boîtier d'épissures en hauteur | [À COMPLÉTER] | |
| Love de câble | Cable slack loop | Réserve de câble enroulée sur le poteau | [À COMPLÉTER] | |
| Armoire de rue active | Active street cabinet | Armoire avec équipements alimentés | [À COMPLÉTER] | |
| Shelter, local préfabriqué | Shelter | Local technique posé sur site | [À COMPLÉTER] | |
| Datacenter conteneurisé | Containerized datacenter | Salle complète dans un conteneur | [À COMPLÉTER] | |

## 17. Réseau mobile

| Équipement | Nom anglais | Rôle | Dimensions réelles | Statut |
|---|---|---|---|---|
| Pylône monotube | Monopole tower | Support d'antennes | Hauteur 20 à 45 m (fourchette commerciale) | E |
| Pylône treillis | Lattice tower | Support d'antennes, plusieurs opérateurs | Jusqu'à 100 m (fourchette commerciale) | E |
| Antenne panneau 4G/5G | Panel antenna | Émet vers un secteur | [À COMPLÉTER] | |
| Antenne active massive MIMO | Massive MIMO antenna | Antenne 5G avec radio intégrée | [À COMPLÉTER] | |
| Tête radio, RRU ou RRH | Remote radio unit | Radio déportée près de l'antenne | [À COMPLÉTER] | |
| Unité de bande de base, BBU | Baseband unit | Traitement du signal au pied du site | [À COMPLÉTER] | |
| Faisceau hertzien | Microwave link | Liaison radio point à point par parabole | [À COMPLÉTER] | |
| Small cell | Small cell | Petite antenne de proximité | [À COMPLÉTER] | |
| Armoire de site radio | Radio site cabinet | Énergie et équipements au pied du pylône | [À COMPLÉTER] | |

## 18. Longue distance, satellite et interconnexion

| Équipement | Nom anglais | Rôle | Statut |
|---|---|---|---|
| Antenne VSAT | VSAT dish | Liaison satellite professionnelle | [À COMPLÉTER] |
| Station terrienne | Ground station | Passerelle entre satellites et réseau terrestre | [À COMPLÉTER] |
| Câble sous-marin, station d'atterrissement | Submarine cable, landing station | Liaisons intercontinentales | [À COMPLÉTER] |
| Répéteur optique, amplificateur EDFA | Optical repeater, EDFA | Régénère le signal sur longue distance | [À COMPLÉTER] |
| Multiplexeur DWDM, ROADM | DWDM mux, ROADM | Fait passer plusieurs longueurs d'onde sur une fibre | [À COMPLÉTER] |
| Point d'échange internet, IXP | Internet exchange | Lieu où les réseaux s'interconnectent | [À COMPLÉTER] |
| Salle d'interconnexion | Meet-me room | Salle de brassage entre opérateurs | [À COMPLÉTER] |
| Point de présence, PoP | Point of presence | Site d'un opérateur dans une ville | [À COMPLÉTER] |
| BNG, BRAS | Broadband gateway | Authentifie et agrège les abonnés | [À COMPLÉTER] |
| CGNAT | Carrier-grade NAT | Partage des adresses IPv4 entre abonnés | [À COMPLÉTER] |

## Valeurs de référence utiles au jeu

### Alimentation par Ethernet (PoE)

Utile pour le système d'énergie du mod. Valeurs lues sur Wikipedia [6], sauf mention.

| Norme | Puissance fournie par le port | Puissance disponible à l'appareil | Statut |
|---|---|---|---|
| 802.3af, type 1 | 15,4 W | 12,95 W | S [6] |
| 802.3at, type 2 | 30 W | 25,5 W | S [6] |
| 802.3bt, type 3 | 60 W | 51 W | S [6] pour 51 W, E pour 60 W |
| 802.3bt, type 4 | 90 W | 71,3 W | S [6] |

### Ethernet sur cuivre

| Norme | Débit | Câble et distance | Statut |
|---|---|---|---|
| 10BASE-T | 10 Mbit/s | Cat 3 ou plus, 100 m | E |
| 100BASE-TX | 100 Mbit/s | Cat 5 ou plus, 100 m | E |
| 1000BASE-T | 1 Gbit/s | Cat 5 ou plus, 100 m | E |
| 10GBASE-T | 10 Gbit/s | 55 m en Cat 6, 100 m en Cat 6A | E |
| 25GBASE-T, 40GBASE-T | 25 et 40 Gbit/s | Cat 8, 30 m | E |

## Ce qui reste à vérifier

- Les lignes en statut E et M, uniquement au moment de modéliser l'équipement concerné (voir « Comment lire ce document »).
- Les dimensions du point de concentration cuivre.
- L'ordre des cotes (largeur, profondeur, hauteur) pour les NAS, l'onduleur et le routeur Starlink Mini.
- Les valeurs contradictoires selon les sources : couleur de gaine OM4, fréquence de la Cat 7, diamètres des coaxiaux.
- Les dimensions de tout ce qui est en extérieur : poteaux, PBO, antennes mobiles, chambres en béton.
- Les consommations des switchs et serveurs dépendent de la configuration : les valeurs citées sont des puissances d'alimentation ou des moyennes constructeur, pas des garanties.
- Dans les sections 5 à 8, les équipements sans modèle cité (switch de coeur, châssis, routeurs, PDU, batteries, confinement) n'ont pas été recherchés.

Documents repérés mais non lus, à consulter en priorité avant les lots 4 et 5 pour compléter le réseau d'opérateur : le guide CREDO 2018 d'Acome sur les composants du réseau d'accès, le recueil de spécifications ARCEP pour les zones moins denses (version du 29/06/2026) et le catalogue Nexans « central office » 2024.

## Sources

Pages lues :

1. Wikipedia, « 19-inch rack » : https://en.wikipedia.org/wiki/19-inch_rack
2. Wikipedia, « Twisted pair » : https://en.wikipedia.org/wiki/Twisted_pair
3. Starlink, fiche de spécifications V5 : https://starlink.com/public-files/specification_sheet_starlink_V5.pdf
4. Groupe Cahors, catalogue réseaux secs 2020, pages 4 à 6 : https://www.groupe-cahors.com/sites/default/files/cahors-reseaux-secs-2020.pdf
5. Wikipedia, « Optical fiber connector » : https://en.wikipedia.org/wiki/Optical_fiber_connector
6. Wikipedia, « Power over Ethernet » : https://en.wikipedia.org/wiki/Power_over_Ethernet
7. Cisco, fiche technique Catalyst 9300 : https://www.cisco.com/c/en/us/products/collateral/switches/catalyst-9300-series-switches/nb-06-cat9300-ser-data-sheet-cte-en.pdf
8. Palo Alto Networks, spécifications physiques et électriques PA-3400 : https://docs.paloaltonetworks.com/hardware/pa-3400-hardware-reference/pa-3400-series-specifications
9. Opengear, fiche technique CM8100 : https://resources.opengear.com/cm/datasheets/cm8100/
10. Wikipedia, « Rack unit » : https://en.wikipedia.org/wiki/Rack_unit

Résultats de recherche, pages non ouvertes (statut E) :

- TP-Link Archer AX55 : https://www.tp-link.com/baltic/products/details/archer-ax55.html
- Netgear GS108 : https://www.netgear.com/uk/business/wired/switches/unmanaged/gs108/
- Ubiquiti U6 Lite : https://cdn.panacompu.com/doc/pd/u6-lite-ds.pdf
- Netgear Nighthawk M6 : https://kb.netgear.com/000065402/What-are-the-specifications-of-NETGEAR-Nighthawk-mobile-hotspots
- Livebox 6 : https://www.lafibreoptique.fr/offre-fibre/orange/livebox-6
- Synology DS223 : https://www.synology.com/en-eu/products/DS223
- Synology DS423 : https://www.synology.com/en-eu/products/DS423
- APC BX700U-GR : https://gho.ie/product/details/apc/power-components-%26-protection/bx700u-gr/0280F011
- Legrand, coffret de communication 4 132 10 : https://assets.legrand.com/pim/NP-FT-GT/S000126011FR-00.pdf
- Legrand, prise RJ45 Mosaic 076562 : https://www.domomat.com/16482-legrand-076562-prise-rj45-lcs-prog-mosaic-blanc-categorie-6-ftp-1-module-legrand-076562.html
- Acome PTO2803 : https://www.batiproduits.com/amp/fiche/produits/chambres-de-tirage-p381319340.html
- Fracarro, ONT GPON : https://fracarro.com/wp-content/uploads/product-downloadable-contents/datasheet/287794_ONTG-8GP-M_fr_ds.pdf
- Grolleau, armoire PM360 2x28U : https://lafibre.info/images/pon/202105_grolleau_armoire_ftth_pm360_2x28u.pdf
- Idea Optical, armoire iBER-1635 : https://lafibre.info/images/dsp/202411_idea-optical_iber_1635_outdoor_g2_2x34u.pdf
- Pylônes de téléphonie mobile : https://conseils.hellopro.fr/quels-sont-les-types-de-pylones-de-telephonie-mobile-3984.html
- Catégories de câbles Ethernet : https://www.ampcom.com/blogs/industry-information/ethernet-cable-categories-cat5e-to-cat8-explained
- Fibres multimodes OM1 à OM5 : https://network-switch.com/blogs/networking/multimode-fiber-types-2026-guide
- Connecteurs fibre : https://www.layer23-switch.com/blog/fiber-optic-connector-types.html
- Modules SFP et QSFP : https://www.router-switch.com/faq/sfp-sfp-sfp28-qsfp-qsfp28-guide.html
- Dimensions des modules : https://internet.watch.impress.co.jp/docs/column/nettech/1269068.html
- Normes PoE : https://community.fs.com/article/understanding-poe-standards-and-poe-wattage.html
- Coaxiaux RG-6 et RG-59 : https://media.iewc.com/SpecificationSheets/Southwire_coaxial_SS.pdf
- Chemins de câbles : https://meandersquare.co.za/Tue-14-Sep-2021-11835.html
- Normes Ethernet cuivre : https://kb.wisc.edu/ns/7829
- Juniper QFX5120 : https://www.juniper.net/us/en/products/switches/qfx-series/qfx5120-data-center-switches/specs.html
- Fortinet FortiGate 600F : https://www.shi.com/product/44900362/Fortinet-FortiGate-600F
- Cisco Catalyst 9130AXI : https://netmode.com/products/cisco-catalyst-c9130axi-ewc-b-series-with-embedded-wireless-controller-mobility-express-new
- Dell PowerEdge R650 : https://www.racksolutions.com/news/blog/dell-poweredge-r650-specs-and-rack-compatibility/
- Dell PowerVault ME484 : https://www.stikc.com/storage/dell-powervault-san-das/powervault-me5084.html
- Synology RS2423+ : https://nasshops.net/home/205-274-synology-rs2423.html
- HPE BladeSystem c7000 : https://www.manualslib.com/manual/642057/Hp-C-Class.html?page=43
- APC Smart-UPS SRT3000RMXLT : https://cdn.blueally.com/apcguard/datasheets/SRT3000RMXLT_datasheet.pdf
- Eaton 9SX : https://eaton.com/content/dam/eaton/products/backup-power-ups-surge-it-power-distribution/backup-power-ups/eaton-9sx-ups/eaton-9sx-5-11kva-ups-datasheet-en-us-east-asia.pdf
- Vertiv Liebert PX023 : https://www.vertiv.com/en-us/products-catalog/thermal-management/room-cooling/px023-23kw/
- Rittal TS 8 : https://www.rittal.com/pdf-creator/variant/com-en/8806750
- Dalles de faux plancher : https://source.thenbs.com/en-us/gb/product/beh600pbt-heavy-grade-porcelain-edge-banded-panel/deWJdesYthhtqkzvTneCzd/h5xPg6gCNF82zv4FWFMw64
- Open Rack version 3 : https://www.opencompute.org/products/440/rittal-open-rack-v3-orv3
- Point de concentration, OVHcloud : https://docs.ovhcloud.com/fr/guides/web-cloud/internet/internet-access/la-desserte-interne
- Point de concentration, avis Arcep DEX150505 : https://arcep.fr/fileadmin/reprise/dossiers/degroup/avis-DEX150505-VDSL2_au_PC-050515.pdf
