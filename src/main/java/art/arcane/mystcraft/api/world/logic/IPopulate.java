package art.arcane.mystcraft.api.world.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

/**
 * Interface for population functions that add decorations to generated terrain.
 * Examples include trees, flowers, ores, and custom decorations.
 *
 * In 1.20.2+, population happens during the FEATURES chunk status via
 * applyBiomeDecoration(), NOT after chunk loading. The WorldGenLevel
 * parameter is typically a WorldGenRegion which provides a limited view
 * of the world optimized for world generation.
 *
 * Important: WorldGenLevel.setBlock() during worldgen does NOT trigger
 * lighting updates or neighbor chunk loads like ServerLevel.setBlock() does.
 * This is the proper way to place blocks during world generation.
 */
public interface IPopulate {

    /**
     * Populates a chunk with decorations.
     * Called during the FEATURES chunk status via applyBiomeDecoration().
     *
     * @param world The world gen level (typically a WorldGenRegion during generation)
     * @param random The random source for this chunk
     * @param chunkPos The position of the chunk being populated (block coordinates of chunk corner)
     */
    void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos);

    /**
     * Gets the population function identifier.
     * @return The identifier name
     */
    String getIdentifier();
}
