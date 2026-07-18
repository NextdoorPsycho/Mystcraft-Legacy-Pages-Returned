package art.arcane.mystcraft.util;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.network.LecternBookSyncPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.network.OpenLecternBookPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Helper utilities for Mystcraft book integration with vanilla Lecterns.
 * Contains cross-platform logic for lectern interactions.
 */
public final class MystcraftLecternHelper {

  private static volatile BiConsumer<ItemStack, BlockPos> clientBookOpener =
      (book, blockPos) -> {
      };

  private MystcraftLecternHelper() {
  }

  /**
   * Checks if the given ItemStack is a Mystcraft book (LinkbookItem or
   * AgebookItem).
   *
   * @param stack the ItemStack to check
   * @return true if the item is a Mystcraft book
   */
  public static boolean isMystcraftBook(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }
    return stack.getItem() instanceof LinkbookItem || stack.getItem() instanceof AgebookItem;
  }

  /**
   * Opens the book screen for a book placed on a vanilla Lectern. Must be
   * called on the client side only. The loader installs the direct screen
   * method reference without exposing client classes to a dedicated server.
   *
   * @param book     the book ItemStack
   * @param blockPos the position of the block holding the book
   */
  public static void openBookScreenForBlock(ItemStack book, BlockPos blockPos) {
    clientBookOpener.accept(book, blockPos);
  }

  /** Installs the client-only lectern book screen opener during client setup. */
  public static void setClientBookOpener(BiConsumer<ItemStack, BlockPos> opener) {
    clientBookOpener = Objects.requireNonNull(opener, "opener");
  }

  /**
   * Handles lectern interaction for Mystcraft books. This is the cross-platform
   * logic called by platform-specific event handlers.
   *
   * @param level           the level
   * @param pos             the lectern position
   * @param state           the block state
   * @param player          the player
   * @param hand            the interaction hand
   * @param bookFieldSetter a function to set the book and pageCount fields
   *                        (platform-specific)
   * @return the interaction result
   */
  public static LecternInteractionResult handleLecternInteraction(
      Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand,
      LecternBookSetter bookFieldSetter) {

    if (!(state.getBlock() instanceof LecternBlock)) {
      return LecternInteractionResult.pass();
    }

    if (!(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)) {
      return LecternInteractionResult.pass();
    }

    ItemStack held = player.getItemInHand(hand);
    ItemStack bookOnLectern = lectern.getBook();

    Mystcraft.LOGGER.debug("[LecternHelper] bookOnLectern={}, isMystcraftBook={}, hasBook={}",
        bookOnLectern, isMystcraftBook(bookOnLectern), state.getValue(LecternBlock.HAS_BOOK));

    if (isMystcraftBook(bookOnLectern)) {
      if (player.isShiftKeyDown() && held.isEmpty()) {

        if (!level.isClientSide()) {
          player.setItemInHand(hand, bookOnLectern.copy());
          lectern.clearContent();
          LecternBlock.resetBookState(player, level, pos, state, false);
        }
        return LecternInteractionResult.success(level.isClientSide());
      }

      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
        Mystcraft.LOGGER.debug("[LecternHelper] Sending OpenLecternBookPacket to {} for pos={}", serverPlayer.getName().getString(), pos);
        MystcraftNetwork.sendToPlayer(new OpenLecternBookPacket(pos, bookOnLectern), serverPlayer);
      }
      return LecternInteractionResult.success(level.isClientSide());
    }

    if (level.isClientSide() && state.getValue(LecternBlock.HAS_BOOK) && bookOnLectern.isEmpty()) {
      return LecternInteractionResult.success(true);
    }

    if (!state.getValue(LecternBlock.HAS_BOOK) && isMystcraftBook(held)) {
      if (!level.isClientSide()) {
        ItemStack bookCopy = held.copy();
        bookCopy.setCount(1);

        bookFieldSetter.setBook(lectern, bookCopy, 1);

        lectern.setChanged();

        BlockState newState = state.setValue(LecternBlock.HAS_BOOK, true);
        level.setBlock(pos, newState, 3);

        if (level instanceof ServerLevel serverLevel) {
          MystcraftNetwork.sendToTrackingBlock(
              new LecternBookSyncPacket(pos, bookCopy),
              serverLevel,
              pos
          );
        }

        held.shrink(1);
      }
      return LecternInteractionResult.success(level.isClientSide());
    }

    return LecternInteractionResult.pass();
  }

  /**
   * Functional interface for setting book fields in a lectern.
   */
  @FunctionalInterface
  public interface LecternBookSetter {
    void setBook(LecternBlockEntity lectern, ItemStack book, int pageCount);
  }

  /**
   * Result of handling a lectern interaction.
   */
  public static class LecternInteractionResult {
    public final boolean handled;
    public final InteractionResult result;

    public LecternInteractionResult(boolean handled, InteractionResult result) {
      this.handled = handled;
      this.result = result;
    }

    public static LecternInteractionResult pass() {
      return new LecternInteractionResult(false, InteractionResult.PASS);
    }

    public static LecternInteractionResult success(boolean clientSide) {
      return new LecternInteractionResult(true, InteractionResult.SUCCESS);
    }
  }
}
