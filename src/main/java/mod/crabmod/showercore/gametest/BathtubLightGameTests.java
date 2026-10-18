package mod.crabmod.showercore.gametest;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.entity.FaucetInteractionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Observes natural block-light updates; never asks the light engine to relight. */
@GameTestHolder("showercore")
@PrefixGameTestTemplate(false)
public final class BathtubLightGameTests {
    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_legacy_blazing_bath(GameTestHelper h) { check(h, false, "blazing_bath"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_legacy_enchanted_bath(GameTestHelper h) { check(h, false, "enchanted_bath"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_legacy_ender_bath(GameTestHelper h) { check(h, false, "ender_bath"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_legacy_golden_bath(GameTestHelper h) { check(h, false, "golden_bath"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_legacy_milk_tea(GameTestHelper h) { check(h, false, "milk_tea"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_legacy_silent_spring(GameTestHelper h) { check(h, false, "silent_spring"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_legacy_slime_bath(GameTestHelper h) { check(h, false, "slime_bath"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_clawfoot_blazing_bath(GameTestHelper h) { check(h, true, "blazing_bath"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_clawfoot_enchanted_bath(GameTestHelper h) { check(h, true, "enchanted_bath"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_clawfoot_ender_bath(GameTestHelper h) { check(h, true, "ender_bath"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_clawfoot_golden_bath(GameTestHelper h) { check(h, true, "golden_bath"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_clawfoot_milk_tea(GameTestHelper h) { check(h, true, "milk_tea"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_clawfoot_silent_spring(GameTestHelper h) { check(h, true, "silent_spring"); }

    @net.minecraft.gametest.framework.GameTest(template="light_empty", timeoutTicks=160)
    public static void light_clawfoot_slime_bath(GameTestHelper h) { check(h, true, "slime_bath"); }

    private static void check(GameTestHelper h, boolean clawfoot, String fluid) {
        var level = h.getLevel();
        // The reserved template and sealed stone walls exclude neighboring test markers.
        BlockPos foot = h.absolutePos(new BlockPos(5, 1, 6));
        BlockPos head = foot.north();
        BlockPos torch = foot.east(2);
        var player = h.makeMockSurvivalPlayer();
        var baseline = new LinkedHashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        BlockPos.betweenClosedStream(foot.offset(-4, -1, -5), foot.offset(4, 5, 4)).forEach(p -> {
            baseline.put(p.immutable(), level.getBlockState(p));
        });
        Runnable cleanup = () -> {
            for (var entity : level.getEntitiesOfClass(FaucetInteractionEntity.class, new AABB(head).inflate(1))) entity.discard();
            for (var entry : baseline.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), 3);
            for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new AABB(Vec3.atLowerCornerOf(foot.offset(-4, -1, -5)),
                            Vec3.atLowerCornerOf(foot.offset(5, 6, 5))))) entity.discard();
        };
        java.util.function.Consumer<Runnable> guarded = action -> {
            try { action.run(); } catch (Throwable error) { cleanup.run(); throw error; }
        };
        guarded.accept(() -> {
            for (var p : baseline.keySet()) {
                boolean wall = p.getX() == foot.getX()-4 || p.getX() == foot.getX()+4
                        || p.getZ() == foot.getZ()-5 || p.getZ() == foot.getZ()+4
                        || p.getY() == foot.getY()-1 || p.getY() == foot.getY()+5;
                level.setBlock(p, wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            }
            Block tub = clawfoot ? mod.crabmod.showercore.registers.BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get()
                    : mod.crabmod.showercore.registers.BlocksRegister.BATHTUB_GREEN.get();
            var state = tub.defaultBlockState().setValue(BathtubBlock.FACING, Direction.NORTH)
                    .setValue(BathtubBlock.PART, BedPart.FOOT).setValue(BathtubBlock.LIQUID, BathtubBlock.LiquidType.EMPTY);
            level.setBlock(foot, state, 3);
            ((BathtubBlock)tub).setPlacedBy(level, foot, state, player, ItemStack.EMPTY);
            var definition = CustomFluidAPI.getFluidDefinition(BathtubFluidTestSupport.id("hotbath:" + fluid)).orElseThrow();
            player.setItemInHand(InteractionHand.MAIN_HAND, CustomFluidAPI.createBucket(definition));
            h.useBlock(foot.subtract(h.absolutePos(BlockPos.ZERO)), player,
                    new BlockHitResult(Vec3.atCenterOf(foot), Direction.UP, foot, false));
        });
        Runnable assertLight = () -> {
            int expected = CustomFluidAPI.getFluidDefinition(BathtubFluidTestSupport.id("hotbath:" + fluid)).orElseThrow().luminosity();
            for (var pos : List.of(foot, head)) {
                var be = (BathtubBlockEntity)level.getBlockEntity(pos);
                h.assertTrue(be != null && be.getFluidTank().getFluidAmount() == 1000
                        && BathtubFluidTestSupport.id("hotbath:" + fluid).equals(be.getCustomFluidId()), "Both halves retain the poured fluid");
                h.assertTrue(level.getBlockState(pos).getLightEmission(level, pos) == expected, "Contextual emission must match the original fluid definition");
                h.assertTrue(level.getBrightness(LightLayer.BLOCK, pos) == expected, "Naturally propagated source light");
                h.assertTrue(level.getBrightness(LightLayer.BLOCK, pos.above()) == Math.max(0, expected-1), "Natural adjacent block light");
                h.assertTrue(level.getBrightness(LightLayer.SKY, pos.above()) == 0, "Dark room excludes skylight");
            }
        };
        h.runAtTickTime(30, () -> guarded.accept(() -> {
            assertLight.run();
            level.setBlock(torch, Blocks.TORCH.defaultBlockState(), 3);
        }));
        h.runAtTickTime(50, () -> guarded.accept(() -> {
            h.assertTrue(level.getBrightness(LightLayer.BLOCK, torch) == 14, "Neighbor torch must actually illuminate the room");
            level.setBlock(torch, Blocks.AIR.defaultBlockState(), 3);
        }));
        h.runAtTickTime(80, () -> guarded.accept(() -> {
            assertLight.run();
            // Older saves decode the newly introduced light property as zero.
            for (var pos : List.of(foot, head)) {
                level.setBlock(pos, level.getBlockState(pos).setValue(BathtubBlock.LIGHT_LEVEL, 0), 3);
                ((BathtubBlockEntity)level.getBlockEntity(pos)).onLoad();
            }
        }));
        h.runAtTickTime(110, () -> guarded.accept(() -> {
            assertLight.run();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.BUCKET));
            h.useBlock(foot.subtract(h.absolutePos(BlockPos.ZERO)), player,
                    new BlockHitResult(Vec3.atCenterOf(foot), Direction.UP, foot, false));
            h.assertTrue(CustomFluidAPI.getFluidFromItem(player.getMainHandItem()).orElseThrow().id()
                    .equals(BathtubFluidTestSupport.id("hotbath:" + fluid)), "Native extraction retains the custom fluid identity");
        }));
        h.runAtTickTime(140, () -> guarded.accept(() -> {
            for (var pos : List.of(foot, head)) {
                var be = (BathtubBlockEntity)level.getBlockEntity(pos);
                h.assertTrue(be.getFluidTank().isEmpty() && be.getCustomFluidId() == null, "Extraction empties both halves");
                h.assertTrue(level.getBlockState(pos).getLightEmission(level, pos) == 0
                        && level.getBrightness(LightLayer.BLOCK, pos) == 0
                        && level.getBrightness(LightLayer.BLOCK, pos.above()) == 0, "Empty bathtub stops emitting light naturally");
            }
            cleanup.run();
            h.succeed();
        }));
    }
}
