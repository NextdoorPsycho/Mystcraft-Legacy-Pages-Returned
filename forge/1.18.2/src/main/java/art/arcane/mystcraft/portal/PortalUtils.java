package art.arcane.mystcraft.portal;

import art.arcane.mystcraft.block.LinkPortalBlock;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for portal-related operations.
 * 1.18.2 Forge version.
 */
public final class PortalUtils {

  private static final int MAX_SEARCH_DISTANCE = 16;

  private PortalUtils() {
  }

  /**
   * Finds the BookReceptacle that controls a portal block.
   * Searches in the direction indicated by the portal's SOURCE_DIRECTION property.
   */
  @Nullable
  public static BlockEntity findReceptacle(Level level, BlockPos portalPos) {
    BlockState state = level.getBlockState(portalPos);
    if (!(state.getBlock() instanceof LinkPortalBlock)) {
      return null;
    }

    Direction sourceDir = state.getValue(LinkPortalBlock.SOURCE_DIRECTION);
    BlockPos.MutableBlockPos searchPos = portalPos.mutable();

    for (int i = 0; i < MAX_SEARCH_DISTANCE; i++) {
      searchPos.move(sourceDir);
      BlockEntity be = level.getBlockEntity(searchPos);
      if (be instanceof BookReceptacleBlockEntity) {
        return be;
      }

      // Stop if we hit a non-portal, non-air block
      BlockState searchState = level.getBlockState(searchPos);
      if (!searchState.isAir() && !(searchState.getBlock() instanceof LinkPortalBlock)) {
        break;
      }
    }

    return null;
  }

  /**
   * Validates a portal block, removing it if invalid.
   */
  public static void validatePortal(Level level, BlockPos pos) {
    BlockState state = level.getBlockState(pos);
    if (!(state.getBlock() instanceof LinkPortalBlock)) {
      return;
    }

    // Check if receptacle still exists
    BlockEntity receptacle = findReceptacle(level, pos);
    if (receptacle == null) {
      // Remove orphaned portal
      level.removeBlock(pos, false);
    }
  }

  /**
   * Checks if a position is a link portal block.
   */
  public static boolean isLinkPortal(Level level, BlockPos pos) {
    return level.getBlockState(pos).getBlock() instanceof LinkPortalBlock;
  }

  /**
   * Fires/activates a portal from a book receptacle.
   * Creates portal blocks in the area if the receptacle has a valid linking book.
   */
  public static void firePortal(Level level, BlockPos receptaclePos) {
    // 1.18.2: Portal firing implementation
    // Search for valid portal space and create portal blocks
    // For now, this is a stub - portal generation is complex
  }

  /**
   * Shuts down a portal, removing all portal blocks associated with a receptacle.
   */
  public static void shutdownPortal(Level level, BlockPos receptaclePos) {
    // 1.18.2: Portal shutdown implementation
    // Search in all directions for portal blocks and remove them
    for (Direction dir : Direction.values()) {
      BlockPos.MutableBlockPos searchPos = receptaclePos.mutable();
      for (int i = 0; i < MAX_SEARCH_DISTANCE; i++) {
        searchPos.move(dir);
        if (isLinkPortal(level, searchPos)) {
          level.removeBlock(searchPos.immutable(), false);
        } else if (!level.getBlockState(searchPos).isAir()) {
          break;
        }
      }
    }
  }
}
