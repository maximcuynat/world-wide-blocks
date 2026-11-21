# Automated checks

How a change is proved to work. Rule adopted on 2026-11-19: every check is automated whenever it can be, and the maintainer only does by hand what cannot be automated.

## The three kinds of automatic checks

| Kind | Command | What it proves |
|---|---|---|
| Unit tests | `./gradlew build test` | The network engine, the protocols, the panel content and the language files, in plain Java (`src/test`) |
| Game tests | `./gradlew runGameTestServer` | Blocks, gestures, saving and messages on a dedicated server, in a throwaway world (`run-gametest/`) |
| Capture run | `./gradlew runClientCapture -Pscenario=<name>` | What the client draws: screens, panels, cables, chat and action bar |

A capture run is a development client that works on its own: it opens a trial world, builds the scene, sets each window size, GUI scale and language asked for, takes named screenshots, writes the values it reads (text shown, sizes, states) to a file, restores the settings it changed and quits. The assistant then reads the screenshots and the values file and reports what it sees, point by point.

Game tests run on a dedicated server: they prove nothing about what the client draws (`tasks/lessons.md`). Anything visual is therefore a capture run, not a game test.

## Who runs what

Gradle cannot run in the assistant's sandbox, so the maintainer launches the commands above and hands back the result (end of the log, screenshots folder, values file). Launching a command is not a check by hand: the maintainer looks at nothing and decides nothing, the command and the assistant do.

## What stays by hand

Only what cannot be automated, each step justified in one line where it is asked for:

- **Choices of taste** (font, colours, icons): a capture run prepares the options side by side, the maintainer chooses.
- **Real keystrokes through the operating system** (keyboard layouts, dead keys): a run can inject key events into the game, but not test the layout of the system that produces them.
- **Actions on the GitHub account that ask for the maintainer's confirmation** (`scripts/dev`, `gh auth`): they need a real terminal and the maintainer's decision.

The assistant never asks the maintainer to run a check that a command could run. If a check seems to need eyes, it is first written as a capture run; a step that stays by hand says why in one line.

## Checks of a lot

When a lot needs a check by eye, its issue describes a capture scenario (world, scene, sizes and scales, named screenshots, values to write), not a manual procedure. `docs/lot-plan.md` keeps the design; the scenarios live with the code of the capture runs.

## How a capture run works

Since lot 1c-1 (issue #52):

- The code is in the source set `capture` (`src/capture`), a client-only mod of its own, `worldwideblocks_capture`, loaded by the run `clientCapture` alone (`loadedMods` in `build.gradle`); the other runs, the dedicated servers included, load the main mod only. `./gradlew build` compiles it and fails if a file of `gametest` or `capture` is in the jar (task `checkJarContents`).
- `./gradlew runClientCapture -Pscenario=<name>` opens a development client in `run-capture/` (ignored by git; the worlds of `run/` are never opened), creates the flat creative world "Trials capture <name>" (the one of the previous run is replaced), sets noon, pauses the time and clears the weather, places the player at the spawn facing north, then runs the steps of the scenario (`CaptureScenarios`): build or clear a trial scene 3 blocks ahead; aim at a device from 2 blocks south of it, standing or sneaking; view the scene from a place in the air; hold an item; click the use key; hover the held item in the inventory (the mouse is moved as the system would report it); open the chat; type a command; change the game mode; change the client option `messageSymbols` in `config/worldwideblocks-client.toml`; remove a cable between two devices on the server; run game time at once, as `/tick sprint` does; save and quit to the title screen, then open the same world again; open the terminal of a computer by a click (on the keyboard of a laptop), type characters in it (`{address:N}` stands for the address of the DHCP lease of the computer N of the scene, read on the server), check the rows it shows (the run fails unless a regular expression matches the expected number of rows), press its keys (Enter, Esc, Up, Shift+Page Up, Shift+Page Down, Ctrl+C, Ctrl+D, Ctrl+L, Ctrl+=, Ctrl+-, Ctrl+0), as the game hands them to the screen, and turn the mouse wheel over it (`MouseHandler.onScroll`, as the system reports a notch); wait; wait and fail if a line reaches the chat; take a shot.
- Each shot is taken in every combination of language, window size and GUI scale the scenario declares (by default English and French, 854 x 480 and 1920 x 1080, automatic scale), saved as `run-capture/screenshots/<name>/<shot>-<size>-<scale>-<language>.png`, with a line of values in `values.txt` next to it: window, framebuffer and GUI sizes, game time of the client, posture and block under the crosshair as the client shows them, the panel as drawn (`StatusPanelShownEvent`), the screen open, the tooltip of the hovered item, the grid of an open terminal (columns, rows, text size, rows shown) and where its frame, title and text area were drawn, in GUI pixels, the message of the action bar and the chat lines received since the previous shot (`ClientChatReceivedEvent.System` of NeoForge). A shot with a terminal open fails the run when the frame was drawn for another GUI size than the window's, leaves the window, or when its title (accents of the capitals included) or its text area crosses the border of the frame (`TerminalLayout.frameProblem`). A chat line keeps the language loaded when it arrived, as in the game: a scenario that needs a message in both languages makes it arrive twice (`dhcp_conflict`).
- Model check (issue #68): once in the world, before the first step, every run checks that the client draws each block and item of the mod with its own model (`ModelCheck`): no block state falls back to the missing model, no item to the missing item model, no quad of a block state uses the missing texture, nor the texture an item picks for its particles (the quads of an item model are not readable from outside its render state in 26.3: the texture of `layer0` for a flat item, of the block for a block item). Otherwise the run fails with the list, and the command with it. The log gives what was checked (`model check passed (16 block states, 4 items, 548 quads)` on 2026-11-21).
- At the end, or at the first failure, the run puts back the window size, the GUI scale, the language, the options of the game it changed and the client options file of the mod as it was when the run began, writes `run-capture/capture-result.txt` (`passed ...` or `failed: <reason>`), leaves the world and quits. The task `checkCaptureResult`, chained to the run, prints that result and fails the command when the scenario failed or never ended. Every wait of the run has a limit of 60 s, beyond the ticks a wait step asks for.

## Current state

Since step 4 of lot 1c-1, every check by eye of lots 1b-1 and 1b-2 and of the message module is a capture scenario; `docs/lot-plan.md` names the scenario of each point:

- Lot 1b-1 and the message module: `box`, `box_hosts`, `chain`, `loop` (with a cable of the loop removed and a minute and a half of silence), `full_ports` (with and without symbols), `distance`, `tooltip`.
- Lot 1b-2: `dhcp_addresses` (with a cable unplugged and plugged again by the player's gestures), `dhcp_renewal` (`/tick sprint`), `chain`, `dhcp_no_box`, `dhcp_conflict`, `dhcp_pool_full`, `dhcp_server_off`, `dhcp_restart` (save, quit to the title screen, reopen), `loop`.
- Lot 1c-1: `terminal_screen` (issue #11): the terminal of the tower and of the laptop.
- `failure_check` fails on purpose, to prove that a failure puts the settings back.

Still by hand: the in-game procedure of lot 1a in `docs/lot-plan.md` (written before the capture runs; its gestures and the restart of the game are not scripted yet).
