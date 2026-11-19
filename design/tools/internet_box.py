"""Internet box, version 2: 7 x 7 x 3 px overall (design/blockbench/internet_box.md).

Elements, all inside the outer box of version 1 (so the Java shape does not change):
- base: dark plinth, set back 0.5 px, so that the box seems to stand off the desk;
- case: the body, behind a front panel 0.5 px thick;
- front_*: the front panel, split around the 4 LAN ports, which are real holes 0.5 px deep: the
  bottom of each port, with its contacts at the bottom, is drawn on the front of the case, seen
  through the hole;
- cover: top cover set back 0.25 px, which draws a bevel around the top.

The body is drawn as one envelope (front, back, sides, top, bottom); each element takes its own part
of it (Sheet.add_slice), so the texture runs on across the elements.

Variants of colour: --variant light (light body) or dark (dark body).
"""
import json

import wwb_design as d

ID = 'internet_box'
ENVELOPE = ([4.5, 0.5, 4.5], [11.5, 2.75, 11.5])
PANEL_Z = (4.5, 5.0)              # front panel thickness: depth of the ports
LAN_COLS = (11, 15, 19, 23)       # LAN 1 to 4, columns on the front, from the viewer's left
PORT_ROWS = (4, 7)                # rows 4 to 6 on the front
T = d.TEXELS_PER_PX


def front_x(col):
    """Model x of the left edge of a front column (front: column 0 on the east side)."""
    return ENVELOPE[1][0] - col / T


def front_y(row):
    return ENVELOPE[1][1] - row / T


HOLES_X = sorted((front_x(c + 3), front_x(c)) for c in LAN_COLS)   # west to east
HOLE_Y = (front_y(PORT_ROWS[1]), front_y(PORT_ROWS[0]))

BASE = {'name': 'base', 'from': [5.0, 0, 5.0], 'to': [11.0, 0.5, 11.0], 'cullfaces': ('down',)}
CASE = {'name': 'case', 'from': [4.5, 0.5, PANEL_Z[1]], 'to': [11.5, 2.75, 11.5]}
COVER = {'name': 'cover', 'from': [4.75, 2.75, 4.75], 'to': [11.25, 3.0, 11.25]}
FRONT_TOP = {'name': 'front_top', 'from': [4.5, HOLE_Y[1], PANEL_Z[0]], 'to': [11.5, 2.75, PANEL_Z[1]]}
FRONT_BOTTOM = {'name': 'front_bottom', 'from': [4.5, 0.5, PANEL_Z[0]], 'to': [11.5, HOLE_Y[0], PANEL_Z[1]]}
_edges = [4.5] + [x for hole in HOLES_X for x in hole] + [11.5]
FRONT_MIDDLE = [{'name': 'front_middle_%d' % (i + 1), 'from': [_edges[2 * i], HOLE_Y[0], PANEL_Z[0]],
                 'to': [_edges[2 * i + 1], HOLE_Y[1], PANEL_Z[1]]} for i in range(len(_edges) // 2)]
ELEMENTS = [BASE, CASE, FRONT_TOP, FRONT_BOTTOM] + FRONT_MIDDLE + [COVER]
ENV = {'name': 'env', 'from': ENVELOPE[0], 'to': ENVELOPE[1]}   # for the ports table only


def build(variant):
    body = 'light' if variant == 'light' else 'dark'
    sheet = d.Sheet(128)

    # Areas of the sheet (texels): the body envelope, the cover, the base, the walls of the holes.
    sheet.add_face('env', 'up', 0, 0, 7, 7)
    sheet.add_face('env', 'down', 32, 0, 7, 7)
    sheet.add_face('cover', 'up', 64, 0, 6.5, 6.5)
    sheet.add_face('base', 'down', 96, 0, 6, 6)
    for i, face in enumerate(('north', 'south', 'east', 'west')):
        sheet.add_face('env', face, 32 * i, 32, 7, 2.25)
        sheet.add_face('cover', face, 32 * i, 44, 6.5, 0.25)
        sheet.add_face('base', face, 32 * i, 48, 6, 0.5)
    sheet.add_face('wall', 'ceiling', 0, 52, 7, PANEL_Z[1] - PANEL_Z[0])
    sheet.add_face('wall', 'floor', 0, 56, 7, PANEL_Z[1] - PANEL_Z[0])
    sheet.add_face('wall', 'vertical', 32, 52, PANEL_Z[1] - PANEL_Z[0], HOLE_Y[1] - HOLE_Y[0])

    # Body: sides with a shadow line at the bottom (the cover bevel draws the top edge).
    for face in ('north', 'south', 'east', 'west'):
        _, _, w, h = sheet.faces[('env', face)]
        sheet.fill('env', face, body)
        sheet.rect('env', face, 0, h - 1, w, 1, body, 'sh')
    sheet.fill('env', 'up', body, 'hi')     # only a 1-texel rim shows around the cover
    sheet.fill('env', 'down', body)

    # Front: slim glass status strip with the power and line LEDs, then the 4 LAN ports (their
    # bottom, seen through the holes of the front panel), an LED above each.
    sheet.rect('env', 'north', 2, 3, 7, 3, 'screen')
    sheet.led('Power LED', 'env', 'north', 4, 4)
    sheet.led('Line LED', 'env', 'north', 6, 4)
    for n, col in enumerate(LAN_COLS, start=1):
        sheet.rj45('LAN %d port, hole %g px deep' % (n, PANEL_Z[1] - PANEL_Z[0]), 'env', 'north', col, PORT_ROWS[0], depth_tone='sh', contacts='bottom')
        sheet.led('LAN %d LED' % n, 'env', 'north', col + 1, 2)

    # Back: line input on the left, power socket (metal ring) on the right, centred on the same row.
    sheet.rj45('Line input', 'env', 'south', 5, 3, large=False)
    sheet.rect('env', 'south', 20, 2, 4, 4, 'metal')
    sheet.rect('env', 'south', 21, 3, 2, 2, 'socket', 'sh')
    sheet.items.append(('Power socket', 'env', 'south', 20, 2, 4, 4, 'metal ring, socket shadow hole (21 to 22, 3 to 4)'))

    # Walls of the holes, from light to dark: floor (lit from above), ceiling and sides, bottom.
    sheet.fill('wall', 'floor', 'dark', 'sh')
    sheet.fill('wall', 'ceiling', 'socket')
    sheet.fill('wall', 'vertical', 'socket')

    # Cover: plain top with a field of vent dots over the back half; sides in highlight tone.
    sheet.fill('cover', 'up', body)
    for row in range(13, 23, 2):
        for col in range(5, 22, 2):
            sheet.rect('cover', 'up', col, row, 1, 1, body, 'sh')
    for face in ('north', 'south', 'east', 'west'):
        sheet.fill('cover', face, body, 'hi')
    # Base: dark plinth (its top is hidden by the case and left out).
    for face in ('north', 'south', 'east', 'west', 'down'):
        sheet.fill('base', face, 'dark', 'sh')

    # Each face of the body elements: its part of the envelope, a wall of a hole, or left out (hidden).
    planes = {'north': (2, ENVELOPE[0][2]), 'south': (2, ENVELOPE[1][2]), 'west': (0, ENVELOPE[0][0]),
              'east': (0, ENVELOPE[1][0]), 'down': (1, ENVELOPE[0][1]), 'up': (1, ENVELOPE[1][1])}
    for e in [CASE, FRONT_TOP, FRONT_BOTTOM] + FRONT_MIDDLE:
        for face, (axis, plane) in planes.items():
            if (e['from'] if face in ('north', 'west', 'down') else e['to'])[axis] == plane:
                sheet.add_slice(e['name'], face, ('env', face), ENVELOPE, (e['from'], e['to']))
    # The front of the case is the bottom of the holes: same drawing as the envelope front.
    sheet.add_slice('case', 'north', ('env', 'north'), ENVELOPE, (ENVELOPE[0], [11.5, 2.75, 4.5]))
    sheet.faces[('front_top', 'down')] = sheet.faces[('wall', 'ceiling')]
    sheet.faces[('front_bottom', 'up')] = sheet.faces[('wall', 'floor')]
    for i, e in enumerate(FRONT_MIDDLE):
        if i > 0:
            sheet.faces[(e['name'], 'west')] = sheet.faces[('wall', 'vertical')]
        if i < len(FRONT_MIDDLE) - 1:
            sheet.faces[(e['name'], 'east')] = sheet.faces[('wall', 'vertical')]
    return sheet


if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--variant', choices=('light', 'dark'), default='light')
    p.add_argument('--name', default=ID, help='file name of the project (default: the device id)')
    p.add_argument('--force', action='store_true', help='overwrite a project never saved from Blockbench')
    p.add_argument('--table', action='store_true', help='print the ports and LEDs table and the display values')
    args = p.parse_args()
    sheet = build(args.variant)
    display = d.display(7, 7, 3)
    if args.table:
        print(d.ports_table(sheet, ELEMENTS + [ENV]))
        print(json.dumps(display, indent=2))
    else:
        d.write_bbmodel(args.name, d.bbmodel(ID, ELEMENTS, sheet, display), force=args.force)
