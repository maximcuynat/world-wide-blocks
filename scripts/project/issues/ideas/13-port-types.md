Title: Port types: RJ45, RJ11, fibre LC/SC, SFP, power C13/C19
Labels: type: idea

Each port has a real connector, and each cable fits only its own: RJ45 (Ethernet), RJ11 (telephone line), fibre LC and SC, SFP cage (which takes a copper or fibre module), power C13 and C19 for rack equipment. Today the engine knows two port kinds (`PortKind`: Ethernet and line input). To bring in with the lots that need each connector: RJ11 at 3b, fibre at 3a, SFP at 6a or 8a, C13/C19 at 1e or 8e.

Base laid in lot 1d-1: the port layout of each device (#61) records the size of each port, and the RJ45 plug is drawn in the port (#63). Each new connector adds its hole size to the layout and its plug model (`docs/cable-design.md`, section 4).
