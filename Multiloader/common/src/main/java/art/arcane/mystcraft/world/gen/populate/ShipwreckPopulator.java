package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
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
 * Shipwreck populator that generates sunken or beached ship structures.
 * Shipwrecks consist of oak/spruce planks and logs in a boat hull shape,
 * with up to 3 loot chests. Approximately 1 per 24 chunks.
 */
public class ShipwreckPopulator implements IPopulate {

    private final long seed;
    private final int rarity;

    private static final int DEFAULT_RARITY = 24;

    private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

    public ShipwreckPopulator(long seed) {
        this(seed, null);
    }

    public ShipwreckPopulator(long seed, JsonObject params) {
        this.seed = seed;
        this.rarity = PopulatorConfig.rarityFrom(params, DEFAULT_RARITY);
    }

    @Override
    public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX() >> 4;
        int chunkZ = chunkPos.getZ() >> 4;
        chunkMinX = chunkX << 4;
        chunkMaxX = chunkMinX + 15;
        chunkMinZ = chunkZ << 4;
        chunkMaxZ = chunkMinZ + 15;

        if ((chunkX * 31L + chunkZ * 17L + seed) % rarity != 0) {
            return;
        }

        int x = chunkPos.getX() + 4 + random.nextInt(8);
        int z = chunkPos.getZ() + 4 + random.nextInt(8);

        int surfaceY = world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        int waterSurfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

        // Must be underwater or at the beach water line
        if (surfaceY >= waterSurfaceY - 1) {
            // Check for beached variant (surface must be sand/gravel near water)
            BlockState ground = world.getBlockState(new BlockPos(x, surfaceY - 1, z));
            boolean isBeach = ground.is(Blocks.SAND) || ground.is(Blocks.GRAVEL);
            if (!isBeach) {
                return;
            }
        }

        BlockPos pos = new BlockPos(x, surfaceY, z);

        // Random rotation
        boolean rotated = random.nextBoolean();
        // Random tilt (partially buried)
        int tilt = random.nextInt(3) - 1;

        generateShipwreck(world, random, pos, rotated, tilt);
    }

    private boolean isInChunk(BlockPos pos) {
        return pos.getX() >= chunkMinX && pos.getX() <= chunkMaxX &&
               pos.getZ() >= chunkMinZ && pos.getZ() <= chunkMaxZ;
    }

    private void safeSetBlock(WorldGenLevel world, BlockPos pos, BlockState state) {
        if (isInChunk(pos)) {
            world.setBlock(pos, state, 2);
        }
    }

    private void generateShipwreck(WorldGenLevel world, RandomSource random, BlockPos pos,
                                   boolean rotated, int tilt) {
        int length = 10 + random.nextInt(6);
        int width = 4;
        int height = 4;

        boolean damaged = random.nextFloat() < 0.7f;
        BlockState hull = random.nextBoolean() ?
                Blocks.OAK_PLANKS.defaultBlockState() :
                Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState log = random.nextBoolean() ?
                Blocks.OAK_LOG.defaultBlockState() :
                Blocks.SPRUCE_LOG.defaultBlockState();

        // Build hull
        for (int l = 0; l < length; l++) {
            int tiltOffset = (l * tilt) / length;

            for (int w = -width / 2; w <= width / 2; w++) {
                int dx = rotated ? l : w;
                int dz = rotated ? w : l;
                int dy = tiltOffset;

                // Damage: random holes in the structure
                if (damaged && random.nextFloat() < 0.25f) {
                    continue;
                }

                // Hull bottom
                BlockPos hullPos = pos.offset(dx, dy, dz);
                safeSetBlock(world, hullPos, hull);

                // Sides (walls)
                boolean isSide = w == -width / 2 || w == width / 2;
                if (isSide) {
                    for (int h = 1; h < height; h++) {
                        if (damaged && random.nextFloat() < 0.3f) {
                            continue;
                        }
                        BlockPos wallPos = pos.offset(dx, dy + h, dz);
                        safeSetBlock(world, wallPos, hull);
                    }
                }

                // Bow and stern walls
                boolean isBow = l == 0;
                boolean isStern = l == length - 1;
                if (isBow || isStern) {
                    for (int h = 1; h < height; h++) {
                        if (damaged && random.nextFloat() < 0.3f) {
                            continue;
                        }
                        BlockPos wallPos = pos.offset(dx, dy + h, dz);
                        safeSetBlock(world, wallPos, hull);
                    }
                }
            }

            // Mast at center-ish
            if (l == length / 2 && !damaged) {
                int dx = rotated ? l : 0;
                int dz = rotated ? 0 : l;
                for (int h = 1; h <= 6; h++) {
                    BlockPos mastPos = pos.offset(dx, tiltOffset + h, dz);
                    safeSetBlock(world, mastPos, log);
                }
            }
        }

        // Place up to 3 chests
        int chestsPlaced = 0;
        for (int attempt = 0; attempt < 10 && chestsPlaced < 3; attempt++) {
            int l = random.nextInt(length);
            int w = random.nextInt(width - 1) - (width / 2 - 1);
            int tiltOffset = (l * tilt) / length;

            int dx = rotated ? l : w;
            int dz = rotated ? w : l;
            BlockPos chestPos = pos.offset(dx, tiltOffset + 1, dz);

            if (!isInChunk(chestPos)) {
                continue;
            }

            world.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(
                    ChestBlock.FACING, rotated ? Direction.NORTH : Direction.EAST), 2);
            BlockEntity be = world.getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity chest) {
                // Alternate between shipwreck loot tables
                switch (chestsPlaced) {
                    case 0 -> chest.setLootTable(BuiltInLootTables.SHIPWRECK_MAP, random.nextLong());
                    case 1 -> chest.setLootTable(BuiltInLootTables.SHIPWRECK_SUPPLY, random.nextLong());
                    default -> chest.setLootTable(BuiltInLootTables.SHIPWRECK_TREASURE, random.nextLong());
                }
                chestsPlaced++;
            }
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:shipwrecks";
    }
}
