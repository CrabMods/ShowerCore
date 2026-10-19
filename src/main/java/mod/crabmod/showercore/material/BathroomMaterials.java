package mod.crabmod.showercore.material;

import mod.crabmod.showercore.item.BathtubBlockItem;
import mod.crabmod.showercore.item.ShowerHeadBlockItem;
import mod.crabmod.showercore.item.RubberDuckItem;
import mod.crabmod.showercore.menu.BathtubMaterialMenu;
import mod.crabmod.showercore.registers.BlockEntitiesRegister;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** One portable material workflow; keep block data compatible with existing saves. */
public final class BathroomMaterials {
    public static final String MATERIAL_ID = "MaterialBlockId";
    private BathroomMaterials() {}

    public static boolean supports(ItemStack stack) {
        return stack.getItem() instanceof BathtubBlockItem || stack.getItem() instanceof ShowerHeadBlockItem
                || stack.getItem() instanceof RubberDuckItem;
    }

    public static int cost(ItemStack stack) { return stack.getItem() instanceof BathtubBlockItem ? 6 : 1; }

    private static CompoundTag data(ItemStack stack) {
        CustomData data = stack.get(stack.getItem() instanceof BlockItem
                ? DataComponents.BLOCK_ENTITY_DATA : DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    @Nullable
    public static ResourceLocation blockId(ItemStack stack) {
        String value = data(stack).getString(MATERIAL_ID);
        return value.isEmpty() ? null : ResourceLocation.tryParse(value);
    }

    public static BathtubMaterialFace face(ItemStack stack) {
        return BathtubMaterialFace.read(data(stack).getString(BathtubMaterialFace.TAG));
    }

    public static void set(ItemStack stack, ResourceLocation block, BathtubMaterialFace face) {
        if (!supports(stack)) throw new IllegalArgumentException("Item has no bathroom material surface");
        CompoundTag data = data(stack);
        data.putString(MATERIAL_ID, block.toString());
        data.putString(BathtubMaterialFace.TAG, face.serializedName());
        if (stack.getItem() instanceof BlockItem) {
            BlockItem.setBlockEntityData(stack, stack.getItem() instanceof BathtubBlockItem
                    ? BlockEntitiesRegister.BATHTUB_BLOCK_ENTITY.get()
                    : BlockEntitiesRegister.RAIN_SHOWER_HEAD_CONTAINER.get(), data);
        } else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static InteractionResultHolder<ItemStack> open(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> {
                var menu = new BathtubMaterialMenu(id, inventory);
                ItemStack held = player.getItemInHand(hand);
                player.setItemInHand(hand, ItemStack.EMPTY);
                menu.getSlot(0).set(held);
                return menu;
            }, Component.translatable("menu.showercore.material.title")));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
