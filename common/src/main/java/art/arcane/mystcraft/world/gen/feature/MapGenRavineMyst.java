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
 * Ravine generator that carves long, deep canyons into terrain. Similar to
 * caves but longer, narrower, and deeper.
 */
public class MapGenRavineMyst implements ITerrainAlteration {

  private static final int RANGE = 8;
  private final long seed;
  private final int rate;
  private final BlockState fillBlock;

  /**
   * Creates a ravine generator with default settings.
   */
  public MapGenRavineMyst(long seed) {
    this(seed, 50, Blocks.AIR.defaultBlockState());
  }

  /**
   * Creates a ravine generator with custom settings.
   */
  public MapGenRavineMyst(long seed, int rate, BlockState fillBlock) {
    this.seed = seed;
    this.rate = Math.max(1, rate);
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
          double startX = originChunkX * 16 + chunkRand.nextInt(16);
          double startY = chunkRand.nextInt(chunkRand.nextInt(40) + 8) + 20;
          double startZ = originChunkZ * 16 + chunkRand.nextInt(16);

          generateRavine(chunk, chunkX, chunkZ, chunkRand, startX, startY, startZ);
        }
      }
    }
  }

  protected RandomSource getChunkRandom(int chunkX, int chunkZ) {
    long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed + 1000L;
    return RandomSource.create(chunkSeed);
  }

  protected void generateRavine(ChunkAccess chunk, int chunkX, int chunkZ,
                                RandomSource rand, double x, double y, double z) {
    double chunkCenterX = chunkX * 16 + 8;
    double chunkCenterZ = chunkZ * 16 + 8;

    float yaw = rand.nextFloat() * (float) Math.PI * 2.0F;
    float pitch = (rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
    float width = (rand.nextFloat() * 2.0F + rand.nextFloat()) * 2.0F;

    int length = 112 - rand.nextInt(28);

    float yawChange = 0.0F;
    float pitchChange = 0.0F;

    for (int segment = 0; segment < length; segment++) {

      double horizRadius = 1.5D + (Mth.sin(segment * (float) Math.PI / length) * width);
      double vertRadius = horizRadius * 3.0D;

      float cosPitch = Mth.cos(pitch);
      float sinPitch = Mth.sin(pitch);
      x += Mth.cos(yaw) * cosPitch;
      y += sinPitch;
      z += Mth.sin(yaw) * cosPitch;

      pitch *= 0.7F;
      pitch += pitchChange * 0.05F;
      yaw += yawChange * 0.05F;

      pitchChange *= 0.8F;
      yawChange *= 0.5F;
      pitchChange += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 2.0F;
      yawChange += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 4.0F;

      double distX = x - chunkCenterX;
      double distZ = z - chunkCenterZ;
      double remaining = length - segment;
      double totalRadius = width + 2.0F + 16.0F;

      if (distX * distX + distZ * distZ - remaining * remaining > totalRadius * totalRadius) {
        return;
      }

      if (x >= chunkCenterX - 16.0D - horizRadius * 2.0D &&
          z >= chunkCenterZ - 16.0D - horizRadius * 2.0D &&
          x <= chunkCenterX + 16.0D + horizRadius * 2.0D &&
          z <= chunkCenterZ + 16.0D + horizRadius * 2.0D) {
        carveRavine(chunk, chunkX, chunkZ, x, y, z, horizRadius, vertRadius);
      }
    }
  }

  protected void carveRavine(ChunkAccess chunk, int chunkX, int chunkZ,
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
    minY = Math.max(chunk.getMinBuildHeight() + 1, minY);
    maxY = Math.min(chunk.getMaxBuildHeight() - 1, maxY);
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

            if (xDist * xDist + yDist * yDist + zDist * zDist < 1.0D) {
              pos.set(x, y, z);
              BlockState existing = chunk.getBlockState(pos);

              if (canCarve(existing, y)) {
                chunk.setBlockState(pos, fillBlock, false);
              }
            }
          }
        }
      }
    }
  }

  protected boolean canCarve(BlockState state, int y) {
    if (state.is(Blocks.BEDROCK)) return false;
    if (state.is(Blocks.WATER) || state.is(Blocks.LAVA)) return false;
    if (y <= 0) return false;
    return !state.isAir();
  }

  @Override
  public String getType() {
    return "ravines";
  }

  @Override
  public int getPriority() {
    return 60;
  }
}
