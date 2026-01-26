package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.IPopulate;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DEPRECATED: This handler is no longer used.
 *
 * Population is now handled correctly during chunk generation via
 * AgeChunkGenerator.applyBiomeDecoration() which is called during
 * the FEATURES chunk status - the proper time for world generation.
 *
 * Using ChunkEvent.Load to populate chunks is incorrect in 1.20.2+
 * because by that point the chunk is fully generated with lighting
 * already calculated. Calling setBlock() on fully-loaded chunks triggers
 * expensive lighting recalculations that can cascade and freeze the game.
 *
 * This class is kept for reference but the event handler is disabled.
 */
// @Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)  // DISABLED
public class AgePopulationHandler {

    // Track populated chunks per dimension to avoid double-processing
    // Uses chunk position encoded as long (x << 32 | z)
    private static final Set<Long> populatedChunks = new HashSet<>();

    // Recursion guard - prevents re-entry during population
    private static volatile boolean isPopulating = false;

    // Queue of chunks to populate when we're done with current population
    private static final Deque<PendingPopulation> pendingQueue = new ArrayDeque<>();

    // Maximum population depth to prevent stack overflow
    private static final int MAX_POPULATION_DEPTH = 1;
    private static volatile int currentDepth = 0;

    /**
     * Handles chunk load events to perform population for Mystcraft ages.
     * Population is performed once per chunk when it first generates.
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
        if (!(generator instanceof AgeChunkGenerator ageGen)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();
        if (chunk == null) {
            return;
        }

        // Only populate fully generated chunks
        if (chunk.getStatus() != ChunkStatus.FULL) {
            return;
        }

        // Check if this chunk has already been populated
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        long chunkKey = encodeChunkKey(serverLevel.dimension().location().hashCode(), chunkX, chunkZ);

        if (populatedChunks.contains(chunkKey)) {
            return;
        }

        // Mark chunk as populated before processing to prevent recursive calls
        populatedChunks.add(chunkKey);

        // If we're already populating, queue this chunk for later
        if (isPopulating || currentDepth >= MAX_POPULATION_DEPTH) {
            Mystcraft.LOGGER.debug("[Population] Queuing chunk [{}, {}] - already populating (depth={})", chunkX, chunkZ, currentDepth);
            pendingQueue.addLast(new PendingPopulation(ageGen, serverLevel, chunkX, chunkZ));
            return;
        }

        // Perform population with recursion guard
        try {
            isPopulating = true;
            currentDepth++;
            Mystcraft.LOGGER.debug("[Population] Begin populating chunk [{}, {}] (depth={}, queueSize={})",
                    chunkX, chunkZ, currentDepth, pendingQueue.size());
            populateChunk(ageGen, serverLevel, chunkX, chunkZ);
        } finally {
            currentDepth--;
            isPopulating = false;

            // Process any pending chunks that were queued during our population
            int queueSize = pendingQueue.size();
            if (queueSize > 0) {
                Mystcraft.LOGGER.debug("[Population] Processing {} queued chunks after chunk [{}, {}]", queueSize, chunkX, chunkZ);
            }
            processPendingQueue();
        }
    }

    /**
     * Process any chunks that were queued during population.
     */
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

    private static class PendingPopulation {
        final AgeChunkGenerator generator;
        final ServerLevel level;
        final int chunkX;
        final int chunkZ;

        PendingPopulation(AgeChunkGenerator generator, ServerLevel level, int chunkX, int chunkZ) {
            this.generator = generator;
            this.level = level;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }

    /**
     * Performs population on a chunk using the AgeChunkGenerator's registered populators.
     */
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

        // Calculate chunk position in world coordinates (block coordinates of chunk corner)
        int x = chunkX * 16;
        int z = chunkZ * 16;
        BlockPos chunkPos = new BlockPos(x, 0, z);

        // Create random source for this chunk (matching legacy behavior)
        long worldSeed = level.getSeed();
        RandomSource random = RandomSource.create();
        long k = random.nextLong() / 2L * 2L + 1L;
        long l = random.nextLong() / 2L * 2L + 1L;
        random.setSeed((long) chunkX * k + (long) chunkZ * l ^ worldSeed);

        // Log all registered populators
        Mystcraft.LOGGER.info("[Population] Populators for chunk [{}, {}]: {}", chunkX, chunkZ,
                populators.stream().map(IPopulate::getIdentifier).toList());

        // Call all registered population functions
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

    /**
     * Encodes dimension, chunk X, and chunk Z into a unique long key.
     */
    private static long encodeChunkKey(int dimensionHash, int chunkX, int chunkZ) {
        // Use dimension hash in upper bits, chunk coords in lower bits
        return ((long) dimensionHash << 48) | ((long) (chunkX & 0xFFFFFF) << 24) | (long) (chunkZ & 0xFFFFFF);
    }

    /**
     * Clears population tracking for a dimension.
     * Called when a dimension is unloaded to prevent memory leaks.
     */
    public static void clearDimensionTracking(int dimensionHash) {
        populatedChunks.removeIf(key -> (key >> 48) == dimensionHash);
    }

    /**
     * Clears all population tracking.
     * Called during server shutdown or world unload.
     */
    public static void clearAllTracking() {
        populatedChunks.clear();
    }
}
