package mod.crabmod.showercore;

import mod.crabmod.showercore.testutil.TestSourceUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source-level regression test for {@code Config.java} (1.20 Forge).
 *
 * <p>The config file declares:
 * <ul>
 *   <li>Six bath-core activation block lists, one per bath core type.</li>
 *   <li>A steam-fluid list and a rubber-duck-destroy fluid list.</li>
 *   <li>A boolean {@code enableModIntegrations} toggle (default {@code true})
 *       reporting the container policy through the Hot Bath API.</li>
 *   <li>An {@code onLoad} handler that populates the eight cached
 *       {@code Set<Block>} / {@code Set<Fluid>} fields plus the boolean toggle,
 *       returning early when the event is for a different spec.</li>
 *   <li>A {@code public static boolean isModIntegrationsEnabled()} accessor.</li>
 * </ul>
 */
class ConfigSpecRegressionTest {

    private static final Path CONFIG_SOURCE = Paths.get(
            "src", "main", "java", "mod", "crabmod", "showercore", "Config.java");

    private static String source;

    @BeforeAll
    static void loadSource() throws IOException {
        source = TestSourceUtils.readSource(CONFIG_SOURCE);
    }

    @Test
    @DisplayName("All six bath-core block list config keys are declared")
    void sixBathCoreBlockListsDefined() {
        for (String key : new String[] {
                "hot_water_core_blocks", "honey_bath_core_blocks", "milk_bath_core_blocks",
                "rose_bath_core_blocks", "peony_bath_core_blocks", "herbal_bath_core_blocks"}) {
            assertTrue(Pattern.compile("\"" + key + "\"").matcher(source).find(),
                    "Config must declare config key '" + key + "'. Removing it would break the "
                            + "corresponding bath core's activation detection.");
        }
    }

    @Test
    @DisplayName("Steam-fluid and rubber-duck-destroy fluid lists are declared")
    void fluidListsDefined() {
        assertTrue(Pattern.compile("\"steam_fluids\"").matcher(source).find(),
                "Config must declare 'steam_fluids' list.");
        assertTrue(Pattern.compile("\"rubber_duck_destroy_fluids\"").matcher(source).find(),
                "Config must declare 'rubber_duck_destroy_fluids' list.");
    }

    @Test
    @DisplayName("enableModIntegrations boolean exists and defaults to true")
    void enableModIntegrationsBooleanDefaultsTrue() {
        assertTrue(Pattern.compile("\"enableModIntegrations\"\\s*,\\s*true").matcher(source).find(),
                "Config must declare '.define(\"enableModIntegrations\", true)'. The default MUST be true "
                        + "so external-mod integrations work out of the box.");
    }

    @Test
    @DisplayName("onLoad populates every cached field (6 block sets + 2 fluid sets + integration flag)")
    void onLoadPopulatesAllFields() {
        for (String field : new String[] {
                "hotWaterCoreBlocks", "honeyBathCoreBlocks", "milkBathCoreBlocks",
                "roseBathCoreBlocks", "peonyBathCoreBlocks", "herbalBathCoreBlocks",
                "steamFluids", "rubberDuckDestroyFluids"}) {
            assertTrue(Pattern.compile("\\b" + field + "\\s*=").matcher(source).find(),
                    "Config.onLoad must assign '" + field + "' — missing assignment leaves the field null.");
        }
        assertTrue(Pattern.compile("enableModIntegrations\\s*=\\s*ENABLE_MOD_INTEGRATIONS\\s*\\.\\s*get\\s*\\(")
                        .matcher(source).find(),
                "Config.onLoad must cache ENABLE_MOD_INTEGRATIONS.get() into the 'enableModIntegrations' "
                        + "field so hot-path compat calls don't hit config-file locking on every tick.");
    }

    @Test
    @DisplayName("onLoad returns early when the loaded spec is not our SPEC")
    void onLoadReturnsEarlyForOtherSpecs() {
        assertTrue(Pattern.compile("getSpec\\s*\\(\\s*\\)\\s*!=\\s*SPEC").matcher(source).find(),
                "Config.onLoad must guard with 'event.getConfig().getSpec() != SPEC' and return early.");
    }

    @Test
    @DisplayName("isModIntegrationsEnabled is public static and returns the cached flag")
    void isModIntegrationsEnabledAccessor() {
        assertTrue(Pattern.compile(
                        "public\\s+static\\s+boolean\\s+isModIntegrationsEnabled\\s*\\(\\s*\\)")
                        .matcher(source).find(),
                "Config must expose 'public static boolean isModIntegrationsEnabled()'. The contact reporters "
                        + "calls this on every integration entry.");
        assertTrue(Pattern.compile("return\\s+enableModIntegrations\\s*;").matcher(source).find(),
                "isModIntegrationsEnabled must return the cached 'enableModIntegrations' field.");
    }
}
