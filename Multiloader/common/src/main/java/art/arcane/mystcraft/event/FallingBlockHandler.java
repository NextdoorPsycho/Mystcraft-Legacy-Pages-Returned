package art.arcane.mystcraft.event;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Prevents gravity blocks from falling in Mystcraft Ages when disabled by config.
 */
public final class FallingBlockHandler {

  private FallingBlockHandler() {
  }

  /**
   * Returns true if the falling block was prevented and removed.
   */
  public static boolean handle(ServerLevel level, FallingBlockEntity entity) {
    if (MystcraftConfig.allowGravityBlocksInAges.get()) {
      return false;
    }
    if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
      return false;
    }

    BlockState state = entity.getBlockState();
    if (state != null) {
      BlockPos pos = entity.blockPosition();
      if (level.getBlockState(pos).isAir()) {
        level.setBlock(pos, state, 3);
      }
    }

    entity.discard();
    return true;
  }
}
