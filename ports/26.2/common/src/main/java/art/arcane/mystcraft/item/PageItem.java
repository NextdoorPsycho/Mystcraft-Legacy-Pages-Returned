package art.arcane.mystcraft.item;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.util.TooltipCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
 * The Page item. Contains a single symbol that can be used in Age creation. Can
 * be blank, a link panel, or contain a symbol.
 */
public class PageItem extends Item implements TooltipCompat {

  public PageItem(Properties properties) {
    super(properties);
  }

  @Override
  @NotNull
  public Component getName(@NotNull ItemStack stack) {
    if (ItemStackNbt.getTag(stack) != null) {
      if (Page.isLinkPanel(stack)) {
        return Component.translatable("item.mystcraft.page.panel");
      }
      if (Page.isBlank(stack)) {
        return Component.translatable("item.mystcraft.page.blank");
      }
      Identifier symbolId = Page.getSymbol(stack);
      if (symbolId != null) {
        IAgeSymbol symbol = SymbolRegistry.get(symbolId);
        if (symbol != null) {
          return Component.translatable("item.mystcraft.page.symbol", symbol.getLocalizedName());
        }
        return Component.translatable("item.mystcraft.page.symbol", symbolId.getPath());
      }
    }
    return Component.translatable("item.mystcraft.page.blank");
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

  /**
   * Link panels have an enchantment glint effect to make them visually
   * distinct.
   */
  @Override
  public boolean isFoil(@NotNull ItemStack stack) {
    return Page.isLinkPanel(stack);
  }

  /**
   * Checks if this page has a symbol written on it.
   */
  public boolean hasSymbol(ItemStack stack) {
    return Page.getSymbol(stack) != null;
  }

  /**
   * Gets the symbol ID from this page.
   */
  @Nullable
  public Identifier getSymbolId(ItemStack stack) {
    return Page.getSymbol(stack);
  }

  /**
   * Checks if this page is blank (can have a symbol written to it).
   */
  public boolean isBlank(ItemStack stack) {
    return Page.isBlank(stack);
  }

  /**
   * Checks if this page is a link panel.
   */
  public boolean isLinkPanel(ItemStack stack) {
    return Page.isLinkPanel(stack);
  }

  /**
   * Writes a symbol to a blank page.
   *
   * @return true if the symbol was written successfully
   */
  public boolean writeSymbol(ItemStack stack, Identifier symbol) {
    if (!Page.isBlank(stack)) {
      return false;
    }
    if (ItemStackNbt.getTag(stack) == null) {
      ItemStackNbt.setTag(stack, Page.createDefault());
    }
    Page.setSymbol(stack, symbol);
    return true;
  }
}
