package art.arcane.mystcraft.world.structure;

import art.arcane.mystcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Programmatic builder for an archivist house structure. Generates a 9x7x12
 * cobblestone building with bookshelves, lecterns, and a writing desk. Used by
 * VillageStructureHandler to build houses in villages.
 */
public final class ArchivistHouseStructurePiece {

  private static final int WIDTH = 9;
  private static final int HEIGHT = 7;
  private static final int DEPTH = 12;

  private ArchivistHouseStructurePiece() {
  }

  /**
   * Builds the archivist house at the given position.
   *
   * @param level  The server level
   * @param origin The bottom-front-left corner of the house
   * @param facing The direction the front door faces
   * @param random Random source for variation
   */
  public static void build(ServerLevel level, BlockPos origin, Direction facing, RandomSource random) {

    for (int x = 0; x < WIDTH; x++) {
      for (int z = 0; z < DEPTH; z++) {
        for (int y = -1; y < HEIGHT; y++) {
          BlockPos pos = origin.offset(x, y, z);
          BlockState state = getBlockAt(x, y, z, random);
          if (state != null) {
            level.setBlock(pos, state, 3);
          }
        }
      }
    }
  }

  private static BlockState getBlockAt(int x, int y, int z, RandomSource random) {

    if (y == -1) {
      return Blocks.COBBLESTONE.defaultBlockState();
    }

    if (y == 0) {
      if (x >= 1 && x <= WIDTH - 2 && z >= 1 && z <= DEPTH - 2) {
        return Blocks.OAK_PLANKS.defaultBlockState();
      }
      return Blocks.COBBLESTONE.defaultBlockState();
    }

    if (y == HEIGHT - 1) {
      return Blocks.OAK_PLANKS.defaultBlockState();
    }

    boolean isWall = x == 0 || x == WIDTH - 1 || z == 0 || z == DEPTH - 1;

    if (z == 0 && x == 4 && (y == 1 || y == 2)) {
      return Blocks.AIR.defaultBlockState();
    }

    if (y == 2) {
      if (z == 0 && (x == 2 || x == 6))
        return Blocks.GLASS_PANE.defaultBlockState();
      if ((x == 0 || x == WIDTH - 1) && (z == 4 || z == 8))
        return Blocks.GLASS_PANE.defaultBlockState();
    }

    if (isWall) {
      return Blocks.COBBLESTONE.defaultBlockState();
    }

    if (z == DEPTH - 2 && x >= 1 && x <= WIDTH - 2 && (y == 1 || y == 2)) {
      return Blocks.BOOKSHELF.defaultBlockState();
    }

    if ((x == 1 || x == WIDTH - 2) && (z == 3 || z == 5) && (y == 1 || y == 2)) {
      return Blocks.BOOKSHELF.defaultBlockState();
    }

    if (y == 1 && (x == 3 || x == 5) && z == 6) {
      return Blocks.LECTERN.defaultBlockState();
    }

    if (y == 1 && x == 4 && z == 9 && ModBlocks.WRITING_DESK != null) {
      return ModBlocks.WRITING_DESK.get().defaultBlockState();
    }

    if (y == 3 && ((x == 3 && z == 3) || (x == 5 && z == 9))) {
      return Blocks.LANTERN.defaultBlockState();
    }

    if (y == 1 && x == 4 && z >= 2 && z <= 4) {
      return Blocks.RED_CARPET.defaultBlockState();
    }

    return Blocks.AIR.defaultBlockState();
  }
}
