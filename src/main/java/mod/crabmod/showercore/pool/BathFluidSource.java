package mod.crabmod.showercore.pool;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import com.crabmod.hotbath.api.HotBathApi;
import com.crabmod.hotbath.api.BathFluidProperties;
import com.crabmod.hotbath.custom_fluid.*;
import mod.crabmod.showercore.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

/** ShowerCore owns container identity, geometry, contact and reload handling. */
public interface BathFluidSource {
    FluidStack fluid();
    @Nullable ResourceLocation getCustomFluidId();
    @Nullable Level getLevel();
    BlockPos getBlockPos();
    boolean touchesBathFluid(AABB bounds);
    double bathSurfaceY();
    void refreshBathDefinition();

    default Optional<CustomFluidDefinition> getCustomFluidDefinition() {
        return fluid().isEmpty()?Optional.empty():CustomFluidAPI.getCustomFluidDefinition(fluid());
    }
    default Optional<BuiltinBathDefinition> getBuiltinBathDefinition() {
        return fluid().isEmpty()?Optional.empty():BuiltinBathDefinitions.forFluid(fluid().getFluid(),getLevel()!=null&&getLevel().isClientSide());
    }
    default BathFluidProperties properties() {return HotBathApi.fluidProperties(fluid(),getLevel()!=null&&getLevel().isClientSide());}
    default int luminosity() {return Math.max(0,Math.min(15,properties().luminosity()));}
    default float temperature() {return properties().temperatureCelsius();}
    default boolean isHot() {return properties().hot();}
    default boolean emitsSteam() {return properties().steam()||!fluid().isEmpty()&&!HotBathApi.isBathFluid(fluid().getFluid())&&Config.steamFluids.contains(fluid().getFluid());}

    default void applyBathEffects(Entity entity) {
        var level=getLevel();
        if(level==null||fluid().isEmpty()||!touchesBathFluid(entity.getBoundingBox()))return;
        HotBathApi.reportFluidContact(entity,fluid(),bathSurfaceY(),Config.isModIntegrationsEnabled());
        if(level.isClientSide())return;
        HotBathApi.applyFluidContact(entity,fluid());
        // Pools provide native fluid depth; Entity.baseTick already handles their lava damage.
        if(!(this instanceof PoolBlockEntity)&&(fluid().getFluid()==Fluids.LAVA||fluid().getFluid()==Fluids.FLOWING_LAVA))
            entity.lavaHurt();
    }
    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    default void animateSurface(net.minecraft.util.RandomSource random,double x,double y,double z) {
        if(!(getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel client)||fluid().isEmpty())return;
        com.crabmod.hotbath.api.client.BathFluidVisuals.animateSurface(client,fluid(),random,x,y,z,bounds->properties().bubbles()&&touchesBathFluid(bounds));
        if(!HotBathApi.isBathFluid(fluid().getFluid())&&emitsSteam()&&random.nextInt(10)==0)
            client.addParticle(com.crabmod.hotbath.registers.ParticleRegister.STEAM_PARTICLE.get(),x,y+.02,z,0,.02,0);
    }
}
