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

import java.lang.reflect.Method;


/**
 * Helper utilities for Mystcraft book integration with vanilla Lecterns.
 * Contains cross-platform logic for lectern interactions.
 */
public final class MystcraftLecternHelper {

  private MystcraftLecternHelper() {
  }

  /**
   * Checks if the given ItemStack is a Mystcraft book (LinkbookItem or AgebookItem).
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

  private static Method openForBlockMethod;

  /**
   * Opens the book screen for a book placed on a block (lectern/bookstand).
   * Must be called on the client side only.
   * Uses reflection to avoid loading client classes on the server.
   *
   * @param book     the book ItemStack
   * @param blockPos the position of the block holding the book
   */
  public static void openBookScreenForBlock(ItemStack book, BlockPos blockPos) {
    try {
      if (openForBlockMethod == null) {
        Class<?> bookScreenClass = Class.forName("art.arcane.mystcraft.client.screen.BookScreen");
        openForBlockMethod = bookScreenClass.getMethod("openForBlock", ItemStack.class, BlockPos.class);
      }
      openForBlockMethod.invoke(null, book, blockPos);
    } catch (ReflectiveOperationException e) {
      Mystcraft.LOGGER.error("[LecternHelper] Failed to open book screen", e);
    }
  }

  /**
   * Handles lectern interaction for Mystcraft books.
   * This is the cross-platform logic called by platform-specific event handlers.
   *
   * @param level           the level
   * @param pos             the lectern position
   * @param state           the block state
   * @param player          the player
   * @param hand            the interaction hand
   * @param bookFieldSetter a function to set the book and pageCount fields (platform-specific)
   * @return the interaction result
   */
  public static LecternInteractionResult handleLecternInteraction(
      Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand,
      LecternBookSetter bookFieldSetter) {

    // Only handle vanilla lecterns
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

    // Handle Mystcraft book already on lectern (server knows about it)
    if (isMystcraftBook(bookOnLectern)) {
      if (player.isShiftKeyDown() && held.isEmpty()) {
        // Shift + empty hand = pick up book
        if (!level.isClientSide) {
          player.setItemInHand(hand, bookOnLectern.copy());
          lectern.clearContent();
          LecternBlock.resetBookState(player, level, pos, state, false);
        }
        return LecternInteractionResult.success(level.isClientSide);
      }

      // Server: send packet to open book screen (includes book data for sync)
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
        Mystcraft.LOGGER.debug("[LecternHelper] Sending OpenLecternBookPacket to {} for pos={}", serverPlayer.getName().getString(), pos);
        MystcraftNetwork.sendToPlayer(new OpenLecternBookPacket(pos, bookOnLectern), serverPlayer);
      }
      return LecternInteractionResult.success(level.isClientSide);
    }

    // Client: HAS_BOOK is true but we don't see a valid book - likely a Mystcraft book that hasn't synced yet
    // Consume the interaction; server will send OpenLecternBookPacket with the book data
    if (level.isClientSide && state.getValue(LecternBlock.HAS_BOOK) && bookOnLectern.isEmpty()) {
      return LecternInteractionResult.success(true);
    }

    // Handle placing Mystcraft book on empty lectern
    if (!state.getValue(LecternBlock.HAS_BOOK) && isMystcraftBook(held)) {
      if (!level.isClientSide) {
        ItemStack bookCopy = held.copy();
        bookCopy.setCount(1);

        // Use platform-specific setter to set the book field
        bookFieldSetter.setBook(lectern, bookCopy, 1);

        // Mark dirty and update block state
        lectern.setChanged();

        // Update block state to HAS_BOOK=true
        BlockState newState = state.setValue(LecternBlock.HAS_BOOK, true);
        level.setBlock(pos, newState, 3);

        // Send custom packet to sync book data to clients
        if (level instanceof ServerLevel serverLevel) {
          MystcraftNetwork.sendToTrackingBlock(
              new LecternBookSyncPacket(pos, bookCopy),
              serverLevel,
              pos
          );
        }

        held.shrink(1);
      }
      return LecternInteractionResult.success(level.isClientSide);
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
      return new LecternInteractionResult(true, InteractionResult.sidedSuccess(clientSide));
    }
  }
}
