package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Igloo populator that generates small snow-brick shelters. Igloos consist of a
 * packed snow dome with a bed, crafting table, furnace, and optionally a hidden
 * basement with a brewing stand and trapped villager. Approximately 1 per 48
 * chunks.
 */
public class IglooPopulator implements IPopulate {

  private static final int DEFAULT_RARITY = 48;
  private final long seed;
  private final int rarity;
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public IglooPopulator(long seed) {
    this(seed, null);
  }

  public IglooPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.rarity = PopulatorConfig.rarityFrom(params, DEFAULT_RARITY);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    int chunkX = chunkPos.getX() >> 4;
    int chunkZ = chunkPos.getZ() >> 4;
    chunkMinX = chunkX << 4;
    chunkMaxX = chunkMinX + 15;
    chunkMinZ = chunkZ << 4;
    chunkMaxZ = chunkMinZ + 15;

    if ((chunkX + chunkZ * 41L + seed) % rarity != 0) {
      return;
    }

    int x = chunkPos.getX() + 4 + random.nextInt(8);
    int z = chunkPos.getZ() + 4 + random.nextInt(8);

    int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    if (surfaceY < 60 || surfaceY > 120) {
      return;
    }

    BlockPos surfacePos = new BlockPos(x, surfaceY, z);

    BlockState groundBlock = world.getBlockState(surfacePos.below());
    boolean isSnowy = groundBlock.is(Blocks.SNOW_BLOCK) || groundBlock.is(Blocks.POWDER_SNOW) ||
        groundBlock.is(Blocks.GRASS_BLOCK) || groundBlock.is(Blocks.DIRT);

    if (!isSnowy) {
      return;
    }

    generateIgloo(world, random, surfacePos);
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

  private void generateIgloo(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int radius = 3;
    int height = 3;

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        for (int dy = 0; dy <= height; dy++) {
          double dist = Math.sqrt(dx * dx + dz * dz + (dy * 1.5) * (dy * 1.5));
          if (dist > radius + 0.5 || dist < radius - 0.5) {
            continue;
          }

          BlockPos blockPos = pos.offset(dx, dy, dz);
          safeSetBlock(world, blockPos, Blocks.SNOW_BLOCK.defaultBlockState());
        }
      }
    }

    for (int dx = -radius + 1; dx <= radius - 1; dx++) {
      for (int dz = -radius + 1; dz <= radius - 1; dz++) {
        for (int dy = 0; dy < height; dy++) {
          double dist = Math.sqrt(dx * dx + dz * dz);
          if (dist < radius - 0.5) {
            BlockPos blockPos = pos.offset(dx, dy, dz);
            safeSetBlock(world, blockPos, Blocks.AIR.defaultBlockState());
          }
        }
      }
    }

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist <= radius) {
          BlockPos floorPos = pos.offset(dx, -1, dz);
          safeSetBlock(world, floorPos, Blocks.SNOW_BLOCK.defaultBlockState());
        }
      }
    }

    safeSetBlock(world, pos.offset(radius, 0, 0), Blocks.AIR.defaultBlockState());
    safeSetBlock(world, pos.offset(radius, 1, 0), Blocks.AIR.defaultBlockState());

    safeSetBlock(world, pos.offset(-1, 0, -1), Blocks.RED_BED.defaultBlockState());
    safeSetBlock(world, pos.offset(1, 0, -1), Blocks.CRAFTING_TABLE.defaultBlockState());
    safeSetBlock(world, pos.offset(-1, 0, 1), Blocks.FURNACE.defaultBlockState());

    safeSetBlock(world, pos, Blocks.RED_CARPET.defaultBlockState());
    safeSetBlock(world, pos.offset(1, 0, 0), Blocks.WHITE_CARPET.defaultBlockState());

    safeSetBlock(world, pos.offset(0, 1, -1), Blocks.REDSTONE_TORCH.defaultBlockState());

    if (random.nextBoolean()) {
      generateBasement(world, random, pos);
    }
  }

  private void generateBasement(WorldGenLevel world, RandomSource random, BlockPos pos) {

    BlockPos ladderPos = pos.offset(-2, 0, 0);
    for (int dy = 0; dy >= -8; dy--) {
      safeSetBlock(world, ladderPos.offset(0, dy, 0), Blocks.LADDER.defaultBlockState());
    }

    BlockPos basementCenter = pos.offset(0, -8, 0);
    for (int dx = -2; dx <= 2; dx++) {
      for (int dz = -2; dz <= 2; dz++) {
        for (int dy = 0; dy <= 3; dy++) {
          BlockPos roomPos = basementCenter.offset(dx, dy, dz);
          boolean isWall = dx == -2 || dx == 2 || dz == -2 || dz == 2;
          boolean isFloor = dy == 0;
          boolean isCeiling = dy == 3;

          if (isFloor) {
            safeSetBlock(world, roomPos, Blocks.STONE_BRICKS.defaultBlockState());
          } else if (isCeiling) {
            safeSetBlock(world, roomPos, Blocks.STONE_BRICKS.defaultBlockState());
          } else if (isWall) {
            safeSetBlock(world, roomPos, Blocks.STONE_BRICKS.defaultBlockState());
          } else {
            safeSetBlock(world, roomPos, Blocks.AIR.defaultBlockState());
          }
        }
      }
    }

    safeSetBlock(world, basementCenter.offset(1, 1, 1), Blocks.BREWING_STAND.defaultBlockState());
    safeSetBlock(world, basementCenter.offset(-1, 1, 1), Blocks.CAULDRON.defaultBlockState());

    BlockPos chestPos = basementCenter.offset(1, 1, -1);
    if (isInChunk(chestPos)) {
      world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
      BlockEntity be = world.getBlockEntity(chestPos);
      if (be instanceof ChestBlockEntity chest) {
        chest.setLootTable(BuiltInLootTables.IGLOO_CHEST, random.nextLong());
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:igloos";
  }
}
