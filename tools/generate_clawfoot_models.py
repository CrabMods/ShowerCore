"""Generate the clawfoot tub as one hollow shell, then slice its block halves.

Coordinates below use Minecraft's 1/16-block modelling units. No geometry is
nudged to conceal overlapping faces. Texture tile boundaries only subdivide
existing surfaces; they do not move those surfaces.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
import json
import math
from pathlib import Path


@dataclass(frozen=True)
class Vertex:
    x: float
    y: float
    z: float
    u: float
    v: float

    def interpolate(self, other: Vertex, t: float) -> Vertex:
        return Vertex(*(a + (b - a) * t for a, b in zip(self.values(), other.values())))

    def values(self) -> tuple[float, ...]:
        return self.x, self.y, self.z, self.u, self.v


@dataclass(frozen=True)
class Face:
    group: str
    material: str
    vertices: tuple[Vertex, ...]


def rounded_rectangle(x0, z0, x1, z1, radius, segments=8):
    points = []
    for cx, cz, start in ((x1 - radius, z0 + radius, -90),
                          (x1 - radius, z1 - radius, 0),
                          (x0 + radius, z1 - radius, 90),
                          (x0 + radius, z0 + radius, 180)):
        for i in range(segments + 1):
            angle = math.radians(start + 90 * i / segments)
            points.append((cx + radius * math.cos(angle), cz + radius * math.sin(angle)))
    return points


def clip(vertices, field, value, below):
    result = []
    for previous, current in zip(vertices[-1:] + vertices[:-1], vertices):
        a, b = getattr(previous, field), getattr(current, field)
        inside_a = a <= value + 1e-10 if below else a >= value - 1e-10
        inside_b = b <= value + 1e-10 if below else b >= value - 1e-10
        if inside_a != inside_b:
            result.append(previous.interpolate(current, (value - a) / (b - a)))
        if inside_b:
            result.append(current)
    unique = []
    for vertex in result:
        if not unique or sum((a - b) ** 2 for a, b in zip(vertex.values(), unique[-1].values())) > 1e-18:
            unique.append(vertex)
    if len(unique) > 1 and unique[0] == unique[-1]:
        unique.pop()
    # A slice touching only an edge or a point contains no renderable surface.
    if len(unique) < 3 or sum(value * value for value in area_vector(unique)) < 1e-18:
        return ()
    return tuple(unique)


def texture_tiles(face):
    pieces = [face.vertices]
    for axis in ('u', 'v'):
        for boundary in range(math.floor(min(getattr(v, axis) for v in face.vertices)) + 1,
                              math.ceil(max(getattr(v, axis) for v in face.vertices))):
            pieces = [part for vertices in pieces
                      for part in (clip(vertices, axis, boundary, True),
                                   clip(vertices, axis, boundary, False)) if len(part) >= 3]
    result = []
    for vertices in pieces:
        tile_u = math.floor(sum(v.u for v in vertices) / len(vertices) + 1e-9)
        tile_v = math.floor(sum(v.v for v in vertices) / len(vertices) + 1e-9)
        result.append(Face(face.group, face.material,
                           tuple(Vertex(v.x, v.y, v.z, v.u - tile_u, v.v - tile_v) for v in vertices)))
    return result


def shell():
    # A continuous section from the underside, over the rounded lip, into the bowl.
    sections = [(1, 1, 15, 31, 2, 2),
                (0, 0, 16, 32, 3, 15.5),
                (.5, .5, 15.5, 31.5, 2.5, 16),
                (1.5, 1.5, 14.5, 30.5, 1.5, 16),
                (2, 2, 14, 30, 1, 15.5),
                (3, 3, 13, 29, 1, 4)]
    loops = [rounded_rectangle(*section[:5]) for section in sections]
    reference = loops[1]
    distances = [0.0]
    for p, q in zip(reference, reference[1:] + reference[:1]):
        distances.append(distances[-1] + math.dist(p, q))
    perimeter_u = [distance * 6 / distances[-1] for distance in distances]

    # Insert the same perimeter texture cuts in every ring, including the floor.
    samples = []
    for i, (u0, u1) in enumerate(zip(perimeter_u, perimeter_u[1:])):
        samples.append((i, 0.0, u0))
        for boundary in range(math.floor(u0) + 1, math.ceil(u1)):
            samples.append((i, (boundary - u0) / (u1 - u0), float(boundary)))

    section_v = [0.0]
    for a, b in zip(sections, sections[1:]):
        section_v.append(section_v[-1] + math.dist((a[2], a[5]), (b[2], b[5])) / 16)
    rings = []
    for section, loop, v in zip(sections, loops, section_v):
        vertices = []
        for i, t, u in samples:
            p, q = loop[i], loop[(i + 1) % len(loop)]
            vertices.append(Vertex(p[0] + (q[0] - p[0]) * t, section[5],
                                   p[1] + (q[1] - p[1]) * t, u, v))
        rings.append(vertices)
    faces = []
    for lower, upper in zip(rings, rings[1:]):
        for i in range(len(lower)):
            j = (i + 1) % len(lower)
            a, b, c, d = lower[i], upper[i], upper[j], lower[j]
            if j == 0:
                c = Vertex(c.x, c.y, c.z, 6, c.v)
                d = Vertex(d.x, d.y, d.z, 6, d.v)
            faces.extend(texture_tiles(Face('shell', 'body', (a, b, c, d))))
    for ring, upward, material in ((rings[0], False, 'base'), (rings[-1], True, 'body')):
        center = Vertex(8, ring[0].y, 16, .5, 1)
        for i in range(len(ring)):
            a, b = ring[i], ring[(i + 1) % len(ring)]
            a = Vertex(a.x, a.y, a.z, a.x / 16, a.z / 16)
            b = Vertex(b.x, b.y, b.z, b.x / 16, b.z / 16)
            faces.extend(texture_tiles(Face('shell', material, (center, b, a) if upward else (center, a, b))))
    return faces


def feet():
    faces = []
    for x, z in ((2, 2), (14, 2), (2, 30), (14, 30)):
        outward_x, outward_z = (-.2 if x < 8 else .2), (-.2 if z < 16 else .2)
        profiles = [(x + outward_x, z + outward_z, 1, .4, 0),
                    (x + outward_x, z + outward_z, 1, .4, .5),
                    (x, z, .4, .2, 2)]
        rings = []
        for cx, cz, half, radius, y in profiles:
            loop = rounded_rectangle(cx - half, cz - half, cx + half, cz + half, radius, 2)
            rings.append([Vertex(px, y, pz, index / len(loop), y / 2)
                          for index, (px, pz) in enumerate(loop)])
        for lower, upper in zip(rings, rings[1:]):
            for i in range(len(lower)):
                j = (i + 1) % len(lower)
                a, b, c, d = lower[i], upper[i], upper[j], lower[j]
                if j == 0:
                    c = Vertex(c.x, c.y, c.z, 1, c.v)
                    d = Vertex(d.x, d.y, d.z, 1, d.v)
                faces.append(Face('feet', 'feet', (a, b, c, d)))
        center = Vertex(x + outward_x, 0, z + outward_z, .5, .5)
        for i in range(len(rings[0])):
            edge = (rings[0][i], rings[0][(i + 1) % len(rings[0])])
            a, b = (Vertex(v.x, v.y, v.z, (v.x - (x + outward_x - 1)) / 2,
                           (v.z - (z + outward_z - 1)) / 2) for v in edge)
            faces.append(Face('feet', 'feet', (center, a, b)))
        # The open attachment at y=2 is inside the shell underside, with no offset cap.
    return faces


def faucet():
    # Exact box union: buttons touch the bar at y=17; shared internal faces vanish.
    boxes = [('faucet', (12.5, 15, 5), (15.5, 17, 15)),
             ('cold_button', (13, 17, 6), (15, 19, 8)),
             ('button_base', (13, 17, 9), (15, 19, 11)),
             ('hot_button', (13, 17, 12), (15, 19, 14))]
    axes = [sorted({box[end][axis] for box in boxes for end in (1, 2)}) for axis in range(3)]
    cells = {}
    for i in range(len(axes[0]) - 1):
        for j in range(len(axes[1]) - 1):
            for k in range(len(axes[2]) - 1):
                center = tuple((axes[a][index] + axes[a][index + 1]) / 2 for a, index in enumerate((i, j, k)))
                for material, low, high in boxes:
                    if all(low[a] < center[a] < high[a] for a in range(3)):
                        cells[i, j, k] = (material, low, high)
    faces = []
    for cell, (material, low, high) in cells.items():
        for axis in range(3):
            for sign in (-1, 1):
                neighbor = list(cell);neighbor[axis] += sign
                if tuple(neighbor) in cells:
                    continue
                other = [a for a in range(3) if a != axis]
                corners = []
                for u, v in ((0, 0), (1, 0), (1, 1), (0, 1)):
                    position = [0.0, 0.0, 0.0]
                    position[axis] = axes[axis][cell[axis] + (sign == 1)]
                    position[other[0]] = axes[other[0]][cell[other[0]] + u]
                    position[other[1]] = axes[other[1]][cell[other[1]] + v]
                    texture_u = (position[other[0]] - low[other[0]]) / (high[other[0]] - low[other[0]])
                    texture_v = (position[other[1]] - low[other[1]]) / (high[other[1]] - low[other[1]])
                    corners.append(Vertex(*position, texture_u, texture_v))
                n = normal(corners)
                if n[axis] * sign < 0:
                    corners.reverse()
                faces.append(Face('faucet', material, tuple(corners)))
    return faces


def area_vector(vertices):
    result = [0.0, 0.0, 0.0]
    for a, b in zip(vertices, vertices[1:] + vertices[:1]):
        result[0] += (a.y - b.y) * (a.z + b.z)
        result[1] += (a.z - b.z) * (a.x + b.x)
        result[2] += (a.x - b.x) * (a.y + b.y)
    return tuple(result)


def normal(vertices):
    cross = area_vector(vertices)
    length = math.sqrt(sum(value * value for value in cross))
    if length < 1e-10:
        raise ValueError('Degenerate model face')
    return tuple(value / length for value in cross)


def block_halves(faces):
    head, foot = [], []
    for face in faces:
        for target, below in ((head, True), (foot, False)):
            vertices = clip(face.vertices, 'z', 16, below)
            if len(vertices) >= 3:
                target.append(Face(face.group, face.material, vertices))
    return head, foot


def obj(faces, z_offset=0):
    vertices, coordinates, normals, records = {}, {}, {}, []
    def index(table, value):
        rounded = tuple(round(component, 8) for component in value)
        if rounded not in table:
            table[rounded] = len(table) + 1
        return table[rounded]
    # Forge 1.20 replaces a named group on every `g` declaration. Keep each
    # group contiguous and change only `usemtl` when its surface material changes.
    for face in sorted(faces, key=lambda face: face.group):
        n = normal(face.vertices)
        corners = []
        for vertex in face.vertices:
            corners.append((index(vertices, (vertex.x / 16, vertex.y / 16, (vertex.z - z_offset) / 16)),
                            index(coordinates, (vertex.u, vertex.v)), index(normals, n)))
        # Native OBJ models accept triangles and quads; split convex clipped n-gons.
        polygons = [corners] if len(corners) <= 4 else [[corners[0], corners[i], corners[i + 1]] for i in range(1, len(corners) - 1)]
        records.extend((face.group, face.material, polygon) for polygon in polygons)
    lines = ['# Generated by tools/generate_clawfoot_models.py; units are blocks.',
             'mtllib showercore:models/block/template/clawfoot/clawfoot.mtl']
    for prefix, table in (('v', vertices), ('vt', coordinates), ('vn', normals)):
        for value in table:
            lines.append(prefix + ' ' + ' '.join(f'{component:.8f}' for component in value))
    previous_group = None
    previous_material = None
    for group, material, polygon in records:
        if previous_group != group:
            lines.append('g ' + group)
            previous_group = group
        if previous_material != material:
            lines.append('usemtl ' + material)
            previous_material = material
        lines.append('f ' + ' '.join('/'.join(map(str, corner)) for corner in polygon))
    return '\n'.join(lines) + '\n', len(records)


def generate(project, output):
    version = next(line.split('=', 1)[1].strip() for line in (project / 'gradle.properties').read_text().splitlines() if line.startswith('minecraft_version='))
    loader = 'forge' if version == '1.20.1' else 'neoforge'
    template = output / 'assets/showercore/models/block/template'
    mesh = template / 'clawfoot';mesh.mkdir(parents=True, exist_ok=True)
    all_faces = shell() + feet() + faucet()
    head, foot = block_halves(all_faces)
    counts = {}
    for name, faces, offset in (('full', head + foot, 0), ('head', head, 0), ('foot', foot, 16)):
        data, counts[name] = obj(faces, offset)
        (mesh / f'{name}.obj').write_text(data, encoding='utf-8', newline='\n')
    material_names = ('body', 'base', 'feet', 'faucet', 'button_base', 'hot_button', 'cold_button')
    # Forge 1.20 defaults to tint index 0, which is the bathtub's water color.
    materials = '\n\n'.join(f'newmtl {name}\nKd 1 1 1\nforge_TintIndex {1 if name == "base" else -1}\nmap_Kd #{name}' for name in material_names) + '\n'
    (mesh / 'clawfoot.mtl').write_text(materials, encoding='utf-8', newline='\n')
    textures = {'base': 'minecraft:block/polished_deepslate', 'feet': 'minecraft:block/blackstone',
                'faucet': 'minecraft:block/polished_blackstone', 'button_base': 'minecraft:block/andesite',
                'hot_button': 'minecraft:block/redstone_block', 'cold_button': 'minecraft:block/lapis_block'}
    for filename, model in (('bathtub_clawfoot', 'full'), ('bathtub_clawfoot_head_empty', 'head'),
                            ('bathtub_clawfoot_head_faucet', 'head'), ('bathtub_clawfoot_foot_empty', 'foot')):
        data = {'credit': 'Continuous hollow shell generated by tools/generate_clawfoot_models.py',
                'loader': loader + ':obj', 'model': f'showercore:models/block/template/clawfoot/{model}.obj',
                'automatic_culling': True, 'shade_quads': True, 'flip_v': False,
                'emissive_ambient': False, 'render_type': 'cutout', 'textures': textures}
        (template / f'{filename}.json').write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps({'minecraftVersion': version, 'meshFaceCounts': counts, 'output': str(output)}, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    generate(args.project, args.output or args.project / 'src/main/resources')
