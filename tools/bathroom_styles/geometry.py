"""A small box-modelling toolkit that writes Blockbench-compatible block models.

Coordinates are Minecraft model pixels (16 per block). Faces default to
world-aligned UVs, so a material texture continues seamlessly across adjacent
boxes and across the bathtub's head/foot seam. Hidden faces between touching
boxes are removed on export; same-plane faces that would z-fight are reported.
"""
from __future__ import annotations

from dataclasses import dataclass, field, replace
import math

import numpy as np

FACES = ('north', 'south', 'east', 'west', 'up', 'down')
# face -> (normal axis, outward sign)
NORMAL = {'north': (2, -1), 'south': (2, 1), 'east': (0, 1), 'west': (0, -1), 'up': (1, 1), 'down': (1, -1)}
# face -> ((u axis, u increases with coordinate), (v axis, v increases with coordinate))
UV_AXES = {
    'north': ((0, False), (1, False)),
    'south': ((0, True), (1, False)),
    'east': ((2, False), (1, False)),
    'west': ((2, True), (1, False)),
    'up': ((0, True), (2, True)),
    'down': ((0, True), (2, False)),
}
EPS = 1e-6


@dataclass
class Face:
    tex: str
    mode: str = 'world'          # world | world90 | fixed
    uv: tuple | None = None      # fixed mode: (u1, v1, u2, v2)
    uvrot: int = 0
    emissive: bool = False
    tint: int | None = None


@dataclass
class Box:
    f: tuple
    t: tuple
    faces: dict
    rot: dict | None = None
    shade: bool = True
    name: str = ''
    occludes: bool = True

    def moved(self, dx=0.0, dy=0.0, dz=0.0):
        rot = None
        if self.rot:
            o = self.rot['origin']
            rot = {**self.rot, 'origin': (o[0] + dx, o[1] + dy, o[2] + dz)}
        return replace(self, f=(self.f[0] + dx, self.f[1] + dy, self.f[2] + dz),
                       t=(self.t[0] + dx, self.t[1] + dy, self.t[2] + dz), rot=rot,
                       faces=dict(self.faces))


def default_uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {
        'north': (16 - x1, 16 - y1, 16 - x0, 16 - y0),
        'south': (x0, 16 - y1, x1, 16 - y0),
        'east': (16 - z1, 16 - y1, 16 - z0, 16 - y0),
        'west': (z0, 16 - y1, z1, 16 - y0),
        'up': (x0, z0, x1, z1),
        'down': (x0, 16 - z1, x1, 16 - z0),
    }[face]


class Model:
    def __init__(self, textures=None):
        self.boxes: list[Box] = []
        self.textures = dict(textures or {})

    def box(self, f, t, tex, *, name='', only=None, skip=(), uv=None, fit=None, uvrot=0,
            world90=False, rot=None, shade=True, emissive=False, occludes=True, tint=None):
        """Add one box. `tex` is a texture variable or {face|'*': variable}."""
        f = tuple(float(v) for v in f)
        t = tuple(float(v) for v in t)
        if not all(a < b for a, b in zip(f, t)):
            raise ValueError(f'degenerate box {name}: {f} {t}')
        faces = {}
        for face in FACES:
            if (only is not None and face not in only) or face in skip:
                continue
            variable = tex.get(face, tex.get('*')) if isinstance(tex, dict) else tex
            if variable is None:
                continue
            explicit = (uv or {}).get(face) if isinstance(uv, dict) else None
            rotation = uvrot.get(face, 0) if isinstance(uvrot, dict) else uvrot
            light = face in emissive if isinstance(emissive, (set, tuple, list)) else bool(emissive)
            turned = face in world90 if isinstance(world90, (set, tuple, list)) else bool(world90)
            if explicit is not None:
                faces[face] = Face(variable, 'fixed', tuple(explicit), rotation, light, tint)
            elif fit is not None:
                faces[face] = Face(variable, 'fixed', tuple(fit), rotation, light, tint)
            else:
                faces[face] = Face(variable, 'world90' if turned else 'world', None, 0, light, tint)
        if rot is not None:
            angle = rot[0]
            if angle not in (-45, -22.5, 0, 22.5, 45):
                raise ValueError(f'unsupported rotation {angle} on {name}')
            rot = {'angle': angle, 'axis': rot[1], 'origin': tuple(float(v) for v in rot[2]),
                   **({'rescale': True} if len(rot) > 3 and rot[3] else {})}
        b = Box(f, t, faces, rot, shade, name, occludes and rot is None)
        self.boxes.append(b)
        return b

    def extend(self, boxes):
        self.boxes.extend(boxes)


# --------------------------------------------------------------------------- splitting

def _sub_uv(face, spec, box, axis, lo, hi):
    """Fixed UV of the part of `face` whose coordinate along `axis` is [lo, hi]."""
    (ua, ui), (va, vi) = UV_AXES[face]
    u1, v1, u2, v2 = spec.uv
    if spec.uvrot:
        raise ValueError(f'cannot split rotated fixed UV on {box.name}')
    a, b = box.f[axis], box.t[axis]

    def part(c1, c2, increasing):
        if increasing:
            return c1 + (c2 - c1) * (lo - a) / (b - a), c1 + (c2 - c1) * (hi - a) / (b - a)
        return c1 + (c2 - c1) * (b - hi) / (b - a), c1 + (c2 - c1) * (b - lo) / (b - a)

    if axis == ua:
        u1, u2 = part(u1, u2, ui)
    elif axis == va:
        v1, v2 = part(v1, v2, vi)
    return (u1, v1, u2, v2)


def split_box(box: Box, axis: int, c: float):
    if not (box.f[axis] + EPS < c < box.t[axis] - EPS):
        return [box]
    pieces = []
    for lo, hi, keep in ((box.f[axis], c, 'min'), (c, box.t[axis], 'max')):
        f = list(box.f); t = list(box.t)
        f[axis], t[axis] = lo, hi
        faces = {}
        for face, spec in box.faces.items():
            n_axis, sign = NORMAL[face]
            if n_axis == axis:
                if (sign < 0) == (keep == 'min'):
                    faces[face] = spec
                continue
            faces[face] = replace(spec, uv=_sub_uv(face, spec, box, axis, lo, hi)) if spec.mode == 'fixed' else spec
        pieces.append(replace(box, f=tuple(f), t=tuple(t), faces=faces))
    return pieces


def split_cells(boxes, axes=(0, 1, 2)):
    """Split boxes with world UVs so each lies within one 16-pixel cell per axis."""
    out = []
    for box in boxes:
        parts = [box]
        if any(spec.mode != 'fixed' for spec in box.faces.values()):
            for axis in axes:
                nxt = []
                for p in parts:
                    cuts = [k * 16 for k in range(math.floor(p.f[axis] / 16) + 1, math.ceil(p.t[axis] / 16))]
                    pieces = [p]
                    for c in cuts:
                        pieces = [q for piece in pieces for q in split_box(piece, axis, c)]
                    nxt.extend(pieces)
                parts = nxt
        out.extend(parts)
    return out


def slice_halves(boxes, z=16.0):
    """Split a north-facing two-block bathtub into head (z<16) and foot (z>=16, shifted)."""
    head, foot = [], []
    for box in boxes:
        for piece in split_box(box, 2, z):
            if piece.t[2] <= z + EPS:
                head.append(piece)
            else:
                foot.append(piece.moved(dz=-16))
    return head, foot


# --------------------------------------------------------------------------- culling

def _covered(rect, covers, step=0.125):
    (i0, i1), (j0, j1) = rect
    ni = max(1, int(round((i1 - i0) / step)))
    nj = max(1, int(round((j1 - j0) / step)))
    gi = i0 + (np.arange(ni) + 0.5) * (i1 - i0) / ni
    gj = j0 + (np.arange(nj) + 0.5) * (j1 - j0) / nj
    mask = np.zeros((ni, nj), bool)
    for (a0, a1), (b0, b1) in covers:
        mi = (gi > a0) & (gi < a1)
        mj = (gj > b0) & (gj < b1)
        mask |= np.outer(mi, mj)
        if mask.all():
            return True
    return bool(mask.all())


def cull_hidden(boxes):
    result = []
    for box in boxes:
        if box.rot is not None:
            result.append(box)
            continue
        faces = {}
        for face, spec in box.faces.items():
            k, sign = NORMAL[face]
            c = box.t[k] if sign > 0 else box.f[k]
            i, j = [a for a in range(3) if a != k]
            rect = ((box.f[i], box.t[i]), (box.f[j], box.t[j]))
            covers = []
            for other in boxes:
                if other is box or not other.occludes or other.rot is not None:
                    continue
                touching = (other.f[k] <= c + EPS and other.t[k] > c + EPS) if sign > 0 else \
                           (other.t[k] >= c - EPS and other.f[k] < c - EPS)
                if not touching:
                    continue
                a0, a1 = max(rect[0][0], other.f[i]), min(rect[0][1], other.t[i])
                b0, b1 = max(rect[1][0], other.f[j]), min(rect[1][1], other.t[j])
                if a1 - a0 > EPS and b1 - b0 > EPS:
                    covers.append(((a0, a1), (b0, b1)))
            if not covers or not _covered(rect, covers):
                faces[face] = spec
        if faces:
            result.append(replace(box, faces=faces))
    return result


def _buried(boxes, k, c, i, j, rect):
    """True when a same-plane overlap lies strictly inside some opaque box, so it is never seen."""
    for b in boxes:
        if b.occludes and b.rot is None and b.f[k] + EPS < c < b.t[k] - EPS and \
                b.f[i] <= rect[0] + EPS and b.t[i] >= rect[1] - EPS and \
                b.f[j] <= rect[2] + EPS and b.t[j] >= rect[3] - EPS:
            return True
    return False


def zfight_report(boxes):
    seen = []
    issues = []
    for box in boxes:
        if box.rot is not None:
            continue
        for face in box.faces:
            k, sign = NORMAL[face]
            c = box.t[k] if sign > 0 else box.f[k]
            i, j = [a for a in range(3) if a != k]
            rect = (box.f[i], box.t[i], box.f[j], box.t[j])
            for other, oface, oc, orect in seen:
                if oface == face and abs(oc - c) < EPS and other is not box:
                    overlap = (max(rect[0], orect[0]), min(rect[1], orect[1]),
                               max(rect[2], orect[2]), min(rect[3], orect[3]))
                    if overlap[1] - overlap[0] > EPS and overlap[3] - overlap[2] > EPS and \
                            not _buried(boxes, k, c, i, j, overlap):
                        issues.append(f'{box.name or box.f}/{other.name or other.f} {face}@{c}')
            seen.append((box, face, c, rect))
    return issues


# --------------------------------------------------------------------------- export

def _num(v):
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


def face_json(face, spec: Face, box: Box, light_key):
    if spec.mode == 'fixed':
        uv = spec.uv
        rotation = spec.uvrot
    else:
        offset = [math.floor((box.f[a] + EPS) / 16) * 16 for a in range(3)]
        f = [box.f[a] - offset[a] for a in range(3)]
        t = [box.t[a] - offset[a] for a in range(3)]
        uv = default_uv(face, f, t)
        rotation = 0
        if spec.mode == 'world90':
            uv = (uv[1], uv[0], uv[3], uv[2])
            rotation = 90
    out = {'uv': [_num(v) for v in uv], 'texture': '#' + spec.tex}
    if rotation:
        out['rotation'] = rotation
    if spec.tint is not None:
        out['tintindex'] = spec.tint
    if spec.emissive:
        out[light_key] = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}
    return out


def element_json(box: Box, light_key):
    el = {}
    if box.name:
        el['name'] = box.name
    el['from'] = [_num(v) for v in box.f]
    el['to'] = [_num(v) for v in box.t]
    if box.rot:
        el['rotation'] = {'angle': _num(box.rot['angle']), 'axis': box.rot['axis'],
                          'origin': [_num(v) for v in box.rot['origin']]}
        if box.rot.get('rescale'):
            el['rotation']['rescale'] = True
    if not box.shade or any(s.emissive for s in box.faces.values()):
        el['shade'] = False
    el['faces'] = {face: face_json(face, spec, box, light_key) for face, spec in box.faces.items()}
    return el


def check_bounds(boxes, label):
    for b in boxes:
        for v in b.f + b.t:
            if v < -16 - EPS or v > 32 + EPS:
                raise ValueError(f'{label}: element {b.name} outside [-16, 32]: {b.f} {b.t}')


def model_json(boxes, textures, light_key, credit, render_type='cutout', display=None, ambient=True):
    boxes = cull_hidden(split_cells(boxes))
    check_bounds(boxes, credit)
    data = {'credit': credit}
    if not ambient:
        data['ambientocclusion'] = False
    if render_type:
        data['render_type'] = render_type
    if textures:
        data['textures'] = dict(textures)
    data['elements'] = [element_json(b, light_key) for b in boxes]
    if display:
        data['display'] = display
    return data, zfight_report(boxes)
