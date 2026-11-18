# Going public

What the maintainer does by hand, once the rename branch (`chore/rename-and-english`) is merged into `developement`, to move the repository to its new name and make it public. Nothing here is run by a script or by an AI assistant. Written on 2026-11-18.

Run the commands from the root of the local clone, in a **real terminal**: several of them ask for a confirmation, and without a terminal the answer is no. `<login>` is the GitHub account that owns the repository. `{owner}/{repo}` is written as is: `gh api` fills it from the current repository.

Every `gh` option below was checked with `--help` (gh 2.103.3) unless it is marked **(to verify)**.

## 1. Rename the electricity milestone

The lot of electricity used to be named with an accented capital E; it is now plain "E" everywhere in the repository (`scripts/project/milestones.tsv`). `scripts/dev project sync` recognises a milestone by its **title**: rename the milestone on GitHub first, otherwise the next sync creates a second milestone "Lot E: Electricity" next to the old one.

```bash
gh api 'repos/{owner}/{repo}/milestones?state=all' --jq '.[] | select(.title | endswith(": Electricity")) | "\(.number) \(.title)"'
# one line expected: the number and the old title. Put that number below (5 is an example):
number=5
gh api --method PATCH "repos/{owner}/{repo}/milestones/$number" -f title="Lot E: Electricity"
scripts/dev --dry-run project sync   # expected: no milestone to create
```

The descriptions of the milestones follow `milestones.tsv` at each sync: the next `scripts/dev project sync` updates the description of "v0.2.0: Power and home network", which also used the old name of the lot.

## 2. Rename the repository

```bash
gh repo rename world-wide-blocks   # asks for confirmation
git remote -v                       # origin must point to .../world-wide-blocks
```

If `origin` still shows the old name, point it to the new one, keeping the same protocol (SSH or HTTPS) as before:

```bash
git remote set-url origin git@github.com:<login>/world-wide-blocks.git      # SSH
git remote set-url origin https://github.com/<login>/world-wide-blocks.git  # or HTTPS
git fetch origin && git remote set-head origin --auto
```

Whether `gh repo rename` updates the `origin` remote itself is not said by its help: check with `git remote -v`. GitHub redirects the old address to the new one, but do not rely on it: a new repository created later under the old name would break the redirection.

## 3. Update the text of the existing issues

The issues created before the rename still link to the old document names (`docs/plan-lots.md`…) and use the old name of lot E. Their definition files are up to date; this rewrites the body of the issues on GitHub from them:

```bash
scripts/dev --dry-run project update-issues   # shows the difference for each issue, changes nothing
scripts/dev project update-issues             # asks for confirmation, then rewrites the bodies
```

Only the body changes (title, labels, milestone, state and comments are left alone). An issue whose body has a ticked box on GitHub is skipped and listed: rewriting it would untick the box, so fix its links by hand.

## 4. Description and topics

Texts and command in [`listing.md`](listing.md), section "GitHub repository".

## 5. Social preview image

Image of 1280 × 640 pixels, PNG or JPG, under 1 MB: see [`listing.md`](listing.md), section "Social preview image". Repository **Settings** > **General** > **Social preview** > **Edit** > **Upload an image**.

## 6. Projects board

The board is called "Roadmap": the mod's name is not in it, nothing to rename. Projects are linked by number, not by repository name, so the link survives the rename. To rename it anyway:

```bash
gh project edit <number> --owner <login> --title "<new title>"
```

## 7. Check the license

The repository holds the LGPL in `COPYING.LESSER` and the GPL it builds on in `COPYING`. GitHub detects a license from these files and may show the GPL, the LGPL or "Other":

```bash
gh repo view --json licenseInfo --jq '.licenseInfo'   # field name (to verify)
```

Also look at the right column of the repository home page. Expected: GNU Lesser General Public License v3.0. If GitHub shows something else, the mod's license is still the one declared in `gradle.properties` (`LGPL-3.0-only`) and in the README; the usual fix is to keep the LGPL text in a file GitHub reads first (`LICENSE`), **to decide** before going public.

## 8. Make the repository public

Check first that `developement` and `main` are pushed, that the README reads well on GitHub, and that the history holds nothing private (audit of session A, `tasks/todo.md`).

```bash
gh repo edit --visibility public --accept-visibility-change-consequences
```

Or: repository **Settings** > **General** > **Danger Zone** > **Change visibility** > **Make public**.

## 9. Right after: protections and secret scanning

In the order of [`repository-security.md`](repository-security.md):

1. **Branch protection** of `main` and `developement` (rulesets): section 5. Rulesets are only available on a public repository with GitHub Free, hence this order.
2. **Secret scanning** and **push protection**: section 6. From the command line:

   ```bash
   gh repo edit --enable-secret-scanning
   gh repo edit --enable-secret-scanning-push-protection   # secret scanning must be enabled first
   ```

3. **Private vulnerability reporting**: section 7 (needed by `SECURITY.md`).
4. **Dependabot alerts**: section 8.

## 10. Optional: local folder

The local clone can keep its folder name; to rename it, close the editors and terminals that use it, then rename the folder. An AI assistant that keeps notes per project folder (for example Claude Code) loses them with the old path: move them or start again.
