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
- `./gradlew runClientCapture -Pscenario=<name>` opens a development client in `run-capture/` (ignored by git; the worlds of `run/` are never opened), creates the flat creative world "Trials capture <name>" (the one of the previous run is replaced), sets noon, pauses the time and clears the weather, places the player at the spawn facing north, then runs the steps of the scenario (`CaptureScenarios`): build or clear a trial scene 3 blocks ahead; aim at a device from 2 blocks south of it, standing or sneaking; view the scene from a place in the air; hold an item; click the use key; hover the held item in the inventory (the mouse is moved as the system would report it); open the chat; type a command; change the game mode; change the client option `messageSymbols` in `config/worldwideblocks-client.toml`; wait; take a shot.
- Each shot is taken in every combination of language, window size and GUI scale the scenario declares (by default English and French, 854 x 480 and 1920 x 1080, automatic scale), saved as `run-capture/screenshots/<name>/<shot>-<size>-<scale>-<language>.png`, with a line of values in `values.txt` next to it: window, framebuffer and GUI sizes, posture and block under the crosshair as the client shows them, the panel as drawn (`StatusPanelShownEvent`), the screen open, the tooltip of the hovered item, the message of the action bar and the chat lines received since the previous shot (`ClientChatReceivedEvent.System` of NeoForge).
- At the end, or at the first failure, the run puts back the window size, the GUI scale, the language and the options it changed, writes `run-capture/capture-result.txt` (`passed ...` or `failed: <reason>`), leaves the world and quits. The task `checkCaptureResult`, chained to the run, prints that result and fails the command when the scenario failed or never ended. Every wait of the run has a limit of 60 s.

## Current state

Since steps 4a and 4b of lot 1c-1: the check by eye of lot 1b-1 (`box`, `box_hosts`, `chain`, `loop`, `full_ports`, `distance`) and of the message module (`tooltip`, the panels, the action bar of `full_ports` with and without symbols, the loop message, `/worldwideblocks links` in `box_hosts`, both languages), and `failure_check`, which fails on purpose to prove that a failure puts the settings back. One point is left to a game test: removing a cable of the loop to stop its messages (`loop_forgotten_only_for_changed_network`). The ten points of lot 1b-2 come with step 4c; until then, their procedure of `docs/lot-plan.md` with `./gradlew runClient` still applies.
