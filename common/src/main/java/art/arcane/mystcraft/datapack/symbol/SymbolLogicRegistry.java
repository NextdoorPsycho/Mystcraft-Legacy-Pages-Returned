package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of datapack symbol logic types.
 * Mods can register new behavior types here.
 */
public final class SymbolLogicRegistry {

  private static final Map<ResourceLocation, SymbolLogicType> TYPES = new HashMap<>();

  private SymbolLogicRegistry() {
  }

  public static void register(SymbolLogicType type) {
    if (type == null || type.id() == null) {
      return;
    }
    SymbolLogicType existing = TYPES.putIfAbsent(type.id(), type);
    if (existing != null) {
      Mystcraft.LOGGER.warn("[SymbolLogicRegistry] Duplicate logic type {} ignored", type.id());
    }
  }

  public static SymbolLogic create(ResourceLocation id, JsonObject json) {
    SymbolLogicType type = TYPES.get(id);
    if (type == null) {
      Mystcraft.LOGGER.warn("[SymbolLogicRegistry] Unknown logic type {}", id);
      return null;
    }
    return type.parse(json);
  }

  public static Map<ResourceLocation, SymbolLogicType> getAll() {
    return Collections.unmodifiableMap(TYPES);
  }

  public static ResourceLocation resolveId(String raw) {
    if (raw == null || raw.isBlank()) return null;
    String trimmed = raw.trim();
    if (trimmed.indexOf(':') >= 0) {
      return ResourceLocation.tryParse(trimmed);
    }
    return new ResourceLocation(Mystcraft.MOD_ID, trimmed);
  }
}
