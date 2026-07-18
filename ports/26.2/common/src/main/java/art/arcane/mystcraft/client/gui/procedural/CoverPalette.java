package art.arcane.mystcraft.client.gui.procedural;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps cover item identifiers to a base color + grain pattern used by
 * {@link BookTextureFactory} when generating the open-book cover image.
 * <p>
 * This is a hand-curated table covering the items the Book Binder accepts by
 * default ({@code minecraft:leather}, {@code mystcraft:folder}) plus the common
 * alternatives players are likely to configure. Unknown items fall back to the
 * leather palette so generation never fails.
 */
public final class CoverPalette {

  private static final Map<Identifier, Entry> ENTRIES = new HashMap<>();
  private static final Entry DEFAULT = new Entry(0x6B3F1A, 0x4A2A10, 0xC79A4D, Grain.LEATHER);

  static {
    register("minecraft:leather", new Entry(0x6B3F1A, 0x4A2A10, 0xC79A4D, Grain.LEATHER));
    register("mystcraft:folder", new Entry(0x1F2A55, 0x131A38, 0xC79A4D, Grain.LEATHER));

    register("minecraft:oak_planks", new Entry(0xB78953, 0x8C5F33, 0x4A2A10, Grain.WOOD));
    register("minecraft:spruce_planks", new Entry(0x7A5836, 0x573D24, 0xC79A4D, Grain.WOOD));
    register("minecraft:birch_planks", new Entry(0xD7C497, 0xB09869, 0x6B3F1A, Grain.WOOD));
    register("minecraft:jungle_planks", new Entry(0xA47648, 0x7B5532, 0xC79A4D, Grain.WOOD));
    register("minecraft:acacia_planks", new Entry(0xB45935, 0x8A3F22, 0x2E1606, Grain.WOOD));
    register("minecraft:dark_oak_planks", new Entry(0x402919, 0x281710, 0xC79A4D, Grain.WOOD));
    register("minecraft:mangrove_planks", new Entry(0x763C3B, 0x542927, 0xC79A4D, Grain.WOOD));
    register("minecraft:cherry_planks", new Entry(0xE0A4A0, 0xB57979, 0x6B3F1A, Grain.WOOD));
    register("minecraft:bamboo_planks", new Entry(0xC4A55B, 0x96763A, 0x4A2A10, Grain.WOOD));
    register("minecraft:crimson_planks", new Entry(0x6A3050, 0x47213A, 0xC79A4D, Grain.WOOD));
    register("minecraft:warped_planks", new Entry(0x2C7378, 0x1B5256, 0xC79A4D, Grain.WOOD));

    register("minecraft:paper", new Entry(0xEDE5C8, 0xC9BE93, 0x6B3F1A, Grain.PAPER));
    register("minecraft:book", new Entry(0x6B3F1A, 0x4A2A10, 0xC79A4D, Grain.LEATHER));
    register("minecraft:writable_book", new Entry(0x7A4A22, 0x5A3315, 0xC79A4D, Grain.LEATHER));
    register("minecraft:written_book", new Entry(0x5A2C13, 0x3D1C0A, 0xE6C778, Grain.LEATHER));

    register("minecraft:stone", new Entry(0x808080, 0x5A5A5A, 0xC0C0C0, Grain.STONE));
    register("minecraft:cobblestone", new Entry(0x6E6E6E, 0x4F4F4F, 0xA8A8A8, Grain.STONE));
    register("minecraft:deepslate", new Entry(0x363639, 0x222226, 0x707074, Grain.STONE));
    register("minecraft:blackstone", new Entry(0x2A252B, 0x191519, 0x807078, Grain.STONE));

    register("minecraft:iron_ingot", new Entry(0xD8D8D8, 0xA0A0A0, 0xF0F0F0, Grain.METAL));
    register("minecraft:gold_ingot", new Entry(0xF7D54B, 0xC79A1F, 0xFFEB80, Grain.METAL));
    register("minecraft:copper_ingot", new Entry(0xC87E5F, 0x8E563E, 0xE3A07F, Grain.METAL));
    register("minecraft:netherite_ingot", new Entry(0x423A39, 0x2A2424, 0x6E5F5C, Grain.METAL));

    register("minecraft:diamond", new Entry(0x4ADBC4, 0x2EA295, 0xCFFFF7, Grain.CRYSTAL));
    register("minecraft:emerald", new Entry(0x35C054, 0x208B36, 0xB8FFC6, Grain.CRYSTAL));
    register("minecraft:amethyst_shard", new Entry(0x9968C9, 0x6E458D, 0xE7CCFF, Grain.CRYSTAL));
    register("minecraft:quartz", new Entry(0xE9E1D2, 0xB7AC97, 0xFFFFFF, Grain.CRYSTAL));

    register("minecraft:white_wool", new Entry(0xE4E4E4, 0xB8B8B8, 0x6B3F1A, Grain.FLAT));
    register("minecraft:black_wool", new Entry(0x191919, 0x0A0A0A, 0xC79A4D, Grain.FLAT));
    register("minecraft:red_wool", new Entry(0xA32B26, 0x6E1B17, 0xF0D688, Grain.FLAT));
    register("minecraft:blue_wool", new Entry(0x35399D, 0x21256B, 0xE6C778, Grain.FLAT));
  }
  private CoverPalette() {
  }

  private static void register(@NotNull String id, @NotNull Entry entry) {
    ENTRIES.put(Identifier.parse(id), entry);
  }

  /**
   * Registers (or overrides) a cover entry. Datapacks / addon mods may call
   * this from client setup to introduce their own cover items.
   */
  public static void register(@NotNull Identifier id, @NotNull Entry entry) {
    ENTRIES.put(id, entry);
  }

  /**
   * Looks up a cover entry by item ID. Returns {@link #defaultEntry()} if the
   * item has no registered palette so that generation never fails for
   * configured cover items the palette doesn't know about.
   */
  @NotNull
  public static Entry get(@Nullable Identifier coverItemId) {
    if (coverItemId == null) return DEFAULT;
    Entry entry = ENTRIES.get(coverItemId);
    return entry != null ? entry : DEFAULT;
  }

  /**
   * Default cover entry (warm leather) — used for unknown / missing cover NBT.
   */
  @NotNull
  public static Entry defaultEntry() {
    return DEFAULT;
  }

  /**
   * Whether the given item ID has a hand-curated palette entry.
   */
  public static boolean hasEntry(@Nullable Identifier coverItemId) {
    return coverItemId != null && ENTRIES.containsKey(coverItemId);
  }

  /**
   * Available grain styles drawn over the base color.
   */
  public enum Grain {
    /**
     * Smooth leather with subtle pore stippling.
     */
    LEATHER,
    /**
     * Vertical wood-grain streaks.
     */
    WOOD,
    /**
     * Cream parchment with horizontal ruled lines.
     */
    PAPER,
    /**
     * Stone speckle texture.
     */
    STONE,
    /**
     * Smooth metallic sheen.
     */
    METAL,
    /**
     * Crystalline / faceted highlights.
     */
    CRYSTAL,
    /**
     * No overlay — flat color with edge bevel only.
     */
    FLAT
  }

  /**
   * Immutable per-cover descriptor.
   *
   * @param baseColor   Primary cover color (0xRRGGBB).
   * @param accentColor Secondary color used for grain overlays / spine
   *                    shading.
   * @param trimColor   Color of the spine trim, ribbon, and bookmark.
   * @param grain       Grain style to overlay on the base color.
   */
  public record Entry(int baseColor, int accentColor, int trimColor,
                      Grain grain) {

    public Entry {

      baseColor &= 0xFFFFFF;
      accentColor &= 0xFFFFFF;
      trimColor &= 0xFFFFFF;
    }
  }
}
