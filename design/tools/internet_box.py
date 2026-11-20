"""Internet box, version 3: standing box, 7 x 3 x 7 px (design/blockbench/internet_box.md).

Family signature (design guide, section 5): a standing case, with a black glass band that stands out
of the top of the front, and 4 recessed LAN ports low on the front (RJ45, the single size: 2 x 2 texels).

Elements (front facing north):
- base: dark plinth, set back 0.5 px;
- case and front_*: the body behind a front panel 0.5 px thick, split around the 4 LAN ports
  (RecessedFront);
- band: the glass status band, 0.25 px out of the front, set back 0.5 px from each side edge;
- cover: top cover set back 0.25 px (bevel).
"""
import json

import wwb_design as d

ID = 'internet_box'
T = d.TEXELS_PER_PX
ENVELOPE = ([4.5, 0.5, 6.75], [11.5, 6.75, 9.5])   # the band takes the first 0.25 px of the depth
LAN_COLS = (7, 11, 15, 19)                         # LAN 1 to 4, from the viewer's left; 7 texels on each side
PORT_ROWS = (19, 2)                                # the bottom of the ports stays on row 20
BAND_ROWS = (2, 5)                                 # band height on the front, from the top

FRONT = d.RecessedFront(ENVELOPE, 0.5, [(c, 2) for c in LAN_COLS], PORT_ROWS)
BASE = {'name': 'base', 'from': [5.0, 0, 7.25], 'to': [11.0, 0.5, 9.0], 'cullfaces': ('down',)}
COVER = {'name': 'cover', 'from': [4.75, 6.75, 7.0], 'to': [11.25, 7.0, 9.25]}
_top = ENVELOPE[1][1]
BAND = {'name': 'band', 'from': [5.0, _top - (BAND_ROWS[0] + BAND_ROWS[1]) / T, 6.5],
        'to': [11.0, _top - BAND_ROWS[0] / T, 6.75]}
ELEMENTS = [BASE] + FRONT.elements() + [BAND, COVER]
ENV = {'name': 'env', 'from': ENVELOPE[0], 'to': ENVELOPE[1]}   # for the ports table only


def build():
    sheet = d.Sheet(128)
    # Areas of the sheet (texels).
    sheet.add_face('env', 'north', 0, 0, 7, 6.25)
    sheet.add_face('env', 'south', 32, 0, 7, 6.25)
    sheet.add_face('env', 'east', 64, 0, 2.75, 6.25)
    sheet.add_face('env', 'west', 80, 0, 2.75, 6.25)
    sheet.add_face('env', 'up', 96, 0, 7, 2.75)
    sheet.add_face('env', 'down', 96, 16, 7, 2.75)
    sheet.add_face('cover', 'up', 0, 32, 6.5, 2.25)
    sheet.add_face('cover', 'north', 32, 32, 6.5, 0.25)
    sheet.add_face('cover', 'south', 32, 34, 6.5, 0.25)
    sheet.add_face('cover', 'east', 32, 36, 2.25, 0.25)
    sheet.add_face('cover', 'west', 32, 38, 2.25, 0.25)
    sheet.add_face('base', 'down', 64, 32, 6, 1.75)
    sheet.add_face('base', 'north', 0, 44, 6, 0.5)
    sheet.add_face('base', 'south', 32, 44, 6, 0.5)
    sheet.add_face('base', 'east', 64, 44, 1.75, 0.5)
    sheet.add_face('base', 'west', 80, 44, 1.75, 0.5)
    FRONT.add_walls(sheet, (0, 52), (0, 56), (32, 52))
    sheet.add_face('band', 'north', 0, 64, 6, 1.25)
    sheet.add_face('band', 'up', 0, 72, 6, 0.25)
    sheet.add_face('band', 'down', 0, 74, 6, 0.25)
    sheet.add_face('band', 'east', 32, 64, 0.25, 1.25)
    sheet.add_face('band', 'west', 34, 64, 0.25, 1.25)

    # Body: light plastic, shadow line at the bottom of the sides; rim around the cover in highlight.
    for face in ('north', 'south', 'east', 'west'):
        _, _, w, h = sheet.faces[('env', face)]
        sheet.fill('env', face, 'light')
        sheet.rect('env', face, 0, h - 1, w, 1, 'light', 'sh')
    sheet.fill('env', 'up', 'light', 'hi')
    sheet.fill('env', 'down', 'light')

    # Front: 4 LAN ports low (their bottom, seen through the holes), an LED above the left texel of each. The contacts
    # are drawn where they stay visible through a 2 x 2 hole: the bottom row of its back (seen from the front) and the
    # back row of its floor (seen from above or from the side).
    for n, col in enumerate(LAN_COLS, start=1):
        sheet.rj45('LAN %d port, hole 0.5 px deep' % n, 'env', 'north', col, PORT_ROWS[0], depth_tone='sh')
        _, _, floor_w, _ = sheet.faces[('wall', 'floor')]   # floor of the hole: columns from the west, row 1 at the back
        sheet.rect('wall', 'floor', floor_w - col - 2, 1, 2, 1, 'contacts')
        label, el, face, c, r, w, h, desc = sheet.items[-1]
        sheet.items[-1] = (label, el, face, c, r, w, h, desc + ', contacts on the back row of the hole floor')
        sheet.led('LAN %d LED' % n, 'env', 'north', col, PORT_ROWS[0] - 2)

    # Glass band: all its faces in the screen colour, power and line LEDs on the left.
    for face in ('north', 'up', 'down', 'east', 'west'):
        sheet.fill('band', face, 'screen')
    sheet.led('Power LED', 'band', 'north', 1, 2)
    sheet.led('Line LED', 'band', 'north', 3, 2)

    # Back (painted, it faces the wall): line input on the left, power socket on the right.
    sheet.rj45('Line input', 'env', 'south', 7, 18)
    sheet.rect('env', 'south', 18, 17, 4, 4, 'metal')
    sheet.rect('env', 'south', 19, 18, 2, 2, 'socket', 'sh')
    sheet.items.append(('Power socket', 'env', 'south', 18, 17, 4, 4, 'metal ring, socket shadow hole (19 to 20, 18 to 19)'))

    # Sides: vent dots in the shadow tone.
    for face in ('east', 'west'):
        for col in range(3, 9, 2):
            for row in range(4, 21, 2):
                sheet.rect('env', face, col, row, 1, 1, 'light', 'sh')

    # Cover: plain top, sides in highlight; plinth in dark plastic shadow tone.
    sheet.fill('cover', 'up', 'light')
    for face in ('north', 'south', 'east', 'west'):
        sheet.fill('cover', face, 'light', 'hi')
    for face in ('north', 'south', 'east', 'west', 'down'):
        sheet.fill('base', face, 'dark', 'sh')

    FRONT.map_faces(sheet)
    return sheet


DISPLAY = d.display(7, 3, 7)

if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--name', default=ID, help='file name of the project (default: the device id)')
    p.add_argument('--force', action='store_true', help='overwrite a project never saved from Blockbench')
    p.add_argument('--table', action='store_true', help='print the ports and LEDs table and the display values')
    args = p.parse_args()
    sheet = build()
    if args.table:
        print(d.ports_table(sheet, ELEMENTS + [ENV]))
        print(json.dumps(DISPLAY, indent=2))
    else:
        d.write_bbmodel(args.name, d.bbmodel(ID, ELEMENTS, sheet, DISPLAY), force=args.force)
