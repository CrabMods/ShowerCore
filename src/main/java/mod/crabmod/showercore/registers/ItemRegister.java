package mod.crabmod.showercore.registers;

import mod.crabmod.showercore.ShowerCore;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraftforge.registries.RegistryObject;
import mod.crabmod.showercore.item.RubberDuckItem;

public class ItemRegister {
  public static final DeferredRegister<Item> ITEMS =
          DeferredRegister.create(ForgeRegistries.ITEMS, ShowerCore.MODID);

  public static final RegistryObject<Item> RUBBER_DUCK = ITEMS.register("rubber_duck", () -> new RubberDuckItem(new Item.Properties()));

  public static final RegistryObject<Item> RUBBER_DUCK_DELUXE = ITEMS.register("rubber_duck_deluxe", () -> new RubberDuckItem(new Item.Properties()));

  public static final RegistryObject<Item> BATHING_POOL = ITEMS.register("bathing_pool",
      () -> new mod.crabmod.showercore.item.BathingPoolItem(new Item.Properties().stacksTo(1)));

  public static void register(IEventBus eventBus) {
    ITEMS.register(eventBus);
  }
}