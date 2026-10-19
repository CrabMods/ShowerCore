package mod.crabmod.showercore.client.screen;

import mod.crabmod.showercore.menu.PoolConfigurationMenu;
import mod.crabmod.showercore.pool.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;

public final class PoolConfigurationScreen extends AbstractContainerScreen<PoolConfigurationMenu> {
    private final Map<Integer,Button> buttons=new HashMap<>();
    public PoolConfigurationScreen(PoolConfigurationMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=248;imageHeight=278;inventoryLabelX=44;inventoryLabelY=184;
    }
    private void button(String key,int id,int x,int y,int width) {
        buttons.put(id,addRenderableWidget(Button.builder(Component.translatable(key),b->{
            if(id==40&&!menu.bound())minecraft.player.closeContainer();
            else minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id==20&&menu.face()==mod.crabmod.showercore.material.BathtubMaterialFace.SIDE?21:id);
        })
                .bounds(leftPos+x,topPos+y,width,20).build()));
    }
    @Override protected void init() {
        super.init();buttons.clear();
        button("menu.showercore.pool.rectangle",0,58,22,55);
        button("menu.showercore.pool.rounded",1,117,22,55);
        button("menu.showercore.pool.circle",2,176,22,55);
        button("menu.showercore.pool.minus",10,58,48,18);button("menu.showercore.pool.plus",11,116,48,18);
        button("menu.showercore.pool.minus",12,148,48,18);button("menu.showercore.pool.plus",13,206,48,18);
        button("menu.showercore.pool.minus",14,58,74,18);button("menu.showercore.pool.plus",15,116,74,18);
        button("menu.showercore.material.face.side",20,148,74,76);
        button("menu.showercore.pool.room",30,12,110,94);
        button("menu.showercore.pool.done",40,12,136,94);
    }
    @Override protected void containerTick() {
        super.containerTick();
        var spec=menu.spec();
        for(int i=0;i<3;i++)buttons.get(i).active=spec.shape().ordinal()!=i||menu.roomSelection();
        buttons.get(12).active=buttons.get(13).active=spec.shape()!=PoolSpec.Shape.CIRCLE;
        buttons.get(14).active=buttons.get(15).active=spec.shape()==PoolSpec.Shape.ROUNDED;
        buttons.get(20).setMessage(Component.translatable("menu.showercore.material.face."+menu.face().serializedName()));
        if(menu.bound())for(int id:new int[]{0,1,2,10,11,12,13,14,15,30})buttons.get(id).active=false;
        buttons.get(40).setMessage(Component.translatable(menu.bound()?"menu.showercore.pool.apply_finish":"menu.showercore.pool.done"));
        if(menu.bound())buttons.get(40).active=menu.available()>=menu.required();
        buttons.get(30).setMessage(Component.translatable(menu.roomSelection()?"menu.showercore.pool.room_on":"menu.showercore.pool.room"));
    }
    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY) {
        int x=leftPos,y=topPos;
        g.fill(x,y,x+imageWidth,y+imageHeight,0xFF373431);g.fill(x+1,y+1,x+imageWidth-1,y+imageHeight-1,0xFFC6C6C6);
        g.fill(x+2,y+2,x+imageWidth-2,y+3,0xFFFFFFFF);
        for(var slot:menu.slots) {
            int sx=x+slot.x,sy=y+slot.y;g.fill(sx-1,sy-1,sx+17,sy+17,0xFF373737);
            g.fill(sx,sy,sx+17,sy+17,0xFFFFFFFF);g.fill(sx,sy,sx+16,sy+16,0xFF8B8B8B);
        }
        var spec=menu.spec();
        center(g,Component.translatable("menu.showercore.pool.width",spec.width()),96,54);
        center(g,Component.translatable("menu.showercore.pool.length",spec.length()),186,54);
        center(g,Component.translatable("menu.showercore.pool.radius",spec.radiusQuarters()/4.0),96,80);
        center(g,Component.translatable("menu.showercore.pool.planner"),25,16);
        center(g,Component.translatable("menu.showercore.material.block"),25,60);
        int pw=112,ph=66;double scale=Math.min(pw/(double)spec.width(),ph/(double)spec.length());
        double ox=x+116+(pw-spec.width()*scale)/2,oz=y+102+(ph-spec.length()*scale)/2;
        for(int j=0;j<ph;j++) {
            int start=0,previous=0;
            for(int i=0;i<=pw;i++) {
                double px=(x+116+i-ox)/scale,pz=(y+102+j-oz)/scale;
                int color=i==pw||!spec.contains(px,pz,0)?0:spec.contains(px,pz,PoolSpec.RIM_WIDTH)?0xFF527E9E:0xFFF1E8DA;
                if(color!=previous) {
                    if(previous!=0)g.fill(x+116+start,y+102+j,x+116+i,y+103+j,previous);
                    start=i;previous=color;
                }
            }
        }
        center(g,Component.translatable(menu.bound()?"menu.showercore.pool.finish_cost":"menu.showercore.pool.cost",menu.required(),menu.available()),124,173);
    }
    private void center(GuiGraphics g,Component text,int x,int y) {g.drawString(font,text,leftPos+x-font.width(text)/2,topPos+y,0xFF404040,false);}
    @Override public void render(GuiGraphics g,int x,int y,float partialTick) {super.render(g,x,y,partialTick);renderTooltip(g,x,y);}
}
