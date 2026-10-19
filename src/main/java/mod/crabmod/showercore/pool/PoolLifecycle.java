package mod.crabmod.showercore.pool;

import mod.crabmod.showercore.ShowerCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid=ShowerCore.MODID)
public final class PoolLifecycle {
    private PoolLifecycle() {}
    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var entered=event.getChunk().getPos();
        // A bounded pool can overlap only the controller's immediate neighbor chunks.
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            LevelChunk chunk=level.getChunkSource().getChunkNow(entered.x+dx,entered.z+dz);
            if(chunk==null)continue;
            for(var be:chunk.getBlockEntities().values())if(be instanceof PoolBlockEntity part&&part.configured()) {
                var owner=part.ownerPos();
                if((owner.getX()>>4)==entered.x&&(owner.getZ()>>4)==entered.z)level.scheduleTick(part.getBlockPos(),part.getBlockState().getBlock(),1);
            }
        }
    }
}
