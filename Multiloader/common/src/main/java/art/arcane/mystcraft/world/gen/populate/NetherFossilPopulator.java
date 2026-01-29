package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Nether fossil populator that generates bone block fossil structures.
 * Fossils consist of bone blocks arranged in rib-cage or skull-like shapes,
 * embedded in soul sand/soul soil. Approximately 1 per 16 chunks.
 */
public class NetherFossilPopulator implements IPopulate {

    private final long seed;
    private final int rarity;

    private static final int DEFAULT_RARITY = 16;

    private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

    public NetherFossilPopulator(long seed) {
        this(seed, null);
    }

    public NetherFossilPopulator(long seed, JsonObject params) {
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

        if ((chunkX * 47L + chunkZ * 13L + seed) % rarity != 0) {
            return;
        }

        int x = chunkPos.getX() + 4 + random.nextInt(8);
        int z = chunkPos.getZ() + 4 + random.nextInt(8);

        // Search for soul sand/soul soil to place fossil on
        int placementY = -1;
        for (int y = 40; y <= 100; y++) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(checkPos);
            BlockState above = world.getBlockState(checkPos.above());

            boolean isSoul = state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL);
            boolean aboveOpen = above.isAir() || above.is(Blocks.SOUL_SAND) || above.is(Blocks.SOUL_SOIL);

            if (isSoul && aboveOpen) {
                placementY = y;
                break;
            }
        }

        if (placementY == -1) {
            return;
        }

        BlockPos pos = new BlockPos(x, placementY, z);

        // Choose a fossil variant
        int variant = random.nextInt(4);
        switch (variant) {
            case 0 -> generateRibCage(world, random, pos);
            case 1 -> generateSkull(world, random, pos);
            case 2 -> generateSpine(world, random, pos);
            default -> generateScattered(world, random, pos);
        }
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

    private void generateRibCage(WorldGenLevel world, RandomSource random, BlockPos pos) {
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        int length = 5 + random.nextInt(4);

        // Spine
        for (int i = 0; i < length; i++) {
            safeSetBlock(world, pos.offset(i, 1, 0), bone);
        }

        // Ribs
        for (int i = 1; i < length - 1; i += 2) {
            int ribHeight = 2 + random.nextInt(2);
            // Left rib
            for (int h = 0; h < ribHeight; h++) {
                safeSetBlock(world, pos.offset(i, 1 + h, -1), bone);
                if (h == ribHeight - 1) {
                    safeSetBlock(world, pos.offset(i, 1 + h, -2), bone);
                }
            }
            // Right rib
            for (int h = 0; h < ribHeight; h++) {
                safeSetBlock(world, pos.offset(i, 1 + h, 1), bone);
                if (h == ribHeight - 1) {
                    safeSetBlock(world, pos.offset(i, 1 + h, 2), bone);
                }
            }
        }
    }

    private void generateSkull(WorldGenLevel world, RandomSource random, BlockPos pos) {
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();

        // 5x5x4 skull shape
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    double dist = Math.sqrt(dx * dx + dz * dz + (dy - 1.5) * (dy - 1.5));
                    if (dist > 2.8 || dist < 1.8) {
                        continue;
                    }
                    if (random.nextFloat() < 0.15f) {
                        continue;
                    }
                    safeSetBlock(world, pos.offset(dx, dy + 1, dz), bone);
                }
            }
        }

        // Eye sockets
        safeSetBlock(world, pos.offset(-1, 2, -2), Blocks.AIR.defaultBlockState());
        safeSetBlock(world, pos.offset(1, 2, -2), Blocks.AIR.defaultBlockState());
    }

    private void generateSpine(WorldGenLevel world, RandomSource random, BlockPos pos) {
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        int length = 8 + random.nextInt(6);

        for (int i = 0; i < length; i++) {
            safeSetBlock(world, pos.offset(i, 1, 0), bone);

            // Vertebrae bumps
            if (i % 2 == 0) {
                safeSetBlock(world, pos.offset(i, 2, 0), bone);
            }
        }
    }

    private void generateScattered(WorldGenLevel world, RandomSource random, BlockPos pos) {
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        int count = 8 + random.nextInt(8);

        for (int i = 0; i < count; i++) {
            int dx = random.nextInt(6) - 3;
            int dy = random.nextInt(3);
            int dz = random.nextInt(6) - 3;
            safeSetBlock(world, pos.offset(dx, dy + 1, dz), bone);
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:nether_fossils";
    }
}
