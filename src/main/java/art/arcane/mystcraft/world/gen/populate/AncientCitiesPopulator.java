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
 * Ancient cities populator that generates deep dark city structures.
 * Cities consist of deepslate brick structures with sculk, soul lanterns, and warden spawns.
 * Very rare: approximately 1 per 64 chunks, only generates at Y < -20.
 */
public class AncientCitiesPopulator implements IPopulate {

    private final long seed;

    private static final int CHUNKS_BETWEEN = 64;
    private static final int MAX_Y = -20;
    private static final int MIN_Y = -50;

    public AncientCitiesPopulator(long seed) {
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

        // Very rare generation
        if (random.nextFloat() > 0.15f) {
            return;
        }

        int x = chunkPos.getX() + random.nextInt(16) + 8;
        int z = chunkPos.getZ() + random.nextInt(16) + 8;

        // Deep underground only
        int y = MIN_Y + random.nextInt(MAX_Y - MIN_Y);

        BlockPos pos = new BlockPos(x, y, z);

        generateAncientCity(world, random, pos);
    }

    private void generateAncientCity(ServerLevel world, RandomSource random, BlockPos pos) {
        // Build simplified city structure (21x21 base, 12 blocks tall)
        int radius = 10;
        int height = 12;

        // Clear area and add sculk foundation
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Floor layers with sculk
                for (int dy = -2; dy <= 0; dy++) {
                    BlockPos floorPos = pos.offset(dx, dy, dz);

                    BlockState floorBlock;
                    if (dy == 0) {
                        // Top floor - mix of deepslate and sculk
                        floorBlock = random.nextFloat() < 0.4f ?
                                Blocks.SCULK.defaultBlockState() :
                                Blocks.DEEPSLATE_TILES.defaultBlockState();
                    } else {
                        floorBlock = Blocks.DEEPSLATE.defaultBlockState();
                    }

                    world.setBlock(floorPos, floorBlock, 2);
                }

                // Clear interior
                for (int dy = 1; dy < height; dy++) {
                    BlockPos clearPos = pos.offset(dx, dy, dz);
                    world.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // Build central structure with deepslate bricks
        int structureRadius = 6;
        for (int dy = 1; dy < height; dy++) {
            for (int dx = -structureRadius; dx <= structureRadius; dx++) {
                for (int dz = -structureRadius; dz <= structureRadius; dz++) {
                    BlockPos buildPos = pos.offset(dx, dy, dz);

                    // Outer walls
                    if (Math.abs(dx) == structureRadius || Math.abs(dz) == structureRadius) {
                        BlockState wallBlock;
                        if (dy % 3 == 0) {
                            wallBlock = Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();
                        } else {
                            wallBlock = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                        }
                        world.setBlock(buildPos, wallBlock, 2);
                    }
                    // Corner pillars
                    else if (Math.abs(dx) == structureRadius - 1 && Math.abs(dz) == structureRadius - 1) {
                        world.setBlock(buildPos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Add ceiling
        for (int dx = -structureRadius; dx <= structureRadius; dx++) {
            for (int dz = -structureRadius; dz <= structureRadius; dz++) {
                BlockPos ceilingPos = pos.offset(dx, height, dz);
                world.setBlock(ceilingPos, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
            }
        }

        // Add sculk spread throughout
        for (int i = 0; i < 100; i++) {
            int dx = random.nextInt(radius * 2) - radius;
            int dy = random.nextInt(height);
            int dz = random.nextInt(radius * 2) - radius;

            BlockPos sculkPos = pos.offset(dx, dy, dz);

            float sculkType = random.nextFloat();
            if (sculkType < 0.6f) {
                world.setBlock(sculkPos, Blocks.SCULK.defaultBlockState(), 2);
            } else if (sculkType < 0.8f) {
                world.setBlock(sculkPos, Blocks.SCULK_VEIN.defaultBlockState(), 2);
            } else if (sculkType < 0.95f) {
                world.setBlock(sculkPos, Blocks.SCULK_CATALYST.defaultBlockState(), 2);
            } else {
                world.setBlock(sculkPos, Blocks.SCULK_SENSOR.defaultBlockState(), 2);
            }
        }

        // Add soul lanterns for lighting
        for (int i = 0; i < 15; i++) {
            int dx = random.nextInt(structureRadius * 2) - structureRadius;
            int dy = random.nextInt(height - 4) + 2;
            int dz = random.nextInt(structureRadius * 2) - structureRadius;

            BlockPos lanternPos = pos.offset(dx, dy, dz);

            // Place on walls
            if (Math.abs(dx) == structureRadius || Math.abs(dz) == structureRadius) {
                world.setBlock(lanternPos, Blocks.SOUL_LANTERN.defaultBlockState(), 2);
            }
        }

        // Add candles
        for (int i = 0; i < 20; i++) {
            int dx = random.nextInt(radius * 2) - radius;
            int dz = random.nextInt(radius * 2) - radius;

            BlockPos candlePos = pos.offset(dx, 1, dz);

            if (world.getBlockState(candlePos).isAir() && world.getBlockState(candlePos.below()).isSolid()) {
                world.setBlock(candlePos, Blocks.CANDLE.defaultBlockState(), 2);
            }
        }

        // Add reinforced deepslate (rare)
        for (int i = 0; i < 5; i++) {
            int dx = random.nextInt(structureRadius * 2) - structureRadius;
            int dy = random.nextInt(height);
            int dz = random.nextInt(structureRadius * 2) - structureRadius;

            BlockPos reinforcedPos = pos.offset(dx, dy, dz);
            world.setBlock(reinforcedPos, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), 2);
        }

        // Create central sculk shrieker area (warden spawn point)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos shriekerAreaPos = pos.offset(dx, 1, dz);

                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    world.setBlock(shriekerAreaPos, Blocks.SCULK_SHRIEKER.defaultBlockState(), 2);
                }
            }
        }

        // Small chance to spawn warden
        if (random.nextFloat() < 0.1f) {
            BlockPos wardenPos = pos.offset(0, 2, 0);
            EntityType.WARDEN.spawn(world, wardenPos, MobSpawnType.STRUCTURE);
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:ancient_cities";
    }
}
