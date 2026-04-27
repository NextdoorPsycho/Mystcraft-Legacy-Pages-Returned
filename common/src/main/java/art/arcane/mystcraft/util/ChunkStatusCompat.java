package art.arcane.mystcraft.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

/**
 * Chunk status helpers for the supported Minecraft 1.20.1 API.
 */
public final class ChunkStatusCompat {

  private ChunkStatusCompat() {
  }

  public static boolean isFull(ChunkAccess chunk) {
    return chunk != null && chunk.getStatus() == ChunkStatus.FULL;
  }

  public static ChunkAccess getChunk(ServerLevel level, int chunkX, int chunkZ, boolean create) {
    if (level == null) {
      return null;
    }
    return level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, create);
  }
}
