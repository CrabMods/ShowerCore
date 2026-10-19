package mod.crabmod.showercore.event;

import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.block.bath_core.herbal_bath_core.HerbalBathCoreBlockEntityRenderer;
import mod.crabmod.showercore.block.bath_core.herbal_bath_core.HerbalBathCoreModelLayers;
import mod.crabmod.showercore.block.bath_core.honey_bath_core.HoneyBathCoreBlockEntityRenderer;
import mod.crabmod.showercore.block.bath_core.honey_bath_core.HoneyBathCoreModelLayers;
import mod.crabmod.showercore.block.bath_core.hot_water_core.HotWaterCoreBlockEntityRenderer;
import mod.crabmod.showercore.block.bath_core.hot_water_core.HotWaterCoreModelLayers;
import mod.crabmod.showercore.block.bath_core.milk_bath_core.MilkBathCoreBlockEntityRenderer;
import mod.crabmod.showercore.block.bath_core.milk_bath_core.MilkBathCoreModelLayers;
import mod.crabmod.showercore.block.bath_core.peony_bath_core.PeonyBathCoreBlockEntityRenderer;
import mod.crabmod.showercore.block.bath_core.peony_bath_core.PeonyBathCoreModelLayers;
import mod.crabmod.showercore.block.bath_core.rose_bath_core.RoseBathCoreBlockEntityRenderer;
import mod.crabmod.showercore.block.bath_core.rose_bath_core.RoseBathCoreModelLayers;
import mod.crabmod.showercore.particle.ShowerParticle;
import mod.crabmod.showercore.client.model.DynamicBathtubMaterialModel;
import mod.crabmod.showercore.registers.BlockEntitiesRegister;
import mod.crabmod.showercore.client.renderer.SeatEntityRenderer;
import mod.crabmod.showercore.client.renderer.BathtubBlockEntityRenderer;
import mod.crabmod.showercore.entity.FaucetInteractionEntity;
import mod.crabmod.showercore.registers.EntityRegister;
import mod.crabmod.showercore.registers.ParticleRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EnchantmentTableParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.fluid_details.FluidsColor;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.registers.BlocksRegister;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mod.EventBusSubscriber(
    modid = ShowerCore.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public class ClientEvent {

  @SubscribeEvent
  public static void registerScreens(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
    event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(
        mod.crabmod.showercore.registers.MenuRegister.BATHTUB_MATERIAL.get(),
        mod.crabmod.showercore.client.screen.BathtubMaterialScreen::new));
    event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(
        mod.crabmod.showercore.registers.MenuRegister.POOL_CONFIGURATION.get(),
        mod.crabmod.showercore.client.screen.PoolConfigurationScreen::new));
  }

  @SubscribeEvent
  public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
    DynamicBathtubMaterialModel.clearCache();
    event.getModels().replaceAll((location, model) -> {
        if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation(location.getNamespace(),location.getPath())) instanceof mod.crabmod.showercore.block.ShowerHeadBlock)
            model = new mod.crabmod.showercore.client.model.ShowerMountModel(model);
        return DynamicBathtubMaterialModel.shouldWrap(location) ? new DynamicBathtubMaterialModel(model) : model;
    });
  }

  @SubscribeEvent
  public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
    event.register(
        (state, world, pos, tintIndex) -> {
          if (world != null && pos != null && tintIndex == 0) {
            BathtubBlock.LiquidType liquid = state.getValue(BathtubBlock.LIQUID);
            switch (liquid) {
              case WATER:
                return BiomeColors.getAverageWaterColor(world, pos);
              case HOT_WATER:
                return FluidsColor.HOT_WATER_COLOR;
              case HERBAL_BATH:
                return FluidsColor.HERBAL_BATH_COLOR;
              case HONEY_BATH:
                return FluidsColor.HONEY_BATH_COLOR;
              case MILK_BATH:
                return FluidsColor.MILK_BATH_COLOR;
              case PEONY_BATH:
                return FluidsColor.PEONY_BATH_COLOR;
              case ROSE_BATH:
                return FluidsColor.ROSE_BATH_COLOR;
              case CUSTOM:
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof BathtubBlockEntity bathtubBe) {
                    ResourceLocation fluidId = bathtubBe.getCustomFluidId();
                    if (fluidId != null) {
                        int color = CustomFluidAPI.getFluidColor(fluidId);
                        if (color != -1) {
                            return color;
                        }
                    }
                }
                return -1;
              default:
                return -1;
            }
          }
          return -1;
        },
        BlocksRegister.BATHTUB_WHITE.get(),
        BlocksRegister.BATHTUB_ORANGE.get(),
        BlocksRegister.BATHTUB_MAGENTA.get(),
        BlocksRegister.BATHTUB_LIGHT_BLUE.get(),
        BlocksRegister.BATHTUB_YELLOW.get(),
        BlocksRegister.BATHTUB_LIME.get(),
        BlocksRegister.BATHTUB_PINK.get(),
        BlocksRegister.BATHTUB_GRAY.get(),
        BlocksRegister.BATHTUB_LIGHT_GRAY.get(),
        BlocksRegister.BATHTUB_CYAN.get(),
        BlocksRegister.BATHTUB_PURPLE.get(),
        BlocksRegister.BATHTUB_BLUE.get(),
        BlocksRegister.BATHTUB_BROWN.get(),
        BlocksRegister.BATHTUB_GREEN.get(),
        BlocksRegister.BATHTUB_RED.get(),
        BlocksRegister.BATHTUB_BLACK.get(),
        BlocksRegister.BATHTUB_OAK.get(),
        BlocksRegister.BATHTUB_SPRUCE.get(),
        BlocksRegister.BATHTUB_BIRCH.get(),
        BlocksRegister.BATHTUB_JUNGLE.get(),
        BlocksRegister.BATHTUB_ACACIA.get(),
        BlocksRegister.BATHTUB_DARK_OAK.get(),
        BlocksRegister.BATHTUB_MANGROVE.get(),
        BlocksRegister.BATHTUB_CHERRY.get(),
        BlocksRegister.BATHTUB_BAMBOO.get(),
        BlocksRegister.BATHTUB_CRIMSON.get(),
        BlocksRegister.BATHTUB_WARPED.get(),
        BlocksRegister.BATHTUB_STONE.get(),
        BlocksRegister.BATHTUB_COBBLESTONE.get(),
        BlocksRegister.BATHTUB_IRON.get(),
        BlocksRegister.BATHTUB_GOLD.get(),
        BlocksRegister.BATHTUB_COPPER.get(),
        BlocksRegister.BATHTUB_DIAMOND.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_ORANGE.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_MAGENTA.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_LIGHT_BLUE.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_YELLOW.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_LIME.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_PINK.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_GRAY.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_LIGHT_GRAY.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_CYAN.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_PURPLE.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_BLUE.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_BROWN.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_GREEN.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_RED.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_BLACK.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_OAK.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_SPRUCE.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_BIRCH.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_JUNGLE.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_ACACIA.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_DARK_OAK.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_MANGROVE.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_CHERRY.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_BAMBOO.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_CRIMSON.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_WARPED.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_STONE.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_COBBLESTONE.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_IRON.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_GOLD.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_COPPER.get(),
        BlocksRegister.BATHTUB_CLAWFOOT_DIAMOND.get(),
        BlocksRegister.BATHTUB_OFURO_BIRCH.get(),
        BlocksRegister.BATHTUB_ROMAN_WHITE.get(),
        BlocksRegister.BATHTUB_SEASHELL_PINK.get(),
        BlocksRegister.BATHTUB_WASHTUB_IRON.get(),
        BlocksRegister.BATHTUB_SPA_WHITE.get());
  }

  @SubscribeEvent
  public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
    // HotWater Core Layers
    event.registerLayerDefinition(
        HotWaterCoreModelLayers.HOT_WATER_CORE_CAGE,
        HotWaterCoreBlockEntityRenderer::createCageLayer);
    event.registerLayerDefinition(
        HotWaterCoreModelLayers.HOT_WATER_CORE_WIND,
        HotWaterCoreBlockEntityRenderer::createWindLayer);
    event.registerLayerDefinition(
        HotWaterCoreModelLayers.HOT_WATER_CORE_EYE,
        HotWaterCoreBlockEntityRenderer::createEyeLayer);
    event.registerLayerDefinition(
        HotWaterCoreModelLayers.HOT_WATER_CORE_SHELL,
        HotWaterCoreBlockEntityRenderer::createShellLayer);

    // Herbal Bath Core Layers
    event.registerLayerDefinition(
        HerbalBathCoreModelLayers.HERBAL_BATH_CORE_CAGE,
        HerbalBathCoreBlockEntityRenderer::createCageLayer);
    event.registerLayerDefinition(
        HerbalBathCoreModelLayers.HERBAL_BATH_CORE_WIND,
        HerbalBathCoreBlockEntityRenderer::createWindLayer);
    event.registerLayerDefinition(
        HerbalBathCoreModelLayers.HERBAL_BATH_CORE_EYE,
        HerbalBathCoreBlockEntityRenderer::createEyeLayer);
    event.registerLayerDefinition(
        HerbalBathCoreModelLayers.HERBAL_BATH_CORE_SHELL,
        HerbalBathCoreBlockEntityRenderer::createShellLayer);

    // Milk Bath Core Layers
    event.registerLayerDefinition(
        MilkBathCoreModelLayers.MILK_BATH_CORE_CAGE,
        MilkBathCoreBlockEntityRenderer::createCageLayer);
    event.registerLayerDefinition(
        MilkBathCoreModelLayers.MILK_BATH_CORE_WIND,
        MilkBathCoreBlockEntityRenderer::createWindLayer);
    event.registerLayerDefinition(
        MilkBathCoreModelLayers.MILK_BATH_CORE_EYE,
        MilkBathCoreBlockEntityRenderer::createEyeLayer);
    event.registerLayerDefinition(
        MilkBathCoreModelLayers.MILK_BATH_CORE_SHELL,
        MilkBathCoreBlockEntityRenderer::createShellLayer);

    // Honey Bath Core Layers
    event.registerLayerDefinition(
        HoneyBathCoreModelLayers.HONEY_BATH_CORE_CAGE,
        HoneyBathCoreBlockEntityRenderer::createCageLayer);
    event.registerLayerDefinition(
        HoneyBathCoreModelLayers.HONEY_BATH_CORE_WIND,
        HoneyBathCoreBlockEntityRenderer::createWindLayer);
    event.registerLayerDefinition(
        HoneyBathCoreModelLayers.HONEY_BATH_CORE_EYE,
        HoneyBathCoreBlockEntityRenderer::createEyeLayer);
    event.registerLayerDefinition(
        HoneyBathCoreModelLayers.HONEY_BATH_CORE_SHELL,
        HoneyBathCoreBlockEntityRenderer::createShellLayer);

    // Rose Bath Core Layers
    event.registerLayerDefinition(
        RoseBathCoreModelLayers.ROSE_BATH_CORE_CAGE,
        RoseBathCoreBlockEntityRenderer::createCageLayer);
    event.registerLayerDefinition(
        RoseBathCoreModelLayers.ROSE_BATH_CORE_WIND,
        RoseBathCoreBlockEntityRenderer::createWindLayer);
    event.registerLayerDefinition(
        RoseBathCoreModelLayers.ROSE_BATH_CORE_EYE,
        RoseBathCoreBlockEntityRenderer::createEyeLayer);
    event.registerLayerDefinition(
        RoseBathCoreModelLayers.ROSE_BATH_CORE_SHELL,
        RoseBathCoreBlockEntityRenderer::createShellLayer);

    // Peony Bath Core Layers
    event.registerLayerDefinition(
        PeonyBathCoreModelLayers.PEONY_BATH_CORE_CAGE,
        PeonyBathCoreBlockEntityRenderer::createCageLayer);
    event.registerLayerDefinition(
        PeonyBathCoreModelLayers.PEONY_BATH_CORE_WIND,
        PeonyBathCoreBlockEntityRenderer::createWindLayer);
    event.registerLayerDefinition(
        PeonyBathCoreModelLayers.PEONY_BATH_CORE_EYE,
        PeonyBathCoreBlockEntityRenderer::createEyeLayer);
    event.registerLayerDefinition(
        PeonyBathCoreModelLayers.PEONY_BATH_CORE_SHELL,
        PeonyBathCoreBlockEntityRenderer::createShellLayer);
  }

  @SubscribeEvent
  public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(BlockEntitiesRegister.BATHING_POOL.get(), mod.crabmod.showercore.client.renderer.PoolRenderer::new);
    // milk, honey, peony, rose, herbal
    event.registerBlockEntityRenderer(
        BlockEntitiesRegister.HOT_WATER_CORE_BLOCK_ENTITY.get(),
        HotWaterCoreBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(
        BlockEntitiesRegister.HERBAL_BATH_CORE_BLOCK_ENTITY.get(),
        HerbalBathCoreBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(
        BlockEntitiesRegister.MILK_BATH_CORE_BLOCK_ENTITY.get(),
        MilkBathCoreBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(
        BlockEntitiesRegister.HONEY_BATH_CORE_BLOCK_ENTITY.get(),
        HoneyBathCoreBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(
        BlockEntitiesRegister.ROSE_BATH_CORE_BLOCK_ENTITY.get(),
        RoseBathCoreBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(
        BlockEntitiesRegister.PEONY_BATH_CORE_BLOCK_ENTITY.get(),
        PeonyBathCoreBlockEntityRenderer::new);
    
    event.registerBlockEntityRenderer(
        BlockEntitiesRegister.BATHTUB_BLOCK_ENTITY.get(),
        BathtubBlockEntityRenderer::new);

    event.registerEntityRenderer(EntityRegister.SEAT_ENTITY.get(), SeatEntityRenderer::new);
    event.registerEntityRenderer(EntityRegister.FAUCET_ENTITY.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    event.registerEntityRenderer(EntityRegister.RUBBER_DUCK.get(), mod.crabmod.showercore.client.renderer.RubberDuckRenderer::new);
  }

  @SubscribeEvent
  public static void registerParticles(final RegisterParticleProvidersEvent event) {
    Minecraft.getInstance()
        .particleEngine
        .register(
            ParticleRegister.BATH_CORE_PARTICLE.get(),
            EnchantmentTableParticle.NautilusProvider::new);

    Minecraft.getInstance()
        .particleEngine
        .register(
            ParticleRegister.HERBAL_BATH_BATH_CORE_PARTICLE.get(),
            EnchantmentTableParticle.NautilusProvider::new);
    Minecraft.getInstance()
        .particleEngine
        .register(
            ParticleRegister.HOT_WATER_BATH_CORE_PARTICLE.get(),
            EnchantmentTableParticle.NautilusProvider::new);
    Minecraft.getInstance()
        .particleEngine
        .register(
            ParticleRegister.HONEY_BATH_BATH_CORE_PARTICLE.get(),
            EnchantmentTableParticle.NautilusProvider::new);
    Minecraft.getInstance()
        .particleEngine
        .register(
            ParticleRegister.MILK_BATH_BATH_CORE_PARTICLE.get(),
            EnchantmentTableParticle.NautilusProvider::new);
    Minecraft.getInstance()
        .particleEngine
        .register(
            ParticleRegister.PEONY_BATH_BATH_CORE_PARTICLE.get(),
            EnchantmentTableParticle.NautilusProvider::new);
    Minecraft.getInstance()
        .particleEngine
        .register(
            ParticleRegister.ROSE_BATH_BATH_CORE_PARTICLE.get(),
            EnchantmentTableParticle.NautilusProvider::new);

    event.registerSpriteSet(
        ParticleRegister.HOT_WATER_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
    event.registerSpriteSet(
        ParticleRegister.HERBAL_BATH_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
    event.registerSpriteSet(
        ParticleRegister.HONEY_BATH_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
    event.registerSpriteSet(
        ParticleRegister.MILK_BATH_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
    event.registerSpriteSet(
        ParticleRegister.PEONY_BATH_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
    event.registerSpriteSet(
        ParticleRegister.ROSE_BATH_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
  }

  public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
    event.registerSpriteSet(
        ParticleRegister.HOT_WATER_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
    event.registerSpriteSet(
        ParticleRegister.HERBAL_BATH_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
    event.registerSpriteSet(
        ParticleRegister.HONEY_BATH_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
    event.registerSpriteSet(
        ParticleRegister.MILK_BATH_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
    event.registerSpriteSet(
        ParticleRegister.PEONY_BATH_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
    event.registerSpriteSet(
        ParticleRegister.ROSE_BATH_SHOWER_PARTICLE.get(), ShowerParticle.Provider::new);
  }


}
