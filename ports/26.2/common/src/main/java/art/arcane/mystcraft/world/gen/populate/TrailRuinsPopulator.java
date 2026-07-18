package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trail ruins populator that generates buried archaeological structures. Ruins
 * consist of mostly buried terracotta and gravel structures with suspicious
 * gravel blocks. Approximately 1 per 16 chunks.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk
 * loads.
 */
public class TrailRuinsPopulator implements IPopulate {

  private static final int CHUNKS_BETWEEN = 16;

  private static final BlockState[] TERRACOTTA_BLOCKS = {
      Blocks.TERRACOTTA.defaultBlockState(),
      Blocks.DYED_TERRACOTTA.white().defaultBlockState(),
      Blocks.DYED_TERRACOTTA.lightGray().defaultBlockState(),
      Blocks.DYED_TERRACOTTA.gray().defaultBlockState(),
      Blocks.DYED_TERRACOTTA.brown().defaultBlockState(),
      Blocks.DYED_TERRACOTTA.red().defaultBlockState(),
      Blocks.DYED_TERRACOTTA.orange().defaultBlockState(),
      Blocks.DYED_TERRACOTTA.yellow().defaultBlockState()
  };
  private final long seed;
  private final float spawnChance;

  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public TrailRuinsPopulator(long seed) {
    this(seed, null);
  }

  public TrailRuinsPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spawnChance = PopulatorConfig.chanceFrom(params, 1.0f, 0);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    if (random.nextFloat() > spawnChance) {
      return;
    }

    int chunkX = chunkPos.getX() >> 4;
    int chunkZ = chunkPos.getZ() >> 4;
    chunkMinX = chunkX << 4;
    chunkMaxX = chunkMinX + 15;
    chunkMinZ = chunkZ << 4;
    chunkMaxZ = chunkMinZ + 15;

    if (chunkX % CHUNKS_BETWEEN != 0 || chunkZ % CHUNKS_BETWEEN != 0) {
      return;
    }

    if (random.nextFloat() > 0.5f) {
      return;
    }

    int x = chunkPos.getX() + random.nextInt(16);
    int z = chunkPos.getZ() + random.nextInt(16);

    int y = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z);

    BlockPos surfacePos = new BlockPos(x, y, z);
    if (!isInChunk(surfacePos)) {
      return;
    }
    if (world.getBlockState(surfacePos).is(Blocks.WATER)) {
      return;
    }

    BlockPos pos = new BlockPos(x, y - 3, z);

    generateRuins(world, random, pos);
  }

  private boolean isInChunk(BlockPos pos) {
    return pos.getX() >= chunkMinX && pos.getX() <= chunkMaxX &&
        pos.getZ() >= chunkMinZ && pos.getZ() <= chunkMaxZ;
  }

  private void safeSetBlock(WorldGenLevel world, BlockPos pos, BlockState state) {
    if (isInChunk(pos)) {
      world.setBlock(pos, state, 2);
    }
  }

  private BlockState safeGetBlockState(WorldGenLevel world, BlockPos pos) {
    if (isInChunk(pos)) {
      return world.getBlockState(pos);
    }
    return Blocks.AIR.defaultBlockState();
  }

  private void generateRuins(WorldGenLevel world, RandomSource random, BlockPos pos) {

    int radius = 6 + random.nextInt(4);

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {

        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > radius) {
          continue;
        }

        if (random.nextFloat() < 0.4f) {
          continue;
        }

        int dy = random.nextInt(4) - 2;
        BlockPos ruinPos = pos.offset(dx, dy, dz);

        BlockState ruinBlock;
        float blockChoice = random.nextFloat();

        if (blockChoice < 0.6f) {

          ruinBlock = TERRACOTTA_BLOCKS[random.nextInt(TERRACOTTA_BLOCKS.length)];
        } else if (blockChoice < 0.85f) {

          ruinBlock = Blocks.GRAVEL.defaultBlockState();
        } else {

          ruinBlock = Blocks.SUSPICIOUS_GRAVEL.defaultBlockState();
        }

        safeSetBlock(world, ruinPos, ruinBlock);

        if (random.nextFloat() < 0.3f) {
          BlockPos abovePos = ruinPos.above();
          if (random.nextFloat() < 0.7f) {
            safeSetBlock(world, abovePos, Blocks.GRAVEL.defaultBlockState());
          } else {
            safeSetBlock(world, abovePos, Blocks.SUSPICIOUS_GRAVEL.defaultBlockState());
          }
        }
      }
    }

    for (int i = 0; i < 3; i++) {
      int dx = random.nextInt(radius * 2) - radius;
      int dz = random.nextInt(radius * 2) - radius;

      BlockPos checkPos = new BlockPos(pos.getX() + dx, 0, pos.getZ() + dz);
      if (!isInChunk(checkPos)) {
        continue;
      }

      int surfaceY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
          pos.getX() + dx, pos.getZ() + dz);

      BlockPos potPos = new BlockPos(pos.getX() + dx, surfaceY, pos.getZ() + dz);

      if (world.getBlockState(potPos).isAir() && world.getBlockState(potPos.below()).isSolid()) {
        safeSetBlock(world, potPos, Blocks.DECORATED_POT.defaultBlockState());
      }
    }

    if (random.nextFloat() < 0.7f) {
      boolean horizontal = random.nextBoolean();
      int pathLength = 8 + random.nextInt(8);

      for (int i = 0; i < pathLength; i++) {
        int dx = horizontal ? i - pathLength / 2 : random.nextInt(3) - 1;
        int dz = horizontal ? random.nextInt(3) - 1 : i - pathLength / 2;

        BlockPos pathPos = pos.offset(dx, 0, dz);

        BlockState pathBlock = random.nextFloat() < 0.3f ?
            Blocks.MOSSY_COBBLESTONE.defaultBlockState() :
            Blocks.COBBLESTONE.defaultBlockState();

        safeSetBlock(world, pathPos, pathBlock);
      }
    }

    for (int i = 0; i < 5; i++) {
      int dx = random.nextInt(radius * 2) - radius;
      int dz = random.nextInt(radius * 2) - radius;

      BlockPos checkPos = new BlockPos(pos.getX() + dx, 0, pos.getZ() + dz);
      if (!isInChunk(checkPos)) {
        continue;
      }

      int surfaceY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
          pos.getX() + dx, pos.getZ() + dz);

      BlockPos cropPos = new BlockPos(pos.getX() + dx, surfaceY, pos.getZ() + dz);

      if (world.getBlockState(cropPos).isAir() && world.getBlockState(cropPos.below()).is(Blocks.GRASS_BLOCK)) {
        safeSetBlock(world, cropPos, Blocks.WHEAT.defaultBlockState());
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:trail_ruins";
  }
}
