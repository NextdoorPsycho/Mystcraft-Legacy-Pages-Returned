package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

/**
 * Spikes populator that generates ice-spike style formations.
 * Used when the Spikes symbol is applied to an age.
 * Generates tall pointed spikes rising from the ground.
 * <p>
 * 1.18.2 version - uses java.util.Random instead of RandomSource.
 */
public class SpikesPopulator implements IPopulate {

  private static final int DEFAULT_SPIKES_PER_CHUNK = 1;
  private static final int DEFAULT_MIN_HEIGHT = 10;
  private static final int DEFAULT_MAX_HEIGHT = 30;
  private static final float DEFAULT_COLD_TEMPERATURE = 0.15f;
  // ~8% of chunks spawn a spike
  private static final float DEFAULT_SPAWN_CHANCE = 0.08f;
  private final long seed;
  private final int spikesPerChunk;
  private final int minHeight;
  private final int maxHeight;
  private final float coldTemperature;
  private final float spawnChance;

  public SpikesPopulator(long seed) {
    this(seed, null);
  }

  public SpikesPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spikesPerChunk = PopulatorConfig.getInt(params, "count", DEFAULT_SPIKES_PER_CHUNK);
    this.minHeight = Math.max(1, PopulatorConfig.getInt(params, "min_height", DEFAULT_MIN_HEIGHT));
    this.maxHeight = Math.max(this.minHeight, PopulatorConfig.getInt(params, "max_height", DEFAULT_MAX_HEIGHT));
    this.coldTemperature = PopulatorConfig.getFloat(params, "cold_temperature", DEFAULT_COLD_TEMPERATURE);
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
  }

  @Override
  public void populate(WorldGenLevel world, Random random, BlockPos chunkPos) {
    int chunkX = chunkPos.getX();
    int chunkZ = chunkPos.getZ();

    for (int i = 0; i < spikesPerChunk; i++) {
      if (random.nextFloat() >= spawnChance) {
        continue;
      }

      int x = chunkX + random.nextInt(16);
      int z = chunkZ + random.nextInt(16);
      int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      BlockPos spikePos = new BlockPos(x, y, z);

      // Check if valid location for spike
      if (!canSupportSpike(world, spikePos)) {
        continue;
      }

      // Sample biome to determine spike material
      Holder<Biome> biomeHolder = world.getBiome(spikePos);
      float temperature = biomeHolder.value().getBaseTemperature();

      // Generate spike
      generateSpike(world, random, spikePos, temperature, chunkPos);
    }
  }

  private boolean canSupportSpike(WorldGenLevel world, BlockPos pos) {
    BlockState ground = world.getBlockState(pos.below());
    return ground.getMaterial().isSolid() && !ground.is(BlockTags.LEAVES);
  }

  private void generateSpike(WorldGenLevel world, Random random, BlockPos basePos, float temperature, BlockPos chunkPos) {
    // Choose spike material based on biome temperature
    BlockState spikeBlock = temperature < coldTemperature
        ? Blocks.PACKED_ICE.defaultBlockState()
        : Blocks.STONE.defaultBlockState();

    int height = minHeight + random.nextInt(maxHeight - minHeight + 1);

    // Generate tapering spike
    for (int y = 0; y < height; y++) {
      float progress = (float) y / height;

      // Radius decreases as we go up (tapering effect)
      // Start with radius 2-3 at base, taper to point
      int baseRadius = 2 + random.nextInt(2);
      int radius = (int) Math.max(0, baseRadius * (1.0f - progress));

      // Add some variation to the taper for a more natural look
      if (progress > 0.7f) {
        // Sharp point at top
        radius = (int) (radius * (1.0f - (progress - 0.7f) / 0.3f));
      }

      // Place blocks in a circular pattern
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
          double distance = Math.sqrt(dx * dx + dz * dz);

          // Circular cross-section with some irregularity
          if (distance <= radius + random.nextFloat() * 0.5) {
            BlockPos spikeBlockPos = basePos.offset(dx, y, dz);

            if (!isInWritableArea(spikeBlockPos, chunkPos)) {
              continue;
            }

            // Only place if air or replaceable
            BlockState existing = world.getBlockState(spikeBlockPos);
            if (existing.isAir() || existing.is(BlockTags.LEAVES) ||
                existing.is(Blocks.SNOW) || existing.is(Blocks.SNOW_BLOCK)) {
              world.setBlock(spikeBlockPos, spikeBlock, 2);
            }
          }
        }
      }

      // Add some crystalline features for ice spikes
      if (spikeBlock.is(Blocks.PACKED_ICE) && y > height / 2 && random.nextInt(4) == 0) {
        // Occasionally add small ice protrusions
        int direction = random.nextInt(4);
        int dx = (direction == 0) ? 1 : (direction == 1) ? -1 : 0;
        int dz = (direction == 2) ? 1 : (direction == 3) ? -1 : 0;

        BlockPos protrusionPos = basePos.offset(radius + dx, y, radius + dz);
        if (isInWritableArea(protrusionPos, chunkPos) && world.getBlockState(protrusionPos).isAir()) {
          world.setBlock(protrusionPos, Blocks.ICE.defaultBlockState(), 2);
        }
      }
    }

    // Add tip block at the very top
    if (height > 0) {
      BlockPos tipPos = basePos.above(height);
      if (isInWritableArea(tipPos, chunkPos) && world.getBlockState(tipPos).isAir()) {
        world.setBlock(tipPos, spikeBlock, 2);
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:spikes";
  }
}
