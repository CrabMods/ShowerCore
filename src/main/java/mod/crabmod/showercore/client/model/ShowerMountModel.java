package mod.crabmod.showercore.client.model;

import mod.crabmod.showercore.block.ShowerHeadBlock;
import mod.crabmod.showercore.entity.ShowerHeadContainerEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.*;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** The same installed transform drives chunk meshes, targeting, particles and effects. */
public final class ShowerMountModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<ShowerHeadContainerEntity.HorizontalMount> HORIZONTAL=new ModelProperty<>();
    private record Shift(BakedQuad quad,int pixels,float x,float z) {}
    private final Map<Shift,BakedQuad> translated=Collections.synchronizedMap(new LinkedHashMap<>(128,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Shift,BakedQuad> entry){return size()>4096;}
    });
    public ShowerMountModel(BakedModel original){super(original);}
    @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data) {
        var result=super.getModelData(level,pos,state,data);
        return level.getBlockEntity(pos) instanceof ShowerHeadContainerEntity shower?result.derive().with(HORIZONTAL,shower.horizontalMount()).build():result;
    }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state,@Nullable Direction side,RandomSource random) {
        return shift(super.getQuads(state,side,random),state,null);
    }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state,@Nullable Direction side,RandomSource random,ModelData data,@Nullable RenderType layer) {
        return shift(super.getQuads(state,side,random,data,layer),state,data.get(HORIZONTAL));
    }
    private List<BakedQuad> shift(List<BakedQuad> quads,@Nullable BlockState state,@Nullable ShowerHeadContainerEntity.HorizontalMount horizontal) {
        if(state==null||!(state.getBlock() instanceof ShowerHeadBlock))return quads;
        int pixels=ShowerHeadBlock.mountOffsetPixels(state);float x=horizontal==null?0:horizontal.x(),z=horizontal==null?0:horizontal.z();
        if(pixels==0&&x==0&&z==0)return quads;
        return quads.stream().map(quad->translated.computeIfAbsent(new Shift(quad,pixels,x,z),key->{
            int[] vertices=quad.getVertices().clone();
            for(int vertex=0;vertex<4;vertex++)for(int axis=0;axis<3;axis++) {
                float delta=axis==0?x:axis==1?pixels/16f:z;int index=vertex*8+axis;
                vertices[index]=Float.floatToRawIntBits(Float.intBitsToFloat(vertices[index])+delta);
            }
            return new BakedQuad(vertices,quad.getTintIndex(),quad.getDirection(),quad.getSprite(),quad.isShade());
        })).toList();
    }
}
