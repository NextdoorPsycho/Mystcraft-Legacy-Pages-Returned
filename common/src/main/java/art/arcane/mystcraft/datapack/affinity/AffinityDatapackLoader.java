package art.arcane.mystcraft.datapack.affinity;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.data.InkAffinity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses and registers ink-affinity JSON entries shipped under
 * {@code data/<namespace>/mystcraft/ink_affinity/}.
 * <p>
 * Schema (every section is optional):
 * <pre>{@code
 * {
 *   "items":            ["minecraft:diamond"],
 *   "tags":             ["forge:gems/diamond"],
 *   "amount_per_item":  1.0,
 *   "tier_bonus":       1,
 *   "symbol_weights": {
 *     "mystcraft:dense_ores":               0.6,
 *     "mystcraft:biome_dripstone_caves":    0.3
 *   },
 *   "category_weights": {
 *     "feature_medium": 0.3,
 *     "feature_large":  0.15
 *   },
 *   "poem_token_weights": {
 *     "stone": 0.2,
 *     "deep":  0.25
 *   },
 *   "link_property_weights": {
 *     "intra_linking": 0.1
 *   }
 * }
 * }</pre>
 * <p>
 * The loader is invoked from
 * {@link
 * art.arcane.mystcraft.datapack.affinity.MystcraftAffinityReloadListener}
 * during the standard datapack reload pipeline.
 */
public final class AffinityDatapackLoader {

  private AffinityDatapackLoader() {
  }

  /**
   * Clears the affinity registry then re-registers every entry parsed from
   * {@code resources}. Called by the reload listener.
   */
  public static void apply(@NotNull Map<ResourceLocation, JsonElement> resources) {
    InkAffinity.clear();
    int loaded = 0;
    int skipped = 0;
    for (Map.Entry<ResourceLocation, JsonElement> e : resources.entrySet()) {
      try {
        if (parseAndRegister(e.getKey(), e.getValue())) {
          loaded++;
        } else {
          skipped++;
        }
      } catch (Exception ex) {
        skipped++;
        Mystcraft.LOGGER.warn("[InkAffinity] Failed to parse {}: {}", e.getKey(), ex.toString());
      }
    }
    Mystcraft.LOGGER.info("[InkAffinity] Loaded {} affinity entries ({} skipped)", loaded, skipped);
  }

  private static boolean parseAndRegister(ResourceLocation id, JsonElement element) {
    if (!element.isJsonObject()) {
      Mystcraft.LOGGER.warn("[InkAffinity] {} is not a JSON object, skipping", id);
      return false;
    }
    JsonObject obj = element.getAsJsonObject();

    float amountPerItem = optFloat(obj, "amount_per_item", 1.0f);
    int tierBonus = optInt(obj, "tier_bonus", 0);

    Map<ResourceLocation, Float> symbolWeights = readSymbolWeights(obj.getAsJsonObject("symbol_weights"));
    Map<SymbolCategory, Float> categoryWeights = readCategoryWeights(obj.getAsJsonObject("category_weights"));
    Map<String, Float> poemTokenWeights = readStringFloatMap(obj.getAsJsonObject("poem_token_weights"));
    Map<String, Float> linkPropertyWeights = readStringFloatMap(obj.getAsJsonObject("link_property_weights"));

    InkAffinity.Entry entry = new InkAffinity.Entry(
        amountPerItem,
        tierBonus,
        symbolWeights,
        categoryWeights,
        poemTokenWeights,
        linkPropertyWeights
    );

    if (entry.isEmpty()) {
      Mystcraft.LOGGER.debug("[InkAffinity] {} has no contributions, skipping", id);
      return false;
    }

    boolean any = false;
    JsonArray items = obj.getAsJsonArray("items");
    if (items != null) {
      for (JsonElement raw : items) {
        if (!raw.isJsonPrimitive()) continue;
        ResourceLocation itemId = ResourceLocation.tryParse(raw.getAsString());
        if (itemId == null) continue;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getDefaultKey())) {

          if (item == null) continue;
        }
        InkAffinity.register(item, entry);
        any = true;
      }
    }

    JsonArray tags = obj.getAsJsonArray("tags");
    if (tags != null) {
      for (JsonElement raw : tags) {
        if (!raw.isJsonPrimitive()) continue;
        ResourceLocation tagId = ResourceLocation.tryParse(raw.getAsString());
        if (tagId == null) continue;
        TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
        InkAffinity.register(tag, entry);
        any = true;
      }
    }

    if (!any) {
      Mystcraft.LOGGER.warn("[InkAffinity] {} declared no items or tags, skipping", id);
      return false;
    }
    return true;
  }

  private static Map<ResourceLocation, Float> readSymbolWeights(JsonObject obj) {
    Map<ResourceLocation, Float> out = new HashMap<>();
    if (obj == null) return out;
    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
      ResourceLocation id = ResourceLocation.tryParse(e.getKey());
      if (id == null) continue;
      out.put(id, e.getValue().getAsFloat());
    }
    return out;
  }

  private static Map<SymbolCategory, Float> readCategoryWeights(JsonObject obj) {
    Map<SymbolCategory, Float> out = new HashMap<>();
    if (obj == null) return out;
    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
      SymbolCategory cat = SymbolCategory.fromName(e.getKey());
      if (cat == null) continue;
      out.put(cat, e.getValue().getAsFloat());
    }
    return out;
  }

  private static Map<String, Float> readStringFloatMap(JsonObject obj) {
    Map<String, Float> out = new HashMap<>();
    if (obj == null) return out;
    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
      out.put(e.getKey(), e.getValue().getAsFloat());
    }
    return out;
  }

  private static float optFloat(JsonObject obj, String key, float def) {
    return obj.has(key) ? obj.get(key).getAsFloat() : def;
  }

  private static int optInt(JsonObject obj, String key, int def) {
    return obj.has(key) ? obj.get(key).getAsInt() : def;
  }
}
