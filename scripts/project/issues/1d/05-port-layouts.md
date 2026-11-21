Title: Port layout of each device
Labels: type: feature, area: blocks, priority: high
Milestone: lot 1d-1

One plain Java description of the ports of each device, the single source of truth for the server (which port was clicked) and the client (outline, panel, attachment, plug). Design in `docs/cable-design.md`, section 3. Needs the tower and the laptop redrawn on the design guide with their ports first (issue #56). Related ideas: port types (#37), port lights (#38).

- [x] `PortLayout`: for each port, the face that carries it, its aim zone, its hole, its mouth (point and outward direction) and its connector (RJ45, a single size of 2 x 2 texels on every device), for a front facing north, turned by `FACING`
- [x] Aim zones up to halfway to the neighbouring ports; on a flat device, the zone goes on over the top face
- [ ] The aimed port outlined by a two-tone frame (light inside, dark outside)
- [x] Layouts of the tower, the laptop, the internet box and the desktop switches, taken from their device sheets
- [x] Unit tests: zones on a face of the body of the device and disjoint, each hole inside its zone, rotation in the four orientations
- [ ] As many ports as the block's `PORTS` and zones on the real shape of the block, in every state: game tests, for every device including the two switches
- [x] Each device sheet gets a "Port layout" table; `design/tools` prints the layout of a device for the Java code
