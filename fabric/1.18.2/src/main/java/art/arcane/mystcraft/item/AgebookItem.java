package art.arcane.mystcraft.item;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.link.LinkingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The Descriptive Book (Agebook) item.
 * Contains the complete description of an Age and allows travel to it.
 * <p>
 * 1.18.2 version - uses TextComponent/TranslatableComponent instead of Component.literal()/translatable().
 */
public class AgebookItem extends Item {

  private static final String TAG_PAGES = "Pages";
  private static final String TAG_AUTHORS = "Authors";

  public AgebookItem(Properties properties) {
    super(properties);
  }

  /**
   * Creates an Agebook from pages.
   */
  public static void create(ItemStack agebook, Player player, List<ItemStack> pages, String title) {
    agebook.setTag(new CompoundTag());

    AgebookItem item = (AgebookItem) agebook.getItem();
    item.addPages(agebook, pages);
    item.addAuthor(agebook, player);
    item.setDisplayName(agebook, title);

    if (!pages.isEmpty()) {
      ItemStack linkpanel = pages.get(0);
      if (Page.isLinkPanel(linkpanel)) {
        Page.applyLinkPanel(linkpanel, agebook);
      }
    }
  }

  /**
   * Checks if this is a new (unlinked) Agebook.
   */
  public static boolean isNewAgebook(ItemStack stack) {
    if (!(stack.getItem() instanceof AgebookItem)) {
      return false;
    }
    if (stack.getTag() == null) {
      return false;
    }
    Integer dimId = LinkOptions.getDimensionUID(stack.getTag());
    if (dimId != null) {
      return false;
    }
    List<ItemStack> pages = ((AgebookItem) stack.getItem()).getPageList(stack);
    return !pages.isEmpty() && Page.isLinkPanel(pages.get(0));
  }

  @Override
  @NotNull
  public Rarity getRarity(@NotNull ItemStack stack) {
    return stack.isEnchanted() ? Rarity.RARE : Rarity.EPIC;
  }

  @Override
  @NotNull
  public Component getName(@NotNull ItemStack stack) {
    if (stack.getTag() != null) {
      String displayName = LinkOptions.getDisplayName(stack.getTag());
      if (!"???".equals(displayName)) {
        return new TextComponent(displayName);
      }
    }
    return super.getName(stack);
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
    if (stack.getTag() != null) {
      Integer dimId = LinkOptions.getDimensionUID(stack.getTag());
      if (dimId != null) {
        tooltip.add(new TranslatableComponent("item.mystcraft.agebook.age", dimId));
      } else {
        tooltip.add(new TranslatableComponent("item.mystcraft.agebook.unwritten"));
      }

      Collection<String> authors = getAuthors(stack);
      if (!authors.isEmpty()) {
        tooltip.add(new TranslatableComponent("item.mystcraft.agebook.authors",
            String.join(", ", authors)));
      }

      List<ItemStack> pages = getPageList(stack);
      if (!pages.isEmpty()) {
        tooltip.add(new TranslatableComponent("item.mystcraft.agebook.pages", pages.size()));
      }
    }
  }

  /**
   * Right-click always opens the book GUI, matching LinkbookItem behavior.
   * Actual linking happens via packet from the GUI's Link button.
   */
  @Override
  @NotNull
  public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    // Always open GUI on client (linking happens via packet from Link button)
    if (level.isClientSide) {
      art.arcane.mystcraft.client.screen.BookScreen.open(stack);
    }

    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
  }

  /**
   * Activates the book, potentially creating a new Age.
   * Called from packet handler when player clicks Link button in GUI.
   */
  public void activate(ItemStack stack, Level level, Entity entity) {
    if (!(entity instanceof Player player)) {
      return;
    }
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }
    if (!(player instanceof ServerPlayer serverPlayer)) {
      return;
    }

    if (stack.getTag() == null) {
      stack.setTag(new CompoundTag());
    }

    Integer dimId = LinkOptions.getDimensionUID(stack.getTag());

    if (dimId == null) {
      // This is a new book - check if it has a link panel
      List<ItemStack> pages = getPageList(stack);
      if (!pages.isEmpty() && Page.isLinkPanel(pages.get(0))) {
        // TODO: Create Age when AgeDimensionFactory is ported to 1.18.2
        Mystcraft.LOGGER.info("Age creation not yet implemented for 1.18.2");
      }
    } else {
      // Existing Age - perform linking
      linkToAge(stack, serverLevel, serverPlayer);
    }
  }

  /**
   * Links the player to the Age described in this book.
   */
  private void linkToAge(ItemStack stack, ServerLevel level, ServerPlayer player) {
    CompoundTag linkData = stack.getTag();
    if (linkData == null) {
      return;
    }

    Integer ageUID = LinkOptions.getDimensionUID(linkData);
    if (ageUID == null) {
      Mystcraft.LOGGER.warn("Agebook link failed: no age UID for player {}", player.getGameProfile().getName());
      return;
    }

    // Perform the link
    LinkingManager.LinkResult result = LinkingManager.performLink(player, linkData);

    if (result != LinkingManager.LinkResult.SUCCESS) {
      Mystcraft.LOGGER.warn("Agebook link failed: result={} player={}", result.name(),
          player.getGameProfile().getName());
    }
  }

  /**
   * Gets the list of pages in this book.
   */
  public List<ItemStack> getPageList(ItemStack stack) {
    if (stack.getTag() == null) {
      return Collections.emptyList();
    }
    CompoundTag tag = stack.getTag();
    ListTag listTag = tag.getList(TAG_PAGES, Tag.TAG_COMPOUND);
    List<ItemStack> pages = new ArrayList<>();
    for (int i = 0; i < listTag.size(); i++) {
      pages.add(ItemStack.of(listTag.getCompound(i)));
    }
    return pages;
  }

  /**
   * Adds pages to this book.
   */
  public void addPages(ItemStack stack, Collection<ItemStack> pages) {
    CompoundTag tag = stack.getOrCreateTag();
    ListTag listTag = tag.getList(TAG_PAGES, Tag.TAG_COMPOUND);
    for (ItemStack page : pages) {
      listTag.add(page.save(new CompoundTag()));
    }
    tag.put(TAG_PAGES, listTag);
  }

  /**
   * Sets the page list.
   */
  public void setPageList(ItemStack stack, List<ItemStack> pages) {
    CompoundTag tag = stack.getOrCreateTag();
    ListTag listTag = new ListTag();
    for (ItemStack page : pages) {
      listTag.add(page.save(new CompoundTag()));
    }
    tag.put(TAG_PAGES, listTag);
  }

  /**
   * Adds an author to this book.
   */
  public void addAuthor(ItemStack stack, Player player) {
    CompoundTag tag = stack.getOrCreateTag();
    ListTag listTag = tag.getList(TAG_AUTHORS, Tag.TAG_STRING);
    String playerName = player.getGameProfile().getName();
    boolean found = false;
    for (int i = 0; i < listTag.size(); i++) {
      if (listTag.getString(i).equals(playerName)) {
        found = true;
        break;
      }
    }
    if (!found) {
      listTag.add(net.minecraft.nbt.StringTag.valueOf(playerName));
      tag.put(TAG_AUTHORS, listTag);
    }
  }

  /**
   * Gets the authors of this book.
   */
  public Collection<String> getAuthors(ItemStack stack) {
    if (stack.getTag() == null) {
      return Collections.emptyList();
    }
    CompoundTag tag = stack.getTag();
    ListTag listTag = tag.getList(TAG_AUTHORS, Tag.TAG_STRING);
    List<String> authors = new ArrayList<>();
    for (int i = 0; i < listTag.size(); i++) {
      authors.add(listTag.getString(i));
    }
    return authors;
  }

  /**
   * Sets the display name of the book.
   */
  public void setDisplayName(ItemStack stack, String name) {
    LinkOptions.setDisplayName(stack.getOrCreateTag(), name);
  }

  /**
   * Gets the display name of the book.
   */
  public String getDisplayName(ItemStack stack) {
    return LinkOptions.getDisplayName(stack.getTag());
  }

  /**
   * Agebooks with a linked Age have a foil effect to show they're active.
   */
  @Override
  public boolean isFoil(@NotNull ItemStack stack) {
    // Show foil if the book has an Age (dimension) linked
    return stack.getTag() != null && LinkOptions.getDimensionUID(stack.getTag()) != null;
  }

  public boolean hasCustomEntity(@NotNull ItemStack stack) {
    return MystcraftConfig.droppedBooksBecomeLivingEntities.get();
  }

  @Nullable
  public Entity createEntity(Level level, Entity location, @NotNull ItemStack stack) {
    // TODO: Create LinkbookEntity when entity class is ported to 1.18.2
    return null;
  }
}
