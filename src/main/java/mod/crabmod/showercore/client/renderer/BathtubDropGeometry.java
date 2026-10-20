package mod.crabmod.showercore.client.renderer;

import mod.crabmod.showercore.block.BathtubWaterGeometry;

/**
 * Pure-math helpers for the running-faucet drop visual drawn by
 * {@link BathtubBlockEntityRenderer} when a bathtub has liquid and
 * RUNNING=true.
 *
 * This class has no Minecraft dependencies so its logic can be unit-tested
 * without a live client. The renderer adapts {@code net.minecraft.core.Direction}
 * and {@code BedPart} values to the simple enum/boolean inputs here.
 *
 * Coordinates are block-local (0..1). Legacy bathtubs mirror the geometry of
 * the bathtub_*_head_running model element at [7,10,3]..[9,11,4] (1/16 units).
 * Clawfoot bathtubs put the faucet on the right rim, so their drop is computed
 * from the split clawfoot model's local side-rim coordinates instead.
 */
public final class BathtubDropGeometry {

    private BathtubDropGeometry() {}

    /** Cardinal horizontal direction of the faucet face on the bathtub HEAD. */
    public enum FaucetSide {
        NORTH, SOUTH, EAST, WEST
    }

    /**
     * Gating predicate: the drop visual only renders on the HEAD half
     * (the half with the faucet) while the faucet is actively running.
     */
    public static boolean shouldRenderDrop(boolean isHead, boolean running) {
        return isHead && running;
    }

    /**
     * Compute the AABB {fx1, fy1, fz1, fx2, fy2, fz2} of the drop cube
     * for the given faucet side, mirroring the head_running model element.
     */
    public static float[] computeDropBounds(FaucetSide faucetSide) {
        return computeDropBounds(faucetSide, false);
    }

    /**
     * Compute the AABB {fx1, fy1, fz1, fx2, fy2, fz2} of the drop cube.
     *
     * <p>For legacy bathtubs, {@code faucetSide} is the short side containing
     * the faucet. For clawfoot bathtubs, {@code faucetSide} is the right side of
     * the bathtub (the side containing the side-rim faucet).
     */
    public static float[] computeDropBounds(FaucetSide faucetSide, boolean clawfoot) {
        return clawfoot ? computeClawfootDropBounds(faucetSide) : computeLegacyDropBounds(faucetSide);
    }

    /** Profile coordinates use head-local right and toward-foot axes, in pixels. */
    public static float[] computeDropBounds(FaucetSide facing, mod.crabmod.showercore.geometry.BathroomGeometry.Tub tub) {
        var drop = tub.drop();
        FaucetSide right = clockWise(facing), foot = clockWise(right);
        float minX = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (double x : new double[]{drop.x1(),drop.x2()}) for(double z : new double[]{drop.z1(),drop.z2()}) {
            float wx = blockLocalX(right,foot,(float)x/16,(float)z/16);
            float wz = blockLocalZ(right,foot,(float)x/16,(float)z/16);
            minX=Math.min(minX,wx); maxX=Math.max(maxX,wx); minZ=Math.min(minZ,wz); maxZ=Math.max(maxZ,wz);
        }
        return new float[]{minX,tub.waterLevel(),minZ,maxX,(float)drop.topY()/16,maxZ};
    }

    private static float[] computeLegacyDropBounds(FaucetSide faucetSide) {
        final float dropHalfWidth = 1f / 16f;   // 2px wide centered on the spout (model: x 7..9)
        final float dropDepth = 1f / 16f;       // 1px deep (model: z 3..4)
        final float dropOffset = 3f / 16f;      // spout is 3px in from the wall
        final float fy1 = BathtubWaterGeometry.DEFAULT_WATER_LEVEL;
        final float fy2 = 11f / 16f;
        float fx1, fx2, fz1, fz2;
        switch (faucetSide) {
            case NORTH:
                fx1 = 0.5f - dropHalfWidth; fx2 = 0.5f + dropHalfWidth;
                fz1 = dropOffset;          fz2 = dropOffset + dropDepth;
                break;
            case SOUTH:
                fx1 = 0.5f - dropHalfWidth; fx2 = 0.5f + dropHalfWidth;
                fz1 = 1f - dropOffset - dropDepth; fz2 = 1f - dropOffset;
                break;
            case WEST:
                fx1 = dropOffset;          fx2 = dropOffset + dropDepth;
                fz1 = 0.5f - dropHalfWidth; fz2 = 0.5f + dropHalfWidth;
                break;
            case EAST:
            default:
                fx1 = 1f - dropOffset - dropDepth; fx2 = 1f - dropOffset;
                fz1 = 0.5f - dropHalfWidth; fz2 = 0.5f + dropHalfWidth;
                break;
        }
        return new float[] { fx1, fy1, fz1, fx2, fy2, fz2 };
    }

    private static float[] computeClawfootDropBounds(FaucetSide faucetSide) {
        final float localRight1 = 12.5f / 16f;
        final float localRight2 = 13.5f / 16f;
        final float localTowardFoot1 = 9f / 16f;
        final float localTowardFoot2 = 11f / 16f;
        final float fy1 = BathtubWaterGeometry.CLAWFOOT_WATER_LEVEL;
        final float fy2 = 15f / 16f;

        FaucetSide towardFoot = clockWise(faucetSide);
        float minX = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (float localRight : new float[] { localRight1, localRight2 }) {
            for (float localTowardFoot : new float[] { localTowardFoot1, localTowardFoot2 }) {
                float x = blockLocalX(faucetSide, towardFoot, localRight, localTowardFoot);
                float z = blockLocalZ(faucetSide, towardFoot, localRight, localTowardFoot);
                minX = Math.min(minX, x);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxZ = Math.max(maxZ, z);
            }
        }

        return new float[] { minX, fy1, minZ, maxX, fy2, maxZ };
    }

    private static float blockLocalX(FaucetSide right, FaucetSide towardFoot, float localRight, float localTowardFoot) {
        return 0.5f
                + stepX(right) * (localRight - 0.5f)
                + stepX(towardFoot) * (localTowardFoot - 0.5f);
    }

    private static float blockLocalZ(FaucetSide right, FaucetSide towardFoot, float localRight, float localTowardFoot) {
        return 0.5f
                + stepZ(right) * (localRight - 0.5f)
                + stepZ(towardFoot) * (localTowardFoot - 0.5f);
    }

    private static FaucetSide clockWise(FaucetSide side) {
        switch (side) {
            case NORTH: return FaucetSide.EAST;
            case EAST: return FaucetSide.SOUTH;
            case SOUTH: return FaucetSide.WEST;
            case WEST:
            default: return FaucetSide.NORTH;
        }
    }

    private static int stepX(FaucetSide side) {
        switch (side) {
            case EAST: return 1;
            case WEST: return -1;
            case NORTH:
            case SOUTH:
            default: return 0;
        }
    }

    private static int stepZ(FaucetSide side) {
        switch (side) {
            case SOUTH: return 1;
            case NORTH: return -1;
            case EAST:
            case WEST:
            default: return 0;
        }
    }
}
