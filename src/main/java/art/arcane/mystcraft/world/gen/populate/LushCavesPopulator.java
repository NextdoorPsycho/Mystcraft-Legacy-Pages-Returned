package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Populates caves with lush cave vegetation features.
 * Generates moss, glow berries, azalea bushes, spore blossoms, and dripleaf plants.
 */
public class LushCavesPopulator implements IPopulate {

    private final long seed;
    private static final int ATTEMPTS_PER_CHUNK = 20;
    private static final int MAX_Y = 60;

    public LushCavesPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX();
        int chunkZ = chunkPos.getZ();

        for (int attempt = 0; attempt < ATTEMPTS_PER_CHUNK; attempt++) {
            int x = chunkX + random.nextInt(16);
            int y = world.getMinBuildHeight() + random.nextInt(MAX_Y - world.getMinBuildHeight());
            int z = chunkZ + random.nextInt(16);

            BlockPos pos = new BlockPos(x, y, z);

            if (!world.getBlockState(pos).isAir()) {
                continue;
            }

            // Check if we're in a cave
            if (!isInCave(world, pos)) {
                continue;
            }

            int featureType = random.nextInt(10);

            if (featureType < 3) {
                tryPlaceMoss(world, random, pos);
            } else if (featureType < 5) {
                tryPlaceGlowBerries(world, random, pos);
            } else if (featureType < 7) {
                tryPlaceAzalea(world, random, pos);
            } else if (featureType < 8) {
                tryPlaceSporeBlossoms(world, random, pos);
            } else {
                tryPlaceDripleaf(world, random, pos);
            }
        }
    }

    /**
     * Attempts to place moss blocks on surfaces.
     */
    private void tryPlaceMoss(ServerLevel world, RandomSource random, BlockPos center) {
        // Try placing moss on floor
        BlockPos below = center.below();
        if (canReplaceMoss(world.getBlockState(below))) {
            world.setBlock(below, Blocks.MOSS_BLOCK.defaultBlockState(), 2);

            // Spread moss to nearby blocks
            int spread = 2 + random.nextInt(3);
            for (int i = 0; i < spread; i++) {
                int offsetX = random.nextInt(3) - 1;
                int offsetZ = random.nextInt(3) - 1;
                BlockPos mossPos = below.offset(offsetX, 0, offsetZ);

                if (world.getBlockState(mossPos.above()).isAir() && canReplaceMoss(world.getBlockState(mossPos))) {
                    world.setBlock(mossPos, Blocks.MOSS_BLOCK.defaultBlockState(), 2);

                    // Occasionally add moss carpet on top
                    if (random.nextFloat() < 0.4f) {
                        world.setBlock(mossPos.above(), Blocks.MOSS_CARPET.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Try placing moss on ceiling
        BlockPos above = center.above();
        if (canReplaceMoss(world.getBlockState(above))) {
            world.setBlock(above, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        }
    }

    /**
     * Attempts to place glow berry vines hanging from the ceiling.
     */
    private void tryPlaceGlowBerries(ServerLevel world, RandomSource random, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = world.getBlockState(above);

        if (!aboveState.isSolid()) {
            return;
        }

        int length = 1 + random.nextInt(4);

        for (int i = 0; i < length; i++) {
            BlockPos vinePos = pos.below(i);

            if (!world.getBlockState(vinePos).isAir()) {
                break;
            }

            boolean hasBerries = random.nextFloat() < 0.3f;
            BlockState vineState = Blocks.CAVE_VINES.defaultBlockState()
                    .setValue(CaveVinesBlock.BERRIES, hasBerries);

            world.setBlock(vinePos, vineState, 2);
        }
    }

    /**
     * Attempts to place azalea bushes.
     */
    private void tryPlaceAzalea(ServerLevel world, RandomSource random, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = world.getBlockState(below);

        if (!canPlantOnMoss(belowState)) {
            return;
        }

        BlockState azaleaState = random.nextBoolean()
                ? Blocks.AZALEA.defaultBlockState()
                : Blocks.FLOWERING_AZALEA.defaultBlockState();

        world.setBlock(pos, azaleaState, 2);
    }

    /**
     * Attempts to place spore blossoms on the ceiling.
     */
    private void tryPlaceSporeBlossoms(ServerLevel world, RandomSource random, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = world.getBlockState(above);

        if (!aboveState.isSolid()) {
            return;
        }

        world.setBlock(pos, Blocks.SPORE_BLOSSOM.defaultBlockState(), 2);
    }

    /**
     * Attempts to place dripleaf plants in water.
     */
    private void tryPlaceDripleaf(ServerLevel world, RandomSource random, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = world.getBlockState(below);

        // Check for clay or moss in water
        if (!(belowState.is(Blocks.CLAY) || belowState.is(Blocks.MOSS_BLOCK))) {
            return;
        }

        // Check if there's water above the base block
        if (!world.getBlockState(pos).is(Blocks.WATER)) {
            return;
        }

        // Place small dripleaf
        if (random.nextBoolean()) {
            world.setBlock(pos, Blocks.SMALL_DRIPLEAF.defaultBlockState(), 2);
        } else {
            // Place big dripleaf (2-3 blocks tall)
            int height = 2 + random.nextInt(2);
            for (int i = 0; i < height; i++) {
                BlockPos leafPos = pos.above(i);
                if (world.getBlockState(leafPos).is(Blocks.WATER) || world.getBlockState(leafPos).isAir()) {
                    world.setBlock(leafPos, Blocks.BIG_DRIPLEAF.defaultBlockState(), 2);
                }
            }
        }
    }

    /**
     * Checks if the position is in a cave (air with stone nearby).
     */
    private boolean isInCave(ServerLevel world, BlockPos pos) {
        int stoneCount = 0;
        int airCount = 0;

        for (Direction direction : Direction.values()) {
            BlockState state = world.getBlockState(pos.relative(direction));
            if (state.isAir()) {
                airCount++;
            } else if (state.isSolid()) {
                stoneCount++;
            }
        }

        return airCount >= 2 && stoneCount >= 2;
    }

    /**
     * Checks if moss can replace this block.
     */
    private boolean canReplaceMoss(BlockState state) {
        return state.is(Blocks.STONE) ||
                state.is(Blocks.DEEPSLATE) ||
                state.is(Blocks.GRANITE) ||
                state.is(Blocks.DIORITE) ||
                state.is(Blocks.ANDESITE) ||
                state.is(Blocks.DIRT) ||
                state.is(Blocks.GRAVEL);
    }

    /**
     * Checks if plants can grow on this block.
     */
    private boolean canPlantOnMoss(BlockState state) {
        return state.is(Blocks.MOSS_BLOCK) ||
                state.is(Blocks.DIRT) ||
                state.is(Blocks.GRASS_BLOCK);
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:lush_caves";
    }
}
