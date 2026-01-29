package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ocean monument populator that generates simplified guardian temples.
 * Monuments consist of prismarine structures with elder guardian spawners and treasure.
 * Approximately 1 per 64 chunks, only generates in water.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk loads.
 */
public class OceanMonumentsPopulator implements IPopulate {

  private static final int DEFAULT_CHUNKS_BETWEEN = 64;
  private final long seed;
  private final int chunksBetween;
  private final float spawnChance;
  // Chunk boundaries for current population
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public OceanMonumentsPopulator(long seed) {
    this(seed, null);
  }

  public OceanMonumentsPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.chunksBetween = Math.max(1, PopulatorConfig.getInt(params, "chunks_between", DEFAULT_CHUNKS_BETWEEN));
    this.spawnChance = PopulatorConfig.chanceFrom(params, 1.0f, 0);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    if (random.nextFloat() > spawnChance) {
      return;
    }

    // Only attempt generation in specific chunks based on grid
    int chunkX = chunkPos.getX() >> 4;
    int chunkZ = chunkPos.getZ() >> 4;

    // Set chunk boundaries for this population run
    chunkMinX = chunkX << 4;
    chunkMaxX = chunkMinX + 15;
    chunkMinZ = chunkZ << 4;
    chunkMaxZ = chunkMinZ + 15;

    if (chunkX % chunksBetween != 0 || chunkZ % chunksBetween != 0) {
      return;
    }

    // Random check for generation
    if (random.nextFloat() > 0.2f) {
      return;
    }

    int x = chunkPos.getX() + random.nextInt(16);
    int z = chunkPos.getZ() + random.nextInt(16);

    // Find ocean floor
    int oceanFloor = -1;
    for (int y = 40; y <= 62; y++) {
      BlockPos checkPos = new BlockPos(x, y, z);
      if (!world.getBlockState(checkPos).is(Blocks.WATER)) {
        continue;
      }

      // Check if we have solid ground below and water above
      BlockPos belowPos = checkPos.below();
      if (world.getBlockState(belowPos).isSolid()) {
        // Make sure we have enough water depth (at least 10 blocks)
        boolean hasDepth = true;
        for (int dy = 1; dy <= 10; dy++) {
          if (!world.getBlockState(checkPos.above(dy)).is(Blocks.WATER)) {
            hasDepth = false;
            break;
          }
        }

        if (hasDepth) {
          oceanFloor = y;
          break;
        }
      }
    }

    // Must be in deep water
    if (oceanFloor == -1) {
      return;
    }

    BlockPos pos = new BlockPos(x, oceanFloor, z);
    generateMonument(world, random, pos);
  }

  /**
   * Checks if a position is within the current chunk boundaries.
   * This prevents cascade chunk loading when structures extend beyond chunk edges.
   */
  private boolean isInChunk(BlockPos pos) {
    return pos.getX() >= chunkMinX && pos.getX() <= chunkMaxX &&
        pos.getZ() >= chunkMinZ && pos.getZ() <= chunkMaxZ;
  }

  /**
   * Safe setBlock that only places blocks within current chunk boundaries.
   */
  private void safeSetBlock(WorldGenLevel world, BlockPos pos, BlockState state) {
    if (isInChunk(pos)) {
      world.setBlock(pos, state, 2);
    }
  }

  private void generateMonument(WorldGenLevel world, RandomSource random, BlockPos pos) {
    // Build simplified monument structure (11x11 base, 8 blocks tall)
    int radius = 5;
    int height = 8;

    // Foundation
    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        BlockPos foundationPos = pos.offset(dx, 0, dz);
        safeSetBlock(world, foundationPos, Blocks.PRISMARINE.defaultBlockState());
      }
    }

    // Build walls with dark prismarine
    for (int dy = 1; dy < height; dy++) {
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
          BlockPos buildPos = pos.offset(dx, dy, dz);

          // Outer walls
          if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
            BlockState wallBlock = random.nextFloat() < 0.3f ?
                Blocks.DARK_PRISMARINE.defaultBlockState() :
                Blocks.PRISMARINE_BRICKS.defaultBlockState();
            safeSetBlock(world, buildPos, wallBlock);
          }
          // Interior - mostly water
          else {
            safeSetBlock(world, buildPos, Blocks.WATER.defaultBlockState());
          }
        }
      }
    }

    // Roof
    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        BlockPos roofPos = pos.offset(dx, height, dz);
        safeSetBlock(world, roofPos, Blocks.PRISMARINE.defaultBlockState());
      }
    }

    // Add sea lanterns for lighting
    for (int i = 0; i < 8; i++) {
      int dx = (random.nextInt(radius * 2) - radius);
      int dy = random.nextInt(height - 2) + 1;
      int dz = (random.nextInt(radius * 2) - radius);

      BlockPos lanternPos = pos.offset(dx, dy, dz);
      if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
        safeSetBlock(world, lanternPos, Blocks.SEA_LANTERN.defaultBlockState());
      }
    }

    // Create central sponge room
    int spongeRadius = 2;
    int spongeY = height / 2;
    for (int dx = -spongeRadius; dx <= spongeRadius; dx++) {
      for (int dy = spongeY - 1; dy <= spongeY + 1; dy++) {
        for (int dz = -spongeRadius; dz <= spongeRadius; dz++) {
          BlockPos spongePos = pos.offset(dx, dy, dz);

          // Walls of sponge room
          if (Math.abs(dx) == spongeRadius || Math.abs(dz) == spongeRadius || dy == spongeY - 1 || dy == spongeY + 1) {
            safeSetBlock(world, spongePos, Blocks.PRISMARINE_BRICKS.defaultBlockState());
          } else {
            // Air inside sponge room
            safeSetBlock(world, spongePos, Blocks.AIR.defaultBlockState());
          }
        }
      }
    }

    // Add sponges (only if in chunk)
    for (int i = 0; i < 30; i++) {
      int dx = random.nextInt(spongeRadius * 2) - spongeRadius;
      int dz = random.nextInt(spongeRadius * 2) - spongeRadius;

      BlockPos wetSpongePos = pos.offset(dx, spongeY, dz);
      if (isInChunk(wetSpongePos) && world.getBlockState(wetSpongePos).isAir()) {
        world.setBlock(wetSpongePos, Blocks.WET_SPONGE.defaultBlockState(), 2);
      }
    }

    // Add gold block core (treasure)
    BlockPos goldPos = pos.offset(0, spongeY, 0);
    safeSetBlock(world, goldPos, Blocks.GOLD_BLOCK.defaultBlockState());

    // Spawn guardians using addFreshEntity to avoid chunk-loading deadlocks
    BlockPos guardianPos = pos.offset(0, spongeY + 2, 0);
    if (isInChunk(guardianPos)) {
      ElderGuardian elder = EntityType.ELDER_GUARDIAN.create(world.getLevel());
      if (elder != null) {
        elder.setPos(guardianPos.getX() + 0.5, guardianPos.getY(), guardianPos.getZ() + 0.5);
        world.addFreshEntity(elder);
      }
    }

    for (int i = 0; i < 3; i++) {
      int dx = random.nextInt(8) - 4;
      int dy = random.nextInt(height - 2) + 1;
      int dz = random.nextInt(8) - 4;

      BlockPos spawnPos = pos.offset(dx, dy, dz);
      if (isInChunk(spawnPos) && world.getBlockState(spawnPos).is(Blocks.WATER)) {
        Guardian guardian = EntityType.GUARDIAN.create(world.getLevel());
        if (guardian != null) {
          guardian.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
          world.addFreshEntity(guardian);
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:ocean_monuments";
  }
}
