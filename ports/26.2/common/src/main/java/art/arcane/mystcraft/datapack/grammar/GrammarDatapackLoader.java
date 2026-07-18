package art.arcane.mystcraft.datapack.grammar;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.grammar.CFGGrammarGenerator;
import art.arcane.mystcraft.grammar.CFGRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.*;

/**
 * Applies datapack-provided grammar rule definitions.
 */
public final class GrammarDatapackLoader {

  private static Map<Identifier, JsonElement> cachedRules = Map.of();

  private GrammarDatapackLoader() {
  }

  public static Map<Identifier, JsonElement> getRules() {
    return cachedRules;
  }

  public static void setRules(Map<Identifier, JsonElement> rules) {
    cachedRules = rules != null ? rules : Map.of();
  }

  public static void applyRules(Map<Identifier, JsonElement> rules) {
    if (rules == null || rules.isEmpty()) {
      return;
    }

    for (Map.Entry<Identifier, JsonElement> entry : rules.entrySet()) {
      if (!entry.getValue().isJsonObject()) continue;
      JsonObject json = entry.getValue().getAsJsonObject();
      boolean replace = GsonHelper.getAsBoolean(json, "replace", false);
      JsonArray ruleArray = GsonHelper.getAsJsonArray(json, "rules", new JsonArray());

      if (replace) {
        Set<Identifier> parents = new HashSet<>();
        for (JsonElement ruleElement : ruleArray) {
          if (!ruleElement.isJsonObject()) continue;
          JsonObject ruleObj = ruleElement.getAsJsonObject();
          Identifier parent = parseId(GsonHelper.getAsString(ruleObj, "parent", null));
          if (parent != null) {
            parents.add(parent);
          }
        }
        for (Identifier parent : parents) {
          CFGGrammarGenerator.removeRulesForParent(parent);
        }
      }

      for (JsonElement ruleElement : ruleArray) {
        if (!ruleElement.isJsonObject()) continue;
        JsonObject ruleObj = ruleElement.getAsJsonObject();
        Identifier parent = parseId(GsonHelper.getAsString(ruleObj, "parent", null));
        if (parent == null) {
          Mystcraft.LOGGER.warn("[GrammarDatapack] Rule missing parent in {}", entry.getKey());
          continue;
        }
        Integer rank = ruleObj.has("rank") ? GsonHelper.getAsInt(ruleObj, "rank") : null;
        List<Identifier> children = new ArrayList<>();
        if (ruleObj.has("children")) {
          JsonArray childrenArray = GsonHelper.getAsJsonArray(ruleObj, "children");
          for (JsonElement childElement : childrenArray) {
            Identifier child = parseId(childElement.getAsString());
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

  private static Identifier parseId(String raw) {
    if (raw == null || raw.isBlank()) return null;
    Identifier parsed = Identifier.tryParse(raw);
    if (parsed != null) {
      return parsed;
    }
    return Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, raw);
  }
}
