Title: Permanent capture runs for the checks by eye
Labels: type: feature, area: tooling, priority: high
Milestone: lot 1c-1

Make the automatic capture mode of the terminal screen prototype (`runClientCapture`, issue "Throwaway prototype of the terminal screen") permanent, so that every check by eye becomes a command whose screenshots the assistant reads. Rule and context in `docs/automated-checks.md`.

A capture run is a development client that works on its own: it opens a trial world, builds the scene, takes named screenshots, writes the values it reads, restores the settings it changed and quits.

- [x] Code in a development-only source set (the existing `gametest` set or a new one), absent from the distributed jar: checked on the jar built by `./gradlew build`
- [x] Client capture code never loaded by the dedicated server of `runGameTestServer`
- [x] A Gradle run (`runClientCapture`) that takes a scenario name and quits on its own when the scenario ends, with an exit code and a log line that say whether it went through
- [x] The world is a trial world created by the run, never one of the maintainer's worlds (`run/saves` untouched)
- [x] Settings changed by the run (window size, GUI scale, language, client options of the mod) restored before quitting, also when the scenario fails
- [x] Reusable scenarios, one per lot or check: trial world, scene (`/worldwideblocks test` scenes reused), player position and aim, sneaking or standing
- [x] Combinations of window sizes and GUI scales (at least 854 x 480 and 1920 x 1080) and of languages (English and French) declared by the scenario
- [x] Named screenshots (scenario, step, size, scale, language) in a folder of their own, ignored by git
- [x] A values file next to the screenshots (text shown, states, sizes read at each step), so that the report rests on values as well as on images
- [ ] The eye check of lot 1b-1 (`/worldwideblocks test` scenes `box`, `box_hosts`, `chain`, `loop`, `full_ports`, `distance`) and of the message module (`docs/lot-plan.md`) written as capture scenarios
- [ ] The ten points of the eye check of lot 1b-2 (`docs/lot-plan.md`) written as capture scenarios; the restart after quitting to the title screen and the client setting `messageSymbols` scripted too, or left out with a one-line reason
- [ ] The manual procedures of lots 1b-1 and 1b-2 and the "Test method" paragraph of `docs/lot-plan.md` replaced by the names of the scenarios and the command to run
- [ ] A first run of each scenario by the maintainer, screenshots and values read and reported by the assistant
