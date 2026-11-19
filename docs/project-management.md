# Project management

How the work is planned and tracked on GitHub, and the GitHub settings the maintainer makes by hand. Written on 2026-11-18 from the GitHub documentation read the same day; GitHub's interface changes often: paths marked **(to verify)** were not confirmed by the documentation. The branch model, the release cycle and the `scripts/dev` commands are described in [`CONTRIBUTING.md`](../CONTRIBUTING.md).

No GitHub Actions: everything that checks runs locally (hooks, `scripts/dev`), and everything that sorts uses the automations built into GitHub.

## Where things live

| What | Where |
|---|---|
| Design: content of each lot, architecture, decisions, check procedures in game | [`docs/lot-plan.md`](lot-plan.md) |
| How changes are checked: automatic tests, capture runs, what stays by hand | [`docs/automated-checks.md`](automated-checks.md) |
| Tasks: one issue per step, in the milestone of its lot or version | GitHub issues, defined in `scripts/project/issues/` (one folder per lot or version, plus `backlog/` and `ideas/`) and created by `scripts/dev project sync` |
| Progress | GitHub Projects board "Roadmap", kept sorted by `scripts/dev project board` (see [Board columns](#board-columns-and-sorting)) |
| Notes of a working session (plan, review) | [`tasks/todo.md`](../tasks/todo.md) |
| Mistakes not to repeat | [`tasks/lessons.md`](../tasks/lessons.md) |

A new task is either opened directly on GitHub (templates "Bug report" and "Idea", or a blank issue for the maintainer), or proposed as a definition file in `scripts/project/issues/`, then created by `scripts/dev project sync` (this is how an AI assistant proposes issues: it never creates anything on GitHub itself). Once created, an issue lives on GitHub: editing its file afterwards changes nothing until `scripts/dev project update-issues` rewrites its body (title, labels, milestone, state and comments are never touched, and boxes ticked on GitHub stay ticked).

## Roadmap

Minor versions follow the order of [`docs/lot-plan.md`](lot-plan.md). Each lot ends with an alpha; the beta freezes the content of the version; 1.0.0 is the first stable version.

| Version | Lots | Content |
|---|---|---|
| 0.1.0 Wired home network | 0, 1a to 1d | PCs, Ethernet cable, internet box, DHCP, terminal and ping, desktop switches. 0.1.0-alpha.1 = lots 1a and 1b-1. |
| 0.2.0 Power and home network | 1e, 1f, 2a to 2d | Electricity, TCP, Wi-Fi, fixed cabling, peripherals and local services, other home devices |
| 0.3.0 Subscriber line | 3a, 3b | Fibre and copper line inputs of the box |
| 0.4.0 Street and neighbourhood | 4a, 4b, 5a, 5b | Aerial and underground lines, street cabinets |
| 0.5.0 ISP | 6a, 6b, 6c-1, 6c-2 | Rack, ISP premises, public address, address translation |
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

This replaces section 4 of [`docs/repository-security.md`](repository-security.md), written when releases went from `developement` straight to `main`.

### 3. Projects board

Create the board **before** the first `scripts/dev project sync`: the automatic addition only sees issues created or updated after it is turned on.

1. Give gh access to projects: `gh auth refresh -s project` (in a real terminal: it waits for a confirmation in the browser).
2. Create the board, named without the mod's name (here "Roadmap"): profile picture > **Your projects** > **New project** > template **Board** **(to verify)**, or `gh project create --owner <login> --title "Roadmap"`.
3. Link it to the repository: `gh project link <number> --owner <login> --repo <owner>/<repo>`. Give the account login: `--owner @me` is refused by this command.
4. Field **Status**: `scripts/dev project board` creates the missing options, so that the order is **Ideas**, **Todo**, **In Progress**, **In review**, **Done** (the names of the GitHub template are kept). The board's owner and number go in `scripts/project/board.conf`.
5. Project menu (top right) > **Workflows**, turn on and save each one:

| Workflow | Setting | Effect |
|---|---|---|
| Auto-add to project | repository of the mod, filter `is:issue is:open` | Every new issue is added to the board. GitHub Free allows one auto-add workflow. |
| Item added to project | Status: **Todo** | New issues start in Todo. `scripts/dev project board` then moves idea tickets to Ideas. |
| Pull request linked to issue | **Turned off** | It would set In Progress when the pull request of a lot opens, while `scripts/dev lot finish` sets In review. To do by hand: project menu > **Workflows** > **Pull request linked to issue** > switch off **(to verify)**. |
| Item closed | Status: **Done** (on by default) | |
| Pull request merged | Status: **Done** (on by default) | |

Known limits, without Actions:

- "Item added to project" gives the same status to every new item: idea tickets (`type: idea`) also start in Todo. `scripts/dev project board` moves them to **Ideas**.
- No built-in workflow moves an item to **In review** for a single maintainer without reviewers: `scripts/dev lot finish` does it.
- Built-in workflows run on GitHub's side, after the API calls of `scripts/dev`: "Item added to project" may set Todo just after `scripts/dev project board` added an issue and gave it another status. A second run puts idea tickets back in Ideas; an issue given to `--start` must then be moved again **(to watch during the first runs)**.
- Exact names and options of the workflows other than "Auto-add to project", "Auto-archive items" and "Pull request linked to issue" are not listed by the documentation: check them in the **Workflows** menu.

### Board columns and sorting

| Column | Holds | Set by |
|---|---|---|
| Ideas | Ideas not planned yet (label `type: idea`) | `scripts/dev project board` |
| Todo | Planned issues | "Item added to project" workflow, `scripts/dev project board` |
| In Progress | Issues being worked on | `scripts/dev lot start` (question on the terminal), `scripts/dev project board --start <n>` |
| In review | Issues whose pull request is open | `scripts/dev lot finish`, `scripts/dev project board --review <n>` |
| Done | Closed issues | "Item closed" and "Pull request merged" workflows, `scripts/dev project board` |

`scripts/dev project board` (with `--dry-run` first) can be run again at any time. It reads the board named in `scripts/project/board.conf` (`owner=`, `number=`) and the issues of the repository of the `origin` remote, shows what it is going to change and asks for confirmation on the terminal. It:

1. creates the missing Status options. The API replaces the whole list of options: every existing option is sent again with its id, color and description, which keeps the status of the items; the command stops before sending anything if an option would be left out. Options that are not in the list above (such as the template's **No status**) are kept after them;
2. adds to the board the open issues of the repository that are not on it (closed issues missing from the board stay off it);
3. sorts each issue with the first rule that applies:
   - closed issue: **Done**;
   - `--start <n>,<n>`: **In Progress**; `--review <n>,<n>`: **In review**;
   - label `type: idea` with no status or **Todo**: **Ideas** (the label decides: an idea that is taken on changes its label);
   - no status: **Todo** (the option **No status** counts as no status);
   - any other status, set by hand, is left alone.

`scripts/dev lot start` asks which open issues of the milestone go to In Progress (numbers, `all`, or nothing); `scripts/dev lot finish` moves the issues closed by the pull request to In review. In both, a board problem (no `board.conf`, gh without the `project` scope) is only a warning that gives the `project board` command to run once it is fixed. Done is left to GitHub's workflows, at the merge.

gh needs the `project` scope for all of this: `gh auth refresh -s project`, in a real terminal.

To try the command on a throwaway copy of the board before the real one (what `gh project copy` copies is not documented: if the copy has no items, seed a few, so that the trial checks that a status set before the options change is kept):

```bash
gh project copy <number> --source-owner <login> --target-owner <login> --title "Roadmap trial"   # note the number of the copy
gh project item-list <copy> --owner <login> --format json -L 100 --jq '.items | length'        # 0: seed the copy
gh project item-add <copy> --owner <login> --url https://github.com/<owner>/<repo>/issues/<n>
gh project item-edit <copy> --owner <login> --url https://github.com/<owner>/<repo>/issues/<n> --field Status --value "In Progress"
sed -i '' 's/^number=.*/number=<copy>/' scripts/project/board.conf                            # not committed
scripts/dev project board --dry-run
scripts/dev project board
scripts/dev project board --dry-run          # must answer "up to date"; issue <n> still In Progress; columns in order in the browser
gh project delete <copy> --owner <login>
git checkout -- scripts/project/board.conf
```

### 4. Labels, milestones and issues

Once the board exists:

```bash
scripts/dev project sync --dry-run   # shows what would be created, renamed or updated
scripts/dev project sync             # asks for confirmation, then creates
```

Run them in a real terminal: like every `scripts/dev` command that asks for a confirmation, the sync reads the answer from the terminal and answers no when there is none (for example in a shell run by an AI assistant). A second run answers "everything is up to date".

The issue templates apply the `type: bug` and `type: idea` labels, which exist only after this first sync.

## Sources

GitHub documentation read on 2026-11-19, for `scripts/dev project board`:

- Using the API to manage Projects (project by number, fields and options, `addProjectV2ItemById` returns the existing item, `updateProjectV2ItemFieldValue`): https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-api-to-manage-projects
- GraphQL reference of Projects (`updateProjectV2Field`: "provided values overwrite existing options"; `ProjectV2SingleSelectFieldOptionInput.id`: "Include this to preserve the option's identity during updates, preventing item field values from being cleared"; option colors): https://docs.github.com/en/graphql/reference/projects
- The `id` input of the options, added in the schema of 2026-04-23: https://docs.github.com/en/graphql/overview/changelog
- `Issue.projectItems`: https://docs.github.com/en/graphql/reference/issues ; `repositoryOwner` and the `ProjectV2Owner` interface: https://docs.github.com/en/graphql/reference/repos
- gh 2.103.3: `gh api --help` (GraphQL pagination with `$endCursor`, `--input`), `gh project copy --help`, `gh project delete`.

Not confirmed by the documentation: that the order of the options sent gives the order of the columns, what `gh project copy` copies (workflows, items), and the exact behaviour of `gh api graphql --input` with a full request body (described only in general terms by `gh api --help`).

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
