Title: Capture scenarios for the in-game check of lot 1a
Labels: type: test, area: tooling, priority: medium
Milestone: version 0.1.0

The in-game procedure of lot 1a (cable and connection, `docs/lot-plan.md`, section "1a: cable and connection") is the last check by eye still done by hand: the checks of lots 1b-1, 1b-2 and of the message module became capture scenarios in lot 1c-1 (issue #52). Rule and capture steps in `docs/automated-checks.md`.

Each point of the procedure becomes a capture scenario (or a step of one), run with `./gradlew runClientCapture -Pscenario=<name>` in English and in French, at 854 x 480 and 1920 x 1080; a point that cannot be scripted is left out with a one-line reason.

- [ ] Panels of a tower and a laptop not connected ("Port 1: free", grey light)
- [ ] Connection with the cord by the player's gestures: action bar after the first device, glowing cord, cable drawn, both panels "connected", `/worldwideblocks links`
- [ ] Unplugging with the cord (sneak and right-click), then the end taken back with an empty hand and moved to a third device, then back to free
- [ ] Link kept while its area is not loaded (player teleported 400 blocks away, `/worldwideblocks links`), cable drawn again on return
- [ ] Saving and restart: the cable and both panels "connected" after reopening the world (step `Restart`); quitting the game process itself left out or scripted, with its reason
- [ ] Refusals in the action bar: device more than 100 blocks away ("Too far"), port in use ("no free port")
- [ ] Breaking a connected device: its cable disappears, the other end goes back to "free"
- [ ] Survival with a single cord: connecting consumes it, taking an end back returns it, breaking a device linked in creative drops no cord (count of cords read in the inventory)
- [ ] The procedure of lot 1a in `docs/lot-plan.md` replaced by the names of the scenarios and the command, as for lots 1b-1 and 1b-2
- [ ] A first run of each scenario by the maintainer, screenshots and values read and reported by the assistant
