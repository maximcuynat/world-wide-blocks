Title: Route of a cable on the ground
Labels: type: feature, area: interface, priority: medium
Milestone: lot 1d-2

The cable comes down from its plug, follows the floor, goes around walls and goes up into the other device. Drawn only: nothing in the world, nothing saved. Design in `docs/cable-design.md`, section 5 (decided on 2026-11-20). Related idea: cables and cable guides (#25), which keeps the steps of lots 2b and 8a.

- [ ] Route as a chain of spans (on the ground, hanging) between the two plugs, in plain Java in common code (not `client/`, so that the preview and the server checks of lot 2b can reuse it), computed from a terrain interface
- [ ] Span on the ground: down from the plug to the surface below, path search in a corridor of 8 blocks on each side of the straight line, climbs of one block at most, drops allowed, doors always passable; then straightened and laid on the real height of the surfaces, sampled every quarter of a block
- [ ] Hanging span (today's curve) when no route is found on the ground or the terrain is not known
- [ ] Unit tests on fake terrains: flat floor, steps, wall with a door, wall without an opening, drop, unknown terrain, same route for the same terrain
