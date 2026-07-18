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
 * Generates nested cubic chambers connected by tunnels - like looking into a
 * tesseract unfolded in 3D space. An outer cube of end stone bricks contains an
 * inner cube of purpur blocks, with connecting tunnels on each face and end rod
 * lighting. Deeply unsettling impossible geometry.
 */
public class TesseractChambersPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.025f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_OUTER_SIZE = 11;
  private static final int MAX_OUTER_SIZE = 17;

  private static final BlockState END_STONE_BRICKS = Blocks.END_STONE_BRICKS.defaultBlockState();
  private static final BlockState PURPUR_BLOCK = Blocks.PURPUR_BLOCK.defaultBlockState();
  private static final BlockState PURPUR_PILLAR = Blocks.PURPUR_PILLAR.defaultBlockState();
  private static final BlockState END_ROD = Blocks.END_ROD.defaultBlockState();
  private static final BlockState AIR = Blocks.AIR.defaultBlockState();
  private static final BlockState END_STONE = Blocks.END_STONE.defaultBlockState();
  private static final BlockState CHORUS_PLANT = Blocks.CHORUS_PLANT.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public TesseractChambersPopulator(long seed) {
    this(seed, null);
  }

  public TesseractChambersPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.count = PopulatorConfig.getInt(params, "count", DEFAULT_COUNT);
  }

  private static long positionHash(long seed, int x, int y, int z) {
    long h = seed;
    h ^= (long) x * 73856093L;
    h ^= (long) y * 19349663L;
    h ^= (long) z * 83492791L;
    h = h * 6364136223846793005L + 1442695040888963407L;
    return h;
  }

  private static float hashFloat(long hash) {
    return ((hash >>> 16) & 0xFFFFL) / 65536.0f;
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    for (int i = 0; i < count; i++) {
      if (random.nextFloat() >= spawnChance) {
        continue;
      }

      int x = chunkPos.getX() + random.nextInt(16);
      int z = chunkPos.getZ() + random.nextInt(16);
      int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

      int outerSize = MIN_OUTER_SIZE + random.nextInt(MAX_OUTER_SIZE - MIN_OUTER_SIZE + 1);

      if (outerSize % 2 == 0) {
        outerSize++;
      }

      int centerY = surfaceY + outerSize / 2 + 2;

      if (surfaceY <= world.getMinY() + 3
          || centerY + outerSize / 2 >= world.getMaxY() - 2) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      generateTesseract(world, chunkPos, x, centerY, z, outerSize);
    }
  }

  private void generateTesseract(WorldGenLevel world, BlockPos chunkPos,
                                 int cx, int cy, int cz, int outerSize) {
    int outerHalf = outerSize / 2;
    int innerSize = Math.max(5, outerSize / 2 - 1);

    if (innerSize % 2 == 0) {
      innerSize++;
    }
    int innerHalf = innerSize / 2;
    int tunnelRadius = 1;

    for (int dx = -outerHalf; dx <= outerHalf; dx++) {
      for (int dy = -outerHalf; dy <= outerHalf; dy++) {
        for (int dz = -outerHalf; dz <= outerHalf; dz++) {
          int bx = cx + dx;
          int by = cy + dy;
          int bz = cz + dz;
          BlockPos pos = new BlockPos(bx, by, bz);
          if (!isInWritableArea(pos, chunkPos)) {
            continue;
          }

          boolean outerShell = Math.abs(dx) == outerHalf
              || Math.abs(dy) == outerHalf
              || Math.abs(dz) == outerHalf;
          boolean innerShell = Math.abs(dx) >= innerHalf - 1 && Math.abs(dx) <= innerHalf
              && Math.abs(dy) >= innerHalf - 1 && Math.abs(dy) <= innerHalf
              && Math.abs(dz) >= innerHalf - 1 && Math.abs(dz) <= innerHalf;
          boolean insideInner = Math.abs(dx) < innerHalf - 1
              && Math.abs(dy) < innerHalf - 1
              && Math.abs(dz) < innerHalf - 1;

          boolean inTunnel = false;

          if (Math.abs(dy) <= tunnelRadius && Math.abs(dz) <= tunnelRadius
              && Math.abs(dx) > innerHalf - 1) {
            inTunnel = true;
          }

          if (Math.abs(dx) <= tunnelRadius && Math.abs(dz) <= tunnelRadius
              && Math.abs(dy) > innerHalf - 1) {
            inTunnel = true;
          }

          if (Math.abs(dx) <= tunnelRadius && Math.abs(dy) <= tunnelRadius
              && Math.abs(dz) > innerHalf - 1) {
            inTunnel = true;
          }

          if (outerShell && !inTunnel) {

            long blockHash = positionHash(seed, bx, by, bz);
            float roll = hashFloat(blockHash);
            if (roll < 0.08f) {
              world.setBlock(pos, END_STONE, 2);
            } else {
              world.setBlock(pos, END_STONE_BRICKS, 2);
            }
          } else if (outerShell && inTunnel) {

            world.setBlock(pos, AIR, 2);
          } else if (insideInner) {

            if (dx == 0 && dz == 0 && (dy == innerHalf - 2 || dy == -(innerHalf - 2))) {
              world.setBlock(pos, END_ROD, 2);
            } else if (dy == 0 && dz == 0 && (dx == innerHalf - 2 || dx == -(innerHalf - 2))) {
              world.setBlock(pos, END_ROD, 2);
            } else {
              world.setBlock(pos, AIR, 2);
            }
          } else if (innerShell && !inTunnel) {

            boolean isEdge = (Math.abs(dx) == innerHalf && Math.abs(dy) == innerHalf)
                || (Math.abs(dy) == innerHalf && Math.abs(dz) == innerHalf)
                || (Math.abs(dx) == innerHalf && Math.abs(dz) == innerHalf);
            if (isEdge) {
              world.setBlock(pos, PURPUR_PILLAR, 2);
            } else {
              world.setBlock(pos, PURPUR_BLOCK, 2);
            }
          } else if (inTunnel) {

            world.setBlock(pos, AIR, 2);
          } else {

            world.setBlock(pos, AIR, 2);
          }
        }
      }
    }

    int cornerOffset = (outerHalf + innerHalf) / 2;
    for (int sx = -1; sx <= 1; sx += 2) {
      for (int sy = -1; sy <= 1; sy += 2) {
        for (int sz = -1; sz <= 1; sz += 2) {
          int cornerX = cx + sx * cornerOffset;
          int cornerY = cy + sy * cornerOffset;
          int cornerZ = cz + sz * cornerOffset;
          long cornerHash = positionHash(seed ^ 0xC047L, cornerX, cornerY, cornerZ);
          if (hashFloat(cornerHash) < 0.6f) {
            for (int ch = 0; ch < 3; ch++) {
              BlockPos chorusPos = new BlockPos(cornerX, cornerY + ch, cornerZ);
              if (isInWritableArea(chorusPos, chunkPos)) {
                world.setBlock(chorusPos, CHORUS_PLANT, 2);
              }
            }
          }
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:tesseract_chambers";
  }
}
