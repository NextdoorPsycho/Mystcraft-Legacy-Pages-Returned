package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Tendrils populator that generates vine-like terrain formations.
 * Tendrils twist, curve, and creep across multiple chunks.
 * Uses the neighbor-seed pattern: each chunk deterministically replays
 * nearby tendril paths and only places blocks within its own 16x16 area.
 */
public class TendrilsPopulator implements IPopulate {

    private final long seed;

    private static final int TENDRILS_PER_CHUNK = 1;
    private static final int MIN_LENGTH = 20;
    private static final int MAX_LENGTH = 50;
    private static final float CEILING_CHANCE = 0.3f;
    // ~8% of chunks spawn a tendril
    private static final float SPAWN_CHANCE = 0.08f;

    // Max lateral drift: curvature ±2.0 per segment * 50 segments = 100 blocks = 7 chunks
    private static final int NEIGHBOR_RANGE = 8;

    public TendrilsPopulator(long seed) {
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

        // Check this chunk and neighbors for tendrils that might reach us
        for (int ncx = thisChunkX - NEIGHBOR_RANGE; ncx <= thisChunkX + NEIGHBOR_RANGE; ncx++) {
            for (int ncz = thisChunkZ - NEIGHBOR_RANGE; ncz <= thisChunkZ + NEIGHBOR_RANGE; ncz++) {
                long chunkSeed = getChunkSeed(ncx, ncz);
                Random chunkRand = new Random(chunkSeed);

                int neighborMinX = ncx << 4;
                int neighborMinZ = ncz << 4;

                for (int i = 0; i < TENDRILS_PER_CHUNK; i++) {
                    // Deterministic spawn chance - skip most chunks
                    if (chunkRand.nextFloat() >= SPAWN_CHANCE) {
                        continue;
                    }

                    // Deterministically compute tendril origin
                    int startX = neighborMinX + chunkRand.nextInt(16);
                    int startZ = neighborMinZ + chunkRand.nextInt(16);
                    boolean fromCeiling = chunkRand.nextFloat() < CEILING_CHANCE;

                    // Consume all random state for this tendril deterministically
                    // (material, decoration, length, curvature params, etc.)
                    BlockState tendrilBlock = getTendrilMaterial(chunkRand);
                    BlockState decorationBlock = getDecorationBlock(tendrilBlock, chunkRand);
                    int length = MIN_LENGTH + chunkRand.nextInt(MAX_LENGTH - MIN_LENGTH + 1);

                    // Initial curvature - moderate lateral bias
                    double curvatureX = (chunkRand.nextDouble() - 0.5) * 1.5;
                    double curvatureZ = (chunkRand.nextDouble() - 0.5) * 1.5;
                    // Moderate base thickness: 2-6 blocks
                    int baseThickness = 2 + chunkRand.nextInt(5);

                    // Pre-consume all random calls for the tendril path so that
                    // the path is fully deterministic regardless of which chunk visits it.
                    // We store the path positions and then filter to our chunk.
                    long pathSeed = chunkRand.nextLong();
                    long decorSeed = chunkRand.nextLong();

                    // Now replay the tendril path and place only blocks in our chunk
                    generateTendril(world, pathSeed, decorSeed, startX, startZ, fromCeiling,
                            tendrilBlock, decorationBlock, length, curvatureX, curvatureZ,
                            baseThickness, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
                }
            }
        }
    }

    private long getChunkSeed(int chunkX, int chunkZ) {
        return seed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + 0x7E4D51L);
    }

    private void generateTendril(WorldGenLevel world, long pathSeed, long decorSeed,
                                 int startX, int startZ, boolean fromCeiling,
                                 BlockState tendrilBlock, BlockState decorationBlock,
                                 int length, double curvatureX, double curvatureZ,
                                 int baseThickness,
                                 int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        // Derive Y deterministically from the path seed so all chunks agree on
        // the same tendril origin. Reading the heightmap or world state at the
        // tendril start position can return different values depending on chunk
        // generation order, causing tendrils to be cut off at chunk borders.
        Random yRand = new Random(pathSeed ^ 0xA6E_57A47L);
        int startY;
        if (fromCeiling) {
            startY = 130 + yRand.nextInt(70); // Deterministic range 130-199, buried into ceiling
        } else {
            startY = 35 + yRand.nextInt(40);  // Deterministic range 35-74, buried underground
        }

        int direction = fromCeiling ? -1 : 1;
        Random pathRand = new Random(pathSeed);

        double currentX = startX;
        double currentY = startY;
        double currentZ = startZ;

        double curX = curvatureX;
        double curZ = curvatureZ;

        for (int segment = 0; segment < length; segment++) {
            float progress = (float) segment / length;

            // Update curve direction every 3 segments
            if (segment % 3 == 0) {
                curX += (pathRand.nextDouble() - 0.5) * 0.8;
                curZ += (pathRand.nextDouble() - 0.5) * 0.8;

                curX = Math.max(-2.0, Math.min(2.0, curX));
                curZ = Math.max(-2.0, Math.min(2.0, curZ));
            }

            // Occasional sharp turns
            if (pathRand.nextInt(10) == 0) {
                curX = (pathRand.nextDouble() - 0.5) * 2.5;
                curZ = (pathRand.nextDouble() - 0.5) * 2.5;
            }

            // Move along the curved path - lateral movement is the primary motion,
            // vertical is secondary (tendril creeps more than it climbs)
            currentX += curX;
            currentY += direction * (0.3 + pathRand.nextDouble() * 0.7);
            currentZ += curZ;

            // Dramatic thickness taper: thick base narrows to a point
            // Uses exponential falloff for more natural look
            double taperFactor = 1.0 - Math.pow(progress, 0.6);
            int thickness = (int) Math.max(1, Math.round(baseThickness * taperFactor));

            // Place blocks in a round cross-section, only within our chunk
            int centerBx = (int) Math.floor(currentX);
            int centerBy = (int) Math.floor(currentY);
            int centerBz = (int) Math.floor(currentZ);
            double thicknessSq = (double) thickness * thickness;
            for (int dx = -thickness; dx <= thickness; dx++) {
                for (int dz = -thickness; dz <= thickness; dz++) {
                    double distSq = (double) dx * dx + (double) dz * dz;
                    if (distSq <= thicknessSq) {
                        int bx = centerBx + dx;
                        int bz = centerBz + dz;

                        // Only place if within our chunk
                        if (bx >= chunkMinX && bx <= chunkMaxX && bz >= chunkMinZ && bz <= chunkMaxZ) {
                            BlockPos tendrilPos = new BlockPos(bx, centerBy, bz);
                            if (shouldPlaceTendrilBlock(world, tendrilPos)) {
                                world.setBlock(tendrilPos, tendrilBlock, 2);
                            }
                        }
                    }
                }
            }

            // Decorations use position-deterministic hash instead of sequential random,
            // so they're consistent regardless of which chunk is being populated
            if (segment > 3) {
                long decorHash = positionHash(decorSeed, centerBx, centerBy, centerBz);
                if ((decorHash & 0x7) == 0) { // ~1/8 chance, matching old decorRand.nextInt(8)
                    int dir = (int) ((decorHash >> 3) & 0x3);
                    int ddx = (dir == 0) ? 1 : (dir == 1) ? -1 : 0;
                    int ddz = (dir == 2) ? 1 : (dir == 3) ? -1 : 0;
                    int dbx = centerBx + ddx;
                    int dbz = centerBz + ddz;
                    if (dbx >= chunkMinX && dbx <= chunkMaxX && dbz >= chunkMinZ && dbz <= chunkMaxZ) {
                        BlockPos decorPos = new BlockPos(dbx, centerBy, dbz);
                        if (world.getBlockState(decorPos).isAir()) {
                            world.setBlock(decorPos, decorationBlock, 2);
                        }
                    }
                }
            }

            // Y bounds check (don't go past the allocated length)
            if (fromCeiling && currentY < startY - length) {
                break;
            } else if (!fromCeiling && currentY > startY + length) {
                break;
            }
        }
    }

    private boolean shouldPlaceTendrilBlock(WorldGenLevel world, BlockPos pos) {
        BlockState existing = world.getBlockState(pos);
        if (existing.isAir() ||
                existing.is(BlockTags.LEAVES) ||
                existing.is(Blocks.SNOW) ||
                existing.is(Blocks.VINE) ||
                existing.is(Blocks.WATER)) {
            return true;
        }
        // Allow replacing natural terrain so tendrils root into the ground
        return existing.is(Blocks.STONE) ||
               existing.is(Blocks.DEEPSLATE) ||
               existing.is(Blocks.DIRT) ||
               existing.is(Blocks.GRASS_BLOCK) ||
               existing.is(Blocks.SAND) ||
               existing.is(Blocks.SANDSTONE) ||
               existing.is(Blocks.GRAVEL) ||
               existing.is(Blocks.CLAY) ||
               existing.is(Blocks.NETHERRACK) ||
               existing.is(Blocks.END_STONE) ||
               existing.is(BlockTags.TERRACOTTA) ||
               existing.is(BlockTags.DIRT);
    }

    private BlockState getTendrilMaterial(Random random) {
        int choice = random.nextInt(8);
        return switch (choice) {
            case 0 -> Blocks.STONE.defaultBlockState();
            case 1 -> Blocks.COBBLESTONE.defaultBlockState();
            case 2 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 3 -> Blocks.ANDESITE.defaultBlockState();
            case 4 -> Blocks.DRIPSTONE_BLOCK.defaultBlockState();
            case 5 -> Blocks.BASALT.defaultBlockState();
            case 6 -> Blocks.BLACKSTONE.defaultBlockState();
            default -> Blocks.STONE_BRICKS.defaultBlockState();
        };
    }

    private BlockState getDecorationBlock(BlockState baseBlock, Random random) {
        if (random.nextInt(3) == 0) {
            int choice = random.nextInt(5);
            return switch (choice) {
                case 0 -> Blocks.GLOWSTONE.defaultBlockState();
                case 1 -> Blocks.SHROOMLIGHT.defaultBlockState();
                case 2 -> Blocks.SEA_LANTERN.defaultBlockState();
                case 3 -> Blocks.OCHRE_FROGLIGHT.defaultBlockState();
                default -> Blocks.LANTERN.defaultBlockState();
            };
        }
        return baseBlock;
    }

    /**
     * Position-deterministic hash. Same position always produces the same value
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

    @Override
    public String getIdentifier() {
        return "mystcraft:tendrils";
    }
}
