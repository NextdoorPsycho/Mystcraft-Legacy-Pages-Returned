package art.arcane.mystcraft.api.world.logic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Interface for terrain alterations that modify terrain after base generation.
 * Examples include cave carving, ravine generation, and floating island creation.
 */
public interface ITerrainAlteration {

  /**
   * Alters the terrain in a chunk after base terrain generation.
   * This is called after the terrain generator has filled the chunk with blocks.
   *
   * @param world  The server level (may be null during initial generation)
   * @param chunkX The chunk X coordinate
   * @param chunkZ The chunk Z coordinate
   * @param chunk  The chunk to modify
   * @param random The random source
   */
  void alterTerrain(ServerLevel world, int chunkX, int chunkZ, ChunkAccess chunk, RandomSource random);

  /**
   * Gets the alteration type identifier.
   *
   * @return The type name (e.g., "caves", "ravines", "floating_islands")
   */
  String getType();

  /**
   * Gets the priority for this alteration.
   * Lower values run first. Default is 100.
   * Caves typically run at 50, ravines at 60, floating islands at 200.
   *
   * @return The priority value
   */
  default int getPriority() {
    return 100;
  }
}
