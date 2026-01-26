package art.arcane.mystcraft.world.gen.feature;

import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Floating island generator that creates suspended landmasses in the sky.
 * Unlike caves, this ADDS terrain rather than carving it.
 */
public class MapGenFloatingIslands implements ITerrainAlteration {

    private final long seed;
    private final int density;      // 1 in density chance per chunk
    private final BlockState structureBlock;
    private final BlockState surfaceBlock;

    // Range in chunks to check for island origins
    private static final int RANGE = 4;

    /**
     * Creates a floating island generator with default settings.
     */
    public MapGenFloatingIslands(long seed) {
        this(seed, 10, Blocks.STONE.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState());
    }

    /**
     * Creates a floating island generator with custom settings.
     */
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
                    // Generate floating island
                    int centerX = originChunkX * 16 + chunkRand.nextInt(16);
                    int centerY = 100 + chunkRand.nextInt(80); // Between y=100 and y=180
                    int centerZ = originChunkZ * 16 + chunkRand.nextInt(16);

                    // Island dimensions
                    int radiusX = 5 + chunkRand.nextInt(15);
                    int radiusZ = 5 + chunkRand.nextInt(15);
                    int heightUp = 3 + chunkRand.nextInt(8);
                    int heightDown = 5 + chunkRand.nextInt(15);

                    generateIsland(chunk, chunkX, chunkZ, chunkRand,
                            centerX, centerY, centerZ, radiusX, radiusZ, heightUp, heightDown);
                }
            }
        }
    }

    protected RandomSource getChunkRandom(int chunkX, int chunkZ) {
        long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed + 5000L;
        return RandomSource.create(chunkSeed);
    }

    protected void generateIsland(ChunkAccess chunk, int chunkX, int chunkZ,
                                  RandomSource rand, int centerX, int centerY, int centerZ,
                                  int radiusX, int radiusZ, int heightUp, int heightDown) {
        // Calculate bounds in chunk-local coordinates
        int minX = centerX - radiusX - chunkX * 16;
        int maxX = centerX + radiusX - chunkX * 16;
        int minZ = centerZ - radiusZ - chunkZ * 16;
        int maxZ = centerZ + radiusZ - chunkZ * 16;

        // Check if any part of the island is in this chunk
        if (maxX < 0 || minX >= 16 || maxZ < 0 || minZ >= 16) {
            return;
        }

        // Clamp to chunk bounds
        minX = Math.max(0, minX);
        maxX = Math.min(15, maxX);
        minZ = Math.max(0, minZ);
        maxZ = Math.min(15, maxZ);

        int minY = Math.max(chunk.getMinBuildHeight(), centerY - heightDown);
        int maxY = Math.min(chunk.getMaxBuildHeight() - 1, centerY + heightUp);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            int worldX = chunkX * 16 + x;
            double xDist = (worldX - centerX) / (double) radiusX;

            for (int z = minZ; z <= maxZ; z++) {
                int worldZ = chunkZ * 16 + z;
                double zDist = (worldZ - centerZ) / (double) radiusZ;

                // Check if within horizontal ellipse
                double horizDist = xDist * xDist + zDist * zDist;
                if (horizDist >= 1.0D) {
                    continue;
                }

                // Calculate the height of the island at this x,z based on distance from center
                double falloff = 1.0D - horizDist;
                int localHeightUp = (int) (heightUp * falloff);
                int localHeightDown = (int) (heightDown * Math.sqrt(falloff));

                for (int y = centerY - localHeightDown; y <= centerY + localHeightUp; y++) {
                    if (y < minY || y > maxY) continue;

                    double yDistUp = (y > centerY) ? (y - centerY) / (double) localHeightUp : 0;
                    double yDistDown = (y < centerY) ? (centerY - y) / (double) localHeightDown : 0;

                    // Use different shapes for top and bottom
                    boolean inIsland;
                    if (y >= centerY) {
                        // Top is more rounded
                        inIsland = horizDist + yDistUp * yDistUp < 1.0D;
                    } else {
                        // Bottom tapers to a point
                        double bottomScale = 1.0D - yDistDown * 0.8D;
                        inIsland = horizDist < bottomScale * bottomScale;
                    }

                    if (inIsland) {
                        pos.set(x, y, z);

                        // Don't overwrite existing solid blocks
                        BlockState existing = chunk.getBlockState(pos);
                        if (existing.isAir()) {
                            // Surface layer on top
                            pos.set(x, y + 1, z);
                            BlockState above = (y + 1 <= maxY) ? chunk.getBlockState(pos) : Blocks.AIR.defaultBlockState();
                            pos.set(x, y, z);

                            if (y >= centerY && above.isAir()) {
                                chunk.setBlockState(pos, surfaceBlock, false);
                            } else {
                                chunk.setBlockState(pos, structureBlock, false);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getType() {
        return "floating_islands";
    }

    @Override
    public int getPriority() {
        return 200; // Floating islands run late (after caves/ravines)
    }
}
