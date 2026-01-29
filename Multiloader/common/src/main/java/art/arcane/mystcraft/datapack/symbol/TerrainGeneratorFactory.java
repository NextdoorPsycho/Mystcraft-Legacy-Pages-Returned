package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.api.world.logic.ITerrainGenerator;
import com.google.gson.JsonObject;

/**
 * Factory for datapack-driven terrain generators.
 */
public interface TerrainGeneratorFactory {
  ITerrainGenerator create(AgeDirector director, long seed, JsonObject params);
}
