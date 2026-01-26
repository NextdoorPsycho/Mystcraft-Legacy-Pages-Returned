package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Deep lakes populator that generates underground water and lava pools.
 * These pools appear in caves and underground spaces, similar to vanilla underground lakes.
 * Water pools generate more frequently at higher Y levels, while lava pools are more common deep underground.
 */
public class DeepLakesPopulator implements IPopulate {

    private final long seed;

    // Number of lake attempts per chunk
    private static final int WATER_ATTEMPTS_PER_CHUNK = 4;
    private static final int LAVA_ATTEMPTS_PER_CHUNK = 1;

    // Y level thresholds for lake generation
    private static final int MAX_WATER_Y = 40;
    private static final int MIN_Y = -60;
    private static final int LAVA_PREFERRED_Y = -20;

    public DeepLakesPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        // Generate water pools
        for (int i = 0; i < WATER_ATTEMPTS_PER_CHUNK; i++) {
            int x = chunkPos.getX() + random.nextInt(16);
            int y = MIN_Y + random.nextInt(MAX_WATER_Y - MIN_Y);
            int z = chunkPos.getZ() + random.nextInt(16);

            generateLake(world, random, new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), true);
        }

        // Generate lava pools (more common at deeper levels)
        for (int i = 0; i < LAVA_ATTEMPTS_PER_CHUNK; i++) {
            int x = chunkPos.getX() + random.nextInt(16);
            int y = MIN_Y + random.nextInt(LAVA_PREFERRED_Y - MIN_Y);
            int z = chunkPos.getZ() + random.nextInt(16);

            generateLake(world, random, new BlockPos(x, y, z), Blocks.LAVA.defaultBlockState(), false);
        }
    }

    private boolean generateLake(ServerLevel world, RandomSource random, BlockPos center, BlockState liquidState, boolean isWater) {
        // Adjust center position down slightly
        center = center.below(4);

        // Create a spherical lake shape using noise
        boolean[] sphereShape = new boolean[2048];
        int sphereRadius = random.nextInt(4) + 4;

        // Generate sphere points
        for (int i = 0; i < random.nextInt(4) + 4; i++) {
            double sizeX = random.nextDouble() * 6.0 + 3.0;
            double sizeY = random.nextDouble() * 4.0 + 2.0;
            double sizeZ = random.nextDouble() * 6.0 + 3.0;
            double centerX = random.nextDouble() * (16.0 - sizeX - 2.0) + 1.0 + sizeX / 2.0;
            double centerY = random.nextDouble() * (8.0 - sizeY - 4.0) + 2.0 + sizeY / 2.0;
            double centerZ = random.nextDouble() * (16.0 - sizeZ - 2.0) + 1.0 + sizeZ / 2.0;

            for (int x = 1; x < 15; x++) {
                for (int z = 1; z < 15; z++) {
                    for (int y = 1; y < 7; y++) {
                        double xDist = (x - centerX) / (sizeX / 2.0);
                        double yDist = (y - centerY) / (sizeY / 2.0);
                        double zDist = (z - centerZ) / (sizeZ / 2.0);
                        double distSq = xDist * xDist + yDist * yDist + zDist * zDist;

                        if (distSq < 1.0) {
                            sphereShape[(x * 16 + z) * 8 + y] = true;
                        }
                    }
                }
            }
        }

        // Check if location is valid (must be underground)
        boolean isUnderground = true;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    boolean insideSphere = sphereShape[(x * 16 + z) * 8 + y];
                    if (insideSphere) {
                        BlockPos checkPos = center.offset(x, y, z);
                        BlockState state = world.getBlockState(checkPos);

                        // Lake must be surrounded by solid blocks
                        if (y >= 4 && state.isAir()) {
                            return false;
                        }

                        // Check for invalid materials
                        if (y < 4 && !state.isSolid() && !state.is(liquidState.getBlock())) {
                            return false;
                        }
                    }
                }
            }
        }

        // Generate the lake
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    boolean insideSphere = sphereShape[(x * 16 + z) * 8 + y];
                    if (insideSphere) {
                        BlockPos lakePos = center.offset(x, y, z);
                        BlockState existing = world.getBlockState(lakePos);

                        // Only replace solid blocks with liquid
                        if (existing.isSolid()) {
                            if (y >= 4) {
                                // Place air above liquid
                                world.setBlock(lakePos, Blocks.AIR.defaultBlockState(), 2);
                            } else {
                                // Place liquid
                                world.setBlock(lakePos, liquidState, 2);

                                // For lava lakes, occasionally place magma blocks at the bottom
                                if (!isWater && y < 2 && random.nextInt(3) == 0) {
                                    world.setBlock(lakePos, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add surrounding blocks (gravel for water, stone for lava)
        BlockState surroundingBlock = isWater ? Blocks.GRAVEL.defaultBlockState() : Blocks.STONE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 4; y < 8; y++) {
                    boolean insideSphere = sphereShape[(x * 16 + z) * 8 + y];
                    if (insideSphere) {
                        BlockPos surroundPos = center.offset(x, y, z);
                        BlockState existing = world.getBlockState(surroundPos);

                        // Check if this block is adjacent to liquid
                        boolean adjacentToLiquid = false;
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                BlockPos checkPos = surroundPos.offset(dx, 0, dz);
                                if (world.getBlockState(checkPos).is(liquidState.getBlock())) {
                                    adjacentToLiquid = true;
                                    break;
                                }
                            }
                            if (adjacentToLiquid) break;
                        }

                        // Place surrounding blocks on the floor adjacent to liquid
                        if (adjacentToLiquid && existing.isSolid() && world.getBlockState(surroundPos.below()).is(liquidState.getBlock())) {
                            world.setBlock(surroundPos, surroundingBlock, 2);
                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:deep_lakes";
    }
}
