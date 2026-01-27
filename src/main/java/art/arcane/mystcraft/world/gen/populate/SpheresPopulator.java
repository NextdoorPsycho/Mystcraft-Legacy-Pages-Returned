package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

/**
 * Spheres populator that generates floating or embedded spherical formations.
 * Uses the neighbor-seed pattern for multi-chunk structures: each chunk
 * deterministically checks nearby chunk seeds for sphere origins and only
 * places the portion that falls within its own 16x16 boundary.
 * This avoids cross-chunk writes and prevents deadlocks during worldgen.
 */
public class SpheresPopulator implements IPopulate {

    private final long seed;

    private static final int SPHERES_PER_CHUNK = 2;
    private static final int MIN_RADIUS = 5;
    private static final int MAX_RADIUS = 15;
    private static final float FLOATING_CHANCE = 0.4f;

    // How many neighbor chunks to scan in each direction.
    // Must cover MAX_RADIUS / 16, rounded up = 1 chunk.
    private static final int NEIGHBOR_RANGE = 1;

    public SpheresPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
        int thisChunkX = chunkPos.getX() >> 4;
        int thisChunkZ = chunkPos.getZ() >> 4;

        int chunkMinX = thisChunkX << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = thisChunkZ << 4;
        int chunkMaxZ = chunkMinZ + 15;

        // Scan this chunk and all neighbors that could have spheres overlapping us
        for (int ncx = thisChunkX - NEIGHBOR_RANGE; ncx <= thisChunkX + NEIGHBOR_RANGE; ncx++) {
            for (int ncz = thisChunkZ - NEIGHBOR_RANGE; ncz <= thisChunkZ + NEIGHBOR_RANGE; ncz++) {
                // Deterministic seed per neighbor chunk (independent of visit order)
                long chunkSeed = getChunkSeed(ncx, ncz);
                Random chunkRand = new Random(chunkSeed);

                int neighborMinX = ncx << 4;
                int neighborMinZ = ncz << 4;

                for (int i = 0; i < SPHERES_PER_CHUNK; i++) {
                    // Compute sphere parameters deterministically for this neighbor chunk
                    int cx = neighborMinX + chunkRand.nextInt(16);
                    int cz = neighborMinZ + chunkRand.nextInt(16);
                    boolean floating = chunkRand.nextFloat() < FLOATING_CHANCE;
                    int radius = MIN_RADIUS + chunkRand.nextInt(MAX_RADIUS - MIN_RADIUS + 1);

                    // Material choices must be consumed deterministically
                    BlockState sphereBlock = getSphereMaterial(chunkRand, floating);
                    BlockState coreBlock = getCoreBlock(sphereBlock, chunkRand);

                    // Quick AABB check: can this sphere overlap our chunk at all?
                    if (cx + radius < chunkMinX || cx - radius > chunkMaxX ||
                        cz + radius < chunkMinZ || cz - radius > chunkMaxZ) {
                        // Still need to consume the random state for heightmap lookups
                        // so that subsequent spheres in this neighbor chunk stay deterministic.
                        // The heightmap calls use world state, not random, so we just skip.
                        continue;
                    }

                    // Get Y from heightmap (this is world-state dependent, not random-dependent)
                    int y;
                    if (floating) {
                        int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
                        y = groundY + 30 + chunkRand.nextInt(70);
                    } else {
                        int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
                        y = groundY - chunkRand.nextInt(20);
                    }

                    BlockPos center = new BlockPos(cx, y, cz);
                    generateSphere(world, chunkRand, center, radius, floating, sphereBlock, coreBlock,
                            chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
                }
            }
        }
    }

    /**
     * Deterministic per-chunk seed based on world seed and chunk coordinates.
     */
    private long getChunkSeed(int chunkX, int chunkZ) {
        return seed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + 0x5943E5L);
    }

    private boolean isInChunk(BlockPos pos, int minX, int maxX, int minZ, int maxZ) {
        return pos.getX() >= minX && pos.getX() <= maxX &&
               pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    private void generateSphere(WorldGenLevel world, Random rand, BlockPos center, int radius,
                                boolean floating, BlockState sphereBlock, BlockState coreBlock,
                                int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        int coreRadius = radius > 8 ? radius / 3 : 0;

        // Only iterate over the intersection of the sphere's AABB and the current chunk
        int startX = Math.max(-radius, chunkMinX - center.getX());
        int endX = Math.min(radius, chunkMaxX - center.getX());
        int startZ = Math.max(-radius, chunkMinZ - center.getZ());
        int endZ = Math.min(radius, chunkMaxZ - center.getZ());

        // Use a sub-seed for irregularity so it's position-deterministic, not iteration-order dependent
        long irregSeed = rand.nextLong();

        for (int dx = startX; dx <= endX; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = startZ; dz <= endZ; dz++) {
                    double distSq = dx * dx + dy * dy + dz * dz;
                    double radiusSq = (double) radius * radius;

                    // Position-deterministic irregularity
                    int bx = center.getX() + dx;
                    int by = center.getY() + dy;
                    int bz = center.getZ() + dz;
                    long posHash = positionHash(irregSeed, bx, by, bz);
                    double irregularity = (posHash & 0xFFFFL) / (double) 0xFFFFL * 0.8;

                    double effectiveRadius = radius + irregularity;
                    if (distSq <= effectiveRadius * effectiveRadius) {
                        BlockPos spherePos = new BlockPos(bx, by, bz);

                        BlockState blockToPlace;
                        if (coreRadius > 0 && distSq <= (double) coreRadius * coreRadius) {
                            blockToPlace = coreBlock;
                        } else {
                            blockToPlace = sphereBlock;
                        }

                        if (shouldPlaceSphereBlock(world, spherePos, floating)) {
                            world.setBlock(spherePos, blockToPlace, 2);
                        }
                    }
                }
            }
        }

        // Decorations for floating spheres
        if (floating) {
            addFloatingSphereDecorations(world, rand, center, radius,
                    chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
    }

    /**
     * Position-deterministic hash for irregularity. Same position always gets the same value
     * regardless of which chunk is being populated.
     */
    private static long positionHash(long seed, int x, int y, int z) {
        long h = seed;
        h ^= (long) x * 73856093L;
        h ^= (long) y * 19349663L;
        h ^= (long) z * 83492791L;
        h = h * 6364136223846793005L + 1442695040888963407L;
        return h;
    }

    private boolean shouldPlaceSphereBlock(WorldGenLevel world, BlockPos pos, boolean floating) {
        BlockState existing = world.getBlockState(pos);

        if (floating) {
            return existing.isAir() ||
                   existing.is(BlockTags.LEAVES) ||
                   existing.is(Blocks.SNOW) ||
                   !existing.isSolid();
        } else {
            return true;
        }
    }

    private BlockState getSphereMaterial(Random random, boolean floating) {
        if (floating) {
            int choice = random.nextInt(5);
            return switch (choice) {
                case 0 -> Blocks.SANDSTONE.defaultBlockState();
                case 1 -> Blocks.SMOOTH_STONE.defaultBlockState();
                case 2 -> Blocks.PRISMARINE.defaultBlockState();
                case 3 -> Blocks.END_STONE.defaultBlockState();
                default -> Blocks.STONE.defaultBlockState();
            };
        } else {
            int choice = random.nextInt(6);
            return switch (choice) {
                case 0 -> Blocks.STONE.defaultBlockState();
                case 1 -> Blocks.DEEPSLATE.defaultBlockState();
                case 2 -> Blocks.ANDESITE.defaultBlockState();
                case 3 -> Blocks.DIORITE.defaultBlockState();
                case 4 -> Blocks.GRANITE.defaultBlockState();
                default -> Blocks.COBBLESTONE.defaultBlockState();
            };
        }
    }

    private BlockState getCoreBlock(BlockState outerBlock, Random random) {
        if (random.nextInt(3) == 0) {
            int choice = random.nextInt(8);
            return switch (choice) {
                case 0 -> Blocks.OBSIDIAN.defaultBlockState();
                case 1 -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
                case 2 -> Blocks.GLOWSTONE.defaultBlockState();
                case 3 -> Blocks.SEA_LANTERN.defaultBlockState();
                case 4 -> Blocks.GOLD_BLOCK.defaultBlockState();
                case 5 -> Blocks.IRON_BLOCK.defaultBlockState();
                case 6 -> Blocks.LAPIS_BLOCK.defaultBlockState();
                default -> Blocks.DIAMOND_BLOCK.defaultBlockState();
            };
        }
        return outerBlock;
    }

    private void addFloatingSphereDecorations(WorldGenLevel world, Random rand, BlockPos center, int radius,
                                              int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        // Use deterministic sub-seed for decorations
        Random decorRand = new Random(rand.nextLong());
        for (int attempt = 0; attempt < radius / 2; attempt++) {
            double angle = decorRand.nextDouble() * Math.PI * 2;
            double elevation = (decorRand.nextDouble() - 0.5) * Math.PI;

            int dx = (int) (Math.cos(angle) * Math.cos(elevation) * (radius + 1));
            int dy = (int) (Math.sin(elevation) * (radius + 1));
            int dz = (int) (Math.sin(angle) * Math.cos(elevation) * (radius + 1));

            BlockPos decorPos = center.offset(dx, dy, dz);
            if (isInChunk(decorPos, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ)
                    && world.getBlockState(decorPos).isAir() && decorRand.nextInt(4) == 0) {
                BlockState decoration = decorRand.nextBoolean()
                        ? Blocks.GLOWSTONE.defaultBlockState()
                        : Blocks.SEA_LANTERN.defaultBlockState();
                world.setBlock(decorPos, decoration, 2);
            }
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:spheres";
    }
}
