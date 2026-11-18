Title: Multiplayer check on a dedicated server with one client
Labels: type: test, area: engine, priority: medium
Milestone: version 0.1.0

At the end of lot 1. Until then, keep the client and server separation strict in the code.

1. `./gradlew runServer` (the first time, accept the EULA in `run/eula.txt`, then run it again).
2. In another terminal, `./gradlew runClient`, then Multiplayer, connect to `localhost`.
3. Follow the check procedure of lot 1 in `docs/plan-lots.md`: plugging, visible cable, status panel, unplugging, addresses and `ping`.
4. Stop and restart the server: the links are still there.
5. Both logs without error, in particular no client class loaded by the server.
