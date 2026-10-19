package mod.crabmod.showercore.pool;

import mod.crabmod.showercore.entity.SeatEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import java.util.Comparator;

public final class PoolSeats {
    private PoolSeats() {}
    public static boolean sit(PoolBlockEntity pool,Player player,Vec3 hit) {
        if(player.isPassenger()||!pool.active())return false;
        double x=hit.x-pool.origin().getX(),z=hit.z-pool.origin().getZ();
        var kind=pool.layout().spec().contains(x,z,PoolSpec.RIM_WIDTH)?PoolLayout.SeatKind.INSIDE:PoolLayout.SeatKind.RIM;
        var seat=pool.layout().seats().stream().filter(s->s.kind()==kind)
                .min(Comparator.comparingDouble(s->Math.hypot(s.x()-x,s.z()-z))).orElse(null);
        if(seat==null||Math.hypot(seat.x()-x,seat.z()-z)>1.25)return false;
        var level=pool.getLevel();
        var area=new AABB(pool.origin()).expandTowards(pool.settings().spec().width(),3,pool.settings().spec().length());
        if(level.getEntitiesOfClass(SeatEntity.class,area).stream().anyMatch(e->pool.poolId().equals(e.poolId())&&e.poolSlot()==seat.index()&&!e.getPassengers().isEmpty())) {
            player.displayClientMessage(Component.translatable("message.showercore.pool.seat_taken"),true);return false;
        }
        double wx=pool.origin().getX()+seat.x(),wy=pool.origin().getY()+seat.y(),wz=pool.origin().getZ()+seat.z();
        if(player.distanceToSqr(wx,wy,wz)>36||!level.noCollision(player,new AABB(wx-.3,wy+.35,wz-.3,wx+.3,wy+1.4,wz+.3)))return false;
        var entity=new SeatEntity(level,wx,wy,wz);entity.bindPool(pool.ownerPos(),pool.poolId(),seat.index());entity.setYRot(seat.yaw());entity.setPoolSeatKind(seat.kind());
        level.addFreshEntity(entity);
        if(!player.startRiding(entity)) {entity.discard();return false;}
        player.setYRot(seat.yaw());return true;
    }
    public static boolean updateSeat(SeatEntity seat) {
        var level=seat.level();var owner=seat.poolOwner();
        if(owner==null||!level.hasChunkAt(owner))return false;
        if(!(level.getBlockEntity(owner) instanceof PoolBlockEntity pool)||!pool.isController()||!pool.active()
                ||!pool.poolId().equals(seat.poolId())||seat.poolSlot()<0||seat.poolSlot()>=pool.layout().seats().size())return false;
        var spot=pool.layout().seats().get(seat.poolSlot());
        seat.setPoolSeatKind(spot.kind());
        double x=pool.origin().getX()+spot.x(),y=pool.origin().getY()+spot.y(),z=pool.origin().getZ()+spot.z();
        // Loaded markers use the current physical surface, including seats saved by older versions.
        if(seat.getX()!=x||seat.getY()!=y||seat.getZ()!=z)seat.setPos(x,y,z);
        return true;
    }
}
