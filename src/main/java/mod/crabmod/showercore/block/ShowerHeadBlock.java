// ShowerHeadBlock.java
package mod.crabmod.showercore.block;

import java.util.Collections;
import mod.crabmod.showercore.geometry.BathroomGeometry;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import java.util.List;
import javax.annotation.Nullable;
import mod.crabmod.showercore.base.RotatableBlock;
import mod.crabmod.showercore.entity.ShowerHeadContainerEntity;
import mod.crabmod.showercore.utils.CoreUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import mod.crabmod.showercore.registers.BlockEntitiesRegister;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;

public class ShowerHeadBlock extends RotatableBlock implements EntityBlock, SimpleWaterloggedBlock {
  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

  public static final IntegerProperty MOUNT_OFFSET =
      IntegerProperty.create("mount_offset", 0, 30);
  private final BathroomGeometry.Shower geometry;
  private final java.util.Map<Direction, VoxelShape> outlines;

  public ShowerHeadBlock(Properties properties) {
    this(properties, BathroomGeometry.RAIN_SHOWER);
  }

  public ShowerHeadBlock(Properties properties, BathroomGeometry.Shower geometry) {
    super(properties);
    this.geometry = geometry;
    var bounds = geometry.bounds();
    this.outlines = createShapeMap(box(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()));
    this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(MOUNT_OFFSET, 15));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(WATERLOGGED, MOUNT_OFFSET);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
    return super.getStateForPlacement(context).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
  }

  @Override
  public FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
    if (state.getValue(WATERLOGGED)) {
      level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
    }
    return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
  }

  @javax.annotation.Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    return type == BlockEntitiesRegister.RAIN_SHOWER_HEAD_CONTAINER.get() ? (lvl, pos, st, be) -> ShowerHeadContainerEntity.tick(lvl, pos, st, (ShowerHeadContainerEntity) be) : null;
  }

  @Override
  protected VoxelShape getBaseShape() {
    return Shapes.or(box(2.75, -16, 5.5, 13, 15, 16));
  }

  public BathroomGeometry.Shower geometry() { return geometry; }

  // Native integer properties are unsigned. Keep the wire encoding behind these accessors.
  public static int mountOffsetPixels(BlockState state) { return state.getValue(MOUNT_OFFSET) - 15; }

  public static BlockState withMountOffset(BlockState state, int pixels) { return state.setValue(MOUNT_OFFSET, pixels + 15); }

  public static double mountOffset(BlockState state) { return mountOffsetPixels(state) / 16.0; }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    float x=0,z=0;
    if(level.getBlockEntity(pos) instanceof ShowerHeadContainerEntity shower){x=shower.horizontalMount().x();z=shower.horizontalMount().z();}
    return outlines.get(state.getValue(FACING)).move(x, mountOffset(state), z);
  }

  public Vec3 sprayOrigin(BlockPos pos, BlockState state) {
    double x = geometry.outletX() / 16.0;
    double z = geometry.outletZ() / 16.0;
    var local = switch (state.getValue(FACING)) {
      case EAST -> new Vec3(1-z, 0, x);
      case SOUTH -> new Vec3(1-x, 0, 1-z);
      case WEST -> new Vec3(z, 0, 1-x);
      default -> new Vec3(x, 0, z);
    };
    return local.add(pos.getX(), pos.getY() + mountOffset(state) + geometry.outletY() / 16.0, pos.getZ());
  }

  public Vec3 sprayOrigin(BlockGetter level, BlockPos pos, BlockState state) {
    var origin=sprayOrigin(pos,state);
    return level.getBlockEntity(pos) instanceof ShowerHeadContainerEntity shower?origin.add(shower.horizontalMount().x(),0,shower.horizontalMount().z()):origin;
  }

  public double sprayWidth(BlockState state) {
    return (state.getValue(FACING).getAxis() == Direction.Axis.X ? geometry.depth() : geometry.width()) / 16.0;
  }

  public double sprayDepth(BlockState state) {
    return (state.getValue(FACING).getAxis() == Direction.Axis.X ? geometry.width() : geometry.depth()) / 16.0;
  }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.empty();
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new ShowerHeadContainerEntity(pos, state);
  }

  @Override
  public InteractionResult use(
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hit) {

    net.minecraft.world.item.ItemStack heldItem = player.getItemInHand(hand);

    if (heldItem.getItem() == net.minecraft.world.item.Items.WATER_BUCKET) {
       if (!state.getValue(WATERLOGGED)) {
          if (!level.isClientSide) {
             if (!player.isCreative()) {
                player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
             }
             level.setBlock(pos, state.setValue(WATERLOGGED, true), 3);
             level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
             level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
          }
          return InteractionResult.sidedSuccess(level.isClientSide);
       }
    }

    BlockEntity blockEntity = level.getBlockEntity(pos);
    if (!(blockEntity instanceof ShowerHeadContainerEntity showerEntity)) {
      return InteractionResult.PASS;
    }

    if (CoreUtils.isCoreItem(heldItem)) {
      if (!level.isClientSide) {
        net.minecraft.world.item.ItemStack existingItem = showerEntity.getItem(0);
        if (!existingItem.isEmpty()) {
          net.minecraft.world.level.block.Block.popResource(level, pos, existingItem.copy());
        }

        net.minecraft.world.item.ItemStack copy = heldItem.copy();
        copy.setCount(1);
        showerEntity.setItem(0, copy);
        showerEntity.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        if (!player.isCreative()) {
          heldItem.shrink(1);
        }
        // Optional: Play a sound to indicate success
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
    }

    if (heldItem.isEmpty()) {
      // Shift + Empty Hand: Remove Core
      if (player.isShiftKeyDown()) {
        if (!showerEntity.isEmpty()) {
          if (!level.isClientSide) {
            net.minecraft.world.item.ItemStack existingItem = showerEntity.getItem(0);
            net.minecraft.world.level.block.Block.popResource(level, pos, existingItem.copy());
            showerEntity.setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
            
            if (showerEntity.isEffectActive()) {
              showerEntity.toggleEffect();
            }
            
            showerEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
          } else {
            if (showerEntity.isEffectActive()) {
              showerEntity.toggleEffect();
              showerEntity.stopEffect();
            }
          }
          return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
      }

      if (showerEntity.isEmpty() && !showerEntity.isEffectActive()) {
        if (level.isClientSide) {
          player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.showercore.need_core"), true);
        }
        return InteractionResult.SUCCESS;
      }

      showerEntity.toggleEffect();
      if (level.isClientSide) {
        if (showerEntity.isEffectActive()) {
          showerEntity.startEffect();
        } else {
          showerEntity.stopEffect();
        }
        return InteractionResult.SUCCESS;
      }
      
      showerEntity.setChanged();
      level.sendBlockUpdated(pos, state, state, 3);
      return InteractionResult.CONSUME;
    }

    return InteractionResult.PASS;
  }

  @Override
  public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
    if (!state.is(newState.getBlock())) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof ShowerHeadContainerEntity showerEntity) {
        if (level.isClientSide) {
          showerEntity.shutdownEffect();
        }
      }
      super.onRemove(state, level, pos, newState, isMoving);
    }
  }

  @Override
  public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
      ItemStack stack = super.getCloneItemStack(level, pos, state);
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof ShowerHeadContainerEntity showerEntity) {
          CompoundTag tag = showerEntity.saveWithoutMetadata();
          tag.remove("MountX");tag.remove("MountZ");
          BlockItem.setBlockEntityData(stack, blockEntity.getType(), tag);
      }
      return stack;
  }

  @Override
  public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
    BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
    if (blockEntity instanceof ShowerHeadContainerEntity showerEntity) {
      ItemStack stack = new ItemStack(this);
      CompoundTag tag = showerEntity.saveWithoutMetadata();
          tag.remove("MountX");tag.remove("MountZ");
      BlockItem.setBlockEntityData(stack, blockEntity.getType(), tag);
      return Collections.singletonList(stack);
    }
    return super.getDrops(state, params);
  }

  @Override
  public void appendHoverText(
      ItemStack stack,
      @Nullable BlockGetter level,
      List<Component> tooltip,
      TooltipFlag flag) {
    
    CompoundTag tag = BlockItem.getBlockEntityData(stack);
    if (tag != null) {
        if (tag.contains("Items")) {
            net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(1, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items);
            ItemStack core = items.get(0);
            if (!core.isEmpty()) {
                 tooltip.add(Component.translatable("tooltip.showercore.contains", core.getHoverName()).withStyle(ChatFormatting.GRAY));
            }
        }
    }
    
    tooltip.add(Component.translatable("tooltip.showercore.material.usage"));
    tooltip.add(Component.translatable("tooltip.showercore.shower_head.usage.install"));
    tooltip.add(Component.translatable("tooltip.showercore.shower_head.usage.toggle"));
    tooltip.add(Component.translatable("tooltip.showercore.shower_head.usage.remove"));
    super.appendHoverText(stack, level, tooltip, flag);
  }
}
