package mod.crabmod.showercore.item;

import mod.crabmod.showercore.material.BathtubMaterialFace;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BathtubBlockItem extends BlockItem {

    public BathtubBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return mod.crabmod.showercore.material.BathroomMaterials.open(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation materialBlockId = getMaterialBlockId(stack);
        if (materialBlockId != null) {
            tooltip.add(Component.translatable("tooltip.showercore.bathtub.material", getMaterialName(materialBlockId))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.showercore.bathtub.usage.sit"));
        tooltip.add(Component.translatable("tooltip.showercore.bathtub.usage.toggle"));
        tooltip.add(Component.translatable("tooltip.showercore.bathtub.usage.fill"));
        tooltip.add(Component.translatable("tooltip.showercore.bathtub.usage.material"));
        tooltip.add(Component.translatable("tooltip.showercore.bathtub.usage.duck"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Nullable
    public static ResourceLocation getMaterialBlockId(ItemStack stack) {
        return mod.crabmod.showercore.material.BathroomMaterials.blockId(stack);
    }

    public static BathtubMaterialFace getMaterialFace(ItemStack stack) {
        return mod.crabmod.showercore.material.BathroomMaterials.face(stack);
    }

    public static void setMaterial(ItemStack stack, ResourceLocation blockId, BathtubMaterialFace face) {
        mod.crabmod.showercore.material.BathroomMaterials.set(stack, blockId, face);
    }

    private static Component getMaterialName(ResourceLocation materialBlockId) {
        return BuiltInRegistries.BLOCK.getOptional(materialBlockId)
                .map(block -> {
                    ItemStack stack = new ItemStack(block);
                    return stack.is(Items.AIR)
                            ? Component.literal(materialBlockId.toString())
                            : stack.getHoverName();
                })
                .orElse(Component.literal(materialBlockId.toString()));
    }
}
