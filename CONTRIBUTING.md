# Contribuer à NetCraft

Merci de votre intérêt ! Ce guide décrit comment le projet est organisé et vérifié. Le projet est écrit en français (code, commentaires, documents, messages de commit), avec des identifiants en anglais (classes, registres, ressources). Les lignes de log et les messages d'exception sont en anglais, comme le reste des logs de Minecraft.
## Installer le projet

Prérequis : Git, un JDK 17 ou plus récent pour lancer Gradle (le JDK 25 est téléchargé automatiquement au premier build), et de préférence [gitleaks](https://github.com/gitleaks/gitleaks) (macOS : `brew install gitleaks`).

```bash
git clone https://github.com/maximcuynat/netcraft.git
cd netcraft
git switch developement
scripts/install-hooks.sh
```

`scripts/install-hooks.sh` branche les hooks versionnés du dossier `.githooks/` sur votre clone (`git config core.hooksPath .githooks`). À relancer seulement si vous reclonez le dépôt.

## Lancer les tests

```bash
./gradlew build test         # compilation (mod et tests en jeu) et tests unitaires du moteur réseau, en Java pur
./gradlew runGameTestServer  # tests en jeu automatiques, serveur dédié dans un monde jetable (run-gametest/)
./gradlew runClient          # client de développement, pour vérifier à l'œil ce qui s'affiche
```

`runGameTestServer` tourne en serveur dédié : il ne prouve rien sur l'affichage côté client. Une modification de l'affichage se vérifie aussi avec `runClient`.

## Branches

| Branche | Rôle |
|---|---|
| `developement` | Intégration : tout le travail courant y arrive. C'est la branche à viser pour une pull request. |
| `main` | Versions publiées uniquement. Elle ne reçoit que des pull requests de release depuis `developement`, fusionnées avec un commit de fusion. |

Pour une contribution externe : créez une branche depuis `developement` dans votre fork (`feat/…`, `fix/…`), puis ouvrez une pull request vers `developement`. Une fois le dépôt public, les deux branches sont protégées : ni réécriture forcée, ni suppression.

## Messages de commit

Le projet suit [Conventional Commits](https://www.conventionalcommits.org/fr/v1.0.0/). Le hook `commit-msg` refuse un message qui ne respecte pas ce format :

```
<type>(<portée>): <description courte et impérative>
```

| Type | Usage | Dans le changelog |
|---|---|---|
| `feat` | Nouvelle fonctionnalité | Ajouté |
| `fix` | Correction de bug | Corrigé |
| `refactor` | Réorganisation sans changement fonctionnel | Modifié |
| `perf` | Optimisation | Modifié |
| `docs` | Documentation | non |
| `test` | Tests | non |
| `chore` | Maintenance | non |
| `style` | Changement cosmétique | non |
| `build` | Construction du projet, outillage | non |
| `ci` | Vérifications automatiques | non |
| `revert` | Retour arrière | non |

- La portée est facultative, en minuscules : `fix(cordon): …`, `feat(box): …`.
- Un `!` avant les deux-points signale un changement incompatible, par exemple une sauvegarde de monde illisible par les versions précédentes : `feat(reseau)!: …`. Il apparaît toujours dans le changelog.
- Un petit commit par étape logique. Les commits de fusion et les `Revert "…"` générés par Git sont acceptés tels quels.

Exemples tirés de l'historique :

```
feat(box): ajouter le bloc box internet avec 4 ports réseau local et une arrivée de ligne
fix(cordon): débrancher l'autre bout en cliquant sur son appareil
docs(plan): découper le lot 1b et consigner l'architecture validée
```

## Hooks Git

| Hook | Ce qu'il fait | En cas d'échec |
|---|---|---|
| `commit-msg` | Vérifie la convention des messages | Commit refusé |
| `pre-commit` | Cherche des secrets (mots de passe, jetons, clés) dans les changements indexés avec gitleaks | Commit refusé ; si gitleaks n'est pas installé, simple avertissement |
| `pre-push` | Lance `./gradlew build test` puis `./gradlew runGameTestServer` | Push refusé |

Le hook `pre-push` :

- saute les tests si tous les fichiers des commits envoyés sont des `.md` ou sont dans `docs/` ; tout le reste, ressources comprises, déclenche les tests ;
- exige un arbre de travail propre et que le commit envoyé soit `HEAD`, car les tests portent sur l'arbre de travail ;
- prend quelques minutes (le premier build plus longtemps).

Contourner un hook (`--no-verify`) est réservé aux cas où la vérification vient d'être faite sur ce même état, comme le fait le script de release.

## Versions et changelog

- Versionnage sémantique, en 0.x tant que le mod n'est pas stable : une version mineure (0.**2**.0) peut casser les mondes existants, un correctif (0.1.**1**) non.
- Canaux alignés sur CurseForge et Modrinth, déduits du suffixe : `-alpha.N` (alpha), `-beta.N` (beta), sans suffixe (release).
- Une seule source de vérité : `mod_version` dans `gradle.properties`, reprise dans `neoforge.mods.toml` et dans le nom du jar (`netcraft-<minecraft>-<version>.jar`).
- `CHANGELOG.md` est généré par [git-cliff](https://git-cliff.org/) (`cliff.toml`) depuis les messages de commit, au format [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) : ne le modifiez pas à la main.

## Publier une version (mainteneur)

Tout se fait en local avec `scripts/release.sh`. Il faut git-cliff, la GitHub CLI connectée (`gh auth login`) et une clé de signature configurée (voir [`docs/securite-depot.md`](docs/securite-depot.md)).

1. **Essai à blanc**, sans rien modifier : `scripts/release.sh --dry-run 0.1.0-alpha.1`.
2. **Préparation** depuis `developement` à jour : `scripts/release.sh 0.1.0-alpha.1`.
   - vérifie la branche, l'arbre et le tag ;
   - écrit la version dans `gradle.properties` et génère `CHANGELOG.md` ;
   - lance `build test` puis `runGameTestServer` ;
   - crée le commit `chore(release): v0.1.0-alpha.1` et le tag annoté `v0.1.0-alpha.1`, tous deux signés ;
   - **demande confirmation**, puis envoie `developement` et le tag et ouvre la pull request vers `main`.
3. **Fusion** de la pull request sur GitHub avec **Create a merge commit** (ni squash ni rebase : le tag doit rester sur `developement` et dans `main`).
4. **Release GitHub** : `git switch --detach v0.1.0-alpha.1`, puis `scripts/release.sh --publish 0.1.0-alpha.1`. Le script construit le jar depuis le tag et, **après confirmation**, crée la release en **brouillon** (marquée pré-version pour alpha et beta) avec le jar et les notes. La publication se fait à la main sur GitHub.
5. Retour sur `developement` : `git switch developement`.

## Signaler une faille

Pas d'issue publique : voir [`SECURITY.md`](SECURITY.md).
