package mod.crabmod.showercore.menu;

import mod.crabmod.showercore.material.BathroomMaterials;
import mod.crabmod.showercore.material.BathtubMaterialFace;
import mod.crabmod.showercore.registers.MenuRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;

/** A portable, server-authoritative two-input crafting menu. */
public class BathtubMaterialMenu extends AbstractContainerMenu {
    public static final int MATERIAL_COST = 6;
    public static final int RESULT_SLOT = 2;
    private static final int INVENTORY_START = 3;
    private static final int INVENTORY_END = 39;
    private final Player owner;
    private final DataSlot face = DataSlot.standalone();
    private boolean updating;
    private boolean closed;
    private final Container inputs = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final Container result = new SimpleContainer(1);

    public BathtubMaterialMenu(int id, Inventory inventory) {
        super(MenuRegister.BATHTUB_MATERIAL.get(), id);
        owner = inventory.player;
        addDataSlot(face);
        addSlot(new Slot(inputs, 0, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BathroomMaterials.supports(stack);
            }
        });
        addSlot(new Slot(inputs, 1, 76, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isMaterial(stack);
            }
        });
        addSlot(new Slot(result, 0, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !closed && !buildResult().isEmpty();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (!player.level().isClientSide) {
                    int cost = materialCost();
                    updating = true;
                    inputs.removeItem(0, 1);
                    inputs.removeItem(1, cost);
                    updating = false;
                    slotsChanged(inputs);
                }
                super.onTake(player, stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 108 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 166));
        }
    }

    public static boolean isMaterial(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item
                && !BathroomMaterials.supports(stack)
                && item.getBlock().defaultBlockState().getRenderShape() == RenderShape.MODEL;
    }

    public BathtubMaterialFace selectedFace() {
        return face.get() == 1 ? BathtubMaterialFace.TOP : BathtubMaterialFace.SIDE;
    }

    public int materialCost() {
        if (owner.isCreative()) return 0;
        ItemStack material = inputs.getItem(1);
        if (isMaterial(material) && BuiltInRegistries.BLOCK.getKey(((BlockItem) material.getItem()).getBlock())
                .equals(BathroomMaterials.blockId(inputs.getItem(0)))) return 0;
        return BathroomMaterials.cost(inputs.getItem(0));
    }

    private ItemStack buildResult() {
        if (closed) return ItemStack.EMPTY;
        ItemStack bathtub = inputs.getItem(0);
        ItemStack material = inputs.getItem(1);
        if (!BathroomMaterials.supports(bathtub) || !isMaterial(material)
                || material.getCount() < materialCost()) return ItemStack.EMPTY;
        var blockId = BuiltInRegistries.BLOCK.getKey(((BlockItem) material.getItem()).getBlock());
        if (blockId.equals(BathroomMaterials.blockId(bathtub))
                && selectedFace() == BathroomMaterials.face(bathtub)) return ItemStack.EMPTY;
        ItemStack output = bathtub.copy();
        output.setCount(1);
        BathroomMaterials.set(output, blockId, selectedFace());
        return output;
    }

    @Override
    public void slotsChanged(Container container) {
        if (!updating && !closed && !owner.level().isClientSide) {
            result.setItem(0, buildResult());
            broadcastChanges();
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (closed || player != owner || button < 0 || button > 1) return false;
        face.set(button);
        slotsChanged(inputs);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return !closed && player == owner && player.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (closed || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < INVENTORY_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) return ItemStack.EMPTY;
        } else if (BathroomMaterials.supports(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (isMaterial(stack)) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        closed = true;
        result.clearContent();
        if (!player.level().isClientSide) clearContainer(player, inputs);
    }
}
