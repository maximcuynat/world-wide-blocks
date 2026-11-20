# Lessons

## Git

- Before creating a requested branch, list the existing branches (`git branch -a`): the development branch of the project is `developement`. If the instruction and the state of the repository differ (e.g. `main` is behind), ask instead of creating one more branch. (2026-11-17)

- Only commit code that passed `./gradlew build test` on Max's machine. Splitting a commit by hand (removing log lines, splitting files) creates a state that was never compiled: run it through the build before committing, or commit only the state that is already compiled. Commits c178668 and 5014267 were made without a build. (2026-11-18)

- Several sessions can share the repository (worktrees such as `../netcraft-proto`). Right before any `git switch`, run `git status` and `git worktree list`: never switch a folder whose uncommitted changes are not mine, and stay in the folder the maintainer named. A switch carried another session's prototype changes onto a new branch. (2026-11-19)

## Tests

- The engine has global limits (frames per tick, queue size, report limit) shared by everything that runs on the server. An automatic test that touches these limits runs apart, in its own batch, so that its result does not depend on the other tests or on the order of execution. (2026-11-18)

- `runGameTestServer` runs a dedicated server: no client code (message display, panel, client settings, tooltips) runs there. A green build and green game tests prove nothing on the client side: tell Max so when delivering a step that touches it, and keep the display logic in plain, testable Java (`StatusPanel`) so that the client only draws. (2026-11-18)

## Network engine

- Deferring a task (an event held until the next tick) opens a window where the state has changed: revalidate each pending change against the current state (device or cable still present) when applying it. A computer placed, cabled and removed in the same tick crashed the test server (2026-11-18). Write the test "added then removed before the tick" with every kind of pending queue.
- An exception in the simulation tick takes the whole server down: every path called from the tick must tolerate a device or a cable that has disappeared. (2026-11-18)

## Shared shell

- Max's `!` commands run in the current folder of my session. Never end a command in a subfolder (`cd src/...`): do the `cd` in a subshell `( cd … && … )` or use absolute paths, and check that the shell is at the project root before giving a checkpoint. A `./gradlew` failed for this reason. (2026-11-17) The `cd` of a command carries over to the next one: a `cd run-capture/logs && …` or `cd src/main/.../os && …` left the session there on 2026-11-20. Never start a command with `cd` into a subfolder; give paths from the root instead.

## Java

- Never write a backslash followed by `u` in Java source, comments included: the compiler reads it as a unicode escape before anything else. `{@code \u@\h}` (the `PS1` of bash) in a javadoc stopped the compilation (2026-11-20, caught by the local compilation before the maintainer's build). Describe such text in words, or double the backslash.

## Minecraft / NeoForge API

- Checking that a method exists is not enough: read what it does with edge values before using it. `GuiGraphicsExtractor.enableScissor` was checked by name only; `GuiRenderer.enableScissor` clamps the area to the window and a height of 0 throws, so a scissor recorded before a window resize crashed the client (2026-11-19, terminal prototype; copied from the prototype branch on 2026-11-20). In screens, prefer code that cannot fail on a resize (cut the text with `Font.plainSubstrByWidth`).

- Never write a Minecraft constant, enum or method name from memory: look it up in the extracted sources (`build/moddev/artifacts/minecraft-patched-*-sources.jar`, and the NeoForge sources from maven.neoforged.net) before using it. Example: `PushReaction.BLOCK` became `PushReaction.IMMOVEABLE` in 26.3, the build failed. (2026-11-17)
- Also check the exact package of each import, not only that the class exists: `RegisterClientPayloadHandlersEvent` is in `neoforge.client.network.event`, not in `neoforge.network.event`. Before a commit, check all `net.neoforged.neoforge.*` imports against the sources (one file per import). (2026-11-17)

- In a subclass of a Minecraft class, an inherited nested type (from the class or its interfaces) hides an import of the same name: in a subclass of `ServerPlayer`, `Connection` means `WaypointTransmitter.Connection`. Before extending a game class, list the nested types of its hierarchy and fully qualify any name that conflicts. (2026-11-18)

## Interaction design

- For each gesture that consumes an item in survival, check that the reverse gesture stays possible when the player no longer has the item: unplugging required a cord in hand, impossible after using the last one. Walk through the "survival, a single copy" path in your head before proposing an interaction. (2026-11-17)
- Before assigning a gesture (sneak + right-click, etc.), reread the plan for the gestures already promised to later lots (here the laptop screen in lot 1c). (2026-11-17)

## Issue boxes

- A box is ticked only if what it says is proven, word for word: split the line into its claims and name the proof of each (real capture, test, green build) before ticking. The box "output checked on a real machine ... ; a timeout prints nothing, as on Linux" of #14 was ticked while the timeout had only been read in the sources: no real run had a request leave and get no answer (captured afterwards, 2026-11-19). When the maintainer reports a box as unproven, check the evidence claim by claim before changing it, and say which claims hold. (2026-11-19)

## GitHub CLI and assisted commands

- Never give a `gh` command as working without checking its `--help` or its documentation: `gh project link --owner @me` was refused, the login is needed (`--owner <login>`). Mark an unchecked command as such. (2026-11-18)
- A command that waits for an answer (`scripts/dev` confirmations, `gh auth refresh`) cannot be run through the assistant's shell (`!` included): tell the maintainer to run it in a real terminal. (2026-11-18)

## Shell (zsh) and text tools

- zsh does not split an unquoted variable into words: `perl -i script $files` received the whole list as one file name, and `git grep … -- $paths` searched one path that does not exist and printed nothing. Pass lists through `xargs -0` or write the paths out, and never take an empty search result as proof before checking that the search can find something. (2026-11-18)
- `perl -CSD` decodes the file but not a non-ASCII letter written in the expression (without `use utf8`): the substitution silently did nothing. After any bulk replacement, search again for what was replaced. (2026-11-18)
- In this shell `cp` and `rm` ask before acting and wait without a terminal (`rm -r` blocked a command until its timeout): use `/bin/cp -f` and `/bin/rm -f` in scripts. (2026-11-18, `rm` added 2026-11-19)

## Translation and delegated rewrites

- A translation delegated to agents is checked by a tool, not by their report: compare the code without comments with the previous commit (only consistent renames allowed), and look for renames to a name that already exists in the file, which can change what a name refers to and still compile. (2026-11-18)
- Agents fill gaps with plausible inventions: a log tag that never existed, English game texts quoted for a check made in a French game. Reread every historical statement of a translated document: a fact must stay true, even when the wording changes. (2026-11-18)

