package art.arcane.mystcraft.world.gen.feature;

import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import art.arcane.mystcraft.platform.Services;
import art.arcane.mystcraft.world.gen.terrain.script.PerlinOctaves;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Procedural floating island generator using coherent Perlin noise for organic,
 * geological landmass shapes. Supports multiple island morphologies: classic plateaus,
 * rocky spires, mushroom caps, eroded formations, layered mesas, and monoliths.
 */
public class MapGenFloatingIslands implements ITerrainAlteration {

  private static final int RANGE = 5;
  private final long seed;
  private final int density;
  private final BlockState structureBlock;
  private final BlockState surfaceBlock;
  private final IslandStyle style;

  public MapGenFloatingIslands(long seed) {
    this(seed, 10, Blocks.STONE.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState(), IslandStyle.MIXED);
  }

  public MapGenFloatingIslands(long seed, int density, BlockState structureBlock, BlockState surfaceBlock) {
    this(seed, density, structureBlock, surfaceBlock, IslandStyle.MIXED);
  }

  public MapGenFloatingIslands(long seed, int density, BlockState structureBlock, BlockState surfaceBlock, IslandStyle style) {
    this.seed = seed;
    this.density = Math.max(1, density);
    this.structureBlock = structureBlock;
    this.surfaceBlock = surfaceBlock;
    this.style = style == null ? IslandStyle.MIXED : style;
  }

  @Override
  public void alterTerrain(ServerLevel world, int chunkX, int chunkZ, ChunkAccess chunk, RandomSource random) {
    for (int dx = -RANGE; dx <= RANGE; dx++) {
      for (int dz = -RANGE; dz <= RANGE; dz++) {
        int originChunkX = chunkX + dx;
        int originChunkZ = chunkZ + dz;

        RandomSource chunkRand = getChunkRandom(originChunkX, originChunkZ);

        if (chunkRand.nextInt(density) == 0) {
          int centerX = originChunkX * 16 + chunkRand.nextInt(16);
          int centerY = switch (style) {
            case SKYLANDS -> 110 + chunkRand.nextInt(90);
            case ARCHIPELAGO -> 95 + chunkRand.nextInt(70);
            case SHARDS -> 120 + chunkRand.nextInt(110);
            case RUINS -> 85 + chunkRand.nextInt(80);
            default -> 90 + chunkRand.nextInt(100);
          };
          int centerZ = originChunkZ * 16 + chunkRand.nextInt(16);

          IslandType type = pickIslandType(chunkRand);
          long islandSeed = chunkRand.nextLong();

          if (style == IslandStyle.MIXED) {
            generateIsland(chunk, chunkX, chunkZ, islandSeed, type,
                centerX, centerY, centerZ);
          } else {
            generateIslandByStyle(chunk, chunkX, chunkZ, islandSeed,
                centerX, centerY, centerZ);
          }
        }
      }
    }
  }

  private IslandType pickIslandType(RandomSource rand) {
    int roll = rand.nextInt(100);
    if (roll < 30) return IslandType.CLASSIC;
    if (roll < 45) return IslandType.SPIRE;
    if (roll < 60) return IslandType.MUSHROOM;
    if (roll < 75) return IslandType.ERODED;
    if (roll < 90) return IslandType.MESA;
    return IslandType.MONOLITH;
  }

  private void generateIslandByStyle(ChunkAccess chunk, int chunkX, int chunkZ,
                                     long islandSeed, int centerX, int centerY, int centerZ) {
    switch (style) {
      case SKYLANDS -> generateSkylands(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
      case ARCHIPELAGO -> generateArchipelago(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
      case SHARDS -> generateShards(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
      case RUINS -> generateRuins(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
      default -> generateClassic(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
    }
  }

  private void generateIsland(ChunkAccess chunk, int chunkX, int chunkZ,
                              long islandSeed, IslandType type,
                              int centerX, int centerY, int centerZ) {
    switch (type) {
      case CLASSIC -> generateClassic(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
      case SPIRE -> generateSpire(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
      case MUSHROOM -> generateMushroom(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
      case ERODED -> generateEroded(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
      case MESA -> generateMesa(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
      case MONOLITH -> generateMonolith(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
    }
  }

  // ========== NOISE FACTORY ==========

  private IslandNoise createIslandNoise(long islandSeed) {
    return new IslandNoise(islandSeed);
  }

  // ========== CLASSIC ISLAND ==========

  private void generateClassic(ChunkAccess chunk, int chunkX, int chunkZ,
                               long islandSeed, int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(islandSeed);
    int radiusX = 8 + rand.nextInt(20);
    int radiusZ = 8 + rand.nextInt(20);
    int heightUp = 4 + rand.nextInt(6);
    int heightDown = 8 + rand.nextInt(18);
    IslandNoise noise = createIslandNoise(islandSeed);

    int minBuild = chunk.getMinBuildHeight();
    int maxBuild = chunk.getMaxBuildHeight() - 1;

    int localMinX = Math.max(0, centerX - radiusX - 2 - chunkX * 16);
    int localMaxX = Math.min(15, centerX + radiusX + 2 - chunkX * 16);
    int localMinZ = Math.max(0, centerZ - radiusZ - 2 - chunkZ * 16);
    int localMaxZ = Math.min(15, centerZ + radiusZ + 2 - chunkZ * 16);

    if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
      return;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
    boolean isLarge = Math.min(radiusX, radiusZ) >= 18;

    for (int x = localMinX; x <= localMaxX; x++) {
      int worldX = chunkX * 16 + x;
      double xDist = (worldX - centerX) / (double) radiusX;

      for (int z = localMinZ; z <= localMaxZ; z++) {
        int worldZ = chunkZ * 16 + z;
        double zDist = (worldZ - centerZ) / (double) radiusZ;

        // Warp the distance field with coherent noise for organic outline
        double warpX = noise.shape.sample2D(worldX * 0.02, worldZ * 0.015, 2.0, 0.5) * 0.35;
        double warpZ = noise.shape.sample2D(worldX * 0.015 + 500, worldZ * 0.02 + 500, 2.0, 0.5) * 0.35;
        double warpedXDist = xDist + warpX;
        double warpedZDist = zDist + warpZ;
        double horizDist = warpedXDist * warpedXDist + warpedZDist * warpedZDist;
        if (horizDist >= 1.0) continue;

        double falloff = 1.0 - horizDist;

        // Coherent surface height variation
        double surfaceNoise = noise.surface.sample2D(worldX * 0.06, worldZ * 0.06, 2.0, 0.5) * 4.0
            + noise.surface.sample2D(worldX * 0.15 + 200, worldZ * 0.15 + 200, 2.0, 0.5) * 1.5;
        int localHeightUp = (int) (heightUp * falloff + surfaceNoise);
        int localHeightDown = (int) (heightDown * Math.pow(falloff, 1.5));

        // Root tendrils hanging below
        double tendrilNoise = Math.abs(noise.carve.sample3D(
            worldX * 0.08, (centerY - localHeightDown) * 0.12, worldZ * 0.08, 2.0, 0.5));
        if (tendrilNoise > 0.6 && horizDist < 0.7) {
          int tendrilLen = (int) ((tendrilNoise - 0.6) * 60.0);
          localHeightDown += tendrilLen;
        }

        // Stalactite rim on outer portion
        if (horizDist > 0.5 && horizDist < 0.95) {
          double stalNoise = noise.detail.sample2D(worldX * 0.3, worldZ * 0.3, 2.0, 0.5);
          if (stalNoise > 0.3) {
            localHeightDown += (int) ((stalNoise - 0.3) * 8.0);
          }
        }

        int yMin = Math.max(minBuild, centerY - localHeightDown);
        int yMax = Math.min(maxBuild, centerY + Math.max(0, localHeightUp));

        for (int y = yMin; y <= yMax; y++) {
          boolean inIsland;
          if (y >= centerY) {
            double yDistUp = (localHeightUp > 0) ? (y - centerY) / (double) localHeightUp : 1.0;
            inIsland = horizDist + yDistUp * yDistUp < 1.0;
          } else {
            double yDistDown = (localHeightDown > 0) ? (centerY - y) / (double) localHeightDown : 1.0;
            double bottomScale = 1.0 - yDistDown * 0.9;
            inIsland = horizDist < bottomScale * bottomScale;
          }

          if (!inIsland) continue;

          // Internal cave carving for large islands
          if (isLarge && y < centerY - 2 && y > centerY - localHeightDown + 3) {
            double cave = noise.carve.sample3D(worldX * 0.08, y * 0.1, worldZ * 0.08, 2.0, 0.5);
            if (cave > 0.35) continue;
          }

          pos.set(x, y, z);
          if (!chunk.getBlockState(pos).isAir()) continue;

          BlockState block = pickClassicBlock(y, centerY, yMax, yMin, worldX, worldZ, noise, islandSeed);
          chunk.setBlockState(pos, block, false);

          // Surface decoration
          if (y == yMax && y >= centerY) {
            decorateSurface(chunk, pos, above, worldX, worldZ, noise, radiusX, radiusZ);
          }

          // Underside glow accents
          if (y == yMin && y < centerY - 3) {
            maybeGlowUnderside(chunk, pos, above, worldX, worldZ, noise);
          }
        }
      }
    }

    // Pond and trees for large islands
    if (isLarge) {
      generatePond(chunk, chunkX, chunkZ, noise, centerX, centerY + heightUp - 1, centerZ, Math.min(radiusX, radiusZ));
      generateTrees(chunk, chunkX, chunkZ, noise, islandSeed, centerX, centerY, centerZ, radiusX, radiusZ, heightUp);
    } else if (Math.min(radiusX, radiusZ) >= 12) {
      generateTrees(chunk, chunkX, chunkZ, noise, islandSeed, centerX, centerY, centerZ, radiusX, radiusZ, heightUp);
    }
  }

  // ========== SPIRE ISLAND ==========

  private void generateSpire(ChunkAccess chunk, int chunkX, int chunkZ,
                             long islandSeed, int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(islandSeed);
    int baseRadius = 4 + rand.nextInt(8);
    int totalHeight = 15 + rand.nextInt(35);
    IslandNoise noise = createIslandNoise(islandSeed);

    int minBuild = chunk.getMinBuildHeight();
    int maxBuild = chunk.getMaxBuildHeight() - 1;

    int maxRadius = baseRadius + 5;
    int localMinX = Math.max(0, centerX - maxRadius - chunkX * 16);
    int localMaxX = Math.min(15, centerX + maxRadius - chunkX * 16);
    int localMinZ = Math.max(0, centerZ - maxRadius - chunkZ * 16);
    int localMaxZ = Math.min(15, centerZ + maxRadius - chunkZ * 16);

    if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
      return;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int yBottom = centerY - totalHeight / 3;
    int yTop = centerY + totalHeight * 2 / 3;

    for (int x = localMinX; x <= localMaxX; x++) {
      int worldX = chunkX * 16 + x;
      for (int z = localMinZ; z <= localMaxZ; z++) {
        int worldZ = chunkZ * 16 + z;

        double dx = worldX - centerX;
        double dz = worldZ - centerZ;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        for (int y = Math.max(minBuild, yBottom); y <= Math.min(maxBuild, yTop); y++) {
          double t = (y - yBottom) / (double) (yTop - yBottom);

          // Spiraling ledge radius: oscillates with height + noise
          double angle = Math.atan2(dz, dx);
          double spiralWarp = noise.shape.sample2D(y * 0.15, angle * 2.0 + 100, 2.0, 0.5) * 2.0;
          double ledgeOscillation = Math.sin(y * 0.25 + angle * 2.0) * 1.5;

          double radiusAtY;
          if (t < 0.25) {
            // Bottom taper - pointed
            double taperT = t / 0.25;
            radiusAtY = baseRadius * taperT * 0.6 + spiralWarp * taperT;
          } else {
            // Main body narrows toward top with spiral ledges
            double narrowFactor = 1.0 - (t - 0.25) / 0.75;
            radiusAtY = baseRadius * narrowFactor * narrowFactor + spiralWarp + ledgeOscillation + 1.0;
          }

          if (horizDist <= radiusAtY) {
            pos.set(x, y, z);
            if (!chunk.getBlockState(pos).isAir()) continue;

            BlockState block;
            if (t > 0.85) {
              // Crystal/calcite cap
              double capNoise = noise.detail.sample3D(worldX * 0.15, y * 0.15, worldZ * 0.15, 2.0, 0.5);
              if (capNoise > 0.2) {
                block = Blocks.CALCITE.defaultBlockState();
              } else if (capNoise > -0.1) {
                block = Blocks.AMETHYST_BLOCK.defaultBlockState();
              } else {
                block = Blocks.SNOW_BLOCK.defaultBlockState();
              }
            } else {
              block = pickSpireBlock(t, noise, worldX, y, worldZ);
            }
            chunk.setBlockState(pos, block, false);
          }
        }
      }
    }
  }

  // ========== MUSHROOM ISLAND ==========

  private void generateMushroom(ChunkAccess chunk, int chunkX, int chunkZ,
                                long islandSeed, int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(islandSeed);
    int stemRadius = 2 + rand.nextInt(3);
    int capRadius = 10 + rand.nextInt(16);
    int stemHeight = 10 + rand.nextInt(20);
    int capThickness = 4 + rand.nextInt(6);
    IslandNoise noise = createIslandNoise(islandSeed);

    int minBuild = chunk.getMinBuildHeight();
    int maxBuild = chunk.getMaxBuildHeight() - 1;

    int maxR = capRadius + 4;
    int localMinX = Math.max(0, centerX - maxR - chunkX * 16);
    int localMaxX = Math.min(15, centerX + maxR - chunkX * 16);
    int localMinZ = Math.max(0, centerZ - maxR - chunkZ * 16);
    int localMaxZ = Math.min(15, centerZ + maxR - chunkZ * 16);

    if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
      return;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
    int stemBottom = centerY - stemHeight / 2;
    int stemTop = centerY + stemHeight / 2;
    int capBottom = stemTop;
    int capTop = stemTop + capThickness;

    for (int x = localMinX; x <= localMaxX; x++) {
      int worldX = chunkX * 16 + x;
      for (int z = localMinZ; z <= localMaxZ; z++) {
        int worldZ = chunkZ * 16 + z;

        double dx = worldX - centerX;
        double dz = worldZ - centerZ;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        // Noise-warped stem with organic wobble
        double stemWarp = noise.shape.sample2D(worldX * 0.08, worldZ * 0.08, 2.0, 0.5) * 2.0;
        double effectiveStemRadius = stemRadius + stemWarp;
        if (horizDist <= Math.max(1.0, effectiveStemRadius)) {
          for (int y = Math.max(minBuild, stemBottom); y <= Math.min(maxBuild, stemTop); y++) {
            // Taper stem slightly at bottom
            double stemT = (y - stemBottom) / (double) (stemTop - stemBottom);
            double taperRadius = effectiveStemRadius * (0.7 + stemT * 0.3);
            if (horizDist <= Math.max(1.0, taperRadius)) {
              pos.set(x, y, z);
              if (chunk.getBlockState(pos).isAir()) {
                chunk.setBlockState(pos, Blocks.MUSHROOM_STEM.defaultBlockState(), false);
              }
            }
          }
        }

        // Noise-warped dome cap
        double capWarpX = noise.shape.sample2D(worldX * 0.025, worldZ * 0.02, 2.0, 0.5) * 0.3;
        double capWarpZ = noise.shape.sample2D(worldX * 0.02 + 300, worldZ * 0.025 + 300, 2.0, 0.5) * 0.3;
        double warpedDx = (dx / capRadius) + capWarpX;
        double warpedDz = (dz / capRadius) + capWarpZ;
        double normalizedHoriz = Math.sqrt(warpedDx * warpedDx + warpedDz * warpedDz);

        if (normalizedHoriz <= 1.0) {
          double capFalloff = 1.0 - normalizedHoriz * normalizedHoriz;
          int localCapHeight = (int) (capThickness * capFalloff);

          for (int y = Math.max(minBuild, capBottom); y <= Math.min(maxBuild, capBottom + localCapHeight); y++) {
            pos.set(x, y, z);
            if (chunk.getBlockState(pos).isAir()) {
              BlockState block;
              if (y == capBottom + localCapHeight) {
                // Surface: mycelium
                block = Blocks.MYCELIUM.defaultBlockState();
              } else if (y >= capBottom + localCapHeight - 2) {
                block = Blocks.DIRT.defaultBlockState();
              } else {
                block = structureBlock;
              }
              chunk.setBlockState(pos, block, false);

              // Surface decoration with mushrooms
              if (y == capBottom + localCapHeight) {
                decorateMushroomSurface(chunk, pos, above, worldX, worldZ, noise);
              }
            }
          }

          // Drip roots hanging from cap underside in a ring
          if (normalizedHoriz > 0.25 && normalizedHoriz < 0.85) {
            double dripNoise = noise.detail.sample2D(worldX * 0.2, worldZ * 0.2, 2.0, 0.5);
            if (dripNoise > 0.3) {
              int dripLength = (int) ((dripNoise - 0.3) * 15.0);
              for (int dy = 1; dy <= dripLength; dy++) {
                int y = capBottom - dy;
                if (y < minBuild) break;
                pos.set(x, y, z);
                if (chunk.getBlockState(pos).isAir()) {
                  BlockState dripBlock = (dy <= 2) ? Blocks.MUSHROOM_STEM.defaultBlockState()
                      : Blocks.HANGING_ROOTS.defaultBlockState();
                  chunk.setBlockState(pos, dripBlock, false);
                }
              }
            }
          }
        }
      }
    }
  }

  // ========== ERODED ISLAND ==========

  private void generateEroded(ChunkAccess chunk, int chunkX, int chunkZ,
                              long islandSeed, int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(islandSeed);
    int radiusX = 10 + rand.nextInt(18);
    int radiusZ = 10 + rand.nextInt(18);
    int heightUp = 3 + rand.nextInt(5);
    int heightDown = 6 + rand.nextInt(14);
    IslandNoise noise = createIslandNoise(islandSeed);

    int minBuild = chunk.getMinBuildHeight();
    int maxBuild = chunk.getMaxBuildHeight() - 1;

    int localMinX = Math.max(0, centerX - radiusX - 2 - chunkX * 16);
    int localMaxX = Math.min(15, centerX + radiusX + 2 - chunkX * 16);
    int localMinZ = Math.max(0, centerZ - radiusZ - 2 - chunkZ * 16);
    int localMaxZ = Math.min(15, centerZ + radiusZ + 2 - chunkZ * 16);

    if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
      return;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();

    for (int x = localMinX; x <= localMaxX; x++) {
      int worldX = chunkX * 16 + x;
      double xDist = (worldX - centerX) / (double) radiusX;

      for (int z = localMinZ; z <= localMaxZ; z++) {
        int worldZ = chunkZ * 16 + z;
        double zDist = (worldZ - centerZ) / (double) radiusZ;

        // Aggressive noise warp for very irregular outline
        double warpX = noise.shape.sample2D(worldX * 0.025, worldZ * 0.02, 2.0, 0.5) * 0.45;
        double warpZ = noise.shape.sample2D(worldX * 0.02 + 400, worldZ * 0.025 + 400, 2.0, 0.5) * 0.45;
        double warpedXDist = xDist + warpX;
        double warpedZDist = zDist + warpZ;
        double horizDist = warpedXDist * warpedXDist + warpedZDist * warpedZDist;
        if (horizDist >= 1.0) continue;

        double falloff = 1.0 - horizDist;

        double surfaceNoise = noise.surface.sample2D(worldX * 0.08, worldZ * 0.08, 2.0, 0.5) * 3.0;
        int localHeightUp = (int) (heightUp * falloff + surfaceNoise);
        int localHeightDown = (int) (heightDown * Math.pow(falloff, 1.5));

        int yMin = Math.max(minBuild, centerY - localHeightDown);
        int yMax = Math.min(maxBuild, centerY + Math.max(0, localHeightUp));

        for (int y = yMin; y <= yMax; y++) {
          // Aggressive erosion holes - lower threshold for more carving
          double erosion = noise.carve.sample3D(worldX * 0.1, y * 0.12, worldZ * 0.1, 2.0, 0.5);
          if (erosion > 0.25) continue;

          boolean inIsland;
          if (y >= centerY) {
            double yDistUp = (localHeightUp > 0) ? (y - centerY) / (double) localHeightUp : 1.0;
            inIsland = horizDist + yDistUp * yDistUp < 1.0;
          } else {
            double yDistDown = (localHeightDown > 0) ? (centerY - y) / (double) localHeightDown : 1.0;
            double bottomScale = 1.0 - yDistDown * 0.85;
            inIsland = horizDist < bottomScale * bottomScale;
          }

          if (inIsland) {
            pos.set(x, y, z);
            if (!chunk.getBlockState(pos).isAir()) continue;

            BlockState block = pickErodedBlock(y, centerY, yMax, noise, worldX, worldZ);
            chunk.setBlockState(pos, block, false);
            if (y == yMax && y >= centerY) {
              decorateSurface(chunk, pos, above, worldX, worldZ, noise, radiusX, radiusZ);
            }
          }
        }
      }
    }
  }

  // ========== MESA ISLAND ==========

  private void generateMesa(ChunkAccess chunk, int chunkX, int chunkZ,
                            long islandSeed, int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(islandSeed);
    int radiusX = 12 + rand.nextInt(16);
    int radiusZ = 12 + rand.nextInt(16);
    int plateauHeight = 2 + rand.nextInt(3);
    int totalDown = 12 + rand.nextInt(20);
    IslandNoise noise = createIslandNoise(islandSeed);

    int minBuild = chunk.getMinBuildHeight();
    int maxBuild = chunk.getMaxBuildHeight() - 1;

    int localMinX = Math.max(0, centerX - radiusX - 2 - chunkX * 16);
    int localMaxX = Math.min(15, centerX + radiusX + 2 - chunkX * 16);
    int localMinZ = Math.max(0, centerZ - radiusZ - 2 - chunkZ * 16);
    int localMaxZ = Math.min(15, centerZ + radiusZ + 2 - chunkZ * 16);

    if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
      return;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();

    for (int x = localMinX; x <= localMaxX; x++) {
      int worldX = chunkX * 16 + x;
      double xDist = (worldX - centerX) / (double) radiusX;

      for (int z = localMinZ; z <= localMaxZ; z++) {
        int worldZ = chunkZ * 16 + z;
        double zDist = (worldZ - centerZ) / (double) radiusZ;

        // Warp for irregular mesa outline
        double warpX = noise.shape.sample2D(worldX * 0.02, worldZ * 0.015, 2.0, 0.5) * 0.25;
        double warpZ = noise.shape.sample2D(worldX * 0.015 + 600, worldZ * 0.02 + 600, 2.0, 0.5) * 0.25;
        double warpedXDist = xDist + warpX;
        double warpedZDist = zDist + warpZ;
        double horizDist = warpedXDist * warpedXDist + warpedZDist * warpedZDist;
        if (horizDist >= 1.0) continue;

        double falloff = 1.0 - horizDist;

        // Stepped cliff faces with noise-offset radius per step
        double stepNoise = noise.surface.sample2D(worldX * 0.04, worldZ * 0.04, 2.0, 0.5) * 0.08;
        double steppedFalloff = Math.floor((falloff + stepNoise) * 5.0) / 5.0;
        steppedFalloff = Math.max(0.0, Math.min(1.0, steppedFalloff));
        int localDown = (int) (totalDown * steppedFalloff);

        // Overhangs on step edges
        double overhangNoise = noise.carve.sample2D(worldX * 0.06, worldZ * 0.06, 2.0, 0.5);
        if (overhangNoise > 0.4 && steppedFalloff < falloff - 0.1) {
          localDown += 2;
        }

        int localUp = (int) (plateauHeight * (falloff > 0.5 ? 1.0 : falloff * 2.0));

        int yMin = Math.max(minBuild, centerY - localDown);
        int yMax = Math.min(maxBuild, centerY + localUp);

        for (int y = yMin; y <= yMax; y++) {
          pos.set(x, y, z);
          if (!chunk.getBlockState(pos).isAir()) continue;

          BlockState block = pickMesaBlock(y, centerY, islandSeed, noise, worldX, worldZ);
          chunk.setBlockState(pos, block, false);
          if (y == yMax) {
            decorateSurface(chunk, pos, above, worldX, worldZ, noise, radiusX, radiusZ);
          }
        }
      }
    }
  }

  // ========== MONOLITH ISLAND (replaces Archipelago type in MIXED) ==========

  private void generateMonolith(ChunkAccess chunk, int chunkX, int chunkZ,
                                long islandSeed, int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(islandSeed);
    int radiusX = 5 + rand.nextInt(8);
    int radiusZ = 5 + rand.nextInt(8);
    int totalHeight = (radiusX + radiusZ) + rand.nextInt(20); // tall relative to width
    IslandNoise noise = createIslandNoise(islandSeed);

    int minBuild = chunk.getMinBuildHeight();
    int maxBuild = chunk.getMaxBuildHeight() - 1;

    int maxR = Math.max(radiusX, radiusZ) + 3;
    int localMinX = Math.max(0, centerX - maxR - chunkX * 16);
    int localMaxX = Math.min(15, centerX + maxR - chunkX * 16);
    int localMinZ = Math.max(0, centerZ - maxR - chunkZ * 16);
    int localMaxZ = Math.min(15, centerZ + maxR - chunkZ * 16);

    if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
      return;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int yBottom = Math.max(minBuild, centerY - totalHeight / 3);
    int yTop = Math.min(maxBuild, centerY + totalHeight * 2 / 3);

    for (int x = localMinX; x <= localMaxX; x++) {
      int worldX = chunkX * 16 + x;

      for (int z = localMinZ; z <= localMaxZ; z++) {
        int worldZ = chunkZ * 16 + z;

        for (int y = yBottom; y <= yTop; y++) {
          double t = (double) (y - yBottom) / (yTop - yBottom);

          // Tilt the slab slightly using noise
          double tiltX = noise.shape.sample2D(centerX * 0.01, centerZ * 0.01, 2.0, 0.5) * 0.15;
          double tiltZ = noise.shape.sample2D(centerX * 0.01 + 200, centerZ * 0.01 + 200, 2.0, 0.5) * 0.15;

          double effectiveCenterX = centerX + (y - centerY) * tiltX;
          double effectiveCenterZ = centerZ + (y - centerY) * tiltZ;

          double xDist = (worldX - effectiveCenterX) / (double) radiusX;
          double zDist = (worldZ - effectiveCenterZ) / (double) radiusZ;

          // Subtle noise warp
          double warp = noise.surface.sample2D(worldX * 0.05, worldZ * 0.05 + y * 0.02, 2.0, 0.5) * 0.2;

          // Taper at top and bottom
          double verticalScale = 1.0;
          if (t < 0.1) {
            verticalScale = t / 0.1;
          } else if (t > 0.9) {
            verticalScale = (1.0 - t) / 0.1;
          }
          verticalScale = Math.max(0.3, verticalScale);

          double dist = (xDist * xDist + zDist * zDist) / (verticalScale * verticalScale) + warp * warp;
          if (dist >= 1.0) continue;

          pos.set(x, y, z);
          if (!chunk.getBlockState(pos).isAir()) continue;

          BlockState block = pickMonolithBlock(t, noise, worldX, y, worldZ);
          chunk.setBlockState(pos, block, false);
        }
      }
    }
  }

  // ========== ARCHIPELAGO STYLE ==========

  private void generateArchipelago(ChunkAccess chunk, int chunkX, int chunkZ,
                                   long islandSeed, int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(islandSeed);
    int count = 5 + rand.nextInt(6);

    // Central hub island
    if (rand.nextInt(100) < 60) {
      generateClassic(chunk, chunkX, chunkZ, islandSeed ^ 0xA55A, centerX, centerY, centerZ);
    }

    for (int i = 0; i < count; i++) {
      int offsetX = rand.nextInt(60) - 30;
      int offsetY = rand.nextInt(24) - 12;
      int offsetZ = rand.nextInt(60) - 30;
      long subSeed = rand.nextLong();

      int subCenterX = centerX + offsetX;
      int subCenterY = centerY + offsetY;
      int subCenterZ = centerZ + offsetZ;

      generateSmallIslet(chunk, chunkX, chunkZ, subSeed, subCenterX, subCenterY, subCenterZ);
    }
  }

  private void generateSmallIslet(ChunkAccess chunk, int chunkX, int chunkZ,
                                  long isletSeed, int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(isletSeed);
    int radiusX = 3 + rand.nextInt(7);
    int radiusZ = 3 + rand.nextInt(7);
    int heightUp = 2 + rand.nextInt(3);
    int heightDown = 3 + rand.nextInt(8);
    IslandNoise noise = createIslandNoise(isletSeed);

    int minBuild = chunk.getMinBuildHeight();
    int maxBuild = chunk.getMaxBuildHeight() - 1;

    int localMinX = Math.max(0, centerX - radiusX - 2 - chunkX * 16);
    int localMaxX = Math.min(15, centerX + radiusX + 2 - chunkX * 16);
    int localMinZ = Math.max(0, centerZ - radiusZ - 2 - chunkZ * 16);
    int localMaxZ = Math.min(15, centerZ + radiusZ + 2 - chunkZ * 16);

    if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
      return;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();

    for (int x = localMinX; x <= localMaxX; x++) {
      int worldX = chunkX * 16 + x;
      double xDist = (worldX - centerX) / (double) radiusX;

      for (int z = localMinZ; z <= localMaxZ; z++) {
        int worldZ = chunkZ * 16 + z;
        double zDist = (worldZ - centerZ) / (double) radiusZ;

        // Noise warp even on small islets
        double warpX = noise.shape.sample2D(worldX * 0.03, worldZ * 0.025, 2.0, 0.5) * 0.3;
        double warpZ = noise.shape.sample2D(worldX * 0.025 + 500, worldZ * 0.03 + 500, 2.0, 0.5) * 0.3;
        double warpedXDist = xDist + warpX;
        double warpedZDist = zDist + warpZ;
        double horizDist = warpedXDist * warpedXDist + warpedZDist * warpedZDist;
        if (horizDist >= 1.0) continue;

        double falloff = 1.0 - horizDist;
        double surfaceNoise = noise.surface.sample2D(worldX * 0.1, worldZ * 0.1, 2.0, 0.5) * 1.5;
        int localHeightUp = (int) (heightUp * falloff + surfaceNoise);
        int localHeightDown = (int) (heightDown * Math.pow(falloff, 1.5));

        int yMin = Math.max(minBuild, centerY - localHeightDown);
        int yMax = Math.min(maxBuild, centerY + Math.max(0, localHeightUp));

        for (int y = yMin; y <= yMax; y++) {
          boolean inIsland;
          if (y >= centerY) {
            double yDistUp = (localHeightUp > 0) ? (y - centerY) / (double) localHeightUp : 1.0;
            inIsland = horizDist + yDistUp * yDistUp < 1.0;
          } else {
            double yDistDown = (localHeightDown > 0) ? (centerY - y) / (double) localHeightDown : 1.0;
            double bottomScale = 1.0 - yDistDown * 0.8;
            inIsland = horizDist < bottomScale * bottomScale;
          }

          if (inIsland) {
            pos.set(x, y, z);
            if (!chunk.getBlockState(pos).isAir()) continue;

            BlockState block = pickClassicBlock(y, centerY, yMax, yMin, worldX, worldZ, noise, isletSeed);
            chunk.setBlockState(pos, block, false);
            if (y == yMax && y >= centerY) {
              decorateSurface(chunk, pos, above, worldX, worldZ, noise, radiusX, radiusZ);
            }
          }
        }
      }
    }
  }

  // ========== SHARDS STYLE ==========

  private void generateShards(ChunkAccess chunk, int chunkX, int chunkZ,
                              long islandSeed, int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(islandSeed);
    int shardCount = 3 + rand.nextInt(4);

    for (int i = 0; i < shardCount; i++) {
      int offsetX = rand.nextInt(50) - 25;
      int offsetZ = rand.nextInt(50) - 25;
      int offsetY = rand.nextInt(40) - 20;
      long shardSeed = rand.nextLong();

      generateShard(chunk, chunkX, chunkZ, shardSeed,
          centerX + offsetX, centerY + offsetY, centerZ + offsetZ);
    }

    // Debris fragments
    int debris = 6 + rand.nextInt(6);
    for (int i = 0; i < debris; i++) {
      int dx = rand.nextInt(60) - 30;
      int dz = rand.nextInt(60) - 30;
      int dy = rand.nextInt(30) - 15;
      long debrisSeed = rand.nextLong();
      generateFragment(chunk, chunkX, chunkZ, debrisSeed, centerX + dx, centerY + dy, centerZ + dz);
    }
  }

  private void generateShard(ChunkAccess chunk, int chunkX, int chunkZ, long shardSeed,
                             int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(shardSeed);
    int baseRadius = 2 + rand.nextInt(4);
    int height = 25 + rand.nextInt(55);
    IslandNoise noise = createIslandNoise(shardSeed);

    int minBuild = chunk.getMinBuildHeight();
    int maxBuild = chunk.getMaxBuildHeight() - 1;

    int maxRadius = baseRadius + 4;
    int localMinX = Math.max(0, centerX - maxRadius - chunkX * 16);
    int localMaxX = Math.min(15, centerX + maxRadius - chunkX * 16);
    int localMinZ = Math.max(0, centerZ - maxRadius - chunkZ * 16);
    int localMaxZ = Math.min(15, centerZ + maxRadius - chunkZ * 16);

    if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
      return;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
    int yBottom = Math.max(minBuild, centerY - height / 3);
    int yTop = Math.min(maxBuild, centerY + height * 2 / 3);

    for (int x = localMinX; x <= localMaxX; x++) {
      int worldX = chunkX * 16 + x;
      for (int z = localMinZ; z <= localMaxZ; z++) {
        int worldZ = chunkZ * 16 + z;

        double dx = worldX - centerX;
        double dz = worldZ - centerZ;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        for (int y = yBottom; y <= yTop; y++) {
          double t = (y - yBottom) / (double) (yTop - yBottom);
          double jag = noise.shape.sample3D(worldX * 0.1, y * 0.08, worldZ * 0.1, 2.0, 0.5) * 2.0;
          double radiusAtY = baseRadius * (1.0 - t) + jag + 0.8;
          if (horizDist <= radiusAtY) {
            pos.set(x, y, z);
            if (!chunk.getBlockState(pos).isAir()) continue;
            BlockState block = pickShardBlock(noise, worldX, y, worldZ);
            chunk.setBlockState(pos, block, false);
            if (y == yBottom) {
              maybeDecorateUnderside(chunk, pos, below, worldX, worldZ, noise);
            }
          }
        }
      }
    }
  }

  private void generateFragment(ChunkAccess chunk, int chunkX, int chunkZ, long debrisSeed,
                                int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(debrisSeed);
    int radiusX = 2 + rand.nextInt(4);
    int radiusZ = 2 + rand.nextInt(4);
    int height = 1 + rand.nextInt(3);
    IslandNoise noise = createIslandNoise(debrisSeed);

    int minBuild = chunk.getMinBuildHeight();
    int maxBuild = chunk.getMaxBuildHeight() - 1;

    int localMinX = Math.max(0, centerX - radiusX - chunkX * 16);
    int localMaxX = Math.min(15, centerX + radiusX - chunkX * 16);
    int localMinZ = Math.max(0, centerZ - radiusZ - chunkZ * 16);
    int localMaxZ = Math.min(15, centerZ + radiusZ - chunkZ * 16);

    if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
      return;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    for (int x = localMinX; x <= localMaxX; x++) {
      int worldX = chunkX * 16 + x;
      double xDist = (worldX - centerX) / (double) radiusX;
      for (int z = localMinZ; z <= localMaxZ; z++) {
        int worldZ = chunkZ * 16 + z;
        double zDist = (worldZ - centerZ) / (double) radiusZ;
        double horizDist = xDist * xDist + zDist * zDist;
        if (horizDist >= 1.0) continue;
        int yMin = Math.max(minBuild, centerY - height);
        int yMax = Math.min(maxBuild, centerY + height);
        for (int y = yMin; y <= yMax; y++) {
          pos.set(x, y, z);
          if (!chunk.getBlockState(pos).isAir()) continue;
          chunk.setBlockState(pos, pickShardBlock(noise, worldX, y, worldZ), false);
        }
      }
    }
  }

  // ========== RUINS STYLE ==========

  private void generateRuins(ChunkAccess chunk, int chunkX, int chunkZ,
                             long islandSeed, int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(islandSeed);
    int radiusX = 20 + rand.nextInt(30);
    int radiusZ = 20 + rand.nextInt(30);
    int thickness = 3 + rand.nextInt(4);
    IslandNoise noise = createIslandNoise(islandSeed);

    int minBuild = chunk.getMinBuildHeight();
    int maxBuild = chunk.getMaxBuildHeight() - 1;

    int localMinX = Math.max(0, centerX - radiusX - chunkX * 16);
    int localMaxX = Math.min(15, centerX + radiusX - chunkX * 16);
    int localMinZ = Math.max(0, centerZ - radiusZ - chunkZ * 16);
    int localMaxZ = Math.min(15, centerZ + radiusZ - chunkZ * 16);

    if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
      return;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();

    for (int x = localMinX; x <= localMaxX; x++) {
      int worldX = chunkX * 16 + x;
      double xDist = (worldX - centerX) / (double) radiusX;

      for (int z = localMinZ; z <= localMaxZ; z++) {
        int worldZ = chunkZ * 16 + z;
        double zDist = (worldZ - centerZ) / (double) radiusZ;

        double horizDist = xDist * xDist + zDist * zDist;
        if (horizDist >= 1.0) continue;

        double crackNoise = noise.carve.sample2D(worldX * 0.06, worldZ * 0.06, 2.0, 0.5);
        if (crackNoise > 0.3) continue;

        double surfH = noise.surface.sample2D(worldX * 0.08, worldZ * 0.08, 2.0, 0.5) * 2.0;
        int topY = Math.min(maxBuild, centerY + (int) surfH);
        double thickNoise = noise.detail.sample2D(worldX * 0.06, worldZ * 0.06, 2.0, 0.5) * 2.0;
        int bottomY = Math.max(minBuild, topY - thickness - (int) Math.abs(thickNoise));

        for (int y = bottomY; y <= topY; y++) {
          pos.set(x, y, z);
          if (!chunk.getBlockState(pos).isAir()) continue;

          BlockState block = pickRuinBlock(y, topY, noise, worldX, worldZ);
          chunk.setBlockState(pos, block, false);
          if (y == topY) {
            decorateSurface(chunk, pos, above, worldX, worldZ, noise, radiusX, radiusZ);
          }
          if (y == bottomY) {
            maybeDecorateUnderside(chunk, pos, below, worldX, worldZ, noise);
          }
        }
      }
    }
  }

  // ========== SKYLANDS STYLE ==========

  private void generateSkylands(ChunkAccess chunk, int chunkX, int chunkZ,
                                long islandSeed, int centerX, int centerY, int centerZ) {
    RandomSource rand = RandomSource.create(islandSeed);
    int radiusX = 24 + rand.nextInt(40);
    int radiusZ = 24 + rand.nextInt(40);
    int heightUp = 6 + rand.nextInt(12);
    int heightDown = 14 + rand.nextInt(22);
    IslandNoise noise = createIslandNoise(islandSeed);

    int minBuild = chunk.getMinBuildHeight();
    int maxBuild = chunk.getMaxBuildHeight() - 1;

    int localMinX = Math.max(0, centerX - radiusX - 2 - chunkX * 16);
    int localMaxX = Math.min(15, centerX + radiusX + 2 - chunkX * 16);
    int localMinZ = Math.max(0, centerZ - radiusZ - 2 - chunkZ * 16);
    int localMaxZ = Math.min(15, centerZ + radiusZ + 2 - chunkZ * 16);

    if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
      return;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
    boolean isLarge = Math.min(radiusX, radiusZ) >= 18;

    for (int x = localMinX; x <= localMaxX; x++) {
      int worldX = chunkX * 16 + x;
      double xDist = (worldX - centerX) / (double) radiusX;

      for (int z = localMinZ; z <= localMaxZ; z++) {
        int worldZ = chunkZ * 16 + z;
        double zDist = (worldZ - centerZ) / (double) radiusZ;

        // Noise-warped edge
        double warpX = noise.shape.sample2D(worldX * 0.015, worldZ * 0.012, 2.0, 0.5) * 0.4;
        double warpZ = noise.shape.sample2D(worldX * 0.012 + 700, worldZ * 0.015 + 700, 2.0, 0.5) * 0.4;
        double warpedXDist = xDist + warpX;
        double warpedZDist = zDist + warpZ;
        double horizDist = warpedXDist * warpedXDist + warpedZDist * warpedZDist;
        if (horizDist >= 1.0) continue;

        double falloff = 1.0 - horizDist;

        double surfaceNoise = noise.surface.sample2D(worldX * 0.04, worldZ * 0.04, 2.0, 0.5) * 5.0
            + noise.surface.sample2D(worldX * 0.12 + 100, worldZ * 0.12 + 100, 2.0, 0.5) * 2.0;
        int localHeightUp = (int) (heightUp * falloff + surfaceNoise);
        int localHeightDown = (int) (heightDown * Math.pow(falloff, 1.5));

        // Root tendrils
        double tendrilNoise = Math.abs(noise.carve.sample3D(
            worldX * 0.07, (centerY - localHeightDown) * 0.1, worldZ * 0.07, 2.0, 0.5));
        if (tendrilNoise > 0.55 && horizDist < 0.6) {
          localHeightDown += (int) ((tendrilNoise - 0.55) * 50.0);
        }

        int yMin = Math.max(minBuild, centerY - localHeightDown);
        int yMax = Math.min(maxBuild, centerY + Math.max(0, localHeightUp));

        for (int y = yMin; y <= yMax; y++) {
          // Void cuts through the body
          double carve = noise.carve.sample3D(worldX * 0.06, y * 0.08, worldZ * 0.06, 2.0, 0.5);
          if (carve > 0.45 && y < centerY + 2) continue;

          boolean inIsland;
          if (y >= centerY) {
            double yDistUp = (localHeightUp > 0) ? (y - centerY) / (double) localHeightUp : 1.0;
            inIsland = horizDist + yDistUp * yDistUp < 1.0;
          } else {
            double yDistDown = (localHeightDown > 0) ? (centerY - y) / (double) localHeightDown : 1.0;
            double bottomScale = 1.0 - yDistDown * 0.75;
            inIsland = horizDist < bottomScale * bottomScale;
          }

          if (inIsland) {
            // Internal cave carving for large skylands
            if (isLarge && y < centerY - 2 && y > centerY - localHeightDown + 4) {
              double cave = noise.carve.sample3D(worldX * 0.09, y * 0.11, worldZ * 0.09, 2.0, 0.5);
              if (cave > 0.32) continue;
            }

            pos.set(x, y, z);
            if (!chunk.getBlockState(pos).isAir()) continue;

            BlockState block = pickClassicBlock(y, centerY, yMax, yMin, worldX, worldZ, noise, islandSeed);
            chunk.setBlockState(pos, block, false);
            if (y == yMax && y >= centerY) {
              decorateSurface(chunk, pos, above, worldX, worldZ, noise, radiusX, radiusZ);
            }
            if (y == yMin && y < centerY - 3) {
              maybeGlowUnderside(chunk, pos, above, worldX, worldZ, noise);
            }
          }
        }
      }
    }

    // Trees and ponds for skylands
    if (isLarge) {
      generatePond(chunk, chunkX, chunkZ, noise, centerX, centerY + heightUp - 1, centerZ, Math.min(radiusX, radiusZ));
      generateTrees(chunk, chunkX, chunkZ, noise, islandSeed, centerX, centerY, centerZ, radiusX, radiusZ, heightUp);
    }
  }

  // ========== BLOCK SELECTION ==========

  private BlockState pickClassicBlock(int y, int centerY, int topY, int bottomY,
                                      int worldX, int worldZ, IslandNoise noise, long islandSeed) {
    int depthFromTop = topY - y;

    if (depthFromTop == 0) {
      return pickSurfaceBlock(worldX, worldZ, noise);
    }
    if (depthFromTop <= 3) {
      return Blocks.DIRT.defaultBlockState();
    }
    // Geological strata: stone -> then deepslate deeper down
    if (y < centerY - 10) {
      double deepslateNoise = noise.detail.sample2D(worldX * 0.05, worldZ * 0.05, 2.0, 0.5);
      if (deepslateNoise > -0.2) {
        return Blocks.DEEPSLATE.defaultBlockState();
      }
    }
    // Ore pockets in interior
    if (depthFromTop > 4 && y < topY - 3) {
      double oreNoise = noise.detail.sample3D(worldX * 0.15, y * 0.15, worldZ * 0.15, 2.0, 0.5);
      if (oreNoise > 0.7) {
        return Blocks.COAL_ORE.defaultBlockState();
      }
      if (oreNoise > 0.62) {
        return Blocks.IRON_ORE.defaultBlockState();
      }
      if (oreNoise < -0.75) {
        return Blocks.GOLD_ORE.defaultBlockState();
      }
    }
    return structureBlock;
  }

  private BlockState pickSpireBlock(double t, IslandNoise noise, int worldX, int y, int worldZ) {
    double blockNoise = noise.detail.sample3D(worldX * 0.12, y * 0.12, worldZ * 0.12, 2.0, 0.5);
    if (t < 0.4) {
      return (blockNoise > 0.3) ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }
    if (t < 0.7) {
      return (blockNoise > 0.4) ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }
    return (blockNoise > 0.2) ? Blocks.CALCITE.defaultBlockState() : Blocks.DIORITE.defaultBlockState();
  }

  private BlockState pickErodedBlock(int y, int centerY, int topY, IslandNoise noise, int worldX, int worldZ) {
    int depthFromTop = topY - y;
    if (depthFromTop == 0) {
      double mossChance = noise.detail.sample2D(worldX * 0.1, worldZ * 0.1, 2.0, 0.5);
      if (mossChance > 0.3) {
        return Blocks.MOSS_BLOCK.defaultBlockState();
      }
      return pickSurfaceBlock(worldX, worldZ, noise);
    }
    if (depthFromTop <= 2) {
      return Blocks.DIRT.defaultBlockState();
    }
    double blockNoise = noise.detail.sample3D(worldX * 0.1, y * 0.1, worldZ * 0.1, 2.0, 0.5);
    if (blockNoise > 0.4) {
      return Blocks.COBBLESTONE.defaultBlockState();
    }
    if (blockNoise < -0.3) {
      return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    }
    return structureBlock;
  }

  private BlockState pickShardBlock(IslandNoise noise, int worldX, int y, int worldZ) {
    double rough = noise.detail.sample3D(worldX * 0.12, y * 0.12, worldZ * 0.12, 2.0, 0.5);
    if (rough > 0.4) {
      return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    }
    if (rough > 0.15) {
      return Blocks.BASALT.defaultBlockState();
    }
    if (rough < -0.35) {
      return Blocks.DEEPSLATE.defaultBlockState();
    }
    return structureBlock;
  }

  private BlockState pickRuinBlock(int y, int topY, IslandNoise noise, int worldX, int worldZ) {
    int depthFromTop = topY - y;
    if (depthFromTop == 0) {
      double mossChance = noise.detail.sample2D(worldX * 0.08, worldZ * 0.08, 2.0, 0.5);
      if (mossChance > 0.35) {
        return Blocks.MOSS_BLOCK.defaultBlockState();
      }
      return Blocks.STONE_BRICKS.defaultBlockState();
    }
    double crack = noise.detail.sample3D(worldX * 0.1, y * 0.1, worldZ * 0.1, 2.0, 0.5);
    if (crack > 0.35) {
      return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    }
    if (crack < -0.35) {
      return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    }
    return Blocks.STONE_BRICKS.defaultBlockState();
  }

  private BlockState pickMonolithBlock(double t, IslandNoise noise, int worldX, int y, int worldZ) {
    double blockNoise = noise.detail.sample3D(worldX * 0.08, y * 0.08, worldZ * 0.08, 2.0, 0.5);
    if (t > 0.85) {
      // Top cap: polished deepslate
      return (blockNoise > 0.3) ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
          : Blocks.DEEPSLATE_TILES.defaultBlockState();
    }
    if (t < 0.15) {
      // Bottom: rough deepslate
      return (blockNoise > 0.2) ? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
          : Blocks.DEEPSLATE.defaultBlockState();
    }
    // Main body: smooth alternation
    if (blockNoise > 0.4) {
      return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    }
    if (blockNoise < -0.3) {
      return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }
    return Blocks.DEEPSLATE.defaultBlockState();
  }

  private BlockState pickMesaBlock(int y, int centerY, long islandSeed, IslandNoise noise, int worldX, int worldZ) {
    if (y == centerY) {
      return Blocks.RED_SAND.defaultBlockState();
    }
    // Band pattern with noise offset for variation
    double bandWarp = noise.detail.sample2D(worldX * 0.03, worldZ * 0.03, 2.0, 0.5) * 1.5;
    int band = Math.abs((int) (y - centerY + bandWarp)) % 7;
    int offset = (int) ((islandSeed & 0x7L) % 7);
    band = (band + offset) % 7;
    return switch (band) {
      case 0 -> Blocks.TERRACOTTA.defaultBlockState();
      case 1 -> Blocks.ORANGE_TERRACOTTA.defaultBlockState();
      case 2 -> Blocks.WHITE_TERRACOTTA.defaultBlockState();
      case 3 -> Blocks.YELLOW_TERRACOTTA.defaultBlockState();
      case 4 -> Blocks.RED_TERRACOTTA.defaultBlockState();
      case 5 -> Blocks.BROWN_TERRACOTTA.defaultBlockState();
      default -> Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
    };
  }

  private BlockState pickSurfaceBlock(int worldX, int worldZ, IslandNoise noise) {
    if (!surfaceBlock.is(Blocks.GRASS_BLOCK)) {
      return surfaceBlock;
    }
    double patchNoise = noise.surface.sample2D(worldX * 0.04, worldZ * 0.04, 2.0, 0.5);
    double detailNoise = noise.detail.sample2D(worldX * 0.1, worldZ * 0.1, 2.0, 0.5);
    if (patchNoise > 0.45) {
      return Blocks.MOSS_BLOCK.defaultBlockState();
    }
    if (patchNoise < -0.45) {
      return Blocks.COARSE_DIRT.defaultBlockState();
    }
    if (detailNoise > 0.4) {
      return Blocks.PODZOL.defaultBlockState();
    }
    if (detailNoise < -0.4) {
      return Blocks.ROOTED_DIRT.defaultBlockState();
    }
    return surfaceBlock;
  }

  // ========== SURFACE DECORATION ==========

  private void decorateSurface(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                               BlockPos.MutableBlockPos above,
                               int worldX, int worldZ, IslandNoise noise,
                               int radiusX, int radiusZ) {
    if (pos.getY() + 1 >= chunk.getMaxBuildHeight()) {
      return;
    }
    above.set(pos.getX(), pos.getY() + 1, pos.getZ());
    if (!chunk.getBlockState(above).isAir()) {
      return;
    }

    double decorNoise = noise.detail.sample2D(worldX * 0.12, worldZ * 0.12, 2.0, 0.5);
    if (decorNoise < 0.15) {
      return;
    }

    BlockState belowBlock = chunk.getBlockState(pos);

    // Moss carpet on moss blocks
    if (belowBlock.is(Blocks.MOSS_BLOCK) && decorNoise > 0.4) {
      chunk.setBlockState(above, Blocks.MOSS_CARPET.defaultBlockState(), false);
      return;
    }

    // Coherent patches of vegetation
    double patchType = noise.surface.sample2D(worldX * 0.06 + 300, worldZ * 0.06 + 300, 2.0, 0.5);

    if (decorNoise > 0.6) {
      // Flower patches
      BlockState flower;
      if (patchType > 0.3) {
        flower = Blocks.DANDELION.defaultBlockState();
      } else if (patchType > 0.0) {
        flower = Blocks.POPPY.defaultBlockState();
      } else if (patchType > -0.3) {
        flower = Blocks.AZURE_BLUET.defaultBlockState();
      } else {
        flower = Blocks.CORNFLOWER.defaultBlockState();
      }
      chunk.setBlockState(above, flower, false);
    } else if (decorNoise > 0.35) {
      // Grass/fern patches
      if (patchType > 0.0) {
        chunk.setBlockState(above, Services.PLATFORM.getShortGrassBlock().defaultBlockState(), false);
      } else {
        chunk.setBlockState(above, Blocks.FERN.defaultBlockState(), false);
      }
    }
  }

  private void decorateMushroomSurface(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                       BlockPos.MutableBlockPos above,
                                       int worldX, int worldZ, IslandNoise noise) {
    if (pos.getY() + 1 >= chunk.getMaxBuildHeight()) {
      return;
    }
    above.set(pos.getX(), pos.getY() + 1, pos.getZ());
    if (!chunk.getBlockState(above).isAir()) {
      return;
    }

    double decorNoise = noise.detail.sample2D(worldX * 0.15, worldZ * 0.15, 2.0, 0.5);
    if (decorNoise > 0.4) {
      chunk.setBlockState(above, Blocks.RED_MUSHROOM.defaultBlockState(), false);
    } else if (decorNoise > 0.2) {
      chunk.setBlockState(above, Blocks.BROWN_MUSHROOM.defaultBlockState(), false);
    }
  }

  // ========== UNDERSIDE DECORATION ==========

  private void maybeDecorateUnderside(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                      BlockPos.MutableBlockPos below,
                                      int worldX, int worldZ, IslandNoise noise) {
    if (pos.getY() - 1 <= chunk.getMinBuildHeight()) {
      return;
    }
    below.set(pos.getX(), pos.getY() - 1, pos.getZ());
    if (!chunk.getBlockState(below).isAir()) {
      return;
    }
    double decorNoise = noise.detail.sample2D(worldX * 0.15, worldZ * 0.15, 2.0, 0.5);
    if (decorNoise > 0.5) {
      chunk.setBlockState(below, Blocks.HANGING_ROOTS.defaultBlockState(), false);
    } else if (decorNoise < -0.45) {
      chunk.setBlockState(below, Blocks.POINTED_DRIPSTONE.defaultBlockState(), false);
    }
  }

  private void maybeGlowUnderside(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                  BlockPos.MutableBlockPos scratch,
                                  int worldX, int worldZ, IslandNoise noise) {
    double glowNoise = noise.detail.sample2D(worldX * 0.08 + 800, worldZ * 0.08 + 800, 2.0, 0.5);
    if (glowNoise > 0.6) {
      // Place glow lichen on the underside face (replace solid block at pos with shroomlight)
      chunk.setBlockState(pos, Blocks.SHROOMLIGHT.defaultBlockState(), false);
    } else if (glowNoise > 0.45) {
      chunk.setBlockState(pos, Blocks.GLOWSTONE.defaultBlockState(), false);
    }
  }

  // ========== TREES ==========

  private void generateTrees(ChunkAccess chunk, int chunkX, int chunkZ, IslandNoise noise,
                             long islandSeed, int centerX, int centerY, int centerZ,
                             int radiusX, int radiusZ, int heightUp) {
    RandomSource treeRand = RandomSource.create(islandSeed ^ 0xBEEF);
    int treeCount = (Math.min(radiusX, radiusZ) >= 20) ? 2 + treeRand.nextInt(3) : 1 + treeRand.nextInt(2);

    for (int t = 0; t < treeCount; t++) {
      int treeOffX = treeRand.nextInt(radiusX) - radiusX / 2;
      int treeOffZ = treeRand.nextInt(radiusZ) - radiusZ / 2;
      int treeWorldX = centerX + treeOffX;
      int treeWorldZ = centerZ + treeOffZ;
      int treeLocalX = treeWorldX - chunkX * 16;
      int treeLocalZ = treeWorldZ - chunkZ * 16;

      if (treeLocalX < 0 || treeLocalX > 15 || treeLocalZ < 0 || treeLocalZ > 15) {
        continue;
      }

      // Find the surface Y at this position
      int surfaceY = -1;
      for (int y = centerY + heightUp + 5; y >= centerY - 2; y--) {
        BlockPos testPos = new BlockPos(treeLocalX, y, treeLocalZ);
        BlockPos abovePos = new BlockPos(treeLocalX, y + 1, treeLocalZ);
        if (y + 1 < chunk.getMaxBuildHeight()
            && !chunk.getBlockState(testPos).isAir()
            && chunk.getBlockState(abovePos).isAir()) {
          surfaceY = y;
          break;
        }
      }
      if (surfaceY < 0) continue;

      // Pick tree type based on noise
      double treeType = noise.detail.sample2D(treeWorldX * 0.05, treeWorldZ * 0.05, 2.0, 0.5);
      BlockState logBlock;
      BlockState leafBlock;
      if (treeType > 0.2) {
        logBlock = Blocks.OAK_LOG.defaultBlockState();
        leafBlock = Blocks.OAK_LEAVES.defaultBlockState();
      } else if (treeType > -0.2) {
        logBlock = Blocks.BIRCH_LOG.defaultBlockState();
        leafBlock = Blocks.BIRCH_LEAVES.defaultBlockState();
      } else {
        logBlock = Blocks.SPRUCE_LOG.defaultBlockState();
        leafBlock = Blocks.SPRUCE_LEAVES.defaultBlockState();
      }

      int trunkHeight = 4 + treeRand.nextInt(4);
      BlockPos.MutableBlockPos treePos = new BlockPos.MutableBlockPos();

      // Trunk
      for (int ty = 1; ty <= trunkHeight; ty++) {
        int y = surfaceY + ty;
        if (y >= chunk.getMaxBuildHeight()) break;
        treePos.set(treeLocalX, y, treeLocalZ);
        if (chunk.getBlockState(treePos).isAir()) {
          chunk.setBlockState(treePos, logBlock, false);
        }
      }

      // Leaf sphere at top
      int leafCenterY = surfaceY + trunkHeight;
      int leafRadius = 2 + treeRand.nextInt(2);
      for (int lx = -leafRadius; lx <= leafRadius; lx++) {
        for (int lz = -leafRadius; lz <= leafRadius; lz++) {
          for (int ly = -1; ly <= leafRadius; ly++) {
            double dist = lx * lx + lz * lz + ly * ly;
            if (dist > (leafRadius + 0.5) * (leafRadius + 0.5)) continue;

            int leafLocalX = treeLocalX + lx;
            int leafLocalZ = treeLocalZ + lz;
            int leafY = leafCenterY + ly;
            if (leafLocalX < 0 || leafLocalX > 15 || leafLocalZ < 0 || leafLocalZ > 15) continue;
            if (leafY >= chunk.getMaxBuildHeight() || leafY <= chunk.getMinBuildHeight()) continue;

            treePos.set(leafLocalX, leafY, leafLocalZ);
            if (chunk.getBlockState(treePos).isAir()) {
              chunk.setBlockState(treePos, leafBlock, false);
            }
          }
        }
      }
    }
  }

  // ========== PONDS ==========

  private void generatePond(ChunkAccess chunk, int chunkX, int chunkZ, IslandNoise noise,
                            int centerX, int surfaceY, int centerZ, int islandRadius) {
    // Place pond offset from center
    double pondOffX = noise.surface.sample2D(centerX * 0.01, centerZ * 0.01, 2.0, 0.5) * islandRadius * 0.3;
    double pondOffZ = noise.surface.sample2D(centerX * 0.01 + 100, centerZ * 0.01 + 100, 2.0, 0.5) * islandRadius * 0.3;
    int pondCenterX = centerX + (int) pondOffX;
    int pondCenterZ = centerZ + (int) pondOffZ;
    int pondRadius = 3 + (int) (Math.abs(noise.detail.sample2D(centerX * 0.02, centerZ * 0.02, 2.0, 0.5)) * 3.0);

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    for (int px = -pondRadius; px <= pondRadius; px++) {
      for (int pz = -pondRadius; pz <= pondRadius; pz++) {
        double dist = px * px + pz * pz;
        if (dist > pondRadius * pondRadius) continue;

        int worldX = pondCenterX + px;
        int worldZ = pondCenterZ + pz;
        int localX = worldX - chunkX * 16;
        int localZ = worldZ - chunkZ * 16;

        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) continue;

        // Find surface at this XZ
        int foundSurface = -1;
        for (int y = surfaceY + 5; y >= surfaceY - 8; y--) {
          pos.set(localX, y, localZ);
          BlockPos.MutableBlockPos aboveCheck = new BlockPos.MutableBlockPos(localX, y + 1, localZ);
          if (y + 1 < chunk.getMaxBuildHeight()
              && !chunk.getBlockState(pos).isAir()
              && chunk.getBlockState(aboveCheck).isAir()) {
            foundSurface = y;
            break;
          }
        }
        if (foundSurface < 0) continue;

        // Dig 1-2 blocks down and fill with water; rim gets sand
        boolean isRim = dist > (pondRadius - 1.5) * (pondRadius - 1.5);
        if (isRim) {
          pos.set(localX, foundSurface, localZ);
          if (!chunk.getBlockState(pos).isAir()) {
            chunk.setBlockState(pos, Blocks.SAND.defaultBlockState(), false);
          }
        } else {
          // Dig and fill with water
          pos.set(localX, foundSurface, localZ);
          if (!chunk.getBlockState(pos).isAir()) {
            chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
          }
          // Clay bottom
          pos.set(localX, foundSurface - 1, localZ);
          if (foundSurface - 1 > chunk.getMinBuildHeight() && !chunk.getBlockState(pos).isAir()) {
            chunk.setBlockState(pos, Blocks.CLAY.defaultBlockState(), false);
          }
        }
      }
    }
  }

  // ========== UTILITIES ==========

  protected RandomSource getChunkRandom(int chunkX, int chunkZ) {
    long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed + 5000L;
    return RandomSource.create(chunkSeed);
  }

  @Override
  public String getType() {
    return "floating_islands";
  }

  @Override
  public int getPriority() {
    return 200;
  }

  // ========== NOISE HOLDER ==========

  /**
   * Pre-built set of coherent noise samplers for a single island.
   * Each island gets its own seeded noise to avoid cross-island pattern repetition.
   */
  private static class IslandNoise {
    final PerlinOctaves shape;   // Large-scale shape warping (4 octaves)
    final PerlinOctaves surface; // Medium-scale surface height (3 octaves)
    final PerlinOctaves carve;   // 3D carving / erosion (4 octaves)
    final PerlinOctaves detail;  // Small-scale material / decoration (2 octaves)

    IslandNoise(long seed) {
      RandomSource rand = RandomSource.create(seed);
      this.shape = new PerlinOctaves(rand, 4);
      this.surface = new PerlinOctaves(rand, 3);
      this.carve = new PerlinOctaves(rand, 4);
      this.detail = new PerlinOctaves(rand, 2);
    }
  }

  // ========== ENUMS ==========

  public enum IslandStyle {
    MIXED,
    SKYLANDS,
    ARCHIPELAGO,
    SHARDS,
    RUINS
  }

  private enum IslandType {
    CLASSIC,
    SPIRE,
    MUSHROOM,
    ERODED,
    MESA,
    MONOLITH
  }
}
