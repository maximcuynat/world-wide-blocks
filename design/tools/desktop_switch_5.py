"""5-port desktop switch: 4 x 3 x 1 px (design/blockbench/desktop_switch_5.md).

Rough draft approved by Max on 2026-11-20 as the starting point of the switch of lot 1d; not
exported to the assets (the block itself is the lot 1d issue "Desktop switch, 5 ports").

Family signature (design guide, section 5): a flat slab, the whole front taken by the ports, the
LEDs on the top along the front edge, one above each port.
"""
import json

import wwb_design as d

ID = 'desktop_switch_5'
ENVELOPE = ([6.0, 0, 6.5], [10.0, 1.0, 9.5])
PORT_COLS = (1, 4, 7, 10, 13)          # ports 1 to 5, from the viewer's left
FRONT = d.RecessedFront(ENVELOPE, 0.5, [(c, 2) for c in PORT_COLS], (1, 2))
ELEMENTS = FRONT.elements()
ENV = {'name': 'env', 'from': ENVELOPE[0], 'to': ENVELOPE[1]}   # for the ports table only


def build():
    sheet = d.Sheet(64)
    sheet.add_face('env', 'up', 0, 0, 4, 3)
    sheet.add_face('env', 'down', 20, 0, 4, 3)
    sheet.add_face('env', 'north', 0, 16, 4, 1)
    sheet.add_face('env', 'south', 20, 16, 4, 1)
    sheet.add_face('env', 'east', 0, 22, 3, 1)
    sheet.add_face('env', 'west', 20, 22, 3, 1)
    FRONT.add_walls(sheet, (0, 32), (0, 36), (20, 32))

    # Sides: no room for a bevel or a plinth at 1 px high: highlight and shadow lines instead.
    for face in ('north', 'south', 'east', 'west'):
        sheet.side('env', face, 'light')
    # Front: the bottom of each port (2 x 2: socket shadow tone, contacts row at the bottom).
    for n, c in enumerate(PORT_COLS, start=1):
        sheet.rect('env', 'north', c, 1, 2, 1, 'socket', 'sh')
        sheet.rect('env', 'north', c, 2, 2, 1, 'contacts')
        sheet.items.append(('Port %d, hole 0.5 px deep' % n, 'env', 'north', c, 1, 2, 2, 'socket shadow tone, contacts row at the bottom'))
    sheet.socket('Power input', 'env', 'south', 11, 1, 2, 2)
    # Top: power LED, then one LED above each port (the front is full), vent dots behind.
    sheet.fill('env', 'up', 'light')
    sheet.led('Power LED', 'env', 'up', 15, 1)
    for n, c in enumerate(PORT_COLS, start=1):
        sheet.led('Port %d LED' % n, 'env', 'up', 14 - c, 1)
    for row in range(5, 11, 2):
        for col in range(3, 14, 2):
            sheet.rect('env', 'up', col, row, 1, 1, 'light', 'sh')
    # Bottom: four rubber feet drawn in the corners.
    sheet.fill('env', 'down', 'light')
    for col, row in ((1, 1), (13, 1), (1, 9), (13, 9)):
        sheet.rect('env', 'down', col, row, 2, 2, 'dark')

    FRONT.map_faces(sheet)
    return sheet


DISPLAY = d.display(4, 3, 1)

if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--force', action='store_true', help='overwrite a project never saved from Blockbench')
    p.add_argument('--table', action='store_true', help='print the ports and LEDs table and the display values')
    args = p.parse_args()
    sheet = build()
    if args.table:
        print(d.ports_table(sheet, ELEMENTS + [ENV]))
        print(json.dumps(DISPLAY, indent=2))
    else:
        d.write_bbmodel(ID, d.bbmodel(ID, ELEMENTS, sheet, DISPLAY), force=args.force)
