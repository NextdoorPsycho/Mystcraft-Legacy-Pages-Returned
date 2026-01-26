package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Generates Bastion remnants with blackstone structures, piglin spawns, and gold blocks.
 * Spawns approximately 1 per 32 chunks.
 */
public class BastionRemnantsPopulator implements IPopulate {

    private final long seed;

    private static final int CHUNKS_BETWEEN_BASTIONS = 32;
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState POLISHED_BLACKSTONE = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState POLISHED_BLACKSTONE_BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState GILDED_BLACKSTONE = Blocks.GILDED_BLACKSTONE.defaultBlockState();
    private static final BlockState GOLD_BLOCK = Blocks.GOLD_BLOCK.defaultBlockState();
    private static final BlockState CHAIN = Blocks.CHAIN.defaultBlockState();
    private static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
    private static final BlockState MAGMA_BLOCK = Blocks.MAGMA_BLOCK.defaultBlockState();

    public BastionRemnantsPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX() >> 4;
        int chunkZ = chunkPos.getZ() >> 4;

        // Only generate in certain chunks (spacing control)
        if (chunkX % CHUNKS_BETWEEN_BASTIONS == 0 && chunkZ % CHUNKS_BETWEEN_BASTIONS == 0) {
            // Random chance to generate
            if (random.nextInt(100) < 8) {
                int x = chunkPos.getX() + random.nextInt(16);
                int y = 40 + random.nextInt(25);
                int z = chunkPos.getZ() + random.nextInt(16);

                BlockPos bastionPos = new BlockPos(x, y, z);
                generateBastion(world, random, bastionPos);
            }
        }
    }

    private void generateBastion(ServerLevel world, RandomSource random, BlockPos pos) {
        // Generate the main hall
        generateMainHall(world, random, pos);

        // Generate treasure room
        BlockPos treasurePos = pos.offset(12, 0, 0);
        generateTreasureRoom(world, random, treasurePos);

        // Generate housing units
        BlockPos housingPos = pos.offset(-12, 0, 8);
        generateHousingUnit(world, random, housingPos);

        // Generate bridge
        BlockPos bridgePos = pos.offset(0, 0, -12);
        generateBridge(world, random, bridgePos);

        // Generate magma cube spawner area
        BlockPos spawnerPos = pos.offset(8, -3, 8);
        generateSpawnerArea(world, random, spawnerPos);
    }

    private void generateMainHall(ServerLevel world, RandomSource random, BlockPos pos) {
        // 11x8x11 main hall
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                // Floor with decorative pattern
                BlockState floorBlock = (x + z) % 2 == 0 ? POLISHED_BLACKSTONE_BRICKS : POLISHED_BLACKSTONE;
                world.setBlock(pos.offset(x, -1, z), floorBlock, 2);

                // Ceiling
                world.setBlock(pos.offset(x, 7, z), POLISHED_BLACKSTONE_BRICKS, 2);

                // Clear interior
                for (int y = 0; y < 7; y++) {
                    world.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // Walls with gilded blackstone accents
        for (int y = 0; y < 7; y++) {
            for (int x = -5; x <= 5; x++) {
                BlockState wallBlock = random.nextInt(10) == 0 ? GILDED_BLACKSTONE : POLISHED_BLACKSTONE_BRICKS;
                world.setBlock(pos.offset(x, y, -5), wallBlock, 2);
                world.setBlock(pos.offset(x, y, 5), wallBlock, 2);
            }
            for (int z = -5; z <= 5; z++) {
                BlockState wallBlock = random.nextInt(10) == 0 ? GILDED_BLACKSTONE : POLISHED_BLACKSTONE_BRICKS;
                world.setBlock(pos.offset(-5, y, z), wallBlock, 2);
                world.setBlock(pos.offset(5, y, z), wallBlock, 2);
            }
        }

        // Add gold block pillars at corners
        for (int y = 0; y < 7; y++) {
            world.setBlock(pos.offset(-4, y, -4), GOLD_BLOCK, 2);
            world.setBlock(pos.offset(-4, y, 4), GOLD_BLOCK, 2);
            world.setBlock(pos.offset(4, y, -4), GOLD_BLOCK, 2);
            world.setBlock(pos.offset(4, y, 4), GOLD_BLOCK, 2);
        }

        // Add lanterns hanging from chains
        world.setBlock(pos.offset(0, 6, 0), CHAIN, 2);
        world.setBlock(pos.offset(0, 5, 0), CHAIN, 2);
        world.setBlock(pos.offset(0, 4, 0), LANTERN, 2);
    }

    private void generateTreasureRoom(ServerLevel world, RandomSource random, BlockPos pos) {
        // 7x5x7 treasure room
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                // Floor
                world.setBlock(pos.offset(x, -1, z), POLISHED_BLACKSTONE_BRICKS, 2);

                // Ceiling
                world.setBlock(pos.offset(x, 4, z), POLISHED_BLACKSTONE_BRICKS, 2);

                // Clear interior
                for (int y = 0; y < 4; y++) {
                    world.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // Walls
        for (int y = 0; y < 4; y++) {
            for (int x = -3; x <= 3; x++) {
                world.setBlock(pos.offset(x, y, -3), BLACKSTONE, 2);
                world.setBlock(pos.offset(x, y, 3), BLACKSTONE, 2);
            }
            for (int z = -3; z <= 3; z++) {
                world.setBlock(pos.offset(-3, y, z), BLACKSTONE, 2);
                world.setBlock(pos.offset(3, y, z), BLACKSTONE, 2);
            }
        }

        // Add gold blocks in center
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlock(pos.offset(x, 0, z), GOLD_BLOCK, 2);
            }
        }

        // Place treasure chests
        for (int i = 0; i < 4; i++) {
            BlockPos chestPos = pos.offset(random.nextInt(5) - 2, 1, random.nextInt(5) - 2);
            if (world.getBlockState(chestPos).isAir()) {
                world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
                BlockEntity be = world.getBlockEntity(chestPos);
                if (be instanceof ChestBlockEntity chest) {
                    chest.setLootTable(BuiltInLootTables.BASTION_TREASURE, random.nextLong());
                }
            }
        }
    }

    private void generateHousingUnit(ServerLevel world, RandomSource random, BlockPos pos) {
        // 5x4x5 housing unit
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Floor
                world.setBlock(pos.offset(x, -1, z), BLACKSTONE, 2);

                // Ceiling
                world.setBlock(pos.offset(x, 3, z), BLACKSTONE, 2);

                // Clear interior
                for (int y = 0; y < 3; y++) {
                    world.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // Walls (leave doorway on one side)
        for (int y = 0; y < 3; y++) {
            for (int x = -2; x <= 2; x++) {
                world.setBlock(pos.offset(x, y, -2), BLACKSTONE, 2);
                if (x != 0) {
                    world.setBlock(pos.offset(x, y, 2), BLACKSTONE, 2);
                }
            }
            for (int z = -2; z <= 2; z++) {
                world.setBlock(pos.offset(-2, y, z), BLACKSTONE, 2);
                world.setBlock(pos.offset(2, y, z), BLACKSTONE, 2);
            }
        }

        // Add chest with housing loot
        BlockPos chestPos = pos.offset(1, 0, 1);
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setLootTable(BuiltInLootTables.BASTION_OTHER, random.nextLong());
        }
    }

    private void generateBridge(ServerLevel world, RandomSource random, BlockPos pos) {
        // 3x1x10 bridge
        for (int z = 0; z < 10; z++) {
            for (int x = -1; x <= 1; x++) {
                world.setBlock(pos.offset(x, 0, z), POLISHED_BLACKSTONE, 2);

                // Clear above
                for (int y = 1; y <= 3; y++) {
                    world.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }

            // Add chain railings
            if (z % 2 == 0) {
                world.setBlock(pos.offset(-1, 1, z), CHAIN, 2);
                world.setBlock(pos.offset(1, 1, z), CHAIN, 2);
            }
        }
    }

    private void generateSpawnerArea(ServerLevel world, RandomSource random, BlockPos pos) {
        // 5x3x5 magma cube spawner area
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Floor with magma blocks
                world.setBlock(pos.offset(x, -1, z), MAGMA_BLOCK, 2);

                // Clear interior
                for (int y = 0; y < 3; y++) {
                    world.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // Place magma cube spawner
        world.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(EntityType.MAGMA_CUBE, random);
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:bastion_remnants";
    }
}
