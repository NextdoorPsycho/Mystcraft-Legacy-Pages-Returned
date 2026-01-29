package art.arcane.mystcraft.world.gen.terrain;

import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.api.world.logic.ITerrainGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Base class for terrain generators using noise-based interpolation.
 * Uses a 4x4x8 resolution grid that is interpolated to full block resolution.
 * This matches the original Mystcraft terrain generation algorithm.
 */
public abstract class TerrainGeneratorBase implements ITerrainGenerator {

  // Grid resolution constants
  protected static final int XZ_STEP = 4;    // Blocks per noise grid point in X/Z
  protected static final int Y_STEP = 8;     // Blocks per noise grid point in Y
  protected static final int GRID_WIDTH = 4; // 16 / XZ_STEP
  protected static final int GRID_HEIGHT = 48; // 384 / Y_STEP (full 1.20+ world height)
  // Noise field dimensions (one extra for interpolation)
  protected static final int NOISE_WIDTH = GRID_WIDTH + 1;  // 5
  protected static final int NOISE_HEIGHT = GRID_HEIGHT + 1; // 49
  // Interpolation factors
  protected static final double Y_STEP_FACTOR = 1.0D / Y_STEP;      // 0.125
  protected static final double XZ_STEP_FACTOR = 1.0D / XZ_STEP;    // 0.25
  protected final AgeDirector controller;
  protected final long seed;
  protected BlockState terrainBlock = Blocks.STONE.defaultBlockState();
  protected BlockState seaBlock = Blocks.WATER.defaultBlockState();
  protected double[] noiseField;

  public TerrainGeneratorBase(AgeDirector controller, long seed) {
    this.controller = controller;
    this.seed = seed;
  }

  @Override
  public void generateTerrain(int chunkX, int chunkZ, ChunkAccess chunk, RandomSource random) {
    int seaLevel = controller.getSeaLevel();
    boolean hasSea = controller.hasSea();

    // Initialize noise field for this chunk
    noiseField = initializeNoiseField(noiseField, chunkX * GRID_WIDTH, 0, chunkZ * GRID_WIDTH,
        NOISE_WIDTH, NOISE_HEIGHT, NOISE_WIDTH);

    // Interpolate and place blocks
    interpolateAndPlace(chunk, chunkX, chunkZ, seaLevel, hasSea);
  }

  /**
   * Initializes the noise field for a chunk.
   * Subclasses implement this to provide different terrain shapes.
   *
   * @param field Existing array to reuse, or null
   * @param x     Grid X offset
   * @param y     Grid Y offset
   * @param z     Grid Z offset
   * @param xSize Grid width
   * @param ySize Grid height
   * @param zSize Grid depth
   * @return The initialized noise field
   */
  protected abstract double[] initializeNoiseField(double[] field, int x, int y, int z,
                                                   int xSize, int ySize, int zSize);

  /**
   * Performs trilinear interpolation of the noise field and places blocks.
   */
  protected void interpolateAndPlace(ChunkAccess chunk, int chunkX, int chunkZ, int seaLevel, boolean hasSea) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();

    // Iterate over large grid cells (4x4x8)
    for (int gridX = 0; gridX < GRID_WIDTH; gridX++) {
      for (int gridZ = 0; gridZ < GRID_WIDTH; gridZ++) {
        for (int gridY = 0; gridY < GRID_HEIGHT; gridY++) {

          // Get 8 corner values from noise field
          // Index = x + z * NOISE_WIDTH + y * NOISE_WIDTH * NOISE_WIDTH
          int baseIndex = gridX + gridZ * NOISE_WIDTH;
          int yOffset = NOISE_WIDTH * NOISE_WIDTH;

          double v000 = noiseField[baseIndex + gridY * yOffset];
          double v100 = noiseField[baseIndex + 1 + gridY * yOffset];
          double v010 = noiseField[baseIndex + NOISE_WIDTH + gridY * yOffset];
          double v110 = noiseField[baseIndex + 1 + NOISE_WIDTH + gridY * yOffset];
          double v001 = noiseField[baseIndex + (gridY + 1) * yOffset];
          double v101 = noiseField[baseIndex + 1 + (gridY + 1) * yOffset];
          double v011 = noiseField[baseIndex + NOISE_WIDTH + (gridY + 1) * yOffset];
          double v111 = noiseField[baseIndex + 1 + NOISE_WIDTH + (gridY + 1) * yOffset];

          // Calculate Y derivatives (step per Y block)
          double dy00 = (v001 - v000) * Y_STEP_FACTOR;
          double dy10 = (v101 - v100) * Y_STEP_FACTOR;
          double dy01 = (v011 - v010) * Y_STEP_FACTOR;
          double dy11 = (v111 - v110) * Y_STEP_FACTOR;

          // Interpolate within the 4x4x8 cell
          double cy00 = v000;
          double cy10 = v100;
          double cy01 = v010;
          double cy11 = v110;

          for (int subY = 0; subY < Y_STEP; subY++) {
            int worldY = minY + gridY * Y_STEP + subY;

            // Calculate Z derivatives at this Y level
            double dz0 = (cy01 - cy00) * XZ_STEP_FACTOR;
            double dz1 = (cy11 - cy10) * XZ_STEP_FACTOR;

            double cz0 = cy00;
            double cz1 = cy10;

            for (int subZ = 0; subZ < XZ_STEP; subZ++) {
              int worldZ = gridZ * XZ_STEP + subZ;

              // Calculate X derivative at this Y,Z
              double dx = (cz1 - cz0) * XZ_STEP_FACTOR;
              double density = cz0;

              for (int subX = 0; subX < XZ_STEP; subX++) {
                int worldX = gridX * XZ_STEP + subX;

                // Determine block to place based on density
                BlockState block;
                if (density > 0.0D) {
                  block = terrainBlock;
                } else if (hasSea && worldY < seaLevel) {
                  block = seaBlock;
                } else {
                  block = Blocks.AIR.defaultBlockState();
                }

                // Place bedrock at world bottom
                if (worldY == minY) {
                  block = Blocks.BEDROCK.defaultBlockState();
                }

                pos.set(worldX, worldY, worldZ);
                chunk.setBlockState(pos, block, false);

                density += dx;
              }

              cz0 += dz0;
              cz1 += dz1;
            }

            cy00 += dy00;
            cy10 += dy10;
            cy01 += dy01;
            cy11 += dy11;
          }
        }
      }
    }
  }

  @Override
  public BlockState getTerrainBlock() {
    return terrainBlock;
  }

  @Override
  public void setTerrainBlock(BlockState block) {
    if (block != null) {
      this.terrainBlock = block;
    }
  }

  @Override
  public BlockState getSeaBlock() {
    return seaBlock;
  }

  @Override
  public void setSeaBlock(BlockState block) {
    if (block != null) {
      this.seaBlock = block;
    }
  }
}
