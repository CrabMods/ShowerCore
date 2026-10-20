package mod.crabmod.showercore.client.renderer;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import mod.crabmod.showercore.pool.BathFluidSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.Nullable;

/** Keep the native ARGB, including opacity and biome tint, for every bath shape. */
final class BathFluidAppearance {
    private BathFluidAppearance() {}

    static int tint(BathFluidSource source,@Nullable Level level,BlockPos pos) {
        var stack=source.fluid();
        var extension=IClientFluidTypeExtensions.of(stack.getFluid());
        return source.getCustomFluidDefinition().map(definition->CustomFluidAPI.getFluidColor(definition.id()))
                .orElseGet(()->level==null?extension.getTintColor(stack)
                        :extension.getTintColor(stack.getFluid().defaultFluidState(),level,pos));
    }

    static net.minecraft.resources.ResourceLocation texture(BathFluidSource source,@Nullable Level level,BlockPos pos,boolean flowing) {
        var stack=source.fluid();var extension=IClientFluidTypeExtensions.of(stack.getFluid());
        if(level==null||CustomFluidAPI.isDynamicCustomFluid(stack))return flowing?extension.getFlowingTexture(stack):extension.getStillTexture(stack);
        return flowing?extension.getFlowingTexture(stack.getFluid().defaultFluidState(),level,pos)
                :extension.getStillTexture(stack.getFluid().defaultFluidState(),level,pos);
    }
}
