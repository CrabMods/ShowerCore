package mod.crabmod.showercore.entity;

import mod.crabmod.showercore.testutil.TestSourceUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RubberDuckBathtubPlacementRegressionTest {

    private static final Path DUCK_ENTITY = Paths.get(
            "src", "main", "java", "mod", "crabmod", "showercore", "entity", "RubberDuckEntity.java");
    private static final Path DUCK_ITEM = Paths.get(
            "src", "main", "java", "mod", "crabmod", "showercore", "item", "RubberDuckItem.java");
    private static final Path BATHTUB_RENDERER = Paths.get(
            "src", "main", "java", "mod", "crabmod", "showercore",
            "client", "renderer", "BathtubBlockEntityRenderer.java");

    @Test
    @DisplayName("Duck buoyancy uses bathtub-specific float surface instead of the old hard-coded height")
    void duckBuoyancyUsesSharedBathtubSurface() throws IOException {
        String source = TestSourceUtils.readSource(DUCK_ENTITY);

        assertTrue(source.contains("BathtubBlock.duckFloatSurfaceFor(blockState)"),
                "RubberDuckEntity must use BathtubBlock.duckFloatSurfaceFor(blockState) so clawfoot "
                        + "bathtubs float the duck above their higher water surface.");
        assertFalse(source.contains("+ 0.65F"),
                "The old hard-coded 0.65F bathtub float height only matched legacy tubs and made "
                        + "ducks sink into clawfoot bath water.");
    }

    @Test
    @DisplayName("Duck item placement delegates bathtub clicks to shared bathtub placement geometry")
    void duckPlacementUsesSharedBathtubPlacementGeometry() throws IOException {
        String source = TestSourceUtils.readSource(DUCK_ITEM);

        assertTrue(source.contains("bathtubAwarePlacementPosition(level, context.getClickedPos(), pos)"),
                "RubberDuckItem.useOn must adjust placement when clicking a bathtub block.");
        assertTrue(source.contains("bathtubAwarePlacementPosition(level, blockHitResult.getBlockPos(), pos)"),
                "RubberDuckItem.use must adjust POV placement when clicking a bathtub block.");
        assertTrue(source.contains("BathtubBlock.duckPlacementFor(clickedPos, clickedState, pos)"),
                "RubberDuckItem must delegate bathtub placement to BathtubBlock so item placement "
                        + "and in-game bathtub interaction cannot drift apart.");
    }

    @Test
    @DisplayName("Bathtub renderer and duck logic share the same water-height helper")
    void rendererUsesSameWaterHeightSourceAsDuck() throws IOException {
        String source = TestSourceUtils.readSource(BATHTUB_RENDERER);

        assertTrue(source.contains("BathtubBlock.waterLevelFor(state)"),
                "BathtubBlockEntityRenderer must use BathtubBlock.waterLevelFor(state), not private "
                        + "renderer-only constants that can drift from duck physics.");
        assertTrue(source.contains("BathtubBlock.geometryFor(state)"),
                "Renderer clawfoot detection must use the shared BathtubBlock helper.");
    }
}
