package mod.crabmod.showercore.mixin;

import mod.crabmod.showercore.entity.SeatEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Floor seats extend the legs along the support surface instead of hanging below a bench. */
@Mixin(value=HumanoidModel.class,remap=false)
public abstract class PoolFloorSittingMixin {
    @Shadow(remap=false) @Final public ModelPart rightLeg;
    @Shadow(remap=false) @Final public ModelPart leftLeg;

    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"),remap=false)
    private void showercore$floorPose(LivingEntity entity,float swing,float speed,float age,float yaw,float pitch,CallbackInfo ci) {
        if(!(entity instanceof Player)||!(entity.getVehicle() instanceof SeatEntity seat)||!seat.isFloorSitting())return;
        rightLeg.xRot=leftLeg.xRot=-Mth.HALF_PI;
        rightLeg.zRot=leftLeg.zRot=0;
    }
}
