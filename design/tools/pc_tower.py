"""PC tower: 7 x 15 x 15 px (design/blockbench/pc_tower.md).

Family signature "computers" (design guide, section 5): dark plastic, tall and narrow, a raised
vertical front panel with a power button standing out of it, the power and disk LEDs, USB ports and
vents; at the back, the metal plate with the power supply at the bottom.

Variants: --front classic (optical drive bay, dotted vents) or modern (no bay, vertical slats);
--port low (RJ45 on a network card in the lowest expansion slot, just above the power supply) or
high (RJ45 on the motherboard I/O panel, at the top).
"""
import json

import wwb_design as d

ID = 'pc_tower'
FEET = [{'name': 'foot_%s' % n, 'from': [x, 0, z], 'to': [x + 1, 0.5, z + 1.5], 'cullfaces': ('down',)}
        for n, x, z in (('front_left', 10.0, 1.5), ('front_right', 5.0, 1.5), ('back_left', 10.0, 13.0), ('back_right', 5.0, 13.0))]
CASE = {'name': 'case', 'from': [4.5, 0.5, 1.0], 'to': [11.5, 15.0, 15.5]}
BEZEL = {'name': 'front_panel', 'from': [4.75, 0.75, 0.75], 'to': [11.25, 14.75, 1.0]}
BUTTON = {'name': 'power_button', 'from': [7.5, 0, 0.5], 'to': [8.5, 0, 0.75]}   # 1 px, centred on the 0.25 px grid; y set by the variant
ELEMENTS = FEET + [CASE, BEZEL, BUTTON]


def build(front, port):
    button_rows = (13, 4) if front == 'classic' else (5, 4)          # rows on the front panel
    BUTTON['from'][1] = BEZEL['to'][1] - (button_rows[0] + button_rows[1]) / d.TEXELS_PER_PX
    BUTTON['to'][1] = BEZEL['to'][1] - button_rows[0] / d.TEXELS_PER_PX

    sheet = d.Sheet(128)
    sheet.add_face('case', 'south', 0, 0, 7, 14.5)
    sheet.add_face('case', 'up', 32, 0, 7, 14.5)
    sheet.add_face('case', 'down', 64, 0, 7, 14.5)
    sheet.add_face('case', 'north', 96, 0, 7, 14.5)
    sheet.add_face('case', 'east', 0, 64, 14.5, 14.5)
    sheet.faces[('case', 'west')] = sheet.faces[('case', 'east')]   # same side panel on both sides
    sheet.add_face('front_panel', 'north', 64, 64, 6.5, 14)
    sheet.add_face('front_panel', 'east', 92, 64, 0.25, 14)
    sheet.add_face('front_panel', 'west', 94, 64, 0.25, 14)
    sheet.add_face('front_panel', 'up', 64, 122, 6.5, 0.25)
    sheet.add_face('front_panel', 'down', 64, 124, 6.5, 0.25)
    sheet.add_face('power_button', 'north', 100, 64, 1, 1)
    sheet.add_face('power_button', 'east', 105, 64, 0.25, 1)
    sheet.add_face('power_button', 'west', 107, 64, 0.25, 1)
    sheet.add_face('power_button', 'up', 100, 69, 1, 0.25)
    sheet.add_face('power_button', 'down', 100, 71, 1, 0.25)
    sheet.add_face('feet', 'north', 110, 64, 1, 0.5)
    sheet.add_face('feet', 'east', 110, 67, 1.5, 0.5)
    sheet.add_face('feet', 'down', 116, 64, 1, 1.5)
    for f in FEET:   # all the feet share the same areas; their top is against the case
        for face, area in (('north', 'north'), ('south', 'north'), ('east', 'east'), ('west', 'east'), ('down', 'down')):
            sheet.faces[(f['name'], face)] = sheet.faces[('feet', area)]

    # Case: dark plastic. Sides: removable panel outlined, a field of vents. Top: vents at the back.
    sheet.fill('case', 'north', 'dark')                 # only a rim shows around the front panel
    sheet.fill('case', 'east', 'dark')
    for col in range(3, 55):
        sheet.rect('case', 'east', col, 3, 1, 1, 'dark', 'sh')
        sheet.rect('case', 'east', col, 54, 1, 1, 'dark', 'sh')
    for row in range(3, 55):
        sheet.rect('case', 'east', 3, row, 1, 1, 'dark', 'sh')
        sheet.rect('case', 'east', 54, row, 1, 1, 'dark', 'sh')
    for col in range(8, 22, 2):
        for row in range(8, 22, 2):
            sheet.rect('case', 'east', col, row, 1, 1, 'dark', 'sh')
    sheet.fill('case', 'up', 'dark')
    for col in range(6, 22, 2):
        for row in range(34, 53, 2):
            sheet.rect('case', 'up', col, row, 1, 1, 'dark', 'sh')
    sheet.fill('case', 'down', 'dark', 'sh')

    # Back: the metal plate (dark rim), I/O panel and fan at the top, expansion slots, power supply at the bottom.
    sheet.fill('case', 'south', 'dark')
    sheet.rect('case', 'south', 1, 1, 26, 56, 'metal')
    sheet.rect('case', 'south', 3, 3, 8, 14, 'metal', 'hi')                       # I/O panel
    for row in (5, 7, 9):
        sheet.rect('case', 'south', 4, row, 2, 1, 'socket')                      # USB
        sheet.rect('case', 'south', 8, row, 2, 1, 'socket')
    for col in range(14, 25, 2):                                                  # exhaust fan
        for row in range(4, 16, 2):
            if (col - 19) ** 2 + (row - 9.5) ** 2 <= 30:
                sheet.rect('case', 'south', col, row, 1, 1, 'socket')
    for row in (24, 28, 32, 36):                                                  # slot covers
        sheet.rect('case', 'south', 13, row, 12, 1, 'metal', 'sh')
    if port == 'low':
        sheet.rect('case', 'south', 13, 38, 12, 7, 'metal', 'hi')                 # network card bracket
        sheet.rj45('RJ45 port (network card, lowest slot)', 'case', 'south', 16, 40)
        sheet.led('Link LED', 'case', 'south', 20, 41)
    else:
        sheet.rect('case', 'south', 13, 40, 12, 1, 'metal', 'sh')
        sheet.rj45('RJ45 port (I/O panel)', 'case', 'south', 4, 12)
        sheet.led('Link LED', 'case', 'south', 8, 13)
    sheet.rect('case', 'south', 2, 46, 24, 10, 'metal', 'sh')                     # power supply
    sheet.rect('case', 'south', 4, 48, 4, 5, 'socket')                            # power inlet
    sheet.rect('case', 'south', 5, 49, 2, 1, 'contacts')
    sheet.rect('case', 'south', 9, 49, 2, 3, 'dark')                              # power switch
    for col in range(14, 24, 2):
        for row in range(48, 55, 2):
            sheet.rect('case', 'south', col, row, 1, 1, 'socket')
    sheet.items.append(('Power inlet', 'case', 'south', 4, 48, 4, 5, 'socket, contacts row 49'))

    # Front panel: raised, dark plastic, highlight on its edges.
    sheet.fill('front_panel', 'north', 'dark')
    for face in ('east', 'west', 'up', 'down'):
        sheet.fill('front_panel', face, 'dark', 'hi')
    if front == 'classic':
        sheet.rect('front_panel', 'north', 2, 3, 22, 5, 'dark', 'sh')             # optical drive bay
        sheet.rect('front_panel', 'north', 3, 5, 20, 1, 'dark', 'hi')             # tray line
        sheet.rect('front_panel', 'north', 20, 6, 2, 1, 'metal')                  # eject button
        leds_row, usb_row = 18, 21
        for col in range(3, 23, 2):                                               # dotted vents
            for row in range(28, 53, 2):
                sheet.rect('front_panel', 'north', col, row, 1, 1, 'socket')
    else:
        leds_row, usb_row = 11, 14
        for col in range(3, 23, 2):                                               # vertical slats
            sheet.rect('front_panel', 'north', col, 18, 1, 35, 'dark', 'sh')
    sheet.led('Power LED', 'front_panel', 'north', 10, leds_row)
    sheet.led('Disk LED', 'front_panel', 'north', 15, leds_row, 'led_orange')
    sheet.socket('USB 1', 'front_panel', 'north', 9, usb_row, 2, 1)
    sheet.socket('USB 2', 'front_panel', 'north', 15, usb_row, 2, 1)
    sheet.items.append(('Power button (stands 0.25 px out)', 'front_panel', 'north', 11, button_rows[0], 4, 4, 'metal'))

    # Power button: metal, highlight on top.
    sheet.fill('power_button', 'north', 'metal')
    sheet.rect('power_button', 'north', 0, 0, 4, 1, 'metal', 'hi')
    for face in ('east', 'west', 'up', 'down'):
        sheet.fill('power_button', face, 'metal', 'sh')
    # Feet: dark plastic shadow tone.
    for face in ('north', 'east', 'down'):
        sheet.fill('feet', face, 'dark', 'sh')
    return sheet


DISPLAY = d.display(7, 15, 15)

if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--front', choices=('classic', 'modern'), default='classic')
    p.add_argument('--port', choices=('low', 'high'), default='low')
    p.add_argument('--name', default=ID, help='file name of the project (default: the device id)')
    p.add_argument('--force', action='store_true', help='overwrite a project never saved from Blockbench')
    p.add_argument('--table', action='store_true', help='print the ports and LEDs table and the display values')
    args = p.parse_args()
    sheet = build(args.front, args.port)
    if args.table:
        print(d.ports_table(sheet, ELEMENTS))
        print(json.dumps(DISPLAY, indent=2))
    else:
        d.write_bbmodel(args.name, d.bbmodel(ID, ELEMENTS, sheet, DISPLAY), force=args.force)
