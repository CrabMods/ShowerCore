package mod.crabmod.showercore.mixin;

import com.crabmod.hotbath.util.CustomFluidHandler;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.utils.CoreUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CustomFluidHandler.class)
public class CustomFluidHandlerMixin {

    /**
     * Intercept isPlayerInHotBathBlock to include ShowerCore bathtubs and shower heads.
     * Used by Farmer's Delight, Twilight Forest, TAN, LSO, etc.
     */
    @Inject(method = "isPlayerInHotBathBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void isPlayerInHotBathBlock(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CoreUtils.isPlayerInShowerCoreBath(player)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Intercept isPlayerInHotBath (temperature-aware version) to include ShowerCore.
     * Used by Cold Sweat's HotBathImmersionModifier.
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

    @Inject(method = "isPlayerInHotWaterBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void isPlayerInHotWaterBlock(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CoreUtils.isEntityInBathtub(player, BathtubBlock.LiquidType.HOT_WATER)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isPlayerInHerbalBathBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void isPlayerInHerbalBathBlock(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CoreUtils.isEntityInBathtub(player, BathtubBlock.LiquidType.HERBAL_BATH)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isPlayerInHoneyBathBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void isPlayerInHoneyBathBlock(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CoreUtils.isEntityInBathtub(player, BathtubBlock.LiquidType.HONEY_BATH)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isPlayerInPeonyBathBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void isPlayerInPeonyBathBlock(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CoreUtils.isEntityInBathtub(player, BathtubBlock.LiquidType.PEONY_BATH)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isPlayerInMilkBathBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void isPlayerInMilkBathBlock(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CoreUtils.isEntityInBathtub(player, BathtubBlock.LiquidType.MILK_BATH)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isPlayerInRoseBathBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void isPlayerInRoseBathBlock(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CoreUtils.isEntityInBathtub(player, BathtubBlock.LiquidType.ROSE_BATH)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isEntityInHerbalBathBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void isEntityInHerbalBathBlock(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (CoreUtils.isEntityInBathtub(entity, BathtubBlock.LiquidType.HERBAL_BATH)) {
            cir.setReturnValue(true);
        }
    }
}
