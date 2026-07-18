package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.IPopulate;
import art.arcane.mystcraft.util.ChunkStatusCompat;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.*;

/**
 * DISABLED: This handler is no longer used.
 * <p>
 * Population is now handled correctly during chunk generation via
 * AgeChunkGenerator.applyBiomeDecoration() which is called during the FEATURES
 * chunk status - the proper time for world generation.
 * <p>
 * This class is kept for reference but is not registered as an event handler.
 */
public class AgePopulationHandler {

  private static final Set<Long> populatedChunks = new HashSet<>();

  private static final Deque<PendingPopulation> pendingQueue = new ArrayDeque<>();
  private static final int MAX_POPULATION_DEPTH = 1;

  private static volatile boolean isPopulating = false;
  private static volatile int currentDepth = 0;

  /**
   * Handles chunk load events to perform population for Mystcraft ages. Called
   * from platform-specific event wrappers if re-enabled.
   */
  public static void onChunkLoad(ServerLevel serverLevel, ChunkAccess chunk) {
    ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
    if (!(generator instanceof AgeChunkGenerator ageGen)) {
      return;
    }

    if (chunk == null) {
      return;
    }

    if (!ChunkStatusCompat.isFull(chunk)) {
      return;
    }

    int chunkX = chunk.getPos().x();
    int chunkZ = chunk.getPos().z();
    long chunkKey = encodeChunkKey(serverLevel.dimension().identifier().hashCode(), chunkX, chunkZ);

    if (populatedChunks.contains(chunkKey)) {
      return;
    }

    populatedChunks.add(chunkKey);

    if (isPopulating || currentDepth >= MAX_POPULATION_DEPTH) {
      Mystcraft.LOGGER.debug("[Population] Queuing chunk [{}, {}] - already populating (depth={})", chunkX, chunkZ, currentDepth);
      pendingQueue.addLast(new PendingPopulation(ageGen, serverLevel, chunkX, chunkZ));
      return;
    }

    try {
      isPopulating = true;
      currentDepth++;
      Mystcraft.LOGGER.debug("[Population] Begin populating chunk [{}, {}] (depth={}, queueSize={})",
          chunkX, chunkZ, currentDepth, pendingQueue.size());
      populateChunk(ageGen, serverLevel, chunkX, chunkZ);
    } finally {
      currentDepth--;
      isPopulating = false;

      int queueSize = pendingQueue.size();
      if (queueSize > 0) {
        Mystcraft.LOGGER.debug("[Population] Processing {} queued chunks after chunk [{}, {}]", queueSize, chunkX, chunkZ);
      }
      processPendingQueue();
    }
  }

  private static void processPendingQueue() {
    while (!pendingQueue.isEmpty() && !isPopulating) {
      PendingPopulation pending = pendingQueue.pollFirst();
      if (pending != null) {
        try {
          isPopulating = true;
          currentDepth++;
          populateChunk(pending.generator, pending.level, pending.chunkX, pending.chunkZ);
        } finally {
          currentDepth--;
          isPopulating = false;
        }
      }
    }
  }

  private static void populateChunk(AgeChunkGenerator generator, ServerLevel level, int chunkX, int chunkZ) {
    AgeDirectorImpl director = generator.getDirector();
    if (director == null) {
      Mystcraft.LOGGER.debug("[Population] No director for chunk [{}, {}]", chunkX, chunkZ);
      return;
    }

    List<IPopulate> populators = director.getPopulateFunctions();
    if (populators.isEmpty()) {
      Mystcraft.LOGGER.debug("[Population] No populators registered for chunk [{}, {}]", chunkX, chunkZ);
      return;
    }

    Mystcraft.LOGGER.debug("[Population] Starting chunk [{}, {}] with {} populators", chunkX, chunkZ, populators.size());

    int x = chunkX * 16;
    int z = chunkZ * 16;
    BlockPos chunkPos = new BlockPos(x, 0, z);

    long worldSeed = level.getSeed();
    RandomSource random = RandomSource.create();
    long k = random.nextLong() / 2L * 2L + 1L;
    long l = random.nextLong() / 2L * 2L + 1L;
    random.setSeed((long) chunkX * k + (long) chunkZ * l ^ worldSeed);

    Mystcraft.LOGGER.info("[Population] Populators for chunk [{}, {}]: {}", chunkX, chunkZ,
        populators.stream().map(IPopulate::getIdentifier).toList());

    long totalStart = System.currentTimeMillis();
    int populatorIndex = 0;
    for (IPopulate populator : populators) {
      populatorIndex++;
      String id = populator.getIdentifier();
      Mystcraft.LOGGER.info("[Population] >>> STARTING populator {}/{}: {} on chunk [{}, {}]",
          populatorIndex, populators.size(), id, chunkX, chunkZ);
      try {
        long start = System.currentTimeMillis();
        populator.populate(level, random, chunkPos);
        long elapsed = System.currentTimeMillis() - start;
        Mystcraft.LOGGER.info("[Population] <<< FINISHED populator {}: {} in {}ms on chunk [{}, {}]",
            populatorIndex, id, elapsed, chunkX, chunkZ);
        if (elapsed > 50) {
          Mystcraft.LOGGER.warn("[Population] SLOW populator {} took {}ms on chunk [{}, {}]",
              id, elapsed, chunkX, chunkZ);
        }
      } catch (Exception e) {
        Mystcraft.LOGGER.error("[Population] !!! ERROR in populator {}: {} on chunk [{}, {}]: {}",
            populatorIndex, id, chunkX, chunkZ, e.getMessage(), e);
      }
    }
    long totalElapsed = System.currentTimeMillis() - totalStart;
    if (totalElapsed > 200) {
      Mystcraft.LOGGER.warn("[Population] SLOW chunk [{}, {}] total population took {}ms with {} populators",
          chunkX, chunkZ, totalElapsed, populators.size());
    }
    Mystcraft.LOGGER.info("[Population] Completed chunk [{}, {}] in {}ms", chunkX, chunkZ, totalElapsed);
  }

  private static long encodeChunkKey(int dimensionHash, int chunkX, int chunkZ) {
    return ((long) dimensionHash << 48) | ((long) (chunkX & 0xFFFFFF) << 24) | (long) (chunkZ & 0xFFFFFF);
  }

  /**
   * Clears population tracking for a dimension.
   */
  public static void clearDimensionTracking(int dimensionHash) {
    populatedChunks.removeIf(key -> (key >> 48) == dimensionHash);
  }

  /**
   * Clears all population tracking.
   */
  public static void clearAllTracking() {
    populatedChunks.clear();
  }

  private record PendingPopulation(AgeChunkGenerator generator,
                                   ServerLevel level, int chunkX, int chunkZ) {
  }
}
