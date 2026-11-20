"""Desktop switches, one family (design/blockbench/desktop_switch_5.md, desktop_switch_8.md).

Family signature (design guide, section 5): a flat slab of light plastic, 1 px high; the whole front taken by RJ45
ports recessed 0.5 px (the single size, 2 x 2 texels, contacts on the bottom row and on the back row of the floor);
the LEDs on the top along the front edge, one above each port, and the power LED at the left end; vent dots on the
top, rubber feet drawn underneath, the power input at the back.

- desktop_switch_5: 4 x 3 x 1 px, 5 ports at a pitch of 3 texels, 1 texel of margin on each side.
- desktop_switch_8: 6.5 x 4 x 1 px (width raised by the fit rule, guide section 2), 8 ports in two groups of 4: a
  pitch of 3 texels in a group, 2 texels between the groups, 1 texel of margin on each side (26 texels, the whole
  front).

Not exported: the blocks come with lot 1d-1 (issues "Desktop switch, 5 ports" and "Desktop switch, 8 ports").

    python3 design/tools/desktop_switches.py [--prefix drafts/] [--force] [--table]
"""
import json

import wwb_design as d

PORT_ROWS = (1, 2)       # rows of the holes on the 4 rows of the front: 1 above, 1 below
SWITCHES = {
    'desktop_switch_5': {
        'envelope': ([6.0, 0.0, 6.5], [10.0, 1.0, 9.5]),
        'ports': (1, 4, 7, 10, 13),                      # columns from the viewer's left
        'power_input': 11,                               # column of the power input at the back
        'vents': (range(3, 14, 2), range(5, 11, 2)),     # columns, rows of the vent dots on the top
        'feet': ((1, 1), (13, 1), (1, 9), (13, 9)),
    },
    'desktop_switch_8': {
        'envelope': ([4.75, 0.0, 6.0], [11.25, 1.0, 10.0]),
        'ports': (1, 4, 7, 10, 14, 17, 20, 23),
        'power_input': 20,
        'vents': (range(3, 24, 2), range(5, 14, 2)),
        'feet': ((1, 1), (23, 1), (1, 13), (23, 13)),
    },
}


def front(spec):
    return d.RecessedFront(spec['envelope'], 0.5, [(c, 2) for c in spec['ports']], PORT_ROWS)


def size(spec):
    lo, hi = spec['envelope']
    return hi[0] - lo[0], hi[2] - lo[2], hi[1] - lo[1]     # w, d, h


def build(spec):
    w, dep, h = size(spec)
    tw, td = int(w * d.TEXELS_PER_PX), int(dep * d.TEXELS_PER_PX)
    fr = front(spec)
    sheet = d.Sheet(64)
    sheet.add_face('env', 'up', 0, 0, w, dep)
    sheet.add_face('env', 'down', 32, 0, w, dep)
    sheet.add_face('env', 'north', 0, 20, w, h)
    sheet.add_face('env', 'south', 32, 20, w, h)
    sheet.add_face('env', 'east', 0, 28, dep, h)
    sheet.add_face('env', 'west', 20, 28, dep, h)
    fr.add_walls(sheet, (0, 36), (0, 40), (32, 36))

    # Sides: no room for a bevel or a plinth at 1 px high: highlight and shadow lines instead.
    for face in ('north', 'south', 'east', 'west'):
        sheet.side('env', face, 'light')
    # Front: the bottom of each port; the floor of each hole gets its contacts row too.
    for n, c in enumerate(spec['ports'], start=1):
        sheet.rj45('Port %d, hole 0.5 px deep' % n, 'env', 'north', c, PORT_ROWS[0], depth_tone='sh')
        label, el, face, col, row, pw, ph, desc = sheet.items[-1]
        sheet.items[-1] = (label, el, face, col, row, pw, ph, desc + ', contacts on the back row of the hole floor')
    fr.add_floor_contacts(sheet)
    sheet.socket('Power input', 'env', 'south', spec['power_input'], 1, 2, 2)
    # Top: power LED at the left end seen from the front, one LED above each port on the front edge (the front is
    # full, guide section 5), vent dots behind. Top columns run from the west, front columns from the east.
    sheet.fill('env', 'up', 'light')
    sheet.led('Power LED', 'env', 'up', tw - 1, 1)
    for n, c in enumerate(spec['ports'], start=1):
        sheet.led('Port %d LED' % n, 'env', 'up', tw - 2 - c, 1)
    cols, rows = spec['vents']
    for row in rows:
        for col in cols:
            sheet.rect('env', 'up', col, row, 1, 1, 'light', 'sh')
    # Bottom: four rubber feet drawn in the corners.
    sheet.fill('env', 'down', 'light')
    for col, row in spec['feet']:
        sheet.rect('env', 'down', col, row, 2, 2, 'dark')
    assert tw <= 32 and td <= 16, 'the faces must fit the 64 x 64 sheet'

    fr.map_faces(sheet)
    return sheet


if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--prefix', default='', help='folder and prefix of the projects, for example drafts/')
    p.add_argument('--force', action='store_true', help='overwrite projects never saved from Blockbench')
    p.add_argument('--table', action='store_true', help='print the ports and LEDs tables and the display values')
    args = p.parse_args()
    for device_id, spec in SWITCHES.items():
        sheet = build(spec)
        display = d.display(*size(spec))
        elements = front(spec).elements()
        if args.table:
            print('##', device_id)
            print(d.ports_table(sheet, elements + [{'name': 'env', 'from': spec['envelope'][0], 'to': spec['envelope'][1]}]))
            print(json.dumps(display, indent=2))
        else:
            d.write_bbmodel(args.prefix + device_id, d.bbmodel(device_id, elements, sheet, display), force=args.force)
