"""Exports an approved Blockbench project to the mod assets.

Usage: python3 design/tools/export.py <id> [--out <dir>]

Reads design/blockbench/<id>.bbmodel (the reference once saved from Blockbench) and writes
models/block/<id>.json and textures/block/<id>.png under src/main/resources/assets/worldwideblocks/,
or under <dir> to look at the result first. Blockstate, item model, language keys and the Java shape
are not touched: check them by hand (docs/design-guide.md, section 9).
"""
import argparse
import base64
import json
import os
import sys

import wwb_design as d

FACES = ('north', 'east', 'south', 'west', 'up', 'down')
DISPLAY_KEYS = ('rotation', 'translation', 'scale')


def num(x):
    """8.0 -> 8, so that the exported model reads like a hand-written one."""
    x = round(x, 4)
    return int(x) if x == int(x) else x


def java_model(device_id, project):
    if project.get('meta', {}).get('model_format') != 'java_block':
        sys.exit('not a Java Block/Item project')
    textures = project['textures']
    if len(textures) != 1:
        sys.exit('expected exactly one texture, found %d' % len(textures))
    tex = textures[0]
    uv_w, uv_h = tex.get('uv_width', project['resolution']['width']), tex.get('uv_height', project['resolution']['height'])
    ref = 'worldwideblocks:block/' + device_id
    elements = []
    for e in project['elements']:
        if e.get('type', 'cube') != 'cube':
            sys.exit('element %s is not a cube' % e.get('name'))
        out = {'name': e['name'], 'from': e['from'], 'to': e['to']}
        rotation = e.get('rotation', [0, 0, 0])
        turned = [(axis, angle) for axis, angle in zip('xyz', rotation) if angle]
        if len(turned) > 1:
            sys.exit('element %s turns on more than one axis' % e['name'])
        if turned:
            out['rotation'] = {'angle': turned[0][1], 'axis': turned[0][0], 'origin': e.get('origin', [8, 8, 8])}
        faces = {}
        for name in FACES:
            face = e['faces'].get(name)
            if not face or face.get('texture') is None:
                continue
            u1, v1, u2, v2 = face['uv']
            f = {'uv': [num(u1 * 16 / uv_w), num(v1 * 16 / uv_h), num(u2 * 16 / uv_w), num(v2 * 16 / uv_h)],
                 'texture': '#texture'}
            for key in ('cullface', 'rotation', 'tintindex'):
                if face.get(key) not in (None, 0, ''):
                    f[key] = face[key]
            faces[name] = f
        out['faces'] = faces
        elements.append(out)
    parent = project.get('parent') or 'block/block'
    model = {
        'parent': parent if ':' in parent else 'minecraft:' + parent,
        'texture_size': [uv_w, uv_h],
        'textures': {'particle': ref, 'texture': ref},
        'elements': elements,
        'display': {ctx: {k: [num(x) for x in v[k]] for k in DISPLAY_KEYS if k in v} for ctx, v in project.get('display', {}).items()},
    }
    png = base64.b64decode(tex['source'].split(',', 1)[1])
    return model, png


def main():
    p = argparse.ArgumentParser()
    p.add_argument('id')
    p.add_argument('--out', metavar='DIR', help='write into DIR instead of the assets')
    args = p.parse_args()
    with open(os.path.join(d.BLOCKBENCH_DIR, args.id + '.bbmodel')) as f:
        project = json.load(f)
    model, png = java_model(args.id, project)
    base = args.out or os.path.join(d.REPO, 'src', 'main', 'resources', 'assets', 'worldwideblocks')
    for sub, name, data in (('models/block', args.id + '.json', None), ('textures/block', args.id + '.png', png)):
        folder = os.path.join(base, sub)
        os.makedirs(folder, exist_ok=True)
        path = os.path.join(folder, name)
        if data is None:
            with open(path, 'w') as f:
                json.dump(model, f, indent=2)
                f.write('\n')
        else:
            with open(path, 'wb') as f:
                f.write(data)
        print('written', os.path.relpath(path, d.REPO) if not args.out else path)


if __name__ == '__main__':
    main()
