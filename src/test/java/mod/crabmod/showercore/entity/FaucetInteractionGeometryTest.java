package mod.crabmod.showercore.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaucetInteractionGeometryTest {
    private static final double EPS = 0.0001D;
    private static final Path TEMPLATE_DIR = Paths.get(
            "src", "main", "resources", "assets", "showercore", "models", "block", "template");

    @Test
    @DisplayName("Legacy faucet interaction hitbox covers the old short-end faucet")
    void legacyHitboxCoversLegacyFaucetModel() throws IOException {
        JsonObject model = readTemplate("bathtub_head_faucet.json");
        double[][] hitboxes = FaucetInteractionGeometry.hitboxOrigins(false);

        assertEquals(1, hitboxes.length, "Legacy bathtubs should keep one short-end faucet hitbox.");
        assertCoveredByHitboxUnion(model.getAsJsonArray("elements").get(4).getAsJsonObject(), hitboxes);
        assertCoveredByHitboxUnion(model.getAsJsonArray("elements").get(5).getAsJsonObject(), hitboxes);
    }

    @Test
    @DisplayName("Clawfoot faucet interaction hitboxes cover faucet body and all three controls")
    void clawfootHitboxesCoverEveryControlModelElement() throws IOException {
        double[][] hitboxes = FaucetInteractionGeometry.hitboxOrigins(true);
        assertEquals(3, hitboxes.length, "Clawfoot controls need three click targets.");
        java.util.List<double[]> positions = new java.util.ArrayList<>();
        boolean faucet = false;
        int observedFaces = 0;
        for (String line : Files.readAllLines(TEMPLATE_DIR.resolve("clawfoot/head.obj"))) {
            String[] fields = line.trim().split("\\s+");
            if (fields[0].equals("v")) {
                positions.add(new double[]{Double.parseDouble(fields[1]), Double.parseDouble(fields[2]), Double.parseDouble(fields[3])});
            } else if (fields[0].equals("g")) {
                faucet = fields[1].equals("faucet");
            } else if (faucet && fields[0].equals("f")) {
                double[] center = new double[3];
                for (int i = 1; i < fields.length; i++) {
                    double[] point = positions.get(Integer.parseInt(fields[i].split("/")[0]) - 1);
                    assertPointCovered(point, hitboxes);
                    for (int axis = 0; axis < 3; axis++) center[axis] += point[axis] / (fields.length - 1);
                }
                assertPointCovered(center, hitboxes);
                observedFaces++;
            }
        }
        assertTrue(observedFaces > 0, "Faucet mesh must contain tested surfaces");
    }

    @Test
    @DisplayName("Clawfoot click targets stay anchored in the bathtub block while reaching tall controls")
    void clawfootHitboxesStayInHeadBlockAndReachAboveRimControls() {
        for (double[] hitbox : FaucetInteractionGeometry.hitboxOrigins(true)) {
            assertTrue(hitbox[1] < 1.0D,
                    "Entity y origin must stay below the next block so blockPosition() resolves to the bathtub.");
            assertTrue(hitbox[1] + FaucetInteractionGeometry.ENTITY_HEIGHT >= 19.0625D / 16.0D - EPS,
                    "Entity height must reach the top of the clawfoot button models.");
        }
    }

    @Test
    @DisplayName("Faucet entity registration uses the shared geometry constants")
    void entityRegistrationUsesSharedGeometryConstants() throws IOException {
        String source = readSource(Paths.get(
                "src", "main", "java", "mod", "crabmod", "showercore", "registers", "EntityRegister.java"));

        assertTrue(source.contains("FaucetInteractionGeometry.ENTITY_WIDTH"),
                "Faucet EntityType width must come from FaucetInteractionGeometry.");
        assertTrue(source.contains("FaucetInteractionGeometry.ENTITY_HEIGHT"),
                "Faucet EntityType height must come from FaucetInteractionGeometry.");
    }

    @Test
    @DisplayName("Bathtub placement selects clawfoot hitboxes by block id family")
    void placementSelectsClawfootHitboxesByBlockId() throws IOException {
        String source = readSource(Paths.get(
                "src", "main", "java", "mod", "crabmod", "showercore", "block", "BathtubBlock.java"));

        assertTrue(source.contains("spawnFaucetInteractionEntities(level, blockpos, state)"),
                "Bathtub placement must centralize faucet entity spawning.");
        assertTrue(source.contains("FaucetInteractionGeometry.hitboxOrigins(isClawfootBathtub(state))"),
                "Clawfoot bathtubs must use the side-rim interaction hitboxes.");
        assertFalse(source.contains("case NORTH: x += 0.5; y += 0.78; z += 0.125"),
                "The old single hard-coded hitbox misses clawfoot side controls.");
    }

    @Test
    @DisplayName("Legacy faucet target keeps the old horizontal placement for every facing")
    void legacyHitboxKeepsLegacyHorizontalPlacementForEveryFacing() {
        double[] legacy = FaucetInteractionGeometry.hitboxOrigins(false)[0];

        assertWorldOrigin(1, 0, 0, 1, legacy, 10.5D, 20.6875D, 30.125D);
        assertWorldOrigin(-1, 0, 0, -1, legacy, 10.5D, 20.6875D, 30.875D);
        assertWorldOrigin(0, -1, 1, 0, legacy, 10.125D, 20.6875D, 30.5D);
        assertWorldOrigin(0, 1, -1, 0, legacy, 10.875D, 20.6875D, 30.5D);
    }

    @Test
    @DisplayName("Clawfoot faucet targets resolve to the head block in every facing")
    void clawfootHitboxOriginsResolveToHeadBlockForEveryFacing() {
        int[][] facings = {
                {1, 0, 0, 1},
                {-1, 0, 0, -1},
                {0, -1, 1, 0},
                {0, 1, -1, 0}
        };
        for (int[] facing : facings) {
            for (double[] hitbox : FaucetInteractionGeometry.hitboxOrigins(true)) {
                assertEquals(10,
                        (int) Math.floor(FaucetInteractionGeometry.worldX(10, facing[0], facing[2], hitbox[0], hitbox[2])));
                assertEquals(20, (int) Math.floor(FaucetInteractionGeometry.worldY(20, hitbox[1])));
                assertEquals(30,
                        (int) Math.floor(FaucetInteractionGeometry.worldZ(30, facing[1], facing[3], hitbox[0], hitbox[2])));
            }
        }
    }

    private static JsonObject readTemplate(String filename) throws IOException {
        return JsonParser.parseString(readSource(TEMPLATE_DIR.resolve(filename))).getAsJsonObject();
    }

    private static String readSource(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void assertPointCovered(double[] point, double[][] hitboxes) {
        double half = FaucetInteractionGeometry.ENTITY_WIDTH / 2;
        assertTrue(java.util.Arrays.stream(hitboxes).anyMatch(box ->
                        point[0] >= box[0] - half - EPS && point[0] <= box[0] + half + EPS
                        && point[1] >= box[1] - EPS && point[1] <= box[1] + FaucetInteractionGeometry.ENTITY_HEIGHT + EPS
                        && point[2] >= box[2] - half - EPS && point[2] <= box[2] + half + EPS),
                "Faucet surface outside clickable targets: " + java.util.Arrays.toString(point));
    }

    private static void assertCoveredByHitboxUnion(JsonObject modelBox, double[][] hitboxes) {
        if (covers(modelBox, hitboxes)) {
            return;
        }
        throw new AssertionError("No faucet interaction hitbox covers model box from "
                + modelBox.getAsJsonArray("from") + " to " + modelBox.getAsJsonArray("to"));
    }

    private static boolean covers(JsonObject modelBox, double[][] hitboxes) {
        double halfWidth = FaucetInteractionGeometry.ENTITY_WIDTH / 2.0D;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (double[] hitbox : hitboxes) {
            minX = Math.min(minX, hitbox[0] - halfWidth);
            minY = Math.min(minY, hitbox[1]);
            minZ = Math.min(minZ, hitbox[2] - halfWidth);
            maxX = Math.max(maxX, hitbox[0] + halfWidth);
            maxY = Math.max(maxY, hitbox[1] + FaucetInteractionGeometry.ENTITY_HEIGHT);
            maxZ = Math.max(maxZ, hitbox[2] + halfWidth);
        }
        double fromX = modelBox.getAsJsonArray("from").get(0).getAsDouble() / 16.0D;
        double fromY = modelBox.getAsJsonArray("from").get(1).getAsDouble() / 16.0D;
        double fromZ = modelBox.getAsJsonArray("from").get(2).getAsDouble() / 16.0D;
        double toX = modelBox.getAsJsonArray("to").get(0).getAsDouble() / 16.0D;
        double toY = modelBox.getAsJsonArray("to").get(1).getAsDouble() / 16.0D;
        double toZ = modelBox.getAsJsonArray("to").get(2).getAsDouble() / 16.0D;

        return minX <= fromX + EPS
                && maxX >= toX - EPS
                && minY <= fromY + EPS
                && maxY >= toY - EPS
                && minZ <= fromZ + EPS
                && maxZ >= toZ - EPS;
    }

    private static void assertWorldOrigin(
            int rightStepX, int rightStepZ, int towardFootStepX, int towardFootStepZ,
            double[] hitbox, double expectedX, double expectedY, double expectedZ) {
        assertEquals(expectedX,
                FaucetInteractionGeometry.worldX(10, rightStepX, towardFootStepX, hitbox[0], hitbox[2]), EPS);
        assertEquals(expectedY, FaucetInteractionGeometry.worldY(20, hitbox[1]), EPS);
        assertEquals(expectedZ,
                FaucetInteractionGeometry.worldZ(30, rightStepZ, towardFootStepZ, hitbox[0], hitbox[2]), EPS);
    }
}
