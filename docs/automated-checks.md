# Automated checks

How a change is proved to work. Rule adopted on 2026-11-19: every check is automated whenever it can be, and the maintainer only does by hand what cannot be automated.

## The three kinds of automatic checks

| Kind | Command | What it proves |
|---|---|---|
| Unit tests | `./gradlew build test` | The network engine, the protocols, the panel content and the language files, in plain Java (`src/test`) |
| Game tests | `./gradlew runGameTestServer` | Blocks, gestures, saving and messages on a dedicated server, in a throwaway world (`run-gametest/`) |
| Capture run | `./gradlew runClientCapture` (to come, see below) | What the client draws: screens, panels, cables, chat and action bar |

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

## Current state

The capture run exists only in the throwaway prototype of the terminal screen (lot 1c-1). Making it permanent, in a development-only source set absent from the jar, and turning the eye checks of lots 1b-1 and 1b-2 into capture scenarios is a lot 1c-1 issue ("Permanent capture runs for the checks by eye"). Until it is done, the procedures of `docs/lot-plan.md` with `./gradlew runClient` still apply.
