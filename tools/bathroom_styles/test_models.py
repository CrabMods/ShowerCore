"""Geometry and palette checks for generated bathroom models. Run with python -B -m unittest tools.bathroom_styles.test_models."""
from __future__ import annotations
import collections
import itertools
import math
import unittest

import numpy as np

from . import duck, showers, tubs, textures
from .generate import material_textures, material_surfaces
from .geometry import model_json, slice_halves


def oriented_box(box):
    center = (np.array(box.f) + box.t) / 2
    half = (np.array(box.t) - box.f) / 2
    rotation = np.eye(3)
    if box.rot:
        angle = math.radians(box.rot['angle'])
        c, s = math.cos(angle), math.sin(angle)
        axis = 'xyz'.index(box.rot['axis'])
        u, v = (axis + 1) % 3, (axis + 2) % 3
        rotation[u, u] = rotation[v, v] = c
        rotation[u, v], rotation[v, u] = -s, s
        origin = np.array(box.rot['origin'])
        center = rotation @ (center - origin) + origin
        if box.rot.get('rescale'):
            scale = np.ones(3)
            scale[[u, v]] = 1 / c
            center = (center - origin) * scale + origin
            rotation = np.diag(scale) @ rotation
    axes = rotation.T.copy()
    lengths = np.linalg.norm(axes, axis=1)
    axes /= lengths[:, None]
    half *= lengths
    extent = np.sum(abs(axes) * half[:, None], axis=0)
    return center, axes, half, center - extent, center + extent


def touches(a, b):
    ca, aa, ha, lo_a, hi_a = a
    cb, ab, hb, lo_b, hi_b = b
    if np.any(lo_a > hi_b + 1e-6) or np.any(lo_b > hi_a + 1e-6):
        return False
    axes = np.vstack((aa, ab, np.cross(aa[:, None, :], ab[None, :, :]).reshape(-1, 3)))
    separation = abs(axes @ (cb - ca)) - abs(axes @ aa.T) @ ha - abs(axes @ ab.T) @ hb
    return bool(np.all(separation <= 1e-6))


def joined(a, b):
    """Parts must share area or volume, rather than merely touching at an edge."""
    if not touches(a, b):
        return False
    ca, aa, ha, _, _ = a
    cb, ab, hb, _, _ = b
    axes = np.vstack((aa, ab, np.cross(aa[:, None, :], ab[None, :, :]).reshape(-1, 3)))
    axes = axes[np.linalg.norm(axes, axis=1) > 1e-7]
    axes /= np.linalg.norm(axes, axis=1)[:, None]
    separation = abs(axes @ (cb - ca)) - abs(axes @ aa.T) @ ha - abs(axes @ ab.T) @ hb
    contact_axes = axes[abs(separation) <= 1e-6]
    return len(contact_axes) == 0 or np.linalg.matrix_rank(contact_axes, tol=1e-5) <= 1


class BathroomModelTest(unittest.TestCase):
    def test_all_authored_parts_connect_to_their_structure(self):
        models = [("tub." + s.key, s.build().boxes) for s in tubs.STYLES]
        models += [("shower." + s.key, s.build().boxes) for s in showers.STYLES]
        models += [("duck.deluxe", duck._shape().boxes)]
        for name, boxes in models:
            with self.subTest(model=name):
                shapes = [oriented_box(b) for b in boxes]
                reached = {0}
                remaining = set(range(1, len(boxes)))
                queue = [0]
                while queue:
                    current = queue.pop()
                    for other in list(remaining):
                        if joined(shapes[current], shapes[other]):
                            reached.add(other)
                            remaining.remove(other)
                            queue.append(other)
                self.assertFalse(remaining, str(collections.Counter(boxes[i].name for i in remaining)))

    def test_shell_rib_crossings_have_no_overlapping_surface(self):
        for model, name in [(tubs.seashell(), 'fan_rib'), (showers.coral(), 'shell_rib')]:
            boxes = [b for b in model.boxes if b.name == name]
            self.assertTrue(boxes)
            for box in boxes:
                self.assertIsNone(box.rot)
            for a, b in itertools.combinations(boxes, 2):
                overlap = np.minimum(a.t, b.t) - np.maximum(a.f, b.f)
                self.assertFalse(np.all(overlap > 1e-6), 'Rib masks must be merged before extrusion')

    def test_starfish_has_visible_depth_and_touches_the_backrest(self):
        boxes = tubs.seashell().boxes
        relief = [b for b in boxes if b.name == 'starfish']
        web = [oriented_box(b) for b in boxes if b.name in ('fan_web', 'fan_rib')]
        self.assertTrue(relief)
        for box in relief:
            self.assertGreaterEqual(box.t[2] - box.f[2], .5)
            self.assertGreater(len(box.faces), 1, 'Relief must have sides, not only a floating front plane')
        self.assertTrue(any(touches(oriented_box(box), support) for box in relief for support in web))

    def test_default_shell_palette_is_layered_but_every_body_surface_can_change(self):
        ivory = textures.shell_ivory()[..., :3].mean(axis=(0, 1))
        rose = textures.shell_rose()[..., :3].mean(axis=(0, 1))
        nacre = textures.nacre()[..., :3].mean(axis=(0, 1))
        self.assertGreater(np.linalg.norm(ivory - rose), 30)
        self.assertGreater(nacre.mean(), rose.mean() + 20)
        for style in [tubs.SEASHELL, showers.CORAL]:
            self.assertEqual(material_textures(style, 'pink')['body'], style.textures['body'])
            self.assertEqual(material_textures(style, 'oak')['body'], 'minecraft:block/oak_planks')
            for box in material_surfaces(style.build().boxes, style.material_slots):
                for face in box.faces.values():
                    self.assertEqual(face.tint, 1 if face.tex in style.material_slots else 2)
            self.assertIn('shell_rose', style.material_slots)
            self.assertIn('nacre', style.material_slots)

    def test_generated_models_have_no_visible_coplanar_axis_faces(self):
        for style in tubs.STYLES:
            boxes = style.build().boxes
            head, foot = slice_halves(boxes)
            for part in [boxes, head, foot]:
                _, problems = model_json(material_surfaces(part, style.material_slots), style.textures,
                                         'neoforge_data', style.key)
                self.assertFalse(problems, str(problems))
        for style in showers.STYLES:
            boxes = style.build().boxes
            _, problems = model_json(material_surfaces(boxes, style.material_slots), style.textures,
                                     'neoforge_data', style.key)
            self.assertFalse(problems, str(problems))

    def test_shower_geometry_profiles_stay_inside_the_installation_contract(self):
        for style in showers.STYLES:
            style.profile.validate(style.build())

    def test_matching_sets_do_not_penetrate_the_tub_fittings(self):
        styles = [showers.BAMBOO, showers.COLUMN, showers.CORAL, showers.BUCKET, showers.PANEL]
        rotation = np.diag([-1, 1, -1])
        for tub, shower in zip(tubs.STYLES, styles):
            translation = np.array([16, tub.profile.rim_y - shower.profile.mount_y, 16])
            for tub_box in tub.build().boxes:
                a = oriented_box(tub_box)
                for shower_box in shower.build().boxes:
                    center, axes, half, _, _ = oriented_box(shower_box)
                    center = rotation @ center + translation
                    axes = axes @ rotation
                    extent = np.sum(abs(axes) * half[:, None], axis=0)
                    b = (center, axes, half, center - extent, center + extent)
                    if not touches(a, b):
                        continue
                    ac, aa, ah, _, _ = a
                    test_axes = np.vstack((aa, axes, np.cross(aa[:, None, :], axes[None, :, :]).reshape(-1, 3)))
                    test_axes = test_axes[np.linalg.norm(test_axes, axis=1) > 1e-7]
                    test_axes /= np.linalg.norm(test_axes, axis=1)[:, None]
                    separation = abs(test_axes @ (center - ac)) - abs(test_axes @ aa.T) @ ah - abs(test_axes @ axes.T) @ half
                    self.assertGreaterEqual(separation.max(), -1e-6,
                                            tub.key + '/' + shower.key + ': ' + tub_box.name + '/' + shower_box.name)


if __name__ == '__main__':
    unittest.main()
