package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Surface lakes populator that generates water and lava lakes on the surface.
 * Water lakes are common and generate at the surface level, while lava lakes are rare.
 * Similar to vanilla lake generation but controlled by the SurfaceLakes symbol.
 */
public class SurfaceLakesPopulator implements IPopulate {

    private final long seed;

    // Number of lake attempts per chunk
    private static final int WATER_ATTEMPTS_PER_CHUNK = 4;
    private static final int LAVA_ATTEMPTS_PER_CHUNK = 1;

    // Lava lakes are much rarer than water lakes
    private static final int LAVA_RARITY = 10;

    public SurfaceLakesPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        // Generate water lakes
        for (int i = 0; i < WATER_ATTEMPTS_PER_CHUNK; i++) {
            int x = chunkPos.getX() + random.nextInt(16);
            int z = chunkPos.getZ() + random.nextInt(16);
            int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

            // Only 1 in 4 attempts actually generate
            if (random.nextInt(4) == 0) {
                generateSurfaceLake(world, random, new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), true);
            }
        }

        // Generate lava lakes (very rare)
        for (int i = 0; i < LAVA_ATTEMPTS_PER_CHUNK; i++) {
            if (random.nextInt(LAVA_RARITY) == 0) {
                int x = chunkPos.getX() + random.nextInt(16);
                int z = chunkPos.getZ() + random.nextInt(16);
                int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

                generateSurfaceLake(world, random, new BlockPos(x, y, z), Blocks.LAVA.defaultBlockState(), false);
            }
        }
    }

    private boolean generateSurfaceLake(ServerLevel world, RandomSource random, BlockPos center, BlockState liquidState, boolean isWater) {
        // Move center down to just below surface
        center = center.below(4);

        // Create a lake shape using spherical noise
        boolean[] lakeShape = new boolean[2048];

        // Generate multiple overlapping spheres for natural lake shape
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
                            lakeShape[(x * 16 + z) * 8 + y] = true;
                        }
                    }
                }
            }
        }

        // Verify the location is suitable for a surface lake
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 4; y < 8; y++) {
                    if (lakeShape[(x * 16 + z) * 8 + y]) {
                        BlockPos checkPos = center.offset(x, y, z);
                        BlockState state = world.getBlockState(checkPos);

                        // Don't generate if there's already a liquid at the surface
                        if (!state.getFluidState().isEmpty() && y >= 4) {
                            return false;
                        }

                        // Lava lakes should not generate near flammable materials
                        if (!isWater) {
                            BlockState aboveState = world.getBlockState(checkPos.above());
                            if (aboveState.is(BlockTags.LOGS) || aboveState.is(BlockTags.LEAVES) ||
                                aboveState.is(BlockTags.PLANKS)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        // Generate the lake
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    boolean inLake = lakeShape[(x * 16 + z) * 8 + y];
                    if (inLake) {
                        BlockPos lakePos = center.offset(x, y, z);
                        BlockState existing = world.getBlockState(lakePos);

                        if (y >= 4) {
                            // Air space above the lake
                            world.setBlock(lakePos, Blocks.AIR.defaultBlockState(), 2);
                        } else {
                            // Fill with liquid
                            world.setBlock(lakePos, liquidState, 2);
                        }
                    }
                }
            }
        }

        // Add decorative blocks around the lake edges
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    boolean inLake = lakeShape[(x * 16 + z) * 8 + y];
                    if (!inLake) {
                        continue;
                    }

                    BlockPos edgePos = center.offset(x, y, z);

                    // Check if this position is on the edge of the lake
                    boolean isEdge = false;
                    if (x < 15 && !lakeShape[((x + 1) * 16 + z) * 8 + y]) isEdge = true;
                    if (x > 0 && !lakeShape[((x - 1) * 16 + z) * 8 + y]) isEdge = true;
                    if (z < 15 && !lakeShape[(x * 16 + (z + 1)) * 8 + y]) isEdge = true;
                    if (z > 0 && !lakeShape[(x * 16 + (z - 1)) * 8 + y]) isEdge = true;

                    if (isEdge && y < 4) {
                        // Check blocks around the lake edge for decoration
                        BlockPos aboveEdge = edgePos.above();
                        BlockState aboveState = world.getBlockState(aboveEdge);

                        if (isWater) {
                            // Add sand/gravel around water lakes
                            if (aboveState.isSolid()) {
                                if (random.nextInt(2) == 0) {
                                    world.setBlock(aboveEdge, Blocks.SAND.defaultBlockState(), 2);
                                } else {
                                    world.setBlock(aboveEdge, Blocks.GRAVEL.defaultBlockState(), 2);
                                }
                            }

                            // Place clay at the bottom
                            if (y == 0 && random.nextInt(3) == 0) {
                                world.setBlock(edgePos, Blocks.CLAY.defaultBlockState(), 2);
                            }
                        } else {
                            // Add stone around lava lakes
                            if (aboveState.isSolid()) {
                                world.setBlock(aboveEdge, Blocks.STONE.defaultBlockState(), 2);
                            }

                            // Occasionally place magma blocks at the bottom
                            if (y == 0 && random.nextInt(4) == 0) {
                                world.setBlock(edgePos, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        }

        // For lava lakes, burn nearby flammable blocks
        if (!isWater) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 4; y < 8; y++) {
                        boolean inLake = lakeShape[(x * 16 + z) * 8 + y];
                        if (inLake) {
                            BlockPos burnPos = center.offset(x, y, z);
                            BlockState burnState = world.getBlockState(burnPos);

                            // Burn wooden blocks and leaves
                            if (burnState.is(BlockTags.LOGS) || burnState.is(BlockTags.LEAVES) ||
                                burnState.is(BlockTags.PLANKS) || burnState.is(BlockTags.FLOWERS)) {
                                world.setBlock(burnPos, Blocks.AIR.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:surface_lakes";
    }
}
