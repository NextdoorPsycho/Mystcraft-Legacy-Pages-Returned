package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Populator that generates veins of a single ore type.
 * Used by ore boost symbols to add extra veins of specific ores.
 */
public class SingleOrePopulator implements IPopulate {

    private final BlockState oreBlock;
    private final BlockState deepslateOreBlock;
    private final int veinSize;
    private final int veinsPerChunk;
    private final int minY;
    private final int maxY;
    private final String identifier;

    // Chunk boundaries for current population
    private int chunkMinX;
    private int chunkMaxX;
    private int chunkMinZ;
    private int chunkMaxZ;

    public SingleOrePopulator(BlockState oreBlock, BlockState deepslateOreBlock,
                              int veinSize, int veinsPerChunk, int minY, int maxY,
                              String identifier) {
        this.oreBlock = oreBlock;
        this.deepslateOreBlock = deepslateOreBlock;
        this.veinSize = veinSize;
        this.veinsPerChunk = veinsPerChunk;
        this.minY = minY;
        this.maxY = maxY;
        this.identifier = identifier;
    }

    @Override
    public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX() >> 4;
        int chunkZ = chunkPos.getZ() >> 4;

        chunkMinX = chunkX << 4;
        chunkMaxX = chunkMinX + 15;
        chunkMinZ = chunkZ << 4;
        chunkMaxZ = chunkMinZ + 15;

        int originX = chunkPos.getX();
        int originZ = chunkPos.getZ();

        for (int i = 0; i < veinsPerChunk; i++) {
            int x = originX + random.nextInt(16);
            int y = minY + random.nextInt(maxY - minY);
            int z = originZ + random.nextInt(16);

            generateVein(world, random, new BlockPos(x, y, z));
        }
    }

    private void generateVein(WorldGenLevel world, RandomSource random, BlockPos center) {
        float angle = random.nextFloat() * (float) Math.PI;
        double xSpread = Math.sin(angle) * veinSize / 8.0;
        double zSpread = Math.cos(angle) * veinSize / 8.0;

        double startX = center.getX() + 8 + xSpread;
        double endX = center.getX() + 8 - xSpread;
        double startZ = center.getZ() + 8 + zSpread;
        double endZ = center.getZ() + 8 - zSpread;

        double startY = center.getY() + random.nextInt(3) - 2;
        double endY = center.getY() + random.nextInt(3) - 2;

        for (int block = 0; block < veinSize; block++) {
            float progress = (float) block / (float) veinSize;
            double interpolatedX = startX + (endX - startX) * progress;
            double interpolatedY = startY + (endY - startY) * progress;
            double interpolatedZ = startZ + (endZ - startZ) * progress;

            double radius = random.nextDouble() * veinSize / 16.0;
            double xzRadius = (Math.sin((float) Math.PI * progress) + 1.0) * radius + 1.0;
            double yRadius = (Math.sin((float) Math.PI * progress) + 1.0) * radius + 1.0;

            int minBlockX = Mth.floor(interpolatedX - xzRadius / 2.0);
            int minBlockY = Mth.floor(interpolatedY - yRadius / 2.0);
            int minBlockZ = Mth.floor(interpolatedZ - xzRadius / 2.0);
            int maxBlockX = Mth.floor(interpolatedX + xzRadius / 2.0);
            int maxBlockY = Mth.floor(interpolatedY + yRadius / 2.0);
            int maxBlockZ = Mth.floor(interpolatedZ + xzRadius / 2.0);

            for (int x = minBlockX; x <= maxBlockX; x++) {
                double xDist = (x + 0.5 - interpolatedX) / (xzRadius / 2.0);
                if (xDist * xDist < 1.0) {
                    for (int y = minBlockY; y <= maxBlockY; y++) {
                        double yDist = (y + 0.5 - interpolatedY) / (yRadius / 2.0);
                        if (xDist * xDist + yDist * yDist < 1.0) {
                            for (int z = minBlockZ; z <= maxBlockZ; z++) {
                                double zDist = (z + 0.5 - interpolatedZ) / (xzRadius / 2.0);
                                if (xDist * xDist + yDist * yDist + zDist * zDist < 1.0) {
                                    BlockPos pos = new BlockPos(x, y, z);
                                    tryPlaceOre(world, pos);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void tryPlaceOre(WorldGenLevel world, BlockPos pos) {
        if (!isInChunk(pos)) {
            return;
        }

        BlockState existing = world.getBlockState(pos);

        // Replace any solid opaque block
        if (existing.isAir() || !existing.isSolid() || !existing.canOcclude()) {
            return;
        }

        // Use deepslate variant below Y=0
        if (pos.getY() < 0) {
            world.setBlock(pos, deepslateOreBlock, 2);
        } else {
            world.setBlock(pos, oreBlock, 2);
        }
    }

    private boolean isInChunk(BlockPos pos) {
        return pos.getX() >= chunkMinX && pos.getX() <= chunkMaxX &&
               pos.getZ() >= chunkMinZ && pos.getZ() <= chunkMaxZ;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }
}
