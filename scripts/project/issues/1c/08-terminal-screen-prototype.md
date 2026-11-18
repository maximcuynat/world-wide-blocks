Title: Throwaway prototype of the terminal screen
Labels: type: chore, area: interface, priority: high
Milestone: lot 1c

To do before the lot, to remove the unknowns of the terminal screen (`docs/lot-plan.md`, lot 1c) before committing to its design. Throwaway code on a separate branch, never merged; what is learned goes into the design of the lot.

- [ ] Keyboard input: the screen receives the typed characters without an edit box (input goes through SDL3 in 26.3), and the game shortcuts do not steal letters
- [ ] A monospaced font: columns stay aligned
- [ ] A character grid that fits at 854 x 480 and at 1920 x 1080
- [ ] The screen does not pause the game in single player
- [ ] Findings written in the issue, with the classes and methods checked in the 26.3 and NeoForge sources
