package art.arcane.mystcraft.item;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.InkBlend;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.util.ItemStackNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Booster Pack (Sealed Notebook) item. Contains random symbol pages when
 * opened. Used to give players new symbols to discover.
 * <p>
 * Booster packs may optionally carry an {@link InkBlend} affinity snapshot
 * under the {@code "BoosterAffinity"} NBT key. When present, generated symbol
 * pages are rolled via
 * {@link SymbolRegistry#getRandomWeightedWithAffinity(RandomSource, InkBlend)}
 * so the pack's contents reflect the themed ink it was crafted with (e.g. a
 * diamond-themed pack favours dense_ores / dripstone / deep_dark). Packs
 * without the tag fall back to plain weighted selection.
 */
public class BoosterPackItem extends Item {

  /**
   * NBT key holding an {@link InkBlend} snapshot for themed packs.
   */
  public static final String TAG_AFFINITY = "BoosterAffinity";

  private static final int PAGES_PER_PACK = 3;

  public BoosterPackItem(Properties properties) {
    super(properties);
  }

  /**
   * Reads the affinity blend stored on a booster pack stack, or returns
   * {@code null} if the pack carries no themed ink.
   */
  @Nullable
  public static InkBlend getAffinity(@NotNull ItemStack stack) {
    if (stack.isEmpty()) return null;
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag == null || !tag.contains(TAG_AFFINITY, Tag.TAG_COMPOUND))
      return null;
    InkBlend blend = InkBlend.fromTag(tag.getCompound(TAG_AFFINITY));
    return blend.isEmpty() ? null : blend;
  }

  /**
   * Stores an affinity blend on a booster pack stack. Pass {@code null} or an
   * empty blend to remove any existing affinity.
   */
  public static void setAffinity(@NotNull ItemStack stack, @Nullable InkBlend blend) {
    if (stack.isEmpty()) return;
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    if (blend == null || blend.isEmpty()) {
      tag.remove(TAG_AFFINITY);
    } else {
      tag.put(TAG_AFFINITY, blend.toNbt());
    }
  }

  @Override
  @NotNull
  public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (!level.isClientSide) {

      List<ItemStack> pages = generatePages(level, player, stack);

      for (ItemStack page : pages) {
        if (!player.getInventory().add(page)) {
          player.drop(page, false);
        }
      }

      stack.shrink(1);

      if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) || serverPlayer.connection != null) {
        player.displayClientMessage(
            Component.translatable("item.mystcraft.booster.opened", pages.size()),
            true);
      }
    }

    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
  }

  private List<ItemStack> generatePages(Level level, Player player, ItemStack packStack) {
    List<ItemStack> pages = new ArrayList<>();
    RandomSource random = level.random;
    InkBlend affinity = getAffinity(packStack);

    boolean allowLinkPanels = MystcraftConfig.linkPanelInBoosterPacks.get();

    for (int i = 0; i < PAGES_PER_PACK; i++) {
      float roll = random.nextFloat();
      if (allowLinkPanels && roll < 0.2f) {

        pages.add(Page.createLinkPage());
      } else {

        IAgeSymbol symbol = affinity != null
            ? SymbolRegistry.getRandomWeightedWithAffinity(random, affinity)
            : SymbolRegistry.getRandomWeighted(random);
        if (symbol != null) {
          pages.add(Page.createSymbolPage(symbol.getRegistryName()));
        } else {

          pages.add(Page.createPage());
        }
      }
    }

    return pages;
  }
}
