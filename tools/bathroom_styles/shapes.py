"""Pixel-art plan shapes (rounded rectangles, discs, rings) decomposed into boxes."""
from __future__ import annotations

import math

import numpy as np


def rrect_mask(x0, z0, x1, z1, radius, res, bounds):
    """Cells of a `res`-pixel grid over `bounds` whose centres lie in a rounded rectangle."""
    bx0, bz0, bx1, bz1 = bounds
    nx, nz = int(round((bx1 - bx0) / res)), int(round((bz1 - bz0) / res))
    cx = bx0 + (np.arange(nx) + 0.5) * res
    cz = bz0 + (np.arange(nz) + 0.5) * res
    X, Z = np.meshgrid(cx, cz, indexing='ij')
    inside = (X > x0) & (X < x1) & (Z > z0) & (Z < z1)
    if radius > 0:
        r = radius
        qx = np.clip(X, x0 + r, x1 - r)
        qz = np.clip(Z, z0 + r, z1 - r)
        # A small allowance gives the classic pixel-circle silhouette instead of jagged notches.
        inside &= (X - qx) ** 2 + (Z - qz) ** 2 <= (r + 0.2 * res) ** 2
    return inside


def disc_mask(cx, cz, radius, res, bounds):
    bx0, bz0, bx1, bz1 = bounds
    nx, nz = int(round((bx1 - bx0) / res)), int(round((bz1 - bz0) / res))
    gx = bx0 + (np.arange(nx) + 0.5) * res
    gz = bz0 + (np.arange(nz) + 0.5) * res
    X, Z = np.meshgrid(gx, gz, indexing='ij')
    return (X - cx) ** 2 + (Z - cz) ** 2 <= (radius + 0.15 * res) ** 2


def greedy(mask):
    """Decompose a boolean grid into maximal-ish rectangles (i0, j0, i1, j1), exclusive ends."""
    mask = mask.copy()
    rects = []
    ni, nj = mask.shape
    for i in range(ni):
        j = 0
        while j < nj:
            if not mask[i, j]:
                j += 1
                continue
            j1 = j
            while j1 < nj and mask[i, j1]:
                j1 += 1
            i1 = i + 1
            while i1 < ni and mask[i1, j:j1].all():
                i1 += 1
            mask[i:i1, j:j1] = False
            rects.append((i, j, i1, j1))
            j = j1
    return rects


def mask_boxes(model, mask, y0, y1, tex, res, bounds, name='', **kwargs):
    bx0, bz0 = bounds[0], bounds[1]
    out = []
    for i0, j0, i1, j1 in greedy(mask):
        out.append(model.box((bx0 + i0 * res, y0, bz0 + j0 * res), (bx0 + i1 * res, y1, bz0 + j1 * res),
                             tex, name=name, **kwargs))
    return out


def rrect(model, y0, y1, rect, radius, tex, res=1.0, hole=None, bounds=(0, 0, 16, 32), name='', **kwargs):
    """A rounded-rectangle slab from y0 to y1, optionally minus a rounded-rectangle hole."""
    mask = rrect_mask(*rect, radius, res, bounds)
    if hole is not None:
        hrect, hradius = hole
        mask &= ~rrect_mask(*hrect, hradius, res, bounds)
    return mask_boxes(model, mask, y0, y1, tex, res, bounds, name, **kwargs)


def _grid(res, bounds):
    bx0, bz0, bx1, bz1 = bounds
    nx, nz = int(round((bx1 - bx0) / res)), int(round((bz1 - bz0) / res))
    gx = bx0 + (np.arange(nx) + 0.5) * res
    gz = bz0 + (np.arange(nz) + 0.5) * res
    return np.meshgrid(gx, gz, indexing='ij')


def perimeter_s(X, Z, x0, z0, x1, z1, r):
    """Clockwise arc length (seen from above) of the nearest point on a rounded rectangle."""
    qx = np.clip(X, x0 + r, x1 - r)
    qz = np.clip(Z, z0 + r, z1 - r)
    dx, dz = X - qx, Z - qz
    th = np.arctan2(dz, dx)
    lx, lz, q = x1 - x0 - 2 * r, z1 - z0 - 2 * r, r * math.pi / 2
    s = np.zeros_like(X)
    n, e, so, w = dz < 0, dx > 0, dz > 0, dx < 0
    flat_x, flat_z = dx == 0, dz == 0
    s = np.where(n & flat_x, X - (x0 + r), s)
    s = np.where(n & e, lx + r * (th + math.pi / 2), s)
    s = np.where(e & flat_z, lx + q + (Z - (z0 + r)), s)
    s = np.where(so & e, lx + q + lz + r * th, s)
    s = np.where(so & flat_x, lx + 2 * q + lz + ((x1 - r) - X), s)
    s = np.where(so & w, 2 * lx + 2 * q + lz + r * (th - math.pi / 2), s)
    s = np.where(w & flat_z, 2 * lx + 3 * q + lz + ((z1 - r) - Z), s)
    s = np.where(n & w, 2 * lx + 3 * q + 2 * lz + r * (th + math.pi), s)
    return s


def perimeter_ribs(model, y0, y1, rect, radius, depth, period, tex, res=0.5, bounds=(0, 0, 16, 32), name='rib',
                   skip_s=None, **kwargs):
    """Vertical ribs standing `depth` proud of a rounded rectangle, spaced along its perimeter."""
    x0, z0, x1, z1 = rect
    outer = rrect_mask(x0 - depth, z0 - depth, x1 + depth, z1 + depth, radius + depth, res, bounds)
    inner = rrect_mask(x0, z0, x1, z1, radius, res, bounds)
    X, Z = _grid(res, bounds)
    s = perimeter_s(X, Z, x0, z0, x1, z1, radius)
    band = outer & ~inner
    phase = np.floor(s / (period / 2)).astype(int) % 2 == 0
    mask = band & phase
    if skip_s is not None:
        mask &= ~skip_s(s)
    return mask_boxes(model, mask, y0, y1, tex, res, bounds, name, **kwargs)


def rotated_rect_mask(rectangles, res, bounds, origin):
    """Union planar ribs before extrusion, so their crossings have one surface."""
    X, Z = _grid(res, bounds)
    result = np.zeros(X.shape, dtype=bool)
    for (x0, z0, x1, z1), angle in rectangles:
        a = math.radians(angle)
        c, s = math.cos(a), math.sin(a)
        local_x = (X - origin[0]) * c + (Z - origin[1]) * s + origin[0]
        local_z = -(X - origin[0]) * s + (Z - origin[1]) * c + origin[1]
        result |= (local_x >= x0) & (local_x < x1) & (local_z >= z0) & (local_z < z1)
    return result
