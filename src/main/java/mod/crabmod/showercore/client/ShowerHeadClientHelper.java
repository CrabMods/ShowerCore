package mod.crabmod.showercore.client;

import mod.crabmod.showercore.utils.BathEffectUtils;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import java.util.function.Supplier;

public class ShowerHeadClientHelper {
    public static Object createBathEffectUtils() {
        return new BathEffectUtils();
    }

    public static void stopBathEffect(Object utils) {
        if (utils instanceof BathEffectUtils) {
            ((BathEffectUtils) utils).stopBathEffect();
        }
    }

    public static void shutdown(Object utils) {
        if (utils instanceof BathEffectUtils) {
            ((BathEffectUtils) utils).shutdown();
        }
    }

    public static void renderBathWater(Object utils, Level level, Vec3 origin, Supplier<ParticleOptions> particleTypeSupplier, double width, double depth) {
        if (utils instanceof BathEffectUtils) {
            ((BathEffectUtils) utils).renderBathWater(level, origin, particleTypeSupplier, width, depth);
        }
    }
}
