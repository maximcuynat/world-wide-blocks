# Repository security: GitHub settings to make

Settings that Max makes by hand on GitHub and on his computer (Claude has no access to the account). Written on 2026-11-18 from the official GitHub documentation read the same day. GitHub's interface changes often: paths marked **(to verify)** were not confirmed by the documentation.

In the paths, "Repository settings" means the **Settings** tab of the repository, and "Account settings" means the menu **profile picture (top right) > Settings**.

## State checked on 2026-11-19

Read by Claude through the GitHub API (read-only `gh api` calls, no setting changed), for the repository `maximcuynat/world-wide-blocks`. Account settings (two-factor authentication, private email, vigilant mode) cannot be read this way: they are not covered.

**Active**

| Setting | State |
|---|---|
| Visibility | Public |
| Default branch, merges | `developement`; merge commits only (squash and rebase disabled); merged branches deleted automatically |
| GitHub Actions | Disabled (`actions/permissions`: `enabled: false`) |
| Secret scanning and push protection | Enabled |
| Private vulnerability reporting | Enabled |
| Dependabot alerts | Enabled (`vulnerability-alerts` answers 204); Dependabot security updates disabled |

**Verified signatures**

| Object | Result |
|---|---|
| Merge commit of pull request #42 (`fdb779c`, `release/v0.1.0-alpha.1` into `main`, merged from the GitHub interface) | Signed by GitHub, **Verified** (`verified: true`, reason `valid`, committer `GitHub`). Same for the merge commit of pull request #41 (`6c6f25c`) |
| Release commit `df8ccc1` (`chore(release): v0.1.0-alpha.1`) | Signed, **Verified** |
| Tag `v0.1.0-alpha.1` (annotated tag object `d7a8bd8`, on `fdb779c`) | Signed, **Verified** (reason `valid`, tagger `maximcuynat`): the SSH signing key of section 3 works |

**Remaining**

| Step | State |
|---|---|
| 7. Protection of `main` and `developement` | **Not in place**: no ruleset (`rulesets` answers `[]`), both branches unprotected, although the repository is already public |
| 11. Require signed commits on `main` | Not in place |
| Account settings (steps 1 and 2, vigilant mode) | Not readable through the repository API: to check by Max in the account settings |

## Order of operations

Many protections do not exist for a **private** repository on a GitHub Free account: branch protection rules (classic or rulesets) are only available on a private repository with GitHub Pro, Team or Enterprise. Hence this order (see also [`going-public.md`](going-public.md) for the steps to make the repository public):

| # | When | Setting | Section |
|---|---|---|---|
| 1 | Now | Two-factor authentication on the account | [1](#1-two-factor-authentication) |
| 2 | Now | Private email address | [2](#2-private-email-address) |
| 3 | Now | SSH signing of commits and tags | [3](#3-sign-commits-and-tags) |
| 4 | Now | Default branch `developement`, merge commits only, automatic deletion of merged branches, Projects board; Actions disabled | [4](#4-general-repository-settings) and [`docs/project-management.md`](project-management.md) |
| 5 | Dedicated session | **Renaming the mod (blocking)**: see `tasks/todo.md` | |
| 6 | Going public | Repository settings > General > Danger Zone > **Change visibility** > Make public | |
| 7 | Right after | Protection of `main` and `developement` (rulesets) | [5](#5-branch-protection) |
| 8 | Right after | Secret scanning and push protection | [6](#6-secret-scanning) |
| 9 | Right after | Private vulnerability reporting | [7](#7-private-vulnerability-reporting) |
| 10 | Right after | Dependency graph and Dependabot alerts | [8](#8-dependabot-alerts) |
| 11 | After the first release | Require signed commits on `main` | [9](#9-require-signed-commits-on-main) |

## 1. Two-factor authentication

Account settings > section **Access** > **Password and authentication** > **Enable two-factor authentication**.

- Prefer a passkey or a security key, otherwise a TOTP app. Avoid SMS.
- Store the recovery codes away from the computer (password manager, paper).
- GitHub then opens a 28-day check period: it asks once more for two-factor authentication to verify that it works.

## 2. Private email address

Account settings > **Emails** **(to verify)**:

- check **Keep my email addresses private**;
- check **Block command line pushes that expose my email**.

The first 57 commits already use the private address `93578199+maximcuynat@users.noreply.github.com`. The second setting rejects a push that would contain your personal address by mistake.

## 3. Sign commits and tags

Signing proves that the commits and tags really come from you; GitHub marks them **Verified**. `scripts/dev release` requires a signing key. Procedure with an SSH key dedicated to signing (separate from the key used to log in):

**3.1. Create the key** (with a passphrase):

```bash
ssh-keygen -t ed25519 -C "git signing maximcuynat" -f ~/.ssh/id_ed25519_signing
```

**3.2. Type the passphrase only once** (macOS keychain):

```bash
ssh-add --apple-use-keychain ~/.ssh/id_ed25519_signing
```

and in `~/.ssh/config`:

```
Host *
    UseKeychain yes
    AddKeysToAgent yes
```

**3.3. Configure Git**:

```bash
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519_signing.pub
git config --global commit.gpgsign true
git config --global tag.gpgsign true
```

To sign only in this repository, run the last two commands without `--global`, from the project folder.

Then check the address used for commits, without changing it: `git config user.email` must answer `93578199+maximcuynat@users.noreply.github.com` (this is the case for all existing commits). This address must belong to the GitHub account, otherwise GitHub does not link the signature to the account.

**3.4. Check signatures locally** (without this file, `git log --show-signature` and `git tag -v` answer "allowedSignersFile needs to be configured"):

```bash
mkdir -p ~/.config/git
echo "93578199+maximcuynat@users.noreply.github.com namespaces=\"git\" $(cat ~/.ssh/id_ed25519_signing.pub)" >> ~/.config/git/allowed_signers
git config --global gpg.ssh.allowedSignersFile ~/.config/git/allowed_signers
```

**3.5. Declare the key on GitHub as a signing key**:

- Interface: Account settings > section **Access** > **SSH and GPG keys** > **New SSH key**, type **Signing Key**, paste the content of `~/.ssh/id_ed25519_signing.pub`.
- Or on the command line: `gh ssh-key add ~/.ssh/id_ed25519_signing.pub --type signing --title "Git signing"`. If gh refuses for lack of permissions: `gh auth refresh -h github.com -s admin:ssh_signing_key` **(to verify)**, then run it again.

A key used both to log in and to sign must be added twice, once of each type: the dedicated key avoids this case.

**3.6. Test**:

```bash
git tag -s test-signature -m "test" && git tag -v test-signature && git tag -d test-signature
```

The expected answer contains `Good "git" signature for 93578199+maximcuynat@users.noreply.github.com`. At the next real commit: `git log -1 --show-signature`, then the **Verified** badge on GitHub after the push.

**3.7. Vigilant mode (optional)**: Account settings > **SSH and GPG keys** > **Vigilant mode** > **Flag unsigned commits as unverified**. GitHub advises to enable it only if you sign all your commits: the 57 commits already published, unsigned, would then show **Unverified**. To decide after the first release.

**3.8. Commits made by Claude Code**: Claude's sandbox reads neither `~/.gitconfig` nor `~/.ssh`. The commits it makes are therefore **unsigned** (private identity passed explicitly). This has no consequence until step 11; after that, an unsigned commit on `developement` would block the release pull request. To decide before step 11:

- **A (recommended)**: Claude prepares the index and proposes the message, Max runs the commit with `! git commit -m "…"` (signed with his key, the key never leaves Max's computer);
- **B**: allow the sandbox to read `~/.gitconfig` and the signing key (`sandbox` setting of Claude Code): commits are signed automatically, but the agent can then sign in your name.

## 4. General repository settings

Default branch, merges and branch deletion: see [`docs/project-management.md`](project-management.md), sections 1 and 2 (revised on 2026-11-18 with the `lot/`, `release/`, `hotfix/` model). In short:

- default branch: `developement` (the "Closes #" keywords are only read towards it);
- **Allow merge commits** only, default message **Default message**; **Allow squash merging** and **Allow rebase merging** unchecked: a squash would flatten the commits of a lot (changelog and signatures), a rebase would lose the signatures;
- **Automatically delete head branches** checked: releases now go through a `release/v<version>` branch, so `developement` is never the source branch of a pull request and is never deleted.

Repository settings > **Actions** > **General** > **Actions permissions** > **Disable actions** **(to verify)**: the project does not use GitHub Actions, all checks run locally.

## 5. Branch protection

Once the repository is public: Repository settings > **Rules** > **Rulesets** > **New ruleset** > **New branch ruleset** **(to verify)**. Create two rulesets, **Enforcement status: Active**, empty bypass list.

**Ruleset `main`** (target: branch `main`):

- **Restrict deletions**: no deletion;
- **Block force pushes**: no forced rewrite;
- **Require a pull request before merging**: changes go through a pull request, with **Required approvals: 0** (sole maintainer, you cannot approve your own pull request) and, if the option is offered, allowed merge methods: **Merge** only;
- **Require signed commits**: not right away, see step 11.

**Ruleset `developement`** (target: branch `developement`):

- **Restrict deletions**;
- **Block force pushes**.

No required pull request on `developement`: small changes land there directly, and `scripts/dev release publish` pushes the carry-over of `main` there after each version. Lots arrive there by pull request from `lot/<id>`.

Optional, **tag ruleset** on `v*`: **Restrict deletions** and **Restrict updates**, so that a published version tag can no longer move.

## 6. Secret scanning

On a public repository, secret scanning runs automatically and for free. To enable it and add push protection: Repository settings > section **Security and quality** > **Advanced Security**:

- **Secret Protection**: **Enable**;
- then in the same section, **Push protection**: **Enable** (rejects a push that contains a recognized secret).

Or from the command line (run on 2026-11-18, which enabled both on this public personal repository):

```bash
gh api -X PATCH 'repos/{owner}/{repo}' \
  -f "security_and_analysis[secret_scanning][status]=enabled" \
  -f "security_and_analysis[secret_scanning_push_protection][status]=enabled"
```

The `pre-commit` hook (gitleaks) stays the local protection, before anything reaches GitHub.

Push protection "for users" (account level) is enabled by default and blocks secrets pushed to any public repository: check that it is active in the account settings **(path to verify, not given by the documentation)**.

## 7. Private vulnerability reporting

Repository settings > section **Security and quality** > **Advanced Security** > **Private vulnerability reporting**: **Enable**. Required for `SECURITY.md`, which points to the **Report a vulnerability** button of the **Security** tab. The documentation presents this setting for public repositories: to do after going public. Or from the command line (run on 2026-11-18):

```bash
gh api -X PUT 'repos/{owner}/{repo}/private-vulnerability-reporting'
```

## 8. Dependabot alerts

Repository settings > section **Security and quality** > **Advanced Security**:

- **Dependency graph**: **Enable**;
- **Dependabot alerts**: **Enable**.

Or from the command line, for the alerts (run on 2026-11-18):

```bash
gh api -X PUT 'repos/{owner}/{repo}/vulnerability-alerts'
```

Known limit: for Gradle, GitHub does not read dependencies by static analysis. The graph is filled by automatic dependency submission, which requires GitHub Actions. Without Actions, the alerts will probably not see the mod's dependencies. The risk is low: the only dependencies are NeoForge (and Minecraft) and JUnit for the tests. NeoForge updates are followed by hand.

## 9. Require signed commits on `main`

GitHub refuses to merge a pull request that contains unsigned commits, even old ones. The current 57 commits are not signed. Order:

1. Sign all new commits and tags (section 3, and decision 3.8 for Claude's commits).
2. Make the first release: the pull request `release/v…` > `main` brings the unsigned history into `main`, which is still allowed.
3. Enable **Require signed commits** in the `main` ruleset. The following pull requests will only bring signed commits.

Checked on 2026-11-19: GitHub signs the merge commit it creates from the interface. The merge commits of pull requests #41 and #42 are signed by GitHub and marked **Verified** (see "State checked on 2026-11-19"), so a merge from the interface does not break this rule.

## Sources

GitHub documentation read on 2026-11-18:

- Branch protection: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches
- Rulesets, creation and available rules: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/creating-rulesets-for-a-repository and https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/available-rules-for-rulesets
- Commit signing: https://docs.github.com/en/authentication/managing-commit-signature-verification/about-commit-signature-verification
- Adding an SSH key (authentication or signing): https://docs.github.com/en/authentication/connecting-to-github-with-ssh/adding-a-new-ssh-key-to-your-github-account
- Vigilant mode: https://docs.github.com/en/authentication/managing-commit-signature-verification/displaying-verification-statuses-for-all-of-your-commits
- Two-factor authentication: https://docs.github.com/en/authentication/securing-your-account-with-two-factor-authentication-2fa/configuring-two-factor-authentication
- Secret scanning: https://docs.github.com/en/code-security/secret-scanning/introduction/about-secret-scanning and https://docs.github.com/en/code-security/secret-scanning/enabling-secret-scanning-features/enabling-secret-scanning-for-your-repository
- Push protection: https://docs.github.com/en/code-security/secret-scanning/introduction/about-push-protection and https://docs.github.com/en/code-security/secret-scanning/enabling-secret-scanning-features/enabling-push-protection-for-your-repository
- Private vulnerability reporting: https://docs.github.com/en/code-security/security-advisories/working-with-repository-security-advisories/configuring-private-vulnerability-reporting-for-a-repository
- Dependabot alerts: https://docs.github.com/en/code-security/dependabot/dependabot-alerts/configuring-dependabot-alerts
- Dependency graph ecosystems: https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/dependency-graph-supported-package-ecosystems
- Automatic dependency submission: https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/configuring-automatic-dependency-submission-for-your-repository
