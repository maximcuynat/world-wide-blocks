Title: Throwaway prototype of the terminal screen
Labels: type: chore, area: interface, priority: high
Milestone: lot 1c-1

To do before the lot, to remove the unknowns of the terminal screen (`docs/lot-plan.md`, lot 1c-1 and "Decisions taken") before committing to its design. Throwaway code on a separate branch, never merged; what is learned goes into the design of the lot.

- [x] Keyboard input: the screen receives the typed characters without an edit box (input goes through SDL3 in 26.3), and the game shortcuts do not steal letters
- [x] AZERTY and QWERTY layouts: typed characters follow the player's layout; Ctrl+C, Ctrl+L and Ctrl+D are read from the letters of the layout, not from the physical keys; the Up and Down arrows and Shift+Page Up / Page Down reach the screen
- [x] Monospaced font, columns aligned, French accents shown: Monocraft (OFL 1.1, TTF) and the game font drawn cell by cell; Spleen (BSD-2-Clause) only if it brings a real gain in density
- [x] A character grid whose columns and rows follow the window size, at 854 x 480 and at 1920 x 1080
- [x] Minecraft-style frame with a title bar, game colours on a dark opaque background, blinking block cursor; text with and without shadow
- [x] The screen does not pause the game in single player
- [x] Screenshots of each font at both sizes, for the final choice with Max
- [x] Findings written in the issue, with the classes and methods checked in the 26.3 and NeoForge sources
- [x] Automatic capture run (`runClientCapture`): the game opens the world, takes the screenshots of each combination, writes the values, restores the settings and quits; made permanent in issue #52

Result (2026-11-19): Monocraft v4.2.1 chosen; the game font, as it is or drawn cell by cell, and Spleen ruled out (Spleen: no French accents in its 5x8 size, no density gain in its 6x12 size). Findings posted in the issue; what they change is applied to `docs/lot-plan.md`, lot 1c-1, and to the issue "Terminal screen on the computers".
