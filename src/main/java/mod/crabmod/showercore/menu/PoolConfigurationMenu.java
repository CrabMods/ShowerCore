package mod.crabmod.showercore.menu;

import mod.crabmod.showercore.item.BathingPoolItem;
import mod.crabmod.showercore.material.BathtubMaterialFace;
import mod.crabmod.showercore.pool.*;
import mod.crabmod.showercore.registers.MenuRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public final class PoolConfigurationMenu extends AbstractContainerMenu {
    public static final int WIDTH_DOWN=10,WIDTH_UP=11,LENGTH_DOWN=12,LENGTH_UP=13,RADIUS_DOWN=14,RADIUS_UP=15;
    public static final int SIDE=20,TOP=21,ROOM=30;
    private final Player owner;
    private final ContainerData fields=new SimpleContainerData(9);
    private boolean changing,closed;
    private PoolBlockEntity boundPool;
    public boolean bound() {return fields.get(7)==1;}
    public void bindPool(PoolBlockEntity pool) {boundPool=pool;fields.set(7,1);broadcastChanges();}
    private final Container inputs=new SimpleContainer(2) {
        @Override public void setChanged() {super.setChanged();slotsChanged(this);}
    };
    public PoolConfigurationMenu(int id,Inventory inventory) {
        super(MenuRegister.POOL_CONFIGURATION.get(),id);owner=inventory.player;
        fields.set(0,PoolSpec.DEFAULT.shape().ordinal());fields.set(1,5);fields.set(2,5);fields.set(3,4);
        addDataSlots(fields);
        addSlot(new Slot(inputs,0,16,28) {@Override public boolean mayPlace(ItemStack stack){return stack.getItem() instanceof BathingPoolItem;}});
        addSlot(new Slot(inputs,1,16,72) {@Override public boolean mayPlace(ItemStack stack){return BathtubMaterialMenu.isMaterial(stack);}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,44+col*18,196+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,44+col*18,254));
    }
    public PoolSpec spec() {
        int shape=Math.max(0,Math.min(2,fields.get(0))),w=Math.max(3,Math.min(16,fields.get(1))),l=Math.max(3,Math.min(16,fields.get(2)));
        return new PoolSpec(PoolSpec.Shape.values()[shape],w,shape==2?w:l,shape==1?Math.max(1,Math.min(fields.get(3),Math.min(w,l)*2)):0);
    }
    public BathtubMaterialFace face() {return fields.get(4)==1?BathtubMaterialFace.TOP:BathtubMaterialFace.SIDE;}
    public boolean roomSelection() {return fields.get(5)==1;}
    public int required() {return owner.isCreative()?0:bound()?fields.get(8):PoolLayout.of(spec()).materialCost();}
    public int available() {return fields.get(6);}

    @Override public void slotsChanged(Container container) {
        if(changing||closed||owner.level().isClientSide)return;
        var input=inputs.getItem(0);
        if(!(input.getItem() instanceof BathingPoolItem))return;
        var settings=PoolSettings.read(input);if(settings==null)return;
        var sample=inputs.getItem(1);
        if(BathtubMaterialMenu.isMaterial(sample))settings=new PoolSettings(settings.spec(),BuiltInRegistries.BLOCK.getKey(((BlockItem)sample.getItem()).getBlock()),settings.face(),settings.roomSelection());
        if(boundPool!=null)settings=new PoolSettings(boundPool.settings().spec(),settings.material(),settings.face(),boundPool.settings().roomSelection());
        apply(settings);super.slotsChanged(container);
    }
    private void apply(PoolSettings settings) {
        changing=true;
        try {
            PoolSettings.write(inputs.getItem(0),settings);
            fields.set(0,settings.spec().shape().ordinal());fields.set(1,settings.spec().width());fields.set(2,settings.spec().length());fields.set(3,settings.spec().radiusQuarters());
            fields.set(4,settings.face()==BathtubMaterialFace.TOP?1:0);fields.set(5,settings.roomSelection()?1:0);
            inputs.setChanged();
        } finally {changing=false;}
        broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player,int id) {
        if(player!=owner||closed||!(inputs.getItem(0).getItem() instanceof BathingPoolItem))return false;
        var current=PoolSettings.read(inputs.getItem(0));if(current==null)return false;
        if(bound()&&id==40) {
            if(boundPool==null||!boundPool.active()||!PoolStructure.refinish(boundPool,current,player,inputs.getItem(1))) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.showercore.pool.finish_failed"),true);return false;
            }
            player.closeContainer();return true;
        }
        if(bound()&&id!=SIDE&&id!=TOP)return false;
        var spec=current.spec();var face=current.face();boolean room=current.roomSelection();
        if(id>=0&&id<3) {spec=spec.withShape(PoolSpec.Shape.values()[id]);room=false;}
        else switch(id) {
            case WIDTH_DOWN,WIDTH_UP -> spec=spec.resized(Math.max(3,Math.min(16,spec.width()+(id==WIDTH_UP?1:-1))),spec.length());
            case LENGTH_DOWN,LENGTH_UP -> {if(spec.shape()==PoolSpec.Shape.CIRCLE)return false;spec=spec.resized(spec.width(),Math.max(3,Math.min(16,spec.length()+(id==LENGTH_UP?1:-1))));}
            case RADIUS_DOWN,RADIUS_UP -> {if(spec.shape()!=PoolSpec.Shape.ROUNDED)return false;spec=new PoolSpec(spec.shape(),spec.width(),spec.length(),Math.max(1,Math.min(Math.min(spec.width(),spec.length())*2,spec.radiusQuarters()+(id==RADIUS_UP?1:-1))));}
            case SIDE -> face=BathtubMaterialFace.SIDE;
            case TOP -> face=BathtubMaterialFace.TOP;
            case ROOM -> {room=!room;spec=spec.withShape(PoolSpec.Shape.RECTANGLE);}
            default -> {return false;}
        }
        apply(new PoolSettings(spec,current.material(),face,room));return true;
    }
    @Override public void broadcastChanges() {
        if(!owner.level().isClientSide) {
            var settings=PoolSettings.read(inputs.getItem(0));
            int count=settings==null?0:PoolStructure.available(owner,BuiltInRegistries.BLOCK.get(settings.material()).asItem());
            var sample=inputs.getItem(1);
            if(settings!=null&&sample.is(BuiltInRegistries.BLOCK.get(settings.material()).asItem()))count+=sample.getCount();
            fields.set(6,count);
            int charge=owner.isCreative()||settings==null?0:PoolLayout.of(settings.spec()).materialCost();
            if(boundPool!=null&&settings!=null&&settings.material().equals(boundPool.settings().material()))charge=0;
            fields.set(8,charge);
        }
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player) {return player==owner&&!closed;}
    @Override public void removed(Player player) {
        super.removed(player);if(closed)return;closed=true;
        if(!player.level().isClientSide)clearContainer(player,inputs);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(closed||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<2) {if(!moveItemStackTo(stack,2,38,true))return ItemStack.EMPTY;}
        else if(stack.getItem() instanceof BathingPoolItem) {if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(BathtubMaterialMenu.isMaterial(stack)) {if(!moveItemStackTo(stack,1,2,false))return ItemStack.EMPTY;}
        else if(index<29) {if(!moveItemStackTo(stack,29,38,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,2,29,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return original;
    }
}
