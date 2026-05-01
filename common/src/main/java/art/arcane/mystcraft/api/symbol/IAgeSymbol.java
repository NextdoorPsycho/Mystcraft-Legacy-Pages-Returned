package art.arcane.mystcraft.api.symbol;

import art.arcane.mystcraft.api.world.AgeDirector;
import net.minecraft.resources.ResourceLocation;

/**
 * Base interface for Age symbols. Symbols define world properties for Ages
 * (dimensions).
 */
public interface IAgeSymbol {

  /**
   * Gets the unique identifier for this symbol.
   *
   * @return The registry name
   */
  ResourceLocation getRegistryName();

  /**
   * Gets the category this symbol belongs to.
   *
   * @return The symbol category
   */
  SymbolCategory getCategory();

  /**
   * Registers this symbol's logic with the Age director. This is called when
   * building an Age to apply the symbol's effects.
   *
   * @param director The Age director to register logic with
   * @param seed     The world seed for deterministic generation
   */
  void registerLogic(AgeDirector director, long seed);

  /**
   * Gets the localized display name for this symbol.
   *
   * @return The localized name
   */
  String getLocalizedName();

  /**
   * Gets the instability modifier for this symbol. Higher values mean more
   * instability.
   *
   * @param count The number of times this symbol appears
   * @return The instability modifier
   */
  default int instabilityModifier(int count) {
    return 0;
  }

  /**
   * Gets the base instability cost for using this symbol.
   *
   * @return The instability cost
   */
  default float getInstabilityCost() {
    return 0.0f;
  }

  /**
   * Gets the card rank for this symbol (rarity tier). Higher ranks are rarer.
   * Null means not found in loot.
   *
   * @return The card rank, or null if not lootable
   */
  Integer getCardRank();

  /**
   * Gets the poem words associated with this symbol.
   *
   * @return The poem words array
   */
  String[] getPoem();

  /**
   * Whether this symbol can appear in random Age generation.
   *
   * @return true if allowed in random generation
   */
  default boolean allowInRandomGeneration() {
    return true;
  }

  /**
   * Whether this symbol can appear multiple times in an Age. Symbols that
   * accumulate effects (like color targets) should return true.
   *
   * @return true if duplicates are allowed
   */
  default boolean canDuplicate() {
    return false;
  }

  /**
   * Optional presentation override loaded from datapack JSON. When non-null,
   * the procedural-symbol page renderer uses the supplied motif name, palette
   * overlay, and per-word seed pins to compose a curated "hero" treatment
   * instead of the auto-generated category default. Returning {@code null} (the
   * default) keeps the symbol on the auto-generated path.
   *
   * @return the per-symbol presentation override, or {@code null} for
   * category-default rendering
   */
  default art.arcane.mystcraft.datapack.symbol.SymbolDisplay getDisplay() {
    return null;
  }
}
