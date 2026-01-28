package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.GrammarBindingMode;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.datapack.grammar.GrammarDatapackLoader;
import art.arcane.mystcraft.datapack.symbol.DataSymbol;
import art.arcane.mystcraft.datapack.symbol.SymbolDatapackLoader;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.grammar.CFGGrammarGenerator;
import art.arcane.mystcraft.grammar.CFGRule;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MystcraftDatapackGameTests {

    @GameTest(template = "empty")
    public void datapack_symbols_loaded(GameTestHelper helper) {
        assertSymbolExists("test_terrain");
        assertSymbolExists("test_biome_controller");
        assertSymbolExists("test_biome");
        assertSymbolExists("test_weather");
        assertSymbolExists("test_lighting");
        assertSymbolExists("test_sun");
        assertSymbolExists("test_moon");
        assertSymbolExists("test_stars");
        assertSymbolExists("test_visual");
        assertSymbolExists("test_effect");
        assertSymbolExists("test_feature_large");
        assertSymbolExists("test_feature_medium");
        assertSymbolExists("test_feature_small");
        assertSymbolExists("test_sea");
        assertSymbolExists("test_logic");
        assertSymbolExists("test_override");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void datapack_logic_registers_populator_and_alteration(GameTestHelper helper) {
        ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "test_logic");
        IAgeSymbol symbol = SymbolRegistry.get(id);
        if (symbol == null) {
            helper.fail("Missing datapack symbol: " + id);
            return;
        }

        AgeDirectorImpl director = new AgeDirectorImpl(1234L);
        symbol.registerLogic(director, 1234L);

        if (!director.areSpikesEnabled()) {
            helper.fail("Expected spikes flag to be enabled by test_logic");
            return;
        }
        if (!director.areCavesEnabled()) {
            helper.fail("Expected caves flag to be enabled by test_logic");
            return;
        }
        if (director.getPopulateFunctions().isEmpty()) {
            helper.fail("Expected populate functions to be registered by test_logic");
            return;
        }
        if (director.getTerrainAlterations().isEmpty()) {
            helper.fail("Expected terrain alterations to be registered by test_logic");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public void datapack_grammar_rules_loaded(GameTestHelper helper) {
        ResourceLocation parent = new ResourceLocation(Mystcraft.MOD_ID, "test_parent");
        List<CFGRule> rules = CFGGrammarGenerator.getAllRules(parent);
        if (rules == null || rules.isEmpty()) {
            helper.fail("Expected grammar rules for " + parent);
            return;
        }

        ResourceLocation child = new ResourceLocation(Mystcraft.MOD_ID, "test_child");
        boolean found = rules.stream().anyMatch(rule -> rule.getValues().contains(child));
        if (!found) {
            helper.fail("Expected grammar rule " + parent + " -> " + child);
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public void datapack_override_replaces_symbol(GameTestHelper helper) {
        ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "test_override");
        reloadFromResources(helper.getLevel().getServer().getResourceManager());

        // Simulate override via register with replace
        SymbolRegistry.resetToStatic();
        DataSymbol base = new DataSymbol(
                id,
                SymbolCategory.SPECIAL,
                4,
                0.0f,
                new String[]{"Base", "Symbol", "Test", "A"},
                true,
                false,
                GrammarBindingMode.DISABLED,
                null,
                null,
                List.of()
        );
        SymbolRegistry.register(base, false);

        DataSymbol replacement = new DataSymbol(
                id,
                SymbolCategory.SPECIAL,
                4,
                0.0f,
                new String[]{"Override", "Symbol", "Test", "B"},
                true,
                false,
                GrammarBindingMode.DISABLED,
                null,
                null,
                List.of()
        );
        SymbolRegistry.register(replacement, true);

        IAgeSymbol result = SymbolRegistry.get(id);
        String[] poem = result != null ? result.getPoem() : null;
        if (poem == null || poem.length == 0 || !"Override".equals(poem[0])) {
            helper.fail("Expected override to replace symbol data");
            return;
        }

        reloadFromResources(helper.getLevel().getServer().getResourceManager());
        helper.succeed();
    }

    private static void assertSymbolExists(String path) {
        ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, path);
        if (!SymbolRegistry.contains(id)) {
            throw new IllegalStateException("Missing datapack symbol: " + id);
        }
    }

    private static void reloadFromResources(ResourceManager manager) {
        Map<ResourceLocation, JsonElement> symbols = loadJsonResources(manager, "mystcraft/symbols");
        Map<ResourceLocation, JsonElement> grammar = loadJsonResources(manager, "mystcraft/grammar");
        GrammarDatapackLoader.setRules(grammar);
        SymbolDatapackLoader.apply(symbols, grammar);
    }

    private static Map<ResourceLocation, JsonElement> loadJsonResources(ResourceManager manager, String folder) {
        Map<ResourceLocation, JsonElement> out = new HashMap<>();
        Map<ResourceLocation, Resource> resources = manager.listResources(folder,
                location -> location.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            String path = fileId.getPath();
            String prefix = folder + "/";
            if (!path.startsWith(prefix) || !path.endsWith(".json")) {
                continue;
            }
            String idPath = path.substring(prefix.length(), path.length() - 5);
            ResourceLocation id = new ResourceLocation(fileId.getNamespace(), idPath);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                out.put(id, json);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load datapack resource " + fileId, e);
            }
        }
        return out;
    }
}
