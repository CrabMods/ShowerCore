package mod.crabmod.showercore.client.renderer;

import mod.crabmod.showercore.testutil.TestSourceUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source-level regression test for {@link BathtubBlockEntityRenderer}.
 *
 * <p>The BER renders fluid surfaces for ALL non-empty bathtub liquid types.
 * It fetches the fluid texture from {@code IClientFluidTypeExtensions.getStillTexture()}
 * which is registered by hotBath's {@code DynamicFluidType.initializeClient()} and is
 * guaranteed non-null ({@code hotbath:block/still_custom_fluid_grayscale}).
 *
 * <p>Past bugs:
 * <ul>
 *   <li>Silent null-texture abort: {@code if (stillTexture == null) return;} hid the
 *       real rendering issue instead of surfacing it.</li>
 *   <li>Wrong fallback: replacing null with {@code minecraft:block/water_still} masked
 *       the real issue and displayed the wrong texture.</li>
 *   <li>BER only rendered for CUSTOM: the guard {@code != CUSTOM} meant classic fluids
 *       never got BER rendering and relied on block models whose blockstate texture
 *       overrides were silently ignored by Minecraft.</li>
 * </ul>
 */
class BathtubBERRenderingRegressionTest {

    private static final Path BER_SOURCE = Paths.get(
            "src", "main", "java", "mod", "crabmod", "showercore", "client",
            "renderer", "BathtubBlockEntityRenderer.java");

    /** Matches patterns like {@code if (stillTexture == null) return} or null-fallback blocks. */
    private static final Pattern NULL_TEXTURE_GUARD =
            Pattern.compile("stillTexture\\s*==\\s*null");

    /** Matches the block-entity translucent sheet that survives the Fabulous render pipeline. */
    private static final Pattern BLOCK_ENTITY_TRANSLUCENT_SHEET =
            Pattern.compile("Sheets\\.translucentCullBlockSheet\\(\\)");

    /** Matches the chunk translucent target that Fabulous clears after block entities render. */
    private static final Pattern UNSAFE_CHUNK_TRANSLUCENT_TYPE =
            Pattern.compile("RenderType\\.translucent\\(\\)");

    /** Matches {@code getStillTexture} — the BER must fetch the texture from the FluidType. */
    private static final Pattern GET_STILL_TEXTURE =
            Pattern.compile("getStillTexture");

    /** Matches the correct guard: early-return only for EMPTY. */
    private static final Pattern EMPTY_GUARD =
            Pattern.compile("==\\s*BathtubBlock\\.LiquidType\\.EMPTY");

    /** Matches the old broken guard: only rendering for CUSTOM. */
    private static final Pattern CUSTOM_ONLY_GUARD =
            Pattern.compile("!=\\s*BathtubBlock\\.LiquidType\\.CUSTOM");

    @Test
    @DisplayName("BER must NOT have a null-texture guard (no silent abort or fallback)")
    void noNullTextureGuard() throws IOException {
        String source = TestSourceUtils.readSource(BER_SOURCE);
        assertFalse(NULL_TEXTURE_GUARD.matcher(source).find(),
                "BathtubBlockEntityRenderer must NOT check stillTexture == null. "
                        + "The texture is guaranteed non-null by DynamicFluidType.initializeClient(). "
                        + "A null check either silently aborts rendering (hiding bugs) or falls "
                        + "back to a wrong texture.");
    }

    @Test
    @DisplayName("BER uses the block-entity translucent sheet so water survives Fabulous rendering")
    void usesFabulousSafeBlockEntityTranslucentSheet() throws IOException {
        String source = TestSourceUtils.readSource(BER_SOURCE);
        assertTrue(BLOCK_ENTITY_TRANSLUCENT_SHEET.matcher(source).find(),
                "BathtubBlockEntityRenderer must use Sheets.translucentCullBlockSheet() so "
                        + "fluid geometry is flushed as block-entity transparency and remains "
                        + "visible when Fabulous clears its chunk-translucent framebuffer.");
        assertFalse(UNSAFE_CHUNK_TRANSLUCENT_TYPE.matcher(source).find(),
                "BathtubBlockEntityRenderer must not submit fluid geometry to "
                        + "RenderType.translucent(); Fabulous clears that target after the "
                        + "block-entity batch, erasing the bathtub water.");
    }

    @Test
    @DisplayName("BER fetches texture from IClientFluidTypeExtensions.getStillTexture()")
    void fetchesTextureFromFluidType() throws IOException {
        String source = TestSourceUtils.readSource(BER_SOURCE);
        assertTrue(GET_STILL_TEXTURE.matcher(source).find(),
                "BathtubBlockEntityRenderer must call getStillTexture() on the fluid's "
                        + "IClientFluidTypeExtensions to get the correct grayscale texture.");
    }

    @Test
    @DisplayName("BER must NOT hardcode water_still as a texture")
    void noHardcodedWaterTexture() throws IOException {
        String source = TestSourceUtils.readSource(BER_SOURCE);
        assertFalse(source.contains("water_still"),
                "BathtubBlockEntityRenderer must NOT reference water_still. "
                        + "The custom fluid texture comes from DynamicFluidType's registered "
                        + "still texture (hotbath:block/still_custom_fluid_grayscale).");
    }

    @Test
    @DisplayName("BER early-returns only for EMPTY (renders ALL other liquid types)")
    void guardsOnEmptyNotCustom() throws IOException {
        String source = TestSourceUtils.readSource(BER_SOURCE);
        assertTrue(EMPTY_GUARD.matcher(source).find(),
                "BathtubBlockEntityRenderer must guard with == LiquidType.EMPTY so that "
                        + "ALL non-empty liquid types (water, classic hotBath, custom) get "
                        + "BER-rendered fluid surfaces.");
    }

    @Test
    @DisplayName("BER must NOT guard with != CUSTOM (old bug: only rendered for CUSTOM)")
    void noCustomOnlyGuard() throws IOException {
        String source = TestSourceUtils.readSource(BER_SOURCE);
        assertFalse(CUSTOM_ONLY_GUARD.matcher(source).find(),
                "BathtubBlockEntityRenderer must NOT use != LiquidType.CUSTOM. "
                        + "That old guard skipped BER rendering for classic fluids, which then "
                        + "relied on block model texture overrides that Minecraft silently ignores.");
    }

    @Test
    @DisplayName("BER uses full-bright light for lava (LightTexture.FULL_BRIGHT)")
    void lavaUsesFullBrightLight() throws IOException {
        String source = TestSourceUtils.readSource(BER_SOURCE);
        assertTrue(source.contains("LightTexture.FULL_BRIGHT"),
                "Lava in bathtubs must use LightTexture.FULL_BRIGHT so it glows "
                        + "properly in dark rooms, matching vanilla lava emissive behavior.");
        assertTrue(source.contains("Fluids.LAVA"),
                "BER must check for Fluids.LAVA to apply emissive lighting.");
    }

    @Test
    @DisplayName("BER delegates drop gating to BathtubDropGeometry.shouldRenderDrop")
    void dropGatingDelegated() throws IOException {
        String source = TestSourceUtils.readSource(BER_SOURCE);
        assertTrue(source.contains("BathtubDropGeometry.shouldRenderDrop"),
                "BER must delegate drop-rendering decisions to BathtubDropGeometry "
                        + "so the logic is unit-testable without a Minecraft client.");
    }

    @Test
    @DisplayName("BER positions clawfoot faucet drops from the right-rim faucet side")
    void clawfootDropUsesRightRimSide() throws IOException {
        String source = TestSourceUtils.readSource(BER_SOURCE);
        assertTrue(source.contains("BathtubBlock.geometryFor(state)"),
                "Faucet placement must use the shape profile, including the clawfoot right-rim coordinates.");
        assertTrue(source.contains("computeDropBounds(toFaucetSide(facing), BathtubBlock.geometryFor(state))"),
                "BER must use the shared profile to rotate each shape's faucet coordinates.");
    }

    @Test
    @DisplayName("BER fetches per-fluid color via CustomFluidAPI.getFluidColor")
    void usesCustomFluidAPIColor() throws IOException {
        String source = TestSourceUtils.readSource(BER_SOURCE);
        assertTrue(source.contains("CustomFluidAPI.getFluidColor"),
                "BER must call CustomFluidAPI.getFluidColor(customFluidId) to get the "
                        + "per-fluid color for custom fluids, not rely on the shared FluidType "
                        + "tint which is identical across all custom fluid variants.");
    }
}
