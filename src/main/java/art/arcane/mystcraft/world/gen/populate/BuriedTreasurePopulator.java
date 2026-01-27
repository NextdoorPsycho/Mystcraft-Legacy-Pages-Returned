package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Buried treasure populator that generates underground chests near beaches.
 * A single chest buried 3-6 blocks underground, surrounded by sand or gravel.
 * Approximately 1 per 20 chunks.
 */
public class BuriedTreasurePopulator implements IPopulate {

    private final long seed;

    private static final int RARITY = 20;

    private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

    public BuriedTreasurePopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX() >> 4;
        int chunkZ = chunkPos.getZ() >> 4;
        chunkMinX = chunkX << 4;
        chunkMaxX = chunkMinX + 15;
        chunkMinZ = chunkZ << 4;
        chunkMaxZ = chunkMinZ + 15;

        if ((chunkX * 23L + chunkZ * 59L + seed) % RARITY != 0) {
            return;
        }

        // Place at chunk center for consistency with explorer maps
        int x = chunkMinX + 9;
        int z = chunkMinZ + 9;

        int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        if (surfaceY < 55 || surfaceY > 75) {
            return;
        }

        // Check for sandy/gravelly ground (beach-like)
        BlockPos surfacePos = new BlockPos(x, surfaceY - 1, z);
        BlockState ground = world.getBlockState(surfacePos);
        boolean isSuitable = ground.is(Blocks.SAND) || ground.is(Blocks.GRAVEL) ||
                ground.is(Blocks.RED_SAND) || ground.is(Blocks.GRASS_BLOCK);

        if (!isSuitable) {
            return;
        }

        int depth = 3 + random.nextInt(4);
        int treasureY = surfaceY - depth;

        BlockPos chestPos = new BlockPos(x, treasureY, z);
        if (!isInChunk(chestPos)) {
            return;
        }

        // Place the chest
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(
                ChestBlock.FACING, Direction.NORTH), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setLootTable(BuiltInLootTables.BURIED_TREASURE, random.nextLong());
        }

        // Ensure solid blocks around the chest so it stays buried
        BlockState fillBlock = ground.is(Blocks.SAND) || ground.is(Blocks.RED_SAND) ?
                Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();

        for (Direction dir : Direction.values()) {
            BlockPos adjacent = chestPos.relative(dir);
            if (isInChunk(adjacent) && world.getBlockState(adjacent).isAir()) {
                world.setBlock(adjacent, fillBlock, 2);
            }
        }
    }

    private boolean isInChunk(BlockPos pos) {
        return pos.getX() >= chunkMinX && pos.getX() <= chunkMaxX &&
               pos.getZ() >= chunkMinZ && pos.getZ() <= chunkMaxZ;
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:buried_treasure";
    }
}
