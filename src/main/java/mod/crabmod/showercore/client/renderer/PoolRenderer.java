package mod.crabmod.showercore.client.renderer;

import com.mojang.blaze3d.vertex.*;
import mod.crabmod.showercore.client.model.DynamicBathtubMaterialModel;
import mod.crabmod.showercore.material.BathtubMaterialFace;
import mod.crabmod.showercore.pool.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Matrix4f;
import java.util.List;

/** Each member draws only its own clipped cell, keeping normal chunk/frustum culling. */
public final class PoolRenderer implements BlockEntityRenderer<PoolBlockEntity> {
    public PoolRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(PoolBlockEntity part,float partialTick,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
        if(!part.configured()||part.cell()==null)return;
        var cell=part.cell();var matrix=poses.last().pose();
        var sprite=DynamicBathtubMaterialModel.resolveMaterialSprite(part.settings().material(),part.settings().face());
        if(sprite==null)sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
        var body=buffers.getBuffer(Sheets.cutoutBlockSheet());
        polygon(body,matrix,cell.floor(),cell,PoolSpec.FLOOR_Y,sprite,1,1,1,1,light,true);
        polygon(body,matrix,cell.floor(),cell,0,sprite,.5f,.5f,.5f,1,light,false);
        for(var top:cell.rim())polygon(body,matrix,top,cell,PoolSpec.RIM_Y,sprite,1,1,1,1,light,true);
        for(var edge:cell.outerWalls())wall(body,matrix,edge,cell,0,PoolSpec.RIM_Y,sprite,light,false);
        for(var edge:cell.innerWalls())wall(body,matrix,edge,cell,PoolSpec.FLOOR_Y,PoolSpec.RIM_Y,sprite,light,true);
        if(part.isController())faucet(body,matrix,part,light);
        var fluid=part.fluid();if(fluid.isEmpty()||cell.water().isEmpty())return;
        var texture=BathFluidAppearance.texture(part,part.getLevel(),part.getBlockPos(),false);
        var waterSprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture==null?MissingTextureAtlasSprite.getLocation():texture);
        int color=BathFluidAppearance.tint(part,part.getLevel(),part.getBlockPos());
        float r=((color>>16)&255)/255f,g=((color>>8)&255)/255f,b=(color&255)/255f,a=((color>>>24)&255)/255f;
        if(fluid.getFluid()==Fluids.LAVA)light=LightTexture.FULL_BRIGHT;
        var water=buffers.getBuffer(Sheets.translucentCullBlockSheet());
        polygon(water,matrix,cell.water(),cell,part.waterY(),waterSprite,r,g,b,a,light,true);
        // Keep backface culling: the opposite winding exposes the surface only from below.
        polygon(water,matrix,cell.water(),cell,part.waterY(),waterSprite,r,g,b,a,light,false);
        if(part.isController()&&part.circulating()) {
            var flowingTexture=BathFluidAppearance.texture(part,part.getLevel(),part.getBlockPos(),true);
            var flowingSprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(flowingTexture);
            var f=part.layout().faucet();double x=f.x()-cell.x(),z=.57-cell.z();
            box(water,matrix,x-.035,part.waterY(),z-.035,x+.035,1.18,z+.035,flowingSprite,r,g,b,a,light);
        }
    }

    private static void polygon(VertexConsumer out,Matrix4f matrix,List<PoolLayout.Point> points,PoolLayout.Cell cell,double y,
                                TextureAtlasSprite sprite,float r,float g,float b,float a,int light,boolean up) {
        if(points.size()<3)return;
        var origin=points.get(0);
        for(int i=1;i<points.size()-1;i++) {
            var second=points.get(up?i+1:i);var third=points.get(up?i:i+1);
            point(out,matrix,origin,cell,y,sprite,r,g,b,a,light,up);
            point(out,matrix,second,cell,y,sprite,r,g,b,a,light,up);
            point(out,matrix,third,cell,y,sprite,r,g,b,a,light,up);
            point(out,matrix,third,cell,y,sprite,r,g,b,a,light,up);
        }
    }
    private static void point(VertexConsumer out,Matrix4f matrix,PoolLayout.Point p,PoolLayout.Cell cell,double y,
                              TextureAtlasSprite sprite,float r,float g,float b,float a,int light,boolean up) {
        float x=(float)(p.x()-cell.x()),z=(float)(p.z()-cell.z());
        vertex(out,matrix,x,(float)y,z,sprite,x,z,r,g,b,a,light,0,up?1:-1,0);
    }
    private static void wall(VertexConsumer out,Matrix4f matrix,PoolLayout.Edge edge,PoolLayout.Cell cell,double bottom,double top,TextureAtlasSprite sprite,int light,boolean inner) {
        var p=edge.a();var q=edge.b();float dx=(float)(q.x()-p.x()),dz=(float)(q.z()-p.z());
        float length=(float)Math.hypot(dx,dz);float nx=dz/length,nz=-dx/length;
        if(inner){nx=-nx;nz=-nz;var swap=p;p=q;q=swap;}
        float shade=.8f-.2f*Math.abs(nx);
        float px=(float)(p.x()-cell.x()),pz=(float)(p.z()-cell.z()),qx=(float)(q.x()-cell.x()),qz=(float)(q.z()-cell.z());
        boolean alongX=Math.abs(dx)>=Math.abs(dz);float pu=alongX?px:pz,qu=alongX?qx:qz;
        vertex(out,matrix,px,(float)bottom,pz,sprite,pu,(float)(1-bottom),shade,shade,shade,1,light,nx,0,nz);
        vertex(out,matrix,px,(float)top,pz,sprite,pu,(float)(1-top),shade,shade,shade,1,light,nx,0,nz);
        vertex(out,matrix,qx,(float)top,qz,sprite,qu,(float)(1-top),shade,shade,shade,1,light,nx,0,nz);
        vertex(out,matrix,qx,(float)bottom,qz,sprite,qu,(float)(1-bottom),shade,shade,shade,1,light,nx,0,nz);
    }
    private static void vertex(VertexConsumer out,Matrix4f m,float x,float y,float z,TextureAtlasSprite sprite,float u,float v,
                               float r,float g,float b,float a,int light,float nx,float ny,float nz) {
        float tx=m.m00()*x+m.m10()*y+m.m20()*z+m.m30(),ty=m.m01()*x+m.m11()*y+m.m21()*z+m.m31(),tz=m.m02()*x+m.m12()*y+m.m22()*z+m.m32();
        out.vertex(tx,ty,tz).color(r,g,b,a).uv(sprite.getU(Math.max(0,Math.min(1,u))*16),sprite.getV(Math.max(0,Math.min(1,v))*16))
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx,ny,nz).endVertex();
    }
    private static void faucet(VertexConsumer out,Matrix4f matrix,PoolBlockEntity part,int light) {
        var sprite=DynamicBathtubMaterialModel.resolveMaterialSprite(new ResourceLocation("minecraft:gold_block"),BathtubMaterialFace.SIDE);
        double x=part.layout().faucet().x()-part.cell().x(),z=part.layout().faucet().z();
        box(out,matrix,x-.12,.9,z-.07,x+.12,1.02,z+.07,sprite,1,1,1,1,light);
        box(out,matrix,x-.055,1.02,z-.055,x+.055,1.35,z+.055,sprite,1,1,1,1,light);
        box(out,matrix,x-.055,1.25,z+.055,x+.055,1.35,.63,sprite,1,1,1,1,light);
        box(out,matrix,x-.055,1.18,.52,x+.055,1.25,.63,sprite,1,1,1,1,light);
    }
    private static void box(VertexConsumer out,Matrix4f matrix,double x1,double y1,double z1,double x2,double y2,double z2,TextureAtlasSprite sprite,
                            float r,float g,float b,float a,int light) {
        float[][] vertices={{(float)x1,(float)y1,(float)z1},{(float)x2,(float)y1,(float)z1},{(float)x2,(float)y2,(float)z1},{(float)x1,(float)y2,(float)z1},
                {(float)x1,(float)y1,(float)z2},{(float)x2,(float)y1,(float)z2},{(float)x2,(float)y2,(float)z2},{(float)x1,(float)y2,(float)z2}};
        int[][] faces={{0,3,2,1,0,0,-1},{4,5,6,7,0,0,1},{0,4,7,3,-1,0,0},{1,2,6,5,1,0,0},{3,7,6,2,0,1,0},{0,1,5,4,0,-1,0}};
        for(var face:faces)for(int i=0;i<4;i++) {
            var p=vertices[face[i]];
            vertex(out,matrix,p[0],p[1],p[2],sprite,i==0||i==1?0:1,i==0||i==3?1:0,r,g,b,a,light,face[4],face[5],face[6]);
        }
    }
}
