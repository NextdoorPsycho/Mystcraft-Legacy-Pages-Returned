package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

/**
 * Biome decoration populator that adds trees, grass, and flowers.
 * Uses biome information to determine appropriate vegetation.
 */
public class BiomeDecorationPopulator implements IPopulate {

    private final long seed;

    public BiomeDecorationPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX();
        int chunkZ = chunkPos.getZ();

        // Sample biome at chunk center
        BlockPos centerPos = new BlockPos(chunkX + 8, 64, chunkZ + 8);
        Holder<Biome> biomeHolder = world.getBiome(centerPos);

        // Generate vegetation based on biome
        generateTrees(world, random, chunkPos, biomeHolder);
        generateGrass(world, random, chunkPos, biomeHolder);
        generateFlowers(world, random, chunkPos, biomeHolder);
    }

    private void generateTrees(ServerLevel world, RandomSource random, BlockPos chunkPos, Holder<Biome> biomeHolder) {
        int treesPerChunk = getTreesPerChunk(biomeHolder);

        for (int i = 0; i < treesPerChunk; i++) {
            int x = chunkPos.getX() + random.nextInt(16);
            int z = chunkPos.getZ() + random.nextInt(16);
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos treePos = new BlockPos(x, y, z);

            // Check if valid location for tree
            BlockState ground = world.getBlockState(treePos.below());
            if (!canSupportTree(ground)) {
                continue;
            }

            // Check if there's enough space
            if (!hasSpaceForTree(world, treePos)) {
                continue;
            }

            // Generate tree based on biome
            generateTree(world, random, treePos, biomeHolder);
        }
    }

    private int getTreesPerChunk(Holder<Biome> biomeHolder) {
        // Determine tree density based on biome
        if (biomeHolder.is(BiomeTags.IS_FOREST)) {
            return 10;
        } else if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
            return 50;
        } else if (biomeHolder.is(BiomeTags.IS_TAIGA)) {
            return 8;
        } else if (biomeHolder.is(BiomeTags.IS_BADLANDS) || biomeHolder.is(BiomeTags.IS_BEACH)) {
            return 0;
        } else if (biomeHolder.is(Biomes.PLAINS) || biomeHolder.is(Biomes.SUNFLOWER_PLAINS)) {
            return 1;
        } else if (biomeHolder.is(Biomes.SAVANNA) || biomeHolder.is(Biomes.SAVANNA_PLATEAU)) {
            return 2;
        } else if (biomeHolder.is(Biomes.SWAMP) || biomeHolder.is(Biomes.MANGROVE_SWAMP)) {
            return 4;
        }
        // Default
        return 3;
    }

    private boolean canSupportTree(BlockState ground) {
        return ground.is(BlockTags.DIRT) || ground.is(Blocks.GRASS_BLOCK) ||
                ground.is(Blocks.PODZOL) || ground.is(Blocks.MYCELIUM) ||
                ground.is(Blocks.MUD) || ground.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    private boolean hasSpaceForTree(ServerLevel world, BlockPos pos) {
        // Check 5x5 area above for clearance
        for (int dy = 0; dy < 6; dy++) {
            BlockState state = world.getBlockState(pos.above(dy));
            if (state.isSolid() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) {
                return false;
            }
        }
        return true;
    }

    private void generateTree(ServerLevel world, RandomSource random, BlockPos pos, Holder<Biome> biomeHolder) {
        // Determine tree type based on biome
        TreeType treeType = getTreeType(biomeHolder, random);

        // Generate simple tree structure
        switch (treeType) {
            case OAK -> generateOakTree(world, random, pos);
            case BIRCH -> generateBirchTree(world, random, pos);
            case SPRUCE -> generateSpruceTree(world, random, pos);
            case JUNGLE -> generateJungleTree(world, random, pos);
            case ACACIA -> generateAcaciaTree(world, random, pos);
            case DARK_OAK -> generateDarkOakTree(world, random, pos);
        }
    }

    private TreeType getTreeType(Holder<Biome> biomeHolder, RandomSource random) {
        if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
            return TreeType.JUNGLE;
        } else if (biomeHolder.is(BiomeTags.IS_TAIGA)) {
            return TreeType.SPRUCE;
        } else if (biomeHolder.is(Biomes.BIRCH_FOREST) || biomeHolder.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
            return TreeType.BIRCH;
        } else if (biomeHolder.is(Biomes.DARK_FOREST)) {
            return random.nextInt(3) == 0 ? TreeType.OAK : TreeType.DARK_OAK;
        } else if (biomeHolder.is(Biomes.SAVANNA) || biomeHolder.is(Biomes.SAVANNA_PLATEAU)) {
            return TreeType.ACACIA;
        } else if (biomeHolder.is(BiomeTags.IS_FOREST)) {
            return random.nextInt(5) == 0 ? TreeType.BIRCH : TreeType.OAK;
        }
        // Default to oak
        return random.nextInt(10) == 0 ? TreeType.BIRCH : TreeType.OAK;
    }

    private void generateOakTree(ServerLevel world, RandomSource random, BlockPos pos) {
        int height = 4 + random.nextInt(3);
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
        generateSimpleTree(world, pos, height, log, leaves);
    }

    private void generateBirchTree(ServerLevel world, RandomSource random, BlockPos pos) {
        int height = 5 + random.nextInt(3);
        BlockState log = Blocks.BIRCH_LOG.defaultBlockState();
        BlockState leaves = Blocks.BIRCH_LEAVES.defaultBlockState();
        generateSimpleTree(world, pos, height, log, leaves);
    }

    private void generateSpruceTree(ServerLevel world, RandomSource random, BlockPos pos) {
        int height = 6 + random.nextInt(4);
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState();
        generateConiferTree(world, pos, height, log, leaves);
    }

    private void generateJungleTree(ServerLevel world, RandomSource random, BlockPos pos) {
        int height = 4 + random.nextInt(8);
        BlockState log = Blocks.JUNGLE_LOG.defaultBlockState();
        BlockState leaves = Blocks.JUNGLE_LEAVES.defaultBlockState();
        generateSimpleTree(world, pos, height, log, leaves);
    }

    private void generateAcaciaTree(ServerLevel world, RandomSource random, BlockPos pos) {
        int height = 5 + random.nextInt(3);
        BlockState log = Blocks.ACACIA_LOG.defaultBlockState();
        BlockState leaves = Blocks.ACACIA_LEAVES.defaultBlockState();
        generateSimpleTree(world, pos, height, log, leaves);
    }

    private void generateDarkOakTree(ServerLevel world, RandomSource random, BlockPos pos) {
        int height = 6 + random.nextInt(3);
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.DARK_OAK_LEAVES.defaultBlockState();
        generateSimpleTree(world, pos, height, log, leaves);
    }

    private void generateSimpleTree(ServerLevel world, BlockPos pos, int height, BlockState log, BlockState leaves) {
        // Trunk
        for (int y = 0; y < height; y++) {
            BlockPos logPos = pos.above(y);
            if (world.getBlockState(logPos).isAir() || world.getBlockState(logPos).is(BlockTags.LEAVES)) {
                world.setBlock(logPos, log, 2);
            }
        }

        // Canopy
        int canopyStart = height - 3;
        for (int y = canopyStart; y <= height + 1; y++) {
            int radius = y < height ? 2 : 1;
            if (y == height + 1) {
                radius = 0;
            }

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Skip corners for rounder shape
                    if (Math.abs(x) == radius && Math.abs(z) == radius && radius > 1) {
                        continue;
                    }
                    // Skip trunk position
                    if (x == 0 && z == 0 && y < height) {
                        continue;
                    }

                    BlockPos leafPos = pos.offset(x, y, z);
                    if (world.getBlockState(leafPos).isAir()) {
                        world.setBlock(leafPos, leaves, 2);
                    }
                }
            }
        }
    }

    private void generateConiferTree(ServerLevel world, BlockPos pos, int height, BlockState log, BlockState leaves) {
        // Trunk
        for (int y = 0; y < height; y++) {
            BlockPos logPos = pos.above(y);
            if (world.getBlockState(logPos).isAir() || world.getBlockState(logPos).is(BlockTags.LEAVES)) {
                world.setBlock(logPos, log, 2);
            }
        }

        // Conical canopy
        for (int y = 2; y <= height; y++) {
            int radius = (height - y) / 2 + 1;
            if (y == height) {
                radius = 0;
            }

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Skip trunk position
                    if (x == 0 && z == 0 && y < height) {
                        continue;
                    }

                    // Skip corners for rounder shape
                    if (Math.abs(x) == radius && Math.abs(z) == radius && radius > 1) {
                        continue;
                    }

                    BlockPos leafPos = pos.offset(x, y, z);
                    if (world.getBlockState(leafPos).isAir()) {
                        world.setBlock(leafPos, leaves, 2);
                    }
                }
            }
        }

        // Top leaf
        world.setBlock(pos.above(height), leaves, 2);
    }

    private void generateGrass(ServerLevel world, RandomSource random, BlockPos chunkPos, Holder<Biome> biomeHolder) {
        // Skip grass in inappropriate biomes
        if (biomeHolder.is(BiomeTags.IS_BADLANDS) || biomeHolder.is(BiomeTags.IS_BEACH) ||
                biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_RIVER)) {
            return;
        }

        int grassPatches = getGrassPatches(biomeHolder);

        for (int i = 0; i < grassPatches; i++) {
            int x = chunkPos.getX() + random.nextInt(16);
            int z = chunkPos.getZ() + random.nextInt(16);
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos grassPos = new BlockPos(x, y, z);

            BlockState ground = world.getBlockState(grassPos.below());
            if (ground.is(Blocks.GRASS_BLOCK) || ground.is(BlockTags.DIRT)) {
                if (world.getBlockState(grassPos).isAir()) {
                    // Choose grass type
                    BlockState grass = random.nextInt(3) == 0 ?
                            Blocks.TALL_GRASS.defaultBlockState() :
                            Blocks.GRASS.defaultBlockState();

                    if (grass.is(Blocks.TALL_GRASS)) {
                        // Only place tall grass if there's room
                        if (world.getBlockState(grassPos.above()).isAir()) {
                            world.setBlock(grassPos, Blocks.TALL_GRASS.defaultBlockState(), 2);
                        } else {
                            world.setBlock(grassPos, Blocks.GRASS.defaultBlockState(), 2);
                        }
                    } else {
                        world.setBlock(grassPos, grass, 2);
                    }
                }
            }
        }
    }

    private int getGrassPatches(Holder<Biome> biomeHolder) {
        if (biomeHolder.is(Biomes.PLAINS) || biomeHolder.is(Biomes.SUNFLOWER_PLAINS)) {
            return 30;
        } else if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
            return 25;
        } else if (biomeHolder.is(BiomeTags.IS_FOREST)) {
            return 15;
        } else if (biomeHolder.is(BiomeTags.IS_SAVANNA)) {
            return 10;
        }
        return 20;
    }

    private void generateFlowers(ServerLevel world, RandomSource random, BlockPos chunkPos, Holder<Biome> biomeHolder) {
        // Skip flowers in inappropriate biomes
        if (biomeHolder.is(BiomeTags.IS_BADLANDS) || biomeHolder.is(BiomeTags.IS_BEACH) ||
                biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_RIVER) ||
                biomeHolder.is(BiomeTags.IS_TAIGA)) {
            return;
        }

        int flowerPatches = getFlowerPatches(biomeHolder);

        for (int i = 0; i < flowerPatches; i++) {
            int x = chunkPos.getX() + random.nextInt(16);
            int z = chunkPos.getZ() + random.nextInt(16);
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos flowerPos = new BlockPos(x, y, z);

            BlockState ground = world.getBlockState(flowerPos.below());
            if (ground.is(Blocks.GRASS_BLOCK) || ground.is(BlockTags.DIRT)) {
                if (world.getBlockState(flowerPos).isAir()) {
                    BlockState flower = getRandomFlower(random, biomeHolder);
                    world.setBlock(flowerPos, flower, 2);
                }
            }
        }
    }

    private int getFlowerPatches(Holder<Biome> biomeHolder) {
        if (biomeHolder.is(Biomes.FLOWER_FOREST)) {
            return 20;
        } else if (biomeHolder.is(Biomes.MEADOW)) {
            return 15;
        } else if (biomeHolder.is(Biomes.PLAINS) || biomeHolder.is(Biomes.SUNFLOWER_PLAINS)) {
            return 5;
        }
        return 2;
    }

    private BlockState getRandomFlower(RandomSource random, Holder<Biome> biomeHolder) {
        BlockState[] flowers;

        if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
            flowers = new BlockState[]{
                    Blocks.ORANGE_TULIP.defaultBlockState(),
                    Blocks.ALLIUM.defaultBlockState(),
                    Blocks.BLUE_ORCHID.defaultBlockState()
            };
        } else if (biomeHolder.is(Biomes.SWAMP) || biomeHolder.is(Biomes.MANGROVE_SWAMP)) {
            flowers = new BlockState[]{
                    Blocks.BLUE_ORCHID.defaultBlockState()
            };
        } else {
            flowers = new BlockState[]{
                    Blocks.POPPY.defaultBlockState(),
                    Blocks.DANDELION.defaultBlockState(),
                    Blocks.CORNFLOWER.defaultBlockState(),
                    Blocks.AZURE_BLUET.defaultBlockState(),
                    Blocks.OXEYE_DAISY.defaultBlockState(),
                    Blocks.RED_TULIP.defaultBlockState(),
                    Blocks.ORANGE_TULIP.defaultBlockState(),
                    Blocks.WHITE_TULIP.defaultBlockState(),
                    Blocks.PINK_TULIP.defaultBlockState()
            };
        }

        return flowers[random.nextInt(flowers.length)];
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:biome_decoration";
    }

    private enum TreeType {
        OAK, BIRCH, SPRUCE, JUNGLE, ACACIA, DARK_OAK
    }
}
