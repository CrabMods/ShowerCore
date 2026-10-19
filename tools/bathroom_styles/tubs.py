"""Five bathtub shapes. Each is authored north-facing over a 16 x 32 footprint:
the head half (z 0..16) carries the faucet and the shower mounting rim, the foot
half is z 16..32. The BER draws liquid as one plane over x 2..14, z 2..30, so
every cavity keeps its inner walls at an inset of at least 2 pixels at the
water line and its outer walls beyond that.
"""
from __future__ import annotations

from dataclasses import dataclass, field

from .geometry import Model
from .shapes import disc_mask, greedy, perimeter_ribs, rrect, rotated_rect_mask

T = 'showercore:block/bathroom/'


@dataclass
class TubProfile:
    rim_y: int                 # back-rim height under the shower pipe (BathroomGeometry.Tub)
    water_y: float             # liquid plane height in pixels
    floor_y: float             # cavity floor height in pixels
    seat_y_offset: float       # suggested SeatEntity offset in blocks
    faucet_hitboxes: list      # head-local (x, y, z) pixel origins of FaucetInteractionEntity boxes
    drop: tuple                # head-local running-drop box (x1, z1, x2, z2, top_y); bottom is water_y
    faucet_side: str           # 'end' (head wall) or 'right' (right rim) for drop orientation


@dataclass
class TubStyle:
    key: str
    title: str
    title_zh: str
    concept: str
    default_material: str
    textures: dict
    profile: TubProfile
    build: callable
    trim_from_roster: bool = False
    material_slots: tuple = ('body', 'trim')


# --------------------------------------------------------------------------- ofuro

def ofuro(running=False):
    """Hinoki soaking tub: vertical staves, copper hoops and a bamboo kakei spout."""
    m = Model()
    staves = {'north', 'south', 'east', 'west'}
    # Four corner sleepers lift the tub off wet floors.
    for x0 in (1, 12):
        for z0 in (1.5, 27.5):
            m.box((x0, 0, z0), (x0 + 3, 1, z0 + 3), 'trim', name='sleeper', skip=('down',))
    # Bottom boards and stave walls.
    m.box((0.5, 1, 0.5), (15.5, 3, 31.5), 'body', name='bottom', world90=staves)
    rrect(m, 3, 13.5, (0.5, 0.5, 15.5, 31.5), 0, 'body', hole=((2, 2, 14, 30), 0),
          name='staves', world90=staves)
    # Thick top boards, slightly proud of the staves. Long boards run along the tub.
    m.box((0, 13.5, 0), (16, 15, 2), 'body', name='cap_head')
    m.box((0, 13.5, 30), (16, 15, 32), 'body', name='cap_foot')
    m.box((0, 13.5, 2), (2, 15, 30), 'body', name='cap_left', world90={'up', 'down'})
    m.box((14, 13.5, 2), (16, 15, 30), 'body', name='cap_right', world90={'up', 'down'})
    # Two copper hoops.
    for y in (2.0, 10.5):
        rrect(m, y, y + 1.25, (0, 0, 16, 32), 0, 'hoop', hole=((0.5, 0.5, 15.5, 31.5), 0), name='hoop')
    # Bamboo kakei: a cane post on the head-right corner and a spout leaning over the water.
    m.box((12.25, 15, 0.25), (14.25, 23, 2.25), {'*': 'cane', 'up': 'cane_end', 'down': 'cane_end'},
          name='kakei_post', uv={'up': (0, 0, 16, 16), 'down': (0, 0, 16, 16)})
    m.box((12, 17.5, 0), (14.5, 18.25, 2.5), 'cane_node', name='kakei_node')
    m.box((12, 20.5, 0), (14.5, 22.25, 2.5), 'rope', name='kakei_binding')
    m.box((12.5, 20.75, 2.5), (14, 22, 7), {'*': 'cane', 'south': 'cane_end'}, name='kakei_spout',
          uv={'south': (0, 0, 16, 16)})
    m.box((12.4, 20.65, 4.5), (14.1, 22.1, 5), 'cane_node', name='kakei_spout_node')
    # A small wooden ladle resting on the foot-left rim.
    m.box((0.85, 15, 17.5), (1.35, 15.4, 26.5), 'ladle', name='ladle_handle')
    m.box((0.35, 15, 26.5), (2.35, 16.5, 29), {'*': 'ladle', 'up': 'ladle_inner'}, name='ladle_cup')
    m.box((0.6, 16.5, 26.75), (2.1, 16.6, 28.75), 'ladle', name='ladle_lip', skip=('up', 'down'))
    return m


OFURO = TubStyle(
    key='ofuro', title='Hinoki Ofuro', title_zh='桧木泡汤桶',
    concept='A deep Japanese soaking tub: vertical cedar staves, two copper hoops, thick top boards, '
            'a bamboo kakei spout and a wooden ladle resting on the rim.',
    default_material='birch',
    textures={'body': 'minecraft:block/birch_planks', 'trim': 'minecraft:block/stripped_dark_oak_log',
              'hoop': T + 'copper_hoop', 'cane': T + 'bamboo_cane', 'cane_end': T + 'bamboo_cane_end',
              'cane_node': T + 'bamboo_node', 'rope': T + 'hemp_rope',
              'ladle': 'minecraft:block/stripped_spruce_log', 'ladle_inner': 'minecraft:block/dark_oak_planks'},
    profile=TubProfile(rim_y=15, water_y=12.5, floor_y=3, seat_y_offset=0.2,
                       faucet_hitboxes=[(13.25, 15, 2.5)], drop=(12.75, 6.25, 13.75, 7.0, 20.75),
                       faucet_side='end'),
    build=ofuro,
)


def ring(m, y0, y1, outer, r_out, inner, r_in, tex, res=0.5, **kw):
    return rrect(m, y0, y1, outer, r_out, tex, res=res, hole=(inner, r_in), **kw)


# --------------------------------------------------------------------------- roman

def roman(running=False):
    """Classical marble bath: plinth, pilasters, raised fields, a backboard and a gold lion mask."""
    m = Model()
    m.box((0, 0, 0), (16, 1, 32), 'body', name='plinth')
    m.box((0.5, 1, 0.5), (15.5, 2, 31.5), 'body', name='torus')
    m.box((0.75, 2, 0.75), (15.25, 2.5, 31.25), 'gold', name='fillet_low')
    m.box((1, 2.5, 1), (15, 3, 31), 'body', name='floor')
    ring(m, 3, 10.5, (1, 1, 15, 31), 0, (2, 2, 14, 30), 0, 'body', name='wall')
    # Pilasters at the corners and the middle of the long sides.
    for z0 in (0.5, 15, 29.5):
        z1 = z0 + (2.5 if z0 != 15 else 2)
        m.box((0.5, 2.5, z0), (1, 10.5, z1), 'body', name='pilaster')
        m.box((15, 2.5, z0), (15.5, 10.5, z1), 'body', name='pilaster')
    for x0, x1 in ((1, 3), (13, 15)):
        m.box((x0, 2.5, 0.5), (x1, 10.5, 1), 'body', name='pilaster')
        m.box((x0, 2.5, 31), (x1, 10.5, 31.5), 'body', name='pilaster')
    # Raised fields with gold rosettes.
    for z0, z1 in ((4, 14), (18, 28)):
        zc = (z0 + z1) / 2
        m.box((0.75, 4, z0), (1, 9, z1), 'body', name='field')
        m.box((15, 4, z0), (15.25, 9, z1), 'body', name='field')
        m.box((0.5, 5.75, zc - 0.75), (0.75, 7.25, zc + 0.75), 'gold', name='rosette')
        m.box((15.25, 5.75, zc - 0.75), (15.5, 7.25, zc + 0.75), 'gold', name='rosette')
    m.box((4, 4, 0.75), (12, 9, 1), 'body', name='field')
    m.box((4, 4, 31), (12, 9, 31.25), 'body', name='field')
    m.box((7.25, 5.75, 0.5), (8.75, 7.25, 0.75), 'gold', name='rosette')
    m.box((7.25, 5.75, 31.25), (8.75, 7.25, 31.5), 'gold', name='rosette')
    # Gold fillet, cornice and rim.
    ring(m, 10.5, 11, (0.75, 0.75, 15.25, 31.25), 0, (2, 2, 14, 30), 0, 'gold', name='fillet_high')
    ring(m, 11, 12, (0.5, 0.5, 15.5, 31.5), 0, (2, 2, 14, 30), 0, 'body', name='cornice')
    ring(m, 12, 13, (0, 0, 16, 32), 0, (2, 2, 14, 30), 0, 'body', name='rim')
    # Backboard carrying the shower mount (rim 16) with gold finials.
    m.box((1.5, 13, 0.25), (14.5, 15.5, 2), 'body', name='backboard')
    m.box((1, 15.5, 0), (15, 16, 2.25), 'body', name='backboard_cap')
    m.box((1.5, 14.25, 0.12), (14.5, 14.5, 0.25), 'gold', name='backboard_inlay')
    for x0 in (1.75, 12.75):
        m.box((x0, 16, 0.375), (x0 + 1.5, 17.25, 1.875), 'gold', name='finial')
        m.box((x0 + 0.4, 17.25, 0.775), (x0 + 1.1, 17.75, 1.475), 'gold', name='finial_tip')
    # Lion mask spout on the inner face of the head wall.
    m.box((6.5, 11, 2), (9.5, 15.5, 2.75), 'mane', name='mane', fit=(4, 0, 12, 16))
    m.box((5.5, 11.75, 2), (6.5, 14.75, 2.6), 'mane', name='mane', fit=(0, 3, 2.5, 13))
    m.box((9.5, 11.75, 2), (10.5, 14.75, 2.6), 'mane', name='mane', fit=(13.5, 3, 16, 13))
    m.box((6.75, 11.5, 2.75), (9.25, 14.75, 3.5), {'*': 'gold', 'south': 'lion'}, name='lion_face',
          uv={'south': (0, 0, 16, 16)})
    m.box((7.25, 11.75, 3.5), (8.75, 12.75, 4), 'gold', name='muzzle')
    m.box((7.6, 11.9, 4), (8.4, 12.5, 4.6), {'*': 'gold', 'south': 'spout_hole'}, name='spout',
          uv={'south': (0, 0, 16, 16)})
    return m


ROMAN = TubStyle(
    key='roman', title='Roman Marble Bath', title_zh='罗马大理石浴池',
    concept='A classical bath on a moulded plinth: pilasters with raised fields and gold rosettes, '
            'a cornice rim, a backboard with finials and a gold lion mask that pours the water.',
    default_material='white',
    textures={'body': 'minecraft:block/white_concrete', 'gold': 'minecraft:block/gold_block',
              'mane': T + 'lion_mane', 'lion': T + 'lion_face', 'spout_hole': T + 'spout_hole'},
    profile=TubProfile(rim_y=16, water_y=10.5, floor_y=3, seat_y_offset=0.1,
                       faucet_hitboxes=[(8, 11.25, 3)], drop=(7.6, 4.0, 8.4, 4.6, 11.9), faucet_side='end'),
    build=roman,
)


# --------------------------------------------------------------------------- seashell

def seashell(running=False):
    """Venus scallop tub: fluted bowl on a pedestal, a ribbed shell backrest and a pearl."""
    m = Model()
    rrect(m, 0, 1.5, (3, 4, 13, 28), 2.5, 'trim', name='pedestal')
    rrect(m, 1.5, 3, (1.5, 1.5, 14.5, 30.5), 4, 'body', name='bowl_base')
    ring(m, 3, 5.5, (1, 1, 15, 31), 4.5, (2.5, 2.5, 13.5, 29.5), 3, 'body', name='bowl')
    ring(m, 5.5, 11, (0.5, 0.5, 15.5, 31.5), 5, (2, 2, 14, 30), 3.5, 'body', name='bowl')
    ring(m, 11, 12, (0, 0, 16, 32), 5.5, (2, 2, 14, 30), 3.5, 'nacre', name='lip')
    # Scallop ribs follow the bowl all the way round, corners included.
    perimeter_ribs(m, 5.5, 11, (0.5, 0.5, 15.5, 31.5), 5, 0.375, 2, 'shell_rose', name='rib')
    perimeter_ribs(m, 3, 5.5, (1, 1, 15, 31), 4.5, 0.375, 2, 'shell_rose', name='rib')
    # Shell backrest at the foot: a half disc web with radiating ribs and knobbed ends.
    res = 0.5
    mask = disc_mask(8, 11, 7.75, res, (0, 12, 16, 19))
    for i0, j0, i1, j1 in greedy(mask):
        m.box((i0 * res, 12 + j0 * res, 30), (i1 * res, 12 + j1 * res, 31.75),
              {'*': 'body', 'north': 'nacre'}, name='fan_web', world90={'up', 'down'})
    # Merge the raised ribs in the face plane; separate rotated bars flicker at their crossings.
    ribs = []
    for angle, horizontal in ((0, False), (22.5, False), (-22.5, False), (45, False), (-45, False),
                              (22.5, True), (-22.5, True)):
        rect = ((8, 10.5, 16.6, 11.5) if angle > 0 else (-0.6, 10.5, 8, 11.5)) if horizontal else (7.5, 11, 8.5, 19.6)
        ribs.append((rect, angle))
    rib_res, rib_bounds = 0.25, (-1, 12, 17, 21)
    rib_mask = rotated_rect_mask(ribs, rib_res, rib_bounds, (8, 11))
    for z0, z1 in ((29.5, 30), (31.75, 32)):
        for i0, j0, i1, j1 in greedy(rib_mask):
            m.box((-1 + i0 * rib_res, 12 + j0 * rib_res, z0),
                  (-1 + i1 * rib_res, 12 + j1 * rib_res, z1), 'shell_rose', name='fan_rib')
    # Solid relief rests on the raised ribs; its silhouette has real sides.
    from .textures import starfish
    star_mask = (starfish()[..., 3] > 0).T[:, ::-1]
    step = 3.5 / 16
    for i0, j0, i1, j1 in greedy(star_mask):
        m.box((9.75 + i0 * step, 13.25 + j0 * step, 29),
              (9.75 + i1 * step, 13.25 + j1 * step, 29.5),
              {'*': 'starfish_edge', 'north': 'starfish'}, name='starfish',
              uv={'north': (16-i1,16-j1,16-i0,16-j0)})
    # A pearl resting at the foot of the backrest.
    m.box((6.9, 12.1, 28.9), (9.1, 14.3, 31.1), 'pearl', name='pearl', fit=(3, 3, 13, 13))
    m.box((6.6, 12.6, 29.4), (9.4, 13.8, 30.6), 'pearl', name='pearl', fit=(4, 5, 12, 11))
    m.box((7.3, 11.8, 29.3), (8.7, 14.6, 30.7), 'pearl', name='pearl', fit=(5, 2, 11, 14))
    m.box((7.4, 12.5, 28.6), (8.6, 13.9, 31.4), 'pearl', name='pearl', fit=(5, 5, 11, 11))
    # Brass spout with a pearl knob on the head-right rim.
    m.box((11.5, 12, 0.5), (13.5, 12.75, 2.5), 'brass', name='spout_base')
    m.box((12, 12.75, 1), (13, 15.5, 2), 'brass', name='spout_riser')
    m.box((12, 14.5, 2), (13, 15.5, 4.5), 'brass', name='spout_arm')
    m.box((12, 13.75, 3.75), (13, 14.5, 4.5), {'*': 'brass', 'down': 'spout_hole'}, name='spout_tip',
          uv={'down': (0, 0, 16, 16)})
    m.box((11.9, 15.5, 0.9), (13.1, 16.6, 2.1), 'pearl', name='knob', fit=(4, 4, 12, 12))
    return m


SEASHELL = TubStyle(
    material_slots=('body','trim','nacre','shell_rose'),
    key='seashell', title='Venus Seashell Tub', title_zh='维纳斯贝壳浴缸',
    concept='A fluted scallop bowl on a slim pedestal, with a scalloped crest, a ribbed shell backrest '
            'lined with nacre, a pearl at its foot and a brass spout topped by a pearl.',
    default_material='pink',
    textures={'body': T + 'shell_ivory', 'trim': T + 'shell_rose', 'shell_rose': T + 'shell_rose',
              'nacre': T + 'nacre', 'pearl': T + 'pearl', 'brass': T + 'brass', 'spout_hole': T + 'spout_hole',
              'starfish': T + 'starfish', 'starfish_edge': 'minecraft:block/orange_terracotta'},
    profile=TubProfile(rim_y=12, water_y=9.5, floor_y=3, seat_y_offset=0.1,
                       faucet_hitboxes=[(12.5, 12, 2.5)], drop=(12.1, 3.85, 12.9, 4.4, 13.75), faucet_side='end'),
    build=seashell,
    trim_from_roster=True,
)


# --------------------------------------------------------------------------- washtub

def washtub(running=False):
    """Frontier galvanised washtub: flared ribbed oval, rolled bead, loop handles and a hand pump."""
    m = Model()
    for z0 in (5, 25):
        m.box((0.5, 0, z0), (15.5, 1, z0 + 2), 'trim', name='skid')
    rrect(m, 1, 2, (1.5, 1.5, 14.5, 30.5), 4, 'body', name='bottom')
    ring(m, 2, 4, (1, 1, 15, 31), 4.5, (2.5, 2.5, 13.5, 29.5), 3.5, 'body', name='wall')
    ring(m, 4, 4.75, (0.6, 0.6, 15.4, 31.4), 4.9, (2.25, 2.25, 13.75, 29.75), 3.75, 'body', name='rib')
    ring(m, 4.75, 7.25, (0.75, 0.75, 15.25, 31.25), 4.75, (2.25, 2.25, 13.75, 29.75), 3.75, 'body', name='wall')
    ring(m, 7.25, 8, (0.25, 0.25, 15.75, 31.75), 5.25, (2, 2, 14, 30), 4, 'body', name='rib')
    ring(m, 8, 10, (0.5, 0.5, 15.5, 31.5), 5, (2, 2, 14, 30), 4, 'body', name='wall')
    ring(m, 10, 11, (0, 0, 16, 32), 5.5, (2, 2, 14, 30), 4, 'body', name='bead')
    # Riveted grab bars at both ends, tucked under the bead.
    for z0, z1, zp0, zp1 in ((0, 0.25, 0.25, 0.5), (31.75, 32, 31.5, 31.75)):
        for x0 in (4.5, 10.5):
            m.box((x0, 8.25, zp0), (x0 + 1, 9.75, zp1), 'iron', name='handle_bracket')
        m.box((4.5, 8.5, z0), (11.5, 9.25, z1), 'iron', name='handle_bar')
    # Cast-iron pitcher pump standing on the left rim near the head.
    m.box((0.5, 11, 3), (3.25, 11.5, 6), 'iron', name='pump_flange')
    m.box((0.75, 11.5, 3.25), (3, 16.5, 5.75), 'iron', name='pump_body')
    m.box((0.6, 13.75, 3.1), (3.15, 14.25, 5.9), 'iron', name='pump_band')
    m.box((3, 14.5, 4), (5.5, 15.5, 5), 'iron', name='pump_spout')
    m.box((4.5, 13.5, 4), (5.5, 14.5, 5), {'*': 'iron', 'down': 'spout_hole'}, name='pump_spout_tip',
          uv={'down': (0, 0, 16, 16)})
    m.box((0.5, 16.5, 3), (3.25, 17, 6), 'iron', name='pump_cap')
    m.box((1.5, 17, 4.25), (2.25, 18.25, 5), 'iron', name='pump_pivot')
    m.box((1.6, 17.6, 4.4), (2.15, 18.15, 12), 'iron', name='pump_lever', rot=(-22.5, 'x', (1.875, 17.875, 4.6)))
    m.box((1.45, 17.45, 11.25), (2.3, 18.3, 12.75), 'trim', name='pump_grip', rot=(-22.5, 'x', (1.875, 17.875, 4.6)))
    return m


WASHTUB = TubStyle(
    key='washtub', title='Frontier Washtub', title_zh='西部铁皮澡盆',
    concept='A galvanised oval washtub on wooden skids: flared walls with pressed ribs, a rolled bead '
            'rim, riveted loop handles and a cast-iron pitcher pump that fills it.',
    default_material='iron',
    textures={'body': 'minecraft:block/iron_block', 'trim': 'minecraft:block/spruce_planks',
              'iron': T + 'cast_iron', 'spout_hole': T + 'spout_hole'},
    profile=TubProfile(rim_y=11, water_y=8.75, floor_y=2, seat_y_offset=0.05,
                       faucet_hitboxes=[(2, 13, 4.5)], drop=(4.6, 4.1, 5.4, 4.9, 13.5), faucet_side='end'),
    build=washtub,
)


# --------------------------------------------------------------------------- spa

def spa(running=False):
    """Floating whirlpool spa: dark plinth with under-glow, LED skirt line, jets and a waterfall lip."""
    m = Model()
    rrect(m, 0, 2, (2, 2, 14, 30), 2, 'plinth', name='plinth')
    ring(m, 2, 2.5, (1.5, 1.5, 14.5, 30.5), 2.5, (2.5, 2.5, 13.5, 29.5), 1.5, 'led', name='underglow',
         emissive=True)
    m.box((2.5, 2, 2.5), (13.5, 2.5, 29.5), 'plinth', name='plinth_core')
    rrect(m, 2.5, 3.5, (0.5, 0.5, 15.5, 31.5), 3, 'body', name='base')
    ring(m, 3.5, 11, (0.5, 0.5, 15.5, 31.5), 3, (2, 2, 14, 30), 2.5, 'body', name='shell')
    ring(m, 11, 12, (0.25, 0.25, 15.75, 31.75), 3.25, (2, 2, 14, 30), 2.5, 'body', name='deck')
    ring(m, 12, 13, (0, 0, 16, 32), 3.5, (2, 2, 14, 30), 2.5, 'body', name='deck')
    # Smoked-glass skirt band with an LED line on the long sides.
    m.box((0.375, 5, 3.5), (0.5, 9, 28.5), 'plinth', name='skirt_glass')
    m.box((15.5, 5, 3.5), (15.625, 9, 28.5), 'plinth', name='skirt_glass')
    m.box((0.25, 6.75, 4), (0.375, 7.25, 28), 'led', name='skirt_led', emissive=True)
    m.box((15.625, 6.75, 4), (15.75, 7.25, 28), 'led', name='skirt_led', emissive=True)
    # Whirlpool jets on the inner walls, glowing at the centre.
    for z in (9, 16, 23):
        m.box((2, 6, z - 0.75), (2.25, 7.5, z + 0.75), 'chrome', name='jet')
        m.box((13.75, 6, z - 0.75), (14, 7.5, z + 0.75), 'chrome', name='jet')
        m.box((2.25, 6.5, z - 0.25), (2.4, 7, z + 0.25), 'led', name='jet_light', emissive=True)
        m.box((13.6, 6.5, z - 0.25), (13.75, 7, z + 0.25), 'led', name='jet_light', emissive=True)
    # Waterfall lip below the shower mount and a soft headrest at the foot.
    m.box((5, 11, 2), (11, 11.75, 3.5), 'chrome', name='waterfall')
    m.box((5.5, 11, 3.5), (10.5, 11.25, 3.75), {'*': 'chrome', 'down': 'led'}, name='waterfall_lip',
          emissive={'down'} if running else ())
    m.box((4, 11, 28.25), (12, 13.75, 30), 'cushion', name='headrest', fit=(0, 0, 16, 8))
    m.box((4.5, 13.75, 28.5), (11.5, 14.25, 30), 'cushion', name='headrest_top', fit=(0, 8, 16, 12))
    # Touch panel on the right deck; its status light turns green while the faucet runs.
    m.box((14.1, 13, 3), (15.8, 13.15, 8), 'panel_on' if running else 'panel', name='touch_panel',
          fit=(0, 0, 16, 16), emissive={'up'})
    return m


SPA = TubStyle(
    material_slots=('body','plinth'),
    key='spa', title='Aurora Whirlpool Spa', title_zh='极光按摩浴缸',
    concept='A floating spa on a dark recessed plinth with cyan under-glow, an LED skirt line, '
            'glowing whirlpool jets, a chrome waterfall lip, a soft headrest and a touch panel.',
    default_material='white',
    textures={'body': 'minecraft:block/white_concrete', 'plinth': 'minecraft:block/black_concrete',
              'led': T + 'led_cyan', 'chrome': T + 'chrome', 'cushion': T + 'spa_cushion',
              'panel': T + 'spa_panel', 'panel_on': T + 'spa_panel_running'},
    profile=TubProfile(rim_y=13, water_y=10.5, floor_y=3.5, seat_y_offset=0.12,
                       faucet_hitboxes=[(8, 10, 3), (15, 12, 5.5)], drop=(5.5, 3.5, 10.5, 3.75, 11),
                       faucet_side='end'),
    build=spa,
)

STYLES = [OFURO, ROMAN, SEASHELL, WASHTUB, SPA]
