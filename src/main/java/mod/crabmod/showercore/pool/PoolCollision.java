package mod.crabmod.showercore.pool;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.*;

final class PoolCollision {
    private static final Map<PoolSpec, Map<Integer,VoxelShape>> CACHE = new LinkedHashMap<>(16,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<PoolSpec,Map<Integer,VoxelShape>> entry) { return size()>32; }
    };
    static synchronized VoxelShape shape(PoolSpec spec, int x, int z) {
        return CACHE.computeIfAbsent(spec,k->new HashMap<>()).computeIfAbsent(z*spec.width()+x,k->build(spec,x,z));
    }
    private static VoxelShape build(PoolSpec spec,int x,int z) {
        int[][] mask=new int[16][16];
        for(int i=0;i<16;i++)for(int j=0;j<16;j++) {
            double px=x+(i+.5)/16, pz=z+(j+.5)/16;
            if(spec.contains(px,pz,0))mask[i][j]=spec.contains(px,pz,PoolSpec.RIM_WIDTH)?1:2;
        }
        VoxelShape result=Shapes.empty();
        for(int j=0;j<16;j++)for(int i=0;i<16;i++) {
            int value=mask[i][j];if(value==0)continue;
            int right=i+1,bottom=j+1;
            while(right<16&&mask[right][j]==value)right++;
            outer:while(bottom<16) {
                for(int n=i;n<right;n++)if(mask[n][bottom]!=value)break outer;
                bottom++;
            }
            result=Shapes.or(result,Shapes.box(i/16.0,0,j/16.0,right/16.0,value==1?PoolSpec.FLOOR_Y:PoolSpec.RIM_Y,bottom/16.0));
            for(int a=i;a<right;a++)for(int b=j;b<bottom;b++)mask[a][b]=0;
        }
        return result.optimize();
    }
}
