package mod.crabmod.showercore.client.renderer;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public class BathtubBlockEntityRenderer implements BlockEntityRenderer<BathtubBlockEntity> {
    public BathtubBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BathtubBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        BlockState state = pBlockEntity.getBlockState();
        if (state.getValue(BathtubBlock.LIQUID) == BathtubBlock.LiquidType.EMPTY) {
            return;
        }

        FluidStack fluidStack = pBlockEntity.getFluidTank().getFluid();
        if (fluidStack.isEmpty()) return;

        Fluid fluid = fluidStack.getFluid();
        IClientFluidTypeExtensions fluidTypeExtensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = fluidTypeExtensions.getStillTexture(fluidStack);

        // Lava is emissive in vanilla; use full-bright so the drop/surface don't appear dim in
        // dark rooms. Also covers FLOWING_LAVA for parity with BathtubBlock.entityInside.
        boolean isLava = fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA;
        int lightForFluid = isLava ? LightTexture.FULL_BRIGHT : pPackedLight;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);

        // Legacy bathtubs keep their custom fluid identity separately from the tank stack.
        int color = pBlockEntity.getCustomFluidDefinition()
                .map(definition -> CustomFluidAPI.getFluidColor(definition.id()))
                .orElseGet(() -> pBlockEntity.getLevel() != null
                        ? fluidTypeExtensions.getTintColor(fluid.defaultFluidState(), pBlockEntity.getLevel(), pBlockEntity.getBlockPos())
                        : fluidTypeExtensions.getTintColor(fluidStack));

        float alpha = ((color >> 24) & 0xFF) / 255f;
        float red = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >> 8) & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;

        pPoseStack.pushPose();

        // Block entities render before the chunk translucent pass. In Fabulous graphics, the
        // chunk translucent render type targets LevelRenderer's translucent framebuffer, but
        // that framebuffer is cleared after the block-entity batch is flushed. The vanilla
        // block-entity translucent sheet is flushed on the correct side of that clear.
        VertexConsumer builder = pBufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        Matrix4f matrix = pPoseStack.last().pose();

        float y = BathtubBlock.waterLevelFor(state);
        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();

        float x1 = 0.125f;
        float x2 = 0.875f;
        float z1 = 0.125f;
        float z2 = 0.875f;

        Direction facing = state.getValue(BathtubBlock.FACING);
        BedPart part = state.getValue(BathtubBlock.PART);

        // Determine connected side
        // HEAD connects to opposite of facing (e.g. North facing -> connects to South)
        // FOOT connects to facing (e.g. North facing -> connects to North)
        Direction connectedSide = (part == BedPart.HEAD) ? facing.getOpposite() : facing;

        if (connectedSide == Direction.NORTH) z1 = 0;
        if (connectedSide == Direction.SOUTH) z2 = 1;
        if (connectedSide == Direction.WEST) x1 = 0;
        if (connectedSide == Direction.EAST) x2 = 1;

        // Render Quad
        // Note: UVs should probably be adjusted if we stretch the quad, but for now simple mapping is fine.
        addVertex(builder, matrix, x1, y, z1, minU, minV, red, green, blue, alpha, lightForFluid);
        addVertex(builder, matrix, x1, y, z2, minU, maxV, red, green, blue, alpha, lightForFluid);
        addVertex(builder, matrix, x2, y, z2, maxU, maxV, red, green, blue, alpha, lightForFluid);
        addVertex(builder, matrix, x2, y, z1, maxU, minV, red, green, blue, alpha, lightForFluid);

        // Running faucet drop: small fluid box right below the faucet spout on the HEAD side.
        // The BER owns fluid rendering for every non-empty liquid; block models provide only
        // the tub and faucet geometry.
        if (BathtubDropGeometry.shouldRenderDrop(part == BedPart.HEAD, state.getValue(BathtubBlock.RUNNING))) {
            float[] b = BathtubDropGeometry.computeDropBounds(toFaucetSide(facing), BathtubBlock.geometryFor(state));
            renderDropCube(builder, matrix, b[0], b[1], b[2], b[3], b[4], b[5],
                    sprite, red, green, blue, alpha, lightForFluid);
        }

        pPoseStack.popPose();
    }

    /** Adapter from Minecraft's {@code Direction} to the test-friendly {@link BathtubDropGeometry.FaucetSide}. */
    private static BathtubDropGeometry.FaucetSide toFaucetSide(Direction d) {
        switch (d) {
            case NORTH: return BathtubDropGeometry.FaucetSide.NORTH;
            case SOUTH: return BathtubDropGeometry.FaucetSide.SOUTH;
            case WEST:  return BathtubDropGeometry.FaucetSide.WEST;
            case EAST:
            default:    return BathtubDropGeometry.FaucetSide.EAST;
        }
    }

    private void renderDropCube(VertexConsumer builder, Matrix4f matrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                TextureAtlasSprite sprite,
                                float r, float g, float b, float a, int light) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        // Scale UV to a small sub-region so the drop samples just a corner of the fluid texture.
        float uMid = u0 + (u1 - u0) * (2f / 16f);
        float vMid = v0 + (v1 - v0) * (2f / 16f);

        // Top face (y2), normal up
        addVertexNormal(builder, matrix, x1, y2, z1, u0, v0, r, g, b, a, light, 0, 1, 0);
        addVertexNormal(builder, matrix, x1, y2, z2, u0, vMid, r, g, b, a, light, 0, 1, 0);
        addVertexNormal(builder, matrix, x2, y2, z2, uMid, vMid, r, g, b, a, light, 0, 1, 0);
        addVertexNormal(builder, matrix, x2, y2, z1, uMid, v0, r, g, b, a, light, 0, 1, 0);
        // Bottom face (y1), normal down
        addVertexNormal(builder, matrix, x1, y1, z2, u0, vMid, r, g, b, a, light, 0, -1, 0);
        addVertexNormal(builder, matrix, x1, y1, z1, u0, v0, r, g, b, a, light, 0, -1, 0);
        addVertexNormal(builder, matrix, x2, y1, z1, uMid, v0, r, g, b, a, light, 0, -1, 0);
        addVertexNormal(builder, matrix, x2, y1, z2, uMid, vMid, r, g, b, a, light, 0, -1, 0);
        // North face (z1)
        addVertexNormal(builder, matrix, x1, y2, z1, u0, v0, r, g, b, a, light, 0, 0, -1);
        addVertexNormal(builder, matrix, x2, y2, z1, uMid, v0, r, g, b, a, light, 0, 0, -1);
        addVertexNormal(builder, matrix, x2, y1, z1, uMid, vMid, r, g, b, a, light, 0, 0, -1);
        addVertexNormal(builder, matrix, x1, y1, z1, u0, vMid, r, g, b, a, light, 0, 0, -1);
        // South face (z2)
        addVertexNormal(builder, matrix, x2, y2, z2, u0, v0, r, g, b, a, light, 0, 0, 1);
        addVertexNormal(builder, matrix, x1, y2, z2, uMid, v0, r, g, b, a, light, 0, 0, 1);
        addVertexNormal(builder, matrix, x1, y1, z2, uMid, vMid, r, g, b, a, light, 0, 0, 1);
        addVertexNormal(builder, matrix, x2, y1, z2, u0, vMid, r, g, b, a, light, 0, 0, 1);
        // West face (x1)
        addVertexNormal(builder, matrix, x1, y2, z2, u0, v0, r, g, b, a, light, -1, 0, 0);
        addVertexNormal(builder, matrix, x1, y2, z1, uMid, v0, r, g, b, a, light, -1, 0, 0);
        addVertexNormal(builder, matrix, x1, y1, z1, uMid, vMid, r, g, b, a, light, -1, 0, 0);
        addVertexNormal(builder, matrix, x1, y1, z2, u0, vMid, r, g, b, a, light, -1, 0, 0);
        // East face (x2)
        addVertexNormal(builder, matrix, x2, y2, z1, u0, v0, r, g, b, a, light, 1, 0, 0);
        addVertexNormal(builder, matrix, x2, y2, z2, uMid, v0, r, g, b, a, light, 1, 0, 0);
        addVertexNormal(builder, matrix, x2, y1, z2, uMid, vMid, r, g, b, a, light, 1, 0, 0);
        addVertexNormal(builder, matrix, x2, y1, z1, u0, vMid, r, g, b, a, light, 1, 0, 0);
    }

    private void addVertex(VertexConsumer builder, Matrix4f matrix, float x, float y, float z, float u, float v, float r, float g, float b, float a, int packedLight) {
        addVertexNormal(builder, matrix, x, y, z, u, v, r, g, b, a, packedLight, 0, 1, 0);
    }

    private void addVertexNormal(VertexConsumer builder, Matrix4f matrix,
                                 float x, float y, float z,
                                 float u, float v,
                                 float r, float g, float b, float a,
                                 int packedLight,
                                 float nx, float ny, float nz) {
        org.joml.Vector4f vector = new org.joml.Vector4f(x, y, z, 1.0F);
        matrix.transform(vector);
        builder.addVertex(vector.x, vector.y, vector.z)
               .setColor(r, g, b, a)
               .setUv(u, v)
               .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
               .setLight(packedLight)
               .setNormal(nx, ny, nz);
    }
}
