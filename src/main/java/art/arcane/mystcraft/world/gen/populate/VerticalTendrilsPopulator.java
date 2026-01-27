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
 * Vertical tendrils populator that generates tall, mostly-straight columns
 * extending from ground or ceiling. These are the classic Mystcraft pillar-like
 * formations that grow vertically with only minor wobble.
 * Uses the neighbor-seed pattern for chunk-safe generation.
 */
public class VerticalTendrilsPopulator implements IPopulate {

    private final long seed;

    private static final int TENDRILS_PER_CHUNK = 1;
    private static final int MIN_LENGTH = 15;
    private static final int MAX_LENGTH = 45;
    private static final float CEILING_CHANCE = 0.3f;
    // ~10% of chunks spawn a vertical tendril
    private static final float SPAWN_CHANCE = 0.10f;

    // Vertical tendrils wobble slightly but can be thick, check nearby chunks
    private static final int NEIGHBOR_RANGE = 2;

    public VerticalTendrilsPopulator(long seed) {
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

                    int startX = neighborMinX + chunkRand.nextInt(16);
                    int startZ = neighborMinZ + chunkRand.nextInt(16);
                    boolean fromCeiling = chunkRand.nextFloat() < CEILING_CHANCE;

                    BlockState tendrilBlock = getTendrilMaterial(chunkRand);
                    BlockState decorationBlock = getDecorationBlock(tendrilBlock, chunkRand);
                    int length = MIN_LENGTH + chunkRand.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
                    int baseThickness = 1 + chunkRand.nextInt(4);

                    // Slight wobble parameters
                    double wobbleX = (chunkRand.nextDouble() - 0.5) * 0.15;
                    double wobbleZ = (chunkRand.nextDouble() - 0.5) * 0.15;
                    long pathSeed = chunkRand.nextLong();
                    long decorSeed = chunkRand.nextLong();

                    generateTendril(world, pathSeed, decorSeed, startX, startZ, fromCeiling,
                            tendrilBlock, decorationBlock, length, wobbleX, wobbleZ,
                            baseThickness, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
                }
            }
        }
    }

    private long getChunkSeed(int chunkX, int chunkZ) {
        return seed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + 0xBE47L);
    }

    private void generateTendril(WorldGenLevel world, long pathSeed, long decorSeed,
                                 int startX, int startZ, boolean fromCeiling,
                                 BlockState tendrilBlock, BlockState decorationBlock,
                                 int length, double wobbleX, double wobbleZ,
                                 int baseThickness,
                                 int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        int startY;
        if (fromCeiling) {
            startY = findCeilingPosition(world, startX, startZ, pathSeed);
            if (startY == -1) {
                return;
            }
            BlockPos checkPos = new BlockPos(startX, startY, startZ);
            if (!world.getBlockState(checkPos).isSolid() || !world.getBlockState(checkPos.below()).isAir()) {
                return;
            }
            // Bury into the ceiling by moving up into solid blocks
            startY += baseThickness + 3;
        } else {
            startY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, startX, startZ);
            BlockState ground = world.getBlockState(new BlockPos(startX, startY - 1, startZ));
            if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
                return;
            }
            // Bury into the ground so the base is embedded in terrain
            startY -= (baseThickness + 3);
        }

        int direction = fromCeiling ? -1 : 1;
        Random pathRand = new Random(pathSeed);

        double currentX = startX;
        double currentY = startY;
        double currentZ = startZ;

        for (int segment = 0; segment < length; segment++) {
            float progress = (float) segment / length;

            // Very gentle wobble updates
            if (segment % 5 == 0) {
                wobbleX += (pathRand.nextDouble() - 0.5) * 0.08;
                wobbleZ += (pathRand.nextDouble() - 0.5) * 0.08;
                wobbleX = Math.max(-0.2, Math.min(0.2, wobbleX));
                wobbleZ = Math.max(-0.2, Math.min(0.2, wobbleZ));
            }

            currentX += wobbleX;
            currentY += direction;
            currentZ += wobbleZ;

            int thickness = (int) Math.max(1, baseThickness * (1.0f - progress * 0.7f));

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

                        if (bx >= chunkMinX && bx <= chunkMaxX && bz >= chunkMinZ && bz <= chunkMaxZ) {
                            BlockPos tendrilPos = new BlockPos(bx, centerBy, bz);
                            if (shouldPlaceBlock(world, tendrilPos)) {
                                world.setBlock(tendrilPos, tendrilBlock, 2);
                            }
                        }
                    }
                }
            }

            // Position-deterministic decorations
            if (segment > 3) {
                long decorHash = positionHash(decorSeed, centerBx, centerBy, centerBz);
                if ((decorHash & 0x7) == 0) {
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

            if (fromCeiling && currentY < startY - length) {
                break;
            } else if (!fromCeiling && currentY > startY + length) {
                break;
            }
        }
    }

    private int findCeilingPosition(WorldGenLevel world, int x, int z, long pathSeed) {
        Random ceilRand = new Random(pathSeed ^ 0xCE116L);
        for (int attempt = 0; attempt < 10; attempt++) {
            int y = 80 + ceilRand.nextInt(120);
            BlockPos checkPos = new BlockPos(x, y, z);
            if (world.getBlockState(checkPos).isSolid() &&
                    world.getBlockState(checkPos.below()).isAir()) {
                return y;
            }
        }
        return -1;
    }

    private boolean shouldPlaceBlock(WorldGenLevel world, BlockPos pos) {
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
        return "mystcraft:vertical_tendrils";
    }
}
