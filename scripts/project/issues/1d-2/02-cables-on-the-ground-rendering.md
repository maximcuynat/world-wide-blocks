Title: Draw the cables on the ground
Labels: type: feature, area: interface, priority: medium
Milestone: lot 1d-2

Thick cables laid along their route, recomputed only when needed, that the player walks over. Design in `docs/cable-design.md`, section 5.

- [ ] Thick mesh along the route, built once per route and kept
- [ ] Route cache on the client, recomputed when a block changes in the corridor or when a chunk it crosses arrives, with a budget per frame
- [ ] Several cables in the same place: shifted sideways by an amount taken from the cable identifier, raised a quarter of a pixel where they cross
- [ ] No collision: the player walks over the cables
- [ ] Capture scenarios: a room with several computers and the cables side by side, a cable through a door, up steps, against a wall without an opening (hanging), crossing cables
