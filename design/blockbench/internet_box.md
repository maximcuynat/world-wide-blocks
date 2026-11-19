# Device sheet: internet box (`internet_box`)

Follows `docs/design-guide.md`. Status: approved by Max in Blockbench on 2026-11-19, exported to the assets with `design/tools/export.py`; checked in game by Max on 2026-11-20.

## Size

- Real: 183 x 183 x 80 mm (status E, `docs/network-equipment.md`).
- In game, x 2.4: 7.0 x 7.0 x 3.1 px, rounded to **7 x 7 x 3 px**. Unchanged from the previous model, so the Java shape (`InternetBoxBlock.SHAPE`, 4.5 to 11.5, 0 to 3) stays as it is.
- One element, `case`, from [4.5, 0, 4.5] to [11.5, 3, 11.5].

## Texture

64 x 64 sheet, 4 texels per px, colours of the palette only (light plastic, socket, contacts, metal, green LED, dark plastic for the feet).

| Face | Sheet area (x, y, w, h) | Content |
|---|---|---|
| up | 0, 0, 28, 28 | Light plastic, 5 vent slots (16 texels long) over the back half |
| down | 32, 0, 28, 28 | Light plastic, 4 rubber feet drawn (not modelled) in dark plastic |
| north (front) | 0, 32, 28, 12 | Power and line LEDs on the left, 4 LAN ports on the right, one LED above each port |
| south (back) | 32, 32, 28, 12 | Line input on the left, power socket on the right |
| east, west | 0, 48 and 32, 48, 28 x 12 | Light plastic, highlight and shadow edges |

## Placement of the ports

- **4 LAN ports on the front, line input and power socket at the back.** The front turns toward the player who places the box (`InternetBoxBlock`, FACING opposite to the player's look), so the ports where the room's computers plug in are in sight; the line goes to the back, toward the wall where it will arrive at lot 3 (optical or telephone socket). This is the split already written in `InternetBoxBlock`. **A choice of playability, made on purpose against the realism of a real box** (decided by Max on 2026-11-19).
- A real box has all its ports at the back. Alternative set aside: everything at the back as in reality, with only the LEDs on the front.
- The power socket is drawn now, although nothing uses it before lot 1e (electricity).

## Ports and LEDs

Face coordinates: column and row in texels from the top left corner of the face as seen from outside. Model coordinates in px. On the front (north, z = 4.5), the viewer's left is east: x = 11.5 - column / 4. On the back (south, z = 11.5), the viewer's left is west: x = 4.5 + column / 4. On both: y = 3 - row / 4.

| Item | Face | Columns | Rows | Model x | Model y | Colour |
|---|---|---|---|---|---|---|
| Power LED | north | 3 | 4 | 10.5 to 10.75 | 1.75 to 2.0 | green |
| Line LED | north | 6 | 4 | 9.75 to 10.0 | 1.75 to 2.0 | green (normal operating colour, although the line is still inactive) |
| LAN 1 port | north | 11 to 13 | 6 to 8 | 8.0 to 8.75 | 0.75 to 1.5 | contacts row 6, socket rows 7 and 8 |
| LAN 1 LED | north | 12 | 4 | 8.25 to 8.5 | 1.75 to 2.0 | green |
| LAN 2 port | north | 15 to 17 | 6 to 8 | 7.0 to 7.75 | 0.75 to 1.5 | same |
| LAN 2 LED | north | 16 | 4 | 7.25 to 7.5 | 1.75 to 2.0 | green |
| LAN 3 port | north | 19 to 21 | 6 to 8 | 6.0 to 6.75 | 0.75 to 1.5 | same |
| LAN 3 LED | north | 20 | 4 | 6.25 to 6.5 | 1.75 to 2.0 | green |
| LAN 4 port | north | 23 to 25 | 6 to 8 | 5.0 to 5.75 | 0.75 to 1.5 | same |
| LAN 4 LED | north | 24 | 4 | 5.25 to 5.5 | 1.75 to 2.0 | green |
| Line input | south | 5 to 6 | 6 to 7 | 5.75 to 6.25 | 1.0 to 1.5 | socket |
| Power socket | south | 20 to 23 (hole 21 to 22) | 4 to 7 (hole 5 to 6) | 9.5 to 10.5 (hole 9.75 to 10.25) | 1.0 to 2.0 (hole 1.25 to 1.75) | metal ring, socket shadow hole |

Ports are numbered from the viewer's left: LAN n is engine port index n - 1 of `InternetBoxBlock.PORTS` (four ETHERNET, then LINE at index 4). No code reads these drawn positions yet. The face orientation (north: column 0 on the east side; south: column 0 on the west side) was checked in `FaceBakery.defaultFaceUV` of the 26.3 sources. The LAN ports use the 3 x 3 form (pitch of 4 texels); the line input is smaller (2 x 2, no contacts) so that it does not look like a LAN port.

## Display

w = d = 7, h = 3. Gui scale 1.21 (the width decides: 12 px drawn; height 9.1 px), magnification 1.936.

| Context | Rotation | Translation | Scale |
|---|---|---|---|
| gui | 30, 225, 0 | 0, 6.81, 3.93 | 1.21 |
| ground | 0, 0, 0 | 0, 6.12, 0 | 0.48 |
| fixed | 0, 0, 0 | 0, 6.3, 0 | 0.97 |
| thirdperson_righthand | 75, 45, 0 | 0, 3.73, 4.58 | 0.73 |
| firstperson_righthand | 0, 45, 0 | 0, 5.0, 0 | 0.77 |
| firstperson_lefthand | 0, 225, 0 | 0, 5.0, 0 | 0.77 |

Computed values, to check in game.
