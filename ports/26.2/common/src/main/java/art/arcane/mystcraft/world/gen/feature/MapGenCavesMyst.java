package art.arcane.mystcraft.world.gen.feature;

import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Cave generator that carves tunnels and rooms into terrain.
 */
public class MapGenCavesMyst implements ITerrainAlteration {

  private static final int RANGE = 8;
  private final long seed;
  private final int rate;
  private final int size;
  private final BlockState fillBlock;

  /**
   * Creates a cave generator with default settings.
   */
  public MapGenCavesMyst(long seed) {
    this(seed, 15, 40, Blocks.AIR.defaultBlockState());
  }

  /**
   * Creates a cave generator with custom settings.
   *
   * @param seed      World seed
   * @param rate      Cave frequency (1 in rate chance per chunk)
   * @param size      Maximum cave size
   * @param fillBlock Block to fill caves with (usually air)
   */
  public MapGenCavesMyst(long seed, int rate, int size, BlockState fillBlock) {
    this.seed = seed;
    this.rate = Math.max(1, rate);
    this.size = size;
    this.fillBlock = fillBlock;
  }

  @Override
  public void alterTerrain(ServerLevel world, int chunkX, int chunkZ, ChunkAccess chunk, RandomSource random) {

    for (int dx = -RANGE; dx <= RANGE; dx++) {
      for (int dz = -RANGE; dz <= RANGE; dz++) {
        int originChunkX = chunkX + dx;
        int originChunkZ = chunkZ + dz;

        RandomSource chunkRand = getChunkRandom(originChunkX, originChunkZ);

        if (chunkRand.nextInt(rate) == 0) {

          int startX = originChunkX * 16 + chunkRand.nextInt(16);
          int startY = chunkRand.nextInt(chunkRand.nextInt(120) + 8);
          int startZ = originChunkZ * 16 + chunkRand.nextInt(16);

          int tunnelCount = 1;
          if (chunkRand.nextInt(4) == 0) {

            generateRoom(chunk, chunkX, chunkZ, chunkRand, startX, startY, startZ);
            tunnelCount += chunkRand.nextInt(4);
          }

          for (int t = 0; t < tunnelCount; t++) {
            float yaw = chunkRand.nextFloat() * (float) Math.PI * 2.0F;
            float pitch = (chunkRand.nextFloat() - 0.5F) * 2.0F / 8.0F;
            float radius = chunkRand.nextFloat() * 2.0F + chunkRand.nextFloat();

            if (chunkRand.nextInt(10) == 0) {
              radius *= chunkRand.nextFloat() * chunkRand.nextFloat() * 3.0F + 1.0F;
            }

            generateTunnel(chunk, chunkX, chunkZ, chunkRand,
                startX, startY, startZ, radius, yaw, pitch, 0, size, 1.0D);
          }
        }
      }
    }
  }

  /**
   * Gets a deterministic random source for a chunk.
   */
  protected RandomSource getChunkRandom(int chunkX, int chunkZ) {
    long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
    return RandomSource.create(chunkSeed);
  }

  /**
   * Generates a cave room (larger spherical cavity).
   */
  protected void generateRoom(ChunkAccess chunk, int chunkX, int chunkZ,
                              RandomSource rand, double x, double y, double z) {
    float radius = 1.0F + rand.nextFloat() * 6.0F;
    carveEllipsoid(chunk, chunkX, chunkZ, x, y, z, radius, 0.5D);
  }

  /**
   * Generates a cave tunnel that winds through the terrain.
   */
  protected void generateTunnel(ChunkAccess chunk, int chunkX, int chunkZ,
                                RandomSource rand, double x, double y, double z,
                                float radius, float yaw, float pitch,
                                int currentLength, int maxLength, double heightScale) {
    double chunkCenterX = chunkX * 16 + 8;
    double chunkCenterZ = chunkZ * 16 + 8;

    float yawChange = 0.0F;
    float pitchChange = 0.0F;

    if (maxLength <= 0) {
      int range = RANGE * 16 - 16;
      maxLength = range - rand.nextInt(range / 4);
    }

    int splitPoint = 0;
    if (currentLength == 0) {
      splitPoint = maxLength / 2 + rand.nextInt(maxLength / 4);
    }

    boolean largeTunnel = rand.nextInt(6) == 0;

    for (int length = currentLength; length < maxLength; length++) {
      double horizRadius = 1.5D + (Mth.sin(length * (float) Math.PI / maxLength) * radius);
      double vertRadius = horizRadius * heightScale;

      float cosPitch = Mth.cos(pitch);
      float sinPitch = Mth.sin(pitch);
      x += Mth.cos(yaw) * cosPitch;
      y += sinPitch;
      z += Mth.sin(yaw) * cosPitch;

      if (largeTunnel) {
        pitch *= 0.92F;
      } else {
        pitch *= 0.7F;
      }

      pitch += pitchChange * 0.1F;
      yaw += yawChange * 0.1F;

      pitchChange *= 0.9F;
      yawChange *= 0.75F;
      pitchChange += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 2.0F;
      yawChange += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 4.0F;

      if (splitPoint > 0 && length == splitPoint && radius > 1.0F) {
        generateTunnel(chunk, chunkX, chunkZ, RandomSource.create(rand.nextLong()),
            x, y, z, rand.nextFloat() * 0.5F + 0.5F,
            yaw - ((float) Math.PI / 2F), pitch / 3.0F,
            length, maxLength, 1.0D);
        generateTunnel(chunk, chunkX, chunkZ, RandomSource.create(rand.nextLong()),
            x, y, z, rand.nextFloat() * 0.5F + 0.5F,
            yaw + ((float) Math.PI / 2F), pitch / 3.0F,
            length, maxLength, 1.0D);
        return;
      }

      if (rand.nextInt(4) != 0) {

        double distX = x - chunkCenterX;
        double distZ = z - chunkCenterZ;
        double remaining = maxLength - length;
        double totalRadius = radius + 2.0F + 16.0F;

        if (distX * distX + distZ * distZ - remaining * remaining > totalRadius * totalRadius) {
          return;
        }

        if (x >= chunkCenterX - 16.0D - horizRadius * 2.0D &&
            z >= chunkCenterZ - 16.0D - horizRadius * 2.0D &&
            x <= chunkCenterX + 16.0D + horizRadius * 2.0D &&
            z <= chunkCenterZ + 16.0D + horizRadius * 2.0D) {
          carveEllipsoid(chunk, chunkX, chunkZ, x, y, z, horizRadius, vertRadius);
        }
      }
    }
  }

  /**
   * Carves an ellipsoid cavity at the specified position.
   */
  protected void carveEllipsoid(ChunkAccess chunk, int chunkX, int chunkZ,
                                double centerX, double centerY, double centerZ,
                                double horizRadius, double vertRadius) {
    int minX = Mth.floor(centerX - horizRadius) - chunkX * 16 - 1;
    int maxX = Mth.floor(centerX + horizRadius) - chunkX * 16 + 1;
    int minY = Mth.floor(centerY - vertRadius) - 1;
    int maxY = Mth.floor(centerY + vertRadius) + 1;
    int minZ = Mth.floor(centerZ - horizRadius) - chunkZ * 16 - 1;
    int maxZ = Mth.floor(centerZ + horizRadius) - chunkZ * 16 + 1;

    minX = Math.max(0, minX);
    maxX = Math.min(16, maxX);
    minY = Math.max(chunk.getMinY() + 1, minY);
    maxY = Math.min(chunk.getMaxY() - 1, maxY);
    minZ = Math.max(0, minZ);
    maxZ = Math.min(16, maxZ);

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    for (int x = minX; x < maxX; x++) {
      double xDist = ((chunkX * 16 + x) + 0.5D - centerX) / horizRadius;

      for (int z = minZ; z < maxZ; z++) {
        double zDist = ((chunkZ * 16 + z) + 0.5D - centerZ) / horizRadius;

        if (xDist * xDist + zDist * zDist < 1.0D) {
          for (int y = maxY; y > minY; y--) {
            double yDist = ((y - 1) + 0.5D - centerY) / vertRadius;

            if (yDist > -0.7D && xDist * xDist + yDist * yDist + zDist * zDist < 1.0D) {
              pos.set(x, y, z);
              BlockState existing = chunk.getBlockState(pos);

              if (canCarve(existing, y)) {
                chunk.setBlockState(pos, fillBlock, 0);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Checks if a block can be carved out.
   */
  protected boolean canCarve(BlockState state, int y) {

    if (state.is(Blocks.BEDROCK)) {
      return false;
    }

    if (state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
      return false;
    }

    if (y <= 0) {
      return false;
    }

    return !state.isAir();
  }

  @Override
  public String getType() {
    return "caves";
  }

  @Override
  public int getPriority() {
    return 50;
  }
}
