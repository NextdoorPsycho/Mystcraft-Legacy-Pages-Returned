package art.arcane.mystcraft.data;

import art.arcane.mystcraft.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import art.arcane.mystcraft.util.ItemStackNbt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for working with Page item data.
 * Handles symbol pages, link panels, and page properties.
 */
public abstract class Page {

  private static final String TAG_SYMBOL = "symbol";
  private static final String TAG_LINK_PANEL = "linkpanel";
  private static final String TAG_PROPERTIES = "properties";
  private static final String TAG_QUALITY = "Quality";
  private static final String TAG_INK_TINT = "InkTint";
  private static final String TAG_AFFINITY = "Affinity";

  public static void setQuality(@NotNull ItemStack page, String trait, int quality) {
    getQualityStruct(page).putInt(trait, quality);
  }

  public static int getTotalQuality(@NotNull ItemStack page) {
    CompoundTag compound = getQualityStruct(page);
    int sum = 0;
    for (String tagname : compound.getAllKeys()) {
      sum += compound.getInt(tagname);
    }
    return sum;
  }

  @Nullable
  public static Integer getQuality(@NotNull ItemStack page, String trait) {
    CompoundTag data = getQualityStruct(page);
    if (data.contains(trait)) {
      return data.getInt(trait);
    }
    return null;
  }

  @NotNull
  private static CompoundTag getData(@NotNull ItemStack item) {
    if (item.isEmpty()) {
      return new CompoundTag();
    }
    CompoundTag tag = ItemStackNbt.getTag(item);
    if (tag == null) {
      tag = new CompoundTag();
      ItemStackNbt.setTag(item, tag);
    }
    return tag;
  }

  @NotNull
  private static CompoundTag getQualityStruct(@NotNull ItemStack page) {
    CompoundTag data = getData(page);
    if (!data.contains(TAG_QUALITY)) {
      data.put(TAG_QUALITY, new CompoundTag());
    }
    return data.getCompound(TAG_QUALITY);
  }

  /**
   * Checks if the page is blank (no symbol and not a link panel).
   */
  public static boolean isBlank(@NotNull ItemStack page) {
    return !isLinkPanel(page) && !getData(page).contains(TAG_SYMBOL);
  }

  /**
   * Checks if the page is a link panel.
   */
  public static boolean isLinkPanel(@NotNull ItemStack page) {
    return getData(page).contains(TAG_LINK_PANEL);
  }

  /**
   * Makes the page a link panel.
   */
  public static void makeLinkPanel(@NotNull ItemStack page) {
    if (page.isEmpty()) {
      return;
    }

    if (ItemStackNbt.getTag(page) == null) {
      ItemStackNbt.setTag(page, createDefault());
    }
    CompoundTag data = getData(page);
    if (!data.contains(TAG_LINK_PANEL)) {
      data.put(TAG_LINK_PANEL, new CompoundTag());
    }
  }

  /**
   * Adds a link property to a link panel page.
   */
  public static void addLinkProperty(@NotNull ItemStack page, String linkproperty) {
    if (page.isEmpty()) {
      return;
    }

    if (ItemStackNbt.getTag(page) == null) {
      ItemStackNbt.setTag(page, createDefault());
    }
    CompoundTag data = getData(page);
    if (!data.contains(TAG_LINK_PANEL)) {
      data.put(TAG_LINK_PANEL, new CompoundTag());
    }
    CompoundTag linkpanel = data.getCompound(TAG_LINK_PANEL);
    ListTag list = linkpanel.getList(TAG_PROPERTIES, Tag.TAG_STRING);
    list.add(StringTag.valueOf(linkproperty));
    linkpanel.put(TAG_PROPERTIES, list);
  }

  /**
   * Gets the link properties from a link panel page.
   */
  @NotNull
  public static List<String> getLinkProperties(@NotNull ItemStack page) {
    List<String> result = new ArrayList<>();
    if (page.isEmpty() || ItemStackNbt.getTag(page) == null) {
      return result;
    }
    CompoundTag data = getData(page);
    if (!data.contains(TAG_LINK_PANEL)) {
      return result;
    }
    CompoundTag linkpanel = data.getCompound(TAG_LINK_PANEL);
    ListTag list = linkpanel.getList(TAG_PROPERTIES, Tag.TAG_STRING);
    for (int i = 0; i < list.size(); i++) {
      result.add(list.getString(i));
    }
    return result;
  }

  /**
   * Checks if a page has link properties.
   */
  public static boolean hasLinkProperties(@NotNull ItemStack page) {
    return !getLinkProperties(page).isEmpty();
  }

  /**
   * Checks if a page has a specific link property.
   */
  public static boolean hasLinkProperty(@NotNull ItemStack page, String property) {
    return getLinkProperties(page).contains(property);
  }

  /**
   * Removes a link property from a page.
   */
  public static void removeLinkProperty(@NotNull ItemStack page, String property) {
    if (page.isEmpty() || ItemStackNbt.getTag(page) == null) {
      return;
    }
    CompoundTag data = getData(page);
    if (!data.contains(TAG_LINK_PANEL)) {
      return;
    }
    CompoundTag linkpanel = data.getCompound(TAG_LINK_PANEL);
    ListTag list = linkpanel.getList(TAG_PROPERTIES, Tag.TAG_STRING);
    ListTag newList = new ListTag();
    for (int i = 0; i < list.size(); i++) {
      String prop = list.getString(i);
      if (!prop.equals(property)) {
        newList.add(StringTag.valueOf(prop));
      }
    }
    linkpanel.put(TAG_PROPERTIES, newList);
  }

  /**
   * Applies link panel properties to a linking item.
   */
  public static void applyLinkPanel(@NotNull ItemStack linkpanel, @NotNull ItemStack linkingitem) {
    Collection<String> properties = getLinkProperties(linkpanel);
    if (properties == null) {
      return;
    }
    for (String property : properties) {
      CompoundTag tag = ItemStackNbt.getOrCreateTag(linkingitem);
      LinkOptions.setFlag(tag, property, true);
      ItemStackNbt.setTag(linkingitem, tag);
    }
  }

  /**
   * Sets the symbol on a page.
   */
  public static void setSymbol(@NotNull ItemStack page, ResourceLocation symbol) {
    if (page.isEmpty() || ItemStackNbt.getTag(page) == null) {
      return;
    }
    CompoundTag data = getData(page);
    if (symbol == null) {
      data.remove(TAG_SYMBOL);
    } else {
      data.putString(TAG_SYMBOL, symbol.toString());
    }
  }

  /**
   * Gets the symbol from a page.
   */
  @Nullable
  public static ResourceLocation getSymbol(@NotNull ItemStack page) {
    if (page.isEmpty() || ItemStackNbt.getTag(page) == null) {
      return null;
    }
    CompoundTag data = getData(page);
    String symbol = data.getString(TAG_SYMBOL);
    if (symbol.isEmpty()) {
      return null;
    } else {
      return new ResourceLocation(symbol);
    }
  }

  // ---------------------------------------------------------------------------
  // Ink tint + affinity snapshot (added 2026-04 for procedural book UI / ink
  // affinity bias). Both are optional and default to "unset" so old pages
  // continue to read cleanly.
  // ---------------------------------------------------------------------------

  /**
   * Records the blended ARGB tint of the ink used to write this page.
   * Used by the procedural book UI to colour ink-edged decorations.
   * Pass {@code 0} to clear.
   */
  public static void setInkTint(@NotNull ItemStack page, int argb) {
    if (page.isEmpty()) {
      return;
    }
    CompoundTag data = getData(page);
    if (argb == 0) {
      data.remove(TAG_INK_TINT);
    } else {
      data.putInt(TAG_INK_TINT, argb);
    }
  }

  /**
   * Returns the recorded ink tint, or {@code -1} (white / unset) when the page
   * pre-dates the field.
   */
  public static int getInkTint(@NotNull ItemStack page) {
    if (page.isEmpty()) {
      return -1;
    }
    CompoundTag data = getData(page);
    if (data.contains(TAG_INK_TINT)) {
      return data.getInt(TAG_INK_TINT);
    }
    return -1;
  }

  /**
   * Stores the ink-affinity snapshot (a {@link CompoundTag} containing the
   * per-symbol / per-category / per-token weights frozen in at
   * link-panel write time). Used by the symbol-roll algorithm and by the
   * procedural cover sigil renderer.
   */
  public static void setAffinitySnapshot(@NotNull ItemStack page, @Nullable CompoundTag snapshot) {
    if (page.isEmpty()) {
      return;
    }
    CompoundTag data = getData(page);
    if (snapshot == null) {
      data.remove(TAG_AFFINITY);
    } else {
      data.put(TAG_AFFINITY, snapshot.copy());
    }
  }

  /**
   * Returns the affinity snapshot, or null if absent.
   */
  @Nullable
  public static CompoundTag getAffinitySnapshot(@NotNull ItemStack page) {
    if (page.isEmpty()) {
      return null;
    }
    CompoundTag data = getData(page);
    if (data.contains(TAG_AFFINITY)) {
      return data.getCompound(TAG_AFFINITY).copy();
    }
    return null;
  }

  /**
   * Gets tooltip information for a page.
   */
  public static void getTooltip(ItemStack page, List<Component> list) {
    if (isLinkPanel(page)) {
      Collection<String> properties = getLinkProperties(page);
      if (properties != null && !properties.isEmpty()) {
        for (String property : properties) {
          // Get localized name from InkEffects
          String localizedName = InkEffects.getLocalizedName(property);
          InkEffects.PropertyColor color = InkEffects.getPropertyColor(property);
          if (color != null) {
            // Add colored text
            int rgb = color.toRGB();
            list.add(Component.literal(localizedName).withStyle(style ->
                style.withColor(rgb)));
          } else {
            list.add(Component.literal(localizedName));
          }
        }
      }
    }
  }

  @NotNull
  public static CompoundTag createDefault() {
    return new CompoundTag();
  }

  /**
   * Creates an empty page item.
   */
  @NotNull
  public static ItemStack createPage() {
    ItemStack page = new ItemStack(ModItems.PAGE.get());
    ItemStackNbt.setTag(page, createDefault());
    return page;
  }

  /**
   * Creates a link panel page.
   */
  @NotNull
  public static ItemStack createLinkPage() {
    ItemStack page = new ItemStack(ModItems.PAGE.get());
    ItemStackNbt.setTag(page, createDefault());
    makeLinkPanel(page);
    return page;
  }

  /**
   * Creates a link panel page with a specific property.
   */
  @NotNull
  public static ItemStack createLinkPage(String property) {
    ItemStack page = new ItemStack(ModItems.PAGE.get());
    ItemStackNbt.setTag(page, createDefault());
    addLinkProperty(page, property);
    return page;
  }

  /**
   * Creates a symbol page.
   */
  @NotNull
  public static ItemStack createSymbolPage(ResourceLocation symbol) {
    ItemStack page = new ItemStack(ModItems.PAGE.get());
    ItemStackNbt.setTag(page, createDefault());
    setSymbol(page, symbol);
    return page;
  }

  /**
   * Creates a page with existing NBT data.
   */
  @NotNull
  public static ItemStack createPage(CompoundTag pagedata) {
    ItemStack page = new ItemStack(ModItems.PAGE.get());
    ItemStackNbt.setTag(page, pagedata);
    return page;
  }
}
