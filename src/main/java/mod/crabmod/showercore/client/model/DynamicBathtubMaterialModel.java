package mod.crabmod.showercore.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.material.BathtubMaterialFace;
import mod.crabmod.showercore.block.BathtubBlock;
import mod.crabmod.showercore.block.entity.BathtubBlockEntity;
import mod.crabmod.showercore.material.BathroomMaterials;
import mod.crabmod.showercore.block.ShowerHeadBlock;
import mod.crabmod.showercore.entity.ShowerHeadContainerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicBathtubMaterialModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<ResourceLocation> MATERIAL_BLOCK_ID = new ModelProperty<>();
    private static final ModelProperty<BathtubMaterialFace> MATERIAL_FACE = new ModelProperty<>();
    private record MaterialKey(ResourceLocation blockId, BathtubMaterialFace face) {}
    private static final Map<MaterialKey, Optional<TextureAtlasSprite>> MATERIAL_SPRITE_CACHE = new ConcurrentHashMap<>();
    private static final int INTS_PER_VERTEX = 8;
    private static final int U_OFFSET = 4;
    private static final int V_OFFSET = 5;
    @Nullable
    private final ResourceLocation itemMaterialBlockId;
    private final BathtubMaterialFace itemMaterialFace;
    private final ItemOverrides overrides;

    public DynamicBathtubMaterialModel(BakedModel originalModel) {
        this(originalModel, null, BathtubMaterialFace.LEGACY);
    }

    private DynamicBathtubMaterialModel(BakedModel originalModel, @Nullable ResourceLocation itemMaterialBlockId, BathtubMaterialFace face) {
        super(originalModel);
        this.itemMaterialBlockId = itemMaterialBlockId;
        this.itemMaterialFace = face;
        this.overrides = new MaterialItemOverrides(originalModel.getOverrides());
    }

    public static boolean shouldWrap(ResourceLocation location) {
        return location.getNamespace().equals(ShowerCore.MODID) && (location.getPath().startsWith("bathtub_") || location.getPath().contains("shower_head") || location.getPath().startsWith("rubber_duck"));
    }

    public static void clearCache() {
        MATERIAL_SPRITE_CACHE.clear();
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        ModelData data = super.getModelData(level, pos, state, modelData);
        if (state.getBlock() instanceof BathtubBlock
                && level.getBlockEntity(pos) instanceof BathtubBlockEntity bathtubEntity
                && bathtubEntity.getMaterialBlockId() != null) {
            return data.derive().with(MATERIAL_BLOCK_ID, bathtubEntity.getMaterialBlockId())
                    .with(MATERIAL_FACE, bathtubEntity.getMaterialFace()).build();
        }
        if (level.getBlockEntity(pos) instanceof ShowerHeadContainerEntity shower && shower.getMaterialBlockId() != null) {
            return data.derive().with(MATERIAL_BLOCK_ID, shower.getMaterialBlockId()).with(MATERIAL_FACE, shower.getMaterialFace()).build();
        }
        return data;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return replaceMaterialQuads(super.getQuads(state, side, rand), state, itemMaterialBlockId, itemMaterialFace);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData extraData, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, renderType);
        ResourceLocation materialBlockId = itemMaterialBlockId != null ? itemMaterialBlockId : extraData.get(MATERIAL_BLOCK_ID);
        BathtubMaterialFace face = itemMaterialBlockId != null ? itemMaterialFace : extraData.get(MATERIAL_FACE);
        return replaceMaterialQuads(quads, state, materialBlockId, face == null ? BathtubMaterialFace.LEGACY : face);
    }

    private List<BakedQuad> replaceMaterialQuads(List<BakedQuad> quads, @Nullable BlockState state,
                                                 @Nullable ResourceLocation materialBlockId, BathtubMaterialFace face) {
        if (materialBlockId == null
                || (state != null && !(state.getBlock() instanceof BathtubBlock) && !(state.getBlock() instanceof ShowerHeadBlock))) {
            return quads;
        }

        TextureAtlasSprite materialSprite = resolveMaterialSprite(materialBlockId, face);
        if (materialSprite == null) {
            return quads;
        }

        TextureAtlasSprite bodySprite = originalModel.getParticleIcon(ModelData.EMPTY);

        List<BakedQuad> replaced = new ArrayList<>(quads.size());
        boolean changed = false;
        for (BakedQuad quad : quads) {
            // Tint 1 identifies the clawfoot underside, which has its own legacy texture.
            boolean body = (quad.getTintIndex() == 1 && face != BathtubMaterialFace.LEGACY) || (quad.getTintIndex() != 2 && quad.getSprite() == bodySprite);
            if (body && quad.getSprite() != materialSprite) {
                replaced.add(copyWithSprite(quad, quad.getSprite(), materialSprite));
                changed = true;
            } else {
                replaced.add(quad);
            }
        }
        return changed ? replaced : quads;
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poses, boolean leftHand) {
        BakedModel transformed = originalModel.applyTransform(context, poses, leftHand);
        return transformed == originalModel ? this : new DynamicBathtubMaterialModel(transformed, itemMaterialBlockId, itemMaterialFace);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        List<BakedModel> passes = originalModel.getRenderPasses(stack, fabulous);
        if (itemMaterialBlockId == null) {
            return passes;
        }
        // Native item rendering consumes each pass, not the parent model's quads.
        return passes.stream()
                .map(pass -> (BakedModel) new DynamicBathtubMaterialModel(pass, itemMaterialBlockId, itemMaterialFace))
                .toList();
    }

    @Nullable
    public static TextureAtlasSprite resolveMaterialSprite(ResourceLocation materialBlockId, BathtubMaterialFace face) {
        MaterialKey key = new MaterialKey(materialBlockId, face);
        Optional<TextureAtlasSprite> cached = MATERIAL_SPRITE_CACHE.get(key);
        if (cached != null) return cached.orElse(null);
        Optional<TextureAtlasSprite> resolved = Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(materialBlockId))
                .filter(block -> !(block instanceof BathtubBlock))
                .map(block -> face == BathtubMaterialFace.LEGACY ? getParticleSprite(block) : getFaceSprite(block, face));
        MATERIAL_SPRITE_CACHE.put(key, resolved);
        return resolved.orElse(null);
    }

    @Nullable
    private static TextureAtlasSprite getFaceSprite(Block block, BathtubMaterialFace face) {
        BlockState state = block.defaultBlockState();
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        Direction direction = face == BathtubMaterialFace.TOP ? Direction.UP : Direction.NORTH;
        // Models can put the same visible face in either culled or unculled quads.
        for (Direction culled : new Direction[] {direction, null}) {
            for (RenderType layer : model.getRenderTypes(state, RandomSource.create(42), ModelData.EMPTY)) {
                for (BakedQuad quad : model.getQuads(state, culled, RandomSource.create(42), ModelData.EMPTY, layer)) {
                    if (quad.getDirection() == direction) return quad.getSprite();
                }
            }
        }
        return null;
    }

    private static TextureAtlasSprite getParticleSprite(Block block) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(block.defaultBlockState());
        return model.getParticleIcon(ModelData.EMPTY);
    }

    private static BakedQuad copyWithSprite(BakedQuad quad, TextureAtlasSprite fromSprite, TextureAtlasSprite toSprite) {
        int[] vertices = quad.getVertices().clone();
        remapSpriteUvs(vertices, fromSprite, toSprite);
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), toSprite, quad.isShade());
    }

    private static void remapSpriteUvs(int[] vertices, TextureAtlasSprite fromSprite, TextureAtlasSprite toSprite) {
        float fromUSize = fromSprite.getU1() - fromSprite.getU0();
        float fromVSize = fromSprite.getV1() - fromSprite.getV0();
        if (fromUSize == 0.0F || fromVSize == 0.0F) {
            return;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * INTS_PER_VERTEX;
            float oldU = Float.intBitsToFloat(vertices[base + U_OFFSET]);
            float oldV = Float.intBitsToFloat(vertices[base + V_OFFSET]);
            float relativeU = (oldU - fromSprite.getU0()) / fromUSize;
            float relativeV = (oldV - fromSprite.getV0()) / fromVSize;
            float newU = toSprite.getU0() + relativeU * (toSprite.getU1() - toSprite.getU0());
            float newV = toSprite.getV0() + relativeV * (toSprite.getV1() - toSprite.getV0());
            vertices[base + U_OFFSET] = Float.floatToRawIntBits(newU);
            vertices[base + V_OFFSET] = Float.floatToRawIntBits(newV);
        }
    }

    private class MaterialItemOverrides extends ItemOverrides {
        private final ItemOverrides delegate;

        private MaterialItemOverrides(ItemOverrides delegate) {
            this.delegate = delegate;
        }

        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            BakedModel resolved = delegate.resolve(originalModel, stack, level, entity, seed);
            if (resolved == null) {
                resolved = originalModel;
            }
            ResourceLocation materialBlockId = BathroomMaterials.blockId(stack);
            if (materialBlockId == null) {
                return resolved;
            }
            return new DynamicBathtubMaterialModel(resolved, materialBlockId, BathroomMaterials.face(stack));
        }
    }
}
