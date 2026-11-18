Title: Game tests and in-game check procedure for DHCP
Labels: type: test, area: engine, priority: medium
Milestone: lot 1b-2

- [ ] Game tests: a PC plugged into the box gets an address, unplugged it loses it, renewal, restart (INIT-REBOOT)
- [ ] Test scene with PCs on a box (test scene command of the mod), to check the panel by eye
- [ ] Status and check procedure of lot 1b-2 in `docs/plan-lots.md`
- [ ] `./gradlew build test` and `./gradlew runGameTestServer` green
