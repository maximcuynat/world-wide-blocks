# Messages

Audit des messages des lots 1a et 1b-1 (2026-11-18), puis conception du module des messages. Statut : **validé par Max le 2026-11-18**, avec ces décisions :

- Réseau saturé : prévenir les opérateurs connectés, plus le propriétaire du monde en solo.
- Symboles des niveaux : d'accord, à vérifier à l'œil, avec un repli en texte simple (réglage du client).
- Ligne « Accroupi : détails » gardée dans le panneau compact.
- Textes de `/netcraft test` en dur et en français.
- Vouvoiement partout, comme la traduction française de Minecraft.
- Distances arrondies à l'entier le plus proche (« 4 blocs »), dans toutes les langues.
- **Données séparées du dessin** : le serveur envoie clé, valeurs et niveau ; le client décide seul de l'apparence (symbole, couleur, place). Une future interface HUD soignée (lot de finition) doit pouvoir remplacer le dessin sans toucher au moteur ni aux messages.

## 1. Audit

### Barre d'action (au-dessus de la barre d'objets)

Tous envoyés par `EthernetCableItem.tell()` (`player.sendOverlayMessage`), au seul joueur qui fait le geste, une fois par geste. Chaque envoi est aussi tracé en `[ESSAI 1b-1]` (DEBUG, en français, texte rendu par le serveur).

| Clé (`message.netcraft.cable.…`) | Texte français actuel | Déclencheur |
|---|---|---|
| `start` | Extrémité 1 : %s, port %s. Clic droit sur le second appareil (accroupi + clic dans le vide pour annuler). | Premier clic, cordon en main |
| `no_free_port` | %s : aucun port libre. | Premier clic sur un appareil plein |
| `cancelled` | Branchement annulé. | Accroupi + clic dans le vide, branchement commencé |
| `start_gone` | La première extrémité n'existe plus : branchement annulé. | Second clic, ou chaque tick d'inventaire, si le départ a disparu |
| `cancelled_dimension` | Changement de dimension : branchement annulé, le cordon est libre. | Tick d'inventaire après un changement de dimension |
| `start_taken` | Le port de la première extrémité est désormais occupé : branchement annulé. | Second clic, port de départ pris entre-temps |
| `connected` | Connecté : %s port %s ↔ %s port %s (%s blocs). | Second clic réussi |
| `unplugged` | %s : port %s débranché. | Accroupi + clic, cordon en main ; ou bout retiré d'un câble posé en créatif, en survie |
| `nothing_to_unplug` | %s : aucun câble branché. | Accroupi + clic sur un appareil sans câble |
| `end_taken` | %s : bout débranché, l'autre reste sur %s port %s. Clic droit sur un appareil pour le rebrancher, accroupi + clic dans le vide pour le ranger. | Accroupi + clic main vide |
| `retracted` | %s : câble débranché, le cordon est libre dans ta main. | Clic sur l'appareil où l'autre bout est branché |
| `result.not_a_device` | Ce n'est pas un appareil réseau. | Refus du moteur au second clic |
| `result.incompatible_port` | Ce port n'accepte pas ce câble. | Idem |
| `result.same_device` | Choisissez un autre appareil que le premier. | Idem |
| `result.other_dimension` | Les deux appareils doivent être dans la même dimension. | Idem |
| `result.too_far` | Trop loin : %s blocs, %s au maximum. | Idem |
| `result.port_in_use` | Aucun port libre sur cet appareil. | Idem |
| `result.ok` | Connecté. | Jamais affiché |

### Chat

| Clé | Texte français actuel | Déclencheur, destinataire, fréquence |
|---|---|---|
| `message.netcraft.network.loop` | Boucle réseau : une trame diffusée revient à %s [%s, %s, %s] par le port %s. Elle a été supprimée ; retirez un des câbles de la boucle. | Trame revenue à un switch (`SimulationEvent.LoopDetected`). Joueurs à moins de 32 blocs de l'appareil qui la voit. Au plus une fois par minute **et par appareil** |
| `commands.netcraft.links.*` | %s lien(s) réseau : … | Réponse à `/netcraft liens`, à qui a tapé la commande |
| (texte en dur) | Décor « … » : …, Commande réservée aux mondes d'essai : ce monde s'appelle « … », Aucun décor à retirer depuis ta connexion., … | Réponses à `/netcraft test`, développement seulement (jeu de sources `gametest`) |

### Panneau d'état (client, appareil visé)

Toujours détaillé : le nom, puis une ligne par port avec un voyant (vert connecté, gris libre, gris foncé inactif), et pour un poste une ligne `MAC …` sous chaque port.

| Clé (`hud.netcraft.…`) | Texte français |
|---|---|
| `port.connected` | Port %s : connecté |
| `port.free` | Port %s : libre |
| `port.line` | Port %s (ligne) : inactif |
| `port.connected_learned` | Port %s : connecté, %s |
| `mac` | MAC %s |
| `learned_more` | %s (+%s) |

### Log

| Niveau | Texte | Où |
|---|---|---|
| INFO | NetCraft chargé | `NetCraft` |
| WARN | Lien réseau incohérent ignoré au chargement : {} | `NetworkSavedData` |
| WARN | Réseau saturé : plus de {} trames en attente, de nouvelles trames ont été perdues. | `NetworkManager`, au plus une par minute ; aucun joueur prévenu |
| WARN | Boucle réseau : une trame diffusée revient à {} ({}) par le port {}. Copie supprimée. | `NetworkManager`, une par appareil et par minute |
| DEBUG | `[ESSAI 1b-1]` message envoyé par le cordon | `EthernetCableItem.tell` |
| DEBUG | `[ESSAI 1b-1]` appareil enregistré, câble branché, câble débranché, appareil retiré | `NetworkManager` |
| DEBUG | `[ESSAI 1b-1]` chargement : résumé, puis un appareil et un câble par ligne | `NetworkSavedData` |
| DEBUG | `[ESSAI 1b-1]` ce qu'affiche l'indicateur de chaque joueur, à chaque changement | `NetworkSync` |
| DEBUG | `[ESSAI 1b-1]` pose d'un bloc avec sa façade | `NetworkDeviceBlock` |
| DEBUG | `[ESSAI 1b-1]` MAC apprises et oubliées, relevé chaque seconde | `TrialLog.compareLearned` |
| INFO | [GameTest] signalements de boucle reçus … | tests en jeu |
| (exceptions) | Nombre de ports négatif, Limites invalides, Adresse MAC hors de 48 bits, … (une dizaine) | Erreurs de programmation, qui finissent dans le log ou un rapport de plantage |

### Défauts relevés

1. **Boucle : deux messages pour un seul problème.** Le signalement est limité par appareil : deux box en boucle donnent deux messages dans le chat, puis deux par minute.
2. **Log en deux langues.** Phrases en français, nom de l'appareil rendu par le serveur (en anglais, ou la clé brute), traces d'essai en français.
3. **Doublon** : `no_free_port` et `result.port_in_use` disent la même chose. `result.ok` n'est jamais affiché.
4. **Trop long pour la barre d'action**, qui s'efface en 3 s : `end_taken` (environ 150 caractères), `start` (environ 100).
5. **Tutoiement et vouvoiement mélangés** : « dans ta main », « ta connexion » d'un côté, « Choisissez », « retirez » de l'autre.
6. **Nombres** : les longueurs sont formatées par le serveur avec un point (`4.0 blocs` dans un jeu en français).
7. **Aucun niveau** : un refus et une réussite ont la même apparence dans la barre d'action.
8. **Panneau toujours détaillé** : 6 lignes pour la box, une ligne de MAC par port pour un poste.
9. **Réseau saturé** : seulement dans le log, le joueur n'en sait rien.
10. **Format des appareils** : `%s [%s, %s, %s]` recopié dans chaque texte (boucle, `/netcraft liens`), autrement dans les traces.
11. **Tests** : les messages ne sont captés qu'en surchargeant le joueur de test (`TestPlayer.sendSystemMessage`). Le log n'est pas vérifiable, et le test de boucle lit les coordonnées par position dans les arguments.

## 2. Conception proposée

### Usages, niveaux et canaux

Chaque message déclare un **usage** et un **niveau** ; le canal en découle, le code appelant ne le choisit pas.

| Usage | Niveau | Canal |
|---|---|---|
| Geste (réponse immédiate au joueur qui agit) | tous | Barre d'action du joueur, plus une ligne DEBUG dans le log |
| Événement réseau | avertissement, erreur | Chat des joueurs concernés, plus une ligne WARN ou ERROR dans le log |
| Événement réseau | information, succès | Log seulement (INFO) |
| Diagnostic | (aucun) | Log, DEBUG |
| Appareil visé | (voyants) | Panneau d'état, côté client |

Exception assumée : la réponse à une commande (`/netcraft liens`, `/netcraft test`) reste dans le chat de qui l'a tapée, comme toutes les commandes de Minecraft. Les textes de `/netcraft test` restent en dur, en français : la commande n'existe qu'en développement (jeu de sources `gametest`, absent du jar), et ses clés n'ont pas leur place dans les fichiers de langue distribués. Seul « ta connexion » passe au vouvoiement.

Niveau d'un geste : la réussite est un **succès** ; une étape ou une annulation voulue par le joueur, une **information** ; une annulation causée par autre chose (départ disparu, changement de dimension, port pris entre-temps), un **avertissement** ; un geste refusé, une **erreur**.

| Niveau | Couleur | Symbole |
|---|---|---|
| Information | blanc | ℹ |
| Succès | vert | ✔ |
| Avertissement | jaune | ⚠ |
| Erreur | rouge | ✖ |

Le symbole et la couleur sont choisis par le client à partir du niveau reçu : ils ne sont ni dans les fichiers de langue ni envoyés par le serveur. Repli : un réglage du client (`netcraft-client.toml`, `messageSymbols = false`) remplace le symbole par un mot traduit (« Info », « OK », « Attention », « Erreur »). Rendu des symboles à vérifier à l'œil.

### Anti-répétition

- Un événement réseau porte une **clé de problème**. Le module ne le transmet qu'une fois par minute de jeu pour une même clé (valeur déjà validée), quel que soit le nombre d'appareils qui le voient. Le temps est celui du tick de la simulation (pas d'écart possible avec l'horloge corrigée le 2026-11-18).
- La boucle a pour clé le **réseau** où elle se trouve (l'ensemble des appareils reliés par des câbles, désigné par son plus petit identifiant d'appareil). Le moteur regroupe toutes les détections d'un même tick par réseau et émet un seul événement, avec la liste des appareils et ports où les trames sont revenues. La limite par minute passe du moteur au module : le moteur constate, le module décide qui prévenir et quand.
- **Oubli des clés de boucle** : à chaque changement d'un réseau (appareil posé ou retiré, câble branché ou débranché), la clé de boucle de ce réseau est oubliée, avant comme après le changement (un câble retiré peut le couper en deux, un câble ajouté en réunir deux). Les autres réseaux gardent leur limite : un joueur qui bricole ailleurs ne relance pas les messages d'une boucle qu'il ne touche pas. Retirer un câble est justement le geste qui répare une boucle : si elle est toujours là, le joueur le sait au signalement suivant au lieu d'attendre la minute. Cela couvre aussi `/netcraft test effacer` puis `boucle` au même endroit, et un réseau dont le plus petit identifiant change (ce qui n'arrive que par un changement du réseau). Fréquence maximale : une fois par minute, plus une fois par geste du joueur.
- Réseau saturé : clé unique, au plus un message par minute, jamais oubliée avant.
- La barre d'action n'a pas besoin d'anti-répétition : un message remplace le précédent, et chacun répond à un clic.

### Destinataires

- Boucle : chaque joueur à moins de 32 blocs d'un des appareils touchés reçoit **un** message, qui nomme l'appareil touché le plus proche de lui.
- Réseau saturé : le problème est global au serveur. Proposition : les opérateurs connectés, plus le propriétaire du monde en solo. Vérifié dans les sources (`PlayerList.isOp`) : en solo sans triches, le joueur n'est pas opérateur, d'où le second cas.

### Textes

- Le serveur n'envoie que des clés et des valeurs : nombres entiers, coordonnées, et noms d'appareils sous forme de clé (`block.netcraft.…`).
- Un appareil s'écrit partout avec la même clé `message.netcraft.device` : `%s [%s, %s, %s]`, par exemple « Box internet [12, 64, -5] ».
- Longueurs en blocs entiers, arrondies à l'entier le plus proche (plus de séparateur décimal, dans aucune langue).
- Vouvoiement partout, ou tournure impersonnelle.
- Doublon fusionné (`result.port_in_use` remplacé par `no_free_port`), `result.ok` retiré.
- Longueurs maximales, valeurs les plus longues comprises (nom « Box internet » ou « Internet Box », port à un chiffre, 3 chiffres de longueur, coordonnées à 5 caractères) : **80 caractères** pour la barre d'action (en interface automatique sur un écran 1920 x 1080, elle tient environ 85 caractères), **45** pour une ligne du panneau, **90** pour le chat (deux lignes de chat). Vérifié par un test sur les deux fichiers de langue.
- Les consignes permanentes (« accroupi + clic dans le vide pour annuler », « clic droit sur un appareil pour rebrancher ») quittent la barre d'action, où elles ne tiennent pas, pour l'infobulle du cordon dans l'inventaire.
- Les erreurs de programmation (exceptions) passent en anglais : elles finissent dans le log.

Textes réécrits (français, avec la longueur du cas le plus long ; l'anglais suit le même sens et la même limite) :

| Clé | Proposition | Car. |
|---|---|---|
| `cable.start` | %s, port %s : clic droit sur le second appareil. | 58 |
| `cable.end_taken` | Bout retiré de %s, l'autre reste sur %s, port %s. | 68 |
| `cable.retracted` | %s : câble débranché, le cordon est libre. | 54 |
| `cable.connected` | Connecté : %s port %s ↔ %s port %s (%s blocs). | 66 |
| `network.loop` | Boucle réseau près de %s. Retirez un câble de la boucle. | 85 |
| `network.saturated` | Réseau saturé : des trames sont perdues. Cherchez une boucle. | 60 |
| `item.netcraft.ethernet_cable.tooltip` (3 lignes) | Clic droit sur deux appareils : les relier. / Accroupi + clic sur un appareil : débrancher. / Accroupi + clic dans le vide : annuler. | 45 par ligne |

### Panneau d'état

Compact par défaut, détaillé quand le joueur est accroupi. Exemple, la box avec 3 ports connectés et 2 MAC derrière le port 1 :

```
Compact                       Accroupi
┌──────────────────────┐      ┌─────────────────────────────────────────────┐
│ Box internet         │      │ Box internet                                │
│ ● 3/4 ports connectés│      │ ● Port 1 : connecté, 02:5e:11:a0:7c:1a (+1) │
│   Accroupi : détails │      │ ● Port 2 : connecté, 02:5e:11:a0:7c:2b      │
└──────────────────────┘      │ ● Port 3 : connecté                         │
                              │ ○ Port 4 : libre                            │
                              │ ▪ Port 5 (ligne) : inactif                  │
                              └─────────────────────────────────────────────┘
```

Les MAC restent affichées en entier (42 caractères pour la ligne la plus longue).

- Box : compact, le nombre de ports réseau local connectés ; accroupi, une ligne par port comme aujourd'hui.
- Poste : compact, « Connecté » ou « Non connecté » ; accroupi, le port et sa MAC.
- La ligne « Accroupi : détails » (grise) n'apparaît qu'en compact : proposition, à retirer si elle encombre.
- Les lignes du panneau sont construites par une classe en Java pur, testée sans lancer le jeu ; le client ne fait que les dessiner.

### Log

- Entièrement en anglais, textes écrits dans le code (le serveur ne traduit rien) ; un appareil s'y écrit `netcraft:internet_box [12, 64, -5] minecraft:overworld`.
- Chaque message envoyé à un joueur y est tracé en DEBUG : canal, clé, valeurs, destinataire.
- Événements réseau en WARN (boucle : tous les appareils et ports touchés, en une ligne).

### Traces `[ESSAI 1b-1]`

| Trace | Sort |
|---|---|
| Message du cordon | Remplacée par la trace DEBUG de tout message envoyé |
| Appareil enregistré ou retiré, câble branché ou débranché | Gardée, en anglais, DEBUG, par le module |
| Chargement : résumé | Gardée (nombre d'appareils et de câbles, MAC générées) |
| Chargement : une ligne par appareil et par câble | Retirée (illisible sur un grand monde ; `/netcraft liens` existe) |
| Indicateur de chaque joueur | Retirée |
| Pose d'un bloc | Retirée (l'enregistrement de l'appareil la couvre) |
| MAC apprises et oubliées chaque seconde | Retirée |

`debug/TrialLog` et `NetworkManager.graphForTrial()` disparaissent.

### Tests

- Le module garde, pour le serveur en cours, une liste d'écouteurs. Un test s'abonne et reçoit chaque message envoyé : canal, niveau, clé, valeurs, destinataire (aucun pour le log), temps de jeu. Rien ne passe par le chat d'un joueur ; `TestPlayer` ne capte plus les messages. Les tests d'un même lot partagent le serveur : chacun filtre ce qu'il capte par son joueur de test ou par ses appareils, et le test de boucle reste dans son lot à part.
- Les tests de la limite par minute ne disparaissent pas, ils suivent la limite : les trois tests du moteur sur la boucle (dont l'écart exact de 1 200 ticks) deviennent « un événement par réseau et par tick » côté moteur et « un message par minute, exactement » côté module ; le test en jeu garde ses instants exacts (0, 1 200, 2 400 ticks), avec une seule série au lieu d'une par box.
- Tests unitaires (Java pur) :
  - choix du canal selon l'usage et le niveau ;
  - anti-répétition : même clé dans la minute refusée, acceptée à 1 200 ticks exactement, clés différentes indépendantes ;
  - moteur : deux box en boucle donnent un seul événement, qui cite les deux box ;
  - panneau : compact et détaillé pour la box et le poste, « (+N) » ;
  - fichiers de langue : mêmes clés en français et en anglais, longueurs maximales respectées.
- Tests en jeu : les tests du cordon vérifient leurs messages par l'écouteur (barre d'action, destinataire) ; le test de boucle vérifie **un seul** message de chat par minute pour deux box, et une seule ligne WARN.

### Code

Données séparées du dessin, pour qu'une future interface HUD remplace le dessin sans toucher au reste :

- `message/` en Java pur, sans classe Minecraft : niveaux, usages, canaux, valeurs d'un message (texte brut, nombre, clé à traduire, appareil), message (niveau, clé, valeurs), routage, anti-répétition, message envoyé (pour les écouteurs), lignes du panneau d'état.
- `message/MessageCenter`, porté par les données du serveur comme la simulation : routage, anti-répétition, envoi, log en anglais, écouteurs. Aucun état statique.
- `network/sync/MessagePayload` : canal, niveau, clé et valeurs, vers le client. Le serveur n'envoie jamais de texte ni de mise en forme.
- `client/` : seul endroit qui décide de l'apparence (symbole, couleur, barre d'action ou chat, dessin du panneau). Il passe par le `ChatListener` du jeu (`gui.chatListener()`), ce qui garde la narration et les réglages de chat du joueur.
- Les réponses aux commandes restent des textes de commande Minecraft (exception ci-dessus).
