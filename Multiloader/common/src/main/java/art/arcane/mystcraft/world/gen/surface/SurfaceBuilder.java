package art.arcane.mystcraft.world.gen.surface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies biome-appropriate surface blocks to terrain.
 * Each biome has associated surface and subsurface blocks that get applied
 * to the top layers of terrain (replacing stone with grass/dirt, sand, etc.)
 */
public final class SurfaceBuilder {

  // Surface configuration per biome namespace:path
  // Format: [topBlock, fillerBlock, underwaterBlock, fillerDepth]
  private static final Map<String, SurfaceConfig> BIOME_SURFACES = new HashMap<>();
  // Default surface config (plains-like)
  private static final SurfaceConfig DEFAULT_CONFIG = new SurfaceConfig(
      Blocks.GRASS_BLOCK.defaultBlockState(),
      Blocks.DIRT.defaultBlockState(),
      Blocks.DIRT.defaultBlockState(),
      4
  );

  static {
    // Plains-type biomes
    registerSurface("plains", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("sunflower_plains", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("meadow", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("flower_forest", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);

    // Forest biomes
    registerSurface("forest", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("birch_forest", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("old_growth_birch_forest", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("dark_forest", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("cherry_grove", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);

    // Taiga biomes
    registerSurface("taiga", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("snowy_taiga", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("old_growth_pine_taiga", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("old_growth_spruce_taiga", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("grove", Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);

    // Snowy biomes
    registerSurface("snowy_plains", Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("snowy_slopes", Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState(), 4);
    registerSurface("ice_spikes", Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("frozen_peaks", Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState(), 4);

    // Desert biomes
    registerSurface("desert", Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), 4);

    // Badlands biomes
    registerSurface("badlands", Blocks.RED_SAND.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), 4);
    registerSurface("eroded_badlands", Blocks.RED_SAND.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), 4);
    registerSurface("wooded_badlands", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), 4);

    // Jungle biomes
    registerSurface("jungle", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("sparse_jungle", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("bamboo_jungle", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);

    // Savanna biomes
    registerSurface("savanna", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("savanna_plateau", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);
    registerSurface("windswept_savanna", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 2);

    // Mountain biomes
    registerSurface("windswept_hills", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);
    registerSurface("windswept_gravelly_hills", Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.STONE.defaultBlockState(), 3);
    registerSurface("windswept_forest", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState(), 3);
    registerSurface("stony_peaks", Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 1);
    registerSurface("jagged_peaks", Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 2);

    // Swamp biomes
    registerSurface("swamp", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.CLAY.defaultBlockState(), 4);
    registerSurface("mangrove_swamp", Blocks.MUD.defaultBlockState(), Blocks.MUD.defaultBlockState(), Blocks.MUD.defaultBlockState(), 4);

    // Beach and shore biomes
    registerSurface("beach", Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), 4);
    registerSurface("snowy_beach", Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.SAND.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), 4);
    registerSurface("stony_shore", Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 1);

    // Ocean biomes (underwater surfaces)
    registerSurface("ocean", Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);
    registerSurface("deep_ocean", Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);
    registerSurface("warm_ocean", Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), 3);
    registerSurface("lukewarm_ocean", Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), 3);
    registerSurface("cold_ocean", Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);
    registerSurface("frozen_ocean", Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);
    registerSurface("deep_lukewarm_ocean", Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), 3);
    registerSurface("deep_cold_ocean", Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);
    registerSurface("deep_frozen_ocean", Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);

    // River biomes
    registerSurface("river", Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);
    registerSurface("frozen_river", Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);

    // Mushroom biome
    registerSurface("mushroom_fields", Blocks.MYCELIUM.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4);

    // Cave biomes (use default)
    registerSurface("lush_caves", Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.CLAY.defaultBlockState(), 4);
    registerSurface("dripstone_caves", Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 1);
    registerSurface("deep_dark", Blocks.SCULK.defaultBlockState(), Blocks.SCULK.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState(), 2);
  }

  private SurfaceBuilder() {
  }

  private static void registerSurface(String biomeId, BlockState top, BlockState filler, BlockState underwater, int depth) {
    BIOME_SURFACES.put(biomeId, new SurfaceConfig(top, filler, underwater, depth));
  }

  /**
   * Builds biome-appropriate surfaces for a chunk.
   * Main entry point for applying biome surfaces to a chunk.
   */
  public static void buildBiomeSurfaces(ChunkAccess chunk, BiomeManager biomeManager, RandomState randomState, int seaLevel) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int maxY = chunk.getMaxBuildHeight();
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;

    long seed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L;
    RandomSource random = RandomSource.create(seed);

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        int worldX = chunkX * 16 + x;
        int worldZ = chunkZ * 16 + z;

        // Get biome at this column
        Holder<Biome> biome = biomeManager.getBiome(new BlockPos(worldX, seaLevel, worldZ));
        SurfaceConfig config = getSurfaceConfig(biome);

        // Find the surface - the topmost solid block exposed to air/water
        int surfaceY = findSurface(chunk, x, z, minY, maxY, pos);
        if (surfaceY < minY + 1) {
          continue;
        }

        // Randomize depth slightly
        int depth = config.depth + random.nextInt(2);

        // Check if underwater
        pos.set(x, surfaceY + 1, z);
        boolean underwater = chunk.getBlockState(pos).getBlock() == Blocks.WATER;

        // Apply surface blocks
        applySurface(chunk, x, z, surfaceY, minY, config, depth, underwater, random, pos);
      }
    }
  }

  private static SurfaceConfig getSurfaceConfig(Holder<Biome> biome) {
    if (biome == null) {
      return DEFAULT_CONFIG;
    }

    ResourceLocation biomeId = biome.unwrapKey()
        .map(key -> key.location())
        .orElse(null);

    if (biomeId != null) {
      SurfaceConfig config = BIOME_SURFACES.get(biomeId.getPath());
      if (config != null) {
        return config;
      }
    }

    return DEFAULT_CONFIG;
  }

  private static int findSurface(ChunkAccess chunk, int x, int z, int minY, int maxY, BlockPos.MutableBlockPos pos) {
    for (int y = maxY - 1; y >= minY; y--) {
      pos.set(x, y, z);
      BlockState state = chunk.getBlockState(pos);
      if (!state.isAir() && state.getBlock() != Blocks.WATER) {
        return y;
      }
    }
    return minY - 1;
  }

  private static void applySurface(ChunkAccess chunk, int x, int z, int surfaceY, int minY,
                                   SurfaceConfig config, int depth, boolean underwater,
                                   RandomSource random, BlockPos.MutableBlockPos pos) {
    // Only replace stone-like blocks
    pos.set(x, surfaceY, z);
    BlockState currentBlock = chunk.getBlockState(pos);
    if (!isReplaceable(currentBlock)) {
      return;
    }

    if (underwater) {
      // Underwater surface
      chunk.setBlockState(pos, config.underwater, false);
      for (int d = 1; d < depth && surfaceY - d >= minY; d++) {
        pos.set(x, surfaceY - d, z);
        if (isReplaceable(chunk.getBlockState(pos))) {
          chunk.setBlockState(pos, config.underwater, false);
        }
      }
    } else {
      // Above water - top block then filler
      chunk.setBlockState(pos, config.top, false);
      for (int d = 1; d < depth && surfaceY - d >= minY; d++) {
        pos.set(x, surfaceY - d, z);
        if (isReplaceable(chunk.getBlockState(pos))) {
          chunk.setBlockState(pos, config.filler, false);
        }
      }
    }
  }

  private static boolean isReplaceable(BlockState state) {
    return state.getBlock() == Blocks.STONE ||
        state.getBlock() == Blocks.DEEPSLATE ||
        state.getBlock() == Blocks.GRANITE ||
        state.getBlock() == Blocks.DIORITE ||
        state.getBlock() == Blocks.ANDESITE;
  }

  /**
   * Configuration for a biome's surface blocks.
   */
  private record SurfaceConfig(BlockState top, BlockState filler, BlockState underwater, int depth) {
  }
}
