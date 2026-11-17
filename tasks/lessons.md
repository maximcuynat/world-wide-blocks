# Leçons

## Git

- Avant de créer une branche demandée, lister les branches existantes (`git branch -a`) : la branche de développement du projet est `developement`. Si la consigne et l'état du dépôt divergent (ex. `main` en retard), demander plutôt que créer une branche de plus. (2026-11-17)

## API Minecraft / NeoForge

- Ne jamais écrire de mémoire un nom de constante, d'enum ou de méthode Minecraft : le chercher dans les sources extraites (`build/moddev/artifacts/minecraft-patched-*-sources.jar`, et les sources NeoForge de maven.neoforged.net) avant de l'utiliser. Exemple : `PushReaction.BLOCK` est devenu `PushReaction.IMMOVEABLE` en 26.3, le build a échoué. (2026-11-17)
- Vérifier aussi le package exact de chaque import, pas seulement l'existence de la classe : `RegisterClientPayloadHandlersEvent` est dans `neoforge.client.network.event`, pas dans `neoforge.network.event`. Avant un commit, contrôler tous les imports `net.neoforged.neoforge.*` contre les sources (un fichier par import). (2026-11-17)

## Conception des interactions

- Pour chaque geste qui consomme un objet en survie, vérifier que le geste inverse reste possible quand le joueur n'a plus cet objet : débrancher exigeait un cordon en main, impossible après avoir utilisé le dernier. Tester mentalement le parcours « survie, un seul exemplaire » avant de proposer une interaction. (2026-11-17)
- Avant d'attribuer un geste (accroupi + clic droit, etc.), relire le plan pour les gestes déjà promis aux lots suivants (ici l'écran du portable au lot 1c). (2026-11-17)
