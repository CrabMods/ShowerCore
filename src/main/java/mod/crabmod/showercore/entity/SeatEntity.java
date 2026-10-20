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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import net.minecraft.world.entity.LivingEntity;

public class SeatEntity extends Entity {
    private static final EntityDataAccessor<Byte> POOL_SEAT_KIND =
            SynchedEntityData.defineId(SeatEntity.class,EntityDataSerializers.BYTE);
    // Vanilla player origin, hip pivot and half leg depth, at the native player render scale.
    private static final double SITTING_POSE_HEIGHT = (1.501F - 12.0 / 16.0 - 2.0 / 16.0) * .9375;

    public boolean isFloorSitting() { return entityData.get(POOL_SEAT_KIND)==1; }
    public void setPoolSeatKind(mod.crabmod.showercore.pool.PoolLayout.SeatKind kind) {
        entityData.set(POOL_SEAT_KIND,(byte)(kind==mod.crabmod.showercore.pool.PoolLayout.SeatKind.INSIDE?1:2));
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (entityData.get(POOL_SEAT_KIND)!=0 && passenger instanceof Player player) {
            move.accept(passenger,getX(),getY()-SITTING_POSE_HEIGHT*player.getScale(),getZ());
        } else {
            super.positionRider(passenger,move);
        }
    }

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
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { builder.define(POOL_SEAT_KIND,(byte)0); }

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
        if(level().isClientSide)return;
        if(tickCount>5&&getPassengers().isEmpty()){discard();return;}
        mod.crabmod.showercore.pool.BathFluidSource source=null;
        if(poolId!=null) {
            if(!mod.crabmod.showercore.pool.PoolSeats.updateSeat(this)){ejectPassengers();discard();return;}
            if(level().getBlockEntity(poolOwner) instanceof mod.crabmod.showercore.pool.PoolBlockEntity pool)source=pool;
        } else if(level().getBlockEntity(blockPosition()) instanceof BathtubBlockEntity tub)source=tub;
        if(source!=null)for(var passenger:getPassengers())source.applyBathEffects(passenger);
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
