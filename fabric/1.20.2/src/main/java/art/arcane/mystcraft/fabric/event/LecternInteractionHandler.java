package art.arcane.mystcraft.fabric.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

import java.lang.reflect.Field;

/**
 * Handles Mystcraft book interactions with vanilla lecterns using Fabric events.
 */
public class LecternInteractionHandler {

  private static Field bookField;
  private static Field pageCountField;

  static {
    try {
      bookField = LecternBlockEntity.class.getDeclaredField("book");
      bookField.setAccessible(true);
      pageCountField = LecternBlockEntity.class.getDeclaredField("pageCount");
      pageCountField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      Mystcraft.LOGGER.error("[LecternHandler] Failed to find LecternBlockEntity fields", e);
    }
  }

  public static void register() {
    UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
      var pos = hitResult.getBlockPos();
      var state = world.getBlockState(pos);

      // Only handle vanilla lecterns
      if (!(state.getBlock() instanceof LecternBlock)) {
        return InteractionResult.PASS;
      }

      var result = MystcraftLecternHelper.handleLecternInteraction(
          world, pos, state, player, hand,
          (lectern, book, pageCount) -> {
            try {
              if (bookField != null && pageCountField != null) {
                bookField.set(lectern, book);
                pageCountField.set(lectern, pageCount);
              }
            } catch (Exception e) {
              Mystcraft.LOGGER.error("[LecternHandler] Failed to set book fields", e);
            }
          });

      return result.handled ? result.result : InteractionResult.PASS;
    });

    Mystcraft.LOGGER.info("[LecternHandler] Registered Fabric lectern interaction handler");
  }
}
