package mod.crabmod.showercore.mixin;

import mod.crabmod.showercore.testutil.TestSourceUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resource-level regression test for {@code mixins.showercore.json} and the
 * mixin Java sources it references.
 *
 * <p>Mixins are wired by listing short class names in {@code mixins.showercore
 * .json}; the {@code package} field at the top prefixes each entry to form the
 * FQN. A typo or a rename leaves the mixin class orphaned — it compiles but is
 * never loaded, and every {@code @Inject} silently no-ops.
 *
 * <p>Invariants guarded:
 * <ul>
 *   <li>{@code mixins.showercore.json} lists every mixin class that exists in
 *       the {@code mixin/} package, and conversely every listed class has a
 *       matching {@code .java} file.</li>
 *   <li>{@code required: true} — the mixin config must fail-fast if the mod
 *       loader can't apply it, rather than silently skipping the config and
 *       leaving dirtiness/custom-fluid behaviour broken.</li>
 *   <li>The {@code package} prefix matches the actual Java package so mixins
 *       are discoverable at runtime.</li>
 * </ul>
 */
class MixinConfigIntegrityTest {

    private static final Path MIXINS_JSON = Paths.get(
            "src", "main", "resources", "mixins.showercore.json");
    private static final Path MIXIN_DIR = Paths.get(
            "src", "main", "java", "mod", "crabmod", "showercore", "mixin");
    private static final String EXPECTED_PACKAGE = "mod.crabmod.showercore.mixin";

    private static String mixinsJson;

    @BeforeAll
    static void loadMixinsJson() throws IOException {
        mixinsJson = TestSourceUtils.normalize(Files.readString(MIXINS_JSON));
    }

    @Test
    @DisplayName("mixins.showercore.json declares required: true (fail-fast semantics)")
    void mixinsJsonIsRequired() {
        assertTrue(mixinsJson.matches("(?s).*\"required\"\\s*:\\s*true.*"),
                "mixins.showercore.json must set \"required\": true. A non-required mixin config silently "
                        + "skips on apply failure — the virtual-fluid movement mixins are "
                        + "both load-bearing (Bugs R and the hotBath temperature integration). If the "
                        + "mixin-platform can't apply them, the mod MUST refuse to load rather than "
                        + "running broken.");
    }

    @Test
    @DisplayName("package field matches mod.crabmod.showercore.mixin (where the .java files actually live)")
    void packageFieldMatches() {
        assertTrue(mixinsJson.contains("\"package\": \"" + EXPECTED_PACKAGE + "\"")
                        || mixinsJson.contains("\"package\":\"" + EXPECTED_PACKAGE + "\""),
                "mixins.showercore.json 'package' field must be '" + EXPECTED_PACKAGE + "' — the Java "
                        + "package where the mixin sources actually live. A mismatch makes every mixin "
                        + "entry unresolvable at load time.");
    }

    @Test
    @DisplayName("Every class listed in mixins.json exists as a .java file in the mixin package")
    void everyListedMixinHasSourceFile() throws IOException {
        for (String name : extractMixinList(mixinsJson)) {
            Path p = MIXIN_DIR.resolve(name + ".java");
            assertTrue(Files.isRegularFile(p),
                    "mixins.showercore.json lists '" + name + "' but no matching file exists at " + p
                            + ". Either the class was deleted/renamed, or the JSON entry is stale. An "
                            + "orphan entry fails at load time with a ClassNotFoundException.");
        }
    }

    @Test
    @DisplayName("Every .java file in the mixin package is listed in mixins.json")
    void everyMixinSourceIsListedInJson() throws IOException {
        if (!Files.isDirectory(MIXIN_DIR)) return;
        try (var stream = Files.newDirectoryStream(MIXIN_DIR, "*Mixin.java")) {
            for (Path p : stream) {
                String name = p.getFileName().toString().replace(".java", "");
                assertTrue(mixinsJson.contains("\"" + name + "\""),
                        "Mixin class '" + name + "' exists under " + MIXIN_DIR + " but is NOT listed in "
                                + "mixins.showercore.json. The mixin loader only applies classes named in "
                                + "the config; an un-listed mixin class compiles but is silently orphaned, "
                                + "and any behaviour it was supposed to inject is absent at runtime.");
            }
        }
    }

    private static java.util.List<String> extractMixinList(String json) {
        java.util.List<String> out = new java.util.ArrayList<>();
        int arrStart = json.indexOf("\"mixins\"");
        if (arrStart < 0) return out;
        int openBracket = json.indexOf('[', arrStart);
        int closeBracket = json.indexOf(']', openBracket);
        if (openBracket < 0 || closeBracket < 0) return out;
        String arr = json.substring(openBracket + 1, closeBracket);
        for (String part : arr.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 2) {
                out.add(trimmed.substring(1, trimmed.length() - 1));
            }
        }
        return out;
    }
}
