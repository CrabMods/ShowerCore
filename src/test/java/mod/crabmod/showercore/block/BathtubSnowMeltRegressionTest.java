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
 * Source-level regression test for Bug U (the 1.20 Forge counterpart of Bug S)
 * — snow layers / snow blocks within 3 blocks of a hot bathtub must melt.
 *
 * <p>The mechanism is identical to the 1.21 fix: {@code BathtubBlock#getTicker}
 * returns a server-tick lambda; {@code serverTick} runs every 80 ticks
 * (staggered by {@code pos.asLong()}) and removes SNOW/SNOW_BLOCK/POWDER_SNOW
 * within radius 3 whenever the bath is hot.
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
                "getTicker must early-return for level.isClientSide.");
        assertTrue(Pattern.compile("BATHTUB_BLOCK_ENTITY").matcher(getTickerBody).find(),
                "getTicker must guard on BATHTUB_BLOCK_ENTITY type.");
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
                "serverTick must be gated by '... % 80 != 0'.");
        assertTrue(Pattern.compile("pos\\s*\\.\\s*asLong\\s*\\(\\s*\\)").matcher(serverTickBody).find(),
                "serverTick must reference pos.asLong() to stagger the gate across positions.");
        assertTrue(Pattern.compile("level\\s*\\.\\s*getGameTime\\s*\\(\\s*\\)").matcher(serverTickBody).find(),
                "serverTick must reference level.getGameTime() as the time source for the 80-tick gate.");
    }

    @Test
    @DisplayName("serverTick skips EMPTY and WATER liquids")
    void serverTickSkipsEmptyAndWater() {
        Pattern liquidSkip = Pattern.compile(
                "\\w+\\s*==\\s*LiquidType\\s*\\.\\s*EMPTY\\s*\\|\\|\\s*\\w+\\s*==\\s*LiquidType\\s*\\.\\s*WATER");
        assertTrue(liquidSkip.matcher(serverTickBody).find(),
                "serverTick must early-return when the liquid is EMPTY or WATER.");
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
                        + "radius (or 3). Found " + count + ".");

        Pattern skipOrigin = Pattern.compile(
                "\\w+\\s*==\\s*0\\s*&&\\s*\\w+\\s*==\\s*0\\s*&&\\s*\\w+\\s*==\\s*0");
        assertTrue(skipOrigin.matcher(serverTickBody).find(),
                "serverTick must skip the origin (all three axis offsets == 0).");
    }

    @Test
    @DisplayName("serverTick removes SNOW, SNOW_BLOCK, and POWDER_SNOW via setBlock(AIR, 3)")
    void serverTickRemovesAllThreeSnowTypes() {
        assertTrue(Pattern.compile("Blocks\\s*\\.\\s*SNOW\\b").matcher(serverTickBody).find(),
                "serverTick must check Blocks.SNOW.");
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
                        + "once to actually remove the snow block. Found " + count + ".");
    }
}
