Title: Cable attached to its port, with its plug
Labels: type: feature, area: interface, priority: high
Milestone: lot 1d-1

The cable leaves from the port itself, with an RJ45 plug pushed into it, and gets a thickness. The hanging curve between the two plugs stays until lot 1d-2. Design in `docs/cable-design.md`, section 4 (decided on 2026-11-20). Related ideas: cables and cable guides (#25), port types (#37). The choice of the plug model from the colour of the link moved on 2026-11-21 to the idea "Cord colours: a property of the cord and of the link" (`ideas/20-cord-colours.md`): every link is blue until the colour is a property of the link.

- [x] First, check in the 26.3 sources how to draw a small model or textured quads in `SubmitCustomGeometryEvent`
- [x] Plug model (one RJ45 size, clear body and long boot), drawn following the design guide and approved by the maintainer on 2026-11-20: `design/blockbench/rj45_plug_<colour>.bbmodel` and its sheet `rj45_plug.md`, by `design/tools/rj45_plug.py`
- [x] Plug exported: one texture `rj45_plug.png` (32 x 32) for the eight cord colours, one model per colour where only the UV of the boot changes
- [x] Plug models `worldwideblocks:block/rj45_plug_<colour>` loaded as standalone models (`docs/cable-design.md`, section 4), the eight checked by the model check of every capture run
- [x] Cord item icon exported, the variant with the taller plug chosen on 2026-11-20 (`ICON` of `design/tools/cables.py`, in the cord colours; design guide, section 4); the palette exception of `textures/item/ethernet_cable.png` removed
- [x] `BlockResourcesTest` accepts the roles and the cord colours of `design/palette.json` for the plug texture and the cord item icon only, the roles alone elsewhere (design guide, section 4)
- [x] The end of a cable is the mouth of its port (port layout, `FACING` read on the client); the held cable goes from the plug to the hand
- [x] `ATTACH_HEIGHT` removed (an end whose device the client has not received keeps the middle of its block at 0.3 block, without a plug)
- [x] Plugs drawn by the client from the cables it knows, not from block states
- [x] Cable 0.25 px thick, square section, lit by the light of the world, instead of the line; the plug may stand out of the block (tower, laptop), accepted
- [x] Capture scenario: plugs in the internet box and a switch, cables in the four orientations
