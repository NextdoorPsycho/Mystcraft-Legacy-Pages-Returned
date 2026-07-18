package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pillager outpost populator that generates dark oak tower structures. Outposts
 * consist of a watchtower with pillager spawns, target blocks, and cages.
 * Approximately 1 per 32 chunks.
 */
public class PillagerOutpostsPopulator implements IPopulate {

  private static final int CHUNKS_BETWEEN = 32;
  private final long seed;
  private final float spawnChance;

  public PillagerOutpostsPopulator(long seed) {
    this(seed, null);
  }

  public PillagerOutpostsPopulator(long seed, JsonObject params) {
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

    if (chunkX % CHUNKS_BETWEEN != 0 || chunkZ % CHUNKS_BETWEEN != 0) {
      return;
    }

    if (random.nextFloat() > 0.3f) {
      return;
    }

    int x = chunkPos.getX() + random.nextInt(16);
    int z = chunkPos.getZ() + random.nextInt(16);

    int y = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z);

    if (y < 60 || y > 120) {
      return;
    }

    BlockPos pos = new BlockPos(x, y, z);
    BlockState groundState = world.getBlockState(pos.below());

    if (!groundState.isSolid() || groundState.is(Blocks.WATER)) {
      return;
    }

    generateOutpost(world, random, pos);
  }

  private void generateOutpost(WorldGenLevel world, RandomSource random, BlockPos pos) {

    int height = 12;
    int radius = 2;

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        BlockPos floorPos = pos.offset(dx, 0, dz);
        world.setBlock(floorPos, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
      }
    }

    for (int dy = 1; dy < height; dy++) {
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
          BlockPos buildPos = pos.offset(dx, dy, dz);

          if ((Math.abs(dx) == radius && Math.abs(dz) == radius)) {
            world.setBlock(buildPos, Blocks.DARK_OAK_LOG.defaultBlockState(), 2);
          } else if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
            if (dy % 3 == 0 || dy == 1) {
              world.setBlock(buildPos, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
            } else {
              world.setBlock(buildPos, Blocks.AIR.defaultBlockState(), 2);
            }
          } else {
            world.setBlock(buildPos, Blocks.AIR.defaultBlockState(), 2);
          }
        }
      }
    }

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        BlockPos roofPos = pos.offset(dx, height, dz);
        world.setBlock(roofPos, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
      }
    }

    BlockPos targetPos = pos.offset(0, height - 1, 0);
    world.setBlock(targetPos, Blocks.TARGET.defaultBlockState(), 2);

    BlockPos cagePos = pos.offset(random.nextInt(8) + 4, 0, random.nextInt(8) + 4);
    generateCage(world, cagePos);

    spawnPillagers(world, pos, random);
  }

  private void generateCage(WorldGenLevel world, BlockPos pos) {

    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = 0; dy <= 2; dy++) {
        for (int dz = -1; dz <= 1; dz++) {
          BlockPos cageBlockPos = pos.offset(dx, dy, dz);

          if (Math.abs(dx) == 1 || Math.abs(dz) == 1 || dy == 0 || dy == 2) {
            if (!(dx == 0 && dz == 0 && dy == 0)) {
              world.setBlock(cageBlockPos, Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
            }
          }
        }
      }
    }
  }

  private void spawnPillagers(WorldGenLevel world, BlockPos pos, RandomSource random) {
    int count = 3 + random.nextInt(3);
    for (int i = 0; i < count; i++) {
      int dx = random.nextInt(10) - 5;
      int dz = random.nextInt(10) - 5;
      BlockPos spawnPos = pos.offset(dx, 1, dz);

      if (world.getBlockState(spawnPos).isAir() && world.getBlockState(spawnPos.below()).isSolid()) {
        Pillager pillager = EntityTypes.PILLAGER.create(
            world.getLevel(), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
        if (pillager != null) {
          pillager.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
          world.addFreshEntity(pillager);
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:pillager_outposts";
  }
}
