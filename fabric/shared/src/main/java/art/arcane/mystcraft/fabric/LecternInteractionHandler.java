package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.util.MystcraftLecternHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Handles Mystcraft book interactions with vanilla lecterns using Fabric
 * events.
 */
public class LecternInteractionHandler {

  public static void register() {
    UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
      BlockPos pos = hitResult.getBlockPos();
      BlockState state = world.getBlockState(pos);

      if (!(state.getBlock() instanceof LecternBlock)) {
        return InteractionResult.PASS;
      }

      MystcraftLecternHelper.LecternInteractionResult result = MystcraftLecternHelper.handleLecternInteraction(
          world, pos, state, player, hand,
          (lectern, book, pageCount) -> {
            lectern.book = book;
            lectern.pageCount = pageCount;
          });

      return result.handled ? result.result : InteractionResult.PASS;
    });

  }
}
