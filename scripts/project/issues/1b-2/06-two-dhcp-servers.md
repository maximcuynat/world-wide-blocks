Title: Report two DHCP servers on the same network
Labels: type: feature, area: messages, priority: low
Milestone: lot 1b-2

Two boxes on the same network both answer DHCP requests: the status indicator reports the conflict.

- [ ] Detection: a PC receives offers from two different server ids
- [ ] Event through the message module (`MessageCenter.event`), once per minute per network
- [ ] Game test with two boxes linked together
