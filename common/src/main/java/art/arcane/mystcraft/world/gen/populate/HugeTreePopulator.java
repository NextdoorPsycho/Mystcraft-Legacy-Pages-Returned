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
 * Huge tree populator that generates mega/giant trees.
 * Used when the Huge Trees symbol is applied to an age.
 * Generates large trees in addition to normal tree generation.
 */
public class HugeTreePopulator implements IPopulate {

  // Number of huge tree attempts per chunk
  private static final int DEFAULT_HUGE_TREES_PER_CHUNK = 3;
  private final long seed;
  private final int hugeTreesPerChunk;
  private BlockPos currentChunkPos;

  public HugeTreePopulator(long seed) {
    this(seed, null);
  }

  public HugeTreePopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.hugeTreesPerChunk = PopulatorConfig.getInt(params, "count", DEFAULT_HUGE_TREES_PER_CHUNK);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    this.currentChunkPos = chunkPos;

    // Sample biome at chunk center
    BlockPos centerPos = new BlockPos(chunkPos.getX() + 8, 64, chunkPos.getZ() + 8);
    Holder<Biome> biomeHolder = world.getBiome(centerPos);

    for (int i = 0; i < hugeTreesPerChunk; i++) {
      // Huge trees need 2x2 base, so start at 2-12 to keep canopy within writable area
      int x = chunkPos.getX() + 2 + random.nextInt(12);
      int z = chunkPos.getZ() + 2 + random.nextInt(12);
      int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      BlockPos treePos = new BlockPos(x, y, z);

      // Check if valid location for huge tree (2x2 area)
      if (!canSupportHugeTree(world, treePos)) {
        continue;
      }

      // Check if there's enough space
      if (!hasSpaceForHugeTree(world, treePos)) {
        continue;
      }

      // Generate huge tree based on biome
      generateHugeTree(world, random, treePos, biomeHolder);
    }
  }

  private boolean canSupportHugeTree(WorldGenLevel world, BlockPos pos) {
    // Check 2x2 area for valid ground
    for (int dx = 0; dx <= 1; dx++) {
      for (int dz = 0; dz <= 1; dz++) {
        BlockPos checkPos = pos.offset(dx, -1, dz);
        BlockState ground = world.getBlockState(checkPos);
        if (!ground.is(BlockTags.DIRT) && !ground.is(Blocks.GRASS_BLOCK) &&
            !ground.is(Blocks.PODZOL) && !ground.is(Blocks.MYCELIUM)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean hasSpaceForHugeTree(WorldGenLevel world, BlockPos pos) {
    // Check vertical clearance for huge tree (only within writable area)
    for (int dy = 0; dy < 16; dy++) {
      for (int dx = -1; dx <= 2; dx++) {
        for (int dz = -1; dz <= 2; dz++) {
          BlockPos checkPos = pos.offset(dx, dy, dz);
          if (!isInWritableArea(checkPos, currentChunkPos)) continue;
          BlockState state = world.getBlockState(checkPos);
          if (state.isSolid() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private void generateHugeTree(WorldGenLevel world, RandomSource random, BlockPos pos, Holder<Biome> biomeHolder) {
    // Determine tree type based on biome
    HugeTreeType treeType = getHugeTreeType(biomeHolder, random);

    switch (treeType) {
      case MEGA_SPRUCE -> generateMegaSpruce(world, random, pos);
      case MEGA_PINE -> generateMegaPine(world, random, pos);
      case MEGA_JUNGLE -> generateMegaJungle(world, random, pos);
      case DARK_OAK -> generateLargeDarkOak(world, random, pos);
      case GIANT_OAK -> generateGiantOak(world, random, pos);
    }
  }

  private HugeTreeType getHugeTreeType(Holder<Biome> biomeHolder, RandomSource random) {
    if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
      return HugeTreeType.MEGA_JUNGLE;
    } else if (biomeHolder.is(BiomeTags.IS_TAIGA)) {
      return random.nextBoolean() ? HugeTreeType.MEGA_SPRUCE : HugeTreeType.MEGA_PINE;
    } else if (biomeHolder.is(Biomes.DARK_FOREST)) {
      return HugeTreeType.DARK_OAK;
    }
    // Default to random mega spruce/giant oak
    return random.nextBoolean() ? HugeTreeType.MEGA_SPRUCE : HugeTreeType.GIANT_OAK;
  }

  private void generateMegaSpruce(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 13 + random.nextInt(8);
    BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
    BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState();

    // 2x2 trunk
    for (int y = 0; y < height; y++) {
      for (int dx = 0; dx <= 1; dx++) {
        for (int dz = 0; dz <= 1; dz++) {
          BlockPos logPos = pos.offset(dx, y, dz);
          setBlockIfAir(world, logPos, log);
        }
      }
    }

    // Conical canopy
    for (int y = height / 2; y <= height + 2; y++) {
      int radius = Math.max(1, (height - y + 4) / 2);

      for (int dx = -radius; dx <= radius + 1; dx++) {
        for (int dz = -radius; dz <= radius + 1; dz++) {
          // Distance from center (adjusted for 2x2 trunk)
          double dist = Math.sqrt((dx - 0.5) * (dx - 0.5) + (dz - 0.5) * (dz - 0.5));
          if (dist <= radius + 0.5) {
            BlockPos leafPos = pos.offset(dx, y, dz);
            setLeafIfAir(world, leafPos, leaves);
          }
        }
      }
    }
  }

  private void generateMegaPine(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 15 + random.nextInt(8);
    BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
    BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState();

    // 2x2 trunk
    for (int y = 0; y < height; y++) {
      for (int dx = 0; dx <= 1; dx++) {
        for (int dz = 0; dz <= 1; dz++) {
          BlockPos logPos = pos.offset(dx, y, dz);
          setBlockIfAir(world, logPos, log);
        }
      }
    }

    // Pine-style canopy (leaves only at top third)
    int canopyStart = height - height / 3;
    for (int y = canopyStart; y <= height + 2; y++) {
      int radius = Math.max(1, (height - y + 3) / 2);

      for (int dx = -radius; dx <= radius + 1; dx++) {
        for (int dz = -radius; dz <= radius + 1; dz++) {
          double dist = Math.sqrt((dx - 0.5) * (dx - 0.5) + (dz - 0.5) * (dz - 0.5));
          if (dist <= radius + 0.5) {
            BlockPos leafPos = pos.offset(dx, y, dz);
            setLeafIfAir(world, leafPos, leaves);
          }
        }
      }
    }
  }

  private void generateMegaJungle(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 20 + random.nextInt(15);
    BlockState log = Blocks.JUNGLE_LOG.defaultBlockState();
    BlockState leaves = Blocks.JUNGLE_LEAVES.defaultBlockState();

    // 2x2 trunk
    for (int y = 0; y < height; y++) {
      for (int dx = 0; dx <= 1; dx++) {
        for (int dz = 0; dz <= 1; dz++) {
          BlockPos logPos = pos.offset(dx, y, dz);
          setBlockIfAir(world, logPos, log);
        }
      }

      // Add vines on trunk
      if (y > 3 && random.nextInt(3) == 0) {
        for (int dx = -1; dx <= 2; dx++) {
          for (int dz = -1; dz <= 2; dz++) {
            if (dx == -1 || dx == 2 || dz == -1 || dz == 2) {
              if (random.nextInt(4) == 0) {
                BlockPos vinePos = pos.offset(dx, y, dz);
                if (isInWritableArea(vinePos, currentChunkPos) && world.getBlockState(vinePos).isAir()) {
                  world.setBlock(vinePos, Blocks.VINE.defaultBlockState(), 2);
                }
              }
            }
          }
        }
      }
    }

    // Large canopy
    for (int y = height - 8; y <= height + 2; y++) {
      int radius = Math.max(1, 4 - (height - y) / 3);

      for (int dx = -radius; dx <= radius + 1; dx++) {
        for (int dz = -radius; dz <= radius + 1; dz++) {
          double dist = Math.sqrt((dx - 0.5) * (dx - 0.5) + (dz - 0.5) * (dz - 0.5));
          if (dist <= radius + 0.5) {
            BlockPos leafPos = pos.offset(dx, y, dz);
            setLeafIfAir(world, leafPos, leaves);
          }
        }
      }
    }

    // Cocoa pods
    for (int y = 5; y < height - 2; y++) {
      if (random.nextInt(6) == 0) {
        for (int side = 0; side < 4; side++) {
          int dx = (side == 0) ? -1 : (side == 1) ? 2 : random.nextInt(2);
          int dz = (side == 2) ? -1 : (side == 3) ? 2 : random.nextInt(2);
          BlockPos cocoaPos = pos.offset(dx, y, dz);
          if (isInWritableArea(cocoaPos, currentChunkPos) && world.getBlockState(cocoaPos).isAir()) {
            // Would need proper facing calculation, simplified for now
            world.setBlock(cocoaPos, Blocks.COCOA.defaultBlockState(), 2);
          }
        }
      }
    }
  }

  private void generateLargeDarkOak(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 7 + random.nextInt(4);
    BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
    BlockState leaves = Blocks.DARK_OAK_LEAVES.defaultBlockState();

    // 2x2 trunk
    for (int y = 0; y < height; y++) {
      for (int dx = 0; dx <= 1; dx++) {
        for (int dz = 0; dz <= 1; dz++) {
          BlockPos logPos = pos.offset(dx, y, dz);
          setBlockIfAir(world, logPos, log);
        }
      }
    }

    // Branches
    generateDarkOakBranches(world, random, pos, height, log);

    // Wide canopy
    for (int y = height - 3; y <= height + 1; y++) {
      int radius = y < height ? 4 : 2;

      for (int dx = -radius; dx <= radius + 1; dx++) {
        for (int dz = -radius; dz <= radius + 1; dz++) {
          double dist = Math.sqrt((dx - 0.5) * (dx - 0.5) + (dz - 0.5) * (dz - 0.5));
          if (dist <= radius + 0.5) {
            BlockPos leafPos = pos.offset(dx, y, dz);
            setLeafIfAir(world, leafPos, leaves);
          }
        }
      }
    }
  }

  private void generateDarkOakBranches(WorldGenLevel world, RandomSource random, BlockPos pos, int height, BlockState log) {
    int branchHeight = height - 2;

    // Generate 2-4 branches
    int branches = 2 + random.nextInt(3);
    for (int b = 0; b < branches; b++) {
      int direction = random.nextInt(4);
      int dx = direction == 0 ? -1 : direction == 1 ? 2 : random.nextInt(2);
      int dz = direction == 2 ? -1 : direction == 3 ? 2 : random.nextInt(2);

      int branchLength = 2 + random.nextInt(2);
      for (int i = 0; i < branchLength; i++) {
        BlockPos branchPos = pos.offset(dx + (direction == 1 ? i : direction == 0 ? -i : 0),
            branchHeight + i / 2,
            dz + (direction == 3 ? i : direction == 2 ? -i : 0));
        setBlockIfAir(world, branchPos, log);
      }
    }
  }

  private void generateGiantOak(WorldGenLevel world, RandomSource random, BlockPos pos) {
    int height = 12 + random.nextInt(6);
    BlockState log = Blocks.OAK_LOG.defaultBlockState();
    BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();

    // 2x2 trunk
    for (int y = 0; y < height; y++) {
      for (int dx = 0; dx <= 1; dx++) {
        for (int dz = 0; dz <= 1; dz++) {
          BlockPos logPos = pos.offset(dx, y, dz);
          setBlockIfAir(world, logPos, log);
        }
      }
    }

    // Large spherical canopy
    int canopyRadius = 5;
    int canopyCenter = height - 2;

    for (int y = canopyCenter - canopyRadius; y <= canopyCenter + canopyRadius; y++) {
      int yDist = Math.abs(y - canopyCenter);
      int radius = (int) Math.sqrt(canopyRadius * canopyRadius - yDist * yDist);

      for (int dx = -radius; dx <= radius + 1; dx++) {
        for (int dz = -radius; dz <= radius + 1; dz++) {
          double dist = Math.sqrt((dx - 0.5) * (dx - 0.5) + (dz - 0.5) * (dz - 0.5) + yDist * yDist);
          if (dist <= canopyRadius + 0.5) {
            BlockPos leafPos = pos.offset(dx, y, dz);
            setLeafIfAir(world, leafPos, leaves);
          }
        }
      }
    }
  }

  private void setBlockIfAir(WorldGenLevel world, BlockPos pos, BlockState state) {
    if (!isInWritableArea(pos, currentChunkPos)) return;
    BlockState existing = world.getBlockState(pos);
    if (existing.isAir() || existing.is(BlockTags.LEAVES)) {
      world.setBlock(pos, state, 2);
    }
  }

  private void setLeafIfAir(WorldGenLevel world, BlockPos pos, BlockState leaves) {
    if (!isInWritableArea(pos, currentChunkPos)) return;
    if (world.getBlockState(pos).isAir()) {
      world.setBlock(pos, leaves, 2);
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:huge_trees";
  }

  private enum HugeTreeType {
    MEGA_SPRUCE, MEGA_PINE, MEGA_JUNGLE, DARK_OAK, GIANT_OAK
  }
}
