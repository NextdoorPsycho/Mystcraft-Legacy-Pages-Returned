package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of datapack terrain alteration factories.
 * 1.18.2 Forge version - stubbed (world.gen.feature not ported).
 */
public final class TerrainAlterationRegistry {

  private static final Map<ResourceLocation, TerrainAlterationFactory> FACTORIES = new HashMap<>();

  private TerrainAlterationRegistry() {
  }

  public static void register(ResourceLocation id, TerrainAlterationFactory factory) {
    if (id == null || factory == null) return;
    TerrainAlterationFactory existing = FACTORIES.putIfAbsent(id, factory);
    if (existing != null) {
      Mystcraft.LOGGER.warn("[TerrainAlterationRegistry] Duplicate alteration factory {} ignored", id);
    }
  }

  public static ITerrainAlteration create(ResourceLocation id, JsonObject json, long seed) {
    TerrainAlterationFactory factory = FACTORIES.get(id);
    if (factory == null) {
      Mystcraft.LOGGER.warn("[TerrainAlterationRegistry] Unknown terrain alteration {}", id);
      return null;
    }
    return factory.create(seed, json);
  }

  public static Map<ResourceLocation, TerrainAlterationFactory> getAll() {
    return Collections.unmodifiableMap(FACTORIES);
  }

  /**
   * Registers default terrain alterations.
   * Note: 1.18.2 - world.gen.feature not ported, so this is a no-op.
   */
  public static void registerDefaults() {
    // 1.18.2: MapGenCavesMyst, MapGenRavineMyst, MapGenFloatingIslands not ported
    // register(myst("caves"), TerrainAlterationRegistry::createCaves);
    // register(myst("ravines"), TerrainAlterationRegistry::createRavines);
    // register(myst("floating_islands"), TerrainAlterationRegistry::createFloatingIslands);
    Mystcraft.LOGGER.info("[TerrainAlterationRegistry] 1.18.2: Default alterations not available");
  }

  private static BlockState resolveBlockState(String raw, Block fallback) {
    if (raw == null || raw.isBlank()) {
      return fallback != null ? fallback.defaultBlockState() : null;
    }
    ResourceLocation id = ResourceLocation.tryParse(raw);
    if (id == null) {
      return fallback != null ? fallback.defaultBlockState() : null;
    }
    if (!Registry.BLOCK.containsKey(id)) {
      return fallback != null ? fallback.defaultBlockState() : null;
    }
    Block block = Registry.BLOCK.get(id);
    return block.defaultBlockState();
  }

  private static ResourceLocation myst(String path) {
    return new ResourceLocation(Mystcraft.MOD_ID, path);
  }
}
