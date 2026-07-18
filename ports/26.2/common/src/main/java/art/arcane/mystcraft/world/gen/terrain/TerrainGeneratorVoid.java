package art.arcane.mystcraft.world.gen.terrain;

import art.arcane.mystcraft.api.world.AgeDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Void terrain generator that creates an empty world with only bedrock at y=0.
 */
public class TerrainGeneratorVoid extends TerrainGeneratorBase {

  public TerrainGeneratorVoid(AgeDirector controller, long seed) {
    super(controller, seed);
  }

  @Override
  public void generateTerrain(int chunkX, int chunkZ, ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {

        pos.set(x, chunk.getMinY(), z);
        chunk.setBlockState(pos, Blocks.BEDROCK.defaultBlockState(), 0);

        for (int y = chunk.getMinY() + 1; y < chunk.getMaxY(); y++) {
          pos.set(x, y, z);
          chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), 0);
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
    return "void";
  }
}
