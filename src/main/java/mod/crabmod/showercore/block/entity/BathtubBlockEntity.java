package mod.crabmod.showercore.block.entity;

import mod.crabmod.showercore.registers.BlockEntitiesRegister;
import mod.crabmod.showercore.block.BathtubBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.crabmod.showercore.material.BathtubMaterialFace;

public class BathtubBlockEntity extends BlockEntity implements mod.crabmod.showercore.pool.BathFluidSource {
    private static final String TAG_CUSTOM_FLUID_ID = "CustomFluidId";
    public static final String TAG_MATERIAL_BLOCK_ID = "MaterialBlockId";

    private final FluidTank fluidTank = new FluidTank(1000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    @Nullable
    private ResourceLocation customFluidId;
    @Nullable
    private ResourceLocation materialBlockId;
    private BathtubMaterialFace materialFace = BathtubMaterialFace.LEGACY;

    public BathtubBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesRegister.BATHTUB_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    public ResourceLocation getCustomFluidId() {
        return customFluidId;
    }

    public void setCustomFluidId(@Nullable ResourceLocation customFluidId) {
        this.customFluidId = customFluidId;
        if(customFluidId!=null)com.crabmod.hotbath.custom_fluid.CustomFluidAPI.setCustomFluidId(fluidTank.getFluid(),customFluidId);
        setChanged();
        updateLightLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    public ResourceLocation getMaterialBlockId() {
        return materialBlockId;
    }

    public BathtubMaterialFace getMaterialFace() {
        return materialFace;
    }

    public void setMaterialBlockId(@Nullable ResourceLocation materialBlockId) {
        setMaterial(materialBlockId, BathtubMaterialFace.LEGACY);
    }

    public void setMaterial(@Nullable ResourceLocation materialBlockId, BathtubMaterialFace face) {
        if (java.util.Objects.equals(this.materialBlockId, materialBlockId) && materialFace == face) return;
        this.materialBlockId = materialBlockId;
        this.materialFace = face;
        setChanged();
        requestModelDataUpdate();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        mod.crabmod.showercore.event.BathDefinitionEvents.loaded(this);
        // Also initializes the light property when loading saves made before it existed.
        updateLightLevel();
    }

    private void updateLightLevel() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(getBlockPos());
        int light=luminosity();
        // The light worker cannot read server block entities. A native state property
        // gives both sides the same emission and lets vanilla propagate normal updates.
        if (state.getValue(BathtubBlock.LIGHT_LEVEL) != light) {
            level.setBlock(getBlockPos(), state.setValue(BathtubBlock.LIGHT_LEVEL, light), 3);
        }
    }

    @Override
    public net.neoforged.neoforge.fluids.FluidStack fluid() {
        return fluidTank.getFluid();
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidTank.readFromNBT(registries, tag);
        if (tag.contains(TAG_CUSTOM_FLUID_ID)) {
            customFluidId = ResourceLocation.tryParse(tag.getString(TAG_CUSTOM_FLUID_ID));
        } else {
            customFluidId = null;
        }
        if(customFluidId!=null)com.crabmod.hotbath.custom_fluid.CustomFluidAPI.setCustomFluidId(fluidTank.getFluid(),customFluidId);
        if (tag.contains(TAG_MATERIAL_BLOCK_ID)) {
            materialBlockId = ResourceLocation.tryParse(tag.getString(TAG_MATERIAL_BLOCK_ID));
        } else {
            materialBlockId = null;
        }
        materialFace = BathtubMaterialFace.read(tag.getString(BathtubMaterialFace.TAG));
        requestModelDataUpdate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluidTank.writeToNBT(registries, tag);
        if (customFluidId != null) {
            tag.putString(TAG_CUSTOM_FLUID_ID, customFluidId.toString());
        }
        if (materialBlockId != null) {
            tag.putString(TAG_MATERIAL_BLOCK_ID, materialBlockId.toString());
            if (materialFace != BathtubMaterialFace.LEGACY) {
                tag.putString(BathtubMaterialFace.TAG, materialFace.serializedName());
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    @Override
    public boolean touchesBathFluid(net.minecraft.world.phys.AABB bounds) {
        if(fluid().isEmpty())return false;
        var state=getBlockState();var facing=state.getValue(BathtubBlock.FACING);
        var part=state.getValue(BathtubBlock.PART);
        double minX=.125,maxX=.875,minZ=.125,maxZ=.875;
        var connection=part==net.minecraft.world.level.block.state.properties.BedPart.FOOT?facing:facing.getOpposite();
        switch(connection){case NORTH->minZ=0;case SOUTH->maxZ=1;case WEST->minX=0;case EAST->maxX=1;default->{}}
        return bounds.intersects(new net.minecraft.world.phys.AABB(worldPosition.getX()+minX,worldPosition.getY()+.25,worldPosition.getZ()+minZ,
                worldPosition.getX()+maxX,worldPosition.getY()+BathtubBlock.waterLevelFor(state),worldPosition.getZ()+maxZ));
    }
    @Override public double bathSurfaceY() {
        return worldPosition.getY()+BathtubBlock.waterLevelFor(getBlockState());
    }
    @Override public void refreshBathDefinition() {
        updateLightLevel();
    }
    @Override public void setRemoved() {
        mod.crabmod.showercore.event.BathDefinitionEvents.removed(this);super.setRemoved();
    }
}
