Title: Cord colours: a property of the cord and of the link
Labels: type: idea

Proposed by the maintainer on 2026-11-20, design in `docs/cable-design.md`, section 7 ("Colour"), to decide before the lot that brings it. A colour carries no rule of the game: it only helps the player find a cable.

- The colour is a property of the cord item (a data component), not a separate item; blue by default. Changed in the crafting table with one of the eight dyes of the cord colours (`design/palette.json`, key `cords`), as dyed leather armour (to check in the 26.3 sources); one entry per colour in the creative tab.
- The colour becomes a property of the link: kept by the engine and the save (old worlds load with blue), sent to the clients with the link (`LinkAddedPayload`); a cable unplugged gives back a cord of its colour. Proposal: a right-click with a dye on an aimed port recolours its cable.
- The tooltip and the status panel name the colour (language keys in both files), for colour-blind players.

Base laid in lot 1d-1 (#63): one plug model per cord colour (`worldwideblocks:block/rj45_plug_<colour>`, loaded as standalone models and checked by the model check of every capture run), the cable drawn in the base tone of its colour (`port/CordColour`); every link is drawn blue (`CordColour.DEFAULT`) until the colour is a property of the link.

- [ ] The client picks the plug model from the colour of the link (moved from #63 on 2026-11-21): `CableRenderer` draws `CordColour.DEFAULT` for every cable until then
