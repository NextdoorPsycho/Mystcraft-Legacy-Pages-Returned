package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Standard ore populator that provides baseline ore generation for all ages.
 * Generates ores at approximately vanilla rates (slightly reduced).
 */
public class StandardOresPopulator implements IPopulate {

    private final long seed;

    // Ore configurations: blockState, veinSize, veinsPerChunk, minY, maxY
    private static final OreConfig[] ORE_CONFIGS = {
            new OreConfig(Blocks.COAL_ORE.defaultBlockState(), Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(),
                    17, 10, -64, 192),
            new OreConfig(Blocks.IRON_ORE.defaultBlockState(), Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),
                    9, 10, -64, 72),
            new OreConfig(Blocks.COPPER_ORE.defaultBlockState(), Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState(),
                    10, 8, -16, 112),
            new OreConfig(Blocks.GOLD_ORE.defaultBlockState(), Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState(),
                    9, 2, -64, 32),
            new OreConfig(Blocks.REDSTONE_ORE.defaultBlockState(), Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(),
                    8, 4, -64, 16),
            new OreConfig(Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState(),
                    8, 1, -64, 16),
            new OreConfig(Blocks.LAPIS_ORE.defaultBlockState(), Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState(),
                    7, 1, -64, 64),
            new OreConfig(Blocks.EMERALD_ORE.defaultBlockState(), Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState(),
                    3, 3, -16, 64)
    };

    public StandardOresPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        for (OreConfig config : ORE_CONFIGS) {
            generateOre(world, random, chunkPos, config);
        }
    }

    private void generateOre(ServerLevel world, RandomSource random, BlockPos chunkPos, OreConfig config) {
        int chunkX = chunkPos.getX();
        int chunkZ = chunkPos.getZ();

        for (int i = 0; i < config.veinsPerChunk; i++) {
            int x = chunkX + random.nextInt(16);
            int y = config.minY + random.nextInt(config.maxY - config.minY);
            int z = chunkZ + random.nextInt(16);

            generateVein(world, random, new BlockPos(x, y, z), config);
        }
    }

    private void generateVein(ServerLevel world, RandomSource random, BlockPos center, OreConfig config) {
        int numberOfBlocks = config.veinSize;

        float angle = random.nextFloat() * (float) Math.PI;
        double xSpread = Math.sin(angle) * numberOfBlocks / 8.0;
        double zSpread = Math.cos(angle) * numberOfBlocks / 8.0;

        double startX = center.getX() + 8 + xSpread;
        double endX = center.getX() + 8 - xSpread;
        double startZ = center.getZ() + 8 + zSpread;
        double endZ = center.getZ() + 8 - zSpread;

        double startY = center.getY() + random.nextInt(3) - 2;
        double endY = center.getY() + random.nextInt(3) - 2;

        for (int block = 0; block < numberOfBlocks; block++) {
            float progress = (float) block / (float) numberOfBlocks;
            double interpolatedX = startX + (endX - startX) * progress;
            double interpolatedY = startY + (endY - startY) * progress;
            double interpolatedZ = startZ + (endZ - startZ) * progress;

            double radius = random.nextDouble() * numberOfBlocks / 16.0;
            double xzRadius = (Math.sin((float) Math.PI * progress) + 1.0) * radius + 1.0;
            double yRadius = (Math.sin((float) Math.PI * progress) + 1.0) * radius + 1.0;

            int minX = Mth.floor(interpolatedX - xzRadius / 2.0);
            int minY = Mth.floor(interpolatedY - yRadius / 2.0);
            int minZ = Mth.floor(interpolatedZ - xzRadius / 2.0);
            int maxX = Mth.floor(interpolatedX + xzRadius / 2.0);
            int maxY = Mth.floor(interpolatedY + yRadius / 2.0);
            int maxZ = Mth.floor(interpolatedZ + xzRadius / 2.0);

            for (int x = minX; x <= maxX; x++) {
                double xDist = (x + 0.5 - interpolatedX) / (xzRadius / 2.0);
                if (xDist * xDist < 1.0) {
                    for (int y = minY; y <= maxY; y++) {
                        double yDist = (y + 0.5 - interpolatedY) / (yRadius / 2.0);
                        if (xDist * xDist + yDist * yDist < 1.0) {
                            for (int z = minZ; z <= maxZ; z++) {
                                double zDist = (z + 0.5 - interpolatedZ) / (xzRadius / 2.0);
                                if (xDist * xDist + yDist * yDist + zDist * zDist < 1.0) {
                                    BlockPos pos = new BlockPos(x, y, z);
                                    tryPlaceOre(world, pos, config);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void tryPlaceOre(ServerLevel world, BlockPos pos, OreConfig config) {
        BlockState existing = world.getBlockState(pos);

        // Replace stone, deepslate, netherrack, or end stone with appropriate ore
        if (existing.is(Blocks.STONE)) {
            world.setBlock(pos, config.oreBlock, 2);
        } else if (existing.is(Blocks.DEEPSLATE)) {
            world.setBlock(pos, config.deepslateOreBlock, 2);
        } else if (existing.is(Blocks.NETHERRACK)) {
            // Use regular ore in netherrack (for Mystcraft nether terrain)
            world.setBlock(pos, config.oreBlock, 2);
        } else if (existing.is(Blocks.END_STONE)) {
            // Use regular ore in end stone (for Mystcraft end terrain)
            world.setBlock(pos, config.oreBlock, 2);
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:standard_ores";
    }

    private static class OreConfig {
        final BlockState oreBlock;
        final BlockState deepslateOreBlock;
        final int veinSize;
        final int veinsPerChunk;
        final int minY;
        final int maxY;

        OreConfig(BlockState oreBlock, BlockState deepslateOreBlock, int veinSize, int veinsPerChunk, int minY, int maxY) {
            this.oreBlock = oreBlock;
            this.deepslateOreBlock = deepslateOreBlock;
            this.veinSize = veinSize;
            this.veinsPerChunk = veinsPerChunk;
            this.minY = minY;
            this.maxY = maxY;
        }
    }
}
