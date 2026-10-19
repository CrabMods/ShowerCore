package mod.crabmod.showercore.event;

import mod.crabmod.showercore.entity.ShowerHeadContainerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Core server-side event handler for ShowerCore.
 * Handles cleanup of player tracking data on logout/death to prevent memory leaks.
 */
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
