package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.util.TooltipCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * A blank linkbook that converts to a linked linkbook at the current position
 * on right-click. Only converts when exactly 1 is held. Transfers link panel
 * properties to the new book.
 */
public class LinkbookUnlinkedItem extends Item implements TooltipCompat {

  public LinkbookUnlinkedItem(Properties properties) {
    super(properties.stacksTo(16));
  }

  /**
   * Creates an unlinked book with a link panel's properties.
   */
  public static ItemStack createItem(@NotNull ItemStack linkpanel, @NotNull ItemStack covermat) {
    ItemStack linkbook = new ItemStack(ModItems.LINKBOOK_UNLINKED.get());
    CompoundTag prev = ItemStackNbt.getTag(linkpanel);
    if (prev == null) {
      prev = new CompoundTag();
    }
    ItemStackNbt.setTag(linkbook, prev.copy());
    return linkbook;
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                              @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltip,
                              @NotNull TooltipFlag flag) {

    if (ItemStackNbt.getTag(stack) != null) {
      List<Component> lines = new java.util.ArrayList<>();
      Page.getTooltip(stack, lines);
      lines.forEach(tooltip);
    }
  }

  @Override
  @NotNull
  public InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack inHand = player.getItemInHand(hand);

    if (level.isClientSide() || inHand.getCount() > 1) {
      return InteractionResult.PASS;
    }

    ItemStack linkBook = new ItemStack(ModItems.LINKBOOK.get());

    initializeLinkbook(linkBook, level, player);
    Page.applyLinkPanel(inHand, linkBook);
    player.setItemInHand(hand, linkBook);
    inHand.setCount(0);

    return InteractionResult.SUCCESS.heldItemTransformedTo(linkBook);
  }

  private void initializeLinkbook(ItemStack linkBook, Level level, Player player) {
    CompoundTag tag = new CompoundTag();

    LinkOptions.setSpawn(tag, player.blockPosition());
    LinkOptions.setSpawnYaw(tag, player.getYRot());

    int dimId = LinkingManager.getDimensionUID(level);
    LinkOptions.setDimensionUID(tag, dimId);

    String dimName = getDimensionDisplayName(level);
    LinkOptions.setDisplayName(tag, dimName);

    ItemStackNbt.setTag(linkBook, tag);
  }

  private String getDimensionDisplayName(Level level) {
    ResourceKey<Level> dimension = level.dimension();
    if (dimension == Level.OVERWORLD) {
      return "Overworld";
    } else if (dimension == Level.NETHER) {
      return "The Nether";
    } else if (dimension == Level.END) {
      return "The End";
    }

    return dimension.identifier().getPath();
  }

  public boolean hasCustomEntity(@NotNull ItemStack stack) {
    return true;
  }

  @Nullable
  public net.minecraft.world.entity.Entity createEntity(Level level, net.minecraft.world.entity.Entity location, @NotNull ItemStack stack) {
    art.arcane.mystcraft.entity.LinkbookEntity entity = new art.arcane.mystcraft.entity.LinkbookEntity(level, location.getX(), location.getY(), location.getZ());
    entity.setBookItem(stack.copy());
    entity.setDeltaMovement(location.getDeltaMovement());
    return entity;
  }
}
