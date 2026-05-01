package art.arcane.mystcraft.data;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Mutable runtime accumulator for ink-mixer affinity contributions.
 * <p>
 * As the player drops items into the mixer each one's {@link InkAffinity.Entry}
 * is added to the blend. The blend is then frozen into NBT when the link panel
 * is built so the resulting page (and any downstream booster pack crafted with
 * the ink) can use it to bias symbol rolls in
 * {@code SymbolRegistry.getRandomWeightedWithAffinity}.
 */
public final class InkBlend {

  /**
   * Per-entry weight cap to keep one item type from saturating the blend.
   */
  public static final float WEIGHT_CAP = 5.0f;

  private static final String TAG_TIER = "tier";
  private static final String TAG_TOTAL = "total";
  private static final String TAG_SYMBOLS = "symbols";
  private static final String TAG_CATEGORIES = "categories";
  private static final String TAG_POEM = "poem";
  private static final String TAG_LINKPROPS = "linkprops";

  private final Map<ResourceLocation, Float> symbolWeights = new HashMap<>();
  private final Map<SymbolCategory, Float> categoryWeights = new HashMap<>();
  private final Map<String, Float> poemTokenWeights = new HashMap<>();
  private final Map<String, Float> linkPropertyWeights = new HashMap<>();
  private int tierBonus = 0;
  private float totalContribution = 0f;

  public InkBlend() {
  }

  /**
   * Convenience constructor: build an instance from NBT.
   */
  @NotNull
  public static InkBlend fromTag(@Nullable CompoundTag tag) {
    InkBlend blend = new InkBlend();
    blend.fromNbt(tag);
    return blend;
  }

  private static <K> void addTo(Map<K, Float> dst, Map<K, Float> src, float scale) {
    for (Map.Entry<K, Float> e : src.entrySet()) {
      Float existing = dst.get(e.getKey());
      float total = (existing == null ? 0f : existing) + e.getValue() * scale;
      if (total > WEIGHT_CAP) total = WEIGHT_CAP;
      dst.put(e.getKey(), total);
    }
  }

  private static void addToString(Map<String, Float> dst, Map<String, Float> src, float scale) {
    for (Map.Entry<String, Float> e : src.entrySet()) {
      String key = e.getKey().toLowerCase(Locale.ROOT);
      Float existing = dst.get(key);
      float total = (existing == null ? 0f : existing) + e.getValue() * scale;
      if (total > WEIGHT_CAP) total = WEIGHT_CAP;
      dst.put(key, total);
    }
  }

  /**
   * Adds the given affinity {@code count} times. Weights accumulate additively
   * (capped at {@link #WEIGHT_CAP}); tier bonus takes the maximum of
   * contributions.
   */
  public synchronized void add(@NotNull InkAffinity.Entry entry, int count) {
    if (entry == null || entry == InkAffinity.EMPTY || count <= 0) {
      return;
    }
    float scale = entry.amountPerItem() <= 0f ? count : entry.amountPerItem() * count;
    if (entry.tierBonus() > tierBonus) {
      tierBonus = entry.tierBonus();
    }
    addTo(symbolWeights, entry.symbolWeights(), scale);
    addTo(categoryWeights, entry.categoryWeights(), scale);
    addToString(poemTokenWeights, entry.poemTokenWeights(), scale);
    addToString(linkPropertyWeights, entry.linkPropertyWeights(), scale);
    totalContribution += scale;
  }

  /**
   * Merges another blend into this one. Useful when a booster pack crafted with
   * two themed inks is consumed.
   */
  public synchronized void merge(@NotNull InkBlend other) {
    if (other == null || other.isEmpty()) return;
    if (other.tierBonus > this.tierBonus) {
      this.tierBonus = other.tierBonus;
    }
    addTo(symbolWeights, other.symbolWeights, 1f);
    addTo(categoryWeights, other.categoryWeights, 1f);
    addToString(poemTokenWeights, other.poemTokenWeights, 1f);
    addToString(linkPropertyWeights, other.linkPropertyWeights, 1f);
    totalContribution += other.totalContribution;
  }

  /**
   * Wipes all accumulated weights.
   */
  public synchronized void clear() {
    symbolWeights.clear();
    categoryWeights.clear();
    poemTokenWeights.clear();
    linkPropertyWeights.clear();
    tierBonus = 0;
    totalContribution = 0f;
  }

  public synchronized boolean isEmpty() {
    return symbolWeights.isEmpty() && categoryWeights.isEmpty()
        && poemTokenWeights.isEmpty() && linkPropertyWeights.isEmpty()
        && tierBonus == 0;
  }

  public synchronized int tierBonus() {
    return tierBonus;
  }

  public synchronized float totalContribution() {
    return totalContribution;
  }

  public synchronized Map<ResourceLocation, Float> symbolWeights() {
    return Collections.unmodifiableMap(new HashMap<>(symbolWeights));
  }

  public synchronized Map<SymbolCategory, Float> categoryWeights() {
    return Collections.unmodifiableMap(new HashMap<>(categoryWeights));
  }

  public synchronized Map<String, Float> poemTokenWeights() {
    return Collections.unmodifiableMap(new HashMap<>(poemTokenWeights));
  }

  public synchronized Map<String, Float> linkPropertyWeights() {
    return Collections.unmodifiableMap(new HashMap<>(linkPropertyWeights));
  }

  /**
   * Symbol weight for a specific id (0 if absent).
   */
  public synchronized float symbolWeight(@Nullable ResourceLocation id) {
    if (id == null) return 0f;
    Float v = symbolWeights.get(id);
    return v == null ? 0f : v;
  }

  /**
   * Category weight (0 if absent).
   */
  public synchronized float categoryWeight(@Nullable SymbolCategory cat) {
    if (cat == null) return 0f;
    Float v = categoryWeights.get(cat);
    return v == null ? 0f : v;
  }

  /**
   * Sums poem-token weights for any tokens this poem array matches
   * (case-insensitive).
   */
  public synchronized float poemTokenSum(@Nullable String[] poem) {
    if (poem == null || poem.length == 0 || poemTokenWeights.isEmpty()) {
      return 0f;
    }
    float total = 0f;
    for (String word : poem) {
      if (word == null) continue;
      Float v = poemTokenWeights.get(word.trim().toLowerCase(Locale.ROOT));
      if (v != null) total += v;

      Float exact = poemTokenWeights.get(word.trim());
      if (exact != null) total += exact;
    }
    return total;
  }

  /**
   * Link-property probability accumulated for the given property (capped at
   * 1.0).
   */
  public synchronized float linkPropertyProbability(@NotNull String property) {
    Float v = linkPropertyWeights.get(property);
    return v == null ? 0f : Math.min(1f, v);
  }

  public synchronized CompoundTag toNbt() {
    CompoundTag tag = new CompoundTag();
    if (tierBonus != 0) tag.putInt(TAG_TIER, tierBonus);
    if (totalContribution > 0f) tag.putFloat(TAG_TOTAL, totalContribution);
    if (!symbolWeights.isEmpty()) {
      ListTag list = new ListTag();
      for (Map.Entry<ResourceLocation, Float> e : symbolWeights.entrySet()) {
        CompoundTag entry = new CompoundTag();
        entry.putString("id", e.getKey().toString());
        entry.putFloat("w", e.getValue());
        list.add(entry);
      }
      tag.put(TAG_SYMBOLS, list);
    }
    if (!categoryWeights.isEmpty()) {
      CompoundTag map = new CompoundTag();
      for (Map.Entry<SymbolCategory, Float> e : categoryWeights.entrySet()) {
        map.putFloat(e.getKey().getName(), e.getValue());
      }
      tag.put(TAG_CATEGORIES, map);
    }
    if (!poemTokenWeights.isEmpty()) {
      CompoundTag map = new CompoundTag();
      for (Map.Entry<String, Float> e : poemTokenWeights.entrySet()) {
        map.putFloat(e.getKey(), e.getValue());
      }
      tag.put(TAG_POEM, map);
    }
    if (!linkPropertyWeights.isEmpty()) {
      CompoundTag map = new CompoundTag();
      for (Map.Entry<String, Float> e : linkPropertyWeights.entrySet()) {
        map.putFloat(e.getKey(), e.getValue());
      }
      tag.put(TAG_LINKPROPS, map);
    }
    return tag;
  }

  /**
   * Replaces this blend's contents with the data deserialised from
   * {@code tag}.
   */
  public synchronized void fromNbt(@Nullable CompoundTag tag) {
    clear();
    if (tag == null || tag.isEmpty()) return;
    if (tag.contains(TAG_TIER)) tierBonus = tag.getInt(TAG_TIER);
    if (tag.contains(TAG_TOTAL)) totalContribution = tag.getFloat(TAG_TOTAL);
    if (tag.contains(TAG_SYMBOLS, Tag.TAG_LIST)) {
      ListTag list = tag.getList(TAG_SYMBOLS, Tag.TAG_COMPOUND);
      for (int i = 0; i < list.size(); i++) {
        CompoundTag entry = list.getCompound(i);
        ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
        if (id != null) {
          symbolWeights.put(id, entry.getFloat("w"));
        }
      }
    }
    if (tag.contains(TAG_CATEGORIES, Tag.TAG_COMPOUND)) {
      CompoundTag map = tag.getCompound(TAG_CATEGORIES);
      for (String key : map.getAllKeys()) {
        SymbolCategory cat = SymbolCategory.fromName(key);
        if (cat != null) categoryWeights.put(cat, map.getFloat(key));
      }
    }
    if (tag.contains(TAG_POEM, Tag.TAG_COMPOUND)) {
      CompoundTag map = tag.getCompound(TAG_POEM);
      for (String key : map.getAllKeys()) {
        poemTokenWeights.put(key, map.getFloat(key));
      }
    }
    if (tag.contains(TAG_LINKPROPS, Tag.TAG_COMPOUND)) {
      CompoundTag map = tag.getCompound(TAG_LINKPROPS);
      for (String key : map.getAllKeys()) {
        linkPropertyWeights.put(key, map.getFloat(key));
      }
    }
  }

  /**
   * Quick debug print used by tooltips during dev only.
   */
  @Override
  public synchronized String toString() {
    StringBuilder sb = new StringBuilder("InkBlend[");
    sb.append("tier=").append(tierBonus);
    sb.append(", total=").append(String.format(Locale.ROOT, "%.2f", totalContribution));
    if (!symbolWeights.isEmpty())
      sb.append(", symbols=").append(symbolWeights.size());
    if (!categoryWeights.isEmpty())
      sb.append(", categories=").append(categoryWeights.size());
    if (!poemTokenWeights.isEmpty())
      sb.append(", poem=").append(poemTokenWeights.size());
    if (!linkPropertyWeights.isEmpty())
      sb.append(", linkProps=").append(linkPropertyWeights.size());
    sb.append(']');
    return sb.toString();
  }
}
