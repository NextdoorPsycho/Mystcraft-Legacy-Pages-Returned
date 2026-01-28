package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Jungle temple populator that generates mossy cobblestone structures.
 * Jungle temples consist of a puzzle-filled temple with levers, tripwires,
 * and hidden chests containing loot.
 */
public class JungleTemplesPopulator implements IPopulate {

    private final long seed;
    private static final int RARITY = 32;

    public JungleTemplesPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX() >> 4;
        int chunkZ = chunkPos.getZ() >> 4;

        if ((chunkX + chunkZ * 43L + seed) % RARITY != 0) {
            return;
        }

        int x = chunkPos.getX() + random.nextInt(16);
        int z = chunkPos.getZ() + random.nextInt(16);

        BlockPos surfacePos = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z));

        if (surfacePos.getY() < 60 || surfacePos.getY() > 90) {
            return;
        }

        if (!world.getBiome(surfacePos).is(Biomes.JUNGLE) && !world.getBiome(surfacePos).is(Biomes.BAMBOO_JUNGLE)) {
            return;
        }

        generateJungleTemple(world, random, surfacePos);
    }

    private void generateJungleTemple(WorldGenLevel world, RandomSource random, BlockPos basePos) {
        BlockState mossyCobble = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState cobbleStairs = Blocks.COBBLESTONE_STAIRS.defaultBlockState();
        BlockState mossyCobbleStairs = Blocks.MOSSY_COBBLESTONE_STAIRS.defaultBlockState();

        BlockPos base = basePos.offset(-7, 0, -7);

        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 14; z++) {
                world.setBlock(base.offset(x, -1, z), mossyCobble, 2);
            }
        }

        for (int y = 0; y <= 10; y++) {
            for (int x = 0; x <= 14; x++) {
                for (int z = 0; z <= 14; z++) {
                    boolean isWall = (x == 0 || x == 14 || z == 0 || z == 14);
                    boolean isCorner = (x == 0 || x == 14) && (z == 0 || z == 14);

                    if (y == 0 || (isWall && !isCorner)) {
                        world.setBlock(base.offset(x, y, z), random.nextInt(3) == 0 ? mossyCobble : cobble, 2);
                    } else if (!isWall) {
                        world.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 14; z++) {
                world.setBlock(base.offset(x, 11, z), random.nextInt(3) == 0 ? mossyCobble : cobble, 2);
            }
        }

        BlockPos entrance = base.offset(7, 1, 0);
        for (int y = 1; y <= 3; y++) {
            world.setBlock(entrance.above(y - 1), Blocks.AIR.defaultBlockState(), 2);
        }

        for (int i = 0; i < 3; i++) {
            BlockPos stairPos = base.offset(7, 0, -1 - i);
            world.setBlock(stairPos, cobbleStairs.setValue(StairBlock.FACING, Direction.NORTH), 2);
        }

        BlockPos chest1Pos = base.offset(3, 1, 7);
        BlockPos chest2Pos = base.offset(11, 1, 7);

        world.setBlock(chest1Pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST), 2);
        world.setBlock(chest2Pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), 2);

        BlockEntity be1 = world.getBlockEntity(chest1Pos);
        if (be1 instanceof ChestBlockEntity chest) {
            chest.setLootTable(BuiltInLootTables.JUNGLE_TEMPLE, random.nextLong());
        }

        BlockEntity be2 = world.getBlockEntity(chest2Pos);
        if (be2 instanceof ChestBlockEntity chest) {
            chest.setLootTable(BuiltInLootTables.JUNGLE_TEMPLE, random.nextLong());
        }

        BlockPos lever1Pos = base.offset(5, 2, 3);
        BlockPos lever2Pos = base.offset(7, 2, 3);
        BlockPos lever3Pos = base.offset(9, 2, 3);

        world.setBlock(lever1Pos, Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACING, Direction.SOUTH)
                .setValue(LeverBlock.FACE, AttachFace.WALL), 2);
        world.setBlock(lever2Pos, Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACING, Direction.SOUTH)
                .setValue(LeverBlock.FACE, AttachFace.WALL), 2);
        world.setBlock(lever3Pos, Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACING, Direction.SOUTH)
                .setValue(LeverBlock.FACE, AttachFace.WALL), 2);

        BlockPos redstonePos = base.offset(7, 1, 4);
        world.setBlock(redstonePos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);

        world.setBlock(base.offset(7, 0, 5), Blocks.STICKY_PISTON.defaultBlockState(), 2);
        world.setBlock(base.offset(7, 1, 5), mossyCobble, 2);

        BlockPos hookPos1 = base.offset(3, 2, 11);
        BlockPos hookPos2 = base.offset(11, 2, 11);

        world.setBlock(hookPos1, Blocks.TRIPWIRE_HOOK.defaultBlockState()
                .setValue(TripWireHookBlock.FACING, Direction.EAST), 2);
        world.setBlock(hookPos2, Blocks.TRIPWIRE_HOOK.defaultBlockState()
                .setValue(TripWireHookBlock.FACING, Direction.WEST), 2);

        for (int x = 4; x <= 10; x++) {
            world.setBlock(base.offset(x, 2, 11), Blocks.TRIPWIRE.defaultBlockState()
                    .setValue(TripWireBlock.ATTACHED, true), 2);
        }

        world.setBlock(base.offset(7, 1, 12), Blocks.DISPENSER.defaultBlockState(), 2);

        for (int i = 0; i < 4; i++) {
            BlockPos towerBase = base.offset(i == 0 || i == 1 ? 1 : 13, 0, i % 2 == 0 ? 1 : 13);
            for (int y = 0; y <= 15; y++) {
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        world.setBlock(towerBase.offset(dx, y, dz), random.nextInt(3) == 0 ? mossyCobble : cobble, 2);
                    }
                }
            }

            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = -1; dz <= 2; dz++) {
                    if (dx == -1 || dx == 2 || dz == -1 || dz == 2) {
                        world.setBlock(towerBase.offset(dx, 16, dz),
                                random.nextInt(3) == 0 ? mossyCobble : cobble, 2);
                    }
                }
            }
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:jungle_temples";
    }
}
