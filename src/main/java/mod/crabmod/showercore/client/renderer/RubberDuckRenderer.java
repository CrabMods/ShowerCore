package mod.crabmod.showercore.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mod.crabmod.showercore.entity.RubberDuckEntity;
import mod.crabmod.showercore.registers.ItemRegister;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class RubberDuckRenderer extends EntityRenderer<RubberDuckEntity> {
    private final ItemRenderer itemRenderer;

    public RubberDuckRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(RubberDuckEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        
        ItemStack appearance = entity.appearance();
        float scale = appearance.is(ItemRegister.RUBBER_DUCK_DELUXE.get()) ? 0.38F : 0.6F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0D, 0.25D, 0.0D);

        this.itemRenderer.renderStatic(appearance, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RubberDuckEntity entity) {
        return new ResourceLocation("showercore", "textures/item/rubber_duck.png");
    }
}
