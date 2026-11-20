Title: Port types: RJ45, RJ11, fibre LC/SC, SFP, power C13/C19
Labels: type: idea

Each port has a real connector, and each cable fits only its own: RJ45 (Ethernet), RJ11 (telephone line), fibre LC and SC, SFP cage (which takes a copper or fibre module), power C13 and C19 for rack equipment. Today the engine knows two port kinds (`PortKind`: Ethernet and line input). To bring in with the lots that need each connector: RJ11 at 3b, fibre at 3a, SFP at 6a or 8a, C13/C19 at 1e or 8e.

Base laid in lot 1d-1: the port layout of each device (#61) records the connector of each port, and the RJ45 plug is drawn in the port (#63). Each new connector adds its hole to the layout and its plug model (`docs/cable-design.md`, section 4).

Proposed by the maintainer on 2026-11-20 (`docs/cable-design.md`, sections 4 and 7):

- The RJ45 has a single size on every device, 2 x 2 texels, the smallest the grid allows: other connectors are told apart by the shape of their plug and the thickness of their cable, never by a smaller hole.
- Each type of cable (copper RJ45, telephone RJ11, fibre, power cord of lot 1e, console cable of lot 8b) is a distinct item, recognised by its plug and its thickness, never by its colour; the colour is a property of the item, changed with a dye.
- Fibres take by default the standard colours of real cords: yellow for single-mode, orange or aqua for multimode (OM1 orange, OM3 and OM4 aqua, OM5 lime; OM2 orange or grey depending on the source), to check against TIA-598-C.
