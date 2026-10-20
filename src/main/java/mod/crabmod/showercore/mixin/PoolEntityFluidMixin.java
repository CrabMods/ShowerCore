package mod.crabmod.showercore.mixin;

import mod.crabmod.showercore.pool.PoolContact;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Contribute the tank volume to the same per-tick fluid state as world fluids. */
@Mixin(Entity.class)
public abstract class PoolEntityFluidMixin {
    @Shadow(remap=false) protected abstract void setFluidTypeHeight(FluidType type,double height);
    @Shadow(remap=false) private FluidType forgeFluidTypeOnEyes;

    @Inject(method="updateFluidHeightAndDoFluidPushing()V",at=@At("RETURN"),remap=false)
    private void showercore$bathContactAndPoolDepth(CallbackInfo ci) {
        var entity=(Entity)(Object)this;
        if(entity.touchingUnloadedChunk())return;
        var source=mod.crabmod.showercore.utils.CoreUtils.bathAt(entity);
        if(source==null) {
            com.crabmod.hotbath.api.HotBathApi.endFluidContact(entity);return;
        }
        source.applyBathEffects(entity);
        if(!(source instanceof mod.crabmod.showercore.pool.PoolBlockEntity pool))return;
        var bounds=entity.getBoundingBox().deflate(.001);
        if(!pool.touchesWater(bounds))return;
        var type=pool.fluid().getFluid().getFluidType();
        double depth=pool.origin().getY()+pool.waterY()-bounds.minY;
        setFluidTypeHeight(type,Math.max(entity.getFluidTypeHeight(type),depth));
    }

    @Inject(method="updateFluidOnEyes",at=@At("RETURN"))
    private void showercore$poolEyes(CallbackInfo ci) {
        var entity=(Entity)(Object)this;
        if(!forgeFluidTypeOnEyes.isAir()||entity.getVehicle() instanceof Boat boat&&!boat.isUnderWater())return;
        var pool=PoolContact.atPoint(entity.level(),entity.getEyePosition());
        if(pool!=null)forgeFluidTypeOnEyes=pool.fluid().getFluid().getFluidType();
    }
}
