package mod.crabmod.showercore.pool;

import com.mojang.serialization.MapCodec;
import mod.crabmod.showercore.entity.RubberDuckEntity;
import mod.crabmod.showercore.item.RubberDuckItem;
import mod.crabmod.showercore.registers.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class PoolBlock extends Block implements EntityBlock,BathFaucet,mod.crabmod.showercore.geometry.BathMountSurface {
    public static final MapCodec<PoolBlock> CODEC=simpleCodec(PoolBlock::new);
    public static final BooleanProperty CONTROLLER=BooleanProperty.create("controller");
    public static final IntegerProperty LIGHT=IntegerProperty.create("light",0,15);
    public PoolBlock(Properties properties) {
        super(properties.dynamicShape().lightLevel(s->s.getValue(LIGHT)));
        registerDefaultState(stateDefinition.any().setValue(CONTROLLER,false).setValue(LIGHT,0));
    }
    @Override protected MapCodec<? extends Block> codec() {return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {b.add(CONTROLLER,LIGHT);}
    @Override public RenderShape getRenderShape(BlockState state) {return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new PoolBlockEntity(pos,state);}
    @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return !level.isClientSide&&state.getValue(CONTROLLER)&&type==BlockEntitiesRegister.BATHING_POOL.get()
                ?(l,p,s,be)->PoolBlockEntity.tick(l,p,s,(PoolBlockEntity)be):null;
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        if(level.getBlockEntity(pos) instanceof PoolBlockEntity part&&part.configured()) {
            var cell=part.cell();if(cell!=null)return PoolCollision.shape(part.settings().spec(),cell.x(),cell.z());
        }
        return Shapes.empty();
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(held.getItem() instanceof RubberDuckItem||held.getItem() instanceof mod.crabmod.showercore.item.BathingPoolItem)return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean bucket=held.getItem() instanceof BucketItem||com.crabmod.hotbath.custom_fluid.CustomFluidAPI.hasCustomFluid(held);
        if(!(level.getBlockEntity(pos) instanceof PoolBlockEntity part))return bucket?ItemInteractionResult.CONSUME:ItemInteractionResult.FAIL;
        var pool=part.controller();if(pool==null)return bucket?ItemInteractionResult.CONSUME:ItemInteractionResult.FAIL;
        if(held.isEmpty()) {
            if(!level.isClientSide)PoolSeats.sit(pool,player,hit.getLocation());
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(level.isClientSide)return bucket?ItemInteractionResult.SUCCESS:ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(FluidUtil.interactWithFluidHandler(player,hand,pool.fluidHandler()))return ItemInteractionResult.CONSUME;
        return bucket?ItemInteractionResult.CONSUME:ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override public InteractionResult useFaucet(Level level,BlockPos pos,BlockState state,Player player) {
        if(!(level.getBlockEntity(pos) instanceof PoolBlockEntity part))return InteractionResult.PASS;
        var pool=part.controller();if(pool==null)return InteractionResult.FAIL;
        pool.toggleFaucet();return InteractionResult.CONSUME;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof PoolBlockEntity part) {
            var owner=part.controller();if(owner!=null)PoolStructure.dismantle(owner,!player.isCreative());
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if(!replacement.is(this)&&!level.isClientSide&&level.getBlockEntity(pos) instanceof PoolBlockEntity part) {
            var owner=part.isController()?part:part.controller();
            if(owner!=null)PoolStructure.dismantle(owner,true);
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder params) {return List.of();}
    @Override public ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state) {
        var stack=new ItemStack(ItemRegister.BATHING_POOL.get());
        if(level.getBlockEntity(pos) instanceof PoolBlockEntity part&&part.configured())PoolSettings.write(stack,part.settings());
        return stack;
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!(level.getBlockEntity(pos) instanceof PoolBlockEntity part)||!part.configured())return;
        if(part.isController()) {PoolStructure.validateLoaded(part);return;}
        if(level.hasChunkAt(part.ownerPos())) {
            var owner=part.controller();
            if(owner==null) {part.retire();level.removeBlock(pos,false);}
            else level.scheduleTick(owner.getBlockPos(),this,1); // Coalesce member loads into one structure update.
        }
    }

    @Override public mod.crabmod.showercore.geometry.BathMountSurface.Support showerSupport(Level level,BlockPos clicked,BlockHitResult hit,mod.crabmod.showercore.geometry.BathroomGeometry.Shower shower) {
        if(!(level.getBlockEntity(clicked) instanceof PoolBlockEntity part)||!part.configured()||part.cell()==null||!part.cell().hasRim())return null;
        var pool=part.controller();if(pool==null)return null;
        double x=hit.getLocation().x-pool.origin().getX(),z=hit.getLocation().z-pool.origin().getZ();
        var rim=pool.layout().closestRim(x,z);
        if(rim.point().distance(new PoolLayout.Point(x,z))>.55)return null;
        var facing=Direction.getNearest(-rim.normalX(),0,-rim.normalZ());
        mod.crabmod.showercore.geometry.BathMountSurface.Support best=null;double distance=Double.POSITIVE_INFINITY;
        var outline=pool.layout().outline();
        for(int i=0;i<outline.size();i++) {
            var a=outline.get(i);var b=outline.get((i+1)%outline.size());
            int steps=Math.max(1,(int)Math.ceil(a.distance(b)/.125));
            for(int j=0;j<=steps;j++) {
                var edge=a.lerp(b,j/(double)steps);var normal=pool.layout().closestRim(edge.x(),edge.z());
                if(Direction.getNearest(-normal.normalX(),0,-normal.normalZ())!=facing)continue;
                var point=pool.layout().clampToInterior(edge.x(),edge.z(),PoolSpec.RIM_WIDTH/2);
                if(point.distance(pool.layout().faucet())<.65)continue;
                double d=point.distance(new PoolLayout.Point(x,z));if(d>1.1||d>=distance)continue;
                var support=new mod.crabmod.showercore.geometry.BathMountSurface.Support(new Vec3(pool.origin().getX()+point.x(),pool.origin().getY()+PoolSpec.RIM_Y,pool.origin().getZ()+point.z()),facing);
                var outlet=support.place(shower).outlet(shower);
                double outletX=outlet.x-pool.origin().getX(),outletZ=outlet.z-pool.origin().getZ();
                if(!pool.settings().spec().contains(outletX,outletZ,PoolSpec.RIM_WIDTH))continue;
                best=support;distance=d;
            }
        }
        return best;
    }

}
