"""Shared helpers to draw equipment for Blockbench, following docs/design-guide.md.

A device script (for example internet_box.py) builds a Sheet (the texture), lists its elements, and
calls write_bbmodel(). Standard library only (Python 3.9 or later).
"""
import base64
import json
import math
import os
import struct
import sys
import uuid
import zlib

TEXELS_PER_PX = 4  # design guide, section 3

# Design guide, section 4: role -> (base, highlight, shadow). None when the role has no such tone.
PALETTE = {
    'light': ('#c4c8ce', '#dde0e4', '#969aa2'),
    'dark': ('#3a3d42', '#4b4f56', '#2a2c30'),
    'metal': ('#7d838b', '#9aa0a8', '#555a61'),
    'socket': ('#1c1e22', None, '#0b0b0d'),
    'contacts': ('#b09646', '#c8aa3c', None),
    'led_green': ('#4ade80', None, None),
    'led_orange': ('#f0aa3c', None, None),
    'screen': ('#0d1b2a', None, None),
}
TONES = {'base': 0, 'hi': 1, 'sh': 2}

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
BLOCKBENCH_DIR = os.path.join(REPO, 'design', 'blockbench')


def colour(role, tone='base'):
    """RGBA tuple of a palette colour. Refuses any colour outside the palette."""
    value = PALETTE[role][TONES[tone]]
    if value is None:
        raise ValueError('%s has no %s tone in the palette' % (role, tone))
    return (int(value[1:3], 16), int(value[3:5], 16), int(value[5:7], 16), 255)


class Sheet:
    """Texture sheet. Faces are placed on it, then drawn with face-relative texel coordinates."""

    def __init__(self, size):
        self.size = size
        self.pixels = [[(0, 0, 0, 0)] * size for _ in range(size)]
        self.faces = {}   # (element, face) -> (x, y, w, h) in texels
        self.items = []   # (label, element, face, col, row, w, h, colour) for the device sheet

    def add_face(self, element, face, x, y, w_px, h_px):
        """Reserves the area of one face, w_px x h_px model px, at texel (x, y)."""
        w, h = int(round(w_px * TEXELS_PER_PX)), int(round(h_px * TEXELS_PER_PX))
        if x + w > self.size or y + h > self.size:
            raise ValueError('face %s/%s does not fit on the sheet' % (element, face))
        self.faces[(element, face)] = (x, y, w, h)

    def rect(self, element, face, col, row, w, h, role, tone='base'):
        fx, fy, fw, fh = self.faces[(element, face)]
        if col < 0 or row < 0 or col + w > fw or row + h > fh:
            raise ValueError('rectangle outside face %s/%s' % (element, face))
        c = colour(role, tone)
        for j in range(h):
            for i in range(w):
                self.pixels[fy + row + j][fx + col + i] = c

    def fill(self, element, face, role, tone='base'):
        _, _, w, h = self.faces[(element, face)]
        self.rect(element, face, 0, 0, w, h, role, tone)

    def side(self, element, face, role):
        """Side face: base colour, highlight line on top, shadow line at the bottom (guide, section 5)."""
        _, _, w, h = self.faces[(element, face)]
        self.fill(element, face, role)
        self.rect(element, face, 0, 0, w, 1, role, 'hi')
        self.rect(element, face, 0, h - 1, w, 1, role, 'sh')

    def rj45(self, label, element, face, col, row, large=True):
        """RJ45 port: 3 x 3 with a contacts row on top (large) or 2 x 2 in socket colour."""
        if large:
            self.rect(element, face, col, row, 3, 1, 'contacts')
            self.rect(element, face, col, row + 1, 3, 2, 'socket')
            self.items.append((label, element, face, col, row, 3, 3, 'contacts row on top, socket below'))
        else:
            self.rect(element, face, col, row, 2, 2, 'socket')
            self.items.append((label, element, face, col, row, 2, 2, 'socket'))

    def socket(self, label, element, face, col, row, w, h):
        self.rect(element, face, col, row, w, h, 'socket')
        self.items.append((label, element, face, col, row, w, h, 'socket'))

    def led(self, label, element, face, col, row, role='led_green'):
        self.rect(element, face, col, row, 1, 1, role)
        self.items.append((label, element, face, col, row, 1, 1, role.replace('led_', '')))

    def png(self, scale=1):
        return encode_png(self.pixels, scale)


def encode_png(pixels, scale=1):
    raw = b''
    for row in pixels:
        line = b''.join(bytes(px) * scale for px in row)
        raw += (b'\x00' + line) * scale

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    h, w = len(pixels) * scale, len(pixels[0]) * scale
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def display(w, d, h):
    """Display settings of the design guide, section 7, for a device of w x d x h model px."""
    cos30 = math.cos(math.radians(30))
    s = min(12 / ((w + d) / math.sqrt(2)), 12 / (h * cos30 + (w + d) / math.sqrt(2) * 0.5))
    s = math.floor(s * 100) / 100
    m = s / 0.625

    def ctx(rotation, translation, vanilla_scale):
        scale = s if vanilla_scale is None else round(vanilla_scale * m, 2)
        v = (8 - h / 2) * scale
        a = math.radians(rotation[0])
        return {'rotation': rotation,
                'translation': [0, round(translation[1] + v * math.cos(a), 2), round(translation[2] + v * math.sin(a), 2)],
                'scale': [scale] * 3}

    return {
        'gui': ctx([30, 225, 0], [0, 0, 0], None),
        'ground': ctx([0, 0, 0], [0, 3, 0], 0.25),
        'fixed': ctx([0, 0, 0], [0, 0, 0], 0.5),
        'thirdperson_righthand': ctx([75, 45, 0], [0, 2.5, 0], 0.375),
        'firstperson_righthand': ctx([0, 45, 0], [0, 0, 0], 0.40),
        'firstperson_lefthand': ctx([0, 225, 0], [0, 0, 0], 0.40),
    }


def model_coords(element, face, col, row, w, h):
    """Model px area of a texel rectangle of a face (face orientation checked in FaceBakery.defaultFaceUV)."""
    (x1, y1, z1), (x2, y2, z2) = element['from'], element['to']
    t = TEXELS_PER_PX
    u1, u2, v1, v2 = col / t, (col + w) / t, row / t, (row + h) / t
    if face in ('north', 'south', 'east', 'west'):
        y = (y2 - v2, y2 - v1)
        if face == 'north':
            return 'x', (x2 - u2, x2 - u1), 'y', y
        if face == 'south':
            return 'x', (x1 + u1, x1 + u2), 'y', y
        if face == 'east':
            return 'z', (z2 - u2, z2 - u1), 'y', y
        return 'z', (z1 + u1, z1 + u2), 'y', y
    if face == 'up':
        return 'x', (x1 + u1, x1 + u2), 'z', (z1 + v1, z1 + v2)
    return 'x', (x1 + u1, x1 + u2), 'z', (z2 - v2, z2 - v1)


def ports_table(sheet, elements):
    """Markdown table of the ports and LEDs, for the device sheet (design/blockbench/<id>.md)."""
    by_name = {e['name']: e for e in elements}
    rows = ['| Item | Face | Columns | Rows | Model area | Colour |', '|---|---|---|---|---|---|']
    for label, el, face, col, row, w, h, desc in sheet.items:
        a1, r1, a2, r2 = model_coords(by_name[el], face, col, row, w, h)
        cols = str(col) if w == 1 else '%d to %d' % (col, col + w - 1)
        rws = str(row) if h == 1 else '%d to %d' % (row, row + h - 1)
        area = '%s %g to %g, %s %g to %g' % (a1, r1[0], r1[1], a2, r2[0], r2[1])
        rows.append('| %s | %s | %s | %s | %s | %s |' % (label, face, cols, rws, area, desc))
    return '\n'.join(rows)


def bbmodel(device_id, elements, sheet, display_settings, parent='block/block'):
    """Blockbench project (format Java Block/Item) with the texture embedded. UUIDs are derived from
    the device id, so that the same script always writes the same file."""
    ns = uuid.uuid5(uuid.NAMESPACE_URL, 'worldwideblocks:' + device_id)
    out_elements = []
    for e in elements:
        faces = {}
        for face in ('north', 'east', 'south', 'west', 'up', 'down'):
            x, y, w, h = sheet.faces[(e['name'], face)]
            faces[face] = {'uv': [x, y, x + w, y + h], 'texture': 0}
            if face in e.get('cullfaces', ()):
                faces[face]['cullface'] = face
        out_elements.append({
            'name': e['name'], 'box_uv': False, 'rescale': False, 'locked': False,
            'from': e['from'], 'to': e['to'], 'autouv': 0, 'color': 0,
            'origin': e.get('origin', [8, 0, 8]), 'faces': faces, 'type': 'cube',
            'uuid': str(uuid.uuid5(ns, 'element:' + e['name'])),
        })
    return {
        'meta': {'format_version': '4.10', 'model_format': 'java_block', 'box_uv': False},
        'name': device_id,
        'parent': parent,
        'ambientocclusion': True,
        'front_gui_light': False,
        'visible_box': [1, 1, 0],
        'resolution': {'width': sheet.size, 'height': sheet.size},
        'elements': out_elements,
        'outliner': [e['uuid'] for e in out_elements],
        'textures': [{
            'path': '', 'name': device_id + '.png', 'folder': 'block', 'namespace': 'worldwideblocks',
            'id': '0', 'particle': True, 'render_mode': 'default', 'render_sides': 'auto',
            'width': sheet.size, 'height': sheet.size, 'uv_width': sheet.size, 'uv_height': sheet.size,
            'visible': True, 'internal': True, 'saved': False, 'uuid': str(uuid.uuid5(ns, 'texture')),
            'source': 'data:image/png;base64,' + base64.b64encode(sheet.png()).decode(),
        }],
        'display': display_settings,
    }


def write_bbmodel(device_id, project, preview_dir=None, force=False):
    """Writes design/blockbench/<id>.bbmodel. Refuses to overwrite an existing project unless --force:
    once Max has saved it from Blockbench, the project is the reference (design/tools/README.md)."""
    path = os.path.join(BLOCKBENCH_DIR, device_id + '.bbmodel')
    if os.path.exists(path) and not force:
        sys.exit('%s exists: it may have been saved from Blockbench and is then the reference. '
                 'Not overwritten (use --force only for a project never saved from Blockbench).' % path)
    os.makedirs(BLOCKBENCH_DIR, exist_ok=True)
    with open(path, 'w') as f:
        json.dump(project, f, indent=2)
        f.write('\n')
    print('written', os.path.relpath(path, REPO))
    if preview_dir:
        tex = base64.b64decode(project['textures'][0]['source'].split(',', 1)[1])
        with open(os.path.join(preview_dir, device_id + '.png'), 'wb') as f:
            f.write(tex)


def main_args():
    """Common command line of the device scripts: [--force] [--preview <dir>] [--table]."""
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--force', action='store_true', help='overwrite a project never saved from Blockbench')
    p.add_argument('--preview', metavar='DIR', help='also write the texture PNG into DIR')
    p.add_argument('--table', action='store_true', help='print the ports and LEDs table and the display values, write nothing')
    return p.parse_args()
