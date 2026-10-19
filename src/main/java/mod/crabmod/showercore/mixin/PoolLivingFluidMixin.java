package mod.crabmod.showercore.mixin;

import mod.crabmod.showercore.pool.PoolContact;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class PoolLivingFluidMixin {
    // Let the registered fluid type own travel, including mod-fluid movement hooks.
    @Redirect(method="travel",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState showercore$travelFluid(Level level,BlockPos pos) {
        var state=level.getFluidState(pos);
        if(!state.isEmpty())return state;
        var pool=PoolContact.find((LivingEntity)(Object)this,p->true);
        return pool==null?state:pool.fluid().getFluid().defaultFluidState();
    }
}
