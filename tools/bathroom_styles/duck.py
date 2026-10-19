"""The deluxe rubber duck: stepped boxes for a round silhouette, one 128x128 texture
painted from 3D positions (sphere-shaded rubber, glossy eyes, blush).

It keeps the original duck's footing so RubberDuckRenderer's GROUND transform
places it identically: the bottom sits at y = 4, the body is centred on x = 8,
and the beak points north (-z). No ground transform is declared on purpose.
"""
from __future__ import annotations

from dataclasses import replace
from functools import lru_cache
import math

import numpy as np
from PIL import Image

from .geometry import Face, Model, UV_AXES, NORMAL, cull_hidden

NAME = 'rubber_duck_deluxe'
TEXTURES = {'0': f'showercore:block/{NAME}', 'particle': f'showercore:block/{NAME}'}
ITEM_DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, -1.5, 0], 'scale': [1.25, 1.25, 1.25]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, -1, 0], 'scale': [1.25, 1.25, 1.25]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 1, 1], 'scale': [0.8, 0.8, 0.8]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 1, 0], 'scale': [0.9, 0.9, 0.9]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 1, 0], 'scale': [0.9, 0.9, 0.9]},
}

TEX = 128         # texture size in texels
DENSITY = 3       # texels per model pixel
PART_OF = {}      # box name -> paint part


def _shape():
    m = Model()

    def b(f, t, part, name, **kw):
        m.box(f, t, '0', name=name, **kw)
        PART_OF[name] = part

    # Boat-shaped body: rounded keel, full hull, a chest bulge and a raised back.
    b((5.5, 4, 5.75), (10.5, 4.75, 11.75), 'body', 'keel')
    b((5, 4.75, 5), (11, 8, 12.5), 'body', 'hull')
    b((5.75, 5.25, 4.5), (10.25, 7.75, 5), 'body', 'chest')
    b((5.875, 8, 8.25), (10.125, 8.75, 12.25), 'body', 'back')
    # Stubby tail tilted up from the rump.
    b((6.75, 7.75, 11.5), (9.25, 9.25, 13), 'tail', 'tail', rot=(-22.5, 'x', (8, 8.5, 12)))
    b((7.25, 8, 12.75), (8.75, 9.25, 14), 'tail', 'tail_tip', rot=(-22.5, 'x', (8, 8.5, 12)))
    # Wings lying on the flanks, tips lifting backwards.
    for x0, x1, s in ((4.5, 5, 'l'), (11, 11.5, 'r')):
        b((x0, 5.75, 7.25), (x1, 7.75, 10.75), 'wing', f'wing_{s}')
        b((x0 + 0.05, 7.25, 9.5), (x1 - 0.05, 8.5, 11.25), 'wing', f'wing_tip_{s}')
    # Big head on the chest, with a rounded crown and a hair curl.
    b((5.5, 8.5, 4.75), (10.5, 13, 8.5), 'head', 'head')
    b((5.75, 8.625, 4.25), (10.25, 12.875, 9), 'head', 'face')
    b((6, 13, 4.75), (10, 13.75, 8.5), 'head', 'crown')
    b((7.75, 13.75, 6), (8.25, 14.5, 6.5), 'head', 'curl_stem')
    b((7.75, 14.25, 6.5), (8.25, 14.75, 7.25), 'head', 'curl_hook')
    # Wide smiling beak.
    b((6.25, 9.75, 2.75), (9.75, 10.75, 4.25), 'beak', 'beak')
    b((6.75, 9.25, 3.25), (9.25, 9.75, 4.25), 'beak_low', 'beak_low')
    return m


# --------------------------------------------------------------------------- UV layout

def _face_size(box, face):
    (ua, _), (va, _) = UV_AXES[face]
    return box.t[ua] - box.f[ua], box.t[va] - box.f[va]


@lru_cache(maxsize=1)
def layout():
    """Cull hidden faces, then shelf-pack every visible face into the texture."""
    boxes = cull_hidden(_shape().boxes)
    items = []
    for bi, box in enumerate(boxes):
        for face in box.faces:
            w, h = _face_size(box, face)
            items.append((max(1, math.ceil(w * DENSITY - 1e-6)), max(1, math.ceil(h * DENSITY - 1e-6)), bi, face))
    items.sort(key=lambda it: (-it[1], -it[0]))
    x = y = shelf = 0
    places = {}
    for w, h, bi, face in items:
        if x + w > TEX:
            x, y, shelf = 0, y + shelf + 1, 0
        places[(bi, face)] = (x, y, w, h)
        x += w + 1
        shelf = max(shelf, h)
    if y + shelf > TEX:
        raise ValueError('duck texture layout overflow')
    out = []
    k = 16 / TEX
    for bi, box in enumerate(boxes):
        faces = {}
        for face, spec in box.faces.items():
            px, py, w, h = places[(bi, face)]
            faces[face] = Face('0', 'fixed', (px * k, py * k, (px + w) * k, (py + h) * k))
        out.append(replace(box, faces=faces))
    return out, places


def build():
    m = Model()
    boxes, places = layout()
    for bi, box in enumerate(boxes):
        part = PART_OF[box.name]
        if part not in ('head', 'body', 'tail', 'wing'):
            m.extend([replace(box, faces={name: replace(spec, tint=2) for name, spec in box.faces.items()})])
            continue
        for face, spec in box.faces.items():
            px, py, w, h = places[(bi, face)]
            # Paint identifies eyes, highlights and blush; rubber receives a new finish.
            mask = []
            for j in range(h):
                row = []
                for i in range(w):
                    point = _texel_point(box, face, i, j, w, h)
                    painted = _color(part, point, face, box.name)
                    fixed = part == 'head' and (np.allclose(painted, EYE) or np.allclose(painted, SHINE)
                            or np.allclose(painted, SHINE2) or any(
                                ((point[0]-bx)/.7)**2 + ((point[1]-by)/.42)**2 + ((point[2]-bz)/.9)**2 <= 1
                                for bx, by, bz in BLUSH_AT))
                    row.append(2 if fixed else 1)
                mask.append(row)
            active = {}
            rectangles = []
            for j in range(h + 1):
                runs = []
                if j < h:
                    i = 0
                    while i < w:
                        end = i + 1
                        while end < w and mask[j][end] == mask[j][i]: end += 1
                        runs.append((i, end, mask[j][i])); i = end
                for run, start in list(active.items()):
                    if run not in runs: rectangles.append((*run, start, j)); del active[run]
                for run in runs: active.setdefault(run, j)
            (ua, ui), (va, vi) = UV_AXES[face]
            for i0, i1, tint, j0, j1 in rectangles:
                f, t = list(box.f), list(box.t)
                for axis, inc, lo, hi, total in ((ua,ui,i0,i1,w),(va,vi,j0,j1,h)):
                    length = box.t[axis]-box.f[axis]
                    f[axis] = box.f[axis] + (lo/total if inc else 1-hi/total)*length
                    t[axis] = box.f[axis] + (hi/total if inc else 1-lo/total)*length
                uv = ((px+i0)*16/TEX,(py+j0)*16/TEX,(px+i1)*16/TEX,(py+j1)*16/TEX)
                m.extend([replace(box, f=tuple(f), t=tuple(t), faces={face: replace(spec, uv=uv, tint=tint)}, occludes=False)])
    return m


# --------------------------------------------------------------------------- painting

def _hex(h):
    return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)], np.float32)


YELLOW = [_hex(c) for c in ('#d47f08', '#eb9a0e', '#f9b315', '#ffc523', '#ffd443', '#ffe274', '#fff1b8')]
ORANGE = [_hex(c) for c in ('#d2560c', '#ec6c12', '#fb861f', '#ff9d3a', '#ffb862', '#ffd296')]
EYE, SHINE, SHINE2 = _hex('#24170e'), _hex('#ffffff'), _hex('#8aa4c8')
BLUSH = _hex('#ff8a7a')
LIGHT = np.array([-0.4, 0.85, -0.35]) / np.linalg.norm([-0.4, 0.85, -0.35])

# Ellipsoids that the boxes approximate, used for round shading.
SPHERES = {
    'body': ((8, 6.4, 8.75), (3.0, 2.4, 3.8)),
    'tail': ((8, 8.6, 12.8), (1.5, 0.9, 1.4)),
    'wing': ((8, 6.9, 9.2), (3.5, 1.4, 2.0)),
    'head': ((8, 10.9, 6.6), (2.5, 2.6, 2.4)),
    'beak': ((8, 10.25, 3.5), (1.75, 0.5, 0.75)),
    'beak_low': ((8, 9.5, 3.75), (1.25, 0.25, 0.5)),
}
EYES = ((6.85, 11.55), (9.15, 11.55))
BLUSH_AT = ((5.7, 10.65, 4.6), (10.3, 10.65, 4.6))


def _pick(palette, t):
    """Smoothly interpolate along a dark-to-light palette."""
    t = min(max(t, 0.0), 1.0) * (len(palette) - 1)
    i = min(int(t), len(palette) - 2)
    return palette[i] * (1 - (t - i)) + palette[i + 1] * (t - i)


def _texel_point(box, face, i, j, w, h):
    (ua, ui), (va, vi) = UV_AXES[face]
    p = [0.0, 0.0, 0.0]
    n_axis, sign = NORMAL[face]
    p[n_axis] = box.t[n_axis] if sign > 0 else box.f[n_axis]
    fu, fv = (i + 0.5) / w, (j + 0.5) / h
    a0, a1 = box.f[ua], box.t[ua]
    p[ua] = a0 + fu * (a1 - a0) if ui else a1 - fu * (a1 - a0)
    b0, b1 = box.f[va], box.t[va]
    p[va] = b0 + fv * (b1 - b0) if vi else b1 - fv * (b1 - b0)
    return np.array(p)


def _color(part, p, face, box_name):
    c, r = SPHERES[part]
    n = (p - np.array(c)) / np.array(r) ** 2
    n = n / (np.linalg.norm(n) + 1e-9)
    lambert = float(np.dot(n, LIGHT))
    t = 0.6 + 0.22 * lambert
    # Gentle occlusion at the waterline and in the crease under the head.
    if part in ('body', 'wing', 'tail'):
        t -= 0.12 * max(0.0, 5.0 - p[1])
        if p[2] < 9.2 and p[1] > 7.4:
            t -= 0.1 * min(1.0, (p[1] - 7.4) / 0.6)
    if face == 'down':
        t -= 0.15
    palette = YELLOW if part in ('body', 'tail', 'wing', 'head') else ORANGE
    col = _pick(palette, t)
    if part == 'wing' and face in ('east', 'west'):
        # A soft feather edge along the lower rim of the wing.
        if p[1] < 6.0 or p[2] > 11.1:
            col = _pick(YELLOW, t - 0.16)
    if part == 'head':
        x, y, z = p
        if face == 'north' and z < 4.3:
            for ex, ey in EYES:
                dx, dy = (x - ex) / 0.62, (y - ey) / 0.85
                if dx * dx + dy * dy <= 1:
                    col = EYE
                    if (x - (ex - 0.2)) ** 2 + (y - (ey + 0.34)) ** 2 < 0.2 ** 2:
                        col = SHINE
                    elif (x - (ex + 0.22)) ** 2 + (y - (ey - 0.42)) ** 2 < 0.13 ** 2:
                        col = SHINE2
        for bx, by, bz in BLUSH_AT:
            d = ((x - bx) / 0.7) ** 2 + ((y - by) / 0.42) ** 2 + ((z - bz) / 0.9) ** 2
            if d <= 1:
                col = col * 0.3 + BLUSH * 0.7
    if part == 'beak' and face == 'up' and p[2] < 3.2 and abs(abs(p[0] - 8) - 0.6) < 0.2:
        col = _pick(ORANGE, t - 0.3)  # nostrils
    return col


def paint(path):
    boxes, places = layout()
    img = np.zeros((TEX, TEX, 4), np.float32)
    for bi, box in enumerate(boxes):
        part = PART_OF[box.name]
        for face in box.faces:
            px, py, w, h = places[(bi, face)]
            for j in range(h):
                for i in range(w):
                    p = _texel_point(box, face, i, j, w, h)
                    img[py + j, px + i, :3] = _color(part, p, face, box.name)
                    img[py + j, px + i, 3] = 255
    path.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA').save(path)
