package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Generates Bastion remnants with blackstone structures, piglin spawns, and gold blocks.
 * Spawns approximately 1 per 32 chunks.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk loads.
 */
public class BastionRemnantsPopulator implements IPopulate {

  private static final int CHUNKS_BETWEEN_BASTIONS = 32;
  private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
  private static final BlockState POLISHED_BLACKSTONE = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
  private static final BlockState POLISHED_BLACKSTONE_BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
  private static final BlockState GILDED_BLACKSTONE = Blocks.GILDED_BLACKSTONE.defaultBlockState();
  private static final BlockState GOLD_BLOCK = Blocks.GOLD_BLOCK.defaultBlockState();
  private static final BlockState CHAIN = Blocks.CHAIN.defaultBlockState();
  private static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
  private static final BlockState MAGMA_BLOCK = Blocks.MAGMA_BLOCK.defaultBlockState();
  private final long seed;
  private final float spawnChance;
  // Chunk boundaries for current population
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public BastionRemnantsPopulator(long seed) {
    this(seed, null);
  }

  public BastionRemnantsPopulator(long seed, JsonObject params) {
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
    if (chunkX % CHUNKS_BETWEEN_BASTIONS == 0 && chunkZ % CHUNKS_BETWEEN_BASTIONS == 0) {
      // Random chance to generate
      if (random.nextInt(100) < 8) {
        int x = chunkPos.getX() + random.nextInt(16);
        int y = 40 + random.nextInt(25);
        int z = chunkPos.getZ() + random.nextInt(16);

        BlockPos bastionPos = new BlockPos(x, y, z);
        generateBastion(world, random, bastionPos);
      }
    }
  }

  private void generateBastion(WorldGenLevel world, RandomSource random, BlockPos pos) {
    // Generate the main hall
    generateMainHall(world, random, pos);

    // Generate treasure room
    BlockPos treasurePos = pos.offset(12, 0, 0);
    generateTreasureRoom(world, random, treasurePos);

    // Generate housing units
    BlockPos housingPos = pos.offset(-12, 0, 8);
    generateHousingUnit(world, random, housingPos);

    // Generate bridge
    BlockPos bridgePos = pos.offset(0, 0, -12);
    generateBridge(world, random, bridgePos);

    // Generate magma cube spawner area
    BlockPos spawnerPos = pos.offset(8, -3, 8);
    generateSpawnerArea(world, random, spawnerPos);
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

  private void generateMainHall(WorldGenLevel world, RandomSource random, BlockPos pos) {
    // 11x8x11 main hall
    for (int x = -5; x <= 5; x++) {
      for (int z = -5; z <= 5; z++) {
        // Floor with decorative pattern
        BlockState floorBlock = (x + z) % 2 == 0 ? POLISHED_BLACKSTONE_BRICKS : POLISHED_BLACKSTONE;
        safeSetBlock(world, pos.offset(x, -1, z), floorBlock);

        // Ceiling
        safeSetBlock(world, pos.offset(x, 7, z), POLISHED_BLACKSTONE_BRICKS);

        // Clear interior
        for (int y = 0; y < 7; y++) {
          safeSetBlock(world, pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
      }
    }

    // Walls with gilded blackstone accents
    for (int y = 0; y < 7; y++) {
      for (int x = -5; x <= 5; x++) {
        BlockState wallBlock = random.nextInt(10) == 0 ? GILDED_BLACKSTONE : POLISHED_BLACKSTONE_BRICKS;
        safeSetBlock(world, pos.offset(x, y, -5), wallBlock);
        safeSetBlock(world, pos.offset(x, y, 5), wallBlock);
      }
      for (int z = -5; z <= 5; z++) {
        BlockState wallBlock = random.nextInt(10) == 0 ? GILDED_BLACKSTONE : POLISHED_BLACKSTONE_BRICKS;
        safeSetBlock(world, pos.offset(-5, y, z), wallBlock);
        safeSetBlock(world, pos.offset(5, y, z), wallBlock);
      }
    }

    // Add gold block pillars at corners
    for (int y = 0; y < 7; y++) {
      safeSetBlock(world, pos.offset(-4, y, -4), GOLD_BLOCK);
      safeSetBlock(world, pos.offset(-4, y, 4), GOLD_BLOCK);
      safeSetBlock(world, pos.offset(4, y, -4), GOLD_BLOCK);
      safeSetBlock(world, pos.offset(4, y, 4), GOLD_BLOCK);
    }

    // Add lanterns hanging from chains
    safeSetBlock(world, pos.offset(0, 6, 0), CHAIN);
    safeSetBlock(world, pos.offset(0, 5, 0), CHAIN);
    safeSetBlock(world, pos.offset(0, 4, 0), LANTERN);
  }

  private void generateTreasureRoom(WorldGenLevel world, RandomSource random, BlockPos pos) {
    // 7x5x7 treasure room
    for (int x = -3; x <= 3; x++) {
      for (int z = -3; z <= 3; z++) {
        // Floor
        safeSetBlock(world, pos.offset(x, -1, z), POLISHED_BLACKSTONE_BRICKS);

        // Ceiling
        safeSetBlock(world, pos.offset(x, 4, z), POLISHED_BLACKSTONE_BRICKS);

        // Clear interior
        for (int y = 0; y < 4; y++) {
          safeSetBlock(world, pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
      }
    }

    // Walls
    for (int y = 0; y < 4; y++) {
      for (int x = -3; x <= 3; x++) {
        safeSetBlock(world, pos.offset(x, y, -3), BLACKSTONE);
        safeSetBlock(world, pos.offset(x, y, 3), BLACKSTONE);
      }
      for (int z = -3; z <= 3; z++) {
        safeSetBlock(world, pos.offset(-3, y, z), BLACKSTONE);
        safeSetBlock(world, pos.offset(3, y, z), BLACKSTONE);
      }
    }

    // Add gold blocks in center
    for (int x = -1; x <= 1; x++) {
      for (int z = -1; z <= 1; z++) {
        safeSetBlock(world, pos.offset(x, 0, z), GOLD_BLOCK);
      }
    }

    // Place treasure chests (only if in chunk)
    for (int i = 0; i < 4; i++) {
      BlockPos chestPos = pos.offset(random.nextInt(5) - 2, 1, random.nextInt(5) - 2);
      if (isInChunk(chestPos) && world.getBlockState(chestPos).isAir()) {
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
          chest.setLootTable(BuiltInLootTables.BASTION_TREASURE, random.nextLong());
        }
      }
    }
  }

  private void generateHousingUnit(WorldGenLevel world, RandomSource random, BlockPos pos) {
    // 5x4x5 housing unit
    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        // Floor
        safeSetBlock(world, pos.offset(x, -1, z), BLACKSTONE);

        // Ceiling
        safeSetBlock(world, pos.offset(x, 3, z), BLACKSTONE);

        // Clear interior
        for (int y = 0; y < 3; y++) {
          safeSetBlock(world, pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
      }
    }

    // Walls (leave doorway on one side)
    for (int y = 0; y < 3; y++) {
      for (int x = -2; x <= 2; x++) {
        safeSetBlock(world, pos.offset(x, y, -2), BLACKSTONE);
        if (x != 0) {
          safeSetBlock(world, pos.offset(x, y, 2), BLACKSTONE);
        }
      }
      for (int z = -2; z <= 2; z++) {
        safeSetBlock(world, pos.offset(-2, y, z), BLACKSTONE);
        safeSetBlock(world, pos.offset(2, y, z), BLACKSTONE);
      }
    }

    // Add chest with housing loot (only if in chunk)
    BlockPos chestPos = pos.offset(1, 0, 1);
    if (isInChunk(chestPos)) {
      world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
      BlockEntity be = world.getBlockEntity(chestPos);
      if (be instanceof ChestBlockEntity chest) {
        chest.setLootTable(BuiltInLootTables.BASTION_OTHER, random.nextLong());
      }
    }
  }

  private void generateBridge(WorldGenLevel world, RandomSource random, BlockPos pos) {
    // 3x1x10 bridge
    for (int z = 0; z < 10; z++) {
      for (int x = -1; x <= 1; x++) {
        safeSetBlock(world, pos.offset(x, 0, z), POLISHED_BLACKSTONE);

        // Clear above
        for (int y = 1; y <= 3; y++) {
          safeSetBlock(world, pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
      }

      // Add chain railings
      if (z % 2 == 0) {
        safeSetBlock(world, pos.offset(-1, 1, z), CHAIN);
        safeSetBlock(world, pos.offset(1, 1, z), CHAIN);
      }
    }
  }

  private void generateSpawnerArea(WorldGenLevel world, RandomSource random, BlockPos pos) {
    // 5x3x5 magma cube spawner area
    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        // Floor with magma blocks
        safeSetBlock(world, pos.offset(x, -1, z), MAGMA_BLOCK);

        // Clear interior
        for (int y = 0; y < 3; y++) {
          safeSetBlock(world, pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
      }
    }

    // Place magma cube spawner (only if in chunk)
    if (isInChunk(pos)) {
      world.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
      BlockEntity be = world.getBlockEntity(pos);
      if (be instanceof SpawnerBlockEntity spawner) {
        spawner.setEntityId(EntityType.MAGMA_CUBE, random);
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:bastion_remnants";
  }
}
