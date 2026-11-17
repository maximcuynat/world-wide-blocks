# Leçons

## Git

- Avant de créer une branche demandée, lister les branches existantes (`git branch -a`) : la branche de développement du projet est `developement`. Si la consigne et l'état du dépôt divergent (ex. `main` en retard), demander plutôt que créer une branche de plus. (2026-11-17)

## API Minecraft / NeoForge

- Ne jamais écrire de mémoire un nom de constante, d'enum ou de méthode Minecraft : le chercher dans les sources extraites (`build/moddev/artifacts/minecraft-patched-*-sources.jar`, et les sources NeoForge de maven.neoforged.net) avant de l'utiliser. Exemple : `PushReaction.BLOCK` est devenu `PushReaction.IMMOVEABLE` en 26.3, le build a échoué. (2026-11-17)
