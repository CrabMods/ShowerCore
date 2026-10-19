package mod.crabmod.showercore.utils;

import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.entity.ShowerHeadContainerEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import mod.crabmod.showercore.registers.ParticleRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.level.Level;
import mod.crabmod.showercore.entity.SeatEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;

import java.util.Optional;

public class CoreUtils {

    public static boolean isEntityInBathtub(Entity entity, BathtubBlock.LiquidType liquidType) {
        if (mod.crabmod.showercore.pool.PoolContact.bathing(entity, p -> p.liquidType() == liquidType)) return true;
        if (entity.getVehicle() instanceof SeatEntity) {
             return checkBathtubAt(entity.level(), entity.getVehicle().blockPosition(), liquidType);
        }
        
        BlockPos pos = entity.blockPosition();
        if (checkBathtubAt(entity.level(), pos, liquidType)) {
            return isEntityInLiquidBounds(entity, pos);
        }
        return false;
    }

    public static boolean isEntityInAnyHotBathtub(Entity entity) {
        if (mod.crabmod.showercore.pool.PoolContact.bathing(entity, mod.crabmod.showercore.pool.PoolBlockEntity::isHot)) return true;
        if (entity.getVehicle() instanceof SeatEntity) {
             return checkAnyHotBathtubAt(entity.level(), entity.getVehicle().blockPosition());
        }
        
        BlockPos pos = entity.blockPosition();
        if (checkAnyHotBathtubAt(entity.level(), pos)) {
            return isEntityInLiquidBounds(entity, pos);
        }
        return false;
    }

    private static boolean isEntityInLiquidBounds(Entity entity, BlockPos pos) {
        BlockState state = entity.level().getBlockState(pos);
        if (!(state.getBlock() instanceof BathtubBlock)) return false;
        
        AABB liquidBox = getLiquidBox(state, pos);
        return entity.getBoundingBox().intersects(liquidBox);
    }

    private static AABB getLiquidBox(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(BathtubBlock.FACING);
        BedPart part = state.getValue(BathtubBlock.PART);
        
        double minX = 0.125;
        double maxX = 0.875;
        double minZ = 0.125;
        double maxZ = 0.875;
        double minY = 0.25;
        double maxY = 0.7; // Slightly less than 0.75 to avoid rim standing

        if (part == BedPart.FOOT) {
            switch (facing) {
                case NORTH: minZ = 0.0; break;
                case SOUTH: maxZ = 1.0; break;
                case WEST:  minX = 0.0; break;
                case EAST:  maxX = 1.0; break;
                default: break;
            }
        } else {
            switch (facing) {
                case NORTH: maxZ = 1.0; break;
                case SOUTH: minZ = 0.0; break;
                case WEST:  maxX = 1.0; break;
                case EAST:  minX = 0.0; break;
                default: break;
            }
        }
        
        return new AABB(pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ,
                        pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ);
    }

    private static boolean checkBathtubAt(Level level, BlockPos pos, BathtubBlock.LiquidType liquidType) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BathtubBlock) {
            return state.getValue(BathtubBlock.LIQUID) == liquidType;
        }
        return false;
    }

    private static boolean checkAnyHotBathtubAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BathtubBlock) {
            BathtubBlock.LiquidType type = state.getValue(BathtubBlock.LIQUID);
            if (type == BathtubBlock.LiquidType.EMPTY || type == BathtubBlock.LiquidType.WATER) {
                return false;
            }
            if (type == BathtubBlock.LiquidType.CUSTOM) {
                return isCustomFluidHotAt(level, pos);
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if the custom fluid in a bathtub at the given position is hot.
     * Uses the CustomFluidDefinition's isHot() method to determine temperature.
     */
    public static boolean isCustomFluidHotAt(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof mod.crabmod.showercore.pool.BathFluidSource bathtubBe) {
            Optional<CustomFluidDefinition> defOpt = bathtubBe.getCustomFluidDefinition();
            return defOpt.map(CustomFluidDefinition::isHot).orElse(false);
        }
        return false;
    }

    /**
     * Checks if the entity is in a bathtub containing a CUSTOM fluid that is hot.
     */
    public static boolean isEntityInHotCustomBathtub(Entity entity) {
        if (mod.crabmod.showercore.pool.PoolContact.bathing(entity, p -> p.liquidType() == BathtubBlock.LiquidType.CUSTOM && p.isHot())) return true;
        if (entity.getVehicle() instanceof SeatEntity seatEntity) {
            BlockPos pos = seatEntity.blockPosition();
            return checkBathtubAt(entity.level(), pos, BathtubBlock.LiquidType.CUSTOM)
                    && isCustomFluidHotAt(entity.level(), pos);
        }

        BlockPos pos = entity.blockPosition();
        if (checkBathtubAt(entity.level(), pos, BathtubBlock.LiquidType.CUSTOM)
                && isCustomFluidHotAt(entity.level(), pos)) {
            return isEntityInLiquidBounds(entity, pos);
        }
        return false;
    }

    /**
     * Checks if a player is currently experiencing a "hot bath" effect from ShowerCore.
     * This returns true if the player is in a ShowerCore hot bathtub OR under an active shower head.
     * Used by temperature mod compat handlers (Cold Sweat, Tough As Nails, LSO).
     *
     * @param player The player to check
     * @return true if the player is in a hot bathtub or under an active shower
     */
    public static boolean isPlayerInShowerCoreHotWater(Entity player) {
        return isEntityInAnyHotBathtub(player) ||
               ShowerHeadContainerEntity.isPlayerUnderActiveShower(player.getUUID());
    }

    /**
     * Variant of {@link #isPlayerInShowerCoreHotWater} that excludes CUSTOM-fluid bathtubs.
     * Used by the hotBath DirtinessHandler mixin so that hotBath's fast dirtiness decrement only
     * fires for the 6 built-in bath cores and shower heads. CUSTOM-fluid bathtubs rely on
     * ShowerCore's own (slower) dirtiness handling so their cleaning speed stays pack-tunable.
     */
    public static boolean isPlayerInShowerCoreHotWaterNonCustom(Entity player) {
        return isEntityInBuiltInHotBathtub(player) ||
               ShowerHeadContainerEntity.isPlayerUnderActiveShower(player.getUUID());
    }

    private static boolean isEntityInBuiltInHotBathtub(Entity entity) {
        if (mod.crabmod.showercore.pool.PoolContact.bathing(entity, p -> p.liquidType() != BathtubBlock.LiquidType.CUSTOM && p.isHot())) return true;
        if (entity.getVehicle() instanceof SeatEntity) {
            return checkBuiltInHotBathtubAt(entity.level(), entity.getVehicle().blockPosition());
        }
        BlockPos pos = entity.blockPosition();
        if (checkBuiltInHotBathtubAt(entity.level(), pos)) {
            return isEntityInLiquidBounds(entity, pos);
        }
        return false;
    }

    private static boolean checkBuiltInHotBathtubAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BathtubBlock) {
            BathtubBlock.LiquidType type = state.getValue(BathtubBlock.LIQUID);
            return type != BathtubBlock.LiquidType.EMPTY
                && type != BathtubBlock.LiquidType.WATER
                && type != BathtubBlock.LiquidType.CUSTOM;
        }
        return false;
    }

    /**
     * Returns the bath temperature for a player in a ShowerCore hot bath.
     * For custom fluids, returns the actual CustomFluidDefinition temperature.
     * For built-in hot liquids and shower heads, returns the default 40.0f.
     */
    public static float getShowerCoreBathTemperature(Entity player) {
        var pool = mod.crabmod.showercore.pool.PoolContact.find(player, p -> p.getCustomFluidId() != null);
        if(pool != null)return pool.getCustomFluidDefinition().map(f -> f.temperature()).orElse(40.0f);
        Entity vehicle = player.getVehicle();
        BlockPos pos = vehicle instanceof SeatEntity ? vehicle.blockPosition() : player.blockPosition();
        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof mod.crabmod.showercore.pool.BathFluidSource bathtubBe) {
            Optional<CustomFluidDefinition> defOpt = bathtubBe.getCustomFluidDefinition();
            if (defOpt.isPresent()) {
                return defOpt.get().temperature();
            }
        }
        return 40.0f;
    }

    public static boolean isCoreItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
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

    /**
     * Light level a shower head should emit based on which core is installed.
     * Hot water / peony / honey cores emit warm ambient light; others (and empty) emit none.
     */
    public static int getCoreLight(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (registryName == null || !registryName.getNamespace().equals(ShowerCore.MODID)) return 0;
        switch (registryName.getPath()) {
            case "hot_water_core":
            case "peony_bath_core":
            case "honey_bath_core":
                return 12;
            default:
                return 0;
        }
    }

    public static SimpleParticleType getParticleForCore(ItemStack stack) {
        if (stack.isEmpty()) {
            return ParticleRegister.HOT_WATER_SHOWER_PARTICLE.get();
        }
        
        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
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
