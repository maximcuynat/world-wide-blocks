Title: Show the address and the lease state in the status panel
Labels: type: feature, area: interface, priority: medium
Milestone: lot 1b-2

Playable at the end of the lot: each PC plugged into the box shows its address in the status panel; unplugged, it loses it.

- [ ] PC, compact panel: its address, or "no address"
- [ ] PC, detailed panel (sneaking): address, mask, gateway, DNS server, lease state and remaining time
- [ ] Box, detailed panel: leases in progress
- [ ] Panel lines built in plain Java (`StatusPanel`), new language keys in both files with their sample values in `LangFilesTest`
- [ ] Checked in game with `./gradlew runClient` (game tests draw nothing)
