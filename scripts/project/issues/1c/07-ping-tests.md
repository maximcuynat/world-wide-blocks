Title: Game tests for ping between two PCs on the box
Labels: type: test, area: engine, priority: medium
Milestone: lot 1c-1

- [x] Game test: two PCs on the box, `ping` from one to the other gets replies
- [x] Game test: `ping` to an absent address of the local network gets `Destination Host Unreachable` (ARP finds nobody)
- [ ] Capture scenario `terminal` (issue #52) for the check by eye of lot 1c-1: `ip a`, `ping` to the box and to an absent address, at 854 x 480 and 1920 x 1080, in English and French; screenshots and values read and reported by the assistant
