package mod.crabmod.showercore.event;

import com.crabmod.hotbath.api.HotBathApi;
import mod.crabmod.showercore.utils.CoreUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** ShowerCore measures contact; Hot Bath's existing consumers receive a passive snapshot. */
@EventBusSubscriber(modid="showercore")
public final class BathContactEvents {
    private BathContactEvents() {}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void tick(PlayerTickEvent.Post event) {
        var player=event.getEntity();var source=CoreUtils.bathAt(player);
        if(source!=null)HotBathApi.reportFluidContact(player,source.fluid(),source.bathSurfaceY(),mod.crabmod.showercore.Config.isModIntegrationsEnabled());
    }
}
