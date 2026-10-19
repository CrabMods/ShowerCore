package mod.crabmod.showercore.pool;

import mod.crabmod.showercore.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.function.Predicate;

public final class PoolContact {
    private PoolContact() {}
    @Nullable public static PoolBlockEntity at(Level level,BlockPos pos) {
        return level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof PoolBlockEntity part&&part.configured()&&part.active()?part:null;
    }
    public static boolean bathing(Entity entity,Predicate<PoolBlockEntity> fluid) {
        return find(entity,fluid)!=null;
    }
    @Nullable public static PoolBlockEntity find(Entity entity,Predicate<PoolBlockEntity> fluid) {
        if(entity.getVehicle() instanceof SeatEntity seat&&seat.poolOwner()!=null) {
            var part=at(entity.level(),seat.poolOwner());var owner=part==null?null:part.controller();
            return owner!=null&&fluid.test(owner)&&owner.touchesWater(entity.getBoundingBox())?owner:null;
        }
        var center=entity.blockPosition();
        for(int y=0;y<=1;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
            var part=at(entity.level(),center.offset(x,y,z));
            var owner=part==null?null:part.controller();
            if(owner!=null&&fluid.test(owner)&&owner.touchesWater(entity.getBoundingBox()))return owner;
        }
        return null;
    }

    @Nullable public static PoolBlockEntity atPoint(Level level,net.minecraft.world.phys.Vec3 point) {
        var part=at(level,BlockPos.containing(point));var owner=part==null?null:part.controller();
        if(owner==null||owner.fluid().isEmpty()||point.y<=owner.origin().getY()+PoolSpec.FLOOR_Y||point.y>=owner.origin().getY()+owner.waterY())return null;
        return owner.layout().containsWater(point.x-owner.origin().getX(),point.z-owner.origin().getZ())?owner:null;
    }
}
