package mod.crabmod.showercore.pool;

import com.crabmod.hotbath.custom_fluid.BuiltinBathDefinition;
import com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import mod.crabmod.showercore.Config;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

/** Container adapter over Hot Bath's existing, reloadable fluid definitions. */
public interface BathFluidSource extends com.crabmod.hotbath.api.BathFluidContainer {
    @Nullable Level getLevel();
    net.minecraft.core.BlockPos getBlockPos();

    default Optional<BuiltinBathDefinition> getBuiltinBathDefinition() {
        return fluid().isEmpty()?Optional.empty():BuiltinBathDefinitions.forFluid(fluid().getFluid(),getLevel()!=null&&getLevel().isClientSide());
    }

    default int luminosity() {
        if(fluid().isEmpty())return 0;
        return Math.max(0,Math.min(15,getCustomFluidDefinition().map(CustomFluidDefinition::luminosity)
                .orElseGet(()->fluid().getFluid().getFluidType().getLightLevel(fluid()))));
    }

    default boolean isHot() {
        if(fluid().isEmpty())return false;
        return getCustomFluidDefinition().map(CustomFluidDefinition::isHot)
                .orElseGet(()->getBuiltinBathDefinition().map(BuiltinBathDefinition::isHot)
                        .orElseGet(()->temperature()>=CustomFluidDefinition.HOT_TEMPERATURE_THRESHOLD));
    }

    default float temperature() {
        if(fluid().isEmpty())return 0f;
        return getCustomFluidDefinition().map(CustomFluidDefinition::temperature)
                .orElseGet(()->getBuiltinBathDefinition().map(BuiltinBathDefinition::temperature).orElseGet(()->fluid().getFluid().getFluidType().getTemperature(fluid())-273f));
    }

    default boolean emitsSteam() {
        if(fluid().isEmpty())return false;
        var custom=getCustomFluidDefinition();
        if(custom.isPresent())return custom.get().isHot()&&custom.get().showSteam();
        return getBuiltinBathDefinition().map(BuiltinBathDefinition::isHot)
                .orElseGet(()->Config.steamFluids.contains(fluid().getFluid()));
    }

    default void applyBathEffects(net.minecraft.world.entity.Entity entity) {
        var level=getLevel();
        if(level==null||level.isClientSide()||fluid().isEmpty()||!touchesBathFluid(entity.getBoundingBox()))return;
        var block=fluid().getFluid().defaultFluidState().createLegacyBlock().getBlock();
        if(block instanceof com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock bath) {
            bath.entityInside(bath.defaultBlockState(),level,getBlockPos(),entity);
        } else if(fluid().getFluid()==Fluids.LAVA||fluid().getFluid()==Fluids.FLOWING_LAVA) {
            entity.setRemainingFireTicks(300);
            entity.hurt(level.damageSources().lava(),4f);
        }
    }
    default void animateSurface(net.minecraft.util.RandomSource random,double x,double y,double z) {
        var level=getLevel();
        if(level==null||fluid().isEmpty())return;
        var block=fluid().getFluid().defaultFluidState().createLegacyBlock().getBlock();
        if(block instanceof com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock bath)bath.animateContainedSurface(level,getBlockPos(),random,x,y,z);
        else if(emitsSteam()&&random.nextInt(10)==0)
            level.addParticle(com.crabmod.hotbath.registers.ParticleRegister.STEAM_PARTICLE.get(),x,y+.02,z,0,.02,0);
    }
}
