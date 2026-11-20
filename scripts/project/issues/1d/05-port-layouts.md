Title: Port layout of each device
Labels: type: feature, area: blocks, priority: high
Milestone: lot 1d-1

One plain Java description of the ports of each device, the single source of truth for the server (which port was clicked) and the client (outline, panel, attachment, plug). Design in `docs/cable-design.md`, section 3. Needs the tower and the laptop redrawn on the design guide with their ports first (issue #56). Related ideas: port types (#37), port lights (#38).

- [ ] `PortLayout`: for each port, the face that carries it, its aim zone, its hole, its mouth (point and outward direction) and its size (large 3 x 3 or small 2 x 2 texels), for a front facing north, turned by `FACING`
- [ ] Aim zones up to halfway to the neighbouring ports; on a flat device, the zone goes on over the top face
- [ ] Layouts of the tower, the laptop, the internet box and the desktop switches, taken from their device sheets
- [ ] Unit tests: as many ports as the block's `PORTS`, zones inside the shape and disjoint, each hole inside its zone, rotation in the four orientations
- [ ] Each device sheet gets a "Port layout" table; `design/tools` prints the layout of a device for the Java code
