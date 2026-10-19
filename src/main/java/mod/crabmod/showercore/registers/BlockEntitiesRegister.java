package mod.crabmod.showercore.registers;

import static mod.crabmod.showercore.ShowerCore.MODID;

import java.util.ArrayList;
import java.util.List;
import mod.crabmod.showercore.block.bath_core.herbal_bath_core.HerbalBathCoreBlockEntity;
import mod.crabmod.showercore.block.bath_core.honey_bath_core.HoneyBathCoreBlockEntity;
import mod.crabmod.showercore.block.bath_core.hot_water_core.HotWaterCoreBlockEntity;
import mod.crabmod.showercore.block.bath_core.milk_bath_core.MilkBathCoreBlockEntity;
import mod.crabmod.showercore.block.bath_core.peony_bath_core.PeonyBathCoreBlockEntity;
import mod.crabmod.showercore.block.bath_core.rose_bath_core.RoseBathCoreBlockEntity;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.entity.ShowerHeadContainerEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntitiesRegister {
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
      DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

  public static final RegistryObject<BlockEntityType<HotWaterCoreBlockEntity>>
      HOT_WATER_CORE_BLOCK_ENTITY =
          BLOCK_ENTITIES.register(
              "hot_water_core_block_entity",
              () ->
                  BlockEntityType.Builder.of(
                          HotWaterCoreBlockEntity::new, BlocksRegister.HOT_WATER_CORE.get())
                      .build(null));

  public static final RegistryObject<BlockEntityType<HerbalBathCoreBlockEntity>>
      HERBAL_BATH_CORE_BLOCK_ENTITY =
          BLOCK_ENTITIES.register(
              "herbal_bath_core_block_entity",
              () ->
                  BlockEntityType.Builder.of(
                          HerbalBathCoreBlockEntity::new, BlocksRegister.HERBAL_BATH_CORE.get())
                      .build(null));

  public static final RegistryObject<BlockEntityType<HoneyBathCoreBlockEntity>>
      HONEY_BATH_CORE_BLOCK_ENTITY =
          BLOCK_ENTITIES.register(
              "honey_bath_core_block_entity",
              () ->
                  BlockEntityType.Builder.of(
                          HoneyBathCoreBlockEntity::new, BlocksRegister.HONEY_BATH_CORE.get())
                      .build(null));

  public static final RegistryObject<BlockEntityType<MilkBathCoreBlockEntity>>
      MILK_BATH_CORE_BLOCK_ENTITY =
          BLOCK_ENTITIES.register(
              "milk_bath_core_block_entity",
              () ->
                  BlockEntityType.Builder.of(
                          MilkBathCoreBlockEntity::new, BlocksRegister.MILK_BATH_CORE.get())
                      .build(null));

  public static final RegistryObject<BlockEntityType<PeonyBathCoreBlockEntity>>
      PEONY_BATH_CORE_BLOCK_ENTITY =
          BLOCK_ENTITIES.register(
              "peony_bath_core_block_entity",
              () ->
                  BlockEntityType.Builder.of(
                          PeonyBathCoreBlockEntity::new, BlocksRegister.PEONY_BATH_CORE.get())
                      .build(null));

  public static final RegistryObject<BlockEntityType<RoseBathCoreBlockEntity>>
      ROSE_BATH_CORE_BLOCK_ENTITY =
          BLOCK_ENTITIES.register(
              "rose_bath_core_block_entity",
              () ->
                  BlockEntityType.Builder.of(
                          RoseBathCoreBlockEntity::new, BlocksRegister.ROSE_BATH_CORE.get())
                      .build(null));

  public static final RegistryObject<BlockEntityType<ShowerHeadContainerEntity>>
      RAIN_SHOWER_HEAD_CONTAINER =
          BLOCK_ENTITIES.register(
              "rain_shower_head_container",
              () -> {
                  List<Block> blocks = new ArrayList<>();
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_BLACK.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_WHITE.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_ORANGE.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_MAGENTA.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_LIGHT_BLUE.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_YELLOW.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_LIME.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_PINK.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_GRAY.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_LIGHT_GRAY.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_CYAN.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_PURPLE.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_BLUE.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_BROWN.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_GREEN.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_RED.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_OAK.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_SPRUCE.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_BIRCH.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_JUNGLE.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_ACACIA.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_DARK_OAK.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_MANGROVE.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_CHERRY.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_BAMBOO.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_CRIMSON.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_WARPED.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_STONE.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_COBBLESTONE.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_IRON.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_GOLD.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_COPPER.get());
                  blocks.add(BlocksRegister.RAIN_SHOWER_HEAD_DIAMOND.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_BLACK.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_WHITE.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_ORANGE.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_MAGENTA.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_LIGHT_BLUE.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_YELLOW.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_LIME.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_PINK.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_GRAY.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_LIGHT_GRAY.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_CYAN.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_PURPLE.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_BLUE.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_BROWN.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_GREEN.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_RED.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_OAK.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_SPRUCE.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_BIRCH.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_JUNGLE.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_ACACIA.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_DARK_OAK.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_MANGROVE.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_CHERRY.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_BAMBOO.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_CRIMSON.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_WARPED.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_STONE.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_COBBLESTONE.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_IRON.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_GOLD.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_COPPER.get());
                  blocks.add(BlocksRegister.COMPACT_SHOWER_HEAD_DIAMOND.get());
                  blocks.add(BlocksRegister.BAMBOO_SHOWER_HEAD_BAMBOO.get());
                  blocks.add(BlocksRegister.COLUMN_SHOWER_HEAD_WHITE.get());
                  blocks.add(BlocksRegister.CORAL_SHOWER_HEAD_PINK.get());
                  blocks.add(BlocksRegister.BUCKET_SHOWER_HEAD_SPRUCE.get());
                  blocks.add(BlocksRegister.PANEL_SHOWER_HEAD_WHITE.get());
                  return BlockEntityType.Builder.of(ShowerHeadContainerEntity::new, blocks.toArray(new Block[0])).build(null);
              });

  public static final RegistryObject<BlockEntityType<BathtubBlockEntity>> BATHTUB_BLOCK_ENTITY =
      BLOCK_ENTITIES.register(
          "bathtub_block_entity",
          () -> {
             List<Block> bathtubBlocks = new ArrayList<>();
             bathtubBlocks.add(BlocksRegister.BATHTUB_WHITE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_ORANGE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_MAGENTA.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_LIGHT_BLUE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_YELLOW.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_LIME.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_PINK.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_GRAY.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_LIGHT_GRAY.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CYAN.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_PURPLE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_BLUE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_BROWN.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_GREEN.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_RED.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_BLACK.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_OAK.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_SPRUCE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_BIRCH.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_JUNGLE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_ACACIA.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_DARK_OAK.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_MANGROVE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CHERRY.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_BAMBOO.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CRIMSON.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_WARPED.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_STONE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_COBBLESTONE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_IRON.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_GOLD.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_COPPER.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_DIAMOND.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_ORANGE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_MAGENTA.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_LIGHT_BLUE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_YELLOW.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_LIME.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_PINK.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_GRAY.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_LIGHT_GRAY.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_CYAN.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_PURPLE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_BLUE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_BROWN.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_GREEN.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_RED.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_BLACK.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_OAK.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_SPRUCE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_BIRCH.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_JUNGLE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_ACACIA.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_DARK_OAK.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_MANGROVE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_CHERRY.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_BAMBOO.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_CRIMSON.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_WARPED.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_STONE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_COBBLESTONE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_IRON.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_GOLD.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_COPPER.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_CLAWFOOT_DIAMOND.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_OFURO_BIRCH.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_ROMAN_WHITE.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_SEASHELL_PINK.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_WASHTUB_IRON.get());
             bathtubBlocks.add(BlocksRegister.BATHTUB_SPA_WHITE.get());
             return BlockEntityType.Builder.of(BathtubBlockEntity::new, bathtubBlocks.toArray(new Block[0]))
                  .build(null);
          });

  public static final RegistryObject<BlockEntityType<mod.crabmod.showercore.pool.PoolBlockEntity>> BATHING_POOL =
      BLOCK_ENTITIES.register("bathing_pool", () -> BlockEntityType.Builder.of(mod.crabmod.showercore.pool.PoolBlockEntity::new, BlocksRegister.BATHING_POOL.get()).build(null));

  public static void register(IEventBus eventBus) {
    BLOCK_ENTITIES.register(eventBus);
  }
}
