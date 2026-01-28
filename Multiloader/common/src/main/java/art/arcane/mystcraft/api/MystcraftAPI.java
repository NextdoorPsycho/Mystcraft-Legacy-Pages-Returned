package art.arcane.mystcraft.api;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Public API for interacting with the Mystcraft mod.
 * Third-party mods should use this class to register symbols and query the registry.
 *
 * Symbol registration should be done during the {@link RegisterSymbolsEvent},
 * which fires on the Forge mod event bus during common setup.
 *
 * Example usage in a third-party mod:
 * <pre>{@code
 * @Mod.EventBusSubscriber(modid = "mymod", bus = Mod.EventBusSubscriber.Bus.MOD)
 * public class MyModSymbols {
 *     @SubscribeEvent
 *     public static void onRegisterSymbols(RegisterSymbolsEvent event) {
 *         event.register(new MyCustomTerrainSymbol());
 *         event.register(new MyCustomBiomeSymbol());
 *     }
 * }
 * }</pre>
 */
public final class MystcraftAPI {

    private MystcraftAPI() {}

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
