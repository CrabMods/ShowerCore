"""Procedural 16x16 accent textures in a vanilla-like palette style.

Only details that vanilla lacks are painted here; materials stay vanilla so
resource packs keep working. Band and cane textures are uniform along their run
direction, so world-aligned UVs look identical at any height.
"""
from __future__ import annotations

import math
from pathlib import Path

import numpy as np
from PIL import Image


def hexrgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], np.float32)


def ramp(colors, t):
    """Pick from a dark-to-light palette by t in [0, 1] (nearest, vanilla style)."""
    pal = [hexrgb(c) for c in colors]
    t = np.clip(t, 0, 0.9999)
    idx = (t * len(pal)).astype(int)
    out = np.zeros(t.shape + (3,), np.float32)
    for i, c in enumerate(pal):
        out[idx == i] = c
    return out


def rng(seed):
    return np.random.default_rng(seed)


def grid(n=16):
    y, x = np.mgrid[0:n, 0:n].astype(np.float32)
    return x, y


def rgba(rgb, alpha=None):
    a = np.full(rgb.shape[:2] + (1,), 255, np.float32) if alpha is None else alpha[..., None]
    return np.concatenate([rgb, a], -1)


# --------------------------------------------------------------------------- metals

def copper_hoop():
    r = rng(11)
    x, y = grid()
    t = 0.55 + 0.12 * np.sin(x * 0.9) + r.normal(0, 0.06, (16, 16)).mean(0)[None, :] + r.normal(0, 0.035, (16, 16))
    rgb = ramp(['#5c2a1c', '#8a4129', '#b5603b', '#d4825a', '#eaa47c'], t)
    rgb[:, 7] = hexrgb('#6d3322')  # one rivet seam per block
    rgb[:, 8] = hexrgb('#f0b48e')
    return rgba(rgb)


def chrome():
    r = rng(5)
    x, y = grid()
    t = 0.62 + 0.25 * np.sin((x + y) * 0.42) + r.normal(0, 0.03, (16, 16))
    return rgba(ramp(['#6f7884', '#9aa3ad', '#c3cad1', '#e3e8ec', '#fbfdff'], t))


def cast_iron():
    r = rng(7)
    t = 0.45 + r.normal(0, 0.12, (16, 16))
    return rgba(ramp(['#1b1d22', '#25282e', '#30343b', '#3d424a', '#4d535c'], t))


def brass():
    r = rng(9)
    x, y = grid()
    t = 0.6 + 0.22 * np.sin(x * 0.55 + y * 0.2) + r.normal(0, 0.05, (16, 16))
    return rgba(ramp(['#6b4a12', '#9a6c1c', '#c99a2e', '#e8c158', '#fbe59a'], t))


# --------------------------------------------------------------------------- bamboo & rope

def bamboo_cane():
    r = rng(3)
    x, y = grid()
    # Columns: shadowed edges, a bright highlight stripe, fibre streaks; rows uniform.
    col = 0.5 + 0.35 * np.cos((x - 5.5) / 16 * 2 * math.pi) + r.normal(0, 0.05, 16)[None, :]
    t = np.broadcast_to(col, (16, 16)) + r.normal(0, 0.025, (16, 16))
    return rgba(ramp(['#2f4a12', '#3f6216', '#557e1d', '#6d9a27', '#8cb63a', '#a9cd55'], t))


def bamboo_node():
    r = rng(4)
    x, y = grid()
    col = 0.55 + 0.3 * np.cos((x - 5.5) / 16 * 2 * math.pi) + r.normal(0, 0.04, 16)[None, :]
    return rgba(ramp(['#5a5a1c', '#767626', '#93902f', '#b0aa45', '#cbc363'], np.broadcast_to(col, (16, 16))))


def bamboo_cane_end():
    x, y = grid()
    d = np.hypot(x - 7.5, y - 7.5)
    rgb = np.zeros((16, 16, 3), np.float32)
    rgb[:] = hexrgb('#4a6c1a')
    rgb[d < 6.6] = hexrgb('#e3d79a')
    rgb[d < 5.4] = hexrgb('#cbbd78')
    rgb[d < 4.2] = hexrgb('#2e2a1a')
    rgb[d < 2.6] = hexrgb('#1d1b12')
    return rgba(rgb)


def hemp_rope():
    x, y = grid()
    band = np.mod(x + y * 1.0, 4)
    t = np.where(band < 1, 0.2, np.where(band < 2, 0.5, np.where(band < 3, 0.8, 0.55)))
    return rgba(ramp(['#5d4325', '#7e5d35', '#a07b49', '#c1a06a', '#d8bd88'], t))


# --------------------------------------------------------------------------- pixel maps

def pixel_map(rows, palette):
    rgb = np.zeros((16, 16, 3), np.float32)
    alpha = np.full((16, 16), 255, np.float32)
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ' ':
                alpha[y, x] = 0
            else:
                rgb[y, x] = hexrgb(palette[ch])
    return rgba(rgb, alpha)


GOLD = {'0': '#5c3d0a', '1': '#7a5410', '2': '#a8761c', '3': '#d4a634', '4': '#f0cd5a', '5': '#fff0a0',
        'K': '#2b1c05', 'M': '#3d2507'}


def lion_face():
    return pixel_map([
        '3333444444443333',
        '3344555555554433',
        '3445544444455443',
        '3442222442222443',
        '3421KK4334KK1243',
        '3422114554112243',
        '3342245555422433',
        '2334455555544332',
        '2333441KK1443332',
        '2234443113444322',
        '1223441441443221',
        '1223442MM2443221',
        '1122342MM2432211',
        '0112234224322110',
        '0011223333221100',
        '0001122222211000',
    ], GOLD)


def lion_mane():
    # Vertical locks: each 2-texel strand is lit on its left and shaded on its right.
    r = rng(21)
    x, y = grid()
    lock = np.mod(x + np.floor(y / 5), 3)
    t = np.where(lock == 0, 0.78, np.where(lock == 1, 0.52, 0.28)) + r.normal(0, 0.04, (16, 16))
    return rgba(ramp(['#4a3108', '#6e4b0e', '#956a16', '#bf8d22', '#e0b23e'], t))


def starfish():
    pal = {'o': '#e8763a', 'O': '#ffa463', 'd': '#b4501f', 'w': '#ffe0b8'}
    return pixel_map([
        '       oo       ',
        '      oOOo      ',
        '      oOwo      ',
        '      dOOd      ',
        '     doOOod     ',
        'oooooOOwOOOooooo',
        ' dOOOOOOOOOOOd  ',
        '  ddOOwOOwOOd   ',
        '    dOOOOOOd    ',
        '    oOOOwOOo    ',
        '   oOOd  dOOo   ',
        '   oOd    dOo   ',
        '  oOd      dOo  ',
        '  oO        Oo  ',
        ' od          do ',
        ' o            o ',
    ], pal)


def spout_hole():
    x, y = grid()
    d = np.hypot(x - 7.5, y - 7.5)
    return rgba(ramp(['#120c05', '#21170a', '#3a2a12', '#5a4420'], d / 11))


# --------------------------------------------------------------------------- shell & pearl

def smooth_noise(seed, scale=4.0):
    r = rng(seed)
    coarse = r.random((6, 6))
    img = Image.fromarray((coarse * 255).astype(np.uint8)).resize((16, 16), Image.BICUBIC)
    return np.asarray(img).astype(np.float32) / 255


def shell_ivory():
    t = 0.5 + 0.13 * smooth_noise(35) + rng(36).normal(0, 0.025, (16, 16))
    return rgba(ramp(['#d5c6b8', '#dfd2c4', '#e9dfd2', '#f2e9dc', '#f9f1e6'], t))


def shell_rose():
    t = 0.5 + 0.12 * smooth_noise(37) + rng(38).normal(0, 0.025, (16, 16))
    return rgba(ramp(['#af847f', '#bd9690', '#cda79e', '#dcbbb0', '#ead0c2'], t))


def nacre():
    x, y = grid()
    t = 0.15 + 0.7 * smooth_noise(31) + 0.12 * np.sin((x - y) * 0.6) + rng(32).normal(0, 0.03, (16, 16))
    return rgba(ramp(['#ded7ce', '#e9e3d9', '#f3eee4', '#faf6ee', '#f3eef4', '#e5e2ed'], t))


def pearl():
    x, y = grid()
    d = np.hypot(x - 6.0, y - 5.5) / 10.5
    t = 1 - d + rng(33).normal(0, 0.02, (16, 16))
    return rgba(ramp(['#cbbfd2', '#ddd3e3', '#ece4ef', '#f7eff4', '#fdf8fb', '#ffffff'], t))


# --------------------------------------------------------------------------- spa

def led_cyan():
    t = 0.72 + rng(41).normal(0, 0.08, (16, 16))
    return rgba(ramp(['#26bfe0', '#4fdcf5', '#86f1ff', '#c4fbff'], t))


def spa_cushion():
    # Front rows 0..7 (soft roll, lighter at the top), top rows 8..11.
    x, y = grid()
    t = 0.55 + rng(42).normal(0, 0.03, (16, 16)) + np.where(y < 8, (4 - np.abs(y - 3)) * 0.04, 0.12)
    t = t - 0.22 * ((y == 7) | (y == 11)) - 0.1 * ((y == 0) | (y == 8))
    return rgba(ramp(['#5d646e', '#727a85', '#8a929c', '#a2a9b2', '#b8bec6'], t))


def _panel(running):
    pal = {'.': '#10141a', ',': '#1a2029', 'c': '#5fe8ff', 'C': '#c8fbff', 'g': '#5dff8a', 'G': '#d4ffe0',
           'd': '#2b4f59'}
    on = 'g' if running else 'c'
    hi = 'G' if running else 'C'
    rows = [
        '................',
        '.....' + on * 6 + '.....',
        '....' + on + hi * 6 + on + '....',
        '.....' + on * 6 + '.....',
        ',,,,,,,,,,,,,,,,',
        '.......cc.......',
        '.....cccccc.....',
        '.......cc.......',
        ',,,,,,,,,,,,,,,,',
        '................',
        '.....cccccc.....',
        '................',
        ',,,,,,,,,,,,,,,,',
        '....' + ('c' if running else 'd') + '..' + ('c' if running else 'd') + '..' + ('c' if running else 'd') + '....',
        '.....' + ('C' if running else 'd') + '..' + ('C' if running else 'd') + '......',
        '................',
    ]
    return pixel_map([r.ljust(16, '.')[:16] for r in rows], pal)


def spa_panel():
    return _panel(False)


def spa_panel_running():
    return _panel(True)


# --------------------------------------------------------------------------- shower plates

def _holes(pattern):
    x, y = grid()
    d = np.hypot(x - 7.5, y - 7.5)
    a = np.arctan2(y - 7.5, x - 7.5)
    if pattern == 'rings':
        holes = (d < 1.0)
        for radius, count in ((3.2, 6), (5.6, 12)):
            near = np.abs(d - radius) < 0.6
            holes |= near & (np.cos(a * count) > 0.55)
        return holes
    if pattern == 'grid':
        return (np.mod(x, 2) == 1) & (np.mod(y, 2) == 1) & (d < 7.2)
    if pattern == 'fine':
        return (np.mod(x + y, 3) == 0) & (np.mod(x, 3) == 1) & (d < 7.5)
    raise ValueError(pattern)


def spray(palette, hole_rgb, pattern, shade_center=True, seed=50, base=None):
    x, y = grid()
    d = np.hypot(x - 7.5, y - 7.5)
    t = (0.75 - d / 16 if shade_center else 0.55) + rng(seed).normal(0, 0.04, (16, 16))
    rgb = ramp(palette, t) if base is None else base
    holes = _holes(pattern)
    rgb[holes] = hexrgb(hole_rgb)
    return rgba(rgb)


def spray_gold():
    return spray(['#7a5410', '#a8761c', '#d4a634', '#f0cd5a', '#fff0a0'], '#3d2507', 'rings')


def spray_woven():
    x, y = grid()
    over = (np.mod(np.floor(x / 2) + np.floor(y / 2), 2) == 0)
    stripe = np.where(over, np.mod(y, 2), np.mod(x, 2))
    t = 0.35 + 0.35 * stripe + 0.15 * over + rng(51).normal(0, 0.04, (16, 16))
    base = ramp(['#6b5a2a', '#8f7a3b', '#b19a52', '#cdb86d', '#e1cf8c'], t)
    out = base.copy()
    holes = (np.mod(x, 4) == 3) & (np.mod(y, 4) == 3)
    out[holes] = hexrgb('#2c2312')
    return rgba(out)


def spray_shell():
    return spray(['#d9a3bb', '#e9bccd', '#f4d6de', '#fbe9ea'], '#7c4a5c', 'rings', seed=52)


def spray_rose():
    return spray(['#2a2d33', '#3c4048', '#525862', '#6a717c', '#878e98'], '#101114', 'grid', seed=53)


def spray_halo():
    return spray(['#12161c', '#1a2028', '#232b35', '#2e3844'], '#05070a', 'fine', shade_center=False, seed=54)


def volute():
    pal = {'.': '#ece6d8', ':': '#d9d1bf', 'x': '#9c9483', 'X': '#7a7364'}
    return pixel_map([
        '................',
        '.....xxxxxx.....',
        '...xx::::::xx...',
        '..x::......::x..',
        '..x:..xxxx..:x..',
        '.x:..x::::x..:x.',
        '.x:.x:.XX.:x.:x.',
        '.x:.x:.X..:x.:x.',
        '.x:.x:...:x..:x.',
        '.x:..x:::x...:x.',
        '.x:...xxx...:x..',
        '..x:.......:x...',
        '..x::.....::x...',
        '...xx:::::xx....',
        '.....xxxxx......',
        '................',
    ], pal)


def smoked_glass():
    x, y = grid()
    t = 0.35 + 0.12 * (y / 16) + 0.3 * (np.abs((x + y * 0.6) - 9) < 0.9) + rng(55).normal(0, 0.02, (16, 16))
    return rgba(ramp(['#090c10', '#0e1319', '#141b23', '#1d2731', '#2a3846'], t))


def shower_display():
    pal = {'.': '#0b1016', 'c': '#62ecff', 'C': '#cffcff', 'd': '#1f4a55', 'o': '#ffb35c'}
    return pixel_map([
        '................',
        '................',
        '.ccc.ccc..cc....',
        '...c.c.c.c..c...',
        '.ccc.ccc..cc....',
        '...c.c.c........',
        '.ccc.ccc........',
        '................',
        '.oo.ooooodddddd.',
        '................',
        '.C..c..c..c..c..',
        '................',
        '................',
        '................',
        '................',
        '................',
    ], pal)


def shower_buttons():
    pal = {'.': '#0b1016', 'c': '#62ecff', 'd': '#2a6070'}
    return pixel_map([
        '................',
        '..d....ccc....d.',
        '.ddd..c.c.c..ddd',
        '..d...c...c...d.',
        '.......ccc......',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
    ], pal)


def jet_face():
    x, y = grid()
    d = np.hypot(x - 7.5, y - 7.5)
    t = np.where(d < 6, 0.75 - d / 20, 0.3) + rng(56).normal(0, 0.03, (16, 16))
    rgb = ramp(['#4c5560', '#77818c', '#a3acb5', '#ccd3d9', '#eef2f5'], t)
    for cx, cy in ((5, 5), (10, 5), (5, 10), (10, 10), (7.5, 7.5)):
        rgb[np.hypot(x - cx, y - cy) < 1.3] = hexrgb('#1a1e24')
    return rgba(rgb)


def bucket_water():
    x, y = grid()
    d = np.maximum(np.abs(x - 7.5), np.abs(y - 7.5))
    t = 0.5 + 0.25 * np.sin(x * 0.8 + y * 0.5) * 0.5 + rng(57).normal(0, 0.05, (16, 16))
    rgb = ramp(['#1f4f9e', '#2c63b8', '#3a78d0', '#5a95e2', '#86b6f0'], t)
    rgb[d > 6.5] = hexrgb('#5b6168')
    rgb[d > 7.2] = hexrgb('#8a9099')
    return rgba(rgb)


TEXTURES = {
    'spray_gold': spray_gold,
    'spray_woven': spray_woven,
    'spray_shell': spray_shell,
    'spray_rose': spray_rose,
    'spray_halo': spray_halo,
    'volute': volute,
    'smoked_glass': smoked_glass,
    'shower_display': shower_display,
    'shower_buttons': shower_buttons,
    'jet_face': jet_face,
    'bucket_water': bucket_water,
    'lion_face': lion_face,
    'lion_mane': lion_mane,
    'starfish': starfish,
    'spout_hole': spout_hole,
    'shell_ivory': shell_ivory,
    'shell_rose': shell_rose,
    'nacre': nacre,
    'pearl': pearl,
    'led_cyan': led_cyan,
    'spa_cushion': spa_cushion,
    'spa_panel': spa_panel,
    'spa_panel_running': spa_panel_running,
    'copper_hoop': copper_hoop,
    'chrome': chrome,
    'cast_iron': cast_iron,
    'brass': brass,
    'bamboo_cane': bamboo_cane,
    'bamboo_node': bamboo_node,
    'bamboo_cane_end': bamboo_cane_end,
    'hemp_rope': hemp_rope,
}


def write_all(out_dir: Path, only=None):
    out_dir.mkdir(parents=True, exist_ok=True)
    written = []
    for name, paint in TEXTURES.items():
        if only and name not in only:
            continue
        arr = paint()
        img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA')
        img.save(out_dir / f'{name}.png')
        written.append(name)
    return written
