package mod.crabmod.showercore.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;

public class SeatEntity extends Entity {
    private BlockPos poolOwner;
    private java.util.UUID poolId;
    private int poolSlot = -1;
    public void bindPool(BlockPos owner, java.util.UUID id, int slot) { poolOwner=owner.immutable();poolId=id;poolSlot=slot; }
    public BlockPos poolOwner() { return poolOwner; }
    public java.util.UUID poolId() { return poolId; }
    public int poolSlot() { return poolSlot; }
    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public SeatEntity(Level level, double x, double y, double z) {
        this(mod.crabmod.showercore.registers.EntityRegister.SEAT_ENTITY.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if(tag.hasUUID("PoolId"))bindPool(BlockPos.of(tag.getLong("PoolOwner")),tag.getUUID("PoolId"),tag.getInt("PoolSlot"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if(poolId!=null){tag.putUUID("PoolId",poolId);tag.putLong("PoolOwner",poolOwner.asLong());tag.putInt("PoolSlot",poolSlot);}
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }
    
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.tickCount > 5 && this.getPassengers().isEmpty()) {
                this.discard();
            }
            
            if (poolId != null) {
                if (!mod.crabmod.showercore.pool.PoolSeats.validSeat(this)) { ejectPassengers(); discard(); }
                return;
            }
            if (this.tickCount % 20 == 0) {
                 BlockPos pos = this.blockPosition();
                 BlockState state = this.level().getBlockState(pos);
                 if (state.getBlock() instanceof BathtubBlock) {
                     BathtubBlock.LiquidType liquid = state.getValue(BathtubBlock.LIQUID);
                     for (Entity passenger : this.getPassengers()) {
                         if (passenger instanceof LivingEntity living) {
                             applyBathEffects(living, liquid, this.level(), pos);
                         }
                     }
                 }
            }
        }
    }

    public static void applyBathEffects(LivingEntity entity, BathtubBlock.LiquidType liquid, Level level, BlockPos pos) {
        if (entity.level().isClientSide) return;

        long gameTime = entity.level().getGameTime();
        CompoundTag data = entity.getPersistentData();

        // Reset timers if the liquid type changed or the player had a gap (>1.5s)
        // in attendance — otherwise cleanse would fire the instant they re-enter.
        String currentLiquid = data.getString("showercore.bath.liquid");
        long lastSeen = data.getLong("showercore.bath.last_seen");
        if (!currentLiquid.equals(liquid.name()) || gameTime - lastSeen > 30) {
            data.putString("showercore.bath.liquid", liquid.name());
            data.putLong("showercore.bath.start", gameTime);
        }
        data.putLong("showercore.bath.last_seen", gameTime);

        long sittingTime = gameTime - data.getLong("showercore.bath.start");

        boolean shouldRefreshBuffs = gameTime - data.getLong("showercore.bath.buff_refresh") >= 200;
        if (shouldRefreshBuffs) data.putLong("showercore.bath.buff_refresh", gameTime);

        // Cleanse after 15s of continuous bathing, then re-cleanse every 15s.
        boolean shouldCleanse = sittingTime >= 300
                && gameTime - data.getLong("showercore.bath.cleanse") >= 300;
        if (shouldCleanse) data.putLong("showercore.bath.cleanse", gameTime);

        switch (liquid) {
            case HOT_WATER -> {
                if (shouldRefreshBuffs)
                    ShowerHeadContainerEntity.addStackingEffect(entity, MobEffects.MOVEMENT_SPEED, 400, 1);
            }
            case HERBAL_BATH -> {
                if (shouldRefreshBuffs) {
                    ShowerHeadContainerEntity.addStackingEffect(entity, MobEffects.REGENERATION, 400, 1);
                    ShowerHeadContainerEntity.addStackingEffect(entity, MobEffects.DAMAGE_RESISTANCE, 400, 1);
                }
                if (shouldCleanse) ShowerHeadContainerEntity.cureNegativeEffects(entity);
            }
            case HONEY_BATH -> {
                if (shouldRefreshBuffs) {
                    ShowerHeadContainerEntity.addStackingEffect(entity, MobEffects.REGENERATION, 400, 1);
                    ShowerHeadContainerEntity.addStackingEffect(entity, MobEffects.ABSORPTION, 400, 3);
                }
                if (shouldCleanse) ShowerHeadContainerEntity.cureNegativeEffectsExceptSlow(entity);
            }
            case MILK_BATH -> {
                if (shouldRefreshBuffs)
                    ShowerHeadContainerEntity.addStackingEffect(entity, MobEffects.REGENERATION, 400, 1);
                if (shouldCleanse) ShowerHeadContainerEntity.cureNegativeEffects(entity);
            }
            case PEONY_BATH -> {
                if (shouldRefreshBuffs) {
                    ShowerHeadContainerEntity.addStackingEffect(entity, MobEffects.REGENERATION, 400, 1);
                    ShowerHeadContainerEntity.addStackingEffect(entity, MobEffects.LUCK, 400, 1);
                    ShowerHeadContainerEntity.addStackingEffect(entity, MobEffects.DIG_SPEED, 400, 1);
                }
                if (shouldCleanse) {
                    ShowerHeadContainerEntity.cureNegativeEffects(entity);
                    entity.removeEffect(MobEffects.BAD_OMEN);
                }
            }
            case ROSE_BATH -> {
                if (shouldRefreshBuffs) {
                    ShowerHeadContainerEntity.addStackingEffect(entity, MobEffects.REGENERATION, 400, 1);
                    ShowerHeadContainerEntity.addStackingEffect(entity, MobEffects.DAMAGE_BOOST, 400, 1);
                }
                if (shouldCleanse) {
                    ShowerHeadContainerEntity.cureNegativeEffects(entity);
                    entity.removeEffect(MobEffects.BAD_OMEN);
                }
            }
            case CUSTOM -> {
                // Honey/other hotBath custom fluids land here (not in the named cases above).
                // Dispatch to hotBath's CustomFluidAPI so sitting applies the same effects as
                // standing inside the tub (see BathtubBlock#entityInside CUSTOM branch).
                if (shouldRefreshBuffs && entity instanceof ServerPlayer serverPlayer) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof mod.crabmod.showercore.pool.BathFluidSource bathtubBe) {
                        ResourceLocation fluidId = bathtubBe.getCustomFluidId();
                        if (fluidId != null) {
                            CustomFluidAPI.applyFluidEffects(serverPlayer, fluidId);
                        }
                    }
                }
            }
            default -> {}
        }
    }

    @Override
    public net.minecraft.world.phys.Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger) {
        net.minecraft.core.Direction direction = this.getDirection();
        net.minecraft.core.Direction[] directions = {direction, direction.getClockWise(), direction.getCounterClockWise(), direction.getOpposite()};
        
        for (net.minecraft.core.Direction dir : directions) {
            net.minecraft.world.phys.Vec3 vec3 = net.minecraft.world.entity.vehicle.DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), this.blockPosition().relative(dir), false);
            if (vec3 != null) {
                return vec3;
            }
        }
        
        return super.getDismountLocationForPassenger(passenger);
    }
}
