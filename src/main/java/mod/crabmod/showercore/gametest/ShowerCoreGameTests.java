package mod.crabmod.showercore.gametest;

import com.mojang.authlib.GameProfile;
import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.BathtubWaterGeometry;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.entity.FaucetInteractionEntity;
import mod.crabmod.showercore.entity.RubberDuckEntity;
import mod.crabmod.showercore.item.BathtubBlockItem;
import mod.crabmod.showercore.registers.BlockEntitiesRegister;
import mod.crabmod.showercore.registers.BlocksRegister;
import mod.crabmod.showercore.registers.ItemRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder(ShowerCore.MODID)
@PrefixGameTestTemplate(false)
public final class ShowerCoreGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final BlockPos BATHTUB_FOOT_POS = new BlockPos(2, 1, 2);
    private static final String GOLD_BLOCK_ID = "minecraft:gold_block";

    private ShowerCoreGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 40)
    public static void clawfoot_bathtub_places_three_head_bound_faucet_targets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction facing = Direction.NORTH;
        BlockPos footPos = helper.absolutePos(BATHTUB_FOOT_POS);
        BlockPos headPos = footPos.relative(facing);
        BlockState footState = BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get().defaultBlockState()
                .setValue(BathtubBlock.FACING, facing)
                .setValue(BathtubBlock.PART, BedPart.FOOT)
                .setValue(BathtubBlock.LIQUID, BathtubBlock.LiquidType.WATER)
                .setValue(BathtubBlock.RUNNING, false);

        level.setBlock(footPos, footState, 3);
        ((BathtubBlock) footState.getBlock()).setPlacedBy(level, footPos, footState, null, ItemStack.EMPTY);

        AABB searchBox = new AABB(headPos).inflate(1.0D).expandTowards(0.0D, 1.0D, 0.0D);
        List<FaucetInteractionEntity> faucets = level.getEntitiesOfClass(FaucetInteractionEntity.class, searchBox);
        assertEquals(3, faucets.size(), "clawfoot faucet target count");
        for (FaucetInteractionEntity faucet : faucets) {
            assertEquals(headPos, faucet.blockPosition(), "clawfoot faucet target block position");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 80)
    public static void standard_bathtub_water_survives_client_sync_for_renderer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos footPos = helper.absolutePos(BATHTUB_FOOT_POS);
        BlockPos headPos = placeBathtub(level, footPos, BlocksRegister.BATHTUB_GREEN.get(), ItemStack.EMPTY);

        Player player = survivalPlayer(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.useBlock(BATHTUB_FOOT_POS, player, hitTop(footPos));

        assertWaterRenderStateRoundTrip(level, footPos, "standard bathtub foot");
        assertWaterRenderStateRoundTrip(level, headPos, "standard bathtub head");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 80)
    public static void clawfoot_bathtub_fills_with_water_bucket_and_places_edge_duck_inside_basin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos footPos = helper.absolutePos(BATHTUB_FOOT_POS);
        BlockPos headPos = placeBathtub(level, footPos, BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(), ItemStack.EMPTY);

        Player player = survivalPlayer(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.useBlock(BATHTUB_FOOT_POS, player, hitTop(footPos));

        BathtubBlockEntity foot = bathtubAt(level, footPos, "filled clawfoot foot");
        BathtubBlockEntity head = bathtubAt(level, headPos, "filled clawfoot head");
        assertEquals(1000, foot.getFluidTank().getFluidAmount(), "foot water amount");
        assertEquals(1000, head.getFluidTank().getFluidAmount(), "head water amount");
        assertEquals(Fluids.WATER, foot.getFluidTank().getFluid().getFluid(), "foot fluid");
        assertEquals(BathtubBlock.LiquidType.WATER, level.getBlockState(footPos).getValue(BathtubBlock.LIQUID),
                "foot liquid blockstate");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegister.RUBBER_DUCK.get()));
        helper.useBlock(BATHTUB_FOOT_POS, player, hitAt(footPos, Direction.UP, 0.05D, 1.0D, 0.95D));

        List<RubberDuckEntity> ducks = level.getEntitiesOfClass(RubberDuckEntity.class, new AABB(footPos).inflate(1.0D));
        assertEquals(1, ducks.size(), "rubber duck count");
        RubberDuckEntity duck = ducks.get(0);
        assertNear(footPos.getX() + BathtubWaterGeometry.DUCK_SAFE_LOCAL_MIN,
                duck.getX(), 0.001D, "rubber duck clamped x");
        assertNear(footPos.getY() + BathtubBlock.duckFloatSurfaceFor(level.getBlockState(footPos)),
                duck.getY(), 0.001D, "rubber duck surface y");
        assertNear(footPos.getZ() + BathtubWaterGeometry.DUCK_SAFE_LOCAL_MAX,
                duck.getZ(), 0.001D, "rubber duck clamped z");
        assertDuckInsideBasin(footPos, duck);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 40)
    public static void placed_bathtub_does_not_accept_material_change(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos footPos = helper.absolutePos(BATHTUB_FOOT_POS);
        BlockPos headPos = placeBathtub(level, footPos, BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(), ItemStack.EMPTY);
        Player player = survivalPlayer(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.GOLD_BLOCK, 6));

        helper.useBlock(BATHTUB_FOOT_POS, player, hitTop(footPos));

        assertEquals(null, bathtubAt(level, footPos, "placed foot").getMaterialBlockId(), "placed foot material");
        assertEquals(null, bathtubAt(level, headPos, "placed head").getMaterialBlockId(), "placed head material");
        assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() > 0,
                "using a material block on a placed bathtub must not consume the six-block skinning cost");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 40)
    public static void skinned_bathtub_places_and_drops_with_material_data(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos footPos = helper.absolutePos(BATHTUB_FOOT_POS);
        ItemStack stack = bathtubStack(BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(), GOLD_BLOCK_ID);
        BlockPos headPos = placeBathtub(level, footPos, BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(), stack);

        assertEquals(GOLD_BLOCK_ID, bathtubAt(level, footPos, "skinned foot").getMaterialBlockId().toString(),
                "placed foot material");
        assertEquals(GOLD_BLOCK_ID, bathtubAt(level, headPos, "skinned head").getMaterialBlockId().toString(),
                "placed head material");

        Player player = survivalPlayer(level);
        LootParams.Builder params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(footPos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(footPos));
        List<ItemStack> drops = level.getBlockState(footPos).getDrops(params);

        assertEquals(1, drops.size(), "skinned bathtub drop count");
        assertMaterial(drops.get(0), GOLD_BLOCK_ID, "skinned bathtub drop material");
        helper.succeed();
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new GameTestAssertException(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private static void assertNear(double expected, double actual, double epsilon, String message) {
        if (Math.abs(expected - actual) > epsilon) {
            throw new GameTestAssertException(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static BlockPos placeBathtub(ServerLevel level, BlockPos footPos, Block block, ItemStack stack) {
        Direction facing = Direction.NORTH;
        BlockState footState = block.defaultBlockState()
                .setValue(BathtubBlock.FACING, facing)
                .setValue(BathtubBlock.PART, BedPart.FOOT)
                .setValue(BathtubBlock.LIQUID, BathtubBlock.LiquidType.EMPTY)
                .setValue(BathtubBlock.RUNNING, false);
        level.setBlock(footPos, footState, 3);
        ((BathtubBlock) block).setPlacedBy(level, footPos, footState, null, stack);
        return footPos.relative(facing);
    }

    private static BathtubBlockEntity bathtubAt(ServerLevel level, BlockPos pos, String message) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BathtubBlockEntity bathtub)) {
            throw new GameTestAssertException(message + ": expected bathtub block entity at " + pos);
        }
        return bathtub;
    }

    private static void assertWaterRenderStateRoundTrip(ServerLevel level, BlockPos pos, String message) {
        BlockState state = level.getBlockState(pos);
        BathtubBlockEntity serverBathtub = bathtubAt(level, pos, message);
        assertEquals(BathtubBlock.LiquidType.WATER, state.getValue(BathtubBlock.LIQUID),
                message + " liquid blockstate");
        assertEquals(1000, serverBathtub.getFluidTank().getFluidAmount(), message + " server water amount");
        assertEquals(Fluids.WATER, serverBathtub.getFluidTank().getFluid().getFluid(), message + " server fluid");

        BathtubBlockEntity clientBathtub = new BathtubBlockEntity(pos, state);
        clientBathtub.handleUpdateTag(serverBathtub.getUpdateTag());
        assertEquals(1000, clientBathtub.getFluidTank().getFluidAmount(), message + " synced water amount");
        assertEquals(Fluids.WATER, clientBathtub.getFluidTank().getFluid().getFluid(), message + " synced fluid");
    }

    private static BlockHitResult hitTop(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static BlockHitResult hitAt(BlockPos pos, Direction direction, double localX, double localY, double localZ) {
        return new BlockHitResult(
                new Vec3(pos.getX() + localX, pos.getY() + localY, pos.getZ() + localZ),
                direction, pos, false);
    }

    private static void assertDuckInsideBasin(BlockPos pos, RubberDuckEntity duck) {
        AABB box = duck.getBoundingBox();
        double minX = pos.getX() + 2.0D / 16.0D;
        double maxX = pos.getX() + 14.0D / 16.0D;
        double minZ = pos.getZ() + 2.0D / 16.0D;
        double maxZ = pos.getZ() + 14.0D / 16.0D;
        assertTrue(box.minX >= minX - 0.001D, "rubber duck min x stays inside bathtub basin");
        assertTrue(box.maxX <= maxX + 0.001D, "rubber duck max x stays inside bathtub basin");
        assertTrue(box.minZ >= minZ - 0.001D, "rubber duck min z stays inside bathtub basin");
        assertTrue(box.maxZ <= maxZ + 0.001D, "rubber duck max z stays inside bathtub basin");
    }

    private static ItemStack bathtubStack(Block block, String materialBlockId) {
        ItemStack stack = new ItemStack(block);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString(BathtubBlockEntity.TAG_MATERIAL_BLOCK_ID, materialBlockId);
        BlockItem.setBlockEntityData(stack, BlockEntitiesRegister.BATHTUB_BLOCK_ENTITY.get(), tag);
        return stack;
    }

    private static Player survivalPlayer(ServerLevel level) {
        return new Player(level, BlockPos.ZERO, 0.0F,
                new GameProfile(UUID.randomUUID(), "showercore-gametest-survival")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
    }

    private static void assertMaterial(ItemStack stack, String expectedMaterialBlockId, String message) {
        net.minecraft.resources.ResourceLocation materialBlockId = BathtubBlockItem.getMaterialBlockId(stack);
        assertEquals(expectedMaterialBlockId, materialBlockId == null ? null : materialBlockId.toString(), message);
    }
}
