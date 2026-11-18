# Project management

How the work is planned and tracked on GitHub, and the GitHub settings the maintainer makes by hand. Written on 2026-11-18 from the GitHub documentation read the same day; GitHub's interface changes often: paths marked **(to verify)** were not confirmed by the documentation. The branch model, the release cycle and the `scripts/dev` commands are described in [`CONTRIBUTING.md`](../CONTRIBUTING.md).

No GitHub Actions: everything that checks runs locally (hooks, `scripts/dev`), and everything that sorts uses the automations built into GitHub.

## Where things live

| What | Where |
|---|---|
| Design: content of each lot, architecture, decisions, check procedures in game | [`docs/plan-lots.md`](plan-lots.md) |
| Tasks: one issue per step, in the milestone of its lot or version | GitHub issues, defined in `scripts/project/issues/` (one folder per lot or version, plus `backlog/` and `ideas/`) and created by `scripts/dev project sync` |
| Progress | GitHub Projects board "Roadmap" |
| Notes of a working session (plan, review) | [`tasks/todo.md`](../tasks/todo.md) |
| Mistakes not to repeat | [`tasks/lessons.md`](../tasks/lessons.md) |

A new task is either opened directly on GitHub (templates "Bug report" and "Idea", or a blank issue for the maintainer), or proposed as a definition file in `scripts/project/issues/`, then created by `scripts/dev project sync` (this is how an AI assistant proposes issues: it never creates anything on GitHub itself). Once created, an issue lives on GitHub: editing its file afterwards changes nothing.

## Roadmap

Minor versions follow the order of [`docs/plan-lots.md`](plan-lots.md). Each lot ends with an alpha; the beta freezes the content of the version; 1.0.0 is the first stable version.

| Version | Lots | Content |
|---|---|---|
| 0.1.0 Wired home network | 0, 1a to 1d | PCs, Ethernet cable, internet box, DHCP, terminal and ping, desktop switches. 0.1.0-alpha.1 = lots 1a and 1b-1. |
| 0.2.0 Power and home network | É, 2a to 2d | Electricity, Wi-Fi, fixed cabling, peripherals and local services, other home devices |
| 0.3.0 Subscriber line | 3a, 3b | Fibre and copper line inputs of the box |
| 0.4.0 Street and neighbourhood | 4a, 4b, 5a, 5b | Aerial and underground lines, street cabinets |
| 0.5.0 ISP | 6a to 6c | Rack, ISP premises, public address |
| 0.6.0 Internet | 7a to 7c | Routing between operators, servers and domain names, meshing |
| 0.7.0 Enterprise and datacenter | 8a to 8g | Enterprise racks, configurable switch, router and firewall, servers, power and cooling, accounts |
| 0.8.0 Long-range wireless, tools, survival | 9a to 9d | Tools, mobile network, satellite and radio links, crafting in survival |
| 1.0.0 | F | Finishing: lights, textures, sounds, polished models and HUD |

Milestones exist on GitHub for the versions 0.1.0 and 0.2.0 and their lots; the next ones are added to `scripts/project/milestones.tsv` as versions come closer, then created by `scripts/dev project sync`.

A change that breaks existing worlds is marked `!` in its commit and `breaking` on its pull request; it is listed first in the changelog, under "Breaks existing worlds". Only a minor version may break worlds, never a patch version.

## Labels and milestones

Defined in `scripts/project/labels.tsv` and `scripts/project/milestones.tsv`.

| Family | Labels |
|---|---|
| Type | `type: bug`, `type: feature`, `type: docs`, `type: test`, `type: chore`, `type: idea` |
| Area | `area: engine`, `area: protocols`, `area: interface`, `area: messages`, `area: blocks`, `area: tooling` |
| Priority | `priority: high`, `priority: medium`, `priority: low` |
| Worlds | `breaking` |

The default labels `bug`, `enhancement` and `documentation` are renamed to their `type: …` equivalent; the other default labels are left as they are.

Milestones: `v<X.Y.Z>: <name>` for a version (beta fixes, work shared by several lots), `Lot <id>: <name>` for a lot. `scripts/dev release publish` closes, after an alpha or a beta, the lot milestones of the version that have no open issue left, and after the release, the milestone of the version.

## GitHub settings to make by hand

In order. "Repository settings" means the **Settings** tab of the repository.

### 1. Default branch: `developement`

```bash
gh repo edit --default-branch developement
git remote set-head origin --auto   # then in each local clone: origin/HEAD now points to developement
```

Or in the interface: Repository settings > **General** > section **Default branch** > switch button next to the branch name > `developement` > **Update** **(to verify)**.

Why: the keywords `Closes #N` of a pull request are only read when it targets the default branch. Pull requests of lots go to `developement`; with `main` as default branch, they would neither close their issues nor move them on the board. The home page of the repository then shows `developement`, whose README says that released versions are in the Releases page.

### 2. Merges and branch deletion

```bash
gh repo edit --enable-merge-commit --enable-squash-merge=false --enable-rebase-merge=false --delete-branch-on-merge
# check: true, false, false, true, then MERGE_MESSAGE and PR_TITLE (the "Default message")
gh repo view --json mergeCommitAllowed,squashMergeAllowed,rebaseMergeAllowed,deleteBranchOnMerge
gh api repos/{owner}/{repo} --jq '{merge_commit_title, merge_commit_message}'
```

Or in the interface: Repository settings > **General** > section **Pull Requests**:

- **Allow merge commits**: checked, with the default commit message **Default message** (`Merge pull request #N from …`). git-cliff leaves these merge commits out of the changelog; with "Pull request title", the title of a lot's pull request could land in it.
- **Allow squash merging**: unchecked. A squash would replace the commits of a lot by one commit: the changelog would lose its lines and the commits their signatures.
- **Allow rebase merging**: unchecked. A rebase rewrites the commits: signatures lost.
- **Automatically delete head branches**: checked. It deletes `lot/…`, `release/…` and `hotfix/…` after their merge. `developement` is never the head branch of a pull request in this model, and branch protection rules also prevent automatic deletion.

This replaces section 4 of [`docs/securite-depot.md`](securite-depot.md), written when releases went from `developement` straight to `main`.

### 3. Projects board

Create the board **before** the first `scripts/dev project sync`: the automatic addition only sees issues created or updated after it is turned on.

1. Give gh access to projects: `gh auth refresh -s project` (in a real terminal: it waits for a confirmation in the browser).
2. Create the board, named without the mod's name (here "Roadmap"): profile picture > **Your projects** > **New project** > template **Board** **(to verify)**, or `gh project create --owner <login> --title "Roadmap"`.
3. Link it to the repository: `gh project link <number> --owner <login> --repo <owner>/<repo>`. Give the account login: `--owner @me` is refused by this command.
4. Field **Status**, options in this order: **Ideas**, **To do**, **In progress**, **In review**, **Done** (rename or add the options of the template).
5. Project menu (top right) > **Workflows**, turn on and save each one:

| Workflow | Setting | Effect |
|---|---|---|
| Auto-add to project | repository of the mod, filter `is:issue is:open` | Every new issue is added to the board. GitHub Free allows one auto-add workflow. |
| Item added to project | Status: **To do** | New issues start in To do. |
| Pull request linked to issue | Status: **In progress** | An issue goes to In progress when a pull request that closes it is opened (`scripts/dev lot finish`). |
| Item closed | Status: **Done** (on by default) | |
| Pull request merged | Status: **Done** (on by default) | |

Known limits, without Actions:

- "Item added to project" gives the same status to every new item: idea tickets (`type: idea`) also start in To do. Move them to **Ideas** by hand (board filter `label:"type: idea"`).
- No built-in workflow moves an item to **In review** for a single maintainer without reviewers: it stays a manual column.
- Exact names and options of the workflows other than "Auto-add to project", "Auto-archive items" and "Pull request linked to issue" are not listed by the documentation: check them in the **Workflows** menu.

### 4. Labels, milestones and issues

Once the board exists:

```bash
scripts/dev project sync --dry-run   # shows what would be created, renamed or updated
scripts/dev project sync             # asks for confirmation, then creates
```

Run them in a real terminal: like every `scripts/dev` command that asks for a confirmation, the sync reads the answer from the terminal and answers no when there is none (for example in a shell run by an AI assistant). A second run answers "everything is up to date".

The issue templates apply the `type: bug` and `type: idea` labels, which exist only after this first sync.

## Sources

GitHub documentation read on 2026-11-18:

- Built-in automations of Projects: https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-built-in-automations
- Adding items automatically (filters, limit per plan, existing items not added): https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/adding-items-automatically
- "Pull request linked to issue" workflow: https://github.blog/changelog/2025-11-06-improved-onboarding-flow-for-github-projects/
- Keywords only read towards the default branch, and closing keywords in commit messages ("The issue will be closed when you merge the commit into the default branch"): https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue
- Automatic deletion of branches: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/managing-the-automatic-deletion-of-branches
- Merge commit message: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/configuring-commit-merging-for-pull-requests
- Issue forms: https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/syntax-for-issue-forms
- Issue template configuration: https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/configuring-issue-templates-for-your-repository
- Milestones REST API: https://docs.github.com/en/rest/issues/milestones
