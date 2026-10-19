package mod.crabmod.showercore.client.renderer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Surface contracts, independent of the model generator's implementation. */
class BathtubClawfootModelRegressionTest {
    private static final Path ROOT = Path.of("src/main/resources/assets/showercore/models/block/template");
    private static final double EPS = 2e-7;
    private static final String[] TEMPLATES = {"bathtub_clawfoot", "bathtub_clawfoot_head_empty",
            "bathtub_clawfoot_head_faucet", "bathtub_clawfoot_foot_empty"};

    private record Point(double x, double y, double z) {
        Point translate(double zOffset) { return new Point(x, y, z + zOffset); }
        String key() { return String.format(Locale.ROOT, "%.7f,%.7f,%.7f", x, y, z); }
    }
    private record Corner(Point position, double u, double v, Point normal) {}
    private record Face(String group, String material, List<Corner> corners) {}

    private static JsonObject template(String name) throws IOException {
        return JsonParser.parseString(Files.readString(ROOT.resolve(name + ".json"))).getAsJsonObject();
    }

    private static List<Face> mesh(String name) throws IOException {
        List<Point> positions = new ArrayList<>(), normals = new ArrayList<>();
        List<double[]> uvs = new ArrayList<>();
        List<Face> faces = new ArrayList<>();
        String group = "", material = "";
        for (String line : Files.readAllLines(ROOT.resolve("clawfoot/" + name + ".obj"))) {
            String[] fields = line.trim().split("\\s+");
            switch (fields[0]) {
                case "v", "vn" -> {
                    Point point = new Point(Double.parseDouble(fields[1]), Double.parseDouble(fields[2]), Double.parseDouble(fields[3]));
                    (fields[0].equals("v") ? positions : normals).add(point);
                }
                case "vt" -> uvs.add(new double[]{Double.parseDouble(fields[1]), Double.parseDouble(fields[2])});
                case "g" -> group = fields[1];
                case "usemtl" -> material = fields[1];
                case "f" -> {
                    List<Corner> corners = new ArrayList<>();
                    for (int i = 1; i < fields.length; i++) {
                        String[] indices = fields[i].split("/");
                        double[] uv = uvs.get(Integer.parseInt(indices[1]) - 1);
                        corners.add(new Corner(positions.get(Integer.parseInt(indices[0]) - 1), uv[0], uv[1],
                                normals.get(Integer.parseInt(indices[2]) - 1)));
                    }
                    faces.add(new Face(group, material, corners));
                }
                default -> { }
            }
        }
        assertFalse(faces.isEmpty(), name);
        return faces;
    }

    @Test
    void nativeLoaderPreservesCutoutAndNonEmissiveMaterials() throws IOException {
        String version = Files.readString(Path.of("gradle.properties"));
        String loader = version.contains("minecraft_version=1.20.1") ? "forge:obj" : "neoforge:obj";
        for (String name : TEMPLATES) {
            JsonObject model = template(name);
            assertEquals(loader, model.get("loader").getAsString());
            assertEquals("cutout", model.get("render_type").getAsString());
            assertFalse(model.get("emissive_ambient").getAsBoolean());
            assertFalse(model.has("elements"), "No overlapping rotated-box corner patches");
        }
        assertEquals(template(TEMPLATES[1]), template(TEMPLATES[2]), "Running water must not select a different shell");
    }

    @Test
    void exteriorShellIsClosedAndHalvesOpenOnlyAtTheirJoin() throws IOException {
        for (String name : List.of("full", "head", "foot")) {
            Map<String, Integer> counts = new HashMap<>();
            Map<String, List<Point>> endpoints = new HashMap<>();
            for (Face face : mesh(name)) {
                if (!face.group.equals("shell")) continue;
                for (int i = 0; i < face.corners.size(); i++) {
                    Point a = face.corners.get(i).position, b = face.corners.get((i + 1) % face.corners.size()).position;
                    String edge = a.key().compareTo(b.key()) < 0 ? a.key() + "/" + b.key() : b.key() + "/" + a.key();
                    counts.merge(edge, 1, Integer::sum); endpoints.put(edge, List.of(a, b));
                }
            }
            int boundaryEdges = 0;
            for (var edge : counts.entrySet()) {
                if (edge.getValue() == 2) continue;
                assertEquals(1, edge.getValue(), "Non-manifold shell: " + edge.getKey());
                assertNotEquals("full", name, "A visible gap remains in the full shell");
                double plane = name.equals("head") ? 1 : 0;
                for (Point point : endpoints.get(edge.getKey())) assertEquals(plane, point.z, EPS, "Crack away from joining plane");
                boundaryEdges++;
            }
            if (!name.equals("full")) assertTrue(boundaryEdges > 0);
        }
    }

    @Test
    void everyExteriorCornerIsConvexSymmetricAndContinuousToUnderside() throws IOException {
        List<Point> points = mesh("full").stream().filter(f -> f.group.equals("shell"))
                .flatMap(f -> f.corners.stream()).map(Corner::position).toList();
        // All four exterior profiles are reflections of the same outward arc.
        for (double y : new double[]{.125, .96875, 1}) {
            List<Point> ring = points.stream().filter(p -> Math.abs(p.y - y) < EPS).distinct().toList();
            double lo = y == .125 ? .0625 : y == 1 ? .03125 : 0;
            double radius = y == .125 ? .125 : y == 1 ? .15625 : .1875;
            double cx = lo + radius;
            // The eight-segment exterior circle must exist through the full wall height.
            for (int step = 0; step <= 8; step++) {
                double angle = Math.PI + step * Math.PI / 16;
                double x = cx + radius * Math.cos(angle), z = cx + radius * Math.sin(angle);
                for (int corner = 0; corner < 4; corner++) {
                    double expectedX = (corner & 1) == 0 ? x : 1 - x;
                    double expectedZ = (corner & 2) == 0 ? z : 2 - z;
                    assertTrue(ring.stream().anyMatch(p -> Math.abs(p.x - expectedX) < EPS && Math.abs(p.z - expectedZ) < EPS),
                            "Missing outward corner surface at y=" + y + ", angle=" + angle + ", corner=" + corner);
                }
            }
        }
        assertEquals(1, points.stream().mapToDouble(Point::y).max().orElseThrow(), EPS, "No lifted corner patches");
    }

    @Test
    void itemIsExactlyThePlacedHalvesIncludingFaucet() throws IOException {
        List<Face> full = mesh("full"), head = mesh("head"), foot = mesh("foot");
        assertEquals(head.size() + foot.size(), full.size());
        List<Face> joined = new ArrayList<>(head);
        for (Face face : foot) joined.add(new Face(face.group, face.material, face.corners.stream()
                .map(c -> new Corner(c.position.translate(1), c.u, c.v, c.normal)).toList()));
        joined.sort(Comparator.comparing(Face::group));
        for (int i = 0; i < full.size(); i++) {
            Face expected = joined.get(i);
            Face actual = full.get(i);
            assertEquals(expected.group, actual.group); assertEquals(expected.material, actual.material);
            assertEquals(expected.corners.size(), actual.corners.size());
            for (int j = 0; j < actual.corners.size(); j++) {
                Corner a = actual.corners.get(j), b = expected.corners.get(j);
                Point translated = b.position;
                assertEquals(translated.x, a.position.x, EPS); assertEquals(translated.y, a.position.y, EPS);
                assertEquals(translated.z, a.position.z, EPS); assertEquals(b.u, a.u, EPS); assertEquals(b.v, a.v, EPS);
                assertEquals(b.normal, a.normal);
            }
        }
        assertFalse(head.stream().noneMatch(f -> f.group.equals("faucet")));
        assertTrue(foot.stream().noneMatch(f -> f.group.equals("faucet")), "Controls must stay on the head");
    }

    @Test
    void objGroupsAreDeclaredOnceForForgeLoaderCompatibility() throws IOException {
        for (String name : List.of("full", "head", "foot")) {
            Set<String> groups = new HashSet<>();
            for (String line : Files.readAllLines(ROOT.resolve("clawfoot/" + name + ".obj"))) {
                if (line.startsWith("g ")) assertTrue(groups.add(line), "Repeated OBJ group discards earlier faces in Forge 1.20: " + line);
            }
            assertFalse(groups.isEmpty());
        }
    }

    @Test
    void exportedFacesHaveValidUvsNormalsAndWinding() throws IOException {
        for (String name : List.of("full", "head", "foot")) for (Face face : mesh(name)) {
            assertTrue(face.corners.size() == 3 || face.corners.size() == 4);
            double nx = 0, ny = 0, nz = 0;
            for (int i = 0; i < face.corners.size(); i++) {
                Corner corner = face.corners.get(i);
                assertTrue(Double.isFinite(corner.u) && corner.u >= -EPS && corner.u <= 1 + EPS);
                assertTrue(Double.isFinite(corner.v) && corner.v >= -EPS && corner.v <= 1 + EPS);
                Point a = corner.position, b = face.corners.get((i + 1) % face.corners.size()).position;
                nx += (a.y - b.y) * (a.z + b.z); ny += (a.z - b.z) * (a.x + b.x); nz += (a.x - b.x) * (a.y + b.y);
            }
            Point normal = face.corners.get(0).normal;
            assertEquals(1, normal.x * normal.x + normal.y * normal.y + normal.z * normal.z, 1e-6);
            assertTrue(nx * normal.x + ny * normal.y + nz * normal.z > 1e-12, "Inverted or zero-area face");
        }
        assertTrue(mesh("full").size() <= 600, "Keep the shared mesh bounded");
    }

    @Test
    void decorationsStayFixedAndAllBodySurfacesUseTheMaterial() throws IOException {
        String mtl = Files.readString(ROOT.resolve("clawfoot/clawfoot.mtl"));
        assertEquals(7, mtl.lines().filter(line -> Set.of("forge_TintIndex -1", "forge_TintIndex 1").contains(line)).count(),
                "All seven materials must explicitly opt out of water tint 0");
        assertTrue(mesh("full").stream().filter(face -> face.group.equals("shell"))
                .allMatch(face -> Set.of("body", "base").contains(face.material)),
                "The entire shell, rim and bowl must use customizable body surfaces");
        for (String slot : List.of("body", "base", "feet", "faucet", "button_base", "hot_button", "cold_button"))
            assertTrue(mtl.lines().anyMatch(line -> line.equals("map_Kd #" + slot)));
        for (String name : TEMPLATES) {
            JsonObject textures = template(name).getAsJsonObject("textures");
            assertEquals("minecraft:block/polished_deepslate", textures.get("base").getAsString());
            assertEquals("minecraft:block/blackstone", textures.get("feet").getAsString());
            assertEquals("minecraft:block/polished_blackstone", textures.get("faucet").getAsString());
            assertEquals("minecraft:block/andesite", textures.get("button_base").getAsString());
            assertEquals("minecraft:block/redstone_block", textures.get("hot_button").getAsString());
            assertEquals("minecraft:block/lapis_block", textures.get("cold_button").getAsString());
        }
        assertTrue(mtl.contains("newmtl base\nKd 1 1 1\nforge_TintIndex 1\nmap_Kd #base"), "Underside marker must preserve its legacy texture while allowing whole-body customization");
        List<Face> floor = mesh("full").stream().filter(f -> f.group.equals("shell") && f.corners.stream().allMatch(c -> Math.abs(c.position.y - .25) < EPS)).toList();
        assertFalse(floor.isEmpty(), "Bowl floor must exist");
        assertTrue(floor.stream().allMatch(f -> f.material.equals("body")), "Bowl floor must share the body material");
    }

    @Test
    void controlsTouchTheBarWithoutFloatingOrInternalCoplanarFaces() throws IOException {
        List<Face> faces = mesh("head").stream().filter(f -> f.group.equals("faucet")).toList();
        for (String material : List.of("cold_button", "button_base", "hot_button")) {
            List<Point> points = faces.stream().filter(f -> f.material.equals(material)).flatMap(f -> f.corners.stream()).map(Corner::position).toList();
            assertFalse(points.isEmpty());
            assertEquals(17.0 / 16, points.stream().mapToDouble(Point::y).min().orElseThrow(), EPS);
            assertEquals(13.0 / 16, points.stream().mapToDouble(Point::x).min().orElseThrow(), EPS);
            assertEquals(15.0 / 16, points.stream().mapToDouble(Point::x).max().orElseThrow(), EPS);
            assertTrue(faces.stream().filter(f -> f.material.equals(material)).noneMatch(f -> f.corners.stream().allMatch(c -> Math.abs(c.position.y - 17.0 / 16) < EPS)),
                    "Button underside must not share an internal face with the bar");
        }
    }
}
