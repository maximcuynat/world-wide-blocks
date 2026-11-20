"""Isometric preview of Blockbench projects, without Blockbench.

Usage: python3 design/tools/preview.py <out.png> <id> [<id> ...] [--px 24] [--scene] [--span N] [--centre X,Y,Z]
       [--views 45,225]

Draws each project of design/blockbench/<id>.bbmodel from the front (north and east faces) and from
the back (south and west faces), with the face shading of the game (top 1.0, north and south 0.8,
east and west 0.6, bottom 0.5), side by side. An element may turn around one axis, as in the Java
model format (the face keeps the shading of its direction before the rotation: a close enough view). An element with
"shade": false, as in the Java model format, is drawn without the face shading.

--scene draws all the projects together in one view, as if placed next to each other: <id>@<dx>,<dz>
moves a project by dx, dz model px (for example drafts/box@-9,0); <id>@<dx>,<dy>,<dz>,<angle> also moves it up
by dy and first turns it around the vertical axis through the centre of the block by 90, 180 or 270 degrees, as the
y of a blockstate (90: the north face turns to the east), for example a plug in a port that faces east. --span sets the width of the view
in model px, so that several images share the same scale. --centre sets the point at the centre of the view (model
px of the scene), for a close-up; --views picks the views (45: north and east faces, 225: south and west faces).
"""
import argparse
import base64
import json
import math
import os
import struct
import zlib

import wwb_design as d

SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}
BACKGROUND = (236, 238, 241, 255)


def decode_png(data):
    """RGBA 8-bit PNG (the format written by wwb_design) -> rows of RGBA tuples."""
    pos, idat = 8, b''
    while pos < len(data):
        n, = struct.unpack('>I', data[pos:pos + 4])
        kind, body = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + n]
        pos += 12 + n
        if kind == b'IHDR':
            w, h, depth, ctype = struct.unpack('>IIBB', body[:10])
            if depth != 8 or ctype != 6:
                raise ValueError('only 8-bit RGBA PNG is read')
        elif kind == b'IDAT':
            idat += body
    raw, stride, rows, prev, i = zlib.decompress(idat), w * 4, [], bytearray(w * 4), 0
    for _ in range(h):
        f = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - 4] if x >= 4 else 0
            b = prev[x]
            c = prev[x - 4] if x >= 4 else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append([tuple(line[k:k + 4]) for k in range(0, stride, 4)])
        prev = line
    return rows


def face_uv(face, p, lo, hi):
    """Fractions (fu, fv) of a point on a face, in the orientation of FaceBakery.defaultFaceUV."""
    (x1, y1, z1), (x2, y2, z2) = lo, hi
    x, y, z = p

    def fr(a, b, v):
        return (v - a) / (b - a) if b != a else 0.0
    if face == 'north':
        return fr(x2, x1, x), fr(y2, y1, y)
    if face == 'south':
        return fr(x1, x2, x), fr(y2, y1, y)
    if face == 'east':
        return fr(z2, z1, z), fr(y2, y1, y)
    if face == 'west':
        return fr(z1, z2, z), fr(y2, y1, y)
    if face == 'up':
        return fr(x1, x2, x), fr(z1, z2, z)
    return fr(x1, x2, x), fr(z2, z1, z)


def load(device_id):
    with open(os.path.join(d.BLOCKBENCH_DIR, device_id + '.bbmodel')) as f:
        project = json.load(f)
    tex = project['textures'][0]
    pixels = decode_png(base64.b64decode(tex['source'].split(',', 1)[1]))
    scale_u = len(pixels[0]) / tex.get('uv_width', project['resolution']['width'])
    scale_v = len(pixels) / tex.get('uv_height', project['resolution']['height'])
    boxes = []
    for e in project['elements']:
        turned = [(axis, angle) for axis, angle in zip(range(3), e.get('rotation', [0, 0, 0])) if angle]
        if len(turned) > 1:
            raise SystemExit('element %s turns on more than one axis: not supported by the preview' % e['name'])
        rotation = (turned[0][0], math.radians(turned[0][1]), e['origin']) if turned else None
        boxes.append((e['from'], e['to'], e['faces'], pixels, scale_u, scale_v, rotation, e.get('shade', True)))
    return boxes


def rotate(vector, rotation, inverse, is_direction):
    """A point (or a direction) turned by the rotation of an element around its axis through its origin
    (positive axis, as CuboidRotation.SingleAxisRotation), or by the opposite angle (inverse)."""
    axis, angle, origin = rotation
    if inverse:
        angle = -angle
    a, b = (axis + 1) % 3, (axis + 2) % 3
    v = list(vector)
    o = [0.0, 0.0, 0.0] if is_direction else origin
    u, w = v[a] - o[a], v[b] - o[b]
    v[a] = o[a] + u * math.cos(angle) - w * math.sin(angle)
    v[b] = o[b] + u * math.sin(angle) + w * math.cos(angle)
    return v


SIDES = ('north', 'east', 'south', 'west')


def load_placed(spec):
    """<id>, <id>@<dx>,<dz> or <id>@<dx>,<dy>,<dz>,<angle>: the elements of a project, moved (and turned)."""
    device_id, _, offset = spec.partition('@')
    values = [float(v) for v in offset.split(',')] if offset else [0.0, 0.0]
    if len(values) == 4:   # kept in the frame of the project, with a placement: (angle, offset)
        place = (values[3], values[:3])
        return [b + (place,) for b in load(device_id)]
    dx, dz = values
    placed = []
    for f, t, faces, px, su, sv, rotation, shade in load(device_id):
        if rotation:
            rotation = (rotation[0], rotation[1], [rotation[2][0] + dx, rotation[2][1], rotation[2][2] + dz])
        placed.append(([f[0] + dx, f[1], f[2] + dz], [t[0] + dx, t[1], t[2] + dz], faces, px, su, sv, rotation, shade, None))
    return placed


def place(vector, placement, inverse, is_direction):
    """A point (or a direction) of a project turned around the vertical axis through the centre of the block and
    moved (placement: angle, offset), or brought back into the frame of the project (inverse)."""
    angle, offset = placement
    a = math.radians(-angle if inverse else angle)
    c = [8.0, 0.0, 8.0]
    v = list(vector)
    if inverse and not is_direction:
        v = [v[i] - offset[i] for i in range(3)]
    u, w = (v[0], v[2]) if is_direction else (v[0] - c[0], v[2] - c[2])
    x, z = u * math.cos(a) - w * math.sin(a), u * math.sin(a) + w * math.cos(a)
    out = [x, v[1], z] if is_direction else [x + c[0], v[1], z + c[2]]
    if not inverse and not is_direction:
        out = [out[i] + offset[i] for i in range(3)]
    return out


def placed_face(face, placement):
    """Direction in the scene of a face of a turned project (for the shading)."""
    if not placement or face not in SIDES:
        return face
    return SIDES[(SIDES.index(face) + int(round(placement[0] / 90))) % 4]


TILE = 16   # screen pixels: each tile only tests the boxes whose outline on the screen touches it


def render(boxes, azimuth, px, span=None, centre=None):
    """One view. azimuth 45: camera at north-east (front and east faces); 225: south-west."""
    el, az = math.radians(30), math.radians(azimuth)
    cam = (math.cos(el) * math.sin(az), math.sin(el), -math.cos(el) * math.cos(az))
    dirn = tuple(-c for c in cam)
    right = (-dirn[2], 0.0, dirn[0])
    n = math.hypot(right[0], right[2])
    right = (right[0] / n, 0.0, right[2] / n)
    up = (right[1] * dirn[2] - right[2] * dirn[1], right[2] * dirn[0] - right[0] * dirn[2], right[0] * dirn[1] - right[1] * dirn[0])
    extents = []   # (from, to) of each box in the scene; a placed (turned) project by its turned corners
    for b in boxes:
        if b[8]:
            corners = [place((x, y, z), b[8], False, False) for x in (b[0][0], b[1][0]) for y in (b[0][1], b[1][1]) for z in (b[0][2], b[1][2])]
            extents.append(([min(c[i] for c in corners) for i in range(3)], [max(c[i] for c in corners) for i in range(3)]))
        else:
            extents.append((b[0], b[1]))
    lo = [min(e[0][i] for e in extents) for i in range(3)]
    hi = [max(e[1][i] for e in extents) for i in range(3)]
    centre = centre or [(lo[i] + hi[i]) / 2 for i in range(3)]
    span = span or max(hi[i] - lo[i] for i in range(3)) * 1.6 + 2
    size = int(span * px)
    img = [[BACKGROUND] * size for _ in range(size)]
    tiles = {}
    for box in boxes:   # outline of each box on the screen, from its corners (turned with the element, placed)
        (f, t), rotation, placement = box[:2], box[6], box[8]
        xs, ys = [], []
        for corner in ((x, y, z) for x in (f[0], t[0]) for y in (f[1], t[1]) for z in (f[2], t[2])):
            if rotation:
                corner = rotate(corner, rotation, False, False)
            if placement:
                corner = place(corner, placement, False, False)
            rel = [corner[i] - centre[i] for i in range(3)]
            xs.append((sum(rel[i] * right[i] for i in range(3)) + span / 2) * px)
            ys.append((span / 2 - sum(rel[i] * up[i] for i in range(3))) * px)
        for ty in range(max(0, int(min(ys) - 1) // TILE), min(size - 1, int(max(ys) + 1)) // TILE + 1):
            for tx in range(max(0, int(min(xs) - 1) // TILE), min(size - 1, int(max(xs) + 1)) // TILE + 1):
                tiles.setdefault((tx, ty), []).append(box)
    for sy in range(size):
        for sx in range(size):
            candidates = tiles.get((sx // TILE, sy // TILE))
            if not candidates:
                continue
            a, b = (sx + 0.5) / px - span / 2, span / 2 - (sy + 0.5) / px
            origin = [centre[i] + a * right[i] + b * up[i] - dirn[i] * 50 for i in range(3)]
            best = None
            for (f, t, faces, pixels, su, sv, rotation, shade, placement) in candidates:
                ray_o, ray_d = origin, dirn
                if placement:  # into the frame of the project
                    ray_o = place(ray_o, placement, True, False)
                    ray_d = place(ray_d, placement, True, True)
                if rotation:   # into the frame of the element, where it is an axis-aligned box
                    ray_o = rotate(ray_o, rotation, True, False)
                    ray_d = rotate(ray_d, rotation, True, True)
                tmin, tmax, hit = -1e9, 1e9, None
                for i, (neg, pos) in enumerate((('west', 'east'), ('down', 'up'), ('north', 'south'))):
                    if abs(ray_d[i]) < 1e-12:
                        if not f[i] <= ray_o[i] <= t[i]:
                            tmin = 1e9
                        continue
                    t1, t2 = (f[i] - ray_o[i]) / ray_d[i], (t[i] - ray_o[i]) / ray_d[i]
                    face = neg if ray_d[i] > 0 else pos
                    near = min(t1, t2)
                    if near > tmin:
                        tmin, hit = near, face
                    tmax = min(tmax, max(t1, t2))
                if hit and tmin <= tmax and (best is None or tmin < best[0]):
                    best = (tmin, hit, f, t, faces, pixels, su, sv, ray_o, ray_d, shade, placement)
            if best is None:
                continue
            tm, face, f, t, faces, pixels, su, sv, ray_o, ray_d, shade, placement = best
            fc = faces.get(face)
            if not fc or fc.get('texture') is None:
                continue
            p = [ray_o[i] + ray_d[i] * tm for i in range(3)]
            fu, fv = face_uv(face, p, f, t)
            u1, v1, u2, v2 = fc['uv']
            tu = int(min(max((u1 + fu * (u2 - u1)) * su, 0), len(pixels[0]) - 1e-6))
            tv = int(min(max((v1 + fv * (v2 - v1)) * sv, 0), len(pixels) - 1e-6))
            r, g, bl, al = pixels[tv][tu]
            if al == 0:
                continue
            k = SHADE[placed_face(face, placement)] if shade else 1.0
            img[sy][sx] = (int(r * k), int(g * k), int(bl * k), 255)
    return img


def main():
    p = argparse.ArgumentParser()
    p.add_argument('out')
    p.add_argument('ids', nargs='+')
    p.add_argument('--px', type=int, default=24, help='screen pixels per model pixel')
    p.add_argument('--scene', action='store_true', help='draw all the projects together in one view')
    p.add_argument('--span', type=float, help='width of the view in model px (same scale for all)')
    p.add_argument('--centre', help='point at the centre of the view, X,Y,Z in model px of the scene')
    p.add_argument('--views', default='45,225', help='views to draw: 45 (north and east faces), 225 (south and west)')
    args = p.parse_args()
    centre = [float(v) for v in args.centre.split(',')] if args.centre else None
    azimuths = [int(v) for v in args.views.split(',')]
    groups = [[b for spec in args.ids for b in load_placed(spec)]] if args.scene else [load_placed(s) for s in args.ids]
    views = []
    for boxes in groups:
        views += [render(boxes, azimuth, args.px, args.span, centre) for azimuth in azimuths]
    gap = 8
    h = max(len(v) for v in views)
    w = sum(len(v[0]) for v in views) + gap * (len(views) - 1)
    canvas = [[(255, 255, 255, 255)] * w for _ in range(h)]
    x0 = 0
    for v in views:
        for y, row in enumerate(v):
            canvas[y][x0:x0 + len(row)] = row
        x0 += len(v[0]) + gap
    with open(args.out, 'wb') as f:
        f.write(d.encode_png(canvas))
    print('written', args.out)


if __name__ == '__main__':
    main()
