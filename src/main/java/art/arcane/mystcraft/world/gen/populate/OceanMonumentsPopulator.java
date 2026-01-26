package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ocean monument populator that generates simplified guardian temples.
 * Monuments consist of prismarine structures with elder guardian spawners and treasure.
 * Approximately 1 per 64 chunks, only generates in water.
 */
public class OceanMonumentsPopulator implements IPopulate {

    private final long seed;

    private static final int CHUNKS_BETWEEN = 64;

    public OceanMonumentsPopulator(long seed) {
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
        if (random.nextFloat() > 0.2f) {
            return;
        }

        int x = chunkPos.getX() + random.nextInt(16) + 8;
        int z = chunkPos.getZ() + random.nextInt(16) + 8;

        // Find ocean floor
        int oceanFloor = -1;
        for (int y = 40; y <= 62; y++) {
            BlockPos checkPos = new BlockPos(x, y, z);
            if (!world.getBlockState(checkPos).is(Blocks.WATER)) {
                continue;
            }

            // Check if we have solid ground below and water above
            BlockPos belowPos = checkPos.below();
            if (world.getBlockState(belowPos).isSolid()) {
                // Make sure we have enough water depth (at least 10 blocks)
                boolean hasDepth = true;
                for (int dy = 1; dy <= 10; dy++) {
                    if (!world.getBlockState(checkPos.above(dy)).is(Blocks.WATER)) {
                        hasDepth = false;
                        break;
                    }
                }

                if (hasDepth) {
                    oceanFloor = y;
                    break;
                }
            }
        }

        // Must be in deep water
        if (oceanFloor == -1) {
            return;
        }

        BlockPos pos = new BlockPos(x, oceanFloor, z);
        generateMonument(world, random, pos);
    }

    private void generateMonument(ServerLevel world, RandomSource random, BlockPos pos) {
        // Build simplified monument structure (11x11 base, 8 blocks tall)
        int radius = 5;
        int height = 8;

        // Foundation
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos foundationPos = pos.offset(dx, 0, dz);
                world.setBlock(foundationPos, Blocks.PRISMARINE.defaultBlockState(), 2);
            }
        }

        // Build walls with dark prismarine
        for (int dy = 1; dy < height; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos buildPos = pos.offset(dx, dy, dz);

                    // Outer walls
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        BlockState wallBlock = random.nextFloat() < 0.3f ?
                                Blocks.DARK_PRISMARINE.defaultBlockState() :
                                Blocks.PRISMARINE_BRICKS.defaultBlockState();
                        world.setBlock(buildPos, wallBlock, 2);
                    }
                    // Interior - mostly water
                    else {
                        world.setBlock(buildPos, Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Roof
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos roofPos = pos.offset(dx, height, dz);
                world.setBlock(roofPos, Blocks.PRISMARINE.defaultBlockState(), 2);
            }
        }

        // Add sea lanterns for lighting
        for (int i = 0; i < 8; i++) {
            int dx = (random.nextInt(radius * 2) - radius);
            int dy = random.nextInt(height - 2) + 1;
            int dz = (random.nextInt(radius * 2) - radius);

            BlockPos lanternPos = pos.offset(dx, dy, dz);
            if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                world.setBlock(lanternPos, Blocks.SEA_LANTERN.defaultBlockState(), 2);
            }
        }

        // Create central sponge room
        int spongeRadius = 2;
        int spongeY = height / 2;
        for (int dx = -spongeRadius; dx <= spongeRadius; dx++) {
            for (int dy = spongeY - 1; dy <= spongeY + 1; dy++) {
                for (int dz = -spongeRadius; dz <= spongeRadius; dz++) {
                    BlockPos spongePos = pos.offset(dx, dy, dz);

                    // Walls of sponge room
                    if (Math.abs(dx) == spongeRadius || Math.abs(dz) == spongeRadius || dy == spongeY - 1 || dy == spongeY + 1) {
                        world.setBlock(spongePos, Blocks.PRISMARINE_BRICKS.defaultBlockState(), 2);
                    } else {
                        // Air inside sponge room
                        world.setBlock(spongePos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Add sponges
        for (int i = 0; i < 30; i++) {
            int dx = random.nextInt(spongeRadius * 2) - spongeRadius;
            int dz = random.nextInt(spongeRadius * 2) - spongeRadius;

            BlockPos wetSpongePos = pos.offset(dx, spongeY, dz);
            if (world.getBlockState(wetSpongePos).isAir()) {
                world.setBlock(wetSpongePos, Blocks.WET_SPONGE.defaultBlockState(), 2);
            }
        }

        // Add gold block core (treasure)
        BlockPos goldPos = pos.offset(0, spongeY, 0);
        world.setBlock(goldPos, Blocks.GOLD_BLOCK.defaultBlockState(), 2);

        // Spawn elder guardian
        BlockPos guardianPos = pos.offset(0, spongeY + 2, 0);
        EntityType.ELDER_GUARDIAN.spawn(world, guardianPos, MobSpawnType.STRUCTURE);

        // Spawn regular guardians
        for (int i = 0; i < 3; i++) {
            int dx = random.nextInt(8) - 4;
            int dy = random.nextInt(height - 2) + 1;
            int dz = random.nextInt(8) - 4;

            BlockPos spawnPos = pos.offset(dx, dy, dz);
            if (world.getBlockState(spawnPos).is(Blocks.WATER)) {
                EntityType.GUARDIAN.spawn(world, spawnPos, MobSpawnType.STRUCTURE);
            }
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:ocean_monuments";
    }
}
