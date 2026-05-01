package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generates impossibly thin glass and ice bridges spanning vast distances at
 * extreme altitude. Bridges are 1-2 blocks wide and 60-200 blocks long, with a
 * gentle parabolic arc. Occasional lanterns hang below on chains. The bridges
 * appear to defy physics, connecting nothing to nothing.
 */
public class PhantomBridgesPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.04f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_LENGTH = 60;
  private static final int MAX_LENGTH = 200;
  private static final int MIN_HEIGHT = 150;
  private static final int MAX_HEIGHT = 220;
  private static final int ARC_HEIGHT = 8;

  private static final BlockState[] BRIDGE_MATERIALS = {
      Blocks.GLASS.defaultBlockState(),
      Blocks.WHITE_STAINED_GLASS.defaultBlockState(),
      Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
      Blocks.PACKED_ICE.defaultBlockState(),
      Blocks.BLUE_ICE.defaultBlockState(),
      Blocks.ICE.defaultBlockState()
  };

  private static final BlockState CHAIN = Blocks.CHAIN.defaultBlockState();
  private static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
  private static final BlockState SOUL_LANTERN = Blocks.SOUL_LANTERN.defaultBlockState();
  private static final BlockState GLASS_PANE = Blocks.GLASS_PANE.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public PhantomBridgesPopulator(long seed) {
    this(seed, null);
  }

  public PhantomBridgesPopulator(long seed, JsonObject params) {
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

      int startX = chunkPos.getX() + random.nextInt(16);
      int startZ = chunkPos.getZ() + random.nextInt(16);
      int bridgeY = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);

      if (bridgeY >= world.getMaxBuildHeight() - ARC_HEIGHT - 5) {
        continue;
      }

      int length = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
      double angle = random.nextDouble() * Math.PI * 2.0;
      int width = 1 + random.nextInt(2);

      long matHash = positionHash(seed, startX, bridgeY, startZ);
      int matBase = (int) ((matHash >>> 8) & 0xFF) % 3;
      boolean useSoulLanterns = (matHash & 1L) == 0;

      generateBridge(world, chunkPos, startX, bridgeY, startZ,
          length, angle, width, matBase, useSoulLanterns);
    }
  }

  private void generateBridge(WorldGenLevel world, BlockPos chunkPos,
                              int startX, int baseY, int startZ,
                              int length, double angle, int width,
                              int matBase, boolean useSoulLanterns) {
    double dx = Math.cos(angle);
    double dz = Math.sin(angle);

    double perpX = -dz;
    double perpZ = dx;

    int lanternInterval = 12 + (int) (hashFloat(positionHash(seed ^ 0xAA55L, startX, baseY, startZ)) * 8);

    for (int step = 0; step < length; step++) {
      double progress = (double) step / length;

      int arcY = (int) (ARC_HEIGHT * 4.0 * progress * (1.0 - progress));
      int blockY = baseY + arcY;

      double centerX = startX + dx * step;
      double centerZ = startZ + dz * step;

      for (int w = 0; w < width; w++) {
        double offsetX = w > 0 ? perpX * w : 0;
        double offsetZ = w > 0 ? perpZ * w : 0;

        int bx = (int) Math.round(centerX + offsetX);
        int bz = (int) Math.round(centerZ + offsetZ);
        BlockPos pos = new BlockPos(bx, blockY, bz);

        if (!isInWritableArea(pos, chunkPos)) {
          continue;
        }

        long blockHash = positionHash(seed, bx, blockY, bz);
        BlockState material = pickMaterial(blockHash, matBase);
        world.setBlock(pos, material, 2);

        if (width >= 2 && w == 0) {
          BlockPos railPos = new BlockPos(bx, blockY + 1, bz);
          if (isInWritableArea(railPos, chunkPos)) {
            world.setBlock(railPos, GLASS_PANE, 2);
          }
        }
      }

      if (step > 0 && step % lanternInterval == 0 && step < length - 5) {
        int lx = (int) Math.round(centerX);
        int lz = (int) Math.round(centerZ);
        int chainLength = 2 + (int) (hashFloat(positionHash(seed, lx, blockY, lz)) * 3);

        for (int cy = 1; cy <= chainLength; cy++) {
          BlockPos chainPos = new BlockPos(lx, blockY - cy, lz);
          if (isInWritableArea(chainPos, chunkPos)) {
            world.setBlock(chainPos, CHAIN, 2);
          }
        }
        BlockPos lanternPos = new BlockPos(lx, blockY - chainLength - 1, lz);
        if (isInWritableArea(lanternPos, chunkPos)) {
          world.setBlock(lanternPos, useSoulLanterns ? SOUL_LANTERN : LANTERN, 2);
        }
      }
    }

    placeAnchor(world, chunkPos, startX, baseY, startZ);
    int endX = (int) Math.round(startX + dx * (length - 1));
    int endZ = (int) Math.round(startZ + dz * (length - 1));
    int endArcY = baseY;
    placeAnchor(world, chunkPos, endX, endArcY, endZ);
  }

  private void placeAnchor(WorldGenLevel world, BlockPos chunkPos,
                           int x, int y, int z) {

    for (int dy = -2; dy <= 1; dy++) {
      BlockPos pos = new BlockPos(x, y + dy, z);
      if (isInWritableArea(pos, chunkPos)) {
        world.setBlock(pos, Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
      }
    }

    BlockPos topPos = new BlockPos(x, y + 2, z);
    if (isInWritableArea(topPos, chunkPos)) {
      world.setBlock(topPos, Blocks.END_ROD.defaultBlockState(), 2);
    }
  }

  private BlockState pickMaterial(long hash, int matBase) {
    float roll = hashFloat(hash);
    return switch (matBase) {
      case 0 -> roll < 0.5f ? BRIDGE_MATERIALS[0] :
          roll < 0.8f ? BRIDGE_MATERIALS[1] : BRIDGE_MATERIALS[2];
      case 1 -> roll < 0.4f ? BRIDGE_MATERIALS[3] :
          roll < 0.7f ? BRIDGE_MATERIALS[4] : BRIDGE_MATERIALS[5];
      default ->
          BRIDGE_MATERIALS[(int) ((hash >>> 24) & 0xFF) % BRIDGE_MATERIALS.length];
    };
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:phantom_bridges";
  }
}
