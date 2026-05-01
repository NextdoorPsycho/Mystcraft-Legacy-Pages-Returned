package art.arcane.mystcraft.data;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of "ink affinities" — biases an item contributes to the Ink Mixer
 * besides plain link-property probabilities.
 * <p>
 * An affinity entry has four orthogonal levers (any of which may be empty):
 * <ul>
 *   <li><b>symbolWeights</b> — additive boost for specific symbols</li>
 *   <li><b>categoryWeights</b> — additive boost for whole {@link SymbolCategory}s</li>
 *   <li><b>poemTokenWeights</b> — additive boost for any symbol whose poem
 *       contains the listed word (case-insensitive). Lets datapacks paint
 *       broad themes without enumerating individual symbols.</li>
 *   <li><b>linkPropertyWeights</b> — same probability map that
 *       {@link InkEffects} historically tracked. Migrated here so item
 *       contributions live in one place.</li>
 *   <li><b>tierBonus</b> — integer that lifts the maximum {@code card_rank}
 *       a roll can return (e.g. nether star: +2 unlocks rank-5 symbols).</li>
 * </ul>
 * Items can be registered directly or via item tags. Tag entries are checked
 * if and only if a direct lookup misses.
 * <p>
 * The registry is mutable so the JSON loader can rebuild it on datapack
 * reload. Code-side defaults populated by {@link InkEffects#init()} and the
 * matching JSON files in {@code data/mystcraft/mystcraft/ink_affinity/} are
 * the bundled baseline; addon mods may also call {@link #register} from
 * setup.
 */
public final class InkAffinity {

  /**
   * Canonical empty entry — returned for items with no registered affinity.
   */
  public static final Entry EMPTY = new Entry(0f, 0, Map.of(), Map.of(), Map.of(), Map.of());
  private static final Map<Item, Entry> ITEM_ENTRIES = new HashMap<>();
  private static final Map<TagKey<Item>, Entry> TAG_ENTRIES = new HashMap<>();
  private InkAffinity() {
  }

  /**
   * Registers an affinity entry for a specific item. Replaces any prior entry.
   */
  public static synchronized void register(@NotNull Item item, @NotNull Entry entry) {
    ITEM_ENTRIES.put(item, entry);
  }

  /**
   * Registers an affinity entry for an item tag. Replaces any prior entry.
   */
  public static synchronized void register(@NotNull TagKey<Item> tag, @NotNull Entry entry) {
    TAG_ENTRIES.put(tag, entry);
  }

  /**
   * Removes an item-level entry.
   */
  public static synchronized void unregister(@NotNull Item item) {
    ITEM_ENTRIES.remove(item);
  }

  /**
   * Removes a tag-level entry.
   */
  public static synchronized void unregister(@NotNull TagKey<Item> tag) {
    TAG_ENTRIES.remove(tag);
  }

  /**
   * Drops every registered entry. Called by the JSON loader before
   * re-applying.
   */
  public static synchronized void clear() {
    ITEM_ENTRIES.clear();
    TAG_ENTRIES.clear();
  }

  /**
   * Resolves the affinity entry for an item stack.
   * <p>
   * Order of resolution:
   * <ol>
   *   <li>Direct item entry</li>
   *   <li>First matching tag entry (insertion order)</li>
   *   <li>{@link #EMPTY}</li>
   * </ol>
   */
  @NotNull
  public static synchronized Entry getAffinity(@Nullable ItemStack stack) {
    if (stack == null || stack.isEmpty()) return EMPTY;
    Entry direct = ITEM_ENTRIES.get(stack.getItem());
    if (direct != null) return direct;
    for (Map.Entry<TagKey<Item>, Entry> e : TAG_ENTRIES.entrySet()) {
      if (stack.is(e.getKey())) {
        return e.getValue();
      }
    }
    return EMPTY;
  }

  /**
   * Whether the stack carries any non-empty affinity.
   */
  public static boolean hasAffinity(@Nullable ItemStack stack) {
    Entry e = getAffinity(stack);
    return !e.isEmpty();
  }

  /**
   * Number of registered item-level entries (testing aid).
   */
  public static synchronized int size() {
    return ITEM_ENTRIES.size() + TAG_ENTRIES.size();
  }

  /**
   * Read-only view of all registered item entries (testing aid).
   */
  public static synchronized Map<Item, Entry> itemEntries() {
    return Map.copyOf(ITEM_ENTRIES);
  }

  /**
   * Read-only view of all registered tag entries (testing aid).
   */
  public static synchronized Map<TagKey<Item>, Entry> tagEntries() {
    return Map.copyOf(TAG_ENTRIES);
  }

  /**
   * Immutable per-item / per-tag affinity contribution. All maps are
   * unmodifiable; callers do not mutate them after construction.
   */
  public record Entry(
      float amountPerItem,
      int tierBonus,
      Map<ResourceLocation, Float> symbolWeights,
      Map<SymbolCategory, Float> categoryWeights,
      Map<String, Float> poemTokenWeights,
      Map<String, Float> linkPropertyWeights
  ) {

    public Entry {
      symbolWeights = symbolWeights == null ? Map.of() : Map.copyOf(symbolWeights);
      categoryWeights = categoryWeights == null ? Map.of() : Map.copyOf(categoryWeights);
      poemTokenWeights = poemTokenWeights == null ? Map.of() : normalizeKeys(poemTokenWeights);
      linkPropertyWeights = linkPropertyWeights == null ? Map.of() : Map.copyOf(linkPropertyWeights);
      amountPerItem = Math.max(0f, amountPerItem);
    }

    private static Map<String, Float> normalizeKeys(Map<String, Float> in) {
      Map<String, Float> out = new LinkedHashMap<>(in.size());
      for (Map.Entry<String, Float> e : in.entrySet()) {
        if (e.getKey() == null) continue;
        out.put(e.getKey().trim(), e.getValue());
      }
      return Collections.unmodifiableMap(out);
    }

    /**
     * Returns true iff every contribution is empty / zero (so the entry has no
     * effect when added to a blend).
     */
    public boolean isEmpty() {
      return symbolWeights.isEmpty() && categoryWeights.isEmpty()
          && poemTokenWeights.isEmpty() && linkPropertyWeights.isEmpty()
          && tierBonus == 0;
    }
  }
}
