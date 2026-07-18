package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.GrammarBindingMode;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed definition for a datapack-provided symbol.
 */
public final class SymbolDefinition {
  public final Identifier id;
  public final boolean replace;
  public final SymbolCategory category;
  public final Integer cardRank;
  public final float instabilityCost;
  public final String[] poem;
  public final String displayName;
  public final boolean allowRandom;
  public final boolean canDuplicate;
  public final GrammarBindingMode grammarMode;
  public final Identifier grammarToken;
  public final Integer grammarRank;
  public final List<SymbolLogic> logic;
  public final SymbolDisplay display;

  private SymbolDefinition(Identifier id,
                           boolean replace,
                           SymbolCategory category,
                           Integer cardRank,
                           float instabilityCost,
                           String[] poem,
                           String displayName,
                           boolean allowRandom,
                           boolean canDuplicate,
                           GrammarBindingMode grammarMode,
                           Identifier grammarToken,
                           Integer grammarRank,
                           List<SymbolLogic> logic,
                           SymbolDisplay display) {
    this.id = id;
    this.replace = replace;
    this.category = category;
    this.cardRank = cardRank;
    this.instabilityCost = instabilityCost;
    this.poem = poem;
    this.displayName = displayName;
    this.allowRandom = allowRandom;
    this.canDuplicate = canDuplicate;
    this.grammarMode = grammarMode;
    this.grammarToken = grammarToken;
    this.grammarRank = grammarRank;
    this.logic = logic;
    this.display = display;
  }

  public static SymbolDefinition fromJson(Identifier id, JsonObject json) {
    boolean replace = GsonHelper.getAsBoolean(json, "replace", false);
    SymbolCategory category = SymbolCategory.fromName(GsonHelper.getAsString(json, "category"));
    if (category == null) {
      Mystcraft.LOGGER.warn("[SymbolDefinition] Invalid category for {}", id);
      return null;
    }

    Integer cardRank = json.has("card_rank") ? GsonHelper.getAsInt(json, "card_rank") : null;
    float instabilityCost = GsonHelper.getAsFloat(json, "instability_cost", 0.0f);

    String[] poem = null;
    if (json.has("poem")) {
      JsonArray poemArray = GsonHelper.getAsJsonArray(json, "poem");
      List<String> words = new ArrayList<>();
      for (JsonElement element : poemArray) {
        words.add(element.getAsString());
      }
      poem = words.toArray(new String[0]);
    }

    boolean allowRandom = GsonHelper.getAsBoolean(json, "allow_random", true);
    boolean canDuplicate = GsonHelper.getAsBoolean(json, "can_duplicate", false);
    String displayName = GsonHelper.getAsString(json, "display_name", null);

    GrammarBindingMode grammarMode = GrammarBindingMode.DEFAULT;
    Identifier grammarToken = null;
    Integer grammarRank = null;
    if (json.has("grammar")) {
      JsonElement grammarElement = json.get("grammar");
      if (grammarElement.isJsonPrimitive() && grammarElement.getAsJsonPrimitive().isBoolean()) {
        boolean enabled = grammarElement.getAsBoolean();
        grammarMode = enabled ? GrammarBindingMode.DEFAULT : GrammarBindingMode.DISABLED;
      } else if (grammarElement.isJsonObject()) {
        JsonObject grammarObj = grammarElement.getAsJsonObject();
        grammarMode = GrammarBindingMode.CUSTOM;
        String tokenRaw = GsonHelper.getAsString(grammarObj, "token");
        grammarToken = Identifier.tryParse(tokenRaw);
        if (grammarToken == null) {
          Mystcraft.LOGGER.warn("[SymbolDefinition] Invalid grammar token {} for {}", tokenRaw, id);
        }
        if (grammarObj.has("rank")) {
          grammarRank = GsonHelper.getAsInt(grammarObj, "rank");
        }
      }
    }

    List<SymbolLogic> logic = new ArrayList<>();
    if (json.has("logic")) {
      JsonArray logicArray = GsonHelper.getAsJsonArray(json, "logic");
      boolean redundantInstabilityLogic = hasRedundantInstabilityLogic(
          logicArray,
          instabilityCost
      );
      for (JsonElement element : logicArray) {
        if (!element.isJsonObject()) continue;
        JsonObject logicObj = element.getAsJsonObject();
        String typeRaw = GsonHelper.getAsString(logicObj, "type", null);
        Identifier typeId = SymbolLogicRegistry.resolveId(typeRaw);
        if (typeId == null) {
          Mystcraft.LOGGER.warn("[SymbolDefinition] Missing logic type for {}", id);
          continue;
        }
        if (redundantInstabilityLogic && isAddInstability(typeId)) {
          continue;
        }
        SymbolLogic parsed = SymbolLogicRegistry.create(typeId, logicObj);
        if (parsed != null) {
          logic.add(parsed);
        }
      }
    }

    return new SymbolDefinition(
        id,
        replace,
        category,
        cardRank,
        instabilityCost,
        poem,
        displayName,
        allowRandom,
        canDuplicate,
        grammarMode,
        grammarToken,
        grammarRank,
        logic,
        SymbolDisplay.fromJson(id, json.get("display"))
    );
  }

  /**
   * Older symbol packs commonly declared the same cost twice: once as
   * {@code instability_cost} for the grammar and once as executable
   * {@code add_instability} logic. The grammar owns the declared base cost;
   * matching executable entries are therefore metadata duplicates. Logic that
   * intentionally adds a different amount remains executable.
   */
  private static boolean hasRedundantInstabilityLogic(
      JsonArray logicArray,
      float instabilityCost
  ) {
    float logicInstability = 0.0f;
    int instabilityEntries = 0;
    for (JsonElement element : logicArray) {
      if (!element.isJsonObject()) {
        continue;
      }
      JsonObject logicObj = element.getAsJsonObject();
      Identifier typeId = SymbolLogicRegistry.resolveId(
          GsonHelper.getAsString(logicObj, "type", null)
      );
      if (!isAddInstability(typeId)) {
        continue;
      }
      logicInstability += GsonHelper.getAsFloat(logicObj, "value", 0.0f);
      instabilityEntries++;
    }
    return instabilityEntries > 0
        && Math.abs(logicInstability - instabilityCost) <= 0.0001f;
  }

  private static boolean isAddInstability(Identifier typeId) {
    return typeId != null
        && Mystcraft.MOD_ID.equals(typeId.getNamespace())
        && "add_instability".equals(typeId.getPath());
  }
}
