package art.arcane.mystcraft.world.gen.feature;

import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Procedural floating island generator that creates diverse suspended landmasses.
 * Supports multiple island morphologies: classic plateaus, rocky spires, mushroom caps,
 * eroded formations, layered mesas, and small archipelago clusters.
 */
public class MapGenFloatingIslands implements ITerrainAlteration {

    private final long seed;
    private final int density;
    private final BlockState structureBlock;
    private final BlockState surfaceBlock;

    private static final int RANGE = 5;

    // --- Island Type Enum ---

    private enum IslandType {
        CLASSIC,       // Broad plateau with noise-varied surface, tapered underside
        SPIRE,         // Tall narrow rocky peak
        MUSHROOM,      // Wide canopy on a narrow stem
        ERODED,        // Irregular shape with carved-out holes
        MESA,          // Flat-topped with layered sediment bands
        ARCHIPELAGO    // Cluster of small linked chunks
    }

    public MapGenFloatingIslands(long seed) {
        this(seed, 10, Blocks.STONE.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState());
    }

    public MapGenFloatingIslands(long seed, int density, BlockState structureBlock, BlockState surfaceBlock) {
        this.seed = seed;
        this.density = Math.max(1, density);
        this.structureBlock = structureBlock;
        this.surfaceBlock = surfaceBlock;
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
                    int centerY = 90 + chunkRand.nextInt(100);
                    int centerZ = originChunkZ * 16 + chunkRand.nextInt(16);

                    IslandType type = pickIslandType(chunkRand);
                    long islandSeed = chunkRand.nextLong();

                    generateIsland(chunk, chunkX, chunkZ, islandSeed, type,
                            centerX, centerY, centerZ);
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
        return IslandType.ARCHIPELAGO;
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
            case ARCHIPELAGO -> generateArchipelago(chunk, chunkX, chunkZ, islandSeed, centerX, centerY, centerZ);
        }
    }

    // --- Classic: Broad plateau with noise-varied surface and tapered underside ---

    private void generateClassic(ChunkAccess chunk, int chunkX, int chunkZ,
                                 long islandSeed, int centerX, int centerY, int centerZ) {
        RandomSource rand = RandomSource.create(islandSeed);
        int radiusX = 8 + rand.nextInt(20);
        int radiusZ = 8 + rand.nextInt(20);
        int heightUp = 4 + rand.nextInt(6);
        int heightDown = 8 + rand.nextInt(18);

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

                double falloff = 1.0 - horizDist;

                // Noise-based surface height variation
                double surfaceNoise = positionNoise(islandSeed, worldX, worldZ, 0.15) * 3.0
                        + positionNoise(islandSeed + 1, worldX, worldZ, 0.4) * 1.5;
                int localHeightUp = (int) (heightUp * falloff + surfaceNoise);
                int localHeightDown = (int) (heightDown * Math.sqrt(falloff));

                // Stalactite drip formations on the underside
                double stalactiteNoise = positionNoise(islandSeed + 2, worldX, worldZ, 0.3);
                if (stalactiteNoise > 0.4) {
                    localHeightDown += (int) ((stalactiteNoise - 0.4) * 12.0);
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
                        double bottomScale = 1.0 - yDistDown * 0.85;
                        inIsland = horizDist < bottomScale * bottomScale;
                    }

                    if (inIsland) {
                        pos.set(x, y, z);
                        if (!chunk.getBlockState(pos).isAir()) continue;

                        BlockState block = pickLayeredBlock(y, centerY, yMax, worldX, worldZ, islandSeed);
                        chunk.setBlockState(pos, block, false);
                    }
                }
            }
        }
    }

    // --- Spire: Tall narrow rocky peak ---

    private void generateSpire(ChunkAccess chunk, int chunkX, int chunkZ,
                               long islandSeed, int centerX, int centerY, int centerZ) {
        RandomSource rand = RandomSource.create(islandSeed);
        int baseRadius = 4 + rand.nextInt(8);
        int totalHeight = 15 + rand.nextInt(35);

        int minBuild = chunk.getMinBuildHeight();
        int maxBuild = chunk.getMaxBuildHeight() - 1;

        int maxRadius = baseRadius + 3;
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
                    // Radius narrows toward the top and tapers at the bottom
                    double t = (y - yBottom) / (double) (yTop - yBottom);
                    double radiusAtY;
                    if (t < 0.3) {
                        // Bottom taper
                        radiusAtY = baseRadius * (t / 0.3) * 0.7;
                    } else {
                        // Narrowing toward top with noise wobble
                        double narrowFactor = 1.0 - (t - 0.3) / 0.7;
                        double wobble = positionNoise(islandSeed, worldX, y, 0.2) * 1.5;
                        radiusAtY = baseRadius * narrowFactor * narrowFactor + wobble + 0.5;
                    }

                    if (horizDist <= radiusAtY) {
                        pos.set(x, y, z);
                        if (!chunk.getBlockState(pos).isAir()) continue;

                        BlockState block;
                        if (t > 0.85) {
                            // Peak: use structure block or snow
                            double snowChance = positionNoise(islandSeed + 5, worldX, worldZ, 0.5);
                            block = (snowChance > 0.3) ? Blocks.SNOW_BLOCK.defaultBlockState() : structureBlock;
                        } else {
                            block = pickSpireBlock(t, islandSeed, worldX, y, worldZ);
                        }
                        chunk.setBlockState(pos, block, false);
                    }
                }
            }
        }
    }

    // --- Mushroom: Wide canopy on a narrow stem ---

    private void generateMushroom(ChunkAccess chunk, int chunkX, int chunkZ,
                                  long islandSeed, int centerX, int centerY, int centerZ) {
        RandomSource rand = RandomSource.create(islandSeed);
        int stemRadius = 2 + rand.nextInt(3);
        int capRadius = 10 + rand.nextInt(16);
        int stemHeight = 10 + rand.nextInt(20);
        int capThickness = 4 + rand.nextInt(6);

        int minBuild = chunk.getMinBuildHeight();
        int maxBuild = chunk.getMaxBuildHeight() - 1;

        int maxR = capRadius + 2;
        int localMinX = Math.max(0, centerX - maxR - chunkX * 16);
        int localMaxX = Math.min(15, centerX + maxR - chunkX * 16);
        int localMinZ = Math.max(0, centerZ - maxR - chunkZ * 16);
        int localMaxZ = Math.min(15, centerZ + maxR - chunkZ * 16);

        if (localMaxX < 0 || localMinX > 15 || localMaxZ < 0 || localMinZ > 15) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
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

                // Stem
                double stemNoise = positionNoise(islandSeed + 3, worldX, worldZ, 0.4) * 1.5;
                double effectiveStemRadius = stemRadius + stemNoise;
                if (horizDist <= effectiveStemRadius) {
                    for (int y = Math.max(minBuild, stemBottom); y <= Math.min(maxBuild, stemTop); y++) {
                        pos.set(x, y, z);
                        if (chunk.getBlockState(pos).isAir()) {
                            chunk.setBlockState(pos, structureBlock, false);
                        }
                    }
                }

                // Cap (dome shape)
                double capNoise = positionNoise(islandSeed + 4, worldX, worldZ, 0.12) * 2.0;
                double normalizedHoriz = horizDist / (capRadius + capNoise);
                if (normalizedHoriz <= 1.0) {
                    double capFalloff = 1.0 - normalizedHoriz * normalizedHoriz;
                    int localCapHeight = (int) (capThickness * capFalloff);

                    for (int y = Math.max(minBuild, capBottom); y <= Math.min(maxBuild, capBottom + localCapHeight); y++) {
                        pos.set(x, y, z);
                        if (chunk.getBlockState(pos).isAir()) {
                            BlockState block = pickLayeredBlock(y, capBottom, capTop, worldX, worldZ, islandSeed);
                            chunk.setBlockState(pos, block, false);
                        }
                    }

                    // Drip roots hanging from cap underside
                    if (normalizedHoriz > 0.3 && normalizedHoriz < 0.8) {
                        double dripNoise = positionNoise(islandSeed + 7, worldX, worldZ, 0.6);
                        if (dripNoise > 0.5) {
                            int dripLength = (int) ((dripNoise - 0.5) * 10.0);
                            for (int dy = 1; dy <= dripLength; dy++) {
                                int y = capBottom - dy;
                                if (y < minBuild) break;
                                pos.set(x, y, z);
                                if (chunk.getBlockState(pos).isAir()) {
                                    chunk.setBlockState(pos, Blocks.HANGING_ROOTS.defaultBlockState(), false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Eroded: Irregular shape with carved-out holes ---

    private void generateEroded(ChunkAccess chunk, int chunkX, int chunkZ,
                                long islandSeed, int centerX, int centerY, int centerZ) {
        RandomSource rand = RandomSource.create(islandSeed);
        int radiusX = 10 + rand.nextInt(18);
        int radiusZ = 10 + rand.nextInt(18);
        int heightUp = 3 + rand.nextInt(5);
        int heightDown = 6 + rand.nextInt(14);

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

                double falloff = 1.0 - horizDist;

                // Noise-warped edge for irregular outline
                double edgeNoise = positionNoise(islandSeed + 10, worldX, worldZ, 0.08) * 0.4;
                if (horizDist + edgeNoise >= 1.0) continue;

                double surfaceNoise = positionNoise(islandSeed, worldX, worldZ, 0.2) * 2.5;
                int localHeightUp = (int) (heightUp * falloff + surfaceNoise);
                int localHeightDown = (int) (heightDown * Math.sqrt(falloff));

                int yMin = Math.max(minBuild, centerY - localHeightDown);
                int yMax = Math.min(maxBuild, centerY + Math.max(0, localHeightUp));

                for (int y = yMin; y <= yMax; y++) {
                    // Erosion holes: 3D noise carving
                    double erosion = positionNoise3D(islandSeed + 11, worldX, y, worldZ, 0.15);
                    if (erosion > 0.35) continue;

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

                        BlockState block = pickErodedBlock(y, centerY, yMax, islandSeed, worldX, worldZ);
                        chunk.setBlockState(pos, block, false);
                    }
                }
            }
        }
    }

    // --- Mesa: Flat-topped with layered sediment bands ---

    private void generateMesa(ChunkAccess chunk, int chunkX, int chunkZ,
                              long islandSeed, int centerX, int centerY, int centerZ) {
        RandomSource rand = RandomSource.create(islandSeed);
        int radiusX = 12 + rand.nextInt(16);
        int radiusZ = 12 + rand.nextInt(16);
        int plateauHeight = 2 + rand.nextInt(3);
        int totalDown = 12 + rand.nextInt(20);

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

                double falloff = 1.0 - horizDist;

                // Mesa has stepped edges via quantized falloff
                double steppedFalloff = Math.floor(falloff * 4.0) / 4.0;
                int localDown = (int) (totalDown * steppedFalloff);
                int localUp = (int) (plateauHeight * (falloff > 0.5 ? 1.0 : falloff * 2.0));

                int yMin = Math.max(minBuild, centerY - localDown);
                int yMax = Math.min(maxBuild, centerY + localUp);

                for (int y = yMin; y <= yMax; y++) {
                    pos.set(x, y, z);
                    if (!chunk.getBlockState(pos).isAir()) continue;

                    BlockState block = pickMesaBlock(y, centerY, islandSeed);
                    chunk.setBlockState(pos, block, false);
                }
            }
        }
    }

    // --- Archipelago: Cluster of small linked chunks ---

    private void generateArchipelago(ChunkAccess chunk, int chunkX, int chunkZ,
                                     long islandSeed, int centerX, int centerY, int centerZ) {
        RandomSource rand = RandomSource.create(islandSeed);
        int count = 3 + rand.nextInt(5);

        for (int i = 0; i < count; i++) {
            int offsetX = rand.nextInt(40) - 20;
            int offsetY = rand.nextInt(20) - 10;
            int offsetZ = rand.nextInt(40) - 20;
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

                double falloff = 1.0 - horizDist;
                double surfaceNoise = positionNoise(isletSeed, worldX, worldZ, 0.25) * 1.5;
                int localHeightUp = (int) (heightUp * falloff + surfaceNoise);
                int localHeightDown = (int) (heightDown * Math.sqrt(falloff));

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

                        BlockState block = pickLayeredBlock(y, centerY, yMax, worldX, worldZ, isletSeed);
                        chunk.setBlockState(pos, block, false);
                    }
                }
            }
        }
    }

    // --- Block Selection ---

    /** Layered block composition: surface -> dirt -> stone -> deepslate. */
    private BlockState pickLayeredBlock(int y, int centerY, int topY, int worldX, int worldZ, long noiseSeed) {
        int depthFromTop = topY - y;

        // Check if this is a surface position (has air above)
        if (depthFromTop == 0) {
            return surfaceBlock;
        }
        if (depthFromTop <= 3) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (y < centerY - 10) {
            double deepslateNoise = positionNoise(noiseSeed + 20, worldX, worldZ, 0.1);
            if (deepslateNoise > 0.0) {
                return Blocks.DEEPSLATE.defaultBlockState();
            }
        }
        return structureBlock;
    }

    private BlockState pickSpireBlock(double t, long noiseSeed, int worldX, int y, int worldZ) {
        double blockNoise = positionNoise3D(noiseSeed + 30, worldX, y, worldZ, 0.2);
        if (t < 0.4) {
            return (blockNoise > 0.3) ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        if (t < 0.7) {
            return (blockNoise > 0.4) ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        return (blockNoise > 0.2) ? Blocks.CALCITE.defaultBlockState() : Blocks.DIORITE.defaultBlockState();
    }

    private BlockState pickErodedBlock(int y, int centerY, int topY, long noiseSeed, int worldX, int worldZ) {
        int depthFromTop = topY - y;
        if (depthFromTop == 0) {
            double mossChance = positionNoise(noiseSeed + 40, worldX, worldZ, 0.3);
            if (mossChance > 0.4) {
                return Blocks.MOSS_BLOCK.defaultBlockState();
            }
            return surfaceBlock;
        }
        if (depthFromTop <= 2) {
            return Blocks.DIRT.defaultBlockState();
        }
        double blockNoise = positionNoise3D(noiseSeed + 41, worldX, y, worldZ, 0.15);
        if (blockNoise > 0.4) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }
        if (blockNoise < -0.3) {
            return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        }
        return structureBlock;
    }

    /** Mesa uses colored terracotta bands at different Y levels. */
    private BlockState pickMesaBlock(int y, int centerY, long noiseSeed) {
        if (y == centerY) {
            return Blocks.RED_SAND.defaultBlockState();
        }
        // Band pattern based on Y position
        int band = Math.abs(y - centerY) % 7;
        // Offset band pattern per-island so not all mesas look identical
        int offset = (int) ((noiseSeed & 0x7L) % 7);
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

    // --- Noise Utilities ---

    /** 2D position-deterministic noise in range [-1, 1]. */
    private static double positionNoise(long seed, int x, int z, double frequency) {
        double fx = x * frequency;
        double fz = z * frequency;
        long h = positionHash2D(seed, doubleToLongBits(fx), doubleToLongBits(fz));
        return (h & 0xFFFFL) / (double) 0x7FFF - 1.0;
    }

    /** 3D position-deterministic noise in range [-1, 1]. */
    private static double positionNoise3D(long seed, int x, int y, int z, double frequency) {
        double fx = x * frequency;
        double fy = y * frequency;
        double fz = z * frequency;
        long h = positionHash3D(seed, doubleToLongBits(fx), doubleToLongBits(fy), doubleToLongBits(fz));
        return (h & 0xFFFFL) / (double) 0x7FFF - 1.0;
    }

    private static long positionHash2D(long seed, long x, long z) {
        long h = seed;
        h ^= x * 73856093L;
        h ^= z * 83492791L;
        h = h * 6364136223846793005L + 1442695040888963407L;
        h ^= h >>> 16;
        return h;
    }

    private static long positionHash3D(long seed, long x, long y, long z) {
        long h = seed;
        h ^= x * 73856093L;
        h ^= y * 19349663L;
        h ^= z * 83492791L;
        h = h * 6364136223846793005L + 1442695040888963407L;
        h ^= h >>> 16;
        return h;
    }

    private static long doubleToLongBits(double value) {
        return Double.doubleToRawLongBits(value);
    }

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
}
