"""Five shower heads, one per bathroom set. Each is authored north-facing with its
mounting end on the back edge (z = 16), as BathroomGeometry.Shower expects; the
profile below lists the values a registration needs.
"""
from __future__ import annotations

from dataclasses import dataclass

from .geometry import Model
from .shapes import disc_mask, greedy, rrect_mask, mask_boxes, rotated_rect_mask

T = 'showercore:block/bathroom/'

SHOWER_BLOCK_DISPLAY = {
    'thirdperson_righthand': {'translation': [1, 1.75, -3.25]},
    'thirdperson_lefthand': {'translation': [1, 1.75, -3.25]},
    'firstperson_righthand': {'rotation': [-26, 0, 0]},
    'firstperson_lefthand': {'rotation': [-26, 0, 0]},
    'gui': {'rotation': [0, 144, 0], 'translation': [0, -4.25, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'translation': [0, -5.25, -2], 'scale': [0.7, 0.7, 0.7]},
}
SHOWER_ITEM_DISPLAY = {
    'thirdperson': {'rotation': [10, -45, 170], 'translation': [0, 9.5, -2.75], 'scale': [0.375, 0.375, 0.375]},
    'gui': {'rotation': [30, 225, 0], 'translation': [2, 3.0, 0], 'scale': [0.5, 0.5, 0.5]},
}


@dataclass
class ShowerProfile:
    mount_y: int          # pipe end on the back edge (BathroomGeometry.Shower.mountY)
    top_y: int
    outlet: tuple         # (x, y, z) centre of the spray plate
    width: float
    depth: float

    def bounds(self, model):
        """Exact outline of the rendered geometry, rotated elements included."""
        import itertools
        import math
        import numpy as np
        points = []
        for b in model.boxes:
            corners = np.array(list(itertools.product(*zip(b.f, b.t))), float)
            if b.rot:
                a = math.radians(b.rot['angle'])
                c, s = math.cos(a), math.sin(a)
                m = {'x': [[1, 0, 0], [0, c, -s], [0, s, c]], 'y': [[c, 0, s], [0, 1, 0], [-s, 0, c]],
                     'z': [[c, -s, 0], [s, c, 0], [0, 0, 1]]}[b.rot['axis']]
                scale = np.ones(3)
                if b.rot.get('rescale'):
                    scale[[i for i, ax in enumerate('xyz') if ax != b.rot['axis']]] = 1 / c
                o = np.array(b.rot['origin'])
                corners = (corners - o) @ np.array(m).T * scale + o
            points.append(corners)
        p = np.concatenate(points)
        return tuple(round(float(v), 3) for v in (*p.min(0), *p.max(0)))

    def validate(self, model):
        """The same checks as BathroomGeometry.Shower, plus a flush back edge."""
        b = self.bounds(model)
        x, y, z = self.outlet
        assert -16 <= self.mount_y <= 0 < self.top_y <= 16, self
        assert 0 < y <= self.top_y and 0 <= x - self.width / 2 and x + self.width / 2 <= 16, self
        assert 0 <= z - self.depth / 2 and z + self.depth / 2 <= 16, self
        assert abs(b[4] - self.top_y) < 1e-6, f'outline top {b[4]} must equal top_y {self.top_y}'
        assert b[1] <= self.mount_y and b[5] <= 16.01, f'outline {b} must stay in front of the wall'
        return b


@dataclass
class ShowerStyle:
    key: str
    title: str
    title_zh: str
    concept: str
    default_material: str
    textures: dict
    profile: ShowerProfile
    build: callable
    render_type: str = 'cutout'
    trim_from_roster: bool = False
    material_slots: tuple = ('body', 'trim')


def disc(m, cx, cz, r, y0, y1, tex, *, res=0.5, down=None, up=None, hole=0.0, name='disc', **kw):
    """A horizontal pixel disc; `down`/`up` stretch one image across the whole disc."""
    import math
    pad = res * 2
    bounds = (math.floor((cx - r - pad) / res) * res, math.floor((cz - r - pad) / res) * res,
              math.ceil((cx + r + pad) / res) * res, math.ceil((cz + r + pad) / res) * res)
    mask = disc_mask(cx, cz, r, res, bounds)
    if hole:
        mask &= ~disc_mask(cx, cz, hole, res, bounds)
    span = 2 * (r + 0.15 * res)
    u = lambda x: (x - (cx - span / 2)) / span * 16
    v = lambda z: (z - (cz - span / 2)) / span * 16
    out = []
    for i0, j0, i1, j1 in greedy(mask):
        x0, z0 = bounds[0] + i0 * res, bounds[1] + j0 * res
        x1, z1 = bounds[0] + i1 * res, bounds[1] + j1 * res
        texmap, uv = {'*': tex}, {}
        if down:
            texmap['down'] = down
            uv['down'] = (u(x0), v(z1), u(x1), v(z0))
        if up:
            texmap['up'] = up
            uv['up'] = (u(x0), v(z0), u(x1), v(z1))
        out.append(m.box((x0, y0, z0), (x1, y1, z1), texmap, uv=uv, name=name, **kw))
    return out


def cane(m, x0, z0, size, y0, y1, nodes=(), name='cane', end_top=True, end_bottom=False):
    """A vertical bamboo cane with proud nodes."""
    tex = {'*': 'cane'}
    uv = {}
    if end_top:
        tex['up'] = 'cane_end'
        uv['up'] = (0, 0, 16, 16)
    if end_bottom:
        tex['down'] = 'cane_end'
        uv['down'] = (0, 0, 16, 16)
    m.box((x0, y0, z0), (x0 + size, y1, z0 + size), tex, uv=uv, name=name)
    for y in nodes:
        m.box((x0 - 0.25, y, z0 - 0.25), (x0 + size + 0.25, y + 0.75, min(16, z0 + size + 0.25)), 'cane_node',
              name=name + '_node', skip=('south',) if z0 + size >= 16 else ())


# --------------------------------------------------------------------------- bamboo

def bamboo():
    m = Model()
    cane(m, 7, 14, 2, -16, 14, nodes=(-10, -3, 4))
    m.box((6.75, 11, 13.75), (9.25, 13.75, 16), 'rope', name='binding', skip=('south',))
    # Horizontal cane arm with a node, reaching over the bather.
    m.box((7.25, 12.25, 6.25), (8.75, 13.75, 13.75), {'*': 'cane', 'north': 'cane_end'}, name='arm',
          world90={'east', 'west', 'up', 'down'}, uv={'north': (0, 0, 16, 16)})
    m.box((7, 12, 9.5), (9, 14, 10.25), 'cane_node', name='arm_node')
    # Woven bamboo basket head hanging from the arm end.
    m.box((7.5, 11.25, 6.5), (8.5, 12.25, 7.5), 'rope', name='hanger')
    disc(m, 8, 7, 3.5, 10, 11.25, 'body', down='spray', name='basket')
    disc(m, 8, 7, 3.75, 11.25, 11.75, 'body', hole=2.75, name='basket_rim')
    # A wooden spigot valve on the cane, like a sake-barrel tap.
    m.box((7.25, -4, 12.5), (8.75, -1.5, 14), 'body', name='spigot')
    m.box((7.6, -3.25, 11.5), (8.4, -2.5, 12.5), 'body', name='spigot_nose')
    m.box((6.75, -1.5, 12.9), (9.25, -1, 13.6), 'trim', name='spigot_handle')
    m.box((6, -1.5, 12.9), (6.75, -1, 13.6), 'hot', name='hot')
    m.box((9.25, -1.5, 12.9), (10, -1, 13.6), 'cold', name='cold')
    return m


BAMBOO = ShowerStyle(
    material_slots=('body','trim','cane','cane_end','cane_node'),
    key='bamboo', title='Bamboo Kakei Shower', title_zh='竹筧淋浴',
    concept='A green bamboo cane with proud nodes and a hemp binding, a cane arm and a woven basket '
            'rain head; a wooden sake-barrel spigot works as the valve.',
    default_material='bamboo',
    textures={'body': 'minecraft:block/bamboo_planks', 'trim': 'minecraft:block/stripped_bamboo_block',
              'cane': T + 'bamboo_cane', 'cane_end': T + 'bamboo_cane_end', 'cane_node': T + 'bamboo_node',
              'rope': T + 'hemp_rope', 'spray': T + 'spray_woven',
              'hot': 'minecraft:block/red_concrete', 'cold': 'minecraft:block/blue_concrete'},
    profile=ShowerProfile(mount_y=-16, top_y=14, outlet=(8, 10, 7), width=6, depth=6),
    build=bamboo,
    trim_from_roster=True,
)


# --------------------------------------------------------------------------- column

def column():
    m = Model()
    # Attic base.
    m.box((5.5, -16, 12), (10.5, -15, 16), 'trim', name='plinth')
    m.box((6, -15, 12.5), (10, -14.25, 16), 'trim', name='torus')
    # Fluted shaft.
    m.box((6.75, -14.25, 13.25), (9.25, 8, 16), 'body', name='shaft')
    for x in (7.0, 7.75, 8.5):
        m.box((x, -14.25, 13), (x + 0.5, 8, 13.25), 'body', name='flute')
    for z in (13.75, 14.75):
        m.box((6.5, -14.25, z), (6.75, 8, z + 0.5), 'body', name='flute')
        m.box((9.25, -14.25, z), (9.5, 8, z + 0.5), 'body', name='flute')
    # Ionic capital with volutes and a gold necking.
    m.box((6.4, 7.5, 12.9), (9.6, 8, 15.9), 'gold', name='necking')
    m.box((6, 8, 12.5), (10, 9, 16), 'trim', name='echinus')
    m.box((5, 9, 12), (11, 10, 16), 'trim', name='abacus')
    for x0, x1, face in ((4.25, 5, 'west'), (11, 11.75, 'east')):
        m.box((x0, 7.75, 12.25), (x1, 10, 14.5), {'*': 'trim', face: 'volute'}, name='volute',
              uv={face: (0, 0, 16, 16)})
    # Gold gooseneck with a mitred elbow, and the sunflower head.
    m.box((7.5, 10, 14), (8.5, 13.5, 15), 'gold', name='riser')
    m.box((7.5, 13.1, 13.4), (8.5, 14.6, 14.4), 'gold', name='elbow', rot=(45, 'x', (8, 13.85, 13.9)))
    m.box((7.5, 14, 7.5), (8.5, 15, 13.6), 'gold', name='arm')
    m.box((7.6, 13.25, 7.6), (8.4, 14, 8.4), 'gold', name='drop')
    disc(m, 8, 8, 4.5, 12.5, 13.25, 'gold', down='spray', name='sunflower')
    disc(m, 8, 8, 2.5, 13.25, 13.75, 'gold', name='sunflower_dome')
    # Porcelain cross-handle valves on the column front.
    for x, cap in ((6.25, 'hot'), (8.75, 'cold')):
        m.box((x + 0.25, -5.25, 12.25), (x + 0.75, -4.75, 13.5), 'gold', name='valve_stem')
        m.box((x - 0.25, -5.25, 12), (x + 1.25, -4.75, 12.25), 'porcelain', name='valve_cross')
        m.box((x + 0.25, -5.75, 11.95), (x + 0.75, -4.25, 12.2), 'porcelain', name='valve_cross')
        m.box((x + 0.3, -5.2, 11.7), (x + 0.7, -4.8, 11.95), cap, name='valve_cap')
    return m


COLUMN = ShowerStyle(
    key='column', title='Ionic Column Shower', title_zh='爱奥尼柱淋浴',
    concept='A fluted column on an attic base, crowned by an Ionic capital with volutes; a gold '
            'gooseneck carries a sunflower rain head, and porcelain cross handles mix the water.',
    default_material='white',
    textures={'body': 'minecraft:block/white_concrete', 'trim': 'minecraft:block/white_terracotta',
              'gold': 'minecraft:block/gold_block', 'volute': T + 'volute', 'spray': T + 'spray_gold',
              'porcelain': 'minecraft:block/white_concrete_powder',
              'hot': 'minecraft:block/red_concrete', 'cold': 'minecraft:block/blue_concrete'},
    profile=ShowerProfile(mount_y=-16, top_y=15, outlet=(8, 12.5, 8), width=8, depth=8),
    build=column,
    trim_from_roster=True,
)


# --------------------------------------------------------------------------- coral

def sprite(m, cx, y0, cz, size, tex, name='frond'):
    """Vanilla-style crossed planes (like coral fans), double-sided."""
    h = size / 2
    for axis_x in (True, False):
        if axis_x:
            f, t, faces = (cx - h, y0, cz), (cx + h, y0 + size, cz + 0.001), ('north', 'south')
        else:
            f, t, faces = (cx, y0, cz - h), (cx + 0.001, y0 + size, cz + h), ('east', 'west')
        m.box(f, t, tex, only=faces, fit=(0, 0, 16, 16), name=name, rot=(45, 'y', (cx, y0, cz), True))


def coral():
    m = Model()
    # A brain-coral trunk with angled branches ending in coral fans, and a small reef at its foot.
    m.box((7, -16, 14), (9, 12.5, 16), 'coral', name='trunk')
    for (x, y, side, frond) in ((7, -9, -1, 'fan_a'), (9, -3, 1, 'fan_b'), (7, 3.5, -1, 'fan_c')):
        x0 = x - 3.5 if side < 0 else x
        m.box((x0, y, 14.4), (x0 + 3.5, y + 1.25, 15.6), 'coral', name='branch',
              rot=(45 * side, 'z', (x, y + 0.6, 15)))
        sprite(m, x + side * 2.6, y + 2.0, 14, 4, frond)
    # Keep the mounting decoration inside the central rim, clear of the tub's side faucet.
    sprite(m, 7, -16, 13.75, 2.5, 'fan_b', name='reef')
    sprite(m, 10.5, -16, 14, 3.5, 'fan_c', name='reef')
    # Scallop-shell head: a ribbed half disc fanning out from the hinge.
    res, k = 0.5, 16 / 13
    bounds = (1, 6, 15, 14)
    for i0, j0, i1, j1 in greedy(disc_mask(8, 13.5, 6.5, res, bounds)):
        x0, z0 = bounds[0] + i0 * res, bounds[1] + j0 * res
        x1, z1 = bounds[0] + i1 * res, bounds[1] + j1 * res
        m.box((x0, 12.5, z0), (x1, 13.25, z1), {'*': 'body', 'down': 'spray', 'up': 'nacre'},
              uv={'down': ((x0 - 1.5) * k, (z1 - 7) * k, (x1 - 1.5) * k, (z0 - 7) * k)}, name='shell_web')
    ribs = []
    for angle, along_x in ((0, False), (22.5, False), (-22.5, False), (45, False), (-45, False),
                           (22.5, True), (-22.5, True)):
        rect = ((1.75, 13.1, 8, 13.9) if angle < 0 else (8, 13.1, 14.25, 13.9)) if along_x else (7.6, 7.25, 8.4, 13.5)
        ribs.append((rect, -angle))  # A Y rotation has the opposite sign in the X/Z plane.
    rib_mask = rotated_rect_mask(ribs, .25, (1, 6, 15, 14), (8, 13.5))
    mask_boxes(m, rib_mask, 13.25, 13.75, 'shell_rose', .25, (1, 6, 15, 14), name='shell_rib')
    m.box((6.5, 12.25, 12.5), (9.5, 14, 14.5), 'body', name='shell_hinge')
    m.box((7.25, 13.75, 12.75), (8.75, 15, 14.25), 'pearl', name='pearl', fit=(3, 3, 13, 13))
    # Starfish valve on the coral.
    m.box((6.75, -5.5, 13.5), (8.75, -4.5, 14), 'coral', name='valve_hub')
    m.box((5.75, -6.75, 13.3), (9.75, -3.25, 13.5), 'starfish', name='starfish', only=('north',),
          fit=(0, 0, 16, 16), rot=(22.5, 'z', (7.75, -5, 13.4)))
    return m


CORAL = ShowerStyle(
    material_slots=('body','trim','coral','nacre','shell_rose'),
    key='coral', title='Coral Reef Shower', title_zh='珊瑚礁淋浴',
    concept='A brain-coral trunk whose branches end in horn, tube and fire coral fans carries a ribbed '
            'scallop rain head lined with nacre and crowned by a pearl; a starfish works as the valve.',
    default_material='pink',
    textures={'body': T + 'shell_ivory', 'nacre': T + 'nacre', 'shell_rose': T + 'shell_rose', 'pearl': T + 'pearl',
              'spray': T + 'spray_shell', 'starfish': T + 'starfish', 'coral': 'minecraft:block/brain_coral_block',
              'fan_a': 'minecraft:block/horn_coral_fan', 'fan_b': 'minecraft:block/tube_coral_fan',
              'fan_c': 'minecraft:block/fire_coral_fan'},
    profile=ShowerProfile(mount_y=-16, top_y=15, outlet=(8, 12.5, 10), width=10, depth=6),
    build=coral,
)


# --------------------------------------------------------------------------- bucket

def chain(m, x, z, y0, y1, name='chain'):
    """Two crossed planes using vanilla's chain texture, at half scale."""
    m.box((x - 0.75, y0, z), (x + 0.75, y1, z + 0.001), 'chain', only=('north', 'south'), name=name,
          uv={'north': (0, 0, 3, (y1 - y0) * 2 % 16 or 16), 'south': (0, 0, 3, (y1 - y0) * 2 % 16 or 16)},
          rot=(45, 'y', (x, y0, z)))
    m.box((x, y0, z - 0.75), (x + 0.001, y1, z + 0.75), 'chain', only=('east', 'west'), name=name,
          uv={'east': (3, 0, 6, (y1 - y0) * 2 % 16 or 16), 'west': (3, 0, 6, (y1 - y0) * 2 % 16 or 16)},
          rot=(45, 'y', (x, y0, z)))


def bucket():
    m = Model()
    m.box((7, -16, 14), (9, 14.5, 16), 'trim', name='post')
    m.box((6.75, 14.5, 13.75), (9.25, 15, 16), 'trim', name='post_cap')
    m.box((7.25, 12.5, 4), (8.75, 14.5, 14), 'body', name='beam', world90={'east', 'west', 'up', 'down'})
    m.box((7.4, 7.5, 8), (8.6, 8.75, 15.75), 'body', name='brace', rot=(45, 'x', (8, 8, 14)))
    # Galvanised bucket on a chain, with a sprinkler rose underneath.
    chain(m, 8, 6, 11.75, 12.5)
    m.box((4.85, 10.25, 5.75), (5.15, 11.75, 6.25), 'band', name='bail')
    m.box((10.85, 10.25, 5.75), (11.15, 11.75, 6.25), 'band', name='bail')
    m.box((4.85, 11.75, 5.75), (11.15, 12, 6.25), 'band', name='bail')
    m.box((5.5, 7.5, 3.5), (10.5, 9.25, 8.5), 'metal', name='bucket')
    m.box((5.15, 9.25, 3.15), (10.85, 11, 8.85), {'*': 'metal', 'up': 'water_top'}, name='bucket',
          uv={'up': (0, 0, 16, 16)})
    m.box((5.4, 8.75, 3.4), (10.6, 9.25, 8.6), 'band', name='bucket_hoop')
    m.box((5.05, 10.4, 3.05), (10.95, 10.9, 8.95), 'band', name='bucket_hoop')
    disc(m, 8, 6, 2, 6.75, 7.5, 'band', down='spray', name='rose')
    # Pull chain with a turned wooden handle.
    m.box((10.5, 8, 5.75), (11.25, 8.5, 6.25), 'band', name='lever')
    chain(m, 11, 6, 1.5, 8)
    m.box((10.5, -0.5, 5.5), (11.5, 1.5, 6.5), 'trim', name='pull_handle')
    return m


BUCKET = ShowerStyle(
    material_slots=('body','trim','metal'),
    key='bucket', title='Frontier Bucket Shower', title_zh='西部提桶淋浴',
    concept='A timber post and braced beam hold a galvanised bucket on a chain; a sprinkler rose '
            'rains from its base, and a pull chain with a wooden handle tips the water.',
    default_material='spruce',
    textures={'body': 'minecraft:block/spruce_planks', 'trim': 'minecraft:block/spruce_log',
              'metal': 'minecraft:block/iron_block', 'band': T + 'cast_iron', 'chain': 'minecraft:block/chain',
              'spray': T + 'spray_rose', 'water_top': T + 'bucket_water'},
    profile=ShowerProfile(mount_y=-16, top_y=15, outlet=(8, 6.75, 6), width=4, depth=4),
    build=bucket,
    trim_from_roster=True,
)


# --------------------------------------------------------------------------- panel

def panel():
    m = Model()
    m.box((5.75, -16, 15), (10.25, 9.5, 16), 'body', name='frame')
    m.box((6.1, -15.25, 14.75), (9.9, 8.75, 15), 'glass', name='glass')
    m.box((5.75, -15.25, 14.75), (6.1, 8.75, 15), 'led', name='edge_light', emissive=True)
    m.box((9.9, -15.25, 14.75), (10.25, 8.75, 15), 'led', name='edge_light', emissive=True)
    m.box((6.4, 3.5, 14.6), (9.6, 7.5, 14.75), 'display', name='display', only=('north',),
          uv={'north': (0, 0, 16, 16)}, emissive=True)
    m.box((6.4, 1.25, 14.6), (9.6, 2.75, 14.75), 'buttons', name='buttons', only=('north',),
          uv={'north': (0, 0, 16, 8)}, emissive=True)
    for y in (-3.5, -7.5, -11.5):
        m.box((7.25, y, 14.25), (8.75, y + 1.5, 14.75), {'*': 'chrome', 'north': 'jet_face'}, name='body_jet',
              uv={'north': (0, 0, 16, 16)})
    # Slim riser, cantilever arm and halo head.
    m.box((7.25, 9.5, 15.25), (8.75, 14, 16), 'body', name='riser')
    m.box((7.25, 13.25, 8), (8.75, 14, 15.25), 'body', name='arm')
    disc(m, 8, 8, 4.5, 12.75, 13.5, 'body', down='spray', name='halo')
    disc(m, 8, 8, 4.75, 12.5, 12.75, 'led', hole=3.75, name='halo_light', emissive=True)
    return m


PANEL = ShowerStyle(
    key='panel', title='Aurora Smart Shower', title_zh='极光智能淋浴',
    concept='A slim smoked-glass shower tower with glowing edge lights, a temperature display, touch '
            'buttons and three body jets, topped by a cantilevered halo rain head with an LED ring.',
    default_material='white',
    textures={'body': 'minecraft:block/white_concrete', 'glass': T + 'smoked_glass', 'led': T + 'led_cyan',
              'display': T + 'shower_display', 'buttons': T + 'shower_buttons', 'chrome': T + 'chrome',
              'jet_face': T + 'jet_face', 'spray': T + 'spray_halo'},
    profile=ShowerProfile(mount_y=-16, top_y=14, outlet=(8, 12.5, 8), width=8, depth=8),
    build=panel,
)

STYLES = [BAMBOO, COLUMN, CORAL, BUCKET, PANEL]
