package art.arcane.mystcraft.api;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

/**
 * Fired on the mod event bus to allow third-party mods to register custom Age symbols.
 *
 * This event fires after all built-in Mystcraft symbols are registered but before
 * the registry is frozen. Symbols registered after this event will be rejected.
 *
 * Subscribe to this event on the MOD event bus:
 * <pre>{@code
 * @Mod.EventBusSubscriber(modid = "mymod", bus = Mod.EventBusSubscriber.Bus.MOD)
 * public class MyModSymbols {
 *     @SubscribeEvent
 *     public static void onRegisterSymbols(RegisterSymbolsEvent event) {
 *         event.register(new MyTerrainSymbol());
 *     }
 * }
 * }</pre>
 *
 * Custom symbols must implement {@link IAgeSymbol} and should use a unique
 * {@link net.minecraft.resources.ResourceLocation} with your mod's namespace.
 */
public class RegisterSymbolsEvent extends Event implements IModBusEvent {

    private int registeredCount = 0;

    /**
     * Registers a custom Age symbol.
     *
     * @param symbol The symbol to register
     * @return true if registration succeeded, false if the ID was already taken or invalid
     */
    public boolean register(IAgeSymbol symbol) {
        boolean success = SymbolRegistry.register(symbol);
        if (success) {
            registeredCount++;
        }
        return success;
    }

    /**
     * Gets the number of symbols registered through this event.
     *
     * @return The count of newly registered symbols
     */
    public int getRegisteredCount() {
        return registeredCount;
    }
}
