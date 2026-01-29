package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Ruined portal populator that generates broken nether portal structures.
 * Portals consist of partial obsidian frames with crying obsidian, netherrack, and gold blocks.
 * Approximately 1 per 16 chunks.
 */
public class RuinedPortalsPopulator implements IPopulate {

  private static final int CHUNKS_BETWEEN = 16;
  private final long seed;
  private final float spawnChance;

  public RuinedPortalsPopulator(long seed) {
    this(seed, null);
  }

  public RuinedPortalsPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spawnChance = PopulatorConfig.chanceFrom(params, 1.0f, 0);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    if (random.nextFloat() > spawnChance) {
      return;
    }
    // Only attempt generation in specific chunks based on grid
    int chunkX = chunkPos.getX() >> 4;
    int chunkZ = chunkPos.getZ() >> 4;

    if (chunkX % CHUNKS_BETWEEN != 0 || chunkZ % CHUNKS_BETWEEN != 0) {
      return;
    }

    // Random check for generation
    if (random.nextFloat() > 0.4f) {
      return;
    }

    int x = chunkPos.getX() + random.nextInt(16);
    int z = chunkPos.getZ() + random.nextInt(16);

    // Find surface
    int y = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z);

    BlockPos pos = new BlockPos(x, y, z);

    generateRuinedPortal(world, random, pos);
  }

  private void generateRuinedPortal(WorldGenLevel world, RandomSource random, BlockPos pos) {
    // Standard nether portal is 4 wide, 5 tall
    // We'll make it partially broken

    // Randomize orientation (north-south or east-west)
    boolean northSouth = random.nextBoolean();

    // Build partial frame
    for (int dy = 0; dy < 5; dy++) {
      for (int d = 0; d < 4; d++) {
        BlockPos framePos;

        if (northSouth) {
          framePos = pos.offset(d, dy, 0);
        } else {
          framePos = pos.offset(0, dy, d);
        }

        // Frame edges
        if (d == 0 || d == 3 || dy == 0 || dy == 4) {
          // 30% chance to be missing (broken)
          if (random.nextFloat() > 0.3f) {
            // 20% chance to be crying obsidian
            BlockState frameBlock = random.nextFloat() < 0.2f ?
                Blocks.CRYING_OBSIDIAN.defaultBlockState() :
                Blocks.OBSIDIAN.defaultBlockState();
            world.setBlock(framePos, frameBlock, 2);
          }
        }
      }
    }

    // Scatter netherrack and magma blocks around
    for (int i = 0; i < 15; i++) {
      int dx = random.nextInt(10) - 5;
      int dy = random.nextInt(4) - 1;
      int dz = random.nextInt(10) - 5;

      BlockPos scatterPos = pos.offset(dx, dy, dz);

      if (world.getBlockState(scatterPos).isAir() || world.getBlockState(scatterPos).is(Blocks.GRASS_BLOCK)) {
        BlockState scatterBlock = random.nextFloat() < 0.7f ?
            Blocks.NETHERRACK.defaultBlockState() :
            Blocks.MAGMA_BLOCK.defaultBlockState();
        world.setBlock(scatterPos, scatterBlock, 2);
      }
    }

    // Small chance for gold blocks
    if (random.nextFloat() < 0.3f) {
      int dx = random.nextInt(6) - 3;
      int dy = random.nextInt(3) - 1;
      int dz = random.nextInt(6) - 3;

      BlockPos goldPos = pos.offset(dx, dy, dz);
      world.setBlock(goldPos, Blocks.GOLD_BLOCK.defaultBlockState(), 2);
    }

    // Add loot chest nearby
    if (random.nextFloat() < 0.8f) {
      int dx = random.nextInt(8) - 4;
      int dz = random.nextInt(8) - 4;

      int chestY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
          pos.getX() + dx, pos.getZ() + dz);

      BlockPos chestPos = new BlockPos(pos.getX() + dx, chestY, pos.getZ() + dz);

      // Bury chest partially
      BlockPos buriedChestPos = chestPos.below();

      // Only place if position is valid
      if (world.getBlockState(buriedChestPos).isAir() && world.getBlockState(buriedChestPos.below()).isSolid()) {
        world.setBlock(buriedChestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(buriedChestPos);
        if (be instanceof ChestBlockEntity chest) {
          chest.setLootTable(BuiltInLootTables.RUINED_PORTAL, random.nextLong());
        }

        // Cover with netherrack
        world.setBlock(chestPos, Blocks.NETHERRACK.defaultBlockState(), 2);
      }
    }

    // Add some fire
    if (random.nextFloat() < 0.5f) {
      for (int i = 0; i < 3; i++) {
        int dx = random.nextInt(8) - 4;
        int dz = random.nextInt(8) - 4;

        int fireY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
            pos.getX() + dx, pos.getZ() + dz);

        BlockPos firePos = new BlockPos(pos.getX() + dx, fireY, pos.getZ() + dz);

        if (world.getBlockState(firePos).isAir() && world.getBlockState(firePos.below()).isSolid()) {
          world.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 2);
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:ruined_portals";
  }
}
