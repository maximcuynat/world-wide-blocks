Title: A long cable is drawn under the ground
Labels: type: bug, area: interface, priority: medium

Found by the capture run `distance` (lot 1c-1, issue #52): seen from above either end, the cable of 99 blocks is only drawn on about 5 blocks at each end, then goes under the ground.

Cause, read in `CableRenderer`: a cable is attached 0.3 block above the floor of its devices (`ATTACH_HEIGHT`) and sags in its middle by 4 % of its length, capped at 1.5 blocks (`sag`). From 7.5 blocks long, the sag passes 0.3 block: on flat ground, the middle of the cable goes through the floor. For 99 blocks, the parabola is under the floor between 5 % and 95 % of its length.

- [ ] The lowest point of a cable never goes under the floor of its lower end (a long cable lies on the ground, as a real one does)
- [ ] Capture scenario `distance`: the cable visible along its whole length from above each end
- [ ] Unit test of the shape of the cable (sag capped by the height of the attach point), in plain Java
