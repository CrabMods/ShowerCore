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
 * Source-level regression test for {@code Config.java}.
 *
 * <p>The config file declares:
 * <ul>
 *   <li>Six bath-core activation block lists, one per bath core type. A missing
 *       key breaks that bath core's activation detection at runtime.</li>
 *   <li>A steam-fluid list and a rubber-duck-destroy fluid list.</li>
 *   <li>A boolean {@code enableModIntegrations} toggle (default {@code true})
 *       reporting the container policy through the Hot Bath API.</li>
 *   <li>An {@code onLoad} handler that populates the eight cached
 *       {@code Set<Block>} / {@code Set<Fluid>} fields plus the boolean toggle,
 *       returning early when the event is for a different spec.</li>
 *   <li>A {@code public static boolean isModIntegrationsEnabled()} accessor
 *       used by the shared contact reports.</li>
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
                "Config must declare 'steam_fluids' list. Steam particles rely on this list.");
        assertTrue(Pattern.compile("\"rubber_duck_destroy_fluids\"").matcher(source).find(),
                "Config must declare 'rubber_duck_destroy_fluids' list. Without it the duck would no "
                        + "longer despawn in lava and would survive in any fluid indefinitely.");
    }

    @Test
    @DisplayName("enableModIntegrations boolean exists and defaults to true")
    void enableModIntegrationsBooleanDefaultsTrue() {
        assertTrue(Pattern.compile("\"enableModIntegrations\"\\s*,\\s*true").matcher(source).find(),
                "Config must declare '.define(\"enableModIntegrations\", true)'. The default MUST be true "
                        + "so external-mod integrations (hotBath, Cold Sweat, TAN, LSO, Serene Seasons) "
                        + "work out of the box; changing the default to false silently breaks every "
                        + "integration for existing users who haven't edited their config.");
    }

    @Test
    @DisplayName("onLoad populates every cached field (6 block sets + 2 fluid sets + integration flag)")
    void onLoadPopulatesAllFields() {
        for (String field : new String[] {
                "hotWaterCoreBlocks", "honeyBathCoreBlocks", "milkBathCoreBlocks",
                "roseBathCoreBlocks", "peonyBathCoreBlocks", "herbalBathCoreBlocks",
                "steamFluids", "rubberDuckDestroyFluids"}) {
            assertTrue(Pattern.compile("\\b" + field + "\\s*=").matcher(source).find(),
                    "Config.onLoad must assign '" + field + "' — missing assignment leaves the field null, "
                            + "which NPEs on every lookup.");
        }
        assertTrue(Pattern.compile("enableModIntegrations\\s*=\\s*ENABLE_MOD_INTEGRATIONS\\s*\\.\\s*get\\s*\\(")
                        .matcher(source).find(),
                "Config.onLoad must cache ENABLE_MOD_INTEGRATIONS.get() into the 'enableModIntegrations' "
                        + "field. Reading the ForgeConfigSpec value on every hot-path compat call would "
                        + "hit config-file locking.");
    }

    @Test
    @DisplayName("onLoad returns early when the loaded spec is not our SPEC")
    void onLoadReturnsEarlyForOtherSpecs() {
        assertTrue(Pattern.compile("getSpec\\s*\\(\\s*\\)\\s*!=\\s*SPEC").matcher(source).find(),
                "Config.onLoad must guard with 'event.getConfig().getSpec() != SPEC' and return early. "
                        + "Without this guard, EVERY other mod's config-load event would clobber our "
                        + "cached sets with empty values (because OTHER_SPEC.get() throws, and the lambda "
                        + "swallow would leave our fields half-populated).");
    }

    @Test
    @DisplayName("isModIntegrationsEnabled is public static and returns the cached flag")
    void isModIntegrationsEnabledAccessor() {
        assertTrue(Pattern.compile(
                        "public\\s+static\\s+boolean\\s+isModIntegrationsEnabled\\s*\\(\\s*\\)")
                        .matcher(source).find(),
                "Config must expose 'public static boolean isModIntegrationsEnabled()'. The contact reporters "
                        + "calls this on every integration entry; renaming it silently disables every gate.");
        assertTrue(Pattern.compile("return\\s+enableModIntegrations\\s*;").matcher(source).find(),
                "isModIntegrationsEnabled must return the cached 'enableModIntegrations' field — returning "
                        + "ENABLE_MOD_INTEGRATIONS.get() directly triggers a config-file read on every "
                        + "call.");
    }
}
