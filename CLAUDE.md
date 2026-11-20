# CLAUDE.md

Guidance for Claude Code (claude.ai/code) in this repository.

**Always answer the maintainer in French.**

## Project

A NeoForge Minecraft mod that simulates realistic computer networks, "a Packet Tracer made of blocks". Done: lots 0 (PC tower, laptop), 1a (Ethernet cable, ports, status panel, network engine), 1b-1 (internet box, MAC addresses, switching) and the message module.

Read first:
- [`CONTRIBUTING.md`](CONTRIBUTING.md): branches, commits, hooks, versions and releases, `scripts/dev`;
- [`docs/project-management.md`](docs/project-management.md): roadmap, issues, labels, milestones, GitHub settings;
- [`docs/lot-plan.md`](docs/lot-plan.md): design of each lot, decisions, in-game check procedures;
- [`docs/automated-checks.md`](docs/automated-checks.md): how changes are checked, what stays by hand;
- [`docs/messages.md`](docs/messages.md) and [`docs/network-equipment.md`](docs/network-equipment.md) when the task touches them;
- [`tasks/lessons.md`](tasks/lessons.md): mistakes not to repeat.

## How work is organised

1. **Tasks are GitHub issues**, grouped by milestone (one per lot, one per version) and shown on the "Roadmap" Projects board. Their definitions are in `scripts/project/issues/` (one folder per lot or version, plus `backlog/` and `ideas/`). `docs/lot-plan.md` stays the design document; `tasks/todo.md` only holds session notes.
2. **A lot starts with `scripts/dev lot start <id>`, run by the maintainer.** At the start of a session, read the issue definitions of the lot's milestone in `scripts/project/issues/<id>/`; for their state on GitHub, ask the maintainer to run `scripts/dev status` or `gh issue list --milestone "<milestone title>" --state all`. One commit per step, in English, only after the maintainer's green build. The maintainer ends the lot with `scripts/dev lot finish`.
3. **To propose an issue**, add its definition file in `scripts/project/issues/` (format at the top of `scripts/lib/project.sh`) and ask the maintainer to run `scripts/dev project sync`. Never create anything on GitHub.
4. **Never push, merge, tag, release or change GitHub settings.** These actions go through the maintainer and `scripts/dev`. Commands that ask for a confirmation (`scripts/dev`, `gh auth`) must be run by the maintainer in a real terminal: through the assistant's shell, the answer is always no.
5. **Nothing personal or secret in the repository** (no local paths, no personal data, no credentials). Everything in the repository is in English (commits, changelog, issues, docs, code, comments, `tasks/`), except the French game texts of `fr_fr.json`. Older commit messages and old changelog entries stay as they are.

## End of every task

Every session follows this routine at the end of each task (a step of a lot, a fix, a document), before handing over:

1. Say which issue moved forward and what was done.
2. Tick, in its definition file in `scripts/project/issues/`, only the boxes that are done and proved (green build or check reported by the maintainer). Never tick on GitHub.
3. Write the follow-up comment, in English, in `build/issue-comments/<n>.md`: what was done, how it was checked, what is left.
4. Give the maintainer one block of commands to run in a real terminal:
   ```bash
   scripts/dev project board --start <n>    # or --review <n> when its pull request is open outside lot finish
   scripts/dev project update-issues        # carries the ticked boxes to GitHub; boxes ticked there stay ticked
   gh issue comment <n> --body-file build/issue-comments/<n>.md
   ```
5. Update `tasks/todo.md`.

## Automated checks

Details in [`docs/automated-checks.md`](docs/automated-checks.md).

- Every check is automated whenever it can be: unit tests, game tests, and for anything visual a capture run (the game opens a world on its own, builds the scene, takes screenshots, restores the settings and quits); the assistant then reads the screenshots and reports.
- The maintainer does nothing by hand except what cannot be automated, each such step justified in one line: choices of taste (font, colours, icons: prepare the options side by side), real keystrokes through the operating system (keyboard layouts, dead keys), actions on the GitHub account that ask for the maintainer's confirmation. Launching a Gradle command the sandbox cannot run is not a check by hand.
- When a lot needs a check by eye, it is written as a capture run, not as a manual procedure.
- Never ask the maintainer to run a check that a command could run.

## Constraints of the sandbox

- Gradle cannot run in Claude Code's sandbox: the maintainer runs `./gradlew build test`, `./gradlew runGameTestServer` and the capture runs (`./gradlew runClientCapture -Pscenario=<name>`), and reports the result. Never claim a build or a test passed without that report.
- The sandbox reads neither `~/.gitconfig` nor `~/.ssh`. Prefix git commands with `GIT_CONFIG_GLOBAL=/dev/null XDG_CONFIG_HOME=$TMPDIR/xdg` (otherwise gitleaks sees git's warning and blocks the commit), and commit with the identity of the existing commits passed explicitly (`git log -1 --format='%an <%ae>'`, then `git -c user.name=… -c user.email=… commit`), never an identity guessed from the machine. These commits are unsigned (`docs/repository-security.md`, section 3.8).
- Commit messages: Conventional Commits, checked by the `commit-msg` hook; `!` marks a change that breaks existing worlds.

## Versions and API

- Minecraft 26.3, NeoForge 26.3.0.103, `net.neoforged.moddev` 2.0.148, Gradle 9.2.1, Java 25 (downloaded by the toolchain). Mod identity and version only in `gradle.properties`, injected into `neoforge.mods.toml`.
- The APIs changed a lot since 1.21. Check every class, method, enum constant and import package in the sources before using it: Minecraft in `build/moddev/artifacts/minecraft-patched-*-sources.jar`, NeoForge in its `-sources` jar on maven.neoforged.net.

```bash
./gradlew build test         # build, then JUnit tests of the engine (plain Java)
./gradlew runClient          # development client
./gradlew runGameTestServer  # automatic game tests (src/gametest), throwaway world run-gametest/
```

## Network engine rules

- `network/` (graph, devices, cables, ports, chunk index, `sync/PlayerLinkView`) is plain Java, tested in `src/test`. `NetworkSavedData` saves it in the server's global storage, independently of loaded chunks. `NetworkManager.of(server)` is a stateless facade; `NetworkDevices` links the world to the engine.
- The server decides, the client displays: no network logic on the client, no `client/` class loaded on a dedicated server.
- No static state: data lives in the running server (global storage, player attachment) or in the client instance.
- Every change to the network goes through `NetworkManager`, which marks the save dirty and notifies clients.
- A player only receives the cables with one end in a chunk their client has received (`PlayerLinkView`).
- Real protocol exchanges: real Ethernet frames (then DHCP, ARP, ICMP) travel through the engine, delivered instantly through a queue processed each server tick, with a limit of frames per tick.

## Messages to players

Every message goes through the message module (`message/`, full rules in `docs/messages.md`):

- Server side, `MessageCenter.of(server)`: `gesture(player, message)` answers a gesture, `event(problem key, …)` reports a network event. The caller gives a level, a language key and values; the channel follows from the use and the level, never from the caller.
- The server never sends written text or formatting: key, values and level only. The client alone decides how it looks (`client/ClientMessages`, `client/PortStatusLayer`). Only exception: the output of the terminal commands, literal English built by the server (`docs/messages.md`, section 3).
- A problem is reported once per minute at most; the limit of a network is forgotten when that network changes.
- Status panel content is built in plain Java (`StatusPanel`). Technical details go to the log, in English, at DEBUG level (`Diagnostics`).
- Both language files `en_us.json` and `fr_fr.json` keep the same keys (`LangFilesTest`); a new key of a checked family needs its sample values in `LangFilesTest` (80 characters for the action bar, 45 for the panel and tooltip, 90 for the chat).
- Tests capture messages with `NetworkScene.messages()`, never through a player's chat.

## Adding a block

Hand-written files under the same `<id>` (no datagen):
- Java: class in `block/` (extend `NetworkDeviceBlock` for a network device), `ModBlocks`, `ModItems`, `ModCreativeTabs`; new registries go in the constructor of the main mod class.
- `assets/<mod_id>/`: `blockstates/<id>.json`, `models/block/<id>.json`, `textures/block/<id>.png`, `items/<id>.json`, keys `block.<mod_id>.<id>` in both language files.
- `data/`: `<mod_id>/loot_table/blocks/<id>.json` and the id in `minecraft/tags/block/mineable/pickaxe.json`.

`.bbmodel` files may stay next to the resources: they are excluded from the jar.
