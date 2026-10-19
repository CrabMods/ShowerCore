package mod.crabmod.showercore.gametest;

import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.item.BathtubBlockItem;
import mod.crabmod.showercore.material.BathtubMaterialFace;
import mod.crabmod.showercore.menu.BathtubMaterialMenu;
import mod.crabmod.showercore.registers.BlocksRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ShowerCore.MODID)
@PrefixGameTestTemplate(false)
public final class BathtubMaterialMenuGameTests {
    private static final ResourceLocation OAK = new ResourceLocation("minecraft:oak_log");

    private static Player player(GameTestHelper h) {
        return h.makeMockSurvivalPlayer();
    }

    private static BathtubMaterialMenu menu(Player p, int tubs, int blocks) {
        var m = new BathtubMaterialMenu(1, p.getInventory());
        m.getSlot(0).set(new ItemStack(BlocksRegister.BATHTUB_WHITE.get(), tubs));
        m.getSlot(1).set(new ItemStack(Blocks.OAK_LOG, blocks));
        return m;
    }

    private static void check(GameTestHelper h, boolean condition, String message) {
        h.assertTrue(condition, message);
    }

    @GameTest(template = "empty")
    public static void preview_does_not_consume_and_preserves_item_data(GameTestHelper h) {
        Player p = player(h);
        var m = menu(p, 3, 12);
        var name = net.minecraft.network.chat.Component.literal("My bath");
        m.getSlot(0).getItem().setHoverName(name);
        m.slotsChanged(m.getSlot(0).container);
        check(h, m.getSlot(0).getItem().getCount() == 3 && m.getSlot(1).getItem().getCount() == 12, "Preview consumed inputs");
        check(h, m.getSlot(2).getItem().getCount() == 1 && m.getSlot(2).getItem().getHoverName().equals(name), "Preview lost name or returned a stack");
        m.removed(p);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void native_pickup_charges_six_once_and_changes_one_tub(GameTestHelper h) {
        Player p = player(h); var m = menu(p, 3, 11);
        m.clicked(2, 0, ClickType.PICKUP, p);
        check(h, m.getCarried().getCount() == 1 && OAK.equals(BathtubBlockItem.getMaterialBlockId(m.getCarried())), "Native output pickup");
        check(h, m.getSlot(0).getItem().getCount() == 2 && m.getSlot(1).getItem().getCount() == 5, "Incorrect cost or bathtub count");
        m.clicked(2, 0, ClickType.PICKUP, p);
        check(h, m.getCarried().getCount() == 1 && m.getSlot(2).getItem().isEmpty(), "Repeated click duplicated output");
        m.removed(p); h.succeed();
    }

    @GameTest(template = "empty")
    public static void five_blocks_cannot_produce_output(GameTestHelper h) {
        Player p = player(h); var m = menu(p, 1, 5);
        m.clicked(2, 0, ClickType.PICKUP, p);
        check(h, m.getCarried().isEmpty() && m.getSlot(2).getItem().isEmpty(), "Insufficient material produced an item");
        check(h, m.getSlot(1).getItem().getCount() == 5 && m.getSlot(0).getItem().getCount() == 1, "Failure consumed inputs");
        m.removed(p); h.succeed();
    }

    @GameTest(template = "empty")
    public static void native_shift_click_moves_inputs_and_crafts_exact_batches(GameTestHelper h) {
        Player p = player(h); var m = new BathtubMaterialMenu(1, p.getInventory());
        p.getInventory().setItem(9, new ItemStack(BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get(), 3));
        p.getInventory().setItem(10, new ItemStack(Blocks.OAK_LOG, 13));
        m.clicked(3, 0, ClickType.QUICK_MOVE, p);
        m.clicked(4, 0, ClickType.QUICK_MOVE, p);
        m.clicked(2, 0, ClickType.QUICK_MOVE, p);
        check(h, m.getSlot(0).getItem().getCount() == 1 && m.getSlot(1).getItem().getCount() == 1, "Shift crafting did not charge six per output");
        check(h, p.getInventory().countItem(BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get().asItem()) == 2, "Wrong shift-crafted count");
        m.removed(p); h.succeed();
    }

    @GameTest(template = "empty")
    public static void full_inventory_cannot_charge_for_failed_shift_output(GameTestHelper h) {
        Player p = player(h); var m = menu(p, 1, 6);
        for (int i=0; i<36; i++) p.getInventory().setItem(i, new ItemStack(Blocks.STONE, 64));
        m.clicked(2, 0, ClickType.QUICK_MOVE, p);
        check(h, m.getSlot(0).getItem().getCount() == 1 && m.getSlot(1).getItem().getCount() == 6, "Failed transfer charged inputs");
        check(h, m.getSlot(2).hasItem(), "Failed transfer erased preview");
        p.getInventory().clearContent(); m.removed(p); h.succeed();
    }

    @GameTest(template = "empty")
    public static void closing_invalidates_preview_and_rejects_further_clicks(GameTestHelper h) {
        Player p = player(h); var m = menu(p, 2, 7);
        m.removed(p);
        // Vanilla returns inputs only for ServerPlayer; the client lane checks real
        // close-container packets and the authoritative inventory counts.
        check(h, !m.getSlot(2).hasItem(), "Close retained a craftable preview");
        check(h, !m.stillValid(p) && m.quickMoveStack(p, 2).isEmpty(), "Closed menu remains usable");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void same_material_face_switch_needs_only_sample_and_no_charge(GameTestHelper h) {
        Player p = player(h); var m = menu(p, 1, 1);
        BathtubBlockItem.setMaterial(m.getSlot(0).getItem(), OAK, BathtubMaterialFace.SIDE);
        m.clickMenuButton(p, 1);
        m.clicked(2, 0, ClickType.PICKUP, p);
        check(h, BathtubBlockItem.getMaterialFace(m.getCarried()) == BathtubMaterialFace.TOP, "Face selection not applied");
        check(h, m.getSlot(1).getItem().getCount() == 1, "Same-material face change charged source blocks");
        m.removed(p); h.succeed();
    }

    @GameTest(template = "empty")
    public static void same_material_same_face_has_no_output(GameTestHelper h) {
        Player p = player(h); var m = menu(p, 1, 6);
        BathtubBlockItem.setMaterial(m.getSlot(0).getItem(), OAK, BathtubMaterialFace.SIDE);
        m.slotsChanged(m.getSlot(0).container);
        check(h, m.getSlot(2).getItem().isEmpty(), "No-op produced output");
        m.removed(p); h.succeed();
    }

    @GameTest(template = "empty")
    public static void invalid_source_items_are_rejected(GameTestHelper h) {
        Player p = player(h); var m = menu(p, 1, 6);
        check(h, !m.getSlot(1).mayPlace(new ItemStack(Items.WATER_BUCKET)), "Bucket accepted as block material");
        check(h, !m.getSlot(1).mayPlace(new ItemStack(BlocksRegister.BATHTUB_WHITE.get())), "Bathtub accepted as material");
        check(h, !m.getSlot(1).mayPlace(new ItemStack(Blocks.CHEST)), "Special-renderer block accepted");
        check(h, !m.getSlot(2).mayPlace(new ItemStack(Blocks.STONE)), "Result accepts input");
        check(h, !m.clickMenuButton(p, -1) && !m.clickMenuButton(p, 2), "Invalid menu button accepted");
        m.removed(p); h.succeed();
    }

    @GameTest(template = "empty")
    public static void removing_input_invalidates_preview_before_next_click(GameTestHelper h) {
        Player p = player(h); var m = menu(p, 1, 6);
        m.getSlot(1).set(ItemStack.EMPTY);
        m.clicked(2, 0, ClickType.PICKUP, p);
        check(h, m.getCarried().isEmpty() && !m.getSlot(2).hasItem(), "Stale preview duplicated an item");
        m.removed(p); h.succeed();
    }

    @GameTest(template = "empty")
    public static void creative_uses_sample_without_charging_material(GameTestHelper h) {
        Player p = h.makeMockPlayer(); var m = menu(p, 1, 1);
        m.clicked(2, 0, ClickType.PICKUP, p);
        check(h, m.getCarried().getCount() == 1 && m.getSlot(1).getItem().getCount() == 1, "Creative sample charged");
        check(h, !m.getSlot(0).hasItem(), "Creative input bathtub was duplicated");
        m.removed(p); h.succeed();
    }

    @GameTest(template = "empty")
    public static void legacy_material_without_face_remains_legacy(GameTestHelper h) {
        var block = BlocksRegister.BATHTUB_GREEN.get();
        var be = new BathtubBlockEntity(BlockPos.ZERO, block.defaultBlockState());
        be.setMaterialBlockId(OAK);
        var tag = be.saveWithoutMetadata();
        check(h, !tag.contains(BathtubMaterialFace.TAG), "Legacy save gained a new face");
        var restored = new BathtubBlockEntity(BlockPos.ZERO, block.defaultBlockState());
        restored.load(tag);
        check(h, restored.getMaterialFace() == BathtubMaterialFace.LEGACY && OAK.equals(restored.getMaterialBlockId()), "Legacy material appearance not preserved");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void material_face_places_on_both_halves_and_survives_pick_and_reload(GameTestHelper h) {
        var level = h.getLevel(); var foot = h.absolutePos(new BlockPos(2, 1, 2));
        var block = BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get();
        var stack = new ItemStack(block);
        BathtubBlockItem.setMaterial(stack, OAK, BathtubMaterialFace.TOP);
        var state = block.defaultBlockState().setValue(BathtubBlock.FACING, Direction.NORTH);
        level.setBlock(foot, state, 3);
        ((BathtubBlock)block).setPlacedBy(level, foot, state, null, stack);
        for (var pos : java.util.List.of(foot, foot.north())) {
            var be = (BathtubBlockEntity)level.getBlockEntity(pos);
            check(h, OAK.equals(be.getMaterialBlockId()) && be.getMaterialFace() == BathtubMaterialFace.TOP, "Half lost material face");
            var saved = be.saveWithoutMetadata();
            var restored = new BathtubBlockEntity(pos, level.getBlockState(pos));
            restored.load(saved);
            check(h, restored.getMaterialFace() == BathtubMaterialFace.TOP, "Reload lost face");
            var picked = block.getCloneItemStack(level, pos, level.getBlockState(pos));
            check(h, BathtubBlockItem.getMaterialFace(picked) == BathtubMaterialFace.TOP, "Pick block lost face");
        }
        h.succeed();
    }
}
