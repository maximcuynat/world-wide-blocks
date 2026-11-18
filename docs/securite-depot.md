# Sécurité du dépôt : réglages GitHub à faire

Réglages à faire par Max sur GitHub et sur son poste (Claude n'a pas accès au compte). Établi le 2026-11-18 d'après la documentation officielle de GitHub lue le même jour ; l'interface de GitHub change souvent : les chemins marqués **(à vérifier)** n'ont pas été confirmés par la documentation.

Dans les chemins, « Paramètres du dépôt » désigne l'onglet **Settings** du dépôt, et « Paramètres du compte » le menu **photo de profil (en haut à droite) > Settings**.

## Ordre des opérations

Une grande partie des protections n'existe pas pour un dépôt **privé** sur un compte GitHub Free : les règles de protection des branches (classiques ou rulesets) ne sont disponibles sur un dépôt privé qu'avec GitHub Pro, Team ou Enterprise. D'où cet ordre :

| # | Quand | Réglage | Section |
|---|---|---|---|
| 1 | Maintenant | Double authentification sur le compte | [1](#1-double-authentification) |
| 2 | Maintenant | Adresse e-mail masquée | [2](#2-adresse-e-mail-masquée) |
| 3 | Maintenant | Signature SSH des commits et des tags | [3](#3-signer-ses-commits-et-ses-tags) |
| 4 | Maintenant | Fusions : commit de fusion seulement, pas de suppression automatique des branches ; Actions désactivées | [4](#4-réglages-généraux-du-dépôt) |
| 5 | Session dédiée | **Renommage du mod (bloquant)** : voir `tasks/todo.md` | |
| 6 | Passage en public | Paramètres du dépôt > General > Danger Zone > **Change visibility** > Make public | |
| 7 | Juste après | Protection de `main` et de `developement` (rulesets) | [5](#5-protection-des-branches) |
| 8 | Juste après | Détection des secrets et blocage à l'envoi | [6](#6-détection-des-secrets) |
| 9 | Juste après | Signalement privé des failles | [7](#7-signalement-privé-des-failles) |
| 10 | Juste après | Graphe des dépendances et alertes Dependabot | [8](#8-alertes-dependabot) |
| 11 | Après la première release | Exiger des commits signés sur `main` | [9](#9-exiger-des-commits-signés-sur-main) |

## 1. Double authentification

Paramètres du compte > section **Access** > **Password and authentication** > **Enable two-factor authentication**.

- Préférez une clé d'accès (passkey) ou une clé de sécurité, sinon une application TOTP. Évitez les SMS.
- Enregistrez les codes de récupération hors de l'ordinateur (gestionnaire de mots de passe, papier).
- GitHub ouvre ensuite une période de contrôle de 28 jours : il redemande une fois la double authentification pour vérifier qu'elle fonctionne.

## 2. Adresse e-mail masquée

Paramètres du compte > **Emails** **(à vérifier)** :

- cochez **Keep my email addresses private** ;
- cochez **Block command line pushes that expose my email**.

Les 57 premiers commits utilisent déjà l'adresse masquée `93578199+maximcuynat@users.noreply.github.com`. Le second réglage refuse un push qui contiendrait par erreur votre adresse personnelle.

## 3. Signer ses commits et ses tags

La signature prouve que les commits et les tags viennent bien de vous ; GitHub les marque **Verified**. Le script de release exige une clé de signature. Procédure avec une clé SSH dédiée à la signature (séparée de la clé qui sert à se connecter) :

**3.1. Créer la clé** (avec une phrase de passe) :

```bash
ssh-keygen -t ed25519 -C "signature git maximcuynat" -f ~/.ssh/id_ed25519_signing
```

**3.2. Ne taper la phrase de passe qu'une fois** (trousseau macOS) :

```bash
ssh-add --apple-use-keychain ~/.ssh/id_ed25519_signing
```

et dans `~/.ssh/config` :

```
Host *
    UseKeychain yes
    AddKeysToAgent yes
```

**3.3. Configurer Git** :

```bash
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519_signing.pub
git config --global commit.gpgsign true
git config --global tag.gpgsign true
```

Pour ne signer que dans ce dépôt, lancez les deux dernières commandes sans `--global`, depuis le dossier du projet.

Vérifiez ensuite l'adresse utilisée pour les commits, sans la modifier : `git config user.email` doit répondre `93578199+maximcuynat@users.noreply.github.com` (c'est le cas de tous les commits existants). Cette adresse doit appartenir au compte GitHub, sinon GitHub n'associe pas la signature au compte.

**3.4. Vérifier les signatures en local** (sans ce fichier, `git log --show-signature` et `git tag -v` répondent « allowedSignersFile needs to be configured ») :

```bash
mkdir -p ~/.config/git
echo "93578199+maximcuynat@users.noreply.github.com namespaces=\"git\" $(cat ~/.ssh/id_ed25519_signing.pub)" >> ~/.config/git/allowed_signers
git config --global gpg.ssh.allowedSignersFile ~/.config/git/allowed_signers
```

**3.5. Déclarer la clé sur GitHub comme clé de signature** :

- Interface : Paramètres du compte > section **Access** > **SSH and GPG keys** > **New SSH key**, type **Signing Key**, coller le contenu de `~/.ssh/id_ed25519_signing.pub`.
- Ou en ligne de commande : `gh ssh-key add ~/.ssh/id_ed25519_signing.pub --type signing --title "Signature Git"`. Si gh refuse faute de droits : `gh auth refresh -h github.com -s admin:ssh_signing_key` **(à vérifier)**, puis relancer.

Une même clé utilisée pour se connecter et pour signer doit être ajoutée deux fois, une fois de chaque type : la clé dédiée évite ce cas.

**3.6. Tester** :

```bash
git tag -s essai-signature -m "essai" && git tag -v essai-signature && git tag -d essai-signature
```

La réponse attendue contient `Good "git" signature for 93578199+maximcuynat@users.noreply.github.com`. Au prochain vrai commit : `git log -1 --show-signature`, puis le badge **Verified** sur GitHub après le push.

**3.7. Mode vigilant (facultatif)** : Paramètres du compte > **SSH and GPG keys** > **Vigilant mode** > **Flag unsigned commits as unverified**. GitHub conseille de ne l'activer que si vous signez tous vos commits : les 57 commits déjà publiés, non signés, s'afficheraient alors **Unverified**. À décider après la première release.

**3.8. Commits faits par Claude Code** : le bac à sable de Claude ne lit ni `~/.gitconfig` ni `~/.ssh`. Les commits qu'il fait sont donc **non signés** (identité masquée passée explicitement). C'est sans conséquence jusqu'à l'étape 11 ; ensuite, un commit non signé sur `developement` bloquerait la pull request de release. À trancher avant l'étape 11 :

- **A (recommandé)** : Claude prépare l'index et propose le message, Max lance le commit avec `! git commit -m "…"` (signé avec sa clé, la clé ne quitte pas le poste de Max) ;
- **B** : autoriser le bac à sable à lire `~/.gitconfig` et la clé de signature (réglage `sandbox` de Claude Code) : commits signés automatiquement, mais l'agent peut alors signer en votre nom.

## 4. Réglages généraux du dépôt

Paramètres du dépôt > **General** > section **Pull Requests** :

- cochez **Allow merge commits** seulement ; décochez **Allow squash merging** et **Allow rebase merging**. Le modèle de branches repose sur le commit de fusion : un squash ferait perdre le tag de version dans `developement` ;
- laissez **Automatically delete head branches** décoché : sinon GitHub supprimerait `developement` après chaque release.

Paramètres du dépôt > **Actions** > **General** > **Actions permissions** > **Disable actions** **(à vérifier)** : le projet n'utilise pas GitHub Actions, toutes les vérifications tournent en local.

## 5. Protection des branches

Une fois le dépôt public : Paramètres du dépôt > **Rules** > **Rulesets** > **New ruleset** > **New branch ruleset** **(à vérifier)**. Créez deux rulesets, **Enforcement status : Active**, liste de contournement (bypass list) vide.

**Ruleset `main`** (cible : branche `main`) :

- **Restrict deletions** : pas de suppression ;
- **Block force pushes** : pas de réécriture forcée ;
- **Require a pull request before merging** : passage par une pull request, avec **Required approvals : 0** (seul mainteneur, vous ne pouvez pas approuver votre propre pull request) et, si l'option est proposée, méthodes de fusion autorisées : **Merge** seulement ;
- **Require signed commits** : pas tout de suite, voir l'étape 11.

**Ruleset `developement`** (cible : branche `developement`) :

- **Restrict deletions** ;
- **Block force pushes**.

Pas de pull request obligatoire sur `developement` : le travail courant et le script de release y poussent directement.

Facultatif, **tag ruleset** sur `v*` : **Restrict deletions** et **Restrict updates**, pour qu'un tag de version publié ne bouge plus.

## 6. Détection des secrets

Sur un dépôt public, la détection des secrets tourne automatiquement et gratuitement. Pour l'activer et ajouter le blocage à l'envoi : Paramètres du dépôt > section **Security and quality** > **Advanced Security** :

- **Secret Protection** : **Enable** ;
- puis dans la même section, **Push protection** : **Enable** (refuse un push qui contient un secret reconnu).

La documentation ne dit pas clairement si le blocage à l'envoi est gratuit pour un dépôt personnel public : si l'option n'est pas proposée, le hook `pre-commit` (gitleaks) reste la protection locale.

Le blocage à l'envoi « pour les utilisateurs » (au niveau du compte) est activé par défaut et bloque les secrets envoyés vers tout dépôt public : vérifiez qu'il est actif dans les paramètres du compte **(chemin à vérifier, non donné par la documentation)**.

## 7. Signalement privé des failles

Paramètres du dépôt > section **Security and quality** > **Advanced Security** > **Private vulnerability reporting** : **Enable**. Indispensable pour `SECURITY.md`, qui renvoie au bouton **Report a vulnerability** de l'onglet **Security**. La documentation présente ce réglage pour les dépôts publics : à faire après le passage en public.

## 8. Alertes Dependabot

Paramètres du dépôt > section **Security and quality** > **Advanced Security** :

- **Dependency graph** : **Enable** ;
- **Dependabot alerts** : **Enable**.

Limite connue : pour Gradle, GitHub ne lit pas les dépendances par analyse statique. Le graphe est rempli par la soumission automatique des dépendances, qui exige GitHub Actions. Sans Actions, les alertes ne verront probablement pas les dépendances du mod. Le risque est faible : les seules dépendances sont NeoForge (et Minecraft) et JUnit pour les tests. Les mises à jour de NeoForge se suivent à la main.

## 9. Exiger des commits signés sur `main`

GitHub refuse de fusionner une pull request qui contient des commits non signés, même anciens. Or les 57 commits actuels ne le sont pas. Ordre :

1. Signer tous les nouveaux commits et tags (section 3, et décision 3.8 pour les commits de Claude).
2. Faire la première release : la pull request `developement` > `main` amène l'historique non signé dans `main`, ce qui est encore permis.
3. Activer **Require signed commits** dans le ruleset `main`. Les pull requests suivantes n'apporteront que des commits signés.

À vérifier à la première release suivante : la documentation ne dit pas clairement si GitHub signe le commit de fusion qu'il crée depuis l'interface (elle le confirme seulement pour le squash). Si la fusion est refusée pour cette raison, revenir ici avant de désactiver quoi que ce soit.

## Sources

Documentation GitHub lue le 2026-11-18 :

- Protection des branches : https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches
- Rulesets, création et règles disponibles : https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/creating-rulesets-for-a-repository et https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/available-rules-for-rulesets
- Signature des commits : https://docs.github.com/en/authentication/managing-commit-signature-verification/about-commit-signature-verification
- Ajouter une clé SSH (authentification ou signature) : https://docs.github.com/en/authentication/connecting-to-github-with-ssh/adding-a-new-ssh-key-to-your-github-account
- Mode vigilant : https://docs.github.com/en/authentication/managing-commit-signature-verification/displaying-verification-statuses-for-all-of-your-commits
- Double authentification : https://docs.github.com/en/authentication/securing-your-account-with-two-factor-authentication-2fa/configuring-two-factor-authentication
- Détection des secrets : https://docs.github.com/en/code-security/secret-scanning/introduction/about-secret-scanning et https://docs.github.com/en/code-security/secret-scanning/enabling-secret-scanning-features/enabling-secret-scanning-for-your-repository
- Blocage à l'envoi : https://docs.github.com/en/code-security/secret-scanning/introduction/about-push-protection et https://docs.github.com/en/code-security/secret-scanning/enabling-secret-scanning-features/enabling-push-protection-for-your-repository
- Signalement privé des failles : https://docs.github.com/en/code-security/security-advisories/working-with-repository-security-advisories/configuring-private-vulnerability-reporting-for-a-repository
- Alertes Dependabot : https://docs.github.com/en/code-security/dependabot/dependabot-alerts/configuring-dependabot-alerts
- Écosystèmes du graphe des dépendances : https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/dependency-graph-supported-package-ecosystems
- Soumission automatique des dépendances : https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/configuring-automatic-dependency-submission-for-your-repository
