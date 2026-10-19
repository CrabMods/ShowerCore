package mod.crabmod.showercore.client.renderer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BathtubWaterLevelRegressionTest {

    private static final Path RENDERER = Paths.get(
            "src", "main", "java", "mod", "crabmod", "showercore",
            "client", "renderer", "BathtubBlockEntityRenderer.java");
    private static final Path WATER_GEOMETRY = Paths.get(
            "src", "main", "java", "mod", "crabmod", "showercore",
            "block", "BathtubWaterGeometry.java");

    @Test
    @DisplayName("Clawfoot bathtubs render a higher fluid surface than legacy tubs")
    void clawfootBathtubsUseHigherFluidSurface() throws IOException {
        String rendererSource = Files.readString(RENDERER);
        String geometrySource = Files.readString(WATER_GEOMETRY);

        assertTrue(geometrySource.contains("DEFAULT_WATER_LEVEL = 0.6f"),
                "Legacy bathtub water level should remain unchanged.");
        assertTrue(geometrySource.contains("CLAWFOOT_WATER_LEVEL = 0.74f"),
                "Clawfoot bathtubs need a higher water surface to match their taller bowl.");
        assertTrue(rendererSource.contains("BathtubBlock.geometryFor(state)"),
                "The higher water level must be scoped to clawfoot bathtub block ids.");
        assertTrue(rendererSource.contains("BathtubBlock.waterLevelFor(state)"),
                "The renderer should use the block-state-specific water level.");
    }
}
