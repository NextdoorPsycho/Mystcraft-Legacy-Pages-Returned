package art.arcane.mystcraft.api.world.logic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Random;

/**
 * Interface for terrain alterations that modify terrain after base generation.
 * Examples include cave carving, ravine generation, and floating island creation.
 *
 * 1.18.2 version - uses java.util.Random instead of RandomSource.
 */
public interface ITerrainAlteration {

  /**
   * Alters the terrain in a chunk after base terrain generation.
   *
   * @param world  The server level (may be null during initial generation)
   * @param chunkX The chunk X coordinate
   * @param chunkZ The chunk Z coordinate
   * @param chunk  The chunk to modify
   * @param random The random source
   */
  void alterTerrain(ServerLevel world, int chunkX, int chunkZ, ChunkAccess chunk, Random random);

  /**
   * Gets the alteration type identifier.
   *
   * @return The type name (e.g., "caves", "ravines", "floating_islands")
   */
  String getType();

  /**
   * Gets the priority for this alteration.
   * Lower values run first. Default is 100.
   *
   * @return The priority value
   */
  default int getPriority() {
    return 100;
  }
}
