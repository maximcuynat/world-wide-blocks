Title: Terminal screen on the computers
Labels: type: feature, area: interface, priority: high
Milestone: lot 1c-1

The same console on the PC tower and on the laptop: a view of a machine that runs on the server. Design in `docs/lot-plan.md`, lot 1c-1.

- [ ] Tower: a right-click with an empty hand opens the terminal, without peripherals (issue #10)
- [ ] Laptop: a right-click on the screen part opens or closes the lid; a right-click on the keyboard part of the open laptop opens the terminal; the item keeps its open or closed state in the inventory and is placed in that state (sneak + right-click with an empty hand stays the unplugging of a cable end, lot 1a)
- [ ] One local console per machine, kept in server memory while the machine exists: Esc hides the screen, the session and its running commands go on; `exit` or Ctrl+D on an empty line ends the shell session only
- [ ] One player at the keyboard at a time ("computer busy", through the message module); keyboard freed when the player walks away, disconnects or changes dimension
- [ ] Client to server packets checked by the server: line size, rate, session and distance
- [ ] Commands are processes advanced by the server tick, registered in a registry; an error ends its process, never the tick; a cap on running processes per machine
- [ ] Minecraft-style frame with a title bar (name of the machine, how to close, "Ctrl+C: stop" while a command runs); game colours as the ANSI palette, light grey on black; blinking block cursor
- [ ] Prompt `user@<host>:~$` from the data of the machine; host name from the type and the last four hex digits of the MAC of the first port (`laptop-…` for a laptop, `tower-…` for a tower)
- [ ] Columns and rows from the window size; lines wrapped by the client; scrollback with the wheel and Shift+Page Up / Page Down; command history with the Up and Down arrows
- [ ] The player's keyboard layout for typed characters and shortcuts (Ctrl+C, Ctrl+L, Ctrl+D); any typed character accepted
- [ ] Monospaced font chosen after the prototype, with its licence and attribution in the jar
- [ ] Shell base: `hostname`, `help` (translated), `clear`, `history`, `exit`; unknown commands and options get the error of the real tool
- [ ] Output of the commands in literal English built by the server; lines of the game (help, advice after a failure, title bar) translated in both language files and turned off by a client setting for the advice (`docs/messages.md`, section 3)
