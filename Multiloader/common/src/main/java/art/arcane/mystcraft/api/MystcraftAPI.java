package art.arcane.mystcraft.api;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Public API for interacting with the Mystcraft mod.
 * Third-party mods should use this class to register symbols and query the registry.
 * <p>
 * Symbol definitions are now data-driven and loaded from datapacks under
 * {@code data/<namespace>/mystcraft/symbols/*.json}. Mods that want to add new
 * behavior types should register custom logic types via
 * {@link art.arcane.mystcraft.datapack.symbol.SymbolLogicRegistry}.
 */
public final class MystcraftAPI {

  private MystcraftAPI() {
  }

  /**
   * Gets a registered symbol by its ID.
   *
   * @param id The symbol's ResourceLocation identifier
   * @return The symbol, or null if not found
   */
  public static IAgeSymbol getSymbol(ResourceLocation id) {
    return SymbolRegistry.get(id);
  }

  /**
   * Checks if a symbol is registered.
   *
   * @param id The symbol's ResourceLocation identifier
   * @return true if the symbol exists in the registry
   */
  public static boolean isSymbolRegistered(ResourceLocation id) {
    return SymbolRegistry.contains(id);
  }

  /**
   * Gets the total number of registered symbols.
   *
   * @return The symbol count
   */
  public static int getSymbolCount() {
    return SymbolRegistry.size();
  }

  /**
   * Checks if the symbol registry is frozen (no more registrations allowed).
   *
   * @return true if the registry is frozen
   */
  public static boolean isRegistryFrozen() {
    return SymbolRegistry.isFrozen();
  }
}
