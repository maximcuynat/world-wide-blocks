"""Cables, design drafts (docs/cable-design.md): the RJ45 plug, the thick cable on the ground, the highlight of the aimed
port, the cord colours and the cord icon. Writes drafts only, under design/blockbench/drafts/, except --export-icon.

    python3 design/tools/cables.py                  the drafts below
    python3 design/tools/cables.py --export-icon    the cord icon, in blue (the default cord colour, design guide,
                                                    section 4), to textures/item/ethernet_cable.png of the mod assets

- drafts/rj45_plug.bbmodel: the plug alone, for a port facing north, mouth at the centre of the block.
- drafts/cable_scene.bbmodel: plugs, cables, highlight and floor of the scene, in scene coordinates, to draw with the
  devices: preview.py out.png --scene pc_tower@-16,0 desktop_switch_5 internet_box@16,0 laptop@32,0 drafts/cable_scene
- drafts/cable_lanes.bbmodel: pairs of cables sharing a path, side by side at several lane steps.
- drafts/cord_colours.bbmodel: the same plug and cable in each cord colour.
- drafts/ethernet_cable.png: the cord icon, 16 x 16.

Decided by Max on 2026-11-20: one RJ45 size for every device (the hole of 2 x 2 texels, 0.5 px deep), the long boot,
the two-tone highlight, the icon with the taller plug. Sizes come from the holes of the device sheets, not from real plugs.
"""
import base64
import json
import os
import uuid

import wwb_design as d

with open(os.path.join(d.REPO, 'design', 'palette.json'), encoding='utf-8') as _f:
    CORDS = {name: tones for name, tones in json.load(_f)['cords'].items() if name != 'description'}

DRAFTS = os.path.join(d.BLOCKBENCH_DIR, 'drafts')
HOLE = 0.5             # RJ45 hole, the single size: 2 x 2 texels (design guide, section 5)
DEPTH = 0.5            # depth of the holes (device sheets)
BODY_OUT = 0.5         # clear body out of the hole (docs/cable-design.md, section 4)
BOOT = 0.75            # long boot, as wide as the body, in the colour of the cord
CABLE = 0.25           # square section of the cable
STUB = 0.25            # straight cable out of the boot before it bends
FLOOR = CABLE / 2      # centre line of a cable lying on the floor
LANE = 0.5             # proposed step between cables sharing a path: a gap of one cable width


def rgba(value):
    return (int(value[1:3], 16), int(value[3:5], 16), int(value[5:7], 16), 255)


def cord(name, tone='base'):
    return rgba(CORDS[name][tone])


# --- Scene texture: uniform swatches and a floor tile -------------------------------------------------------------

class Swatches:
    """64 x 64 texture: 4 x 4 swatches of one colour, and a floor tile of 32 x 32 texels for one block."""

    def __init__(self):
        self.pixels = [[(0, 0, 0, 0)] * 64 for _ in range(64)]
        self.cells = {}
        self.count = 0
        floor, line = (200, 197, 190, 255), (178, 174, 166, 255)   # neutral floor of the scene, not a mod colour
        for j in range(32):
            for i in range(32):
                self.pixels[32 + j][i] = line if i in (0, 31) or j in (0, 31) else floor
        self.cells['floor'] = [0, 32, 32, 64]

    def swatch(self, key, colour):
        if key not in self.cells:
            x, y = 4 * (self.count % 16), 4 * (self.count // 16)
            self.count += 1
            for j in range(4):
                for i in range(4):
                    self.pixels[y + j][x + i] = colour
            self.cells[key] = [x + 1, y + 1, x + 3, y + 3]
        return key


class Scene:
    """Elements of a draft project in scene coordinates, each face drawn with a cell of the swatches."""

    def __init__(self, name):
        self.name = name
        self.sw = Swatches()
        self.elements = []
        self.sw.swatch('body', d.colour('light', 'hi'))
        for colour in CORDS:
            self.sw.swatch('cord_' + colour, cord(colour))

    def box(self, name, lo, hi, cell, up_cell=None, shade=True):
        faces = {f: {'uv': list(self.sw.cells[cell]), 'texture': 0} for f in ('north', 'east', 'south', 'west', 'up', 'down')}
        if up_cell:
            faces['up']['uv'] = list(self.sw.cells[up_cell])
        e = {'name': name, 'box_uv': False, 'rescale': False, 'locked': False, 'from': [round(v, 4) for v in lo],
             'to': [round(v, 4) for v in hi], 'autouv': 0, 'color': 0, 'origin': [8, 8, 8], 'faces': faces,
             'type': 'cube', 'uuid': str(uuid.uuid5(uuid.NAMESPACE_URL, self.name + ':%d' % len(self.elements)))}
        if not shade:
            e['shade'] = False
        self.elements.append(e)

    def floor(self, x1, z1, x2, z2):
        for bx in range(x1, x2):
            for bz in range(z1, z2):
                self.box('floor_%d_%d' % (bx, bz), [bx * 16, -0.25, bz * 16], [bx * 16 + 16, 0, bz * 16 + 16], 'floor', up_cell='floor')

    def write(self):
        os.makedirs(DRAFTS, exist_ok=True)
        project = {
            'meta': {'format_version': '4.10', 'model_format': 'java_block', 'box_uv': False},
            'name': self.name, 'resolution': {'width': 64, 'height': 64}, 'elements': self.elements,
            'outliner': [e['uuid'] for e in self.elements],
            'textures': [{'name': self.name + '.png', 'id': '0', 'width': 64, 'height': 64, 'uv_width': 64,
                          'uv_height': 64, 'source': 'data:image/png;base64,'
                          + base64.b64encode(d.encode_png(self.sw.pixels)).decode()}]}
        with open(os.path.join(DRAFTS, self.name + '.bbmodel'), 'w') as f:
            json.dump(project, f)


# --- Geometry -----------------------------------------------------------------------------------------------------

AXES = {'+x': (0, 1), '-x': (0, -1), '+z': (2, 1), '-z': (2, -1)}


def along(mouth, direction, s0, s1, half_side, half_up):
    """Box from s0 to s1 out of a mouth along a horizontal direction, half_side wide sideways, half_up up and down."""
    axis, sign = AXES[direction]
    side = 2 - axis
    lo, hi = list(mouth), list(mouth)
    a, b = sorted((mouth[axis] + sign * s0, mouth[axis] + sign * s1))
    lo[axis], hi[axis] = a, b
    lo[side], hi[side] = mouth[side] - half_side, mouth[side] + half_side
    lo[1], hi[1] = mouth[1] - half_up, mouth[1] + half_up
    return lo, hi


def plug(scene, name, mouth, direction, colour):
    """Plug in a port (clear body, boot) and the cable stub; returns the end of the stub, where the cable bends."""
    scene.box(name + '_body', *along(mouth, direction, -DEPTH, BODY_OUT, HOLE / 2, HOLE / 2), 'body')
    scene.box(name + '_boot', *along(mouth, direction, BODY_OUT, BODY_OUT + BOOT, HOLE / 2, HOLE / 2), 'cord_' + colour)
    s = BODY_OUT + BOOT
    scene.box(name + '_stub', *along(mouth, direction, s, s + STUB, CABLE / 2, CABLE / 2), 'cord_' + colour)
    axis, sign = AXES[direction]
    end = list(mouth)
    end[axis] += sign * (s + STUB)
    return end


def cable(scene, name, points, colour, laid):
    """Cable along a chain of points (each step along one axis), square bends. A floor segment that crosses a floor
    segment of a cable laid before goes over it: 0.25 px higher over the crossing (docs/cable-design.md, section 5)."""
    h = CABLE / 2
    floor_segments = []
    for i, (p, q) in enumerate(zip(points, points[1:])):
        lo = [min(p[k], q[k]) - h for k in range(3)]
        hi = [max(p[k], q[k]) + h for k in range(3)]
        scene.box('%s_%d' % (name, i), lo, hi, 'cord_' + colour)
        if p[1] == q[1] == FLOOR:
            axis = 0 if p[0] != q[0] else 2
            floor_segments.append((axis, p, q))
            for other_axis, a, b in laid:
                if other_axis == axis:
                    continue
                c = a[axis]           # the other segment is at a fixed coordinate on our axis
                fixed = p[2 - axis]   # we are at a fixed coordinate on its axis
                if min(p[axis], q[axis]) < c < max(p[axis], q[axis]) and min(a[2 - axis], b[2 - axis]) < fixed < max(a[2 - axis], b[2 - axis]):
                    lo, hi = list(p), list(p)
                    lo[axis], hi[axis] = c - 3 * h, c + 3 * h
                    lo[2 - axis], hi[2 - axis] = fixed - h, fixed + h
                    lo[1], hi[1] = 0.0, 2 * CABLE
                    scene.box('%s_over_%d' % (name, len(scene.elements)), lo, hi, 'cord_' + colour)
    laid.extend(floor_segments)


def frame(scene, name, mouth, direction):
    """Two-tone frame around the aimed port, just in front of the face, drawn without shading like the block outline:
    a light line of 1/16 px against the hole, a dark one outside it, so that it shows on light and dark devices."""
    w, gap = 1 / 16, 1 / 64
    axis, sign = AXES[direction]
    side = 2 - axis
    for ring, colour in enumerate((d.colour('light', 'hi'), d.colour('socket'))):
        key = scene.sw.swatch('frame_%d' % ring, colour)
        inner = HOLE / 2 + ring * w
        outer = inner + w
        for k, (s_lo, s_hi, u_lo, u_hi) in enumerate((
                (-outer, outer, inner, outer), (-outer, outer, -outer, -inner),      # top and bottom bars
                (-outer, -inner, -inner, inner), (inner, outer, -inner, inner))):    # side bars
            lo, hi = list(mouth), list(mouth)
            a, b = sorted((mouth[axis] + sign * gap, mouth[axis] + sign * 2 * gap))
            lo[axis], hi[axis] = a, b
            lo[side], hi[side] = mouth[side] + s_lo, mouth[side] + s_hi
            lo[1], hi[1] = mouth[1] + u_lo, mouth[1] + u_hi
            scene.box('%s_%d_%d' % (name, ring, k), lo, hi, key, shade=False)


# --- Ports of the devices in the scene (device sheets), placed as in the preview command -------------------------

def ports():
    """Centre of each mouth and outward direction, from the device sheets, moved as in the preview command."""
    box_dx, laptop_dx, tower_dx = 16.0, 32.0, -16.0
    p = {}
    for n, x in enumerate((9.5, 8.5, 7.5, 6.5), start=1):                 # internet_box.md, LAN 1 to 4
        p['box_lan%d' % n] = ([box_dx + x, 1.75, 6.75], '-z')
    for n, x in enumerate((9.5, 8.75, 8.0, 7.25, 6.5), start=1):          # desktop_switch_5.md, ports 1 to 5
        p['switch_%d' % n] = ([x, 0.5, 6.5], '-z')
    p['tower'] = ([tower_dx + 8.75, 4.5, 15.5], '+z')                       # pc_tower.md, painted port at the back
    p['laptop'] = ([laptop_dx + 15.0, 0.5, 10.75], '+x')                    # laptop.md, left flank
    return p


def build_scene():
    s = Scene('cable_scene')
    s.floor(-1, -1, 4, 2)
    P = ports()
    laid = []

    def run(port_a, colour, middle, port_b=None):
        a = plug(s, port_a, *P[port_a], colour)
        points = [a, [a[0], FLOOR, a[2]]] + [[x, FLOOR, z] for x, z in middle]
        if port_b:
            b = plug(s, port_b, *P[port_b], colour)
            points += [[b[0], FLOOR, b[2]], b]
        cable(s, 'cable_' + port_a, points, colour, laid)

    run('box_lan1', 'blue', [(25.5, 4.0), (9.5, 4.0)], 'switch_1')
    run('switch_2', 'yellow', [(8.75, 3.0), (-14.0, 3.0), (-14.0, 17.0)], 'tower')
    run('switch_3', 'green', [(8.0, 2.0), (50.0, 2.0), (50.0, 10.75)], 'laptop')
    run('box_lan2', 'red', [(24.5, -14.0)])            # toward the rest of the network, out of the scene
    run('box_lan3', 'grey', [(23.5, -14.0)])
    run('switch_4', 'white', [(7.25, -14.0)])
    run('switch_5', 'black', [(6.5, -14.0)])
    frame(s, 'aim_box_lan4', *P['box_lan4'])             # free port, aimed
    s.write()


def build_lanes():
    """Three pairs of cables sharing a path (same route, one turn), lane steps of 0.25, 0.5 (proposed) and 0.75 px:
    the second cable of each pair is moved sideways by the step, and keeps the step through the turn."""
    s = Scene('cable_lanes')
    s.floor(0, 0, 2, 1)
    for k, step in enumerate((0.25, 0.5, 0.75)):
        z0 = 3.0 + 4.5 * k
        for i, colour in enumerate(('blue', 'yellow')):
            off = i * step
            x_turn = 14.0 - off                           # turning toward +z: the outer cable turns further, the inner one earlier
            cable(s, 'lane_%d_%d' % (k, i), [[4.0, FLOOR, z0 + off], [x_turn, FLOOR, z0 + off], [x_turn, FLOOR, z0 + 3.5]], colour, [])
    s.write()


def build_plug():
    s = Scene('rj45_plug')
    end = plug(s, 'plug', [8.0, 8.0, 8.0], '-z', 'blue')
    s.box('cable', [end[0] - CABLE / 2, end[1] - CABLE / 2, end[2] - 1.0], [end[0] + CABLE / 2, end[1] + CABLE / 2, end[2]], 'cord_blue')
    s.write()


def build_colours():
    s = Scene('cord_colours')
    s.sw.swatch('device', d.colour('light'))
    for i, name in enumerate(CORDS):
        x = 16.0 - 2.0 * i
        s.box('floor_%d' % i, [x - 1, -0.25, 0], [x + 1, 0, 6], 'floor', up_cell='floor')
        end = plug(s, name, [x, 1.375, 4.5], '-z', name)
        cable(s, 'cable_' + name, [end, [end[0], FLOOR, end[2]], [end[0], FLOOR, 0.5]], name, [])
        s.box('device_%d' % i, [x - 0.875, 0, 4.5], [x + 0.875, 2.25, 6.0], 'device')   # a light device around the port
    s.write()


# --- Cord icon (16 x 16): the coil of the current icon, its plug redrawn taller (body 4 rows, boot 2 rows) ---------

ICON = [
    '............WggW',
    '............WwwW',
    '............WwwW',
    '............WWWW',
    '............sbbs',
    '.............bs.',
    '....sshbss..bs..',
    '...sbhbbbbbs....',
    '..sbh....hss....',
    '..sh......bs....',
    '..hb......bb....',
    '..bb......bh....',
    '..sb......hs....',
    '..sbh....hbs....',
    '...hbbbbhbs.....',
    '....ssbhss......',
]


def icon_pixels(colour='blue'):
    key = {'.': (0, 0, 0, 0), 'b': cord(colour), 'h': cord(colour, 'hi'), 's': cord(colour, 'sh'),
           'W': d.colour('light', 'hi'), 'w': d.colour('light'), 'g': d.colour('contacts', 'hi')}
    assert len(ICON) == 16 and all(len(r) == 16 for r in ICON)
    return [[key[c] for c in r] for r in ICON]


def build_icon():
    os.makedirs(DRAFTS, exist_ok=True)
    with open(os.path.join(DRAFTS, 'ethernet_cable.png'), 'wb') as f:
        f.write(d.encode_png(icon_pixels()))


ICON_ASSET = os.path.join(d.REPO, 'src', 'main', 'resources', 'assets', 'worldwideblocks', 'textures', 'item',
                          'ethernet_cable.png')


def export_icon():
    """The cord icon chosen on 2026-11-20 (taller plug), in blue, the colour of every cord until colours come."""
    with open(ICON_ASSET, 'wb') as f:
        f.write(d.encode_png(icon_pixels('blue')))
    print('written', os.path.relpath(ICON_ASSET, d.REPO))


if __name__ == '__main__':
    import sys
    if sys.argv[1:] == ['--export-icon']:
        export_icon()
        sys.exit()
    build_scene()
    build_lanes()
    build_plug()
    build_colours()
    build_icon()
    print('drafts written under', os.path.relpath(DRAFTS, d.REPO))
