package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Desert temple populator that generates pyramid structures.
 * Desert temples consist of a sandstone pyramid with a hidden treasure chamber
 * below with TNT traps and chests containing loot.
 */
public class DesertTemplesPopulator implements IPopulate {

    private final long seed;
    private static final int RARITY = 32;

    public DesertTemplesPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX() >> 4;
        int chunkZ = chunkPos.getZ() >> 4;

        if ((chunkX + chunkZ * 37L + seed) % RARITY != 0) {
            return;
        }

        int x = chunkPos.getX() + random.nextInt(16) + 8;
        int z = chunkPos.getZ() + random.nextInt(16) + 8;

        BlockPos surfacePos = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z));

        if (surfacePos.getY() < 60 || surfacePos.getY() > 90) {
            return;
        }

        if (!world.getBiome(surfacePos).is(Biomes.DESERT)) {
            return;
        }

        generateDesertTemple(world, random, surfacePos);
    }

    private void generateDesertTemple(WorldGenLevel world, RandomSource random, BlockPos basePos) {
        BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
        BlockState smoothSandstone = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        BlockState cutSandstone = Blocks.CUT_SANDSTONE.defaultBlockState();
        BlockState sandstoneSlab = Blocks.SANDSTONE_SLAB.defaultBlockState();
        BlockState sandstoneStairs = Blocks.SANDSTONE_STAIRS.defaultBlockState();
        BlockState blueTerracotta = Blocks.BLUE_TERRACOTTA.defaultBlockState();
        BlockState orangeTerracotta = Blocks.ORANGE_TERRACOTTA.defaultBlockState();

        BlockPos base = basePos.offset(-10, 0, -10);

        for (int x = 0; x <= 20; x++) {
            for (int z = 0; z <= 20; z++) {
                world.setBlock(base.offset(x, -1, z), sandstone, 2);
            }
        }

        for (int x = 0; x <= 20; x++) {
            for (int z = 0; z <= 20; z++) {
                boolean isEdge = (x == 0 || x == 20 || z == 0 || z == 20);

                if (isEdge) {
                    for (int y = 0; y <= 8; y++) {
                        world.setBlock(base.offset(x, y, z), sandstone, 2);
                    }
                } else {
                    world.setBlock(base.offset(x, 0, z), sandstone, 2);
                }
            }
        }

        for (int layer = 1; layer <= 6; layer++) {
            int inset = layer * 2;
            for (int x = inset; x <= 20 - inset; x++) {
                for (int z = inset; z <= 20 - inset; z++) {
                    boolean isLayerEdge = (x == inset || x == 20 - inset || z == inset || z == 20 - inset);
                    if (isLayerEdge) {
                        world.setBlock(base.offset(x, layer + 8, z), smoothSandstone, 2);
                    }
                }
            }
        }

        for (int x = 8; x <= 12; x++) {
            for (int z = 8; z <= 12; z++) {
                world.setBlock(base.offset(x, 1, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }

        world.setBlock(base.offset(9, 1, 9), blueTerracotta, 2);
        world.setBlock(base.offset(9, 1, 11), blueTerracotta, 2);
        world.setBlock(base.offset(11, 1, 9), blueTerracotta, 2);
        world.setBlock(base.offset(11, 1, 11), blueTerracotta, 2);
        world.setBlock(base.offset(10, 1, 10), orangeTerracotta, 2);

        for (int y = -5; y <= 0; y++) {
            for (int x = 8; x <= 12; x++) {
                for (int z = 8; z <= 12; z++) {
                    boolean isWall = (x == 8 || x == 12 || z == 8 || z == 12);
                    if (y == -5 || isWall) {
                        world.setBlock(base.offset(x, y, z), sandstone, 2);
                    } else {
                        world.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        BlockPos chest1Pos = base.offset(9, -4, 9);
        BlockPos chest2Pos = base.offset(9, -4, 11);
        BlockPos chest3Pos = base.offset(11, -4, 9);
        BlockPos chest4Pos = base.offset(11, -4, 11);

        world.setBlock(chest1Pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 2);
        world.setBlock(chest2Pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 2);
        world.setBlock(chest3Pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 2);
        world.setBlock(chest4Pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 2);

        for (BlockPos chestPos : new BlockPos[]{chest1Pos, chest2Pos, chest3Pos, chest4Pos}) {
            BlockEntity be = world.getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity chest) {
                chest.setLootTable(BuiltInLootTables.DESERT_PYRAMID, random.nextLong());
            }
        }

        world.setBlock(base.offset(10, -3, 10), Blocks.TNT.defaultBlockState(), 2);
        world.setBlock(base.offset(10, -2, 10), Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 2);

        for (int x = 9; x <= 11; x++) {
            for (int z = 9; z <= 11; z++) {
                if (x == 10 && z == 10) continue;
                world.setBlock(base.offset(x, -2, z), Blocks.TNT.defaultBlockState(), 2);
            }
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:desert_temples";
    }
}
