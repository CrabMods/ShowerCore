package mod.crabmod.showercore.pool;

import mod.crabmod.showercore.material.BathtubMaterialFace;
import mod.crabmod.showercore.menu.BathtubMaterialMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record PoolSettings(PoolSpec spec, ResourceLocation material, BathtubMaterialFace face, boolean roomSelection) {
    public static final PoolSettings DEFAULT = new PoolSettings(PoolSpec.DEFAULT,
            new ResourceLocation("minecraft:quartz_block"), BathtubMaterialFace.SIDE, false);

    public PoolSettings {
        if (spec == null || material == null || (face != BathtubMaterialFace.SIDE && face != BathtubMaterialFace.TOP))
            throw new IllegalArgumentException("Invalid pool settings");
    }

    public static CompoundTag specTag(PoolSpec spec) {
        var tag = new CompoundTag();
        tag.putInt("Shape", spec.shape().ordinal()); tag.putInt("Width", spec.width());
        tag.putInt("Length", spec.length()); tag.putInt("Radius", spec.radiusQuarters());
        return tag;
    }

    public static PoolSpec readSpec(CompoundTag tag) {
        int shape = tag.getInt("Shape");
        if (shape < 0 || shape >= PoolSpec.Shape.values().length) throw new IllegalArgumentException("Invalid pool shape");
        return new PoolSpec(PoolSpec.Shape.values()[shape], tag.getInt("Width"), tag.getInt("Length"), tag.getInt("Radius"));
    }

    public CompoundTag save() {
        var tag = specTag(spec); tag.putString("Material", material.toString());
        tag.putString("Face", face.serializedName()); tag.putBoolean("Room", roomSelection);
        return tag;
    }

    public static PoolSettings load(CompoundTag tag) {
        ResourceLocation material = ResourceLocation.tryParse(tag.getString("Material"));
        String face = tag.getString("Face");
        if (!face.equals("side") && !face.equals("top")) throw new IllegalArgumentException("Invalid material face");
        return new PoolSettings(readSpec(tag), material, BathtubMaterialFace.read(face), tag.getBoolean("Room"));
    }

    public static boolean validMaterial(ResourceLocation material) {
        var block = BuiltInRegistries.BLOCK.getOptional(material);
        return block.isPresent() && BathtubMaterialMenu.isMaterial(new ItemStack(block.get()));
    }

    public static CompoundTag data(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
    }

    public static void data(ItemStack stack, CompoundTag tag) { stack.setTag(tag); }

    @Nullable public static PoolSettings read(ItemStack stack) {
        if(!(stack.getItem() instanceof mod.crabmod.showercore.item.BathingPoolItem))return null;
        var root = data(stack);
        if (!root.contains("PoolPlan")) return DEFAULT;
        try {
            var settings = load(root.getCompound("PoolPlan"));
            return validMaterial(settings.material) ? settings : null;
        } catch (IllegalArgumentException error) { return null; }
    }

    public static void write(ItemStack stack, PoolSettings settings) {
        var tag = data(stack); tag.put("PoolPlan", settings.save());
        tag.remove("PoolFirst"); tag.remove("PoolReady"); tag.remove("PoolDimension");
        data(stack, tag);
    }
}
