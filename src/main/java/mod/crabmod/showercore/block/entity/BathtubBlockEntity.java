package mod.crabmod.showercore.block.entity;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
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
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> fluidTank);

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
        // Also initializes the light property when loading saves made before it existed.
        updateLightLevel();
    }

    private void updateLightLevel() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(getBlockPos());
        int light = state.getValue(BathtubBlock.LIQUID) == BathtubBlock.LiquidType.CUSTOM
                ? getCustomFluidDefinition().map(CustomFluidDefinition::luminosity).orElse(0) : 0;
        light = Math.max(0, Math.min(15, light));
        // The light worker cannot read server block entities. A native state property
        // gives both sides the same emission and lets vanilla propagate normal updates.
        if (state.getValue(BathtubBlock.LIGHT_LEVEL) != light) {
            level.setBlock(getBlockPos(), state.setValue(BathtubBlock.LIGHT_LEVEL, light), 3);
        }
    }

    public Optional<CustomFluidDefinition> getCustomFluidDefinition() {
        if (customFluidId == null) {
            return Optional.empty();
        }
        return CustomFluidAPI.getFluidDefinition(customFluidId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fluidTank.readFromNBT(tag);
        if (tag.contains(TAG_CUSTOM_FLUID_ID)) {
            customFluidId = ResourceLocation.tryParse(tag.getString(TAG_CUSTOM_FLUID_ID));
        } else {
            customFluidId = null;
        }
        if (tag.contains(TAG_MATERIAL_BLOCK_ID)) {
            materialBlockId = ResourceLocation.tryParse(tag.getString(TAG_MATERIAL_BLOCK_ID));
        } else {
            materialBlockId = null;
        }
        materialFace = BathtubMaterialFace.read(tag.getString(BathtubMaterialFace.TAG));
        requestModelDataUpdate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        fluidTank.writeToNBT(tag);
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
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }
}
