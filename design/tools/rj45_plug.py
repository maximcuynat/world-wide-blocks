"""RJ45 plug for lot 1d-1 (docs/cable-design.md, section 4): one model for every device, drawn by the client in the
port of each cable end (issue "Cable attached to its port, with its plug").

The plug of a port facing north, its mouth at the centre of the block (8, 8, 8); the client turns it with the port
and moves it to the mouth. The cable itself (0.25 px, square) is drawn by the code from the end of the boot.

- body: clear plastic, the section of the hole (2 x 2 texels, 0.5 px), 0.5 px in the hole and 0.5 px out; the latch
  on the top; the gold contacts at the tip, underneath (inside the hole, as on a real plug);
- boot: the long boot, 0.75 px, as wide as the body, in the colour of the cord, a grip groove across it.

One texture for all the colours: the body once, the boot once per cord colour (`design/palette.json`, key `cords`),
so that a model per colour only changes the UV of the boot (chosen by Max on 2026-11-20, design/blockbench/rj45_plug.md).

    python3 design/tools/rj45_plug.py [--prefix drafts/] [--force]   design/blockbench/<prefix>rj45_plug_<colour>.bbmodel
    python3 design/tools/rj45_plug.py --scene                        preview scene, see below

--scene writes drafts/rj45_plug_cables.bbmodel (the floor and a cable stub out of each plug: the cable is code, not
model) and prints the arguments of the preview, every port of the five devices with a plug:

    python3 design/tools/preview.py out.png --scene $(python3 design/tools/rj45_plug.py --scene)
"""
import wwb_design as d
import cables

MOUTH = [8.0, 8.0, 8.0]
HOLE = cables.HOLE              # 0.5 px: 2 x 2 texels
DEPTH = cables.DEPTH            # 0.5 px in the hole
OUT = cables.BODY_OUT           # 0.5 px out of the hole
BOOT = cables.BOOT              # 0.75 px
H = HOLE / 2
BODY = {'name': 'body', 'from': [MOUTH[0] - H, MOUTH[1] - H, MOUTH[2] - OUT], 'to': [MOUTH[0] + H, MOUTH[1] + H, MOUTH[2] + DEPTH]}
BOOT_EL = {'name': 'boot', 'from': [MOUTH[0] - H, MOUTH[1] - H, MOUTH[2] - OUT - BOOT], 'to': [MOUTH[0] + H, MOUTH[1] + H, MOUTH[2] - OUT]}
ELEMENTS = [BODY, BOOT_EL]
COLOURS = list(cables.CORDS)


def cord(name, tone='base'):
    return cables.cord(name, tone)


def paint(sheet, element, face, col, row, w, h, colour):
    """Like Sheet.rect, with any colour (the cord colours are not roles of the devices)."""
    fx, fy, fw, fh = sheet.faces[(element, face)]
    if col < 0 or row < 0 or col + w > fw or row + h > fh:
        raise ValueError('rectangle outside face %s/%s' % (element, face))
    for j in range(h):
        for i in range(w):
            sheet.pixels[fy + row + j][fx + col + i] = colour


def build():
    sheet = d.Sheet(32)
    # Body (faces seen from outside; its north end is against the boot and its south end is the bottom of the hole:
    # both hidden). Top and bottom: u across (x), v along the plug, row 0 at the boot end (north). Sides: u along the
    # plug, east from the tip (south), west from the boot end.
    sheet.add_face('body', 'up', 0, 0, HOLE, OUT + DEPTH)
    sheet.add_face('body', 'down', 2, 0, HOLE, OUT + DEPTH)
    sheet.add_face('body', 'east', 4, 0, OUT + DEPTH, HOLE)
    sheet.add_face('body', 'west', 4, 2, OUT + DEPTH, HOLE)
    hi, base = d.colour('light', 'hi'), d.colour('light')
    for face in ('up', 'down', 'east', 'west'):
        sheet.fill('body', face, 'light', 'hi')
    paint(sheet, 'body', 'up', 0, 1, 2, 1, base)                      # latch, just out of the hole
    sheet.rect('body', 'down', 0, 0, 2, 2, 'contacts', 'hi')          # contacts at the tip (row 0 of the bottom), inside the hole
    sheet.rect('body', 'east', 2, 1, 2, 1, 'light')                   # shadow line on the lower edge, out of the hole
    sheet.rect('body', 'west', 0, 1, 2, 1, 'light')
    # Boot, once per cord colour: base, a grip groove in the shadow tone across the middle, the highlight on its top
    # edge; its north end (where the cable comes out) in the shadow tone.
    for i, colour in enumerate(COLOURS):
        x, y = 16 * (i % 2), 6 + 6 * (i // 2)
        el = 'boot_' + colour
        sheet.add_face(el, 'up', x, y, HOLE, BOOT)
        sheet.add_face(el, 'down', x + 2, y, HOLE, BOOT)
        sheet.add_face(el, 'east', x + 4, y, BOOT, HOLE)
        sheet.add_face(el, 'west', x + 7, y, BOOT, HOLE)
        sheet.add_face(el, 'north', x + 10, y, HOLE, HOLE)
        for face in ('up', 'down', 'east', 'west', 'north'):
            _, _, w, h = sheet.faces[(el, face)]
            paint(sheet, el, face, 0, 0, w, h, cord(colour))
        paint(sheet, el, 'up', 0, 1, 2, 1, cord(colour, 'sh'))       # groove across the top
        paint(sheet, el, 'down', 0, 1, 2, 1, cord(colour, 'sh'))
        paint(sheet, el, 'east', 1, 0, 1, 2, cord(colour, 'sh'))     # groove across the sides
        paint(sheet, el, 'west', 1, 0, 1, 2, cord(colour, 'sh'))
        paint(sheet, el, 'east', 0, 0, 1, 1, cord(colour, 'hi'))     # highlight on the top edge, either side of it
        paint(sheet, el, 'east', 2, 0, 1, 1, cord(colour, 'hi'))
        paint(sheet, el, 'west', 0, 0, 1, 1, cord(colour, 'hi'))
        paint(sheet, el, 'west', 2, 0, 1, 1, cord(colour, 'hi'))
        paint(sheet, el, 'north', 0, 0, 2, 2, cord(colour, 'sh'))
    return sheet


def project(sheet, colour):
    for face in ('up', 'down', 'east', 'west', 'north'):
        sheet.faces[('boot', face)] = sheet.faces[('boot_' + colour, face)]
    model = d.bbmodel('rj45_plug_' + colour, ELEMENTS, sheet, d.display(HOLE, OUT + DEPTH + BOOT, HOLE))
    model['textures'][0]['name'] = 'rj45_plug.png'   # one texture shared by the models of all the colours
    return model


# --- Preview scene: the ports of the devices, as placed in the command of the README of this script ---------------

# id: (dx, dz) of the device in the scene; ports: (centre of the mouth in the device, angle of the port: 0 north,
# 90 east, 180 south), from the device sheets.
DEVICES = {
    'pc_tower': (-16.0, 0.0, [([8.75, 4.5, 15.5], 180)]),
    'desktop_switch_5': (0.0, 0.0, [([x, 0.5, 6.5], 0) for x in (9.5, 8.75, 8.0, 7.25, 6.5)]),
    'internet_box': (16.0, 0.0, [([x, 1.75, 6.75], 0) for x in (9.5, 8.5, 7.5, 6.5)]),
    'laptop': (32.0, 0.0, [([15.0, 0.5, 10.75], 90)]),
    'desktop_switch_8': (48.0, 0.0, [([x, 0.5, 6.0], 0) for x in (10.75, 10.0, 9.25, 8.5, 7.5, 6.75, 6.0, 5.25)]),
}
DIRECTION = {0: '-z', 90: '+x', 180: '+z'}


def scene_specs(prefix):
    """Arguments of preview.py --scene: the devices, then one plug per port, its colour in the order of the palette."""
    specs = ['%s@%g,%g' % (device, dx, dz) for device, (dx, dz, _) in DEVICES.items()]
    n = 0
    for device, (dx, dz, ports) in DEVICES.items():
        for mouth, angle in ports:
            colour = COLOURS[n % len(COLOURS)]
            n += 1
            specs.append('%srj45_plug_%s@%g,%g,%g,%d' % (prefix, colour, mouth[0] + dx - MOUTH[0], mouth[1] - MOUTH[1],
                                                          mouth[2] + dz - MOUTH[2], angle))
    specs.append('drafts/rj45_plug_cables')
    return specs


def build_cables():
    """The floor, and for each plug a cable stub out of the boot, bending down to the floor and going on 2 px."""
    s = cables.Scene('rj45_plug_cables')
    s.floor(-1, 0, 4, 1)
    n = 0
    for device, (dx, dz, ports) in DEVICES.items():
        for mouth, angle in ports:
            colour = COLOURS[n % len(COLOURS)]
            n += 1
            m = [mouth[0] + dx, mouth[1], mouth[2] + dz]
            axis, sign = cables.AXES[DIRECTION[angle]]
            end = list(m)
            end[axis] += sign * (OUT + BOOT)
            bend = list(end)
            bend[axis] += sign * cables.STUB
            floor = [bend[0], cables.FLOOR, bend[2]]
            away = list(floor)
            away[axis] += sign * 2.0
            cables.cable(s, 'cable_%d' % n, [end, bend, floor, away], colour, [])
    s.write()   # always under design/blockbench/drafts/: the cable is drawn by the code, never exported


if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--prefix', default='', help='folder and prefix of the projects, for example drafts/')
    p.add_argument('--force', action='store_true', help='overwrite projects never saved from Blockbench')
    p.add_argument('--scene', action='store_true', help='write the cable stubs of the preview scene, print its arguments')
    args = p.parse_args()
    if args.scene:
        build_cables()
        print(' '.join(scene_specs(args.prefix)))
    else:
        sheet = build()
        for colour in COLOURS:
            d.write_bbmodel(args.prefix + 'rj45_plug_' + colour, project(sheet, colour), force=args.force)
