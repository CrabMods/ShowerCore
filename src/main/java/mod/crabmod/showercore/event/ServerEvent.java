package mod.crabmod.showercore.event;

import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.entity.ShowerHeadContainerEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Core server-side event handler for ShowerCore.
 * Handles cleanup of player tracking data on logout/death to prevent memory leaks.
 */
@EventBusSubscriber(modid = ShowerCore.MODID)
public class ServerEvent {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ShowerHeadContainerEntity.cleanupPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            ShowerHeadContainerEntity.cleanupPlayer(event.getEntity().getUUID());
        }
    }

}
