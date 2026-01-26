package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.IPopulate;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Forge event handler that manages chunk population for Mystcraft Ages.
 * This handler calls all registered IPopulate functions after chunk generation.
 *
 * In 1.20.2, chunk population works differently than in 1.12.2:
 * - We hook into ChunkEvent.Load to perform population
 * - We track which chunks have been populated to avoid double-processing
 * - Population includes ores, decorations, dungeons, and other features
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class AgePopulationHandler {

    // Track populated chunks per dimension to avoid double-processing
    // Uses chunk position encoded as long (x << 32 | z)
    private static final Set<Long> populatedChunks = new HashSet<>();

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

        // Check if this chunk has already been populated
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        long chunkKey = encodeChunkKey(serverLevel.dimension().location().hashCode(), chunkX, chunkZ);

        if (populatedChunks.contains(chunkKey)) {
            return;
        }

        // Mark chunk as populated before processing to prevent recursive calls
        populatedChunks.add(chunkKey);

        // Perform population
        populateChunk(ageGen, serverLevel, chunkX, chunkZ);
    }

    /**
     * Performs population on a chunk using the AgeChunkGenerator's registered populators.
     */
    private static void populateChunk(AgeChunkGenerator generator, ServerLevel level, int chunkX, int chunkZ) {
        AgeDirectorImpl director = generator.getDirector();
        if (director == null) {
            return;
        }

        List<IPopulate> populators = director.getPopulateFunctions();
        if (populators.isEmpty()) {
            return;
        }

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

        // Call all registered population functions
        for (IPopulate populator : populators) {
            try {
                populator.populate(level, random, chunkPos);
            } catch (Exception e) {
                Mystcraft.LOGGER.error("Error during population with {}: {}",
                        populator.getIdentifier(), e.getMessage());
            }
        }
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
