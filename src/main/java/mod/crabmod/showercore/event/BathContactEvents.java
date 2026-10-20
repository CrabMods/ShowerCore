package mod.crabmod.showercore.event;

import com.crabmod.hotbath.api.HotBathApi;
import mod.crabmod.showercore.utils.CoreUtils;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

/** ShowerCore measures contact; Hot Bath's existing consumers receive a passive snapshot. */
@Mod.EventBusSubscriber(modid="showercore")
public final class BathContactEvents {
    private BathContactEvents() {}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var player=event.player;var source=CoreUtils.bathAt(player);
        if(source!=null)HotBathApi.reportFluidContact(player,source.fluid(),source.bathSurfaceY(),mod.crabmod.showercore.Config.isModIntegrationsEnabled());
    }
}
