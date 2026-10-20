package mod.crabmod.showercore.mixin;

import com.crabmod.hotbath.util.CustomFluidHandler;
import mod.crabmod.showercore.utils.CoreUtils;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CustomFluidHandler.class)
public class CustomFluidHandlerMixin {

    /**
     * Intercept isPlayerInHotBathBlock to include ShowerCore bathtubs and shower heads.
     * Used by many compat handlers (Farmer's Delight, Twilight Forest, TAN, LSO, etc.)
     */
    @Inject(method = "isPlayerInHotBathBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void isPlayerInHotBathBlock(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CoreUtils.isPlayerInShowerCoreBath(player)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Intercept isPlayerInHotBath (temperature-aware version) to include ShowerCore.
     * Used by Cold Sweat, TAN, LSO, Farmer's Delight, Serene Seasons, Twilight Forest (1.21).
     */
    @Inject(method = "isPlayerInHotBath", at = @At("HEAD"), cancellable = true, remap = false)
    private static void isPlayerInHotBath(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CoreUtils.isPlayerInShowerCoreHotWater(player)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Intercept getBathTemperature to return the actual bath temperature for ShowerCore.
     * For custom fluids, returns the CustomFluidDefinition's temperature.
     * For built-in hot liquids and shower heads, returns the default 40.0f.
     */
    @Inject(method = "getBathTemperature", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getBathTemperature(Player player, CallbackInfoReturnable<Float> cir) {
        if (CoreUtils.isPlayerInShowerCoreBath(player) || CoreUtils.isPlayerInShowerCoreHotWater(player)) {
            cir.setReturnValue(CoreUtils.getShowerCoreBathTemperature(player));
        }
    }
}
