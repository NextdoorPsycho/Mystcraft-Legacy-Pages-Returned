package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.MystcraftConstants;
import art.arcane.mystcraft.villager.SymbolPageLootFunction;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

/**
 * Typed common definitions for Mystcraft loot-function registrations.
 */
public final class ModLootFunctions {
  public static final Identifier SYMBOL_PAGE_ID = MystcraftConstants.loc("symbol_page");

  private ModLootFunctions() {
  }

  /**
   * Supplies common loot-function codecs to a loader-owned registry adapter.
   * Fabric can register directly in the built-in registry; Forge can forward
   * the supplier to its deferred registry.
   */
  public static void register(Registrar registrar) {
    Objects.requireNonNull(registrar, "registrar");
    registrar.register(SYMBOL_PAGE_ID, () -> SymbolPageLootFunction.MAP_CODEC);
  }

  @FunctionalInterface
  public interface Registrar {
    void register(
        Identifier id,
        Supplier<MapCodec<? extends LootItemFunction>> codecSupplier
    );
  }
}
