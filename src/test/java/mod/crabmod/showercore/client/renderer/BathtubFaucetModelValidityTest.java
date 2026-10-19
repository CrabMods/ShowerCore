package mod.crabmod.showercore.client.renderer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mod.crabmod.showercore.testutil.ModelJsonTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates the structural integrity of bathtub _head_faucet model files.
 * The production variants are texture wrappers over a shared template.
 */
class BathtubFaucetModelValidityTest {

    private static final Path MODELS_DIR = Paths.get(
            "src", "main", "resources", "assets", "showercore", "models", "block");

    private static final String ROOT_MODEL = "bathtub_white_head_faucet.json";
    private static final String EXPECTED_PARENT_REF = "showercore:block/template/bathtub_head_faucet";

    private static final String[] ALL_VARIANTS = {
            "white", "black", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red",
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove",
            "cherry", "bamboo", "crimson", "warped",
            "stone", "cobblestone", "iron", "gold", "copper", "diamond"
    };

    @Test
    @DisplayName("All 33 _head_faucet model files exist and are valid JSON")
    void allFaucetModelsExistAndAreValidJson() throws IOException {
        for (String variant : ALL_VARIANTS) {
            String filename = "bathtub_" + variant + "_head_faucet.json";
            Path model = MODELS_DIR.resolve(filename);
            assertTrue(Files.isRegularFile(model),
                    filename + " must exist at " + model.toAbsolutePath());
            try {
                JsonParser.parseString(Files.readString(model)).getAsJsonObject();
            } catch (Exception e) {
                fail(filename + " is not valid JSON: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Resolved _head_faucet model uses render_type=cutout")
    void rootModelUsesCutout() throws IOException {
        JsonObject json = ModelJsonTestUtils.resolveBlockModel(MODELS_DIR, ROOT_MODEL);
        assertEquals("cutout", json.get("render_type").getAsString(),
                ROOT_MODEL + " must resolve render_type=cutout since it has no fluid elements.");
    }

    @Test
    @DisplayName("Resolved _head_faucet model has exactly 6 elements")
    void rootModelHasSixElements() throws IOException {
        JsonObject json = ModelJsonTestUtils.resolveBlockModel(MODELS_DIR, ROOT_MODEL);
        JsonArray elements = json.getAsJsonArray("elements");
        assertNotNull(elements, ROOT_MODEL + " must resolve an 'elements' array.");
        assertEquals(6, elements.size(),
                ROOT_MODEL + " must resolve exactly 6 elements: 4 walls + faucet handle + spout.");
    }

    @Test
    @DisplayName("Resolved _head_faucet model does not contain fluid elements")
    void rootModelHasNoFluidElements() throws IOException {
        String resolved = ModelJsonTestUtils.resolveBlockModel(MODELS_DIR, ROOT_MODEL).toString();
        assertFalse(resolved.contains("\"tintindex\":0"),
                ROOT_MODEL + " must not use fluid tint 0; lining tint 1 does not tint water.");
        assertFalse(resolved.contains("#fluid"),
                ROOT_MODEL + " must not reference #fluid texture; no fluid is in this model.");
    }

    @Test
    @DisplayName("Resolved _head_faucet model has faucet spout element at [7,11,3]..[9,12,4]")
    void rootModelHasFaucetSpoutElement() throws IOException {
        JsonObject json = ModelJsonTestUtils.resolveBlockModel(MODELS_DIR, ROOT_MODEL);
        JsonArray elements = json.getAsJsonArray("elements");
        JsonObject spout = elements.get(5).getAsJsonObject();
        JsonArray from = spout.getAsJsonArray("from");
        JsonArray to = spout.getAsJsonArray("to");
        assertEquals(7, from.get(0).getAsInt(), "spout from[x]");
        assertEquals(11, from.get(1).getAsInt(), "spout from[y]");
        assertEquals(3, from.get(2).getAsInt(), "spout from[z]");
        assertEquals(9, to.get(0).getAsInt(), "spout to[x]");
        assertEquals(12, to.get(1).getAsInt(), "spout to[y]");
        assertEquals(4, to.get(2).getAsInt(), "spout to[z]");
    }

    @Test
    @DisplayName("All variant _head_faucet models inherit from the optimized template")
    void variantModelsInheritFromTemplate() throws IOException {
        for (String variant : ALL_VARIANTS) {
            String filename = "bathtub_" + variant + "_head_faucet.json";
            JsonObject json = ModelJsonTestUtils.readJson(MODELS_DIR.resolve(filename));
            assertEquals(EXPECTED_PARENT_REF, json.get("parent").getAsString(),
                    filename + " must inherit from '" + EXPECTED_PARENT_REF + "'.");
        }
    }

    @Test
    @DisplayName("Variant models have texture #0 and particle overrides")
    void variantModelsHaveTextureOverrides() throws IOException {
        for (String variant : ALL_VARIANTS) {
            String filename = "bathtub_" + variant + "_head_faucet.json";
            JsonObject json = ModelJsonTestUtils.readJson(MODELS_DIR.resolve(filename));
            assertTrue(json.has("textures"),
                    filename + " must have a textures block with material overrides.");
            JsonObject textures = json.getAsJsonObject("textures");
            assertTrue(textures.has("0"), filename + " must override texture #0.");
            assertTrue(textures.has("particle"), filename + " must override particle texture.");
            assertEquals(textures.get("0").getAsString(), textures.get("particle").getAsString(),
                    filename + " texture #0 and particle must match.");
            if (!"white".equals(variant)) {
                assertFalse(textures.get("0").getAsString().contains("white_concrete"),
                        filename + " must use the " + variant + " texture.");
            }
        }
    }

    @Test
    @DisplayName("Variant wrappers do not define their own elements")
    void variantModelsHaveNoElements() throws IOException {
        for (String variant : ALL_VARIANTS) {
            String filename = "bathtub_" + variant + "_head_faucet.json";
            JsonObject json = ModelJsonTestUtils.readJson(MODELS_DIR.resolve(filename));
            assertFalse(json.has("elements"),
                    filename + " must inherit elements from the shared template.");
        }
    }
}
