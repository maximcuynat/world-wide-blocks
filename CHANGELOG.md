# Journal des modifications

Toutes les modifications notables de ce projet sont consignées dans ce fichier.

Le format s'appuie sur [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet suit le [versionnage sémantique](https://semver.org/lang/fr/). Tant que la version commence par 0, le mod n'est pas stable : une version mineure peut casser les mondes existants.

## [Non publié]

### Ajouté

- *blocks* : Ajouter le bloc Tour PC sans texture
- *blocks* : Ajouter le bloc Tour PC et sa texture
- *blocks* : Ajouter le bloc PC portable
- *reseau* : Ajouter le moteur réseau, sa sauvegarde et la commande /netcraft liens
- *blocks* : Faire de la tour et du portable des appareils réseau
- *cordon* : Ajouter le cordon Ethernet qui relie deux appareils
- *reseau* : Synchroniser les liens vers le client et afficher l'état des ports
- *reseau* : Dessiner les câbles et ne les envoyer qu'aux joueurs qui les voient
- *client* : Afficher l'état des ports dans un panneau en haut au centre
- *cordon* : Retirer un seul bout du câble main vide et le garder en main
- *reseau* : Typer les ports et attribuer une adresse MAC à chaque port
- *protocole* : Ajouter les trames Ethernet en couches et la trame de présence
- *commutation* : Ajouter la table d'apprentissage des MAC et le switch Ethernet réutilisable
- *simulation* : Faire circuler les trames entre appareils avec protection contre les boucles
- *simulation* : Faire tourner la simulation à chaque tick du serveur et signaler les boucles
- *box* : Ajouter le bloc box internet avec 4 ports réseau local et une arrivée de ligne
- *indicateur* : Afficher la MAC des postes et les MAC apprises par la box
- *commande* : Construire les décors d'essai avec /netcraft test
- *messages* : Noyau du module des messages en Java pur
- *messages* : Transport vers le client et affichage décidé par le client
- *messages* : Textes vouvoyés, distances entières et infobulle du cordon
- *messages* : Un message par boucle et par réseau, saturation signalée aux opérateurs
- *panneau* : Panneau d'état compact, détaillé quand le joueur est accroupi

### Modifié

- *log* : Log en anglais, traces d'essai remplacées par les diagnostics du module

### Corrigé

- *blocks* : Utiliser PushReaction.IMMOVEABLE, nom de la constante en 26.3
- *client* : Corriger le package de RegisterClientPayloadHandlersEvent
- *cordon* : Débrancher l'autre bout en cliquant sur son appareil
- *cordon* : Annuler l'attente quand le point de départ disparaît ou que le porteur change de dimension
- *simulation* : Appliquer les changements du graphe au tick suivant, avec son temps


<!-- Généré par git-cliff depuis les messages de commit : ne pas modifier à la main. -->
