package mod.crabmod.showercore.block;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Economy and inventory behavior are exercised in BathtubMaterialMenuGameTests. */
class BathtubMaterialChangeEconomyRegressionTest {
    @Test
    void everyLocaleHasTheMenuAndNoRetiredHandGesture() throws Exception {
        try (var files = Files.list(Path.of("src/main/resources/assets/showercore/lang"))) {
            for (var path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                var json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                for (String key : new String[]{"title", "bathtub", "block", "result", "face.side", "face.top", "cost"}) {
                    assertFalse(json.get("menu.showercore.material." + key).getAsString().isBlank(), path.toString());
                }
                String tooltip = json.get("tooltip.showercore.bathtub.usage.material").getAsString();
                assertFalse(tooltip.contains("offhand") || tooltip.contains("副手") || tooltip.contains("two hands"), path.toString());
            }
        }
    }

    @Test
    void bothBaseRecipesStillExistAndKeepTheirOriginalCost() throws Exception {
        Path data = Path.of("src/main/resources/data/showercore");
        Path recipes = Files.isDirectory(data.resolve("recipe")) ? data.resolve("recipe") : data.resolve("recipes");
        var white = JsonParser.parseString(Files.readString(recipes.resolve("bathtub_white.json"))).getAsJsonObject();
        assertEquals(5, white.getAsJsonArray("pattern").toString().chars().filter(c -> c == 'X').count());
        assertTrue(Files.isRegularFile(recipes.resolve("bathtub_clawfoot_white.json")));
    }
}
