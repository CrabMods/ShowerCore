package mod.crabmod.showercore.entity;

import mod.crabmod.showercore.registers.EntityRegister;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RubberDuckEntity extends Entity {

    private static final net.minecraft.network.syncher.EntityDataAccessor<net.minecraft.world.item.ItemStack> APPEARANCE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RubberDuckEntity.class, net.minecraft.network.syncher.EntityDataSerializers.ITEM_STACK);
    private float rotationalVelocity;

    public void setAppearance(net.minecraft.world.item.ItemStack stack) {
        if (!(stack.getItem() instanceof mod.crabmod.showercore.item.RubberDuckItem))
            throw new IllegalArgumentException("Duck appearance must be a rubber duck item");
        entityData.set(APPEARANCE, stack.copyWithCount(1));
    }

    public net.minecraft.world.item.ItemStack appearance() {
        var stack = entityData.get(APPEARANCE);
        return stack.isEmpty() ? new net.minecraft.world.item.ItemStack(mod.crabmod.showercore.registers.ItemRegister.RUBBER_DUCK.get()) : stack.copy();
    }

    private void pickup(Player player) {
        var stack = appearance();
        if (!player.addItem(stack)) player.drop(stack, false);
        discard();
        playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
    }

    public RubberDuckEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public RubberDuckEntity(Level level, double x, double y, double z) {
        this(EntityRegister.RUBBER_DUCK.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { builder.define(APPEARANCE, net.minecraft.world.item.ItemStack.EMPTY); }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        var stack = net.minecraft.world.item.ItemStack.parseOptional(level().registryAccess(), tag.getCompound("Appearance"));
        if (stack.getItem() instanceof mod.crabmod.showercore.item.RubberDuckItem) setAppearance(stack);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { tag.put("Appearance", appearance().save(level().registryAccess())); }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this, serverEntity);
    }

    @Override
    public void tick() {
        super.tick();
        
        // Calculate fluid height for ANY fluid
        net.minecraft.world.phys.AABB aabb = this.getBoundingBox().deflate(0.001D);
        int minX = net.minecraft.util.Mth.floor(aabb.minX);
        int maxX = net.minecraft.util.Mth.ceil(aabb.maxX);
        int minY = net.minecraft.util.Mth.floor(aabb.minY);
        int maxY = net.minecraft.util.Mth.ceil(aabb.maxY);
        int minZ = net.minecraft.util.Mth.floor(aabb.minZ);
        int maxZ = net.minecraft.util.Mth.ceil(aabb.maxZ);
        
        double fluidHeight = 0.0D;
        mod.crabmod.showercore.pool.PoolBlockEntity floatingPool = null;
        boolean isDestroyFluid = false;
        
        net.minecraft.core.BlockPos.MutableBlockPos mutablePos = new net.minecraft.core.BlockPos.MutableBlockPos();
        
        for(int k = minX; k < maxX; ++k) {
            for(int l = minY; l < maxY; ++l) {
                for(int i1 = minZ; i1 < maxZ; ++i1) {
                    mutablePos.set(k, l, i1);
                    net.minecraft.world.level.material.FluidState fluidstate = this.level().getFluidState(mutablePos);
                    if (!fluidstate.isEmpty()) {
                        if (mod.crabmod.showercore.Config.rubberDuckDestroyFluids.contains(fluidstate.getType())) {
                            isDestroyFluid = true;
                        }
                        
                        double height = (double)((float)l + fluidstate.getHeight(this.level(), mutablePos));
                        if (height >= aabb.minY) {
                            fluidHeight = Math.max(fluidHeight, height - aabb.minY);
                        }
                    }

                    if(this.level().getBlockEntity(mutablePos) instanceof mod.crabmod.showercore.pool.PoolBlockEntity pool && pool.configured() && pool.active() && !pool.fluid().isEmpty()) {
                        double height=pool.origin().getY()+pool.waterY()+.05;
                        if(mod.crabmod.showercore.pool.PoolLayout.area(mod.crabmod.showercore.pool.PoolLayout.clip(pool.layout().waterOutline(),aabb.minX-pool.origin().getX(),aabb.minZ-pool.origin().getZ(),aabb.maxX-pool.origin().getX(),aabb.maxZ-pool.origin().getZ()))>1e-8) {
                            floatingPool=pool;
                            isDestroyFluid|=mod.crabmod.showercore.Config.rubberDuckDestroyFluids.contains(pool.fluid().getFluid());
                            if(height>=aabb.minY)fluidHeight=Math.max(fluidHeight,height-aabb.minY);
                        }
                    }
                    net.minecraft.world.level.block.state.BlockState blockState = this.level().getBlockState(mutablePos);
                    if (blockState.getBlock() instanceof mod.crabmod.showercore.block.BathtubBlock) {
                        if (blockState.getValue(mod.crabmod.showercore.block.BathtubBlock.LIQUID) != mod.crabmod.showercore.block.BathtubBlock.LiquidType.EMPTY) {
                             net.minecraft.world.level.block.entity.BlockEntity be = this.level().getBlockEntity(mutablePos);
                             if (be instanceof mod.crabmod.showercore.block.entity.BathtubBlockEntity bathtubBe) {
                                 if (mod.crabmod.showercore.Config.rubberDuckDestroyFluids.contains(bathtubBe.getFluidTank().getFluid().getFluid())) {
                                     isDestroyFluid = true;
                                 }
                             }

                             double height = (double) l + mod.crabmod.showercore.block.BathtubBlock.duckFloatSurfaceFor(blockState);
                             if (height >= aabb.minY) {
                                 fluidHeight = Math.max(fluidHeight, height - aabb.minY);
                             }
                        }
                    }
                }
            }
        }

        if (isDestroyFluid) {
             if (!this.level().isClientSide) {
                this.discard();
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.GENERIC_BURN, this.getSoundSource(), 1.0F, 1.0F);
             }
             this.level().addParticle(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
             return;
        }
        
        // Gravity
        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04D, 0));

        this.setYRot(this.getYRot() + this.rotationalVelocity);
        this.rotationalVelocity *= 0.85F;

        if (fluidHeight > 0) {
            // Buoyancy: proportional to submerged depth
            // Target: float higher
            double buoyancy = fluidHeight * 0.5D;
            this.setDeltaMovement(this.getDeltaMovement().add(0, buoyancy, 0));
            
            // Water drag, higher Y drag to prevent oscillation
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.9, 0.75, 0.9));
            
            // Custom particles only when moving horizontally
            if (this.level().isClientSide) {
                Vec3 velocity = this.getDeltaMovement();
                if (velocity.x * velocity.x + velocity.z * velocity.z > 0.001) {
                     this.level().addParticle(ParticleTypes.SPLASH, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                }
            }
        } else {
            // Air drag
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.98, 0.98, 0.98));
        }
        
        this.move(MoverType.SELF, this.getDeltaMovement());
        if(floatingPool!=null) {
            var point=floatingPool.layout().clampToInterior(getX()-floatingPool.origin().getX(),getZ()-floatingPool.origin().getZ(),.5);
            this.setPos(floatingPool.origin().getX()+point.x(),getY(),floatingPool.origin().getZ()+point.z());
        }
    }

    @Override
    public void updateSwimming() {
        // Only allow vanilla swimming logic (particles) when moving horizontally
        Vec3 velocity = this.getDeltaMovement();
        if (velocity.x * velocity.x + velocity.z * velocity.z > 0.001) {
            super.updateSwimming();
        }
    }

    @Override
    public net.minecraft.world.item.ItemStack getPickResult() {
        return appearance();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (!this.level().isClientSide) {
                pickup(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (!this.level().isClientSide) {
            Vec3 look = player.getLookAngle();
            
            // Determine arm used
            HumanoidArm arm = player.getMainArm();
            if (hand == InteractionHand.OFF_HAND) {
                arm = arm.getOpposite();
            }
            
            // Add lateral force based on arm swing (Right arm swings Right->Left, so force is Left)
            Vec3 rightVector = look.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 lateralForce = arm == HumanoidArm.RIGHT ? rightVector.scale(-0.5) : rightVector.scale(0.5);
            
            Vec3 force = look.add(lateralForce).normalize();

            this.setDeltaMovement(force.x * 0.2, 0.05, force.z * 0.2);
            
            // Torque = r x F (Y component)
            // vec is relative to entity
            double torque = vec.z * look.x - vec.x * look.z;
            this.rotationalVelocity -= (float)torque * 60.0F;
            
            this.playSound(mod.crabmod.showercore.registers.SoundRegister.RUBBER_DUCK.get(), 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (!this.level().isClientSide) {
                pickup(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.interact(player, hand);
    }
}
