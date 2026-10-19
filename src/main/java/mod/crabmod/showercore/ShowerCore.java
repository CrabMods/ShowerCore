package mod.crabmod.showercore;

import com.mojang.logging.LogUtils;
import mod.crabmod.showercore.client.event.CreativeFilterScreenEvents;
import mod.crabmod.showercore.effect.ModEffects;
import mod.crabmod.showercore.event.ClientEvent;
import mod.crabmod.showercore.registers.BlockEntitiesRegister;
import mod.crabmod.showercore.registers.BlocksRegister;
import mod.crabmod.showercore.registers.EntityRegister;
import mod.crabmod.showercore.registers.ItemRegister;
import mod.crabmod.showercore.registers.ParticleRegister;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

import mod.crabmod.showercore.registers.SoundRegister;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(ShowerCore.MODID)
public class ShowerCore {

  public static final String MODID = "showercore";
  private static final Logger LOGGER = LogUtils.getLogger();

  public ShowerCore(IEventBus modEventBus, ModContainer modContainer) {
    mod.crabmod.showercore.registers.MenuRegister.register(modEventBus);
    ItemRegister.register(modEventBus);
    BlocksRegister.register(modEventBus);

    ModEffects.register(modEventBus);

    BlockEntitiesRegister.register(modEventBus);
    EntityRegister.register(modEventBus);
    ParticleRegister.register(modEventBus);
    SoundRegister.register(modEventBus);

    modEventBus.addListener(this::commonSetup);
    modEventBus.addListener(this::registerCapabilities);

    NeoForge.EVENT_BUS.register(this);
    modEventBus.addListener(this::addCreative);

    modContainer.registerConfig(ModConfig.Type.COMMON, mod.crabmod.showercore.Config.SPEC);
    modContainer.registerConfig(ModConfig.Type.CLIENT, mod.crabmod.showercore.ClientConfig.SPEC);
  }

  private void registerCapabilities(RegisterCapabilitiesEvent event) {
    event.registerBlockEntity(
        net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
        BlockEntitiesRegister.BATHTUB_BLOCK_ENTITY.get(),
        (be, side) -> be.getFluidTank());
    event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
        BlockEntitiesRegister.BATHING_POOL.get(), (be, side) -> be.fluidHandler());
  }

  private void commonSetup(final FMLCommonSetupEvent event) {
    LOGGER.info("ShowerCore common setup complete.");
    // Temperature mod compat (Cold Sweat, TAN, LSO) is handled automatically
    // via CustomFluidHandlerMixin intercepting hotBath's API methods.
    // No separate registration needed - hotBath's own handlers detect ShowerCore players.
  }

  private void addCreative(BuildCreativeModeTabContentsEvent event) {
    try {
      if (event.getTabKey() == com.crabmod.hotbath.item.ItemGroup.HOT_BATH_TAB.getKey()) {
        addShowerCoreItems(event);
      }
    } catch (NoClassDefFoundError | NoSuchFieldError | Exception e) {
      // hotBath mod's ItemGroup is not available - fall back to adding items
      // to the building blocks tab so they are still accessible in creative mode
      LOGGER.warn("Could not access hotBath ItemGroup (hotBath mod may not be installed). " +
          "ShowerCore items will be added to the Building Blocks tab instead. Error: {}", e.getMessage());
      if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS) {
        addShowerCoreItems(event);
      }
    }
  }

  /**
   * Add all ShowerCore items to the given creative mode tab event.
   */
  private void addShowerCoreItems(BuildCreativeModeTabContentsEvent event) {
      event.accept(BlocksRegister.HOT_WATER_CORE.get());
      event.accept(BlocksRegister.HERBAL_BATH_CORE.get());
      event.accept(BlocksRegister.PEONY_BATH_CORE.get());
      event.accept(BlocksRegister.ROSE_BATH_CORE.get());
      event.accept(BlocksRegister.MILK_BATH_CORE.get());
      event.accept(BlocksRegister.HONEY_BATH_CORE.get());
      event.accept(BlocksRegister.RAIN_SHOWER_HEAD_WHITE.get());
      event.accept(BlocksRegister.COMPACT_SHOWER_HEAD_WHITE.get());
      event.accept(BlocksRegister.BATHTUB_WHITE.get());
      event.accept(BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get());
      event.accept(BlocksRegister.BATHTUB_OFURO_BIRCH.get());
      event.accept(BlocksRegister.BATHTUB_ROMAN_WHITE.get());
      event.accept(BlocksRegister.BATHTUB_SEASHELL_PINK.get());
      event.accept(BlocksRegister.BATHTUB_WASHTUB_IRON.get());
      event.accept(BlocksRegister.BATHTUB_SPA_WHITE.get());
      event.accept(BlocksRegister.BAMBOO_SHOWER_HEAD_BAMBOO.get());
      event.accept(BlocksRegister.COLUMN_SHOWER_HEAD_WHITE.get());
      event.accept(BlocksRegister.CORAL_SHOWER_HEAD_PINK.get());
      event.accept(BlocksRegister.BUCKET_SHOWER_HEAD_SPRUCE.get());
      event.accept(BlocksRegister.PANEL_SHOWER_HEAD_WHITE.get());
      event.accept(ItemRegister.RUBBER_DUCK.get());
      event.accept(ItemRegister.RUBBER_DUCK_DELUXE.get());
      event.accept(ItemRegister.BATHING_POOL.get());
  }

  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {}

  @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
  public static class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
      NeoForge.EVENT_BUS.register(new CreativeFilterScreenEvents());
    }
  }
}
