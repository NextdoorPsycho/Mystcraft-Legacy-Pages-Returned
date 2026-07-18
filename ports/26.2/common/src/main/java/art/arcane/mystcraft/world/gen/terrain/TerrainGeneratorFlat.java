package art.arcane.mystcraft.world.gen.terrain;

import art.arcane.mystcraft.api.world.AgeDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Flat terrain generator that creates a simple flat world. Does not use noise
 * interpolation - fills directly.
 */
public class TerrainGeneratorFlat extends TerrainGeneratorBase {

  public TerrainGeneratorFlat(AgeDirector controller, long seed) {
    super(controller, seed);
  }

  @Override
  public void generateTerrain(int chunkX, int chunkZ, ChunkAccess chunk, RandomSource random) {
    int groundLevel = controller.getAverageGroundLevel();
    int seaLevel = controller.getSeaLevel();
    boolean hasSea = controller.hasSea();

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = chunk.getMinY(); y < chunk.getMaxY(); y++) {
          BlockState block;

          if (y == chunk.getMinY()) {

            block = Blocks.BEDROCK.defaultBlockState();
          } else if (y < groundLevel) {

            block = terrainBlock;
          } else if (hasSea && y < seaLevel) {

            block = seaBlock;
          } else {

            block = Blocks.AIR.defaultBlockState();
          }

          pos.set(x, y, z);
          chunk.setBlockState(pos, block, 0);
        }
      }
    }
  }

  @Override
  protected double[] initializeNoiseField(double[] field, int x, int y, int z,
                                          int xSize, int ySize, int zSize) {

    return new double[xSize * ySize * zSize];
  }

  @Override
  public String getType() {
    return "flat";
  }
}
