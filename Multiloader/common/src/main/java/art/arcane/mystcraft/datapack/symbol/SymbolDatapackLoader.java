package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.datapack.grammar.GrammarDatapackLoader;
import art.arcane.mystcraft.grammar.CFGGrammarGenerator;
import art.arcane.mystcraft.grammar.GrammarRules;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Applies datapack symbol definitions to the live registry.
 */
public final class SymbolDatapackLoader {

    private SymbolDatapackLoader() {}

    public static void apply(Map<ResourceLocation, JsonElement> elements,
                             Map<ResourceLocation, JsonElement> grammarRules) {
        SymbolRegistry.resetToStatic();
        CFGGrammarGenerator.reset();
        GrammarRules.registerBaseRules();
        GrammarDatapackLoader.applyRules(grammarRules);

        int applied = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : elements.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            ResourceLocation id = entry.getKey();
            JsonObject json = entry.getValue().getAsJsonObject();
            SymbolDefinition definition = SymbolDefinition.fromJson(id, json);
            if (definition == null) {
                continue;
            }
            DataSymbol symbol = new DataSymbol(
                    definition.id,
                    definition.category,
                    definition.cardRank,
                    definition.instabilityCost,
                    definition.poem,
                    definition.allowRandom,
                    definition.canDuplicate,
                    definition.grammarMode,
                    definition.grammarToken,
                    definition.grammarRank,
                    definition.logic
            );
            boolean registered = SymbolRegistry.register(symbol, definition.replace);
            if (registered) {
                applied++;
            }
        }

        CFGGrammarGenerator.buildGrammar();
        SymbolRegistry.freeze();

        Mystcraft.LOGGER.info("[Datapack] Applied {} symbol definitions", applied);
    }
}
