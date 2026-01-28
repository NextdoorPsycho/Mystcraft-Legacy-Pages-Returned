package art.arcane.mystcraft.datapack.grammar;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.grammar.CFGGrammarGenerator;
import art.arcane.mystcraft.grammar.CFGRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies datapack-provided grammar rule definitions.
 */
public final class GrammarDatapackLoader {

    private static Map<ResourceLocation, JsonElement> cachedRules = Map.of();

    private GrammarDatapackLoader() {}

    public static void setRules(Map<ResourceLocation, JsonElement> rules) {
        cachedRules = rules != null ? rules : Map.of();
    }

    public static Map<ResourceLocation, JsonElement> getRules() {
        return cachedRules;
    }

    public static void applyRules(Map<ResourceLocation, JsonElement> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }

        for (Map.Entry<ResourceLocation, JsonElement> entry : rules.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject json = entry.getValue().getAsJsonObject();
            boolean replace = GsonHelper.getAsBoolean(json, "replace", false);
            JsonArray ruleArray = GsonHelper.getAsJsonArray(json, "rules", new JsonArray());

            if (replace) {
                Set<ResourceLocation> parents = new HashSet<>();
                for (JsonElement ruleElement : ruleArray) {
                    if (!ruleElement.isJsonObject()) continue;
                    JsonObject ruleObj = ruleElement.getAsJsonObject();
                    ResourceLocation parent = parseId(GsonHelper.getAsString(ruleObj, "parent", null));
                    if (parent != null) {
                        parents.add(parent);
                    }
                }
                for (ResourceLocation parent : parents) {
                    CFGGrammarGenerator.removeRulesForParent(parent);
                }
            }

            for (JsonElement ruleElement : ruleArray) {
                if (!ruleElement.isJsonObject()) continue;
                JsonObject ruleObj = ruleElement.getAsJsonObject();
                ResourceLocation parent = parseId(GsonHelper.getAsString(ruleObj, "parent", null));
                if (parent == null) {
                    Mystcraft.LOGGER.warn("[GrammarDatapack] Rule missing parent in {}", entry.getKey());
                    continue;
                }
                Integer rank = ruleObj.has("rank") ? GsonHelper.getAsInt(ruleObj, "rank") : null;
                List<ResourceLocation> children = new ArrayList<>();
                if (ruleObj.has("children")) {
                    JsonArray childrenArray = GsonHelper.getAsJsonArray(ruleObj, "children");
                    for (JsonElement childElement : childrenArray) {
                        ResourceLocation child = parseId(childElement.getAsString());
                        if (child == null) {
                            Mystcraft.LOGGER.warn("[GrammarDatapack] Invalid child token in {}", entry.getKey());
                            children.clear();
                            break;
                        }
                        children.add(child);
                    }
                }
                CFGRule rule = new CFGRule(parent, children, rank);
                CFGGrammarGenerator.registerRule(rule);
            }
        }
    }

    private static ResourceLocation parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        if (parsed != null) {
            return parsed;
        }
        return new ResourceLocation(Mystcraft.MOD_ID, raw);
    }
}
