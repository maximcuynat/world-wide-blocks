# Session notes

Session notes only. Tasks are GitHub issues, grouped by milestone (one lot, one version): `scripts/dev status` and `docs/project-management.md`. The open tasks and the ideas that used to be here became issues on 2026-11-18 (`scripts/project/issues/`, created by `scripts/dev project sync`).

## Blocking for publication

- [x] **Rename the mod before making the repository public.** The former name of the mod was also the name of a British company working in the same field (networking, internet security): risk of a name conflict. Max looked for another name; the rename (display name, `mod_id`, package, resources, GitHub repository, docs) was left to a dedicated session. Nothing is made public, nor published on CurseForge or Modrinth, before. (decided on 2026-11-18) Done on 2026-11-18: renamed World Wide Blocks (session C).

## To do in the renaming session

- [x] Translate the existing content into English: `docs/lot-plan.md`, `docs/messages.md`, `docs/network-equipment.md`, `docs/repository-security.md`, `tasks/`, `CLAUDE.md`, the code and its comments, the hook messages. Max's goal: a repository entirely in English. Since session B, everything new is already in English (commits, changelog, templates, labels, issues, new documents). (decided on 2026-11-18) Done on 2026-11-18 (session C).

## Session B: project management on GitHub and assisted commands (2026-11-18)

Design approved by Max on 2026-11-18: `developement` as default branch ("Closes #" only works towards it), branches `lot/<id>`, `release/v<version>` and `hotfix/<version>`, signed tag set on the merge commit of `main` by `release publish`, roadmap from 0.1.0 to 1.0.0 in the order of `lot-plan.md`, milestones for 0.1.0 and 0.2.0 only, the rest of this file turned into issues (ideas included, rename excluded), notes of 0.1.0-alpha.1 rewritten by hand (review and edit planned in `release publish`). Nothing is created on GitHub by Claude: Max runs the scripts.

One commit per subject (in English), tested in a throwaway clone with fake `gradlew` and `gh`:

- [x] 1. `cliff.toml` in English, "Breaks existing worlds" section first, `CHANGELOG.md` regenerated. Tried on a history with tags on the merge commits of `main`: a lot commit dated before a version but merged after it lands in the next version, as it should, `docs!:` has its section. Found on the way: without `--github-repo`, the comparison links of the changelog stayed empty (`release.sh` of session A); `scripts/dev` passes the repository read from the remote.
- [x] 2. Pre-push hook: Gradle output in `build/pre-push.log`, last 40 lines on failure, code in `scripts/lib/checks.sh` (5 cases tried).
- [x] 3. GitHub templates: bug issue, idea issue (`config.yml` without blank issue), pull request with "Closes #" and the "Breaks existing worlds" box.
- [x] 4. `scripts/dev`: base, `status`, `lot start`, `lot finish`.
- [x] 5. `scripts/dev`: `release prepare`, `release publish`, `hotfix start`; `scripts/release.sh` removed.
- [x] 6. `scripts/dev project sync` and its data: 16 labels, 10 milestones, 28 issues (7 + 7 + 3 for lots 1b-2, 1c, 1d; 4 tasks from this file; 7 ideas).
- [x] 7. `CONTRIBUTING.md` rewritten in English, `docs/project-management.md`, `docs/repository-security.md` (section 4 and table), sentence at the top of `README.md`.
- [x] 8. `CLAUDE.md` (language, branches, tool, issues), this file reduced to session notes, pointer in `docs/lot-plan.md`.

Review of session B (2026-11-18):

- Checked only in throwaway clones, with a fake `gradlew`, a fake `gh` (state in JSON, `--jq` passed to the real `jq`), a throwaway SSH signing key and a pseudo-terminal to answer the questions: each command in dry run (no git change, no GitHub write), without a terminal (answer no), answer no, checks failing, answer yes; empty case (no tag, issue, milestone or pull request); full cycle alpha, beta (freeze, `--allow-features`), release (version milestone closed), fixes 0.1.1 to 0.1.3 (breaking change refused, carry-over with merge commit, conflict on `CHANGELOG.md` kept on the `developement` side, other conflict stopped); `project sync` run again: no write on the second pass.
- Not checked: the real `gh` (milestones API, label renaming, gojq `capture` in `--jq`), the automations of the Projects board, signing with Max's key, the Gradle build in a fresh worktree (duration of the first build), Gradle at all (no mod code touched). First real try of each command by Max with `--dry-run`.
- Choices made by Claude, reported to Max: colours and descriptions of the labels, `type: idea` label in addition to the five requested types, `--allow-features` option, release notes kept in `build/release/` between the dry run and the real publication, release always created as a draft, fix possible only while the version to fix is the last one on `main`, clone URL replaced by an example in `CONTRIBUTING.md` (rename to come).
- Left to Max, in order: GitHub settings of `docs/project-management.md` (default branch, merges, Projects board and its automations), then `scripts/dev project sync --dry-run` and `scripts/dev project sync`, then `scripts/dev status`.
- Follow-up (2026-11-18, done by the maintainer): first push of the rewritten pre-push hook green (real build, unit and game tests); default branch `developement` (`gh repo edit --default-branch`); merge commits only, automatic branch deletion (`gh repo edit`), default merge message checked; "Roadmap" board (#4) linked with `gh project link 4 --owner <login>` (`--owner @me` refused), Status field and workflows set in the interface; `scripts/dev project sync`: 54 changes, issues #1 to #28, second run "everything is up to date". Commands that ask for an answer must run in a real terminal. Docs updated accordingly; `CLAUDE.md` rewritten in English for this organisation. Session B closed.

## Session A: clean, secure and equipped repository (2026-11-18)

Audit of the history done and approved by Max on 2026-11-18: no secret (gitleaks, 57 commits), authors with GitHub's masked address, no unwanted file ever committed, no local path. `CLAUDE.md`, `tasks/` and the mentions of Max stay public. No change to the mod code, no GitHub Actions.

Decisions of Max: LGPL-3.0 license, holder "maximcuynat"; SemVer version `0.1.0-alpha.1` in `gradle.properties` (only source), channel deduced from the suffix (alpha, beta, nothing = release), tag `v0.1.0-alpha.1`, jar `<mod_id>-26.3-0.1.0-alpha.1.jar`; `main` only receives published versions, through a pull request from `developement` merged with a merge commit; the 0.1.0-alpha.1 tag may become a GitHub pre-release, CurseForge and Modrinth wait for the end of lot 1; pre-push hook: tests skipped only if all modified files are `.md` or in `docs/`; gitleaks missing: warning.

One commit per subject, after Max's green build (Gradle does not run in Claude's sandbox):

- [x] 1. Gradle wrapper regenerated as 9.2.1 with the archive checksum (`distributionSha256Sum`). The current jar is an official 8.14.x wrapper, authentic but outdated. Command run by Max, then check: SHA-256 of the jar = `423cb469…` (official 9.2.1 wrapper), `build test` green (Max, 2026-11-18). Line endings set in `.gitattributes` (`gradlew` in LF, `*.bat` in CRLF, `*.jar` binary). The checksum is only checked when the distribution is downloaded: the 9.2.1 distribution already in the cache was not rechecked.
- [x] 2. `.gitignore` completed (`.env`, logs, screenshots, crash reports, macOS and editor files); `git ls-files -ci --exclude-standard` stays empty.
- [x] 3. LGPL-3.0 license: `COPYING.LESSER` (LGPL) and `COPYING` (GPL, which the LGPL refers to), `mod_license=LGPL-3.0-only`. `TEMPLATE_LICENSE.txt` (MIT of the MDK) kept. Build green, `license="LGPL-3.0-only"` in the jar (Max, 2026-11-18). Hooks installed and gitleaks checked on Max's machine.
- [x] 4. Version `0.1.0-alpha.1` and jar name with the Minecraft version. Build and game tests green (FML accepts the version) and `<mod_id>-26.3-0.1.0-alpha.1.jar` produced, `version="0.1.0-alpha.1"` in the jar (Max, 2026-11-18).
- [x] 5. Versioned hooks (`.githooks/`) and `scripts/install-hooks.sh`: commit messages (replayed on the 57 existing subjects), gitleaks before commit, build and game tests before push (except `.md` and `docs/` only; clean tree required). Pre-push hook tested in a throwaway clone with a fake `gradlew` (11 scenarios). The pre-commit hook also blocks when gitleaks reports an error: in a trial, gitleaks answered "no leaks found" while git had failed. Installed and checked on Max's machine.
- [x] 6. Releases: `cliff.toml` (Keep a Changelog in French at the time: Added, Changed, Fixed), `CHANGELOG.md`, `scripts/release.sh` (`--dry-run` mode, signed commit and tag, confirmation before any push, GitHub release as a draft). Full flow tried in a throwaway clone with fake `gradlew` and `gh` (refusal, atomic push, pull request, safeguards of `--publish`, draft as a pre-release, no terminal = no). First real try: `scripts/release.sh --dry-run 0.1.0-alpha.1` by Max.
- [x] 7. `README.md` (French and English), `SECURITY.md` (private report through GitHub), `CONTRIBUTING.md` (commits, branches, hooks, tests, releases).
- [x] 8. `docs/repository-security.md`: GitHub settings for Max to make, interface paths, order of operations, SSH signing of commits and tags.
- [x] 9. `CLAUDE.md`: hooks, release script, branch model. Review of the session below.

Review of session A (2026-11-18):

- Done: audit, 10 commits on `developement` (masked identity, unsigned), all after a green build by Max or with no effect on the build (hooks, docs, release tooling).
- Really checked: official 9.2.1 wrapper, build and tests, license and version in the jar, game tests with `0.1.0-alpha.1`, gitleaks hook on Max's machine. Checked only in throwaway clones: pre-push hook (fake `gradlew`), `scripts/release.sh` (fake `gradlew` and `gh`, signing with a throwaway key). Max's first push will be the first real run of the pre-push hook.
- Left to Max: `scripts/release.sh --dry-run 0.1.0-alpha.1` (first real try of the script), SSH signing then GitHub settings in the order of `docs/repository-security.md`, choice 3.8 (signing Claude's commits) before requiring signed commits on `main`.
- Blocking before making the repository public: rename of the mod (dedicated session).
- Choices made by Claude and reported to Max: `LGPL-3.0-only` rather than `-or-later`, `COPYING` and `COPYING.LESSER` files instead of a `LICENSE` (check GitHub's license badge after the push), commitments of `SECURITY.md` (latest version only, no guaranteed delay) and fork then pull request path of `CONTRIBUTING.md`.
- GitHub project management (labels, milestones, board): session B.

## Step 1: dev environment

- [x] Choose the loader and the Minecraft version: NeoForge 26.3.0.103 on Minecraft 26.3 (the mod must first work alone)
- [x] JDK 25: downloaded automatically by Gradle at the first build (foojay toolchain), the Homebrew Java 17 is enough to run Gradle 9.2.1
- [x] Generate the project from the official MDK-26.3-ModDevGradle template
- [x] Set the mod identity (mod identity: the former id, package and name, replaced on 2026-11-18; version 0.1.0)
- [x] Initialise git and push to the private GitHub repository (commit 41eadcb, run by Max)
- [x] `./gradlew build` passes: BUILD SUCCESSFUL in 3 min 53 s, the jar produced in `build/libs/` (run by Max, Gradle cannot open a local socket in Claude's sandbox)
- [x] The game starts through `./gradlew runClient` (confirmed by Max on 2026-11-17). Test world: a creative world, superflat Redstone Ready. Presence of the mod in the Mods screen not explicitly confirmed.

## Blocks

- [x] PC tower (`pc_tower`): block, model, texture
- Issue: laptop (`laptop`), code and resources written on 2026-11-17, in-game check to do (`scripts/project/issues/0.1.0/01-laptop-in-game.md`).

## Lot plan

The full breakdown (lots, equipment, dependencies) and its approval status are in `docs/lot-plan.md`; roadmap of the versions in `docs/project-management.md`.

Inventory and real dimensions: `docs/network-equipment.md`.

- [x] Lot 1a (cable and connection): coded and approved in game on 2026-11-17 (procedure in `docs/lot-plan.md`).
- [x] Principle "real protocol exchanges" in `docs/lot-plan.md` and `CLAUDE.md` (commit 132fbc6).
- [x] Lot 1b-1 (box, MAC, switching): approved by Max on 2026-11-18 (state and test method in `docs/lot-plan.md`, lot 1b). Steps, one commit each:
  - [x] Typed ports (Ethernet, line) and MAC per port in the graph, save compatible with lot 1a worlds, tests (53 tests green)
  - [x] Frame layer: `MacAddress`, `EtherType`, `EthernetFrame`, typed content, tests
  - [x] Reusable switching: learning table with expiry, forwarding decision, tests (75 tests green)
  - [x] Simulation: delivery queue, timers, tap, loop detection and per-tick limit, computer and box logic, device role, tests (87 tests green)
  - [x] Minecraft connection: server tick, cable events, loop message
  - [x] Internet box block (7 x 7 x 3 px model, provisional texture, resources)
  - [x] Indicator: computer MAC, MACs learned by the box (status packet), green build, in-game trial to do
  - [x] Cord bug: the pending state stayed attached to a broken start point (reported by Max on 2026-11-17). Reproduced by `PendingCableTest`, fixed (cancel also on dimension change)
  - [x] Remove the temporary trial log lines of lot 1b-1 (`debug/TrialLog` and its calls): done at step 5 of the message module; the useful log lines go through `Diagnostics` (DEBUG, English)
  - [x] Check build of c178668 and 5014267 (committed without a build, see `tasks/lessons.md`): green on 5014267 on 2026-11-18
  - [x] Automatic game tests and trial scenes: design approved by Max on 2026-11-18. One commit per step, only after a green build:
    - [x] Source set `src/gametest/` excluded from the jar, wired to the runs; `gameTestServer` run in `run-gametest/` (ignored by git); `./gradlew build` also compiles this source set
    - [x] Empty 16 x 4 x 16 structure, connected test player (modelled on NeoForge's `GameTestPlayer`, messages captured), cleanup of each test's devices at its end, passed or not
    - [x] Tests of the common batch (green on 2026-11-18): start destroyed (break and explosion, creative and survival, cord count), dimension change, disconnection and reconnection, MAC expiry, save and reload
    - [x] Separate batch (dedicated environment) for what touches the engine's global limits: loop and limit of one report per minute
    - [x] Engine clock (requested by Max on 2026-11-18): a change that happened between two ticks took the time of the previous tick, the loop was reported every 90 s instead of 60. Changes applied at the start of the next tick, unit tests of the clock, game tests of the loop (exact gap) and of the expiry (exact bounds) tightened
    - [x] `/worldwideblocks test <scene>` and `/worldwideblocks test clear` (scenes tested in game, check by eye done by Max on 2026-11-18): development only (`gametest` source set), refused outside a world whose name starts with "Trials", refused if the area is not empty
    - [x] Automatic test of the refusal outside a trial world (name rule and real command), second computer on the right box of the `chain` scene (2 MAC on port 1)
  - [x] In-game test procedure (what needs eyes) and state of the lot in `docs/lot-plan.md`
  - [x] Check by eye of the message module: done by Max on 2026-11-18 (game in French, a trial world), no crash, rendering approved as the base of the future HUD (lot F). Table of the 6 points in `docs/lot-plan.md`.
  - [x] Points without a capture (detailed panel of a computer and of the laptop, right box of the `chain` scene, 99-block cable along its whole length, loop messages stopping after a cable is removed, `/worldwideblocks links`, rendering of ✔ and ℹ, fallback `messageSymbols = false`): seen by eye by Max during the same session, approved on his word on 2026-11-18
  - Issue: lot F (HUD interface), rendering on several window sizes (`scripts/project/issues/backlog/02-hud-window-sizes.md`).
  - [x] Message module, before 1b-2: audit and design approved by Max on 2026-11-18 (`docs/messages.md`), 7 steps coded and committed the same day after green builds, check by eye done the same day (result in `docs/lot-plan.md`). Steps:
    - [x] 1. Core in plain Java (`message/`): levels, uses, channels, values, routing, repeat limit, sent message and listener; unit tests of routing and of the repeat limit. No visible change.
    - [x] 2. Transport and display: `MessagePayload` (channel, level, key, values), `MessageCenter` carried by the server data, display decided by the client (symbol or word according to the `messageSymbols` setting), cord messages migrated, game tests of the cord through the listener. `TestPlayer` still captures the chat for the loop test, until step 4.
    - [x] 3. Texts: formal "vous", whole distances, duplicate and `result.ok` removed, common device format, cord tooltip, test of the language files (same keys, maximum lengths).
    - [x] 4. Loop and saturation: one event per network and per tick, per-minute limit in the module, forgotten when the network changes, message to the device closest to the player, saturation to the operators and to the owner in single player; tests of the engine, of the module and in game (a single series of messages for two boxes).
    - [x] 5. Log in English and cleanup: useful log lines passed through the module (DEBUG, English), `TrialLog` and `graphForTrial` removed, exceptions in English.
    - [x] 6. Status panel: lines built in plain Java (compact, detailed when sneaking, "Sneak: details" line), the client only draws; unit tests.
    - [x] 7. Documents: `CLAUDE.md` (log in English, module rules, data separated from drawing), state in `docs/lot-plan.md`, check-by-eye procedure.
- Lot 1b-2 (DHCP): architecture approved with 1b-1, not started. Tasks: issues of the milestone "Lot 1b-2: DHCP" (INIT-REBOOT included).

## Multiplayer tests

Became issues on 2026-11-18: end of lot 1 (milestone "v0.1.0", `scripts/project/issues/0.1.0/02-multiplayer-dedicated-server.md`) and two players towards the end of lot 3 (`scripts/project/issues/backlog/01-multiplayer-two-players.md`). Meanwhile, keep the client and server split strict in the code (see the rules in `CLAUDE.md`).

## Loose ideas

Became `type: idea` issues on 2026-11-18 (`scripts/project/issues/ideas/`). Max's vision: a Packet Tracer made of blocks, faithful to reality, complete, for everyone and for networking enthusiasts. "Computers: PCs to place and cable" is done (lots 0 and 1a) and has no issue.

## Facts checked on 2026-11-17

- Latest Minecraft version: 26.3 (2026-09-15), Java 25. 1.21.1 needs Java 21.
- NeoForge: builds 26.3.0.x available, official templates MDK-26.3-ModDevGradle and MDK-1.21.1-ModDevGradle.
- NeoForge builds on Modrinth:
  - 1.21.1: Mekanism, Create, AE2, CC:Tweaked, Powah, Energized Power
  - 26.3: AE2, CC:Tweaked, Powah, Energized Power (no Mekanism, no Create)
- Immersive Engineering stops at 1.21.1, Thermal Expansion at 1.20.1 (Forge only).
- NeoForge 21.9 (Minecraft 1.21.9) reworked the transfer APIs (energy, items, fluids) and FML: a port from 1.21.1 to 26.x touches the energy layer.
- Machine: Homebrew Java 17 (not enough), VS Code present, IntelliJ absent.
