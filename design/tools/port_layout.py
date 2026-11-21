"""Port layout of a device, for the Java code (PortLayouts) and for the device sheet (docs/cable-design.md, section 3).

The holes are read from the drawing of each device (the RJ45 ports its script draws on the texture sheet, turned into
model pixels by wwb_design.model_coords), so that a port moved on the drawing moves here too. The rest is the rule of the
aim zones: on the face that carries the ports, each port owns the strip of the face up to halfway to its neighbours,
the ports at the ends going to the edge of the face, over a band of the face; on a flat device the strip goes on over
the whole depth of the top face. A device with a single port gets the face of its port as its zone. The body is the
Java shape of the block (front facing north).

    python3 design/tools/port_layout.py <id>             # Markdown table for the device sheet
    python3 design/tools/port_layout.py <id> --java      # the ports as the Java records print them (PortLayout.Port)
    python3 design/tools/port_layout.py --all [--java]
    python3 design/tools/port_layout.py --zones-image docs/images/cable-aim-zones.png
"""
import desktop_switches
import internet_box
import laptop
import pc_tower
import wwb_design as d

RJ45 = 0.5


def _items(sheet, labels):
    """The 2 x 2 port drawings of a sheet, in the order of the labels (engine order)."""
    found = []
    for label in labels:
        match = [item for item in sheet.items if item[0] == label]
        if len(match) != 1:
            raise SystemExit('port %r drawn %d times' % (label, len(match)))
        found.append(match[0])
    return found


def _switch(device_id):
    spec = desktop_switches.SWITCHES[device_id]
    sheet = desktop_switches.build(spec)
    lo, hi = spec['envelope']
    labels = ['Port %d, hole 0.5 px deep' % (n + 1) for n in range(len(spec['ports']))]
    return {
        'body': (lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]),
        'elements': [{'name': 'env', 'from': lo, 'to': hi}],
        'items': _items(sheet, labels),
        'connectors': ['RJ45'] * len(labels),
        'band': (lo[1], hi[1]),
        'over_top': True,
    }


DEVICES = {
    'pc_tower': lambda: {
        'body': (4.5, 0.0, 0.5, 11.5, 15.0, 15.5),
        'elements': pc_tower.ELEMENTS,
        'items': _items(pc_tower.build('classic', 'low'), ['RJ45 port (network card, lowest slot)']),
        'connectors': ['RJ45'],
        'band': None,
        'over_top': False,
    },
    'laptop': lambda: {
        'body': (1.0, 0.0, 3.5, 15.0, 1.0, 12.5),
        'elements': [laptop.ENV],
        'items': _items(laptop.build(), ['RJ45 port, hole 0.5 px deep']),
        'connectors': ['RJ45'],
        'band': None,
        'over_top': False,
    },
    'internet_box': lambda: {
        # The shape starts at the glass band (z 6.5), 0.25 px in front of the panel the holes are cut in.
        'body': (4.5, 0.0, 6.5, 11.5, 7.0, 9.5),
        'elements': [internet_box.ENV],
        'items': _items(internet_box.build(), ['LAN %d port, hole 0.5 px deep' % n for n in range(1, 5)] + ['Line input']),
        'connectors': ['RJ45'] * 4 + ['LINE'],
        'band': (0.75, 3.25),
        # Zone of the line input: from the west edge of the back up to the power socket.
        'single_zone_x': {4: (4.5, 8.0)},
        'over_top': False,
    },
    'desktop_switch_5': lambda: _switch('desktop_switch_5'),
    'desktop_switch_8': lambda: _switch('desktop_switch_8'),
}

AXIS = {'north': (2, -1), 'south': (2, 1), 'west': (0, -1), 'east': (0, 1)}


def _plane(body, face):
    axis, sign = AXIS[face]
    return body[axis + 3] if sign > 0 else body[axis]


def layout(device_id):
    spec = DEVICES[device_id]()
    body = spec['body']
    by_name = {e['name']: e for e in spec['elements']}
    ports = []
    for index, (item, connector) in enumerate(zip(spec['items'], spec['connectors'])):
        label, element, face, col, row, w, h, desc = item
        a1, r1, _, ry = d.model_coords(by_name[element], face, col, row, w, h)
        el = by_name[element]
        axis, sign = AXIS[face]
        mouth_plane = el['to'][axis] if sign > 0 else el['from'][axis]
        depth = RJ45 if 'shadow tone' in desc else 0.0
        inner = mouth_plane - sign * depth
        lo3 = [0.0, ry[0], 0.0]
        hi3 = [0.0, ry[1], 0.0]
        across = 0 if a1 == 'x' else 2
        lo3[across], hi3[across] = r1
        lo3[axis], hi3[axis] = min(mouth_plane, inner), max(mouth_plane, inner)
        mouth = [(lo3[k] + hi3[k]) / 2 for k in range(3)]
        mouth[axis] = mouth_plane
        ports.append({'index': index, 'label': label, 'connector': connector, 'face': face,
                      'hole': lo3 + hi3, 'mouth': mouth, 'across': across})
    # Zones, face by face: halfway to the neighbours, the ends to the edges of the face.
    for face in set(p['face'] for p in ports):
        row = [p for p in ports if p['face'] == face and p['connector'] == 'RJ45']
        plane = _plane(body, face)
        axis, _ = AXIS[face]
        for p in [p for p in ports if p['face'] == face]:
            across = p['across']
            if p in row and len(row) > 1:
                order = sorted(row, key=lambda q: q['hole'][across])
                i = order.index(p)
                low = body[across] if i == 0 else (order[i - 1]['hole'][across + 3] + p['hole'][across]) / 2
                high = body[across + 3] if i == len(order) - 1 else (p['hole'][across + 3] + order[i + 1]['hole'][across]) / 2
                y = spec['band'] or (body[1], body[4])
            elif p['index'] in spec.get('single_zone_x', {}):
                low, high = spec['single_zone_x'][p['index']]
                y = spec['band']
            else:
                low, high = body[across], body[across + 3]
                y = (body[1], body[4])
            area = [0.0] * 6
            area[across], area[across + 3] = low, high
            area[1], area[4] = y
            area[axis], area[axis + 3] = plane, plane
            p['zone'] = [(face, area)]
            if spec['over_top'] and p in row:
                top = [low, body[4], body[2], high, body[4], body[5]]
                p['zone'].append(('up', top))
    return body, ports


def _num(value):
    return ('%.4f' % value).rstrip('0').rstrip('.') if value != int(value) else '%.1f' % value


def _box(values):
    return 'PxBox[minX=%s, minY=%s, minZ=%s, maxX=%s, maxY=%s, maxZ=%s]' % tuple(_num(v) for v in values)


def java(device_id):
    body, ports = layout(device_id)
    lines = ['## %s' % device_id, 'body: [%s]' % _box(body)]
    for p in ports:
        zone = ', '.join('Area[side=%s, box=%s]' % (side.upper(), _box(area)) for side, area in p['zone'])
        lines.append('Port[index=%d, connector=%s, face=%s, zone=[%s], hole=%s, mouth=PxPoint[x=%s, y=%s, z=%s]]' % (
            p['index'], p['connector'], p['face'].upper(), zone, _box(p['hole']), *(_num(v) for v in p['mouth'])))
    return '\n'.join(lines)


def table(device_id):
    _, ports = layout(device_id)
    rows = ['| Port | Engine index | Face | Connector | Hole (model px) | Mouth | Outward | Aim zone |',
            '|---|---|---|---|---|---|---|---|']
    for p in ports:
        h = p['hole']
        hole = 'x %g to %g, y %g to %g, z %g to %g' % (h[0], h[3], h[1], h[4], h[2], h[5])
        mouth = '%g, %g, %g' % tuple(p['mouth'])
        zone = '; '.join('%s: x %g to %g, y %g to %g, z %g to %g' % (side, a[0], a[3], a[1], a[4], a[2], a[5]) for side, a in p['zone'])
        rows.append('| %d | %d | %s | %s | %s | %s | %s | %s |' % (p['index'] + 1, p['index'], p['face'], p['connector'],
                                                             hole, mouth, p['face'], zone))
    return '\n'.join(rows)


# --- Image of the aim zones (docs/images/cable-aim-zones.png) ----------------------------------------------------

def zones(width, height, holes, rows, zone_rows, scale=16):
    """A face seen from the front, scale pixels per texel: the zone of each port tinted (two tints in turn) over the rows
    of zone_rows, a grey line between two zones, the holes in black. holes: (column, width) in texels from the viewer's
    left; rows: (row, height) of the holes. Copied from the draft of the cable design session
    (research/design-scratchpad/cable_mock.py, 2026-11-20), which drew the first version of the image."""
    img = [[(196, 200, 206, 255)] * (width * scale) for _ in range(height * scale)]
    tints = [(255, 214, 170), (170, 214, 255)]
    centres = [c + w / 2 for c, w in holes]
    for i, (c, w) in enumerate(holes):
        left = 0 if i == 0 else (centres[i - 1] + centres[i]) / 2
        right = width if i == len(holes) - 1 else (centres[i] + centres[i + 1]) / 2
        for y in range(zone_rows[0] * scale, zone_rows[1] * scale):
            for x in range(int(left * scale), int(right * scale)):
                img[y][x] = tints[i % 2] + (255,)
        for y in range(rows[0] * scale, (rows[0] + rows[1]) * scale):
            for x in range(c * scale, (c + w) * scale):
                img[y][x] = (28, 30, 34, 255)
    for i in range(1, len(holes)):
        x = int((centres[i - 1] + centres[i]) / 2 * scale)
        for y in range(zone_rows[0] * scale, zone_rows[1] * scale):
            img[y][x] = (90, 90, 90, 255)
    return img


def _texel(img, col, row, colour, scale):
    for y in range(row * scale, (row + 1) * scale):
        for x in range(col * scale, (col + 1) * scale):
            img[y][x] = colour


def zones_image(path):
    """The image of docs/cable-design.md, section 3, from the drawings of today: the front of the internet box (zones
    over the lower band of the front, LAN LEDs above the ports) on the left; on the right the 5-port switch, its top as
    seen from the front (port LEDs along the front edge, power LED at the left end) above its front, each zone running
    over both. The zones follow the rule of PortLayouts (halfway between two ports, to the edge at the ends)."""
    led = d.colour('led_green')
    lo, hi = internet_box.ENVELOPE
    top = hi[1]
    _, box_ports = layout('internet_box')
    band = box_ports[0]['zone'][0][1]
    zone_rows = (int(round((top - band[4]) * d.TEXELS_PER_PX)), int(round((top - band[1]) * d.TEXELS_PER_PX)))
    box = zones(int((hi[0] - lo[0]) * d.TEXELS_PER_PX), int((hi[1] - lo[1]) * d.TEXELS_PER_PX),
                [(c, 2) for c in internet_box.LAN_COLS], internet_box.PORT_ROWS, zone_rows)
    for c in internet_box.LAN_COLS:
        _texel(box, c, internet_box.PORT_ROWS[0] - 2, led, 16)

    spec = desktop_switches.SWITCHES['desktop_switch_5']
    slo, shi = spec['envelope']
    width = int((shi[0] - slo[0]) * d.TEXELS_PER_PX)
    depth = int((shi[2] - slo[2]) * d.TEXELS_PER_PX)
    height = int((shi[1] - slo[1]) * d.TEXELS_PER_PX)
    holes = [(c, 2) for c in spec['ports']]
    scale = 28
    upper = zones(width, depth, holes, (0, 0), (0, depth), scale)
    # The top seen from the front: its front edge at the bottom, next to the front face; LEDs on row 1 from that edge,
    # one above the right texel of each port, the power LED at the left end (desktop_switches.py).
    for c in spec['ports']:
        _texel(upper, c + 1, depth - 2, led, scale)
    _texel(upper, 0, depth - 2, led, scale)
    front = zones(width, height, holes, desktop_switches.PORT_ROWS, (0, height), scale)
    switch = upper + front
    for x in range(len(switch[0])):
        switch[len(upper)][x] = (120, 120, 120, 255)
        switch[len(upper) + 1][x] = (120, 120, 120, 255)

    h = max(len(box), len(switch))
    gap = 40
    canvas = [[(255, 255, 255, 255)] * (len(box[0]) + gap + len(switch[0])) for _ in range(h)]
    for y, row in enumerate(box):
        canvas[y][:len(row)] = row
    for y, row in enumerate(switch):
        canvas[y][len(box[0]) + gap:len(box[0]) + gap + len(row)] = row
    with open(path, 'wb') as out:
        out.write(d.encode_png(canvas))


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('device', nargs='?', choices=sorted(DEVICES))
    parser.add_argument('--all', action='store_true')
    parser.add_argument('--java', action='store_true', help='print the ports as the Java records print them')
    parser.add_argument('--zones-image', metavar='PNG', help='draw the image of the aim zones (docs/images/cable-aim-zones.png)')
    args = parser.parse_args()
    if args.zones_image:
        zones_image(args.zones_image)
        raise SystemExit(0)
    ids = sorted(DEVICES) if args.all or not args.device else [args.device]
    for device in ids:
        print(java(device) if args.java else '## %s\n\n%s\n' % (device, table(device)))
