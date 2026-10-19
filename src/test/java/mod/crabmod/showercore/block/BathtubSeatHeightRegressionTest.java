package mod.crabmod.showercore.block;

import mod.crabmod.showercore.testutil.TestSourceUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BathtubSeatHeightRegressionTest {

    private static final Path BATHTUB_BLOCK_SOURCE = Paths.get(
            "src", "main", "java", "mod", "crabmod", "showercore", "block", "BathtubBlock.java");
    private static final Path COMMAND_SOURCE = Paths.get(
            "src", "main", "java", "mod", "crabmod", "showercore", "command", "ModCommands.java");

    private static String bathtubSource;
    private static String commandSource;

    @BeforeAll
    static void loadSources() throws IOException {
        bathtubSource = TestSourceUtils.readSource(BATHTUB_BLOCK_SOURCE);
        commandSource = TestSourceUtils.readSource(COMMAND_SOURCE);
    }

    @Test
    @DisplayName("Clawfoot bathtubs use a higher seat offset than legacy tubs")
    void clawfootUsesHigherSeatOffset() {
        org.junit.jupiter.api.Assertions.assertEquals(.1, mod.crabmod.showercore.geometry.BathroomGeometry.STANDARD_TUB.seatOffset());
        org.junit.jupiter.api.Assertions.assertEquals(.32, mod.crabmod.showercore.geometry.BathroomGeometry.CLAWFOOT_TUB.seatOffset());
        assertTrue(bathtubSource.contains("geometryFor(state).seatOffset()"),
                "All shapes must use their geometry profile for sitting.");
    }

    @Test
    @DisplayName("All bathtub seat spawns use the shared seat factory")
    void seatSpawnsUseSharedFactory() {
        assertTrue(bathtubSource.contains("createSeatEntity(Level level, BlockPos pos, BlockState state)"),
                "BathtubBlock must expose one seat factory so height rules stay centralized.");
        assertFalse(bathtubSource.contains("getY() + 0.1,"),
                "Direct bathtub seat spawns must not hard-code the old Y offset.");
        assertTrue(commandSource.contains("BathtubBlock.createSeatEntity(level, targetPos, targetState)"),
                "The accept_bath command must reuse the same seat height rules as direct sitting.");
        assertFalse(commandSource.contains("targetPos.getY() + 0.1"),
                "The accept_bath command must not hard-code the old Y offset.");
    }
}
