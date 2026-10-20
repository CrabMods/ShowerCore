package mod.crabmod.showercore.block;

import mod.crabmod.showercore.testutil.TestSourceUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source-level regression test for Bug S — snow layers / snow blocks within 3
 * blocks of a hot bathtub must melt, so that a hot bath stays usable in a
 * Serene Seasons winter (where snow accumulates rapidly on top of every
 * exposed block, including the bathtub itself).
 *
 * <p>Fix: {@code BathtubBlock#getTicker} returns a server-tick lambda that,
 * every 80 ticks (4 seconds) and staggered by {@code pos.asLong()} to spread
 * load across chunks, scans the 7×7×7 neighbourhood around the bathtub and
 * removes any {@code Blocks.SNOW}, {@code Blocks.SNOW_BLOCK}, or
 * {@code Blocks.POWDER_SNOW}. The scan is gated to non-EMPTY, non-WATER
 * liquids; CUSTOM liquids additionally require
 * {@code CoreUtils.isCustomFluidHotAt(level, pos)} to be true so cold custom
 * fluids don't melt snow.
 */
class BathtubSnowMeltRegressionTest {

    private static final Path BATHTUB_SOURCE = Paths.get(
            "src", "main", "java", "mod", "crabmod", "showercore", "block", "BathtubBlock.java");

    private static final Pattern GET_TICKER_SIG = Pattern.compile(
            "public\\s+<T\\s+extends\\s+BlockEntity>\\s+BlockEntityTicker<T>\\s+getTicker\\s*\\(");
    private static final Pattern SERVER_TICK_SIG = Pattern.compile(
            "private\\s+static\\s+void\\s+serverTick\\s*\\(");

    private static String source;
    private static String getTickerBody;
    private static String serverTickBody;

    @BeforeAll
    static void loadSource() throws IOException {
        source = TestSourceUtils.readSource(BATHTUB_SOURCE);
        getTickerBody = TestSourceUtils.extractMethodBody(source, GET_TICKER_SIG, "BathtubBlock.getTicker");
        serverTickBody = TestSourceUtils.extractMethodBody(source, SERVER_TICK_SIG, "BathtubBlock.serverTick");
    }

    @Test
    @DisplayName("BathtubBlock.getTicker is overridden and returns a ticker for BATHTUB_BLOCK_ENTITY only")
    void getTickerIsOverridden() {
        assertTrue(Pattern.compile("level\\s*\\.\\s*isClientSide").matcher(getTickerBody).find(),
                "getTicker must early-return for level.isClientSide to avoid running melt logic client-side.");
        assertTrue(Pattern.compile("BATHTUB_BLOCK_ENTITY").matcher(getTickerBody).find(),
                "getTicker must guard on BATHTUB_BLOCK_ENTITY so we don't try to cast unrelated block "
                        + "entities to BathtubBlockEntity.");
    }

    @Test
    @DisplayName("serverTick runs every 80 ticks, staggered by pos.asLong()")
    void serverTickRunsEvery80TicksStaggered() {
        Pattern fullSig = Pattern.compile(
                "private\\s+static\\s+void\\s+serverTick\\s*\\(\\s*Level\\s+\\w+\\s*,"
                        + "\\s*BlockPos\\s+\\w+\\s*,\\s*BlockState\\s+\\w+\\s*,"
                        + "\\s*BathtubBlockEntity\\s+\\w+\\s*\\)");
        assertTrue(fullSig.matcher(source).find(),
                "private static void serverTick(Level, BlockPos, BlockState, BathtubBlockEntity) must exist.");

        assertTrue(Pattern.compile("%\\s*80L?\\s*!=\\s*0").matcher(serverTickBody).find(),
                "serverTick must be gated by '... % 80 != 0' so the melt scan runs once every 4 seconds.");
        assertTrue(Pattern.compile("pos\\s*\\.\\s*asLong\\s*\\(\\s*\\)").matcher(serverTickBody).find(),
                "serverTick must reference pos.asLong() to stagger the gate across positions and avoid "
                        + "synchronized melt spikes on every 80th tick.");
        assertTrue(Pattern.compile("level\\s*\\.\\s*getGameTime\\s*\\(\\s*\\)").matcher(serverTickBody).find(),
                "serverTick must reference level.getGameTime() as the time source for the 80-tick gate.");
    }

    @Test
    @DisplayName("serverTick skips EMPTY and WATER liquids (cold baths don't melt snow)")
    void serverTickSkipsEmptyAndWater() {
        Pattern liquidSkip = Pattern.compile(
                "\\w+\\s*==\\s*LiquidType\\s*\\.\\s*EMPTY\\s*\\|\\|\\s*\\w+\\s*==\\s*LiquidType\\s*\\.\\s*WATER");
        assertTrue(liquidSkip.matcher(serverTickBody).find(),
                "serverTick must early-return when the liquid is EMPTY or WATER — an empty tub or one "
                        + "filled with cold water must not melt surrounding snow.");
    }

    @Test
    @DisplayName("CUSTOM liquids only melt snow when CoreUtils.isCustomFluidHotAt reports hot")
    void customLiquidMeltsOnlyWhenHot() {
        assertTrue(serverTickBody.contains("be.isHot()"),
                "Snow melting must use the shared fluid policy, including reloaded cold built-in baths.");
    }

    @Test
    @DisplayName("serverTick scans a 7x7x7 cube (-3..3 on each axis) and skips the origin")
    void serverTickScansRadius3() {
        Pattern axisLoop = Pattern.compile(
                "\\w+\\s*=\\s*-\\s*(?:radius|3)\\s*;\\s*\\w+\\s*<=\\s*(?:radius|3)");
        Matcher m = axisLoop.matcher(serverTickBody);
        int count = 0;
        while (m.find()) count++;
        assertTrue(count >= 3,
                "serverTick must declare three nested axis loops ranging from -radius (or -3) to "
                        + "radius (or 3). A smaller range leaves snow on the bathtub itself; larger wastes "
                        + "CPU. Found " + count + " matching axis-loop bounds.");

        Pattern skipOrigin = Pattern.compile(
                "\\w+\\s*==\\s*0\\s*&&\\s*\\w+\\s*==\\s*0\\s*&&\\s*\\w+\\s*==\\s*0");
        assertTrue(skipOrigin.matcher(serverTickBody).find(),
                "serverTick must skip the origin (all three axis offsets == 0) so the bathtub itself "
                        + "isn't sampled.");
    }

    @Test
    @DisplayName("serverTick removes SNOW, SNOW_BLOCK, and POWDER_SNOW via setBlock(AIR, 3)")
    void serverTickRemovesAllThreeSnowTypes() {
        assertTrue(Pattern.compile("Blocks\\s*\\.\\s*SNOW\\b").matcher(serverTickBody).find(),
                "serverTick must check Blocks.SNOW (snow layer).");
        assertTrue(Pattern.compile("Blocks\\s*\\.\\s*SNOW_BLOCK").matcher(serverTickBody).find(),
                "serverTick must check Blocks.SNOW_BLOCK.");
        assertTrue(Pattern.compile("Blocks\\s*\\.\\s*POWDER_SNOW").matcher(serverTickBody).find(),
                "serverTick must check Blocks.POWDER_SNOW.");

        Pattern setAir = Pattern.compile(
                "level\\s*\\.\\s*setBlock\\s*\\(\\s*\\w+\\s*,\\s*"
                        + "Blocks\\s*\\.\\s*AIR\\s*\\.\\s*defaultBlockState\\s*\\(\\s*\\)\\s*,\\s*3\\s*\\)");
        Matcher sm = setAir.matcher(serverTickBody);
        int count = 0;
        while (sm.find()) count++;
        assertTrue(count >= 1,
                "serverTick must call 'level.setBlock(<pos>, Blocks.AIR.defaultBlockState(), 3)' at least "
                        + "once in the snow-remove path. Found " + count + ".");
    }
}
