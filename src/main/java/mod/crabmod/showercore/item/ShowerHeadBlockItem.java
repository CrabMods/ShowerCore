package mod.crabmod.showercore.item;

import mod.crabmod.showercore.block.ShowerHeadBlock;
import mod.crabmod.showercore.entity.ShowerHeadContainerEntity;
import mod.crabmod.showercore.geometry.BathMountSurface;
import net.minecraft.core.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.Nullable;

/** Top/side installation on a declared bathing surface; ordinary walls use native placement. */
public final class ShowerHeadBlockItem extends BlockItem {
    public ShowerHeadBlockItem(ShowerHeadBlock block,Properties properties) {super(block,properties);}
    @Override public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(
            net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand) {
        return mod.crabmod.showercore.material.BathroomMaterials.open(level,player,hand);
    }
    @Override public InteractionResult useOn(UseOnContext use) {
        var level=use.getLevel();var state=level.getBlockState(use.getClickedPos());
        if(use.getClickedFace()==Direction.DOWN||!(state.getBlock() instanceof BathMountSurface surface))return super.useOn(use);
        var shower=(ShowerHeadBlock)getBlock();
        var support=surface.showerSupport(level,use.getClickedPos(),new BlockHitResult(use.getClickLocation(),use.getClickedFace(),use.getClickedPos(),false),shower.geometry());
        if(support==null)return InteractionResult.FAIL;
        var placement=support.place(shower.geometry());
        var context=new MountContext(use,placement);var player=use.getPlayer();
        var box=placement.clearance(shower.geometry());
        var from=BlockPos.containing(box.minX,box.minY,box.minZ);
        var to=BlockPos.containing(Math.nextDown(box.maxX),Math.nextDown(box.maxY),Math.nextDown(box.maxZ));
        for(var pos:BlockPos.betweenClosed(from,to)) {
            if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||pos.getY()<level.getMinBuildHeight()||pos.getY()>=level.getMaxBuildHeight()
                    ||(player!=null&&(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,use.getItemInHand())))
                    ||(!pos.equals(support.baseBlock())&&!level.getBlockState(pos).canBeReplaced(context)))return InteractionResult.FAIL;
        }
        return place(context);
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        var result=super.place(context);
        if(result.consumesAction()&&!context.getLevel().isClientSide&&context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ShowerHeadContainerEntity shower) {
            shower.setHorizontalMount(context instanceof MountContext mount?mount.placement.xShift():0,
                    context instanceof MountContext mount?mount.placement.zShift():0);
        }
        return result;
    }
    @Override @Nullable protected BlockState getPlacementState(BlockPlaceContext context) {
        var state=super.getPlacementState(context);
        return state!=null&&context instanceof MountContext mount
                ?ShowerHeadBlock.withMountOffset(state.setValue(ShowerHeadBlock.FACING,mount.placement.facing()),mount.placement.offsetPixels()):state;
    }
    private static final class MountContext extends BlockPlaceContext {
        private final BathMountSurface.Placement placement;
        private MountContext(UseOnContext use,BathMountSurface.Placement placement) {
            super(use.getLevel(),use.getPlayer(),use.getHand(),use.getItemInHand(),new BlockHitResult(Vec3.atCenterOf(placement.pos()),Direction.UP,placement.pos(),false));
            this.replaceClicked=true;this.placement=placement;
        }
        @Override public boolean canPlace(){return getLevel().getBlockState(getClickedPos()).canBeReplaced(this);}
    }
}
