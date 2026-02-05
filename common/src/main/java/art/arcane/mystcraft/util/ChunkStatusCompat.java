package art.arcane.mystcraft.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Compatibility helper for ChunkStatus across 1.20.1 - 1.20.6 package moves.
 */
public final class ChunkStatusCompat {

  private static final Class<?> STATUS_CLASS = loadClass(
      "net.minecraft.world.level.chunk.ChunkStatus",
      "net.minecraft.world.level.chunk.status.ChunkStatus"
  );
  private static final Object FULL_STATUS = loadStaticField(STATUS_CLASS, "FULL");
  private static final Method GET_CHUNK_WITH_STATUS = findGetChunkMethod();

  private ChunkStatusCompat() {
  }

  public static boolean isFull(ChunkAccess chunk) {
    if (chunk == null) {
      return false;
    }
    Object status = chunk.getStatus();
    if (FULL_STATUS != null && status != null) {
      return status == FULL_STATUS || status.equals(FULL_STATUS);
    }
    return status != null && "full".equalsIgnoreCase(status.toString());
  }

  public static ChunkAccess getChunk(ServerLevel level, int chunkX, int chunkZ, boolean create) {
    if (level == null) {
      return null;
    }
    if (GET_CHUNK_WITH_STATUS != null && FULL_STATUS != null) {
      try {
        return (ChunkAccess) GET_CHUNK_WITH_STATUS.invoke(level, chunkX, chunkZ, FULL_STATUS, create);
      } catch (ReflectiveOperationException ignored) {
      }
    }
    return level.getChunk(chunkX, chunkZ);
  }

  private static Method findGetChunkMethod() {
    if (STATUS_CLASS == null) {
      return null;
    }
    try {
      return ServerLevel.class.getMethod("getChunk", int.class, int.class, STATUS_CLASS, boolean.class);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private static Class<?> loadClass(String... names) {
    for (String name : names) {
      try {
        return Class.forName(name);
      } catch (ClassNotFoundException ignored) {
      }
    }
    return null;
  }

  private static Object loadStaticField(Class<?> clazz, String name) {
    if (clazz == null) {
      return null;
    }
    try {
      Field field = clazz.getField(name);
      return field.get(null);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }
}
