# Design guide for equipment models

Common rules for the models and textures of every piece of equipment, so that they look like one family. Real dimensions come from `docs/network-equipment.md`; the design of each lot is in `docs/lot-plan.md`.

Written on 2026-11-19. Status: approved by Max on 2026-11-19.

## 1. Scope

This guide covers the equipment of a desk or a home: computers, boxes, desktop switches, access points, peripherals, small devices. The infrastructure (racks and their equipment, street cabinets, poles, chambers, technical rooms) is out of scope: its scale is the open decision "Scale of rack equipment" of `docs/lot-plan.md`, to settle before lot 6a. The rule of section 2 is the "small equipment enlarged" family of that proposal.

## 2. Scale

One block is 16 px and stands for 1 m, so 1 px stands for 62.5 mm at real size.

**Rule:**

1. Every device is enlarged by the same coefficient, **k = 2.4**: in-game size in px = real size in mm x 2.4 / 62.5, so **1 px stands for about 26 mm**. The same factor applies to the three dimensions: proportions stay true.
2. **Cap:** if the largest dimension goes over 15 px, the whole device is reduced uniformly until that dimension is 15 px, so that it stays inside one block with a margin.
3. **Rounding:** outer dimensions to the nearest 0.5 px; details on the geometry grid (section 3, 0.25 px).
4. **Minimum thickness:** 1 px for any body part (a 20 mm laptop lid would be 0.8 px).
5. **Fit:** a dimension may be raised to the next 0.5 px when the details of a face (ports) do not fit otherwise. Each such exception is written in the table below.

Why 2.4: the box and the laptop are already at this factor; only the depth and the height of the tower differ.

| Equipment | Real size (W x D x H, mm) | Status | Result (px) | Current model (px) | Change |
|---|---|---|---|---|---|
| PC tower | 210 x 450 x 450 | M | 8.1 x 17.3 x 17.3, over the cap, reduced (1 px = 30 mm): **7 x 15 x 15** | 7 x 15 x 15 | Redrawn on 2026-11-20 (was 8 x 15 x 15); Java shape narrowed |
| Internet box, standing (the 183 x 183 x 80 case turned upright, 2026-11-20) | 183 x 80 x 183 | E | 7.0 x 3.1 x 7.0: **7 x 3 x 7** | 7 x 3 x 7 | Standing since 2026-11-20 (was 7 x 7 x 3 flat); Java shape turned with the orientation |
| Laptop, closed | 360 x 240 x 20 | M | 13.8 x 9.2 x 0.8: **14 x 9 x 2** (base 1 + lid 1, minimum thickness) | 14 x 9 x 2 | Redrawn on 2026-11-20 (was 14 x 10 x 2); Java shapes narrowed |
| 8-port desktop switch | 158 x 102 x 27 | E | 6.1 x 3.9 x 1.0: **6.5 x 4 x 1** (width raised by the fit rule for 8 ports; the spare texel of the front goes between two groups of 4 ports) | none | New, drawn on 2026-11-20 (`desktop_switch_8.md`), block at lot 1d-1 |
| 5-port desktop switch | 100 x 75 x 25, provisional value, not verified | M | 3.8 x 2.9 x 1.0: **4 x 3 x 1** | none | New |

The tower and the box values rest on unverified dimensions (status M and E): they are checked when each device is modelled, as `docs/network-equipment.md` says.

What a strict x 2.4 would give for the tower: 8 x 17.3 x 17.3 px, more than one block in depth and height. The current tower is at x 2.4 in width only (8 px for 210 mm) and at about x 2.1 in depth and height (15 px for 450 mm).

## 3. Texture density and geometry grid

**Rule: 4 texels per model pixel (64 texels per block), the same for every face of every device.**

- No face is stretched or squeezed: a face of w x h px takes exactly 4w x 4h texels on the sheet.
- Geometry grid: 0.25 px (one texel). Outer dimensions on 0.5 px (section 2).
- Texture sheet: square, power of two, the smallest that holds all the faces (64 x 64 for a single-element device such as a switch, 128 x 128 for the box, the tower and the laptop). Faces with the same look may share the same area of the sheet.
- Faces are mapped one by one (per-face UV), not with the automatic box layout.

Why 4 and not 2: the narrowest port faces decide. A port takes at least 2 texels, with 1 texel between two ports and 1 texel of margin on each side:

| Face | Width | Ports | Texels needed | At 2 texels per px | At 4 texels per px |
|---|---|---|---|---|---|
| Box, front | 7 px | 4 | 13 | 14: fits, no room left | 28: fits |
| 8-port switch, front | 6.5 px | 8 | 25 | 13: does not fit | 26: fits |
| 5-port switch, front | 4 px | 5 | 16 | 8: does not fit | 16: fits |
| Switch, front height | 1 px | | port 2 + 1 above + 1 below = 4 | 2: the port fills the face | 4: fits |

At 2 texels per px, the switches would have to be about twice as wide, which breaks the scale. The price of 4: the devices are drawn four times finer than vanilla blocks (16 texels per block).

The current textures do not follow one density: tower 2 texels per px, box 4, laptop 2 with stretched edges. They are aligned when each device is reworked.

## 4. Palette

Eight base colours, with a highlight and a shadow for the materials. Values taken from the textures of the time, except the light plastic highlight.

**Single source: `design/palette.json`**, read by the design tools (`design/tools/wwb_design.py`) and checked by `BlockResourcesTest` (`./gradlew build test`): every colour of every texture of the mod must be one of the tones of its roles; the textures listed in its `cord_textures` (the plugs, `textures/block/rj45_plug.png`, and the cord item, `textures/item/ethernet_cable.png`) may also use the cord colours, and no other texture may (`cordColoursAreRefusedOutsideTheCordTextures`); the textures listed in its `exceptions`, each with its reason, are left out. A new colour means changing `design/palette.json` and this table together.

| Role | Base | Highlight | Shadow | Used for |
|---|---|---|---|---|
| Light plastic | `#c4c8ce` | `#dde0e4` (new) | `#969aa2` | Home devices (box, switches) |
| Dark plastic | `#3a3d42` | `#4b4f56` | `#2a2c30` | Computers, peripherals |
| Metal | `#7d838b` | `#9aa0a8` | `#555a61` | Power supply, metal cases, back plates |
| Socket | `#1c1e22` | | `#0b0b0d` | Port holes, vents, slots, gaps |
| Contacts | `#b09646` | `#c8aa3c` | | Visible pins and metal contacts |
| Green LED | `#4ade80` | | | Power, link |
| Orange LED | `#f0aa3c` | | | Activity, warning |
| Screen, off | `#0d1b2a` | | | Screens and glass |

**Cord colours** (chosen by Max on 2026-11-20; key `cords` of `design/palette.json`): a family of their own, for the cable, the boot of the plugs and the cord item, never in the textures of the devices. Blue, the colour of every cord today (`CableRenderer`, `#2f6fd6`), is the default; the blues of the current cord icon go with its rework. Grey is a dark blue-grey, apart from the metal; white is a warm off-white, apart from the clear body of the plugs and from light plastic. A colour carries no rule of the game (`docs/cable-design.md`, section 7).

| Cord | Base | Highlight | Shadow |
|---|---|---|---|
| Blue | `#2f6fd6` | `#5a8de0` | `#1f4f9f` |
| Grey | `#5e6a7a` | `#7c8798` | `#424b58` |
| Yellow | `#e3bd2d` | `#f0d35e` | `#a8871a` |
| Green | `#3c9e4a` | `#62bb6d` | `#276f32` |
| Red | `#cf3b3b` | `#e26666` | `#942626` |
| Orange | `#e57a2e` | `#ef9a5a` | `#a9541b` |
| Black | `#26282c` | `#3d4046` | `#141517` |
| White | `#e9dfc4` | `#f4edda` | `#bdb196` |

**Where the cord colours are allowed:** the RJ45 plug texture (`rj45_plug.png`, one boot per cord colour) and the cord item icon only. At lot 1d-1, `BlockResourcesTest` accepts the roles and the cord colours for these two textures, the roles alone for every other texture (Max, 2026-11-20).

Out of the palette until its rework: the Ethernet cable item keeps its own blues (exception in `design/palette.json`, removed when the icon with the taller plug is exported). Colours of the current textures that go away at the rework: the blue LED (`#3a8fe8`) and the pink pixel (`#d05a8a`) of the tower, the screen text of the laptop (`#cbd5e1`, the screen content belongs to the terminal and to lot F).

## 5. Drawing

- Flat colours. No gradient, no noise, no dithering. The game already shades each face by its direction: no light is drawn into the texture.
- Edges: a 1-texel highlight line along the top edge of the side faces, a 1-texel shadow line along their bottom edge.
- **RJ45 port: one size on every device, 2 x 2 texels (0.5 px)**, as a real RJ45 has the same size everywhere; at the scale of section 2 this is the real size, and the grid of texels allows nothing smaller (Max, 2026-11-20). Pitch of at least 3 texels. **The contacts are always on the bottom row** (Max, 2026-11-20), painted or recessed: painted, the top row in socket colour and the bottom row in contacts colour; recessed (a hole 0.5 px deep), the bottom of the hole in socket shadow tone with its bottom row in contacts colour (seen from the front), and the back row of the floor of the hole in contacts colour as well (seen from above or from the side: the side walls of a 2 x 2 hole hide its bottom in a three-quarter view).
- Other connectors (RJ11, fibre, power, console): told apart by the shape of their plug and the thickness of their cable, never by a smaller hole (`docs/cable-design.md`, section 4).
- Ports are texture areas aligned on the 0.25 px grid (whole texels), at positions written in the device sheet (section 9). This keeps the models compatible with choosing a port by aiming at it, if that option is kept (open decision "Choice of the exact port" of `docs/lot-plan.md`, before lot 1d-1); this guide does not decide it.
- LED: 1 x 1 texel. Button: at least 2 x 2 texels.
- Vents: discreet, in the shadow tone of the body (dots or lines of 1 texel, 1 texel apart). The socket colour stays for real openings (ports, slots, holes).
- Feet: optional, at most 0.5 px high, counted in the height.

Reusable elements (first used on the internet box, `design/blockbench/internet_box.md`; helpers in `design/tools/`):

- **Plinth:** a base set back 0.5 px on each side, 0.5 px high, in dark plastic (shadow tone), so that the device seems to stand off the desk. Counted in the height.
- **Cover bevel:** the top cover set back 0.25 px on each side, 0.25 px high; its sides and the rim of the body around it in the highlight tone.
- **Recessed ports:** the front is a panel 0.5 px thick, split around the ports (a strip above, a strip below, posts between the holes). The bottom of each port is the front of the body, seen through the hole, with the contacts row at the bottom so that it stays visible from above. Depth reads from light to dark: floor (dark plastic shadow tone), ceiling and sides (socket), bottom (socket shadow tone). The texture of the front is drawn once and shared out between the elements of the panel (`Sheet.add_slice`).
- **Rule: relief on the front, painted ports at the back.** The back faces the wall: its ports stay drawn on a flat face.

**Signature of a family.** Each family of devices (computer, box, switch, access point, and so on) has a visual signature of its own: its silhouette and the organisation of its front. Two families must be told apart at a glance, in the world and in the inventory, not only by their size. The signature is written at the top of the device sheet, and a new family is compared with the existing ones side by side (`design/tools/preview.py --scene`) before it is approved.
- LEDs are drawn lit, in their normal operating colour, and do not change until lot F (lights driven by the state, see the idea issue "Port lights in the finishing lot").
- Each LED has a fixed position, to the texel, written in the device sheet (face, column, row), so that the off and animated variants of lot F are simple to produce.
- A port LED sits next to its port: above it on the same face when there is room. When the port face is full (a switch front of 1 px), the LEDs go on the top face, along the edge above that face, each one aligned on its port.

## 6. Generic equipment

- No brand, no logo, no commercial model name, no shape or colour scheme that identifies a product or an operator.
- No text, letter or number on the textures (unreadable at this scale anyway).

## 7. Display in the inventory and in hand

Small devices are enlarged as items so that they stay readable. Values from the vanilla parent `minecraft:block/block` (read in the 26.3 sources): gui rotation 30, 225, 0 and scale 0.625; ground scale 0.25, translation y 3; fixed scale 0.5; third person rotation 75, 45, 0, translation y 2.5, scale 0.375; first person scale 0.40, rotation 0, 45, 0 (right) and 0, 225, 0 (left). Each context is looked up on its own, from the model up its parents.

**Rule** (proposed value, to check in game):

1. **Inventory (gui):** the device takes about three quarters of the slot. The gui scale `s` is the largest value for which both the drawn width `s x (w + d) / 1.414` and the drawn height `s x (h x 0.866 + (w + d) x 0.354)` stay within 12 px (w, d, h: model size in px; the slot is 16 px).
2. **Other contexts:** the same magnification `m = s / 0.625` multiplies the vanilla scales of ground, fixed, third person and first person. Rotations stay the vanilla ones. `head` and `on_shelf` are left to the parent.
3. **Centring:** a flat device sits low in its block. The translation brings its centre back to the centre of the block: `(8 - h / 2) x scale` px, along the vertical after the context's rotation (for a rotation of a degrees around X: `cos a` on Y and `sin a` on Z), added to the vanilla translation.
4. The model defines all six contexts (gui, ground, fixed, thirdperson_righthand, firstperson_righthand, firstperson_lefthand), so that no context mixes its values with the parent's. The values of each device are in its sheet.

Limits read in the sources: scale at most 4, translation at most 80 px.

## 8. Model conventions

- The front of the device faces north (-Z) in the model. The blockstate turns it with `y` 90, 180 and 270 for east, south and west, as for the three existing blocks.
- The model is centred on X and Z (from 8 - w/2 to 8 + w/2) and rests on y = 0.
- Faces that can never be seen (against another element) are left out of the model.
- Element names in English snake_case: `case`, `front`, `port_lan_1`, `led_power`.
- The selection and collision shape is written in Java (`Block.box` in the block class, for example `InternetBoxBlock.SHAPE`). Changing the outer dimensions of a model means changing that shape too, then a build by Max.

## 9. Files and workflow

- Blockbench projects: `design/blockbench/<id>.bbmodel`, format "Java Block/Item", texture embedded in the project.
- Generator and export scripts: `design/tools/` (see its README).
- Device sheet: `design/blockbench/<id>.md`, next to the project. It records the real and in-game dimensions, the elements, the position of each port and LED (face, texel column and row, model coordinates) and the display values. The `design/` folder is outside `src/main/resources`: it is never in the jar (the jar only contains `sourceSets.main`).
- For each device:
  1. The `.bbmodel` is written, with its texture.
  2. The choices are described in a few lines (dimensions, elements, texture details), and written in the device sheet.
  3. Max opens it in Blockbench and approves it or gives feedback.
  4. Only after approval: export of `models/block/<id>.json` and `textures/block/<id>.png` into `assets/worldwideblocks/`, check of the blockstate (four orientations, and the states of the device), of the item model and of the Java shape. The result is checked in game by a capture run when they exist (lot 1c-1, issue "Permanent capture runs"), by Max's build until then.
