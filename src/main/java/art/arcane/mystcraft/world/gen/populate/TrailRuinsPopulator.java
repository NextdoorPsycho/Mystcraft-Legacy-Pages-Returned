package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trail ruins populator that generates buried archaeological structures.
 * Ruins consist of mostly buried terracotta and gravel structures with suspicious gravel blocks.
 * Approximately 1 per 16 chunks.
 */
public class TrailRuinsPopulator implements IPopulate {

    private final long seed;

    private static final int CHUNKS_BETWEEN = 16;

    // Terracotta colors for ruins
    private static final BlockState[] TERRACOTTA_BLOCKS = {
            Blocks.TERRACOTTA.defaultBlockState(),
            Blocks.WHITE_TERRACOTTA.defaultBlockState(),
            Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState(),
            Blocks.GRAY_TERRACOTTA.defaultBlockState(),
            Blocks.BROWN_TERRACOTTA.defaultBlockState(),
            Blocks.RED_TERRACOTTA.defaultBlockState(),
            Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
            Blocks.YELLOW_TERRACOTTA.defaultBlockState()
    };

    public TrailRuinsPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        // Only attempt generation in specific chunks based on grid
        int chunkX = chunkPos.getX() >> 4;
        int chunkZ = chunkPos.getZ() >> 4;

        if (chunkX % CHUNKS_BETWEEN != 0 || chunkZ % CHUNKS_BETWEEN != 0) {
            return;
        }

        // Random check for generation
        if (random.nextFloat() > 0.5f) {
            return;
        }

        int x = chunkPos.getX() + random.nextInt(16) + 8;
        int z = chunkPos.getZ() + random.nextInt(16) + 8;

        // Find surface
        int y = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z);

        // Don't generate in water
        BlockPos surfacePos = new BlockPos(x, y, z);
        if (world.getBlockState(surfacePos).is(Blocks.WATER)) {
            return;
        }

        BlockPos pos = new BlockPos(x, y - 3, z); // Start 3 blocks below surface (mostly buried)

        generateRuins(world, random, pos);
    }

    private void generateRuins(ServerLevel world, RandomSource random, BlockPos pos) {
        // Generate scattered buried structure
        int radius = 6 + random.nextInt(4);

        // Generate random buried paths and structures
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Create irregular circular pattern
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) {
                    continue;
                }

                // Random placement to make it look ancient and scattered
                if (random.nextFloat() < 0.4f) {
                    continue;
                }

                // Vary the depth
                int dy = random.nextInt(4) - 2;
                BlockPos ruinPos = pos.offset(dx, dy, dz);

                // Choose block type
                BlockState ruinBlock;
                float blockChoice = random.nextFloat();

                if (blockChoice < 0.6f) {
                    // Terracotta (main structure material)
                    ruinBlock = TERRACOTTA_BLOCKS[random.nextInt(TERRACOTTA_BLOCKS.length)];
                } else if (blockChoice < 0.85f) {
                    // Gravel
                    ruinBlock = Blocks.GRAVEL.defaultBlockState();
                } else {
                    // Suspicious gravel (archaeology)
                    ruinBlock = Blocks.SUSPICIOUS_GRAVEL.defaultBlockState();
                }

                world.setBlock(ruinPos, ruinBlock, 2);

                // Sometimes add a layer above for variety
                if (random.nextFloat() < 0.3f) {
                    BlockPos abovePos = ruinPos.above();
                    if (random.nextFloat() < 0.7f) {
                        world.setBlock(abovePos, Blocks.GRAVEL.defaultBlockState(), 2);
                    } else {
                        world.setBlock(abovePos, Blocks.SUSPICIOUS_GRAVEL.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Add some decorated pot sherds and pottery as exposed ruins
        for (int i = 0; i < 3; i++) {
            int dx = random.nextInt(radius * 2) - radius;
            int dz = random.nextInt(radius * 2) - radius;

            int surfaceY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                    pos.getX() + dx, pos.getZ() + dz);

            BlockPos potPos = new BlockPos(pos.getX() + dx, surfaceY, pos.getZ() + dz);

            // Only place if on solid ground
            if (world.getBlockState(potPos).isAir() && world.getBlockState(potPos.below()).isSolid()) {
                world.setBlock(potPos, Blocks.DECORATED_POT.defaultBlockState(), 2);
            }
        }

        // Add some cobblestone paths (ancient road remnants)
        if (random.nextFloat() < 0.7f) {
            boolean horizontal = random.nextBoolean();
            int pathLength = 8 + random.nextInt(8);

            for (int i = 0; i < pathLength; i++) {
                int dx = horizontal ? i - pathLength / 2 : random.nextInt(3) - 1;
                int dz = horizontal ? random.nextInt(3) - 1 : i - pathLength / 2;

                BlockPos pathPos = pos.offset(dx, 0, dz);

                // Place cobblestone or mossy cobblestone
                BlockState pathBlock = random.nextFloat() < 0.3f ?
                        Blocks.MOSSY_COBBLESTONE.defaultBlockState() :
                        Blocks.COBBLESTONE.defaultBlockState();

                world.setBlock(pathPos, pathBlock, 2);
            }
        }

        // Add some wheat crops (overgrown)
        for (int i = 0; i < 5; i++) {
            int dx = random.nextInt(radius * 2) - radius;
            int dz = random.nextInt(radius * 2) - radius;

            int surfaceY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                    pos.getX() + dx, pos.getZ() + dz);

            BlockPos cropPos = new BlockPos(pos.getX() + dx, surfaceY, pos.getZ() + dz);

            if (world.getBlockState(cropPos).isAir() && world.getBlockState(cropPos.below()).is(Blocks.GRASS_BLOCK)) {
                world.setBlock(cropPos, Blocks.WHEAT.defaultBlockState(), 2);
            }
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:trail_ruins";
    }
}
