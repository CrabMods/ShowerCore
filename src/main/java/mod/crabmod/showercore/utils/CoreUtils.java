package mod.crabmod.showercore.utils;

import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.entity.SeatEntity;
import mod.crabmod.showercore.entity.ShowerHeadContainerEntity;
import mod.crabmod.showercore.pool.*;
import mod.crabmod.showercore.registers.ParticleRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.registries.ForgeRegistries;

public class CoreUtils {
    @Nullable
    public static BathFluidSource bathAt(Entity entity) {
        var pool=PoolContact.find(entity,p->true);
        if(pool!=null)return pool;
        var pos=entity.getVehicle() instanceof SeatEntity seat?seat.blockPosition():entity.blockPosition();
        return entity.level().getBlockEntity(pos) instanceof BathtubBlockEntity tub&&tub.touchesBathFluid(entity.getBoundingBox())?tub:null;
    }

    public static boolean isEntityInBathtub(Entity entity,BathtubBlock.LiquidType type) {
        var source=bathAt(entity);
        return source instanceof PoolBlockEntity pool?pool.liquidType()==type
                :source instanceof BathtubBlockEntity tub&&tub.getBlockState().getValue(BathtubBlock.LIQUID)==type;
    }

    public static boolean isEntityInAnyHotBathtub(Entity entity) {
        var source=bathAt(entity);return source!=null&&source.isHot();
    }

    public static boolean isCustomFluidHotAt(Level level,BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BathFluidSource source&&source.getCustomFluidId()!=null&&source.isHot();
    }

    public static boolean isEntityInHotCustomBathtub(Entity entity) {
        var source=bathAt(entity);return source!=null&&source.getCustomFluidId()!=null&&source.isHot();
    }

    public static boolean isPlayerInShowerCoreBath(Entity entity) {
        var source=bathAt(entity);
        return source!=null&&com.crabmod.hotbath.api.HotBathApi.isBathFluid(source.fluid().getFluid())
                ||ShowerHeadContainerEntity.isPlayerUnderActiveShower(entity.getUUID());
    }

    public static boolean isPlayerInShowerCoreHotWater(Entity entity) {
        return isEntityInAnyHotBathtub(entity)||ShowerHeadContainerEntity.isPlayerUnderActiveShower(entity.getUUID());
    }

    public static boolean isPlayerInShowerCoreCleansingFluid(Entity entity) {
        var source=bathAt(entity);
        return source!=null&&com.crabmod.hotbath.api.HotBathApi.isCleansingFluid(source.fluid().getFluid())
                ||ShowerHeadContainerEntity.isPlayerUnderActiveShower(entity.getUUID());
    }

    public static float getShowerCoreBathTemperature(Entity entity) {
        var source=bathAt(entity);return source==null?40f:source.temperature();
    }

    public static boolean isCoreItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registryName != null && registryName.getNamespace().equals(ShowerCore.MODID)) {
             String path = registryName.getPath();
             return isCorePath(path);
        }
        return false;
    }

    private static boolean isCorePath(String path) {
        return path.equals("hot_water_core") ||
               path.equals("herbal_bath_core") ||
               path.equals("peony_bath_core") ||
               path.equals("rose_bath_core") ||
               path.equals("milk_bath_core") ||
               path.equals("honey_bath_core");
    }

    public static SimpleParticleType getParticleForCore(ItemStack stack) {
        if (stack.isEmpty()) {
            return ParticleRegister.HOT_WATER_SHOWER_PARTICLE.get();
        }
        
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registryName != null && registryName.getNamespace().equals(ShowerCore.MODID)) {
            String path = registryName.getPath();
            switch (path) {
                case "herbal_bath_core":
                    return ParticleRegister.HERBAL_BATH_SHOWER_PARTICLE.get();
                case "honey_bath_core":
                    return ParticleRegister.HONEY_BATH_SHOWER_PARTICLE.get();
                case "milk_bath_core":
                    return ParticleRegister.MILK_BATH_SHOWER_PARTICLE.get();
                case "peony_bath_core":
                    return ParticleRegister.PEONY_BATH_SHOWER_PARTICLE.get();
                case "rose_bath_core":
                    return ParticleRegister.ROSE_BATH_SHOWER_PARTICLE.get();
                case "hot_water_core":
                default:
                    return ParticleRegister.HOT_WATER_SHOWER_PARTICLE.get();
            }
        }
        return ParticleRegister.HOT_WATER_SHOWER_PARTICLE.get();
    }
}
