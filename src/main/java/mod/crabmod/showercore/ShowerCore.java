package mod.crabmod.showercore;

import com.crabmod.hotbath.item.ItemGroup;
import com.mojang.logging.LogUtils;
import mod.crabmod.showercore.client.event.CreativeFilterScreenEvents;
import mod.crabmod.showercore.effect.ModEffects;
import mod.crabmod.showercore.event.ClientEvent;
import mod.crabmod.showercore.registers.BlockEntitiesRegister;
import mod.crabmod.showercore.registers.BlocksRegister;
import mod.crabmod.showercore.registers.EntityRegister;
import mod.crabmod.showercore.registers.ItemRegister;
import mod.crabmod.showercore.registers.ParticleRegister;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import mod.crabmod.showercore.registers.SoundRegister;

@Mod(ShowerCore.MODID)
public class ShowerCore {

  public static final String MODID = "showercore";
  private static final Logger LOGGER = LogUtils.getLogger();

  public ShowerCore() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    mod.crabmod.showercore.registers.MenuRegister.register(modEventBus);
    ItemRegister.register(modEventBus);
    BlocksRegister.register(modEventBus);

    ModEffects.register(modEventBus);

    BlockEntitiesRegister.register(modEventBus);
    EntityRegister.register(modEventBus);
    ParticleRegister.register(modEventBus);
    SoundRegister.register(modEventBus);

    modEventBus.addListener(this::commonSetup);

    MinecraftForge.EVENT_BUS.register(this);
    MinecraftForge.EVENT_BUS.register(mod.crabmod.showercore.event.ServerEvent.class);
    modEventBus.addListener(this::addCreative);

    ModLoadingContext.get()
        .registerConfig(ModConfig.Type.COMMON, mod.crabmod.showercore.Config.SPEC);
    ModLoadingContext.get()
        .registerConfig(ModConfig.Type.CLIENT, mod.crabmod.showercore.ClientConfig.SPEC);
  }

  private void commonSetup(final FMLCommonSetupEvent event) {
    LOGGER.info("ShowerCore common setup complete.");
    // Hot Bath owns optional-mod effects; measured bath and shower contacts use its public API.
  }

  private void addCreative(BuildCreativeModeTabContentsEvent event) {
    try {
      if (event.getTabKey() != ItemGroup.HOT_BATH_TAB.getKey()) {
        return;
      }
    } catch (Throwable e) {
      LOGGER.error("Failed to access Hot Bath creative tab - Hot Bath mod may not be installed: {}", e.getMessage());
      return;
    }

    try {
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
    } catch (Throwable e) {
      LOGGER.error("Error adding ShowerCore items to creative tab: {}", e.getMessage());
    }
  }

  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {}

  @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
  public static class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
      MinecraftForge.EVENT_BUS.register(new CreativeFilterScreenEvents());
      MinecraftForge.EVENT_BUS.addListener(ClientEvent::registerParticleFactories);
    }
  }
}
