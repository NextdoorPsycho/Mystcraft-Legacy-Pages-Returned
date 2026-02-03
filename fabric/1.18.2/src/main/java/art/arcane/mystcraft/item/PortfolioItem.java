package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.Page;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Portfolio item - an UNORDERED, COLLECTION-BASED page archive.
 * <p>
 * 1.18.2 version - uses TranslatableComponent instead of Component.translatable().
 */
public class PortfolioItem extends Item {

  public static final int MAX_PAGES = 64;
  private static final String TAG_PAGES = "Pages";

  public PortfolioItem(Properties properties) {
    super(properties.stacksTo(1));
  }

  /**
   * Gets all pages in this portfolio.
   */
  public static List<ItemStack> getPages(ItemStack stack) {
    if (stack.getTag() == null) {
      return new ArrayList<>();
    }
    CompoundTag tag = stack.getTag();
    ListTag listTag = tag.getList(TAG_PAGES, Tag.TAG_COMPOUND);
    List<ItemStack> pages = new ArrayList<>();
    for (int i = 0; i < listTag.size(); i++) {
      ItemStack page = ItemStack.of(listTag.getCompound(i));
      if (!page.isEmpty()) {
        pages.add(page);
      }
    }
    return pages;
  }

  /**
   * Adds a page to this portfolio.
   */
  public static boolean addPage(ItemStack portfolio, ItemStack page) {
    if (portfolio.getTag() == null) {
      portfolio.setTag(new CompoundTag());
    }
    List<ItemStack> pages = getPages(portfolio);
    if (pages.size() >= MAX_PAGES) {
      return false;
    }
    pages.add(page.copy());
    setPages(portfolio, pages);
    return true;
  }

  /**
   * Removes a page from this portfolio.
   */
  public static ItemStack removePage(ItemStack portfolio, int index) {
    List<ItemStack> pages = getPages(portfolio);
    if (index < 0 || index >= pages.size()) {
      return ItemStack.EMPTY;
    }
    ItemStack removed = pages.remove(index);
    setPages(portfolio, pages);
    return removed;
  }

  /**
   * Sets all pages in this portfolio.
   */
  public static void setPages(ItemStack portfolio, List<ItemStack> pages) {
    CompoundTag tag = portfolio.getOrCreateTag();
    ListTag listTag = new ListTag();
    for (ItemStack page : pages) {
      if (!page.isEmpty()) {
        listTag.add(page.save(new CompoundTag()));
      }
    }
    tag.put(TAG_PAGES, listTag);
  }

  /**
   * Sorts pages in this portfolio by category and name.
   */
  public static void sortPages(ItemStack portfolio) {
    List<ItemStack> pages = getPages(portfolio);
    // TODO: Implement sorting when SymbolRegistry is available
    setPages(portfolio, pages);
  }

  /**
   * Checks if this portfolio is full.
   */
  public static boolean isFull(ItemStack portfolio) {
    return getPages(portfolio).size() >= MAX_PAGES;
  }

  /**
   * Gets the number of pages in this portfolio.
   */
  public static int getPageCount(ItemStack portfolio) {
    return getPages(portfolio).size();
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
    List<ItemStack> pages = getPages(stack);
    if (!pages.isEmpty()) {
      tooltip.add(new TranslatableComponent("item.mystcraft.portfolio.pages", pages.size(), MAX_PAGES));
    }
  }

  @Override
  @NotNull
  public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (level.isClientSide) {
      // TODO: Open portfolio GUI when PortfolioMenu is ported to 1.18.2
      return InteractionResultHolder.success(stack);
    }

    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
  }
}
