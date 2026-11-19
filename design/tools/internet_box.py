"""Internet box: 7 x 7 x 3 px (design/blockbench/internet_box.md)."""
import json

import wwb_design as d

ID = 'internet_box'
CASE = {'name': 'case', 'from': [4.5, 0, 4.5], 'to': [11.5, 3, 11.5], 'cullfaces': ('down',)}
ELEMENTS = [CASE]

sheet = d.Sheet(64)
sheet.add_face('case', 'up', 0, 0, 7, 7)
sheet.add_face('case', 'down', 32, 0, 7, 7)
sheet.add_face('case', 'north', 0, 32, 7, 3)
sheet.add_face('case', 'south', 32, 32, 7, 3)
sheet.add_face('case', 'east', 0, 48, 7, 3)
sheet.add_face('case', 'west', 32, 48, 7, 3)

# Front: status LEDs on the left, 4 LAN ports on the right, one LED above each port.
sheet.side('case', 'north', 'light')
sheet.led('Power LED', 'case', 'north', 3, 4)
sheet.led('Line LED', 'case', 'north', 6, 4)
for n, col in enumerate((11, 15, 19, 23), start=1):
    sheet.rj45('LAN %d port' % n, 'case', 'north', col, 6)
    sheet.led('LAN %d LED' % n, 'case', 'north', col + 1, 4)

# Back: line input on the left, power socket (metal ring) on the right.
sheet.side('case', 'south', 'light')
sheet.rj45('Line input', 'case', 'south', 5, 6, large=False)
sheet.rect('case', 'south', 20, 4, 4, 4, 'metal')
sheet.rect('case', 'south', 21, 5, 2, 2, 'socket', 'sh')
sheet.items.append(('Power socket', 'case', 'south', 20, 4, 4, 4, 'metal ring, socket shadow hole (21 to 22, 5 to 6)'))

sheet.side('case', 'east', 'light')
sheet.side('case', 'west', 'light')

# Top: vent slots over the back half.
sheet.fill('case', 'up', 'light')
for row in (16, 18, 20, 22, 24):
    sheet.rect('case', 'up', 6, row, 16, 1, 'socket')

# Bottom: base colour, four rubber feet drawn in the corners.
sheet.fill('case', 'down', 'light')
for col, row in ((2, 2), (24, 2), (2, 24), (24, 24)):
    sheet.rect('case', 'down', col, row, 2, 2, 'dark')

DISPLAY = d.display(7, 7, 3)

if __name__ == '__main__':
    args = d.main_args()
    if args.table:
        print(d.ports_table(sheet, ELEMENTS))
        print(json.dumps(DISPLAY, indent=2))
    else:
        d.write_bbmodel(ID, d.bbmodel(ID, ELEMENTS, sheet, DISPLAY), args.preview, args.force)
