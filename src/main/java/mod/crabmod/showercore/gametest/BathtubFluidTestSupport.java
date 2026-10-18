package mod.crabmod.showercore.gametest;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.entity.FaucetInteractionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Native fixture shared by the fluid interaction and renderer-input regressions. */
final class BathtubFluidTestSupport {
    static final List<String> CUSTOM = List.of("blazing_bath", "enchanted_bath", "ender_bath",
            "golden_bath", "milk_tea", "silent_spring", "slime_bath");

    static ResourceLocation id(String value) { return new ResourceLocation(value); }

    static void check(GameTestHelper h, Block block, Direction facing, List<String> fluids, boolean toggle) {
        var level = h.getLevel();
        BlockPos foot = h.absolutePos(new BlockPos(2, 1, 2));
        BlockPos head = foot.relative(facing);
        var player = h.makeMockPlayer();
        for (String fluid : fluids) {
            var state = block.defaultBlockState().setValue(BathtubBlock.FACING, facing)
                    .setValue(BathtubBlock.PART, BedPart.FOOT).setValue(BathtubBlock.LIQUID, BathtubBlock.LiquidType.EMPTY)
                    .setValue(BathtubBlock.RUNNING, false);
            level.setBlock(foot, state, 3);
            ((BathtubBlock) block).setPlacedBy(level, foot, state, player, ItemStack.EMPTY);
            ItemStack bucket;
            if (fluid.equals("water")) bucket = new ItemStack(Items.WATER_BUCKET);
            else if (fluid.equals("hot_water")) bucket = new ItemStack(BuiltInRegistries.ITEM.get(id("hotbath:hot_water_bucket")));
            else bucket = CustomFluidAPI.createBucket(CustomFluidAPI.getFluidDefinition(id("hotbath:" + fluid)).orElseThrow());
            require(!bucket.isEmpty(), "Missing native bucket fixture: " + fluid);
            player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
            h.useBlock(foot.subtract(h.absolutePos(BlockPos.ZERO)), player,
                    new BlockHitResult(Vec3.atCenterOf(foot), Direction.UP, foot, false));
            for (BlockPos pos : List.of(foot, head)) {
                var be = (BathtubBlockEntity) level.getBlockEntity(pos);
                require(be != null && be.getFluidTank().getFluidAmount() == 1000, "Native bucket must fill both halves");
                var synced = new BathtubBlockEntity(pos, level.getBlockState(pos));
                synced.handleUpdateTag(be.getUpdateTag());
                require(synced.getFluidTank().getFluidAmount() == 1000, "Fluid amount must survive the client update tag");
                require(synced.getFluidTank().getFluid().getFluid() == be.getFluidTank().getFluid().getFluid(), "Fluid identity changed during sync");
                require(java.util.Objects.equals(be.getCustomFluidId(), synced.getCustomFluidId()), "Custom identity changed during sync");
                if (CUSTOM.contains(fluid)) require(id("hotbath:" + fluid).equals(be.getCustomFluidId()), "Custom bucket identity was lost");
            }
            var targets = level.getEntitiesOfClass(FaucetInteractionEntity.class, new AABB(head).inflate(.7));
            require(targets.size() == (BathtubBlock.isClawfootBathtub(state) ? 3 : 1), "Faucet target count");
            if (toggle) for (var target : targets) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                require(target.interact(player, InteractionHand.MAIN_HAND).consumesAction(), "Native faucet did not handle the click");
                require(level.getBlockState(head).getValue(BathtubBlock.RUNNING), "Native faucet did not start");
                target.interact(player, InteractionHand.MAIN_HAND);
                require(!level.getBlockState(head).getValue(BathtubBlock.RUNNING), "Native faucet did not stop");
            }
            for (var target : targets) target.discard();
            level.setBlock(foot, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(head, Blocks.AIR.defaultBlockState(), 3);
        }
        h.succeed();
    }

    private static void require(boolean value, String message) {
        if (!value) throw new GameTestAssertException(message);
    }
}
