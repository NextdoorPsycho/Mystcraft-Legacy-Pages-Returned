package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Bonefields populator that generates scattered skeletal formations resembling
 * the remains of colossal ancient creatures. Structures include ribcage arches,
 * spine trails, and skull mounds, decorated with soul sand patches and soul
 * torches. All formations fit within a single chunk.
 */
public class BonefieldsPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.07f;
  private static final int DEFAULT_COUNT = 1;
  private static final BlockState BONE = Blocks.BONE_BLOCK.defaultBlockState();
  private static final BlockState COAL = Blocks.COAL_BLOCK.defaultBlockState();
  private static final BlockState SOUL_SAND = Blocks.SOUL_SAND.defaultBlockState();
  private static final BlockState SOUL_TORCH = Blocks.SOUL_TORCH.defaultBlockState();
  private static final BlockState AIR = Blocks.AIR.defaultBlockState();
  private final long seed;
  private final float spawnChance;
  private final int count;

  public BonefieldsPopulator(long seed) {
    this(seed, null);
  }

  public BonefieldsPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.count = PopulatorConfig.getInt(params, "count", DEFAULT_COUNT);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    if (random.nextFloat() >= spawnChance) {
      return;
    }

    for (int i = 0; i < count; i++) {
      int x = chunkPos.getX() + random.nextInt(16);
      int z = chunkPos.getZ() + random.nextInt(16);
      int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

      if (surfaceY <= world.getMinBuildHeight() + 3 || surfaceY >= world.getMaxBuildHeight() - 12) {
        continue;
      }

      BlockPos surfacePos = new BlockPos(x, surfaceY, z);
      BlockState ground = world.getBlockState(surfacePos);
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int structureType = random.nextInt(3);
      switch (structureType) {
        case 0 -> generateRibcageArch(world, random, surfacePos, chunkPos);
        case 1 -> generateSpineTrail(world, random, surfacePos, chunkPos);
        case 2 -> generateSkullMound(world, random, surfacePos, chunkPos);
      }

      placeSoulSandPatches(world, random, surfacePos, chunkPos);
    }
  }

  private void generateRibcageArch(WorldGenLevel world, RandomSource random,
                                   BlockPos surface, BlockPos chunkPos) {
    int height = 5 + random.nextInt(4);
    int halfWidth = 3 + random.nextInt(4);
    int spacing = 3 + random.nextInt(2);
    int buryDepth = 1 + random.nextInt(2);
    int baseY = surface.getY() - buryDepth;

    int halfSpacing = spacing / 2;
    for (int arc = -1; arc <= 1; arc += 2) {
      int arcZ = surface.getZ() + arc * halfSpacing;

      for (int dx = -halfWidth; dx <= halfWidth; dx++) {

        double normalized = (double) dx / halfWidth;
        int arcY = (int) Math.round(height * (1.0 - normalized * normalized));
        if (arcY < 0) {
          continue;
        }

        int bx = surface.getX() + dx;

        for (int dy = 0; dy <= arcY; dy++) {
          BlockPos pos = new BlockPos(bx, baseY + dy, arcZ);
          if (isInWritableArea(pos, chunkPos)) {
            placeBoneBlock(world, pos);
          }
        }
      }
    }
  }

  private void generateSpineTrail(WorldGenLevel world, RandomSource random,
                                  BlockPos surface, BlockPos chunkPos) {
    int length = 10 + random.nextInt(11);
    int buryDepth = 1 + random.nextInt(2);
    double angle = random.nextDouble() * Math.PI * 2.0;
    double dx = Math.cos(angle);
    double dz = Math.sin(angle);
    int nextBump = 3 + random.nextInt(3);
    int bumpCounter = 0;

    for (int step = 0; step < length; step++) {
      int bx = surface.getX() + (int) Math.round(dx * step);
      int bz = surface.getZ() + (int) Math.round(dz * step);
      if (!isInWritableColumn(bx, bz, chunkPos)) {
        continue;
      }
      int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;
      int baseY = surfaceY - buryDepth;

      BlockPos pos = new BlockPos(bx, baseY, bz);
      if (!isInWritableArea(pos, chunkPos)) {
        continue;
      }

      placeBoneBlock(world, pos);

      BlockPos surfaceBlock = new BlockPos(bx, surfaceY, bz);
      if (isInWritableArea(surfaceBlock, chunkPos)) {
        placeBoneBlock(world, surfaceBlock);
      }

      bumpCounter++;
      if (bumpCounter >= nextBump) {
        bumpCounter = 0;
        nextBump = 3 + random.nextInt(3);
        BlockPos bumpPos = new BlockPos(bx, surfaceY + 1, bz);
        if (isInWritableArea(bumpPos, chunkPos)) {
          placeBoneBlock(world, bumpPos);
        }
      }
    }
  }

  private void generateSkullMound(WorldGenLevel world, RandomSource random,
                                  BlockPos surface, BlockPos chunkPos) {
    int buryDepth = 1 + random.nextInt(2);
    int baseY = surface.getY() - buryDepth;

    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = 0; dy <= 2; dy++) {
        for (int dz = -1; dz <= 1; dz++) {
          BlockPos pos = new BlockPos(surface.getX() + dx, baseY + dy, surface.getZ() + dz);
          if (!isInWritableArea(pos, chunkPos)) {
            continue;
          }

          if (dx == 0 && dy == 1 && dz == 0) {
            world.setBlock(pos, AIR, 2);
            continue;
          }

          placeBoneBlock(world, pos);
        }
      }
    }

    BlockPos leftEye = new BlockPos(surface.getX() - 1, baseY + 2, surface.getZ() + 1);
    BlockPos rightEye = new BlockPos(surface.getX() + 1, baseY + 2, surface.getZ() + 1);
    if (isInWritableArea(leftEye, chunkPos)) {
      world.setBlock(leftEye, COAL, 2);
    }
    if (isInWritableArea(rightEye, chunkPos)) {
      world.setBlock(rightEye, COAL, 2);
    }
  }

  private void placeSoulSandPatches(WorldGenLevel world, RandomSource random,
                                    BlockPos surface, BlockPos chunkPos) {
    int patchCount = 3 + random.nextInt(3);
    for (int p = 0; p < patchCount; p++) {
      int dx = random.nextInt(7) - 3;
      int dz = random.nextInt(7) - 3;
      int bx = surface.getX() + dx;
      int bz = surface.getZ() + dz;
      if (!isInWritableColumn(bx, bz, chunkPos)) {
        continue;
      }
      int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;

      BlockPos sandPos = new BlockPos(bx, surfaceY, bz);
      if (!isInWritableArea(sandPos, chunkPos)) {
        continue;
      }

      BlockState existing = world.getBlockState(sandPos);
      if (existing.isSolid() && !existing.is(Blocks.BONE_BLOCK)) {
        world.setBlock(sandPos, SOUL_SAND, 2);

        if (random.nextInt(4) == 0) {
          BlockPos torchPos = sandPos.above();
          if (isInWritableArea(torchPos, chunkPos) && world.getBlockState(torchPos).isAir()) {
            world.setBlock(torchPos, SOUL_TORCH, 2);
          }
        }
      }
    }
  }

  private void placeBoneBlock(WorldGenLevel world, BlockPos pos) {
    BlockState existing = world.getBlockState(pos);
    if (existing.isAir() ||
        existing.is(BlockTags.LEAVES) ||
        existing.is(Blocks.SNOW) ||
        existing.is(Blocks.VINE) ||
        existing.is(Blocks.WATER) ||
        existing.is(Blocks.STONE) ||
        existing.is(Blocks.DEEPSLATE) ||
        existing.is(Blocks.DIRT) ||
        existing.is(Blocks.GRASS_BLOCK) ||
        existing.is(Blocks.SAND) ||
        existing.is(Blocks.GRAVEL) ||
        existing.is(BlockTags.DIRT)) {
      world.setBlock(pos, BONE, 2);
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:bonefields";
  }
}
