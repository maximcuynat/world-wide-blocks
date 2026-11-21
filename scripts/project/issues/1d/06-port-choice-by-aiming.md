Title: Choose the port by aiming at it
Labels: type: feature, area: interface, priority: high
Milestone: lot 1d-1

With a cord in hand, the player aims at the port they want: it is outlined and the status panel names it before the click. Anywhere else on the device, the first free port, as today. Design in `docs/cable-design.md`, section 3 (decided on 2026-11-20).

- [x] Server: the click point and face (`UseOnContext`, `BlockHitResult`) resolved to the aimed port with the port layout; no new packet
- [x] The four gestures follow the aimed port: start, finish, unplug (sneak, cord in hand), take an end off (sneak, empty hand); elsewhere on the device, first free port (first occupied port to unplug or take an end off)
- [x] Refusal "port N already in use", new key in both language files with its sample values in `LangFilesTest`
- [x] Client: outline of the aimed port, with a cord in hand or sneaking with an empty hand
- [x] Status panel: a line for the aimed port, compact ("Port 3: free") and detailed (port at the other end, name of the device when its chunk is loaded), built in plain Java in `StatusPanel` with unit tests
- [x] Game tests of the server side: aimed port, fallback, port in use, unplugging the aimed port
- [x] Capture scenario: outline and panel line on the internet box and on a switch, from the front and from above
