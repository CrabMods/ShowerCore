package mod.crabmod.showercore.block;

import javax.annotation.Nullable;
import mod.crabmod.showercore.geometry.BathroomGeometry;
import mod.crabmod.showercore.entity.FaucetInteractionEntity;
import mod.crabmod.showercore.entity.FaucetInteractionGeometry;
import mod.crabmod.showercore.entity.SeatEntity;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import mod.crabmod.showercore.registers.ParticleRegister;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.AABB;
import java.util.List;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.registers.BlockEntitiesRegister;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import mod.crabmod.showercore.Config;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import java.util.Collections;
import java.util.Optional;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.serialization.MapCodec;
import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidBucketItem;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.custom_fluid.SplashCustomFluidBottleItem;

public class BathtubBlock extends HorizontalDirectionalBlock implements EntityBlock, mod.crabmod.showercore.pool.BathFaucet, mod.crabmod.showercore.geometry.BathMountSurface {
  public static final MapCodec<BathtubBlock> CODEC = simpleCodec(BathtubBlock::new);

  @Override
  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
     return CODEC;
  }

  public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
  public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);
  public static final BooleanProperty RUNNING = BooleanProperty.create("running");
  public static final EnumProperty<LiquidType> LIQUID = EnumProperty.create("liquid", LiquidType.class);

  public enum LiquidType implements StringRepresentable {
      EMPTY("empty"),
      WATER("water"),
      HOT_WATER("hot_water"),
      HERBAL_BATH("herbal_bath"),
      HONEY_BATH("honey_bath"),
      MILK_BATH("milk_bath"),
      PEONY_BATH("peony_bath"),
      ROSE_BATH("rose_bath"),
      CUSTOM("custom");

      private final String name;

      private LiquidType(String name) {
         this.name = name;
      }

      public String toString() {
         return this.name;
      }

      public String getSerializedName() {
         return this.name;
      }
   }

  // Shapes for FOOT part (The part you place)
  protected static final VoxelShape FOOT_NORTH_SHAPE = Shapes.or(
      Block.box(0, 0, 0, 16, 4, 16),
      Block.box(0, 4, 0, 2, 12, 16), // West wall
      Block.box(14, 4, 0, 16, 12, 16), // East wall
      Block.box(0, 4, 14, 16, 12, 16) // South wall (Back of foot)
  );
  protected static final VoxelShape FOOT_SOUTH_SHAPE = Shapes.or(
      Block.box(0, 0, 0, 16, 4, 16),
      Block.box(0, 4, 0, 2, 12, 16),
      Block.box(14, 4, 0, 16, 12, 16),
      Block.box(0, 4, 0, 16, 12, 2) // North wall
  );
  protected static final VoxelShape FOOT_WEST_SHAPE = Shapes.or(
      Block.box(0, 0, 0, 16, 4, 16),
      Block.box(0, 4, 0, 16, 12, 2), // North wall
      Block.box(0, 4, 14, 16, 12, 16), // South wall
      Block.box(14, 4, 0, 16, 12, 16) // East wall
  );
  protected static final VoxelShape FOOT_EAST_SHAPE = Shapes.or(
      Block.box(0, 0, 0, 16, 4, 16),
      Block.box(0, 4, 0, 16, 12, 2),
      Block.box(0, 4, 14, 16, 12, 16),
      Block.box(0, 4, 0, 2, 12, 16) // West wall
  );

  // Shapes for HEAD part (The part attached)
  protected static final VoxelShape HEAD_NORTH_SHAPE = Shapes.or(
      Block.box(0, 0, 0, 16, 4, 16),
      Block.box(0, 4, 0, 2, 12, 16), // West wall
      Block.box(14, 4, 0, 16, 12, 16), // East wall
      Block.box(0, 4, 0, 16, 12, 2) // North wall (Front of head)
  );
  protected static final VoxelShape HEAD_SOUTH_SHAPE = Shapes.or(
      Block.box(0, 0, 0, 16, 4, 16),
      Block.box(0, 4, 0, 2, 12, 16),
      Block.box(14, 4, 0, 16, 12, 16),
      Block.box(0, 4, 14, 16, 12, 16) // South wall
  );
  protected static final VoxelShape HEAD_WEST_SHAPE = Shapes.or(
      Block.box(0, 0, 0, 16, 4, 16),
      Block.box(0, 4, 0, 16, 12, 2),
      Block.box(0, 4, 14, 16, 12, 16),
      Block.box(0, 4, 0, 2, 12, 16) // West wall
  );
  protected static final VoxelShape HEAD_EAST_SHAPE = Shapes.or(
      Block.box(0, 0, 0, 16, 4, 16),
      Block.box(0, 4, 0, 16, 12, 2),
      Block.box(0, 4, 14, 16, 12, 16),
      Block.box(14, 4, 0, 16, 12, 16) // East wall
  );

  private final BathroomGeometry.Tub mountGeometry;

  public BathroomGeometry.Tub mountGeometry() {
      return mountGeometry;
  }

  public BathtubBlock(Properties properties) {
      this(properties, BathroomGeometry.STANDARD_TUB);
  }

  public BathtubBlock(Properties properties, BathroomGeometry.Tub geometry) {
    super(properties.lightLevel(state -> state.getValue(LIGHT_LEVEL)));
    this.mountGeometry = geometry;
    this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT).setValue(LIQUID, LiquidType.EMPTY).setValue(LIGHT_LEVEL, 0));
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new BathtubBlockEntity(pos, state);
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      if (level.isClientSide) return null;
      if (type != BlockEntitiesRegister.BATHTUB_BLOCK_ENTITY.get()) return null;
      return (lvl, pos, st, be) -> serverTick(lvl, pos, st, (BathtubBlockEntity) be);
  }

  // Hot bathtubs melt snow layers / snow blocks within 3 blocks. Active regardless of
  // whether Serene Seasons is installed — SS just makes this visibly matter by keeping
  // snow on the ground outside snow biomes.
  private static void serverTick(Level level, BlockPos pos, BlockState state, BathtubBlockEntity be) {
      if ((level.getGameTime() + pos.asLong()) % 80L != 0L) return;

      LiquidType liquid = state.getValue(LIQUID);
      if (liquid == LiquidType.EMPTY || liquid == LiquidType.WATER) return;

      boolean isHot = (liquid != LiquidType.CUSTOM)
              || mod.crabmod.showercore.utils.CoreUtils.isCustomFluidHotAt(level, pos);
      if (!isHot) return;

      final int radius = 3;
      BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();
      for (int dx = -radius; dx <= radius; dx++) {
          for (int dy = -radius; dy <= radius; dy++) {
              for (int dz = -radius; dz <= radius; dz++) {
                  if (dx == 0 && dy == 0 && dz == 0) continue;
                  check.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                  BlockState bs = level.getBlockState(check);
                  if (bs.is(Blocks.SNOW) || bs.is(Blocks.SNOW_BLOCK)) {
                      level.setBlock(check, Blocks.AIR.defaultBlockState(), 3);
                  } else if (bs.is(Blocks.POWDER_SNOW)) {
                      level.setBlock(check, Blocks.AIR.defaultBlockState(), 3);
                  }
              }
          }
      }
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    Direction direction = state.getValue(FACING);
    BedPart part = state.getValue(PART);
    if (mountGeometry != BathroomGeometry.STANDARD_TUB && mountGeometry != BathroomGeometry.CLAWFOOT_TUB) {
        double floor = mountGeometry.floorY(), rim = mountGeometry.rimY();
        VoxelShape shape = direction.getAxis() == Direction.Axis.Z
                ? Shapes.or(Block.box(0,0,0,16,floor,16), Block.box(0,floor,0,2,rim,16), Block.box(14,floor,0,16,rim,16))
                : Shapes.or(Block.box(0,0,0,16,floor,16), Block.box(0,floor,0,16,rim,2), Block.box(0,floor,14,16,rim,16));
        Direction end = part == BedPart.HEAD ? direction : direction.getOpposite();
        return Shapes.or(shape, switch (end) {
            case NORTH -> Block.box(0,floor,0,16,rim,2);
            case SOUTH -> Block.box(0,floor,14,16,rim,16);
            case WEST -> Block.box(0,floor,0,2,rim,16);
            default -> Block.box(14,floor,0,16,rim,16);
        });
    }

    if (part == BedPart.FOOT) {
      switch (direction) {
        case NORTH: return FOOT_NORTH_SHAPE;
        case SOUTH: return FOOT_SOUTH_SHAPE;
        case WEST: return FOOT_WEST_SHAPE;
        case EAST: return FOOT_EAST_SHAPE;
        default: return FOOT_NORTH_SHAPE;
      }
    } else {
      switch (direction) {
        case NORTH: return HEAD_NORTH_SHAPE;
        case SOUTH: return HEAD_SOUTH_SHAPE;
        case WEST: return HEAD_WEST_SHAPE;
        case EAST: return HEAD_EAST_SHAPE;
        default: return HEAD_NORTH_SHAPE;
      }
    }
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    Direction direction = context.getHorizontalDirection();
    BlockPos blockpos = context.getClickedPos();
    BlockPos blockpos1 = blockpos.relative(direction);
    Level level = context.getLevel();
    return level.getBlockState(blockpos1).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(blockpos1)
        ? this.defaultBlockState().setValue(FACING, direction)
        : null;
  }

  @Override
  public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);
    if (!level.isClientSide) {
      BlockPos blockpos = pos.relative(state.getValue(FACING));
      level.setBlock(blockpos, state.setValue(PART, BedPart.HEAD), 3);
      level.blockUpdated(pos, Blocks.AIR);
      state.updateNeighbourShapes(level, pos, 3);

      ResourceLocation materialBlockId = getMaterialBlockId(stack);
      var materialFace = mod.crabmod.showercore.item.BathtubBlockItem.getMaterialFace(stack);
      BlockEntity footBe = level.getBlockEntity(pos);
      if (footBe instanceof BathtubBlockEntity footBathtub) {
          if (materialBlockId != null) {
              footBathtub.setMaterial(materialBlockId, materialFace);
          } else {
              materialBlockId = footBathtub.getMaterialBlockId();
              materialFace = footBathtub.getMaterialFace();
          }
      }
      BlockEntity headBe = level.getBlockEntity(blockpos);
      if (headBe instanceof BathtubBlockEntity headBathtub) {
          headBathtub.setMaterial(materialBlockId, materialFace);
      }

      spawnFaucetInteractionEntities(level, blockpos, state);
    }
  }

  private static void spawnFaucetInteractionEntities(Level level, BlockPos headPos, BlockState state) {
      Direction facing = state.getValue(FACING);
      Direction right = facing.getClockWise();
      Direction towardFoot = facing.getOpposite();
      for (var local : geometryFor(state).faucets()) {
          double x = FaucetInteractionGeometry.worldX(
                  headPos.getX(), right.getStepX(), towardFoot.getStepX(), local.x() / 16.0, local.z() / 16.0);
          double y = FaucetInteractionGeometry.worldY(headPos.getY(), local.y() / 16.0);
          double z = FaucetInteractionGeometry.worldZ(
                  headPos.getZ(), right.getStepZ(), towardFoot.getStepZ(), local.x() / 16.0, local.z() / 16.0);
          level.addFreshEntity(new FaucetInteractionEntity(level, x, y, z));
      }
  }

  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
    if (direction == getNeighbourDirection(state.getValue(PART), state.getValue(FACING))) {
      return neighborState.is(this) && neighborState.getValue(PART) != state.getValue(PART)
          ? state
          : Blocks.AIR.defaultBlockState();
    } else {
      return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }
  }

  private static Direction getNeighbourDirection(BedPart part, Direction direction) {
    return part == BedPart.FOOT ? direction : direction.getOpposite();
  }

  public static SeatEntity createSeatEntity(Level level, BlockPos pos, BlockState state) {
      return new SeatEntity(level, pos.getX() + 0.5, pos.getY() + getSeatYOffset(state), pos.getZ() + 0.5);
  }

  public static double getSeatYOffset(BlockState state) {
      return geometryFor(state).seatOffset();
  }

  public static float waterLevelFor(BlockState state) {
      return geometryFor(state).waterLevel();
  }

  public static float duckFloatSurfaceFor(BlockState state) {
      return waterLevelFor(state) + BathtubWaterGeometry.DUCK_FLOAT_CLEARANCE;
  }

  public static Vec3 duckPlacementFor(BlockPos pos, BlockState state, Vec3 requestedPos) {
      return new Vec3(
              BathtubWaterGeometry.duckSafeWorldCoordinate(pos.getX(), requestedPos.x),
              pos.getY() + duckFloatSurfaceFor(state),
              BathtubWaterGeometry.duckSafeWorldCoordinate(pos.getZ(), requestedPos.z));
  }

  public static boolean isClawfootBathtub(BlockState state) {
      return geometryFor(state) == BathroomGeometry.CLAWFOOT_TUB;
  }

  public static BathroomGeometry.Tub geometryFor(BlockState state) {
      return ((BathtubBlock) state.getBlock()).mountGeometry();
  }

  @Nullable
  private static ResourceLocation getMaterialBlockId(ItemStack stack) {
      CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
      if (customData == null) {
          return null;
      }
      CompoundTag tag = customData.copyTag();
      if (!tag.contains(BathtubBlockEntity.TAG_MATERIAL_BLOCK_ID)) {
          return null;
      }
      return ResourceLocation.tryParse(tag.getString(BathtubBlockEntity.TAG_MATERIAL_BLOCK_ID));
  }

  @Nullable
  private static Block getMaterialBlock(BlockGetter level, BlockPos pos) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof BathtubBlockEntity bathtubEntity && bathtubEntity.getMaterialBlockId() != null) {
          return BuiltInRegistries.BLOCK.getOptional(bathtubEntity.getMaterialBlockId())
                  .filter(block -> !(block instanceof BathtubBlock) && block != Blocks.AIR)
                  .orElse(null);
      }
      return null;
  }

  @Override
  public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
      Block materialBlock = getMaterialBlock(level, pos);
      return materialBlock == null
              ? super.getSoundType(state, level, pos, entity)
              : materialBlock.defaultBlockState().getSoundType(level, pos, entity);
  }

  @Override
  protected ItemInteractionResult useItemOn(ItemStack itemstack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

      // Place Rubber Duck
      if (itemstack.getItem() instanceof mod.crabmod.showercore.item.RubberDuckItem) {
          return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }

      // Empty bucket on a filled bathtub: pick up the fluid instead of falling through
      // to FAIL (which used to leave the player unable to drain custom fluids).
      if (itemstack.getItem() == Items.BUCKET) {
          BlockEntity be = level.getBlockEntity(pos);
          if (be instanceof BathtubBlockEntity bathtubBe && bathtubBe.getFluidTank().getFluidAmount() >= 1000) {
              if (!level.isClientSide) {
                  ResourceLocation customId = bathtubBe.getCustomFluidId();
                  ItemStack bucketOut;
                  if (customId != null) {
                      bucketOut = CustomFluidAPI.createBucket(customId);
                  } else {
                      FluidStack drained = bathtubBe.getFluidTank().getFluid();
                      bucketOut = new ItemStack(drained.getFluid().getBucket());
                      if (bucketOut.isEmpty()) {
                          bucketOut = new ItemStack(Items.WATER_BUCKET);
                      }
                  }
                  bathtubBe.getFluidTank().setFluid(FluidStack.EMPTY);
                  bathtubBe.setCustomFluidId(null);
                  syncFluidToOtherPart(level, pos, state, FluidStack.EMPTY);
                  updateLiquidState(level, pos, state, FluidStack.EMPTY);
                  if (!player.isCreative()) {
                      itemstack.shrink(1);
                      if (itemstack.isEmpty()) {
                          player.setItemInHand(hand, bucketOut);
                      } else if (!player.getInventory().add(bucketOut)) {
                          player.drop(bucketOut, false);
                      }
                  }
                  level.playSound(null, pos, SoundEvents.BUCKET_FILL,
                          net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
              }
              return ItemInteractionResult.sidedSuccess(level.isClientSide);
          }
      }

      if (itemstack.isEmpty() && state.getValue(PART) == BedPart.HEAD) {
          if (!level.isClientSide) {
              Entity occupantToAsk = null;
              // Check if HEAD is occupied
              List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
              if (!seats.isEmpty() && !seats.get(0).getPassengers().isEmpty()) {
                  occupantToAsk = seats.get(0).getFirstPassenger();
              } else {
                  // HEAD is empty. Check FOOT.
                  Direction direction = state.getValue(FACING);
                  BlockPos footPos = pos.relative(direction.getOpposite());
                  List<SeatEntity> footSeats = level.getEntitiesOfClass(SeatEntity.class, new AABB(footPos));
                  
                  if (!footSeats.isEmpty() && !footSeats.get(0).getPassengers().isEmpty()) {
                      // Ask the passenger at FOOT
                      occupantToAsk = footSeats.get(0).getFirstPassenger();
                  }
              }

              if (occupantToAsk instanceof Player occupant) {
                  if (occupant != player) {
                      player.sendSystemMessage(Component.translatable("message.showercore.ask_permission", occupant.getName()));
                      
                      Component accept = Component.translatable("message.showercore.accept")
                          .withStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.GREEN)
                          .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/showercore accept_bath " + player.getName().getString())));
                      
                      Component deny = Component.translatable("message.showercore.reject")
                          .withStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.RED)
                          .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/showercore deny_bath " + player.getName().getString())));

                      occupant.sendSystemMessage(Component.translatable("message.showercore.request_join", player.getName())
                          .append(accept).append(" ").append(deny));
                  }
              } else {
                  SeatEntity seat = createSeatEntity(level, pos, state);
                  level.addFreshEntity(seat);
                  player.startRiding(seat);
              }
          }
          return ItemInteractionResult.sidedSuccess(level.isClientSide);
      }

      if (itemstack.isEmpty() && state.getValue(PART) == BedPart.FOOT) {
          if (!level.isClientSide) {
              Direction direction = state.getValue(FACING);
              BlockPos headPos = pos.relative(direction);
              Entity occupantToAsk = null;
              
              // Check if HEAD is occupied
              List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(headPos));
              if (!seats.isEmpty() && !seats.get(0).getPassengers().isEmpty()) {
                  occupantToAsk = seats.get(0).getFirstPassenger();
              } else {
                  // HEAD is empty. Check FOOT (current pos).
                  List<SeatEntity> footSeats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
                  
                  if (!footSeats.isEmpty() && !footSeats.get(0).getPassengers().isEmpty()) {
                      // Ask the passenger at FOOT
                      occupantToAsk = footSeats.get(0).getFirstPassenger();
                  }
              }

              if (occupantToAsk instanceof Player occupant) {
                  if (occupant != player) {
                      player.sendSystemMessage(Component.translatable("message.showercore.ask_permission", occupant.getName()));
                      
                      Component accept = Component.translatable("message.showercore.accept")
                          .withStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.GREEN)
                          .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/showercore accept_bath " + player.getName().getString())));
                      
                      Component deny = Component.translatable("message.showercore.reject")
                          .withStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.RED)
                          .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/showercore deny_bath " + player.getName().getString())));

                      occupant.sendSystemMessage(Component.translatable("message.showercore.request_join", player.getName())
                          .append(accept).append(" ").append(deny));
                  }
              } else {
                  SeatEntity seat = createSeatEntity(level, headPos, level.getBlockState(headPos));
                  level.addFreshEntity(seat);
                  player.startRiding(seat);
              }
          }
          return ItemInteractionResult.sidedSuccess(level.isClientSide);
      }

      // Fluid interaction
      if (!itemstack.isEmpty()) {
          BlockEntity be = level.getBlockEntity(pos);
          if (be instanceof BathtubBlockEntity bathtubBe) {
              IFluidHandler handler = bathtubBe.getFluidTank();

              // Handle hotBath custom fluid items (buckets/bottles) that use Data Components
              // These items store their fluid ID via CustomFluidDataComponents instead of
              // standard fluid handler capabilities, so FluidUtil cannot handle them.
              if (CustomFluidAPI.hasCustomFluid(itemstack)) {
                  Optional<CustomFluidDefinition> defOpt = CustomFluidAPI.getFluidFromItem(itemstack);
                  if (defOpt.isPresent()) {
                      CustomFluidDefinition definition = defOpt.get();
                      // Use the dynamic custom fluid as the FluidStack base
                      FluidStack customFluidStack = new FluidStack(
                              DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get(), 1000);
                      int filled = handler.fill(customFluidStack, IFluidHandler.FluidAction.SIMULATE);
                      if (filled == 1000) {
                          if (!level.isClientSide) {
                              handler.fill(customFluidStack, IFluidHandler.FluidAction.EXECUTE);

                              // Store the custom fluid ID in the block entity
                              bathtubBe.setCustomFluidId(definition.id());

                              if (!player.isCreative()) {
                                  // Return the appropriate empty container
                                  if (itemstack.getItem() instanceof CustomFluidBucketItem) {
                                      player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                                  } else if (itemstack.getItem() instanceof SplashCustomFluidBottleItem) {
                                      // Splash bottles are consumed without returning a container
                                      itemstack.shrink(1);
                                  } else {
                                      // Regular bottle items - shrink and give back glass bottle
                                      itemstack.shrink(1);
                                      ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                                      if (!player.getInventory().add(bottle)) {
                                          player.drop(bottle, false);
                                      }
                                  }
                              }

                              level.playSound(null, pos, SoundEvents.BUCKET_EMPTY,
                                      net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

                              FluidStack currentFluid = bathtubBe.getFluidTank().getFluid();
                              syncFluidToOtherPart(level, pos, state, currentFluid);
                              updateLiquidState(level, pos, state, currentFluid);
                          }
                          return ItemInteractionResult.sidedSuccess(level.isClientSide);
                      }
                  }
              }

              // Try standard FluidUtil interaction first
              boolean success = FluidUtil.interactWithFluidHandler(player, hand, handler);

              if (success) {
                  if (!level.isClientSide) {
                      FluidStack fluid = bathtubBe.getFluidTank().getFluid();
                      syncFluidToOtherPart(level, pos, state, fluid);
                      updateLiquidState(level, pos, state, fluid);
                  }
                  return ItemInteractionResult.sidedSuccess(level.isClientSide);
              }

              // Fallback for BucketItems that are missing FluidHandler capability (e.g. some modded buckets)
              if (itemstack.getItem() instanceof BucketItem bucketItem) {
                   Fluid bucketContent = bucketItem.content;
                   if (bucketContent != Fluids.EMPTY) {
                       FluidStack fluidStack = new FluidStack(bucketContent, 1000);
                       // Simulate filling
                       int filled = handler.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE);
                       if (filled == 1000) {
                           if (!level.isClientSide) {
                               // Execute filling
                               handler.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);

                               if (!player.isCreative()) {
                                   player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                               }

                               SoundEvent sound = bucketContent.getFluidType().getSound(player, level, pos, net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY);
                               if (sound == null) sound = SoundEvents.BUCKET_EMPTY;
                               level.playSound(null, pos, sound, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

                               FluidStack currentFluid = bathtubBe.getFluidTank().getFluid();
                               syncFluidToOtherPart(level, pos, state, currentFluid);
                               updateLiquidState(level, pos, state, currentFluid);
                           }
                           return ItemInteractionResult.sidedSuccess(level.isClientSide);
                       }
                   }
              }

              // Bucket-like click that didn't produce a fluid transfer (bathtub full,
              // incompatible fluid, empty bucket on empty bathtub, etc.). Return CONSUME
              // so consumesAction() short-circuits MultiPlayerGameMode.performUseItemOn.
              // FAIL is NOT a hard stop here: FAIL.consumesAction()==false, so the chain
              // falls through to itemstack.useOn (PASS by default for BucketItem), then
              // Minecraft.startUseItem continues to gameMode.useItem → BucketItem.use,
              // which places fluid in the block adjacent to the bathtub (creative dupe).
              if (itemstack.getItem() instanceof BucketItem
                      || itemstack.getItem() instanceof CustomFluidBucketItem
                      || CustomFluidAPI.hasCustomFluid(itemstack)) {
                  return ItemInteractionResult.CONSUME;
              }
          }
      }
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   private void syncFluidToOtherPart(Level level, BlockPos pos, BlockState state, FluidStack fluid) {
       Direction direction = state.getValue(FACING);
       BedPart part = state.getValue(PART);
       BlockPos otherPos = part == BedPart.FOOT ? pos.relative(direction) : pos.relative(direction.getOpposite());
       BlockEntity otherBe = level.getBlockEntity(otherPos);
       if (otherBe instanceof BathtubBlockEntity otherBathtubBe) {
           otherBathtubBe.getFluidTank().setFluid(fluid.copy());
           // Sync custom fluid ID to the other part
           BlockEntity thisBe = level.getBlockEntity(pos);
           if (thisBe instanceof BathtubBlockEntity thisBathtubBe) {
               otherBathtubBe.setCustomFluidId(thisBathtubBe.getCustomFluidId());
           }
           BlockState otherState = level.getBlockState(otherPos);
           if (otherState.getBlock() == this) {
               updateLiquidState(level, otherPos, otherState, fluid);
           }
       }
   }

   private void updateLiquidState(Level level, BlockPos pos, BlockState state, FluidStack fluid) {
       LiquidType newLiquid = LiquidType.EMPTY;
       ResourceLocation detectedCustomFluidId = null;

       if (!fluid.isEmpty()) {
           ResourceLocation resourceLocation = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
           String fluidPath = resourceLocation.getPath();
           String namespace = resourceLocation.getNamespace();

           if (fluidPath.equals("water")) {
               newLiquid = LiquidType.WATER;
           } else if (namespace.equals("hotbath")) {
               // Use namespace + exact path matching for built-in hotBath fluids
               // to avoid false positives from other mods with similar fluid names
               if (fluidPath.equals("hot_water_fluid") || fluidPath.equals("hot_water_flowing")) {
                   newLiquid = LiquidType.HOT_WATER;
               } else if (fluidPath.equals("herbal_bath_fluid") || fluidPath.equals("herbal_bath_flowing")) {
                   newLiquid = LiquidType.HERBAL_BATH;
               } else if (fluidPath.equals("honey_bath_fluid") || fluidPath.equals("honey_bath_flowing")) {
                   newLiquid = LiquidType.HONEY_BATH;
               } else if (fluidPath.equals("milk_bath_fluid") || fluidPath.equals("milk_bath_flowing")) {
                   newLiquid = LiquidType.MILK_BATH;
               } else if (fluidPath.equals("peony_bath_fluid") || fluidPath.equals("peony_bath_flowing")) {
                   newLiquid = LiquidType.PEONY_BATH;
               } else if (fluidPath.equals("rose_bath_fluid") || fluidPath.equals("rose_bath_flowing")) {
                   newLiquid = LiquidType.ROSE_BATH;
               } else {
                   // Check if this is the dynamic custom fluid (Data Components system in 1.21).
                   // In 1.21, all custom fluids share a single fluid type (hotbath:dynamic_custom_fluid)
                   // and the specific fluid identity is stored in the block entity via Data Components.
                   boolean isDynamicCustomFluid =
                           fluid.getFluid() == DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()
                        || fluid.getFluid() == DynamicFluidRegistry.DYNAMIC_FLUID_FLOWING.get();

                   if (isDynamicCustomFluid) {
                       // The custom fluid ID should already be stored in the block entity
                       // (set when the custom fluid bucket/bottle was used on the bathtub)
                       BlockEntity currentBe = level.getBlockEntity(pos);
                       if (currentBe instanceof BathtubBlockEntity currentBathtubBe
                               && currentBathtubBe.getCustomFluidId() != null) {
                           newLiquid = LiquidType.CUSTOM;
                           detectedCustomFluidId = currentBathtubBe.getCustomFluidId();
                       } else {
                           // Dynamic custom fluid without stored ID - fallback
                           newLiquid = LiquidType.HOT_WATER;
                       }
                   } else {
                       // Other hotBath namespace fluid (legacy per-fluid registrations)
                       for (CustomFluidDefinition def : CustomFluidAPI.getAllFluids()) {
                           if (resourceLocation.toString().contains(def.id().getPath())) {
                               newLiquid = LiquidType.CUSTOM;
                               detectedCustomFluidId = def.id();
                               break;
                           }
                       }
                       // Fallback to HOT_WATER if no custom fluid matched but still hotbath namespace
                       if (newLiquid != LiquidType.CUSTOM) {
                           newLiquid = LiquidType.HOT_WATER;
                       }
                   }
               }
           } else {
               // Non-hotbath mod fluid: check if it matches any registered custom fluid
               for (CustomFluidDefinition def : CustomFluidAPI.getAllFluids()) {
                   if (resourceLocation.toString().contains(def.id().getPath())
                           || def.id().toString().contains(fluidPath)) {
                       newLiquid = LiquidType.CUSTOM;
                       detectedCustomFluidId = def.id();
                       break;
                   }
               }
               // If no custom fluid matched, still mark as CUSTOM (generic modded fluid)
               if (newLiquid != LiquidType.CUSTOM) {
                   newLiquid = LiquidType.CUSTOM;
               }
           }
       }

       boolean running = state.getValue(RUNNING);
       if (newLiquid == LiquidType.EMPTY && fluid.isEmpty()) {
           running = false;
       }
       // Update the liquid before deriving its cached block-state light level.
       level.setBlock(pos, state.setValue(LIQUID, newLiquid).setValue(RUNNING, running), 3);

       BlockEntity be = level.getBlockEntity(pos);
       if (be instanceof BathtubBlockEntity bathtubBe) {
           if (newLiquid == LiquidType.CUSTOM) {
               bathtubBe.setCustomFluidId(detectedCustomFluidId);
           } else {
               bathtubBe.setCustomFluidId(null);
           }
       }
   }



  @Override
  public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
    Entity entity = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
    if (entity instanceof Player player && player.isCreative()) {
        return Collections.emptyList();
    }
    BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
    if (blockEntity instanceof BathtubBlockEntity bathtubEntity) {
      ItemStack stack = new ItemStack(this);
      CompoundTag tag = bathtubEntity.saveWithoutMetadata(bathtubEntity.getLevel().registryAccess());
      BlockItem.setBlockEntityData(stack, blockEntity.getType(), tag);
      return Collections.singletonList(stack);
    }
    return super.getDrops(state, params);
  }

  @Override
  public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      ItemStack stack = super.getCloneItemStack(level, pos, state);
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof BathtubBlockEntity bathtubEntity) {
          CompoundTag tag = bathtubEntity.saveWithoutMetadata(level.registryAccess());
          BlockItem.setBlockEntityData(stack, blockEntity.getType(), tag);
      }
      return stack;
  }

  @Override
  public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    if (!level.isClientSide) {
      BedPart bedpart = state.getValue(PART);

      BlockPos neighborPos = pos.relative(getNeighbourDirection(bedpart, state.getValue(FACING)));
      BlockState neighborState = level.getBlockState(neighborPos);
      if (neighborState.is(this) && neighborState.getValue(PART) != bedpart) {
        // Survival (flag 35 = UPDATE_SUPPRESS_DROPS | UPDATE_CLIENTS | UPDATE_NEIGHBORS):
        //   allows the updateNeighbourShapes cascade, which fires destroyBlock on THIS half
        //   and produces the single NBT-preserving drop via BathtubBlockEntity.
        // Creative (flag 51 = 35 | UPDATE_KNOWN_SHAPE 16):
        //   suppresses the cascade; otherwise cascade's destroyBlock(THIS, drop=true, null)
        //   bypasses the creative-player check in getDrops (entity is null) and dupes a bathtub.
        int flags = player.isCreative() ? 51 : 35;
        level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), flags);
        level.levelEvent(player, 2001, neighborPos, Block.getId(neighborState));
      }

      if (bedpart == BedPart.HEAD) {
          level.getEntitiesOfClass(FaucetInteractionEntity.class, new net.minecraft.world.phys.AABB(pos)).forEach(Entity::discard);
      }
    }
    return super.playerWillDestroy(level, pos, state, player);
  }

  @Override
  public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    LiquidType liquid = state.getValue(LIQUID);
    boolean produceSteam = false;

    if (liquid == LiquidType.CUSTOM) {
         BlockEntity be = level.getBlockEntity(pos);
         if (be instanceof BathtubBlockEntity bathtubBe) {
             // Check custom fluid definition for steam flag
             Optional<CustomFluidDefinition> defOpt = bathtubBe.getCustomFluidDefinition();
             if (defOpt.isPresent()) {
                 CustomFluidDefinition def = defOpt.get();
                 if (def.isHot() && def.showSteam()) {
                     produceSteam = true;
                 }
             } else {
                 // No custom fluid definition found; fall back to config steam fluids list
                 FluidStack fluidStack = bathtubBe.getFluidTank().getFluid();
                 if (!fluidStack.isEmpty()) {
                     Fluid fluid = fluidStack.getFluid();
                     if (Config.steamFluids.contains(fluid)) {
                         produceSteam = true;
                     }
                 }
             }
         }
    } else if (liquid != LiquidType.EMPTY && liquid != LiquidType.WATER) {
        produceSteam = true;
    }

    if (produceSteam && random.nextInt(10) == 0) {
      double x = (double) pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.8D;
      double y = (double) pos.getY() + 0.9D;
      double z = (double) pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.8D;
      level.addParticle((ParticleOptions) com.crabmod.hotbath.registers.ParticleRegister.STEAM_PARTICLE.get(), x, y, z, 0.0D, 0.02D, 0.0D);
    }

    // Bubble particles (HotBathBubbleParticle) are intentionally NOT spawned in bathtubs:
    // that particle's tick() removes itself unless the blockpos contains water or an
    // AbstractHotbathBlock. A BathtubBlock is neither, so every spawned bubble died on
    // its first tick and appeared as a single-frame flicker. The showBubbles flag still
    // takes effect for hotBath's own fluid blocks (CustomFluidBlock) where the particle
    // survives correctly.
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, PART, LIQUID, RUNNING, LIGHT_LEVEL);
  }



  @Override
  public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      LiquidType liquid = state.getValue(LIQUID);

      if (liquid == LiquidType.CUSTOM) {
          BlockEntity be = level.getBlockEntity(pos);
          if (be instanceof BathtubBlockEntity bathtubBe) {
              // Check for custom fluid definition and apply its effects
              Optional<CustomFluidDefinition> defOpt = bathtubBe.getCustomFluidDefinition();
              if (defOpt.isPresent()) {
                  ResourceLocation fluidId = bathtubBe.getCustomFluidId();
                  if (fluidId != null && entity instanceof ServerPlayer serverPlayer && !level.isClientSide) {
                      // Respect triggerTimeSeconds from the custom fluid definition.
                      // Track how long the player has been in this bath and only apply
                      // effects after the configured trigger delay (matching HotBath behavior).
                      CustomFluidDefinition def = defOpt.get();
                      int triggerTimeTicks = def.triggerTimeSeconds() * 20;

                      CompoundTag playerData = serverPlayer.getPersistentData();
                      long currentTick = level.getGameTime();
                      long lastInsideTick = playerData.getLong("showercore.custom_bath_last_tick");

                      int stayedTicks;
                      if (currentTick - lastInsideTick > 5) {
                          // Re-entry or first entry — reset timer
                          stayedTicks = 1;
                      } else {
                          stayedTicks = playerData.getInt("showercore.custom_bath_stayed") + 1;
                      }

                      playerData.putLong("showercore.custom_bath_last_tick", currentTick);
                      playerData.putInt("showercore.custom_bath_stayed", stayedTicks);

                      if (stayedTicks >= triggerTimeTicks) {
                          CustomFluidAPI.applyFluidEffects(serverPlayer, fluidId);
                      }
                  }
              } else {
                  // No custom fluid definition; handle special cases like lava
                  FluidStack fluidStack = bathtubBe.getFluidTank().getFluid();
                  if (!fluidStack.isEmpty()) {
                      Fluid fluid = fluidStack.getFluid();
                      if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
                          entity.setRemainingFireTicks(300);
                          entity.hurt(level.damageSources().lava(), 4.0F);
                      }
                  }
              }
          }
      }

      // Apply the 6 vanilla bath effects to entities standing in the bathtub. Previously
      // these only fired when a player was sitting on the SeatEntity, so simply standing
      // in a bath gave no buff. Frequency-gated to once per second to match the seated
      // path's cadence and avoid effect-icon flicker.
      if (!level.isClientSide
              && entity instanceof LivingEntity living
              && entity.tickCount % 20 == 0) {
          SeatEntity.applyBathEffects(living, liquid, level, pos);
      }

      // === Hot bathtub interactions (server-side only) ===
      if (!level.isClientSide && liquid != LiquidType.EMPTY && liquid != LiquidType.WATER) {
          boolean isHot = (liquid != LiquidType.CUSTOM) ||
                  mod.crabmod.showercore.utils.CoreUtils.isCustomFluidHotAt(level, pos);

          if (isHot) {
              // Dirtiness cleaning is handled by DirtinessHandlerMixin injecting into
              // hotBath's isInHotBathFluid() - no direct call needed here to avoid double cleaning.

              // Twilight Forest ice mobs take damage in hot bathtub (1 damage/sec)
              if (entity.tickCount % 20 == 0
                      && mod.crabmod.showercore.compat.ShowerCoreCompat.isTwilightForestIceMob(entity)) {
                  entity.hurt(level.damageSources().magic(), 1.0F);
              }

              // Alex's Caves GummyBear melts in hot bathtub (0.5 damage/sec)
              if (entity.tickCount % 20 == 0
                      && mod.crabmod.showercore.compat.ShowerCoreCompat.isGummyBear(entity)) {
                  entity.hurt(level.damageSources().magic(), 0.5F);
              }
          }
      }

      super.entityInside(state, level, pos, entity);
  }

  @Override
  public InteractionResult useFaucet(Level level, BlockPos pos, BlockState state, Player player) {
      boolean running = state.getValue(RUNNING);
      boolean next = !running && state.getValue(LIQUID) != LiquidType.EMPTY;
      level.setBlock(pos, state.setValue(RUNNING, next), 3);
      level.playSound(null, pos, next ? SoundEvents.WATER_AMBIENT : SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
      return InteractionResult.CONSUME;
  }
  @Override
  public mod.crabmod.showercore.geometry.BathMountSurface.Support showerSupport(Level level, BlockPos clicked, BlockHitResult hit, mod.crabmod.showercore.geometry.BathroomGeometry.Shower shower) {
      var state=level.getBlockState(clicked);var facing=state.getValue(FACING);
      var head=state.getValue(PART)==BedPart.HEAD?clicked:clicked.relative(facing);var foot=head.relative(facing.getOpposite());
      if(!level.getBlockState(head).is(this)||!level.getBlockState(foot).is(this)
          ||level.getBlockState(head).getValue(PART)!=BedPart.HEAD||level.getBlockState(foot).getValue(PART)!=BedPart.FOOT
          ||level.getBlockState(head).getValue(FACING)!=facing||level.getBlockState(foot).getValue(FACING)!=facing)return null;
      var into=facing.getOpposite();var ref=mod.crabmod.showercore.geometry.BathMountSurface.rotate(.5,1,into);
      return new mod.crabmod.showercore.geometry.BathMountSurface.Support(new net.minecraft.world.phys.Vec3(head.getX()+ref.x,head.getY()+mountGeometry.rimY()/16.0,head.getZ()+ref.z),into);
  }

}
