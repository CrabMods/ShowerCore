package mod.crabmod.showercore.client.renderer;

import mod.crabmod.showercore.client.renderer.BathtubDropGeometry.FaucetSide;
import mod.crabmod.showercore.block.BathtubWaterGeometry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the faucet-drop visual fix in {@link BathtubBlockEntityRenderer}.
 *
 * Bug fix under test: when a bathtub contains a CUSTOM fluid and the faucet is opened
 * (RUNNING=true), a small water-drop cube must be drawn at the faucet spout even though
 * the blockstate picks the "head_empty" model (which has no running-water geometry).
 *
 * These tests exercise the pure geometry + gating + color-resolution helpers extracted
 * into {@link BathtubDropGeometry} so they can run without a Minecraft client.
 */
class BathtubBlockEntityRendererTest {

    private static final float EPS = 1e-6f;
    private static final float EXPECTED_FY1 = BathtubWaterGeometry.DEFAULT_WATER_LEVEL;
    private static final float EXPECTED_FY2 = 11f / 16f;
    private static final float CLAWFOOT_FY1 = BathtubWaterGeometry.CLAWFOOT_WATER_LEVEL;
    private static final float CLAWFOOT_FY2 = 15f / 16f;

    // ---- Geometry tests (one per facing) ---------------------------------

    @Test
    @DisplayName("NORTH-facing head: drop sits at x [7,9]/16, z [3,4]/16")
    void dropBounds_north() {
        float[] b = BathtubDropGeometry.computeDropBounds(FaucetSide.NORTH);
        assertBounds(b, 7f / 16f, 9f / 16f, 3f / 16f, 4f / 16f);
    }

    @Test
    @DisplayName("SOUTH-facing head: drop mirrors to z [12,13]/16")
    void dropBounds_south() {
        float[] b = BathtubDropGeometry.computeDropBounds(FaucetSide.SOUTH);
        assertBounds(b, 7f / 16f, 9f / 16f, 12f / 16f, 13f / 16f);
    }

    @Test
    @DisplayName("EAST-facing head: drop rotates to x [12,13]/16, z [7,9]/16")
    void dropBounds_east() {
        float[] b = BathtubDropGeometry.computeDropBounds(FaucetSide.EAST);
        assertBounds(b, 12f / 16f, 13f / 16f, 7f / 16f, 9f / 16f);
    }

    @Test
    @DisplayName("WEST-facing head: drop rotates to x [3,4]/16, z [7,9]/16")
    void dropBounds_west() {
        float[] b = BathtubDropGeometry.computeDropBounds(FaucetSide.WEST);
        assertBounds(b, 3f / 16f, 4f / 16f, 7f / 16f, 9f / 16f);
    }

    @Test
    @DisplayName("Y bounds extend legacy drops from water surface to faucet spout")
    void dropBounds_yConstantAcrossFacings() {
        for (FaucetSide s : FaucetSide.values()) {
            float[] b = BathtubDropGeometry.computeDropBounds(s);
            assertEquals(EXPECTED_FY1, b[1], EPS, "fy1 for " + s);
            assertEquals(EXPECTED_FY2, b[4], EPS, "fy2 for " + s);
        }
    }

    @Test
    @DisplayName("Drop footprint is 2x1 (px) in the horizontal plane for every facing")
    void dropBounds_dimensionsInvariant() {
        for (FaucetSide s : FaucetSide.values()) {
            float[] b = BathtubDropGeometry.computeDropBounds(s);
            float dx = b[3] - b[0];
            float dy = b[4] - b[1];
            float dz = b[5] - b[2];
            boolean wideX = Math.abs(dx - 2f / 16f) < EPS && Math.abs(dz - 1f / 16f) < EPS;
            boolean wideZ = Math.abs(dz - 2f / 16f) < EPS && Math.abs(dx - 1f / 16f) < EPS;
            assertTrue(wideX || wideZ, "Unexpected footprint for " + s + ": dx=" + dx + " dz=" + dz);
            assertEquals(EXPECTED_FY2 - EXPECTED_FY1, dy, EPS, "height for " + s);
        }
    }

    @Test
    @DisplayName("Drop extends from legacy water surface to faucet spout")
    void dropBounds_yMatchesModelElement() {
        float[] b = BathtubDropGeometry.computeDropBounds(FaucetSide.NORTH);
        assertEquals(BathtubWaterGeometry.DEFAULT_WATER_LEVEL, b[1], EPS);
        assertEquals(11f / 16f, b[4], EPS);
    }

    @Test
    @DisplayName("Legacy overload preserves old NORTH-facing short-end drop bounds")
    void legacyOverloadPreservesOldDropBounds() {
        float[] oldApi = BathtubDropGeometry.computeDropBounds(FaucetSide.NORTH);
        float[] newApi = BathtubDropGeometry.computeDropBounds(FaucetSide.NORTH, false);
        assertArrayEquals(oldApi, newApi, EPS);
    }

    @Test
    @DisplayName("Clawfoot NORTH-facing tub: drop sits under the right-rim faucet")
    void clawfootDropBounds_rightSideEast() {
        float[] b = BathtubDropGeometry.computeDropBounds(FaucetSide.EAST, true);
        assertBounds(b, CLAWFOOT_FY1, CLAWFOOT_FY2,
                12.5f / 16f, 13.5f / 16f, 9f / 16f, 11f / 16f);
    }

    @Test
    @DisplayName("Clawfoot EAST-facing tub: right-rim drop rotates to the south side")
    void clawfootDropBounds_rightSideSouth() {
        float[] b = BathtubDropGeometry.computeDropBounds(FaucetSide.SOUTH, true);
        assertBounds(b, CLAWFOOT_FY1, CLAWFOOT_FY2,
                5f / 16f, 7f / 16f, 12.5f / 16f, 13.5f / 16f);
    }

    @Test
    @DisplayName("Clawfoot SOUTH-facing tub: right-rim drop rotates to the west side")
    void clawfootDropBounds_rightSideWest() {
        float[] b = BathtubDropGeometry.computeDropBounds(FaucetSide.WEST, true);
        assertBounds(b, CLAWFOOT_FY1, CLAWFOOT_FY2,
                2.5f / 16f, 3.5f / 16f, 5f / 16f, 7f / 16f);
    }

    @Test
    @DisplayName("Clawfoot WEST-facing tub: right-rim drop rotates to the north side")
    void clawfootDropBounds_rightSideNorth() {
        float[] b = BathtubDropGeometry.computeDropBounds(FaucetSide.NORTH, true);
        assertBounds(b, CLAWFOOT_FY1, CLAWFOOT_FY2,
                9f / 16f, 11f / 16f, 2.5f / 16f, 3.5f / 16f);
    }

    // ---- Gating tests: no-render for RUNNING=false or FOOT ----------------

    @Test
    @DisplayName("Drop does NOT render when RUNNING=false, even on HEAD")
    void shouldRenderDrop_falseWhenNotRunning() {
        assertFalse(BathtubDropGeometry.shouldRenderDrop(true, false));
    }

    @Test
    @DisplayName("Drop does NOT render on FOOT part even when RUNNING=true")
    void shouldRenderDrop_falseForFoot() {
        assertFalse(BathtubDropGeometry.shouldRenderDrop(false, true));
    }

    @Test
    @DisplayName("Drop does NOT render on FOOT when RUNNING=false")
    void shouldRenderDrop_falseForFootNotRunning() {
        assertFalse(BathtubDropGeometry.shouldRenderDrop(false, false));
    }

    @Test
    @DisplayName("Drop renders only when HEAD and RUNNING=true")
    void shouldRenderDrop_trueForHeadRunning() {
        assertTrue(BathtubDropGeometry.shouldRenderDrop(true, true));
    }

    // ---- Helpers ----------------------------------------------------------

    private static void assertBounds(float[] b, float expectedFx1, float expectedFx2,
                                     float expectedFz1, float expectedFz2) {
        assertBounds(b, EXPECTED_FY1, EXPECTED_FY2, expectedFx1, expectedFx2, expectedFz1, expectedFz2);
    }

    private static void assertBounds(float[] b, float expectedFy1, float expectedFy2,
                                     float expectedFx1, float expectedFx2,
                                     float expectedFz1, float expectedFz2) {
        assertEquals(6, b.length, "bounds array must be {fx1,fy1,fz1,fx2,fy2,fz2}");
        float[] expected = {expectedFx1, expectedFy1, expectedFz1, expectedFx2, expectedFy2, expectedFz2};
        assertArrayEquals(expected, b, EPS,
                () -> "expected " + java.util.Arrays.toString(expected) + " got " + java.util.Arrays.toString(b));
    }
}
