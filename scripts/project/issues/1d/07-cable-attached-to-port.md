Title: Cable attached to its port, with its plug
Labels: type: feature, area: interface, priority: high
Milestone: lot 1d-1

The cable leaves from the port itself, with an RJ45 plug pushed into it, and gets a thickness. The hanging curve between the two plugs stays until lot 1d-2. Design in `docs/cable-design.md`, section 4 (decided on 2026-11-20). Related ideas: cables and cable guides (#25), port types (#37).

- [ ] First, check in the 26.3 sources how to draw a small model or textured quads in `SubmitCustomGeometryEvent`
- [ ] Plug models, large and small, drawn in Blockbench following the design guide and approved
- [ ] The end of a cable is the mouth of its port (port layout, `FACING` read on the client); `ATTACH_HEIGHT` removed; the held cable goes from the plug to the hand
- [ ] Plugs drawn by the client from the cables it knows, not from block states
- [ ] Cable 0.25 px thick, square section, lit by the light of the world, instead of the line
- [ ] Capture scenario: plugs in the internet box and a switch, cables in the four orientations
