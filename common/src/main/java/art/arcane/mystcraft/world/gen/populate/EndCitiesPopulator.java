package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Generates End cities with purpur towers, shulker spawns, and elytra rewards.
 * Spawns approximately 1 per 32 chunks.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk loads.
 */
public class EndCitiesPopulator implements IPopulate {

  private static final int CHUNKS_BETWEEN_CITIES = 32;
  private static final BlockState PURPUR_BLOCK = Blocks.PURPUR_BLOCK.defaultBlockState();
  private static final BlockState PURPUR_PILLAR = Blocks.PURPUR_PILLAR.defaultBlockState();
  private static final BlockState END_STONE_BRICKS = Blocks.END_STONE_BRICKS.defaultBlockState();
  private static final BlockState END_ROD = Blocks.END_ROD.defaultBlockState();
  private static final BlockState MAGENTA_STAINED_GLASS = Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
  private static final BlockState CHORUS_PLANT = Blocks.CHORUS_PLANT.defaultBlockState();
  private static final BlockState CHORUS_FLOWER = Blocks.CHORUS_FLOWER.defaultBlockState();
  private final long seed;
  private final float spawnChance;
  // Chunk boundaries for current population
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public EndCitiesPopulator(long seed) {
    this(seed, null);
  }

  public EndCitiesPopulator(long seed, JsonObject params) {
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

    // Set chunk boundaries for this population run
    chunkMinX = chunkX << 4;
    chunkMaxX = chunkMinX + 15;
    chunkMinZ = chunkZ << 4;
    chunkMaxZ = chunkMinZ + 15;

    // Only generate in certain chunks (spacing control)
    if (chunkX % CHUNKS_BETWEEN_CITIES == 0 && chunkZ % CHUNKS_BETWEEN_CITIES == 0) {
      // Random chance to generate
      if (random.nextInt(100) < 6) {
        int x = chunkPos.getX() + random.nextInt(16);
        int y = 60 + random.nextInt(20);
        int z = chunkPos.getZ() + random.nextInt(16);

        BlockPos cityPos = new BlockPos(x, y, z);
        generateEndCity(world, random, cityPos);
      }
    }
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

  private void generateEndCity(WorldGenLevel world, RandomSource random, BlockPos pos) {
    // Generate base platform
    generateBasePlatform(world, random, pos);

    // Generate main tower
    int towerHeight = 15 + random.nextInt(15);
    generateTower(world, random, pos.above(2), towerHeight);

    // Generate ship portion (with elytra) - uses chunk boundary checking
    if (random.nextBoolean()) {
      BlockPos shipPos = pos.offset(8, towerHeight + 5, 0);
      generateShip(world, random, shipPos);
    }

    // Generate chorus plants nearby
    for (int i = 0; i < 5; i++) {
      int dx = random.nextInt(20) - 10;
      int dz = random.nextInt(20) - 10;
      BlockPos chorusPos = pos.offset(dx, -2, dz);
      generateChorusPlant(world, random, chorusPos);
    }
  }

  private void generateBasePlatform(WorldGenLevel world, RandomSource random, BlockPos pos) {
    // 9x2x9 platform
    for (int x = -4; x <= 4; x++) {
      for (int z = -4; z <= 4; z++) {
        safeSetBlock(world, pos.offset(x, 0, z), END_STONE_BRICKS);
        safeSetBlock(world, pos.offset(x, 1, z), PURPUR_BLOCK);
      }
    }

    // Add decorative pillars at corners
    for (int y = 2; y < 5; y++) {
      safeSetBlock(world, pos.offset(-4, y, -4), PURPUR_PILLAR);
      safeSetBlock(world, pos.offset(-4, y, 4), PURPUR_PILLAR);
      safeSetBlock(world, pos.offset(4, y, -4), PURPUR_PILLAR);
      safeSetBlock(world, pos.offset(4, y, 4), PURPUR_PILLAR);
    }

    // Add end rods on top of pillars
    safeSetBlock(world, pos.offset(-4, 5, -4), END_ROD);
    safeSetBlock(world, pos.offset(-4, 5, 4), END_ROD);
    safeSetBlock(world, pos.offset(4, 5, -4), END_ROD);
    safeSetBlock(world, pos.offset(4, 5, 4), END_ROD);
  }

  private void generateTower(WorldGenLevel world, RandomSource random, BlockPos pos, int height) {
    // 5x5 tower
    for (int y = 0; y < height; y++) {
      for (int x = -2; x <= 2; x++) {
        for (int z = -2; z <= 2; z++) {
          boolean isWall = x == -2 || x == 2 || z == -2 || z == 2;
          boolean isCorner = (x == -2 || x == 2) && (z == -2 || z == 2);

          BlockPos blockPos = pos.offset(x, y, z);
          if (isCorner) {
            safeSetBlock(world, blockPos, PURPUR_PILLAR);
          } else if (isWall) {
            if (y % 4 == 2 || y % 4 == 3) {
              safeSetBlock(world, blockPos, MAGENTA_STAINED_GLASS);
            } else {
              safeSetBlock(world, blockPos, PURPUR_BLOCK);
            }
          } else {
            safeSetBlock(world, blockPos, Blocks.AIR.defaultBlockState());
          }
        }
      }

      // Add end rod lighting every 5 blocks
      if (y % 5 == 0) {
        safeSetBlock(world, pos.offset(0, y, 0), END_ROD);
      }

      // Add floors every 7 blocks
      if (y % 7 == 0 && y > 0) {
        for (int x = -2; x <= 2; x++) {
          for (int z = -2; z <= 2; z++) {
            safeSetBlock(world, pos.offset(x, y, z), PURPUR_BLOCK);
          }
        }

        // Add chest on floor (only if in chunk)
        BlockPos chestPos = pos.offset(1, y + 1, 1);
        if (isInChunk(chestPos)) {
          world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
          BlockEntity be = world.getBlockEntity(chestPos);
          if (be instanceof ChestBlockEntity chest) {
            chest.setLootTable(BuiltInLootTables.END_CITY_TREASURE, random.nextLong());
          }
        }

        // Add shulker spawner potential
        if (random.nextBoolean()) {
          safeSetBlock(world, pos.offset(-1, y + 1, -1), PURPUR_BLOCK);
        }
      }
    }

    // Top cap
    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        safeSetBlock(world, pos.offset(x, height, z), PURPUR_BLOCK);
      }
    }

    // Central end rod at top
    safeSetBlock(world, pos.offset(0, height + 1, 0), END_ROD);
  }

  private void generateShip(WorldGenLevel world, RandomSource random, BlockPos pos) {
    // 9x7x20 ship hull - uses safe block placement to avoid cascade loading
    for (int x = -4; x <= 4; x++) {
      for (int z = 0; z < 20; z++) {
        for (int y = 0; y < 4; y++) {
          boolean isWall = (x == -4 || x == 4 || y == 0 || y == 3);
          BlockPos blockPos = pos.offset(x, y, z);

          if (isWall) {
            safeSetBlock(world, blockPos, PURPUR_BLOCK);
          } else {
            safeSetBlock(world, blockPos, Blocks.AIR.defaultBlockState());
          }
        }
      }
    }

    // Ship bow (pointed front)
    for (int dx = -3; dx <= 3; dx++) {
      safeSetBlock(world, pos.offset(dx, 0, 20), PURPUR_BLOCK);
      safeSetBlock(world, pos.offset(dx, 1, 20), PURPUR_BLOCK);
    }
    for (int dx = -2; dx <= 2; dx++) {
      safeSetBlock(world, pos.offset(dx, 0, 21), PURPUR_BLOCK);
    }
    for (int dx = -1; dx <= 1; dx++) {
      safeSetBlock(world, pos.offset(dx, 0, 22), PURPUR_BLOCK);
    }

    // Place elytra in item frame at front of ship (only if in chunk)
    BlockPos elytraPos = pos.offset(0, 2, 19);
    if (isInChunk(elytraPos)) {
      world.setBlock(elytraPos, PURPUR_BLOCK, 2);
      net.minecraft.server.level.ServerLevel serverLevel = world.getLevel();
      ItemFrame itemFrame = new ItemFrame(serverLevel, elytraPos, Direction.SOUTH);
      itemFrame.setItem(new ItemStack(Items.ELYTRA));
      world.addFreshEntity(itemFrame);
    }

    // Add treasure chests (only if in chunk)
    BlockPos chest1Pos = pos.offset(2, 1, 10);
    BlockPos chest2Pos = pos.offset(-2, 1, 10);

    if (isInChunk(chest1Pos)) {
      world.setBlock(chest1Pos, Blocks.CHEST.defaultBlockState(), 2);
      BlockEntity be1 = world.getBlockEntity(chest1Pos);
      if (be1 instanceof ChestBlockEntity chest) {
        chest.setLootTable(BuiltInLootTables.END_CITY_TREASURE, random.nextLong());
      }
    }

    if (isInChunk(chest2Pos)) {
      world.setBlock(chest2Pos, Blocks.CHEST.defaultBlockState(), 2);
      BlockEntity be2 = world.getBlockEntity(chest2Pos);
      if (be2 instanceof ChestBlockEntity chest) {
        chest.setLootTable(BuiltInLootTables.END_CITY_TREASURE, random.nextLong());
      }
    }

    // Add end rod lighting
    for (int z = 2; z < 18; z += 4) {
      safeSetBlock(world, pos.offset(-3, 2, z), END_ROD);
      safeSetBlock(world, pos.offset(3, 2, z), END_ROD);
    }
  }

  private void generateChorusPlant(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 5 + random.nextInt(8);

    // Grow chorus plant upward
    for (int y = 0; y < height; y++) {
      safeSetBlock(world, pos.above(y), CHORUS_PLANT);

      // Occasionally branch
      if (y > 2 && random.nextInt(3) == 0) {
        Direction branchDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        safeSetBlock(world, pos.above(y).relative(branchDir), CHORUS_PLANT);
        safeSetBlock(world, pos.above(y).relative(branchDir).above(), CHORUS_FLOWER);
      }
    }

    // Add flower at top
    safeSetBlock(world, pos.above(height), CHORUS_FLOWER);
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:end_cities";
  }
}
