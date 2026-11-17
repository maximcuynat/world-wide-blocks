# Leçons

## Git

- Avant de créer une branche demandée, lister les branches existantes (`git branch -a`) : la branche de développement du projet est `developement`. Si la consigne et l'état du dépôt divergent (ex. `main` en retard), demander plutôt que créer une branche de plus. (2026-11-17)

- Ne commiter que du code qui a passé `./gradlew build test` chez Max. Découper un commit à la main (retirer des traces, scinder des fichiers) crée un état jamais compilé : le soumettre au build avant de commiter, ou commiter seulement l'état déjà compilé. Les commits c178668 et 5014267 ont été faits sans build. (2026-11-18)

## Tests

- Le moteur a des limites globales (trames par tick, taille de la file, limite des signalements) partagées par tout ce qui tourne sur le serveur. Un test automatique qui touche ces limites tourne à part, dans son propre lot, pour que son résultat ne dépende pas des autres tests ni de l'ordre d'exécution. (2026-11-18)

## Shell partagé

- Les commandes `!` de Max s'exécutent dans le dossier courant de ma session. Ne jamais terminer une commande dans un sous-dossier (`cd src/...`) : faire les `cd` dans un sous-shell `( cd … && … )` ou utiliser des chemins absolus, et vérifier que le shell est à la racine du projet avant de donner un point de contrôle. Un `./gradlew` a échoué pour cette raison. (2026-11-17)

## API Minecraft / NeoForge

- Ne jamais écrire de mémoire un nom de constante, d'enum ou de méthode Minecraft : le chercher dans les sources extraites (`build/moddev/artifacts/minecraft-patched-*-sources.jar`, et les sources NeoForge de maven.neoforged.net) avant de l'utiliser. Exemple : `PushReaction.BLOCK` est devenu `PushReaction.IMMOVEABLE` en 26.3, le build a échoué. (2026-11-17)
- Vérifier aussi le package exact de chaque import, pas seulement l'existence de la classe : `RegisterClientPayloadHandlersEvent` est dans `neoforge.client.network.event`, pas dans `neoforge.network.event`. Avant un commit, contrôler tous les imports `net.neoforged.neoforge.*` contre les sources (un fichier par import). (2026-11-17)

- Dans une sous-classe d'une classe Minecraft, un type imbriqué hérité (de la classe ou de ses interfaces) masque l'import du même nom : dans une sous-classe de `ServerPlayer`, `Connection` désigne `WaypointTransmitter.Connection`. Avant d'étendre une classe du jeu, lister les types imbriqués de sa hiérarchie et qualifier entièrement tout nom en conflit. (2026-11-18)

## Conception des interactions

- Pour chaque geste qui consomme un objet en survie, vérifier que le geste inverse reste possible quand le joueur n'a plus cet objet : débrancher exigeait un cordon en main, impossible après avoir utilisé le dernier. Tester mentalement le parcours « survie, un seul exemplaire » avant de proposer une interaction. (2026-11-17)
- Avant d'attribuer un geste (accroupi + clic droit, etc.), relire le plan pour les gestes déjà promis aux lots suivants (ici l'écran du portable au lot 1c). (2026-11-17)
