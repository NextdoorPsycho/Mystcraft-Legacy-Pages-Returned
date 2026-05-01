package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Ocean ruins populator that generates underwater stone or sandstone ruins.
 * Ruins consist of partially destroyed buildings with suspicious sand/gravel
 * and drowned spawners. Approximately 1 per 20 chunks.
 */
public class OceanRuinsPopulator implements IPopulate {

  private static final int DEFAULT_RARITY = 20;

  private static final BlockState[] COLD_BLOCKS = {
      Blocks.STONE_BRICKS.defaultBlockState(),
      Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
      Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),
      Blocks.COBBLESTONE.defaultBlockState(),
      Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
      Blocks.GRAVEL.defaultBlockState()
  };

  private static final BlockState[] WARM_BLOCKS = {
      Blocks.SANDSTONE.defaultBlockState(),
      Blocks.CUT_SANDSTONE.defaultBlockState(),
      Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
      Blocks.SAND.defaultBlockState()
  };
  private final long seed;
  private final int rarity;
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public OceanRuinsPopulator(long seed) {
    this(seed, null);
  }

  public OceanRuinsPopulator(long seed, JsonObject params) {
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

    if ((chunkX * 29L + chunkZ * 43L + seed) % rarity != 0) {
      return;
    }

    int x = chunkPos.getX() + 4 + random.nextInt(8);
    int z = chunkPos.getZ() + 4 + random.nextInt(8);

    int oceanFloor = world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
    int waterSurface = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

    if (oceanFloor >= waterSurface - 3) {
      return;
    }

    BlockPos pos = new BlockPos(x, oceanFloor, z);

    BlockState ground = world.getBlockState(pos.below());
    boolean warm = ground.is(Blocks.SAND) || ground.is(Blocks.SANDSTONE);

    generateRuins(world, random, pos, warm);
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

  private void generateRuins(WorldGenLevel world, RandomSource random, BlockPos pos, boolean warm) {
    BlockState[] materials = warm ? WARM_BLOCKS : COLD_BLOCKS;

    int buildingCount = 1 + random.nextInt(3);

    for (int b = 0; b < buildingCount; b++) {
      int offsetX = b == 0 ? 0 : random.nextInt(12) - 6;
      int offsetZ = b == 0 ? 0 : random.nextInt(12) - 6;

      BlockPos buildingPos = pos.offset(offsetX, 0, offsetZ);
      generateRuinedBuilding(world, random, buildingPos, materials, warm);
    }
  }

  private void generateRuinedBuilding(WorldGenLevel world, RandomSource random,
                                      BlockPos pos, BlockState[] materials, boolean warm) {
    int width = 4 + random.nextInt(4);
    int depth = 4 + random.nextInt(4);
    int maxHeight = 2 + random.nextInt(3);

    for (int x = 0; x < width; x++) {
      for (int z = 0; z < depth; z++) {
        boolean isWall = x == 0 || x == width - 1 || z == 0 || z == depth - 1;

        BlockPos floorPos = pos.offset(x, 0, z);
        if (random.nextFloat() < 0.8f) {
          safeSetBlock(world, floorPos, materials[random.nextInt(materials.length)]);
        }

        if (isWall) {
          int wallHeight = random.nextInt(maxHeight + 1);
          for (int y = 1; y <= wallHeight; y++) {
            if (random.nextFloat() < 0.7f) {
              BlockPos wallPos = pos.offset(x, y, z);
              safeSetBlock(world, wallPos, materials[random.nextInt(materials.length)]);
            }
          }
        }
      }
    }

    for (int i = 0; i < 3; i++) {
      int sx = random.nextInt(width);
      int sz = random.nextInt(depth);
      BlockPos susPos = pos.offset(sx, 0, sz);
      BlockState susBlock = warm ?
          Blocks.SUSPICIOUS_SAND.defaultBlockState() :
          Blocks.SUSPICIOUS_GRAVEL.defaultBlockState();
      safeSetBlock(world, susPos, susBlock);
    }

    if (random.nextFloat() < 0.6f) {
      int cx = 1 + random.nextInt(Math.max(1, width - 2));
      int cz = 1 + random.nextInt(Math.max(1, depth - 2));
      BlockPos chestPos = pos.offset(cx, 1, cz);

      if (isInChunk(chestPos)) {
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(
            ChestBlock.FACING, Direction.NORTH), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
          chest.setLootTable(BuiltInLootTables.UNDERWATER_RUIN_BIG, random.nextLong());
        }
      }
    }

    if (random.nextFloat() < 0.3f) {
      int mx = random.nextInt(width);
      int mz = random.nextInt(depth);
      safeSetBlock(world, pos.offset(mx, 0, mz), Blocks.MAGMA_BLOCK.defaultBlockState());
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:ocean_ruins";
  }
}
