package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Woodland mansion populator that generates large dark oak structures.
 * Mansions consist of a multi-floor structure with various rooms, staircases,
 * and spawns of vindicators and evokers.
 *
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk loads.
 */
public class WoodlandMansionsPopulator implements IPopulate {

    private final long seed;
    private static final int RARITY = 64;

    // Chunk boundaries for current population
    private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

    public WoodlandMansionsPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX() >> 4;
        int chunkZ = chunkPos.getZ() >> 4;

        // Set chunk boundaries for this population run
        chunkMinX = chunkX << 4;
        chunkMaxX = chunkMinX + 15;
        chunkMinZ = chunkZ << 4;
        chunkMaxZ = chunkMinZ + 15;

        if ((chunkX + chunkZ * 53L + seed) % RARITY != 0) {
            return;
        }

        int x = chunkPos.getX() + random.nextInt(16);
        int z = chunkPos.getZ() + random.nextInt(16);

        BlockPos surfacePos = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z));

        if (surfacePos.getY() < 60 || surfacePos.getY() > 90) {
            return;
        }

        if (!world.getBiome(surfacePos).is(Biomes.DARK_FOREST)) {
            return;
        }

        generateWoodlandMansion(world, random, surfacePos);
    }

    /**
     * Checks if a position is within the current chunk boundaries.
     * This prevents cascade chunk loading when structures extend beyond chunk edges.
     */
    private boolean isInChunk(BlockPos pos) {
        return pos.getX() >= chunkMinX && pos.getX() <= chunkMaxX &&
               pos.getZ() >= chunkMinZ && pos.getZ() <= chunkMaxZ;
    }

    /**
     * Safe setBlock that only places blocks within current chunk boundaries.
     */
    private void safeSetBlock(WorldGenLevel world, BlockPos pos, BlockState state) {
        if (isInChunk(pos)) {
            world.setBlock(pos, state, 2);
        }
    }

    private void generateWoodlandMansion(WorldGenLevel world, RandomSource random, BlockPos basePos) {
        BlockState darkOakPlanks = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState darkOakLog = Blocks.DARK_OAK_LOG.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState darkOakStairs = Blocks.DARK_OAK_STAIRS.defaultBlockState();
        BlockState redCarpet = Blocks.RED_CARPET.defaultBlockState();

        BlockPos base = basePos.offset(-15, 0, -15);

        for (int x = 0; x <= 30; x++) {
            for (int z = 0; z <= 30; z++) {
                safeSetBlock(world, base.offset(x, -1, z), cobble);
            }
        }

        for (int floor = 0; floor < 3; floor++) {
            int floorY = floor * 6;

            for (int x = 0; x <= 30; x++) {
                for (int z = 0; z <= 30; z++) {
                    boolean isWall = (x == 0 || x == 30 || z == 0 || z == 30);

                    if (isWall) {
                        for (int y = 0; y <= 5; y++) {
                            safeSetBlock(world, base.offset(x, floorY + y, z), darkOakPlanks);
                        }

                        if ((x % 5 == 0 || z % 5 == 0) && floor < 2) {
                            safeSetBlock(world, base.offset(x, floorY, z), darkOakLog);
                            safeSetBlock(world, base.offset(x, floorY + 5, z), darkOakLog);
                        }
                    } else {
                        safeSetBlock(world, base.offset(x, floorY, z), darkOakPlanks);
                        safeSetBlock(world, base.offset(x, floorY + 5, z), darkOakPlanks);
                    }
                }
            }

            BlockPos entrance = base.offset(15, floorY + 1, 0);
            for (int y = 0; y < 4; y++) {
                safeSetBlock(world, entrance.above(y), Blocks.AIR.defaultBlockState());
                safeSetBlock(world, entrance.above(y).offset(1, 0, 0), Blocks.AIR.defaultBlockState());
            }

            int numRooms = 4 + random.nextInt(3);
            for (int i = 0; i < numRooms; i++) {
                int rx = 3 + random.nextInt(24);
                int rz = 3 + random.nextInt(24);
                int rw = 4 + random.nextInt(4);
                int rh = 4 + random.nextInt(4);

                for (int x = rx; x < rx + rw && x <= 27; x++) {
                    for (int z = rz; z < rz + rh && z <= 27; z++) {
                        boolean isRoomWall = (x == rx || x == rx + rw - 1 || z == rz || z == rz + rh - 1);

                        for (int y = 1; y <= 4; y++) {
                            if (isRoomWall) {
                                safeSetBlock(world, base.offset(x, floorY + y, z), darkOakPlanks);
                            } else {
                                safeSetBlock(world, base.offset(x, floorY + y, z), Blocks.AIR.defaultBlockState());
                            }
                        }

                        if (!isRoomWall && random.nextInt(20) == 0) {
                            safeSetBlock(world, base.offset(x, floorY + 1, z), redCarpet);
                        }
                    }
                }

                BlockPos doorPos = base.offset(rx, floorY + 1, rz);
                safeSetBlock(world, doorPos, Blocks.AIR.defaultBlockState());
                safeSetBlock(world, doorPos.above(), Blocks.AIR.defaultBlockState());

                if (random.nextInt(3) == 0) {
                    BlockPos chestPos = base.offset(rx + 1, floorY + 1, rz + 1);
                    if (isInChunk(chestPos)) {
                        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.NORTH), 2);
                        BlockEntity be = world.getBlockEntity(chestPos);
                        if (be instanceof ChestBlockEntity chest) {
                            chest.setLootTable(BuiltInLootTables.WOODLAND_MANSION, random.nextLong());
                        }
                    }
                }

                if (random.nextInt(2) == 0) {
                    BlockPos mobPos = base.offset(rx + rw / 2, floorY + 1, rz + rh / 2);
                    net.minecraft.server.level.ServerLevel serverLevel = world.getLevel();
                    if (random.nextInt(5) == 0) {
                        Evoker evoker = new Evoker(EntityType.EVOKER, serverLevel);
                        evoker.moveTo(mobPos.getX() + 0.5, mobPos.getY(), mobPos.getZ() + 0.5, 0.0F, 0.0F);
                        evoker.setPersistenceRequired();
                        world.addFreshEntity(evoker);
                    } else {
                        Vindicator vindicator = new Vindicator(EntityType.VINDICATOR, serverLevel);
                        vindicator.moveTo(mobPos.getX() + 0.5, mobPos.getY(), mobPos.getZ() + 0.5, 0.0F, 0.0F);
                        vindicator.setPersistenceRequired();
                        world.addFreshEntity(vindicator);
                    }
                }
            }

            if (floor < 2) {
                BlockPos stairBase = base.offset(5, floorY + 1, 5);
                for (int i = 0; i < 5; i++) {
                    safeSetBlock(world, stairBase.offset(i, i, 0),
                            darkOakStairs.setValue(StairBlock.FACING, Direction.EAST));
                    safeSetBlock(world, stairBase.offset(i, i + 1, 0), Blocks.AIR.defaultBlockState());
                }
            }
        }

        for (int x = 0; x <= 30; x++) {
            for (int z = 0; z <= 30; z++) {
                safeSetBlock(world, base.offset(x, 18, z), darkOakPlanks);
            }
        }

        for (int x = 5; x <= 25; x += 5) {
            for (int z = 5; z <= 25; z += 5) {
                for (int y = 18; y <= 22; y++) {
                    safeSetBlock(world, base.offset(x, y, z), darkOakLog);
                }
            }
        }

        for (int x = 3; x <= 27; x++) {
            for (int z = 3; z <= 27; z++) {
                if ((x > 5 && x < 25) || (z > 5 && z < 25)) {
                    safeSetBlock(world, base.offset(x, 23, z), darkOakPlanks);
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            BlockPos pillarBase = base.offset(15, 0, i * 7 + 3);
            for (int y = 0; y <= 18; y++) {
                safeSetBlock(world, pillarBase, darkOakLog);
            }
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:woodland_mansions";
    }
}
