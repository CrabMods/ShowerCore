package mod.crabmod.showercore.item;

import mod.crabmod.showercore.pool.*;
import mod.crabmod.showercore.menu.PoolConfigurationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

/** Reusable planner. Construction pays from inventory only after a valid preview is confirmed. */
public final class BathingPoolItem extends Item {
    public BathingPoolItem(Properties properties) {super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        open(level,player,hand);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    private static void open(Level level,Player player,InteractionHand hand) {open(level,player,hand,null);}
    private static void open(Level level,Player player,InteractionHand hand,PoolBlockEntity pool) {
        if(!level.isClientSide)player.openMenu(new SimpleMenuProvider((id,inventory,viewer)->{
            var menu=new PoolConfigurationMenu(id,inventory);if(pool!=null)menu.bindPool(pool);
            var planner=player.getItemInHand(hand);player.setItemInHand(hand,ItemStack.EMPTY);
            menu.getSlot(0).set(planner);return menu;
        },Component.translatable(pool==null?"menu.showercore.pool.title":"menu.showercore.pool.finish_title")));
    }
    @Override public InteractionResult useOn(UseOnContext use) {
        var player=use.getPlayer();if(player==null)return InteractionResult.FAIL;
        var level=use.getLevel();var stack=use.getItemInHand();
        if(level.getBlockEntity(use.getClickedPos()) instanceof PoolBlockEntity pool&&pool.configured()) {
            var owner=pool.controller();if(owner==null)return InteractionResult.CONSUME;
            if(!level.isClientSide)PoolSettings.write(stack,owner.settings());
            open(level,player,use.getHand(),owner);return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(use.getClickedFace()!=net.minecraft.core.Direction.UP)return InteractionResult.FAIL;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        var settings=PoolSettings.read(stack);
        if(settings==null) {player.displayClientMessage(Component.translatable("message.showercore.pool.invalid_plan"),true);return InteractionResult.FAIL;}
        var data=PoolSettings.data(stack);String dimension=level.dimension().location().toString();
        var target=use.getClickedPos().above();
        if(player.isShiftKeyDown()) {
            data.remove("PoolFirst");data.remove("PoolReady");data.remove("PoolDimension");PoolSettings.data(stack,data);
            player.displayClientMessage(Component.translatable("message.showercore.pool.cancelled"),true);return InteractionResult.CONSUME;
        }
        if(!dimension.equals(data.getString("PoolDimension"))) {data.remove("PoolFirst");data.remove("PoolReady");}
        if(data.contains("PoolReady")) {
            BlockPos origin=BlockPos.of(data.getLong("PoolReady"));
            boolean within=target.getY()==origin.getY()&&target.getX()>=origin.getX()&&target.getX()<origin.getX()+settings.spec().width()
                    &&target.getZ()>=origin.getZ()&&target.getZ()<origin.getZ()+settings.spec().length();
            if(within) {
                if(PoolStructure.place(level,origin,settings,player)) {
                    data.remove("PoolReady");data.remove("PoolFirst");data.remove("PoolDimension");PoolSettings.data(stack,data);
                    player.displayClientMessage(Component.translatable("message.showercore.pool.built"),true);
                }
                return InteractionResult.CONSUME;
            }
            data.remove("PoolReady");
        }
        if(settings.roomSelection()) {
            if(!data.contains("PoolFirst")) {
                data.putLong("PoolFirst",target.asLong());data.putString("PoolDimension",dimension);PoolSettings.data(stack,data);
                player.displayClientMessage(Component.translatable("message.showercore.pool.second_corner"),true);return InteractionResult.CONSUME;
            }
            BlockPos first=BlockPos.of(data.getLong("PoolFirst"));
            int width=Math.abs(target.getX()-first.getX())+1,length=Math.abs(target.getZ()-first.getZ())+1;
            if(first.getY()!=target.getY()||width<PoolSpec.MIN_SIZE||length<PoolSpec.MIN_SIZE||width>PoolSpec.MAX_SIZE||length>PoolSpec.MAX_SIZE) {
                player.displayClientMessage(Component.translatable("message.showercore.pool.selection"),true);return InteractionResult.CONSUME;
            }
            settings=new PoolSettings(new PoolSpec(PoolSpec.Shape.RECTANGLE,width,length,0),settings.material(),settings.face(),true);
            PoolSettings.write(stack,settings);data=PoolSettings.data(stack);
            target=new BlockPos(Math.min(first.getX(),target.getX()),target.getY(),Math.min(first.getZ(),target.getZ()));
        }
        data.putLong("PoolReady",target.asLong());data.putString("PoolDimension",dimension);PoolSettings.data(stack,data);
        player.displayClientMessage(Component.translatable("message.showercore.pool.confirm",PoolLayout.of(settings.spec()).materialCost()),true);
        return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack,@org.jetbrains.annotations.Nullable Level context,List<Component> lines,TooltipFlag flag) {
        var settings=PoolSettings.read(stack);
        if(settings!=null) {
            lines.add(Component.translatable("tooltip.showercore.pool.size",settings.spec().width(),settings.spec().length()));
            lines.add(Component.translatable("tooltip.showercore.pool.cost",PoolLayout.of(settings.spec()).materialCost()));
        }
        lines.add(Component.translatable("tooltip.showercore.pool.usage"));
    }
}
