package mod.crabmod.showercore.registers;

import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.menu.BathtubMaterialMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class MenuRegister {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ShowerCore.MODID);
    public static final RegistryObject<MenuType<BathtubMaterialMenu>> BATHTUB_MATERIAL =
            MENUS.register("bathtub_material", () -> new MenuType<>(BathtubMaterialMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<mod.crabmod.showercore.menu.PoolConfigurationMenu>> POOL_CONFIGURATION =
            MENUS.register("pool_configuration", () -> new MenuType<>(mod.crabmod.showercore.menu.PoolConfigurationMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
