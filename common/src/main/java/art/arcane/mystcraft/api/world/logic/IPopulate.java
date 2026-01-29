package art.arcane.mystcraft.api.world.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for population functions that add decorations to generated terrain.
 * Examples include trees, flowers, ores, and custom decorations.
 * <p>
 * In 1.20.2+, population happens during the FEATURES chunk status via
 * applyBiomeDecoration(), NOT after chunk loading. The WorldGenLevel
 * parameter is typically a WorldGenRegion which provides a limited view
 * of the world optimized for world generation.
 * <p>
 * Important: WorldGenLevel.setBlock() during worldgen does NOT trigger
 * lighting updates or neighbor chunk loads like ServerLevel.setBlock() does.
 * This is the proper way to place blocks during world generation.
 */
public interface IPopulate {

  /**
   * Populates a chunk with decorations.
   * Called during the FEATURES chunk status via applyBiomeDecoration().
   *
   * @param world    The world gen level (typically a WorldGenRegion during generation)
   * @param random   The random source for this chunk
   * @param chunkPos The position of the chunk being populated (block coordinates of chunk corner)
   */
  void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos);

  /**
   * Gets the population function identifier.
   *
   * @return The identifier name
   */
  String getIdentifier();

  /**
   * Checks if a block position is within the writable area for the given chunk.
   * The writable area extends 1 chunk (16 blocks) in each direction from the chunk boundaries,
   * matching the WorldGenRegion's write radius for feature generation.
   *
   * @param pos      The position to check
   * @param chunkPos The chunk origin (block coordinates of chunk corner)
   * @return true if the position is within the writable area
   */
  default boolean isInWritableArea(BlockPos pos, BlockPos chunkPos) {
    int chunkMinX = chunkPos.getX();
    int chunkMinZ = chunkPos.getZ();
    // Allow writing within the chunk and 1 chunk border in each direction
    return pos.getX() >= chunkMinX - 16 && pos.getX() < chunkMinX + 32 &&
        pos.getZ() >= chunkMinZ - 16 && pos.getZ() < chunkMinZ + 32;
  }

  /**
   * Safely places a block only if the position is within the writable area.
   * Use this instead of world.setBlock() in populators to avoid "far chunk" errors.
   *
   * @param world    The world gen level
   * @param pos      The position to place the block
   * @param state    The block state to place
   * @param chunkPos The chunk origin (block coordinates of chunk corner)
   * @return true if the block was placed
   */
  default boolean safeSetBlock(WorldGenLevel world, BlockPos pos, BlockState state, BlockPos chunkPos) {
    if (isInWritableArea(pos, chunkPos)) {
      world.setBlock(pos, state, 2);
      return true;
    }
    return false;
  }
}
