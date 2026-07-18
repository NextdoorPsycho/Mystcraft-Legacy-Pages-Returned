package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.api.world.logic.ITerrainGenerator;
import art.arcane.mystcraft.world.gen.terrain.ScriptedTerrainGenerator;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of datapack terrain generator factories.
 */
public final class TerrainGeneratorRegistry {

  private static final Map<Identifier, TerrainGeneratorFactory> FACTORIES = new HashMap<>();

  private TerrainGeneratorRegistry() {
  }

  public static void register(Identifier id, TerrainGeneratorFactory factory) {
    if (id == null || factory == null) return;
    TerrainGeneratorFactory existing = FACTORIES.putIfAbsent(id, factory);
    if (existing != null) {
      Mystcraft.LOGGER.warn("[TerrainGeneratorRegistry] Duplicate terrain generator factory {} ignored", id);
    }
  }

  public static ITerrainGenerator create(Identifier id, AgeDirector director, long seed, JsonObject json) {
    TerrainGeneratorFactory factory = FACTORIES.get(id);
    if (factory == null) {
      Mystcraft.LOGGER.warn("[TerrainGeneratorRegistry] Unknown terrain generator {}", id);
      return null;
    }
    try {
      return factory.create(director, seed, json);
    } catch (Exception e) {
      Mystcraft.LOGGER.error("[TerrainGeneratorRegistry] Failed to create terrain generator {}: {}", id, e.getMessage());
      return null;
    }
  }

  public static Map<Identifier, TerrainGeneratorFactory> getAll() {
    return Collections.unmodifiableMap(FACTORIES);
  }

  public static void registerDefaults() {
    register(myst("scripted"), TerrainGeneratorRegistry::createScripted);
  }

  private static ITerrainGenerator createScripted(AgeDirector director, long seed, JsonObject json) {
    return ScriptedTerrainGenerator.fromJson(director, seed, json);
  }

  private static Identifier myst(String path) {
    return Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
  }
}
