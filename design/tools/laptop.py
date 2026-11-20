"""Laptop: 14 x 9 px, base 1 px, lid 1 px (design/blockbench/laptop.md).

Family signature "computers" (design guide, section 5): dark plastic, like the tower; flat when closed, an opened
screen leaning back when open; keyboard and touchpad on the deck; power LED on the front edge.

Two block models sharing one texture: `laptop` (open, the screen turned by 22.5 degrees around its front bottom edge)
and `laptop_closed` (the lid lying on the base). FACING is the side where the user sits: the front edge of the keyboard
faces north for FACING north, the hinge is at the back (south).

The RJ45 port is on the left flank of the base for the seated user (east for FACING north), toward the back, small
size (2 x 2 texels: the base is only 1 px high) and recessed 0.5 px: the side of the base is a panel split around it.
"""
import json

import wwb_design as d

ENVELOPE = ([1.0, 0.0, 3.5], [15.0, 1.0, 12.5])     # the base, 14 x 1 x 9
HOLE_Z = (10.5, 11.0)                                # toward the back
HOLE_Y = (0.25, 0.75)
SIDE_X = (14.5, 15.0)                                # side panel of the left flank: depth of the port

BASE = [
    {'name': 'base', 'from': [1.0, 0.0, 3.5], 'to': [14.5, 1.0, 12.5], 'cullfaces': ('down',)},
    {'name': 'side_bottom', 'from': [14.5, 0.0, 3.5], 'to': [15.0, 0.25, 12.5], 'cullfaces': ('down',)},
    {'name': 'side_top', 'from': [14.5, 0.75, 3.5], 'to': [15.0, 1.0, 12.5]},
    {'name': 'side_front', 'from': [14.5, 0.25, 3.5], 'to': [15.0, 0.75, HOLE_Z[0]]},
    {'name': 'side_back', 'from': [14.5, 0.25, HOLE_Z[1]], 'to': [15.0, 0.75, 12.5]},
]
LID = {'name': 'lid', 'from': [1.0, 1.0, 3.5], 'to': [15.0, 2.0, 12.5]}
SCREEN = {'name': 'screen', 'from': [1.0, 1.0, 11.5], 'to': [15.0, 10.0, 12.5],
          'rotation': {'angle': 22.5, 'axis': 'x', 'origin': [8.0, 1.0, 11.5]}}
OPEN = BASE + [SCREEN]
CLOSED = BASE + [LID]
ENV = {'name': 'env', 'from': ENVELOPE[0], 'to': ENVELOPE[1]}   # for the ports table only


def build():
    sheet = d.Sheet(128)
    sheet.add_face('env', 'up', 0, 0, 14, 9)
    sheet.add_face('env', 'down', 64, 0, 14, 9)
    sheet.add_face('lid', 'up', 0, 40, 14, 9)
    sheet.add_face('screen', 'north', 64, 40, 14, 9)
    sheet.add_face('env', 'north', 0, 80, 14, 1)
    sheet.add_face('env', 'south', 0, 86, 14, 1)
    sheet.add_face('env', 'east', 64, 80, 9, 1)
    sheet.add_face('env', 'west', 64, 86, 9, 1)
    sheet.add_face('lid', 'north', 0, 92, 14, 1)
    sheet.add_face('lid', 'south', 0, 98, 14, 1)
    sheet.add_face('lid', 'east', 64, 92, 9, 1)
    sheet.add_face('lid', 'west', 64, 98, 9, 1)
    sheet.add_face('screen', 'up', 0, 104, 14, 0.25 * 4)
    sheet.add_face('screen', 'down', 0, 110, 14, 0.25 * 4)
    sheet.add_face('screen', 'east', 104, 80, 1, 9)
    sheet.add_face('screen', 'west', 110, 80, 1, 9)
    sheet.faces[('screen', 'south')] = sheet.faces[('lid', 'up')]   # the back of the open screen is the lid
    sheet.add_face('wall', 'ceiling', 116, 80, 0.5, 9)
    sheet.add_face('wall', 'floor', 120, 80, 0.5, 9)
    sheet.add_face('wall', 'side', 64, 110, 0.5, 0.5)

    # Base: dark plastic; highlight on top of the sides, shadow at the bottom.
    for face in ('north', 'south', 'east', 'west'):
        sheet.side('env', face, 'dark')
    # Deck (seen from above, row 0 on the user side): palm rest with the touchpad, keyboard, hinge strip at the back.
    sheet.fill('env', 'up', 'dark')
    sheet.rect('env', 'up', 20, 3, 16, 9, 'dark', 'hi')                                  # touchpad
    for row in (15, 18, 21, 24, 27):
        for col in range(4, 52, 4):
            sheet.rect('env', 'up', col, row, 3, 2, 'dark', 'sh')                       # keys
    sheet.rect('env', 'up', 16, 30, 24, 2, 'dark', 'sh')                                 # space bar
    sheet.rect('env', 'up', 0, 34, 56, 2, 'dark', 'sh')                                  # hinge strip
    sheet.fill('env', 'down', 'dark', 'sh')
    for col, row in ((3, 3), (51, 3), (3, 31), (51, 31)):
        sheet.rect('env', 'down', col, row, 2, 2, 'dark')                                # feet, drawn
    # Front edge: power LED on the user's right. Right flank (west): vents. Left flank (east): 2 USB, the RJ45 port.
    sheet.led('Power LED', 'env', 'north', 6, 1)
    for col in range(10, 30, 2):
        sheet.rect('env', 'west', col, 1, 1, 2, 'dark', 'sh')
    sheet.socket('USB 1', 'env', 'east', 22, 1, 2, 1)
    sheet.socket('USB 2', 'env', 'east', 26, 1, 2, 1)
    # Bottom of the recessed port, seen through the hole: socket shadow tone, contacts row at the bottom.
    hole_col = int((ENVELOPE[1][2] - HOLE_Z[1]) * d.TEXELS_PER_PX)      # east face: column 0 at the back
    hole_row = int((ENVELOPE[1][1] - HOLE_Y[1]) * d.TEXELS_PER_PX)
    for col, row, w, h in ((hole_col - 1, hole_row - 1, 4, 1), (hole_col - 1, hole_row + 2, 4, 1),
                           (hole_col - 1, hole_row, 1, 2), (hole_col + 2, hole_row, 1, 2)):
        sheet.rect('env', 'east', col, row, w, h, 'metal')                              # metal ring, 1 texel
    sheet.rect('env', 'east', hole_col, hole_row, 2, 1, 'socket', 'sh')
    sheet.rect('env', 'east', hole_col, hole_row + 1, 2, 1, 'contacts')
    sheet.items.append(('RJ45 port, hole 0.5 px deep', 'env', 'east', hole_col, hole_row, 2, 2,
                        'socket shadow tone, contacts row at the bottom'))
    sheet.fill('wall', 'floor', 'dark', 'sh')
    sheet.fill('wall', 'ceiling', 'socket')
    sheet.fill('wall', 'side', 'socket')

    # Lid (closed) and back of the screen (open): dark plastic, a highlight frame inset by 2 texels.
    sheet.fill('lid', 'up', 'dark')
    for col in range(2, 54):
        sheet.rect('lid', 'up', col, 2, 1, 1, 'dark', 'hi')
        sheet.rect('lid', 'up', col, 33, 1, 1, 'dark', 'hi')
    for row in range(2, 34):
        sheet.rect('lid', 'up', 2, row, 1, 1, 'dark', 'hi')
        sheet.rect('lid', 'up', 53, row, 1, 1, 'dark', 'hi')
    for face in ('north', 'south', 'east', 'west'):
        sheet.fill('lid', face, 'dark')
        _, _, w, h = sheet.faces[('lid', face)]
        sheet.rect('lid', face, 0, h - 1, w, 1, 'dark', 'sh')                           # gap line above the base
    # Screen (open): bezel in dark plastic, display off, a camera dot in the top bezel.
    sheet.fill('screen', 'north', 'dark')
    sheet.rect('screen', 'north', 2, 2, 52, 31, 'screen')
    sheet.rect('screen', 'north', 27, 0, 2, 1, 'socket')
    for face in ('up', 'down', 'east', 'west'):
        sheet.fill('screen', face, 'dark')

    # Faces of the base pieces: their part of the envelope, a wall of the hole, or left out (hidden).
    lo, hi = ENVELOPE
    planes = {'north': (2, lo[2]), 'south': (2, hi[2]), 'west': (0, lo[0]),
              'east': (0, hi[0]), 'down': (1, lo[1]), 'up': (1, hi[1])}
    for e in BASE:
        for face, (axis, plane) in planes.items():
            if (e['from'] if face in ('north', 'west', 'down') else e['to'])[axis] == plane:
                sheet.add_slice(e['name'], face, ('env', face), ENVELOPE, (e['from'], e['to']))
    # The left flank of the base body is the bottom of the hole: same drawing as the envelope flank.
    sheet.add_slice('base', 'east', ('env', 'east'), ENVELOPE, ([14.5, 0.0, 3.5], [14.5, 1.0, 12.5]))
    sheet.faces[('side_top', 'down')] = sheet.faces[('wall', 'ceiling')]
    sheet.faces[('side_bottom', 'up')] = sheet.faces[('wall', 'floor')]
    sheet.faces[('side_front', 'south')] = sheet.faces[('wall', 'side')]
    sheet.faces[('side_back', 'north')] = sheet.faces[('wall', 'side')]
    return sheet


# Bounds of the open model: the screen turned by 22.5 degrees reaches y 9.32 and z 15.86 (computed from its corners).
DISPLAY_OPEN = d.display_bounds([1.0, 0.0, 3.5], [15.0, 9.32, 15.86])
DISPLAY_CLOSED = d.display(14, 9, 2)


if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--prefix', default='', help='folder and prefix of the projects, for example drafts/')
    p.add_argument('--force', action='store_true', help='overwrite projects never saved from Blockbench')
    p.add_argument('--table', action='store_true', help='print the ports and LEDs table')
    args = p.parse_args()
    sheet = build()
    if args.table:
        print(d.ports_table(sheet, OPEN + [LID, ENV]))
        print(json.dumps({'open': DISPLAY_OPEN, 'closed': DISPLAY_CLOSED}, indent=2))
    else:
        d.write_bbmodel(args.prefix + 'laptop', d.bbmodel('laptop', OPEN, sheet, DISPLAY_OPEN), force=args.force)
        d.write_bbmodel(args.prefix + 'laptop_closed', d.bbmodel('laptop', CLOSED, sheet, DISPLAY_CLOSED), force=args.force)
