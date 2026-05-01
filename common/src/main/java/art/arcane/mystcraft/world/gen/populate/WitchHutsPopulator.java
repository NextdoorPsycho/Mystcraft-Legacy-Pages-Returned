package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.WorldGenLevel;
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
 * Witch hut populator that generates swamp huts on stilts. Witch huts consist
 * of a small spruce wood structure on oak wood supports with a cauldron,
 * crafting table, and flower pot inside.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk
 * loads.
 */
public class WitchHutsPopulator implements IPopulate {

  private static final int DEFAULT_RARITY = 32;
  private final long seed;
  private final int rarity;

  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public WitchHutsPopulator(long seed) {
    this(seed, null);
  }

  public WitchHutsPopulator(long seed, JsonObject params) {
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

    if ((chunkX + chunkZ * 31L + seed) % rarity != 0) {
      return;
    }

    int x = chunkPos.getX() + random.nextInt(16);
    int z = chunkPos.getZ() + random.nextInt(16);

    BlockPos surfacePos = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z));

    if (surfacePos.getY() < 60 || surfacePos.getY() > 90) {
      return;
    }

    if (!isInChunk(surfacePos)) {
      return;
    }

    if (!world.getBiome(surfacePos).is(Biomes.SWAMP) && !world.getBiome(surfacePos).is(Biomes.MANGROVE_SWAMP)) {
      return;
    }

    generateWitchHut(world, random, surfacePos.below());
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

  private void generateWitchHut(WorldGenLevel world, RandomSource random, BlockPos groundPos) {
    int waterLevel = groundPos.getY();

    BlockState sprucePlanks = Blocks.SPRUCE_PLANKS.defaultBlockState();
    BlockState oakFence = Blocks.OAK_FENCE.defaultBlockState();
    BlockState spruceStairs = Blocks.SPRUCE_STAIRS.defaultBlockState();
    BlockState spruceSlab = Blocks.SPRUCE_SLAB.defaultBlockState();

    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        BlockPos pillarBase = groundPos.offset(x * 2, 0, z * 2);

        int height = 3 + random.nextInt(2);
        for (int y = 0; y < height; y++) {
          safeSetBlock(world, pillarBase.above(y), oakFence);
        }
      }
    }

    int floorY = waterLevel + 5;
    BlockPos floorPos = new BlockPos(groundPos.getX(), floorY, groundPos.getZ());

    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        safeSetBlock(world, floorPos.offset(x, 0, z), sprucePlanks);
      }
    }

    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        boolean isCorner = (Math.abs(x) == 2 && Math.abs(z) == 2);
        boolean isEdge = (Math.abs(x) == 2 || Math.abs(z) == 2);

        if (isEdge && !isCorner) {
          for (int y = 1; y <= 3; y++) {
            safeSetBlock(world, floorPos.offset(x, y, z), oakFence);
          }
        }
      }
    }

    for (int x = -3; x <= 3; x++) {
      for (int z = -3; z <= 3; z++) {
        int dist = Math.max(Math.abs(x), Math.abs(z));
        if (dist == 3) {
          BlockPos roofPos = floorPos.offset(x, 4, z);
          safeSetBlock(world, roofPos, spruceStairs.setValue(StairBlock.HALF, Half.TOP));
        } else if (dist == 2) {
          safeSetBlock(world, floorPos.offset(x, 5, z), sprucePlanks);
        }
      }
    }

    for (int x = -1; x <= 1; x++) {
      for (int z = -1; z <= 1; z++) {
        safeSetBlock(world, floorPos.offset(x, 6, z), spruceSlab.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
      }
    }

    safeSetBlock(world, floorPos.offset(-1, 1, -1), Blocks.CRAFTING_TABLE.defaultBlockState());
    safeSetBlock(world, floorPos.offset(1, 1, 1), Blocks.CAULDRON.defaultBlockState());
    safeSetBlock(world, floorPos.offset(-1, 2, 1), Blocks.FLOWER_POT.defaultBlockState());

    BlockPos chestPos = floorPos.offset(1, 1, -1);
    if (isInChunk(chestPos)) {
      world.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), 2);
      BlockEntity be = world.getBlockEntity(chestPos);
      if (be instanceof ChestBlockEntity chest) {
        chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, random.nextLong());
      }
    }

    if (isInChunk(floorPos)) {
      net.minecraft.server.level.ServerLevel serverLevel = world.getLevel();
      Witch witch = new Witch(EntityType.WITCH, serverLevel);
      witch.moveTo(floorPos.getX() + 0.5, floorPos.getY() + 1, floorPos.getZ() + 0.5, 0.0F, 0.0F);
      witch.setPersistenceRequired();
      world.addFreshEntity(witch);
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:witch_huts";
  }
}
