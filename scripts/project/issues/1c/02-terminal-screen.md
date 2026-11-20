Title: Terminal screen on the computers
Labels: type: feature, area: interface, priority: high
Milestone: lot 1c-1

The same console on the PC tower and on the laptop: a view of a machine that runs on the server. Design in `docs/lot-plan.md`, lot 1c-1.

- [ ] Tower: a right-click with an empty hand opens the terminal, without peripherals (issue #10)
- [ ] Laptop: a right-click on the screen part opens or closes the lid; a right-click on the keyboard part of the open laptop opens the terminal; the item keeps its open or closed state in the inventory and is placed in that state (sneak + right-click with an empty hand stays the unplugging of a cable end, lot 1a)
- [ ] One local console per machine, kept in server memory while the machine exists: Esc hides the screen, the session and its running commands go on; `exit` or Ctrl+D on an empty line ends the shell session only
- [x] One player at the keyboard at a time ("computer busy", through the message module); keyboard freed when the player walks away, disconnects or changes dimension
- [x] Client to server packets checked by the server: line size, rate, session and distance
- [x] Commands are processes advanced by the server tick, registered in a registry; an error ends its process, never the tick; a cap on running processes per machine
- [ ] Dark game popup panel (`popup/background`) with a white title bar (name of the machine, how to close, "Ctrl+C: stop" while a command runs); game colours as the ANSI palette, grey (AAAAAA) on black with the game's text shadow; blinking block cursor; rows of 12 px; never the dark blue 0000AA for an important text
- [ ] Prompt `user@<host>:~$` from the data of the machine, `user@host` in green 55FF55 and `~` in blue 5555FF; host name from the type and the last four hex digits of the MAC of the first port (`laptop-…` for a laptop, `tower-…` for a tower)
- [ ] The terminal's own text size, independent of the GUI scale: by default the largest that gives at least 80 columns; changed in a client option and with Ctrl+= / Ctrl+- in the terminal
- [ ] Columns and rows from the window size and the text size, recomputed on every resize; lines wrapped at the column by the client; scrollback with the wheel and Shift+Page Up / Page Down; command history with the Up and Down arrows
- [ ] The player's keyboard layout for typed characters and shortcuts (Ctrl+C, Ctrl+L, Ctrl+D, read from the symbol of the layout); text only from the typed characters (`charTyped`), dead keys through the preedit; the real Ctrl key, also on macOS; Alt+letter never a shortcut; any typed character accepted
- [ ] The screen does not pause the game in single player (`isPauseScreen` false) and survives a window resize (no scissor)
- [ ] Font Monocraft v4.2.1 (SIL OFL 1.1), its licence text next to the TTF in the jar
- [x] Shell base: `hostname`, `help` (translated), `clear`, `history`, `exit`; unknown commands and options get the error of the real tool
- [ ] Output of the commands in literal English built by the server; lines of the game (help, advice after a failure, title bar) translated in both language files and turned off by a client setting for the advice (`docs/messages.md`, section 3)
