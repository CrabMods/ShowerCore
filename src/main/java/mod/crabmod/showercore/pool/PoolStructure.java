package mod.crabmod.showercore.pool;

import mod.crabmod.showercore.entity.FaucetInteractionEntity;
import mod.crabmod.showercore.entity.SeatEntity;
import mod.crabmod.showercore.registers.BlocksRegister;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import java.util.*;

/** Server-thread transaction: validate, place, allow cancellation, then charge once. */
public final class PoolStructure {
    private PoolStructure() {}
    public static int available(Player player,Item material) {
        int total=0;
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            var stack=player.getInventory().getItem(i);if(stack.is(material))total+=stack.getCount();
        }
        return total;
    }
    public static String placementProblem(Level level,BlockPos origin,PoolSettings settings,Player player) {
        if(!PoolSettings.validMaterial(settings.material()))return "invalid_material";
        var layout=PoolLayout.of(settings.spec());
        if(!player.isCreative()&&available(player,BuiltInRegistries.BLOCK.get(settings.material()).asItem())<layout.materialCost())return "materials";
        for(var cell:layout.cells()) {
            var pos=origin.offset(cell.x(),0,cell.z());
            if(pos.getY()<level.getMinBuildHeight()||pos.getY()>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(pos))return "boundary";
            if(!level.hasChunkAt(pos))return "unloaded";
            if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,player.getMainHandItem()))return "permission";
            if(!level.getBlockState(pos).isAir())return "obstruction";
            for(var box:PoolCollision.shape(settings.spec(),cell.x(),cell.z()).toAabbs()) {
                var world=box.move(pos);
                if(!level.getEntitiesOfClass(LivingEntity.class,world,e->!e.isSpectator()).isEmpty())return "occupied";
            }
        }
        var tap=origin.offset(layout.controllerX(),1,0);
        if(!level.getBlockState(tap).isAir())return "obstruction";
        return "";
    }

    public static boolean place(Level level,BlockPos origin,PoolSettings settings,Player player) {
        if(level.isClientSide)return false;
        String problem=placementProblem(level,origin,settings,player);
        if(!problem.isEmpty()) { player.displayClientMessage(Component.translatable("message.showercore.pool."+problem),true);return false; }
        var layout=PoolLayout.of(settings.spec());var id=UUID.randomUUID();
        var snapshots=new ArrayList<BlockSnapshot>();var placed=new ArrayList<BlockPos>();
        PoolBlockEntity controller=null;boolean accepted=false;
        try {
            for(var cell:layout.cells()) {
                var pos=origin.offset(cell.x(),0,cell.z());
                snapshots.add(BlockSnapshot.create(level.dimension(),level,pos,18));
                var state=BlocksRegister.BATHING_POOL.get().defaultBlockState().setValue(PoolBlock.CONTROLLER,cell.x()==layout.controllerX()&&cell.z()==0);
                if(!level.setBlock(pos,state,18))return false;
                placed.add(pos);
                if(!(level.getBlockEntity(pos) instanceof PoolBlockEntity part))return false;
                part.initialize(origin,settings,id);
                if(part.isController())controller=part;
            }
            if(controller==null||EventHooks.onMultiBlockPlace(player,snapshots,Direction.UP))return false;
            for(var pos:placed)if(!(level.getBlockEntity(pos) instanceof PoolBlockEntity part)||!id.equals(part.poolId()))return false;
            Item material=BuiltInRegistries.BLOCK.get(settings.material()).asItem();
            if(!player.isCreative()&&available(player,material)<layout.materialCost())return false;
            if(!player.isCreative()) {
                int left=layout.materialCost();
                for(int i=0;i<player.getInventory().getContainerSize()&&left>0;i++) {
                    var stack=player.getInventory().getItem(i);if(!stack.is(material))continue;
                    int amount=Math.min(left,stack.getCount());stack.shrink(amount);left-=amount;
                }
                player.getInventory().setChanged();
            }
            controller.activate(player.isCreative()?0:layout.materialCost());
            ensureFaucet(controller);
            for(var pos:placed)level.updateNeighborsAt(pos,BlocksRegister.BATHING_POOL.get());
            accepted=true;return true;
        } finally {
            if(!accepted)for(int i=0;i<placed.size();i++) {
                var pos=placed.get(i);
                var be=level.getBlockEntity(pos);
                if(level.getBlockState(pos).is(BlocksRegister.BATHING_POOL.get())&&(be==null||be instanceof PoolBlockEntity part&&(!part.configured()||id.equals(part.poolId())))) {
                    if(be instanceof PoolBlockEntity part)part.retire();snapshots.get(i).restore(18);
                }
            }
        }
    }

    public static void ensureFaucet(PoolBlockEntity controller) {
        var level=controller.getLevel();if(level==null||level.isClientSide||!controller.active())return;
        var pos=controller.getBlockPos();
        if(level.getEntitiesOfClass(FaucetInteractionEntity.class,new AABB(pos)).stream().anyMatch(e->!e.isRemoved()))return;
        var point=controller.layout().faucet();
        level.addFreshEntity(new FaucetInteractionEntity(level,controller.origin().getX()+point.x(),pos.getY()+.9,
                controller.origin().getZ()+point.z()));
    }

    public static void validateLoaded(PoolBlockEntity controller) {
        if(!controller.active())return;
        var level=controller.getLevel();if(level==null||level.isClientSide)return;
        for(var cell:controller.layout().cells()) {
            var pos=controller.origin().offset(cell.x(),0,cell.z());
            if(!level.hasChunkAt(pos))continue;
            if(!(level.getBlockEntity(pos) instanceof PoolBlockEntity part)||!part.matches(controller)) {
                dismantle(controller,true);return;
            }
        }
        controller.synchronize();ensureFaucet(controller);
    }

    public static void dismantle(PoolBlockEntity controller,boolean refund) {
        if(!controller.isController()||!controller.active())return;
        var level=controller.getLevel();if(level==null||level.isClientSide)return;
        int paid=controller.retire();
        var origin=controller.origin();var spec=controller.settings().spec();
        for(var seat:level.getEntitiesOfClass(SeatEntity.class,new AABB(origin).expandTowards(spec.width(),3,spec.length()))) {
            if(controller.poolId().equals(seat.poolId())) { seat.ejectPassengers();seat.discard(); }
        }
        for(var faucet:level.getEntitiesOfClass(FaucetInteractionEntity.class,new AABB(controller.getBlockPos())))faucet.discard();
        for(var cell:controller.layout().cells()) {
            var pos=origin.offset(cell.x(),0,cell.z());
            if(!level.hasChunkAt(pos))continue;
            if(level.getBlockEntity(pos) instanceof PoolBlockEntity part&&part.matches(controller)) {
                part.retire();level.removeBlock(pos,false);
            }
        }
        if(refund) {
            var item=BuiltInRegistries.BLOCK.get(controller.settings().material()).asItem();
            while(paid>0) {int count=Math.min(paid,item.getDefaultMaxStackSize());Block.popResource(level,controller.getBlockPos(),new ItemStack(item,count));paid-=count;}
        }
    }
    /** Appearance changes preserve the structure, shared liquid and occupied seats. */
    public static boolean refinish(PoolBlockEntity owner,PoolSettings target,Player player,ItemStack sample) {
        var level=owner.getLevel();
        if(level==null||level.isClientSide||!owner.active()||!owner.isController()||owner.controller()!=owner
                ||!target.spec().equals(owner.settings().spec())||!PoolSettings.validMaterial(target.material()))return false;
        var before=owner.settings();
        if(target.material().equals(before.material())&&target.face()==before.face())return true;
        var snapshots=new ArrayList<BlockSnapshot>();
        for(var cell:owner.layout().cells()) {
            var pos=owner.origin().offset(cell.x(),0,cell.z());
            if(!level.hasChunkAt(pos))continue;
            if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,player.getMainHandItem()))return false;
            if(!(level.getBlockEntity(pos) instanceof PoolBlockEntity part)||!part.matches(owner))return false;
            snapshots.add(BlockSnapshot.create(level.dimension(),level,pos));
        }
        if(snapshots.isEmpty()||EventHooks.onMultiBlockPlace(player,snapshots,Direction.UP)||!owner.active())return false;
        boolean newMaterial=!target.material().equals(before.material());
        int charge=player.isCreative()||!newMaterial?0:owner.layout().materialCost();
        Item material=BuiltInRegistries.BLOCK.get(target.material()).asItem();
        if(available(player,material)+(sample.is(material)?sample.getCount():0)<charge)return false;
        int left=charge;
        if(sample.is(material)) {int taken=Math.min(left,sample.getCount());sample.shrink(taken);left-=taken;}
        for(int i=0;i<player.getInventory().getContainerSize()&&left>0;i++) {
            var stack=player.getInventory().getItem(i);if(!stack.is(material))continue;
            int taken=Math.min(left,stack.getCount());stack.shrink(taken);left-=taken;
        }
        int refund=newMaterial&&!player.isCreative()?owner.paidMaterials():0;
        int paid=newMaterial?charge:owner.paidMaterials();
        owner.changeFinish(new PoolSettings(before.spec(),target.material(),target.face(),before.roomSelection()),paid);
        if(refund>0) {
            Item old=BuiltInRegistries.BLOCK.get(before.material()).asItem();
            while(refund>0) {
                int n=Math.min(refund,new ItemStack(old).getMaxStackSize());var returned=new ItemStack(old,n);
                if(!player.getInventory().add(returned))player.drop(returned,false);refund-=n;
            }
        }
        player.getInventory().setChanged();return true;
    }

}
