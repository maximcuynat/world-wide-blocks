Title: Game tests for ping between two PCs on the box
Labels: type: test, area: engine, priority: medium
Milestone: lot 1c-1

- [ ] Game test: two PCs on the box, `ping` from one to the other gets replies
- [ ] Game test: `ping` to an absent address of the local network gets `Destination Host Unreachable` (ARP finds nobody)
- [ ] Check procedure of lot 1c-1 in `docs/lot-plan.md` (terminal display, checked with `./gradlew runClient`)
