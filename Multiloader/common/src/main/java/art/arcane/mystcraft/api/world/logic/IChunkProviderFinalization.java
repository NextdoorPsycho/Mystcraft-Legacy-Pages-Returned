package art.arcane.mystcraft.api.world.logic;

import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Interface for chunk finalization logic that runs after all terrain generation.
 * Used for final modifications before the chunk is marked as generated.
 */
public interface IChunkProviderFinalization {

  /**
   * Performs final modifications to a completed chunk.
   * Called after terrain generation and all alterations are complete.
   *
   * @param chunk  The completed chunk
   * @param chunkX The chunk X coordinate
   * @param chunkZ The chunk Z coordinate
   */
  void finalizeChunk(ChunkAccess chunk, int chunkX, int chunkZ);
}
