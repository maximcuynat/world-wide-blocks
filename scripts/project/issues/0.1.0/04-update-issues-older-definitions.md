Title: update-issues and sync must not write older issue definitions
Labels: type: bug, area: tooling, priority: high
Milestone: version 0.1.0

`scripts/dev project update-issues` rewrites each issue body from the definition file of the folder it runs in. Run from a branch that lags behind, it writes older texts over newer ones. Seen on 2026-11-20 at 00:29Z: a run from the worktree of `design/equipment`, which does not contain commit 09234ab, put back the texts of before 09234ab on #11, #15, #16 and #45 (shown by `userContentEdits` of each issue); a run from `lot/1c-1` at 00:52Z put the right texts back. Two branches that each run it overwrite each other in turn, and the uncommitted definitions of one are lost on GitHub at the next run of the other. The only guard today is the diff shown before the confirmation.

Until it is fixed, `sync` and `update-issues` are run only from the branch that holds the newest definitions (`CLAUDE.md`).

- [ ] Before rewriting an issue, `update-issues` finds which committed versions of its definition file give the body now on GitHub (ticks aside, as `merge_ticks` compares), across all local branches (`git log --all -- <file>`)
- [ ] If the body on GitHub comes from a commit that the current branch does not contain (`git merge-base --is-ancestor`), the issue is skipped with a message naming the commit and its branches, and nothing is sent for it; the other issues go on
- [ ] A body that matches no committed version (edited by hand on GitHub, or from uncommitted files) is shown with its diff and asked for, as today, with a warning that its origin is unknown
- [ ] `--dry-run` lists the issues it would skip and why
- [ ] `sync` (which never edits an existing issue) checks its other writes the same way: it does not create an issue whose definition file was renamed or removed by a commit the current branch does not contain, and does not put back a milestone or label description older than the one on GitHub
- [ ] A repeatable test, run without GitHub: a throwaway clone with two branches, a fake `gh` that keeps the issue bodies in files; the body of the newer branch is on "GitHub"; `update-issues` from the older branch skips the issue and sends nothing, as the run of 00:29Z should have done; from the newer branch, a newer edit is sent; a body edited by hand is asked for
- [ ] `docs/project-management.md` and `CONTRIBUTING.md` say when the commands refuse, and the temporary rule of `CLAUDE.md` is removed
