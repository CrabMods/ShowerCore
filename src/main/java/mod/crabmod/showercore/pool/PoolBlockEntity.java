package mod.crabmod.showercore.pool;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.registers.BlockEntitiesRegister;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Members carry a rendering snapshot; only the matching controller owns a tank. */
public final class PoolBlockEntity extends BlockEntity implements BathFluidSource {
    private UUID poolId;
    private BlockPos origin, owner;
    private PoolSettings settings;
    private FluidStack visibleFluid=FluidStack.EMPTY;
    private FluidTank tank;
    private boolean active, circulating, loading;
    private int paidMaterials;
    private int lastSynchronizedLight=-1;

    public PoolBlockEntity(BlockPos pos,BlockState state) { super(BlockEntitiesRegister.BATHING_POOL.get(),pos,state); }
    public boolean configured() { return poolId!=null&&settings!=null; }
    public UUID poolId() { return poolId; }
    public BlockPos origin() { return origin; }
    public BlockPos ownerPos() { return owner; }
    public PoolSettings settings() { return settings; }
    public PoolLayout layout() { return PoolLayout.of(settings.spec()); }
    public PoolLayout.Cell cell() { return layout().cell(worldPosition.getX()-origin.getX(),worldPosition.getZ()-origin.getZ()); }
    public boolean isController() { return configured()&&worldPosition.equals(owner)&&getBlockState().getValue(PoolBlock.CONTROLLER); }
    public boolean active() { return active&&!isRemoved(); }
    public boolean circulating() { return circulating&&!visibleFluid.isEmpty(); }
    public FluidStack fluid() { return visibleFluid; }
    public double waterY() { return layout().waterY(visibleFluid.getAmount()); }
    public int paidMaterials() { return paidMaterials; }

    void initialize(BlockPos origin,PoolSettings settings,UUID id) {
        this.origin=origin.immutable();this.settings=settings;this.poolId=id;
        this.owner=origin.offset(layout().controllerX(),0,0);
        tank=null;
        if(isController())makeTank();
        setChanged();
    }

    private void makeTank() {
        tank=new FluidTank(layout().capacity(),f->!CustomFluidAPI.isDynamicCustomFluid(f)||CustomFluidAPI.getCustomFluidDefinition(f).isPresent()) {
            @Override public int fill(FluidStack resource,IFluidHandler.FluidAction action) {
                return active()?super.fill(resource,action):0;
            }
            @Override public FluidStack drain(int amount,IFluidHandler.FluidAction action) {
                return active()?super.drain(amount,action):FluidStack.EMPTY;
            }
            @Override protected void onContentsChanged() {
                if(!loading) {
                    visibleFluid=getFluid().copy();
                    if(visibleFluid.isEmpty())circulating=false;
                    synchronize();
                }
            }
        };
    }

    @Nullable public PoolBlockEntity controller() {
        if(!configured()||level==null||!level.hasChunkAt(owner))return null;
        var entity=level.getBlockEntity(owner);
        return entity instanceof PoolBlockEntity pool&&pool.isController()&&pool.active()&&matches(pool)?pool:null;
    }
    public boolean matches(PoolBlockEntity other) {
        return configured()&&other.configured()&&poolId.equals(other.poolId)&&origin.equals(other.origin)&&settings.spec().equals(other.settings.spec());
    }
    @Nullable public IFluidHandler fluidHandler() {
        var controller=controller();return controller==null?null:controller.tank;
    }
    public void activate(int paid) { active=true;paidMaterials=paid;synchronize(); }
    public int retire() {
        int paid=paidMaterials;paidMaterials=0;active=false;circulating=false;setChanged();return paid;
    }
    public void changeFinish(PoolSettings value,int paid) { settings=value;paidMaterials=paid;synchronize(); }
    public void toggleFaucet() {
        if(!isController()||!active())return;
        circulating=!circulating&&!visibleFluid.isEmpty();synchronize();
    }

    public void synchronize() {
        if(level==null||level.isClientSide||!configured()||!isController())return;
        int liquidLight=luminosity();lastSynchronizedLight=liquidLight;
        for(var cell:layout().cells()) {
            BlockPos pos=origin.offset(cell.x(),0,cell.z());
            if(!level.hasChunkAt(pos))continue;
            if(level.getBlockEntity(pos) instanceof PoolBlockEntity part&&matches(part)) {
                part.settings=settings;part.visibleFluid=visibleFluid.copy();part.active=active;part.circulating=circulating;
                int light=part.cell().water().isEmpty()||visibleFluid.isEmpty()?0:liquidLight;
                var state=level.getBlockState(pos);
                if(state.getValue(PoolBlock.LIGHT)!=light)level.setBlock(pos,state.setValue(PoolBlock.LIGHT,light),3);
                part.setChanged();level.sendBlockUpdated(pos,part.getBlockState(),part.getBlockState(),3);
            }
        }
    }

    @Override @Nullable public ResourceLocation getCustomFluidId() { return CustomFluidAPI.getCustomFluidId(visibleFluid); }
    public BathtubBlock.LiquidType liquidType() {
        if(visibleFluid.isEmpty())return BathtubBlock.LiquidType.EMPTY;
        if(visibleFluid.getFluid()==Fluids.WATER)return BathtubBlock.LiquidType.WATER;
        if(getCustomFluidId()!=null)return BathtubBlock.LiquidType.CUSTOM;
        var id=BuiltInRegistries.FLUID.getKey(visibleFluid.getFluid());
        if(id.getNamespace().equals("hotbath")) {
            for(var type:BathtubBlock.LiquidType.values())if(id.getPath().equals(type.getSerializedName()+"_fluid"))return type;
        }
        return BathtubBlock.LiquidType.CUSTOM;
    }
    @Override public boolean emitsSteam() {
        return active()&&BathFluidSource.super.emitsSteam();
    }
    public boolean touchesWater(AABB bounds) {
        if(!configured()||visibleFluid.isEmpty()||bounds.minY>=origin.getY()+waterY()||bounds.maxY<=origin.getY()+PoolSpec.FLOOR_Y)return false;
        return PoolLayout.area(PoolLayout.clip(layout().waterOutline(),bounds.minX-origin.getX(),bounds.minZ-origin.getZ(),
                bounds.maxX-origin.getX(),bounds.maxZ-origin.getZ()))>1e-8;
    }

    @Override public boolean touchesBathFluid(AABB bounds) {
        return active()&&touchesWater(bounds);
    }

    @Override public void refreshBathDefinition() {
        if(isController()&&luminosity()!=lastSynchronizedLight)synchronize();
    }

    @Override public void onLoad() {
        super.onLoad();
        if(level!=null&&!level.isClientSide&&configured()) {
            if(level.hasChunkAt(owner))level.scheduleTick(owner,getBlockState().getBlock(),1);

        }
    }
    @Override public void setRemoved() {
        if(level!=null&&!level.isClientSide&&isController()) {
            for(var cell:layout().cells()) {
                var pos=origin.offset(cell.x(),0,cell.z());if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof PoolBlockEntity part)part.invalidateCaps();
            }
        }
        super.setRemoved();
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if(!configured())return;
        tag.putUUID("PoolId",poolId);tag.putLong("PoolOrigin",origin.asLong());tag.put("PoolSettings",settings.save());
        tag.putBoolean("PoolActive",active);tag.putBoolean("Circulating",circulating);
        tag.putInt("PaidMaterials",paidMaterials);tag.put("PoolFluid",visibleFluid.writeToNBT(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if(!tag.hasUUID("PoolId")) {poolId=null;settings=null;tank=null;active=false;visibleFluid=FluidStack.EMPTY;return;}
        try {
            var settings=PoolSettings.load(tag.getCompound("PoolSettings"));
            loading=true;initialize(BlockPos.of(tag.getLong("PoolOrigin")),settings,tag.getUUID("PoolId"));
            active=tag.getBoolean("PoolActive");paidMaterials=Math.max(0,Math.min(tag.getInt("PaidMaterials"),layout().materialCost()));
            visibleFluid=FluidStack.loadFluidStackFromNBT(tag.getCompound("PoolFluid"));
            if(visibleFluid.getAmount()>layout().capacity())throw new IllegalArgumentException("Invalid fluid volume");
            if(tank!=null)tank.setFluid(visibleFluid.copy());
            circulating=tag.getBoolean("Circulating")&&!visibleFluid.isEmpty();
        } catch(IllegalArgumentException error) { poolId=null;settings=null;tank=null;active=false; }
        finally { loading=false; }
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet) {
        load(packet.getTag());
    }
    @Override public double bathSurfaceY() {
        return origin.getY()+waterY();
    }
}
