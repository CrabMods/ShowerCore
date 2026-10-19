package mod.crabmod.showercore.client.event;

import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.item.BathingPoolItem;
import mod.crabmod.showercore.pool.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

@EventBusSubscriber(modid=ShowerCore.MODID,value=Dist.CLIENT)
public final class PoolPreview {
    private static long checkedAt=-1;
    private static BlockPos checkedOrigin;
    private static PoolSettings checkedSettings;
    private static boolean valid;
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        var held=mc.player.getMainHandItem();if(!(held.getItem() instanceof BathingPoolItem))held=mc.player.getOffhandItem();
        if(!(held.getItem() instanceof BathingPoolItem))return;
        var data=PoolSettings.data(held);var settings=PoolSettings.read(held);
        if(settings==null||!data.contains("PoolReady")||!data.getString("PoolDimension").equals(mc.level.dimension().location().toString()))return;
        var origin=BlockPos.of(data.getLong("PoolReady"));
        long now=mc.level.getGameTime();
        if(!origin.equals(checkedOrigin)||!settings.equals(checkedSettings)||now<checkedAt||now-checkedAt>=5) {
            valid=PoolStructure.placementProblem(mc.level,origin,settings,mc.player).isEmpty();
            checkedAt=now;checkedOrigin=origin;checkedSettings=settings;
        }
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();
        pose.pushPose();pose.translate(origin.getX()-camera.x,origin.getY()-camera.y,origin.getZ()-camera.z);
        var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(RenderType.lines());var matrix=pose.last().pose();
        var points=PoolLayout.of(settings.spec()).outline();float r=valid?.25f:1,g=valid?1:.25f;
        for(int i=0;i<points.size();i++) {
            var a=points.get(i);var b=points.get((i+1)%points.size());
            line(out,matrix,a.x(),PoolSpec.RIM_Y,a.z(),b.x(),PoolSpec.RIM_Y,b.z(),r,g);
            line(out,matrix,a.x(),PoolSpec.FLOOR_Y,a.z(),b.x(),PoolSpec.FLOOR_Y,b.z(),r,g);
            if(i%4==0)line(out,matrix,a.x(),PoolSpec.FLOOR_Y,a.z(),a.x(),PoolSpec.RIM_Y,a.z(),r,g);
        }
        buffers.endBatch(RenderType.lines());pose.popPose();
    }
    private static void line(VertexConsumer out,Matrix4f matrix,double x1,double y1,double z1,double x2,double y2,double z2,float r,float g) {
        float dx=(float)(x2-x1),dy=(float)(y2-y1),dz=(float)(z2-z1);float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(length<1e-7)return;
        out.addVertex(matrix,(float)x1,(float)y1,(float)z1).setColor(r,g,.35f,1).setNormal(dx/length,dy/length,dz/length);
        out.addVertex(matrix,(float)x2,(float)y2,(float)z2).setColor(r,g,.35f,1).setNormal(dx/length,dy/length,dz/length);
    }
}
