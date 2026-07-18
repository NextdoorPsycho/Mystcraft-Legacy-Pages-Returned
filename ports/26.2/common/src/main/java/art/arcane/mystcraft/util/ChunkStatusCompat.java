package art.arcane.mystcraft.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Chunk-status helpers for the Minecraft 26.2 API.
 */
public final class ChunkStatusCompat {

  private ChunkStatusCompat() {
  }

  public static boolean isFull(ChunkAccess chunk) {
    return chunk != null && chunk.getPersistedStatus() == ChunkStatus.FULL;
  }

  public static ChunkAccess getChunk(ServerLevel level, int chunkX, int chunkZ, boolean create) {
    if (level == null) {
      return null;
    }
    return level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, create);
  }
}
