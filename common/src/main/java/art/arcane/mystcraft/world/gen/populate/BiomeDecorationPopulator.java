package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Biome decoration populator that adds trees, grass, and flowers.
 * Uses biome information to determine appropriate vegetation.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk loads.
 */
public class BiomeDecorationPopulator implements IPopulate {

  private final long seed;
  private final float spawnChance;
  private final float treeMultiplier;
  private final float grassMultiplier;
  private final float flowerMultiplier;

  // Chunk boundaries for current population
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public BiomeDecorationPopulator(long seed) {
    this(seed, null);
  }

  public BiomeDecorationPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spawnChance = PopulatorConfig.chanceFrom(params, 1.0f, 0);
    this.treeMultiplier = PopulatorConfig.getFloat(params, "tree_multiplier", 1.0f);
    this.grassMultiplier = PopulatorConfig.getFloat(params, "grass_multiplier", 1.0f);
    this.flowerMultiplier = PopulatorConfig.getFloat(params, "flower_multiplier", 1.0f);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    if (random.nextFloat() > spawnChance) {
      return;
    }
    int chunkX = chunkPos.getX();
    int chunkZ = chunkPos.getZ();

    // Set chunk boundaries for this population run
    int chunkCoordX = chunkX >> 4;
    int chunkCoordZ = chunkZ >> 4;
    chunkMinX = chunkCoordX << 4;
    chunkMaxX = chunkMinX + 15;
    chunkMinZ = chunkCoordZ << 4;
    chunkMaxZ = chunkMinZ + 15;

    // Sample biome at chunk center
    BlockPos centerPos = new BlockPos(chunkX + 8, 64, chunkZ + 8);
    Holder<Biome> biomeHolder = world.getBiome(centerPos);

    // Generate vegetation based on biome
    generateTrees(world, random, chunkPos, biomeHolder);
    generateGrass(world, random, chunkPos, biomeHolder);
    generateFlowers(world, random, chunkPos, biomeHolder);
  }

  private void generateTrees(WorldGenLevel world, RandomSource random, BlockPos chunkPos, Holder<Biome> biomeHolder) {
    int treesPerChunk = Math.max(0, Math.round(getTreesPerChunk(biomeHolder) * treeMultiplier));

    for (int i = 0; i < treesPerChunk; i++) {
      int x = chunkPos.getX() + random.nextInt(16);
      int z = chunkPos.getZ() + random.nextInt(16);
      int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      BlockPos treePos = new BlockPos(x, y, z);

      // Skip if tree base is outside chunk boundaries
      if (!isInChunk(treePos)) {
        continue;
      }

      // Check if valid location for tree
      BlockPos belowPos = treePos.below();
      if (!isInChunk(belowPos)) {
        continue;
      }
      BlockState ground = world.getBlockState(belowPos);
      if (!canSupportTree(ground)) {
        continue;
      }

      // Check if there's enough space
      if (!hasSpaceForTree(world, treePos)) {
        continue;
      }

      // Generate tree based on biome
      generateTree(world, random, treePos, biomeHolder);
    }
  }

  private int getTreesPerChunk(Holder<Biome> biomeHolder) {
    // Determine tree density based on biome
    if (biomeHolder.is(BiomeTags.IS_FOREST)) {
      return 10;
    } else if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
      return 50;
    } else if (biomeHolder.is(BiomeTags.IS_TAIGA)) {
      return 8;
    } else if (biomeHolder.is(BiomeTags.IS_BADLANDS) || biomeHolder.is(BiomeTags.IS_BEACH)) {
      return 0;
    } else if (biomeHolder.is(Biomes.PLAINS) || biomeHolder.is(Biomes.SUNFLOWER_PLAINS)) {
      return 1;
    } else if (biomeHolder.is(Biomes.SAVANNA) || biomeHolder.is(Biomes.SAVANNA_PLATEAU)) {
      return 2;
    } else if (biomeHolder.is(Biomes.SWAMP) || biomeHolder.is(Biomes.MANGROVE_SWAMP)) {
      return 4;
    }
    // Default
    return 3;
  }

  /**
   * Checks if a position is within the current chunk boundaries.
   * This prevents cascade chunk loading when decorations extend beyond chunk edges.
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

  private boolean canSupportTree(BlockState ground) {
    return ground.is(BlockTags.DIRT) || ground.is(Blocks.GRASS_BLOCK) ||
        ground.is(Blocks.PODZOL) || ground.is(Blocks.MYCELIUM) ||
        ground.is(Blocks.MUD) || ground.is(Blocks.MUDDY_MANGROVE_ROOTS);
  }

  private boolean hasSpaceForTree(WorldGenLevel world, BlockPos pos) {
    // Check 5x5 area above for clearance
    for (int dy = 0; dy < 6; dy++) {
      BlockPos checkPos = pos.above(dy);
      // Skip positions outside chunk to avoid cascade loading
      if (!isInChunk(checkPos)) {
        continue;
      }
      BlockState state = world.getBlockState(checkPos);
      if (state.isSolid() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) {
        return false;
      }
    }
    return true;
  }

  private void generateTree(WorldGenLevel world, RandomSource random, BlockPos pos, Holder<Biome> biomeHolder) {
    // Determine tree type based on biome
    TreeType treeType = getTreeType(biomeHolder, random);

    // Generate simple tree structure
    switch (treeType) {
      case OAK -> generateOakTree(world, random, pos);
      case BIRCH -> generateBirchTree(world, random, pos);
      case SPRUCE -> generateSpruceTree(world, random, pos);
      case JUNGLE -> generateJungleTree(world, random, pos);
      case ACACIA -> generateAcaciaTree(world, random, pos);
      case DARK_OAK -> generateDarkOakTree(world, random, pos);
    }
  }

  private TreeType getTreeType(Holder<Biome> biomeHolder, RandomSource random) {
    if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
      return TreeType.JUNGLE;
    } else if (biomeHolder.is(BiomeTags.IS_TAIGA)) {
      return TreeType.SPRUCE;
    } else if (biomeHolder.is(Biomes.BIRCH_FOREST) || biomeHolder.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
      return TreeType.BIRCH;
    } else if (biomeHolder.is(Biomes.DARK_FOREST)) {
      return random.nextInt(3) == 0 ? TreeType.OAK : TreeType.DARK_OAK;
    } else if (biomeHolder.is(Biomes.SAVANNA) || biomeHolder.is(Biomes.SAVANNA_PLATEAU)) {
      return TreeType.ACACIA;
    } else if (biomeHolder.is(BiomeTags.IS_FOREST)) {
      return random.nextInt(5) == 0 ? TreeType.BIRCH : TreeType.OAK;
    }
    // Default to oak
    return random.nextInt(10) == 0 ? TreeType.BIRCH : TreeType.OAK;
  }

  private void generateOakTree(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 4 + random.nextInt(3);
    BlockState log = Blocks.OAK_LOG.defaultBlockState();
    BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
    generateSimpleTree(world, pos, height, log, leaves);
  }

  private void generateBirchTree(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 5 + random.nextInt(3);
    BlockState log = Blocks.BIRCH_LOG.defaultBlockState();
    BlockState leaves = Blocks.BIRCH_LEAVES.defaultBlockState();
    generateSimpleTree(world, pos, height, log, leaves);
  }

  private void generateSpruceTree(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 6 + random.nextInt(4);
    BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
    BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState();
    generateConiferTree(world, pos, height, log, leaves);
  }

  private void generateJungleTree(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 4 + random.nextInt(8);
    BlockState log = Blocks.JUNGLE_LOG.defaultBlockState();
    BlockState leaves = Blocks.JUNGLE_LEAVES.defaultBlockState();
    generateSimpleTree(world, pos, height, log, leaves);
  }

  private void generateAcaciaTree(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 5 + random.nextInt(3);
    BlockState log = Blocks.ACACIA_LOG.defaultBlockState();
    BlockState leaves = Blocks.ACACIA_LEAVES.defaultBlockState();
    generateSimpleTree(world, pos, height, log, leaves);
  }

  private void generateDarkOakTree(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 6 + random.nextInt(3);
    BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
    BlockState leaves = Blocks.DARK_OAK_LEAVES.defaultBlockState();
    generateSimpleTree(world, pos, height, log, leaves);
  }

  private void generateSimpleTree(WorldGenLevel world, BlockPos pos, int height, BlockState log, BlockState leaves) {
    // Trunk
    for (int y = 0; y < height; y++) {
      BlockPos logPos = pos.above(y);
      // Skip positions outside chunk to avoid cascade loading
      if (!isInChunk(logPos)) {
        continue;
      }
      if (world.getBlockState(logPos).isAir() || world.getBlockState(logPos).is(BlockTags.LEAVES)) {
        safeSetBlock(world, logPos, log);
      }
    }

    // Canopy
    int canopyStart = height - 3;
    for (int y = canopyStart; y <= height + 1; y++) {
      int radius = y < height ? 2 : 1;
      if (y == height + 1) {
        radius = 0;
      }

      for (int x = -radius; x <= radius; x++) {
        for (int z = -radius; z <= radius; z++) {
          // Skip corners for rounder shape
          if (Math.abs(x) == radius && Math.abs(z) == radius && radius > 1) {
            continue;
          }
          // Skip trunk position
          if (x == 0 && z == 0 && y < height) {
            continue;
          }

          BlockPos leafPos = pos.offset(x, y, z);
          // Skip positions outside chunk to avoid cascade loading
          if (!isInChunk(leafPos)) {
            continue;
          }
          if (world.getBlockState(leafPos).isAir()) {
            safeSetBlock(world, leafPos, leaves);
          }
        }
      }
    }
  }

  private void generateConiferTree(WorldGenLevel world, BlockPos pos, int height, BlockState log, BlockState leaves) {
    // Trunk
    for (int y = 0; y < height; y++) {
      BlockPos logPos = pos.above(y);
      // Skip positions outside chunk to avoid cascade loading
      if (!isInChunk(logPos)) {
        continue;
      }
      if (world.getBlockState(logPos).isAir() || world.getBlockState(logPos).is(BlockTags.LEAVES)) {
        safeSetBlock(world, logPos, log);
      }
    }

    // Conical canopy
    for (int y = 2; y <= height; y++) {
      int radius = (height - y) / 2 + 1;
      if (y == height) {
        radius = 0;
      }

      for (int x = -radius; x <= radius; x++) {
        for (int z = -radius; z <= radius; z++) {
          // Skip trunk position
          if (x == 0 && z == 0 && y < height) {
            continue;
          }

          // Skip corners for rounder shape
          if (Math.abs(x) == radius && Math.abs(z) == radius && radius > 1) {
            continue;
          }

          BlockPos leafPos = pos.offset(x, y, z);
          // Skip positions outside chunk to avoid cascade loading
          if (!isInChunk(leafPos)) {
            continue;
          }
          if (world.getBlockState(leafPos).isAir()) {
            safeSetBlock(world, leafPos, leaves);
          }
        }
      }
    }

    // Top leaf
    safeSetBlock(world, pos.above(height), leaves);
  }

  private void generateGrass(WorldGenLevel world, RandomSource random, BlockPos chunkPos, Holder<Biome> biomeHolder) {
    // Skip grass in inappropriate biomes
    if (biomeHolder.is(BiomeTags.IS_BADLANDS) || biomeHolder.is(BiomeTags.IS_BEACH) ||
        biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_RIVER)) {
      return;
    }

    int grassPatches = Math.max(0, Math.round(getGrassPatches(biomeHolder) * grassMultiplier));

    for (int i = 0; i < grassPatches; i++) {
      int x = chunkPos.getX() + random.nextInt(16);
      int z = chunkPos.getZ() + random.nextInt(16);
      int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      BlockPos grassPos = new BlockPos(x, y, z);

      // Skip positions outside chunk to avoid cascade loading
      if (!isInChunk(grassPos)) {
        continue;
      }

      BlockPos belowPos = grassPos.below();
      if (!isInChunk(belowPos)) {
        continue;
      }
      BlockState ground = world.getBlockState(belowPos);
      if (ground.is(Blocks.GRASS_BLOCK) || ground.is(BlockTags.DIRT)) {
        if (world.getBlockState(grassPos).isAir()) {
          // Choose grass type
          BlockState grass = random.nextInt(3) == 0 ?
              Blocks.TALL_GRASS.defaultBlockState() :
              Blocks.GRASS.defaultBlockState();

          if (grass.is(Blocks.TALL_GRASS)) {
            // Only place tall grass if there's room
            BlockPos abovePos = grassPos.above();
            if (isInChunk(abovePos) && world.getBlockState(abovePos).isAir()) {
              safeSetBlock(world, grassPos, Blocks.TALL_GRASS.defaultBlockState());
            } else {
              safeSetBlock(world, grassPos, Blocks.GRASS.defaultBlockState());
            }
          } else {
            safeSetBlock(world, grassPos, grass);
          }
        }
      }
    }
  }

  private int getGrassPatches(Holder<Biome> biomeHolder) {
    if (biomeHolder.is(Biomes.PLAINS) || biomeHolder.is(Biomes.SUNFLOWER_PLAINS)) {
      return 30;
    } else if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
      return 25;
    } else if (biomeHolder.is(BiomeTags.IS_FOREST)) {
      return 15;
    } else if (biomeHolder.is(BiomeTags.IS_SAVANNA)) {
      return 10;
    }
    return 20;
  }

  private void generateFlowers(WorldGenLevel world, RandomSource random, BlockPos chunkPos, Holder<Biome> biomeHolder) {
    // Skip flowers in inappropriate biomes
    if (biomeHolder.is(BiomeTags.IS_BADLANDS) || biomeHolder.is(BiomeTags.IS_BEACH) ||
        biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_RIVER) ||
        biomeHolder.is(BiomeTags.IS_TAIGA)) {
      return;
    }

    int flowerPatches = Math.max(0, Math.round(getFlowerPatches(biomeHolder) * flowerMultiplier));

    for (int i = 0; i < flowerPatches; i++) {
      int x = chunkPos.getX() + random.nextInt(16);
      int z = chunkPos.getZ() + random.nextInt(16);
      int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      BlockPos flowerPos = new BlockPos(x, y, z);

      // Skip positions outside chunk to avoid cascade loading
      if (!isInChunk(flowerPos)) {
        continue;
      }

      BlockPos belowPos = flowerPos.below();
      if (!isInChunk(belowPos)) {
        continue;
      }
      BlockState ground = world.getBlockState(belowPos);
      if (ground.is(Blocks.GRASS_BLOCK) || ground.is(BlockTags.DIRT)) {
        if (world.getBlockState(flowerPos).isAir()) {
          BlockState flower = getRandomFlower(random, biomeHolder);
          safeSetBlock(world, flowerPos, flower);
        }
      }
    }
  }

  private int getFlowerPatches(Holder<Biome> biomeHolder) {
    if (biomeHolder.is(Biomes.FLOWER_FOREST)) {
      return 20;
    } else if (biomeHolder.is(Biomes.MEADOW)) {
      return 15;
    } else if (biomeHolder.is(Biomes.PLAINS) || biomeHolder.is(Biomes.SUNFLOWER_PLAINS)) {
      return 5;
    }
    return 2;
  }

  private BlockState getRandomFlower(RandomSource random, Holder<Biome> biomeHolder) {
    BlockState[] flowers;

    if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
      flowers = new BlockState[]{
          Blocks.ORANGE_TULIP.defaultBlockState(),
          Blocks.ALLIUM.defaultBlockState(),
          Blocks.BLUE_ORCHID.defaultBlockState()
      };
    } else if (biomeHolder.is(Biomes.SWAMP) || biomeHolder.is(Biomes.MANGROVE_SWAMP)) {
      flowers = new BlockState[]{
          Blocks.BLUE_ORCHID.defaultBlockState()
      };
    } else {
      flowers = new BlockState[]{
          Blocks.POPPY.defaultBlockState(),
          Blocks.DANDELION.defaultBlockState(),
          Blocks.CORNFLOWER.defaultBlockState(),
          Blocks.AZURE_BLUET.defaultBlockState(),
          Blocks.OXEYE_DAISY.defaultBlockState(),
          Blocks.RED_TULIP.defaultBlockState(),
          Blocks.ORANGE_TULIP.defaultBlockState(),
          Blocks.WHITE_TULIP.defaultBlockState(),
          Blocks.PINK_TULIP.defaultBlockState()
      };
    }

    return flowers[random.nextInt(flowers.length)];
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:biome_decoration";
  }

  private enum TreeType {
    OAK, BIRCH, SPRUCE, JUNGLE, ACACIA, DARK_OAK
  }
}
