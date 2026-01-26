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
 * Spheres populator that generates floating or embedded spherical formations.
 * Used when the Spheres symbol is applied to an age.
 * Generates spherical formations of terrain blocks that can be floating in air or embedded in ground.
 */
public class SpheresPopulator implements IPopulate {

    private final long seed;

    private static final int SPHERES_PER_CHUNK = 2;
    private static final int MIN_RADIUS = 5;
    private static final int MAX_RADIUS = 15;
    private static final float FLOATING_CHANCE = 0.4f;

    public SpheresPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX();
        int chunkZ = chunkPos.getZ();

        for (int i = 0; i < SPHERES_PER_CHUNK; i++) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);

            // Determine if sphere is floating or embedded
            boolean floating = random.nextFloat() < FLOATING_CHANCE;

            int y;
            if (floating) {
                // Floating spheres spawn in the air (30-100 blocks above terrain)
                int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                y = groundY + 30 + random.nextInt(70);
            } else {
                // Embedded spheres spawn at or below ground level
                int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                y = groundY - random.nextInt(20);
            }

            BlockPos centerPos = new BlockPos(x, y, z);
            int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);

            // Generate sphere
            generateSphere(world, random, centerPos, radius, floating);
        }
    }

    private void generateSphere(ServerLevel world, RandomSource random, BlockPos center, int radius, boolean floating) {
        // Choose sphere material
        BlockState sphereBlock = getSphereMaterial(world, random, center, floating);
        BlockState coreBlock = getCoreBlock(sphereBlock, random);

        // Calculate core radius (inner sphere with different material)
        int coreRadius = radius > 8 ? radius / 3 : 0;

        // Generate sphere layer by layer
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    // Check if this position is within the sphere
                    // Add some irregularity to make it more interesting
                    double irregularity = random.nextDouble() * 0.8;
                    if (distance <= radius + irregularity) {
                        BlockPos spherePos = center.offset(dx, dy, dz);

                        // Determine which material to use
                        BlockState blockToPlace;
                        if (coreRadius > 0 && distance <= coreRadius) {
                            blockToPlace = coreBlock;
                        } else {
                            blockToPlace = sphereBlock;
                        }

                        // Place block if appropriate
                        if (shouldPlaceSphereBlock(world, spherePos, floating)) {
                            world.setBlock(spherePos, blockToPlace, 2);
                        }
                    }
                }
            }
        }

        // Add decorative elements for floating spheres
        if (floating) {
            addFloatingSphereDecorations(world, random, center, radius);
        }
    }

    private boolean shouldPlaceSphereBlock(ServerLevel world, BlockPos pos, boolean floating) {
        BlockState existing = world.getBlockState(pos);

        if (floating) {
            // Floating spheres replace air and soft blocks
            return existing.isAir() ||
                   existing.is(BlockTags.LEAVES) ||
                   existing.is(Blocks.SNOW) ||
                   !existing.isSolid();
        } else {
            // Embedded spheres replace most blocks
            return true;
        }
    }

    private BlockState getSphereMaterial(ServerLevel world, RandomSource random, BlockPos center, boolean floating) {
        if (floating) {
            // Floating spheres use lighter materials
            int choice = random.nextInt(5);
            return switch (choice) {
                case 0 -> Blocks.SANDSTONE.defaultBlockState();
                case 1 -> Blocks.SMOOTH_STONE.defaultBlockState();
                case 2 -> Blocks.PRISMARINE.defaultBlockState();
                case 3 -> Blocks.END_STONE.defaultBlockState();
                default -> Blocks.STONE.defaultBlockState();
            };
        } else {
            // Embedded spheres use heavier materials
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

    private BlockState getCoreBlock(BlockState outerBlock, RandomSource random) {
        // Core has a chance to be a valuable/interesting material
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

        // Otherwise use outer block material
        return outerBlock;
    }

    private void addFloatingSphereDecorations(ServerLevel world, RandomSource random, BlockPos center, int radius) {
        // Add occasional glowstone or lanterns on the surface
        for (int attempt = 0; attempt < radius / 2; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double elevation = (random.nextDouble() - 0.5) * Math.PI;

            int dx = (int) (Math.cos(angle) * Math.cos(elevation) * (radius + 1));
            int dy = (int) (Math.sin(elevation) * (radius + 1));
            int dz = (int) (Math.sin(angle) * Math.cos(elevation) * (radius + 1));

            BlockPos decorPos = center.offset(dx, dy, dz);
            if (world.getBlockState(decorPos).isAir() && random.nextInt(4) == 0) {
                BlockState decoration = random.nextBoolean()
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
