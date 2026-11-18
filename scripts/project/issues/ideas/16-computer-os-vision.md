Title: An operating system for the computers ("Blocknix")
Labels: type: idea

Vision of an operating system for the computers of the mod, in three phases, each one split into small playable lots when it comes. Working name "Blocknix", to check as a trademark before any announcement.

1. **Terminal**: the shared console of lot 1c, with network commands that follow the Linux convention and arrive with the lots that give them a meaning (`ip`, `ping`, then capture, `traceroute`, name resolution, connections once TCP exists). The same console serves the equipment consoles (7a, 8b, 8d) and SSH.
2. **Desktop and files**: local login, a minimal desktop where the terminal becomes one application among others, a file system saved with the world, a text editor, then applications such as network settings, files, a frame analyzer and a browser for the web servers of the mod. Text mode stays available everywhere, as on a rack server.
3. **Scripts**: the shell becomes a small language to automate tasks (watch the gateway, log failures, scheduled tasks). Hard constraints: a player's program never freezes or crashes the server, and never leaves the game (no host files, no real network, no Java classes).

Related: the services API idea (shared by the network engine and the OS), and lot 8g (system and accounts).
