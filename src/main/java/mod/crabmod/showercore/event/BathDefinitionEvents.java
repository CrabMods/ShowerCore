package mod.crabmod.showercore.event;

import com.crabmod.hotbath.api.BathDefinitionsChangedEvent;
import mod.crabmod.showercore.pool.BathFluidSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** Loaded-container index, owned by ShowerCore and used only on definition changes. */
@EventBusSubscriber(modid="showercore")
public final class BathDefinitionEvents {
    private static final Set<BlockEntity> LOADED=Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private BathDefinitionEvents() {}
    public static void loaded(BlockEntity entity) {LOADED.add(entity);}
    public static void removed(BlockEntity entity) {LOADED.remove(entity);}
    @SubscribeEvent public static void changed(BathDefinitionsChangedEvent event) {
        final List<BlockEntity> snapshot;
        synchronized(LOADED) {snapshot=List.copyOf(LOADED);}
        for(var entity:snapshot) {
            var level=entity.getLevel();
            if(level==null||entity.isRemoved()||level.isClientSide()!=event.clientSide())continue;
            if(!event.clientSide()&&level.getServer()!=event.server())continue;
            var source=(BathFluidSource)entity;
            if(!event.clientSide())source.refreshBathDefinition();
            else {
                level.getLightEngine().checkBlock(entity.getBlockPos());
                var state=entity.getBlockState();level.sendBlockUpdated(entity.getBlockPos(),state,state,3);
            }
        }
    }
}
