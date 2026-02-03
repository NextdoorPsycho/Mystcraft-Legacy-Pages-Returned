package art.arcane.mystcraft.api.world.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Interface for population functions that add decorations to generated terrain.
 * Examples include trees, flowers, ores, and custom decorations.
 *
 * 1.18.2 version - uses java.util.Random instead of RandomSource.
 */
public interface IPopulate {

  /**
   * Populates a chunk with decorations.
   *
   * @param world    The world gen level
   * @param random   The random source for this chunk
   * @param chunkPos The position of the chunk being populated (block coordinates of chunk corner)
   */
  void populate(WorldGenLevel world, Random random, BlockPos chunkPos);

  /**
   * Gets the population function identifier.
   *
   * @return The identifier name
   */
  String getIdentifier();

  /**
   * Checks if a block position is within the writable area for the given chunk.
   *
   * @param pos      The position to check
   * @param chunkPos The chunk origin (block coordinates of chunk corner)
   * @return true if the position is within the writable area
   */
  default boolean isInWritableArea(BlockPos pos, BlockPos chunkPos) {
    int chunkMinX = chunkPos.getX();
    int chunkMinZ = chunkPos.getZ();
    return pos.getX() >= chunkMinX - 16 && pos.getX() < chunkMinX + 32 &&
        pos.getZ() >= chunkMinZ - 16 && pos.getZ() < chunkMinZ + 32;
  }

  /**
   * Safely places a block only if the position is within the writable area.
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
