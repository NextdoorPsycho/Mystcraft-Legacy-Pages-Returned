package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.data.InkAffinity;
import art.arcane.mystcraft.data.InkBlend;
import art.arcane.mystcraft.data.InkEffects;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.menu.InkMixerMenu;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModFluids;
import art.arcane.mystcraft.registry.ModTags;
import art.arcane.mystcraft.util.ItemStackNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Block entity for the Ink Mixer.
 * Handles mixing of inks and dyes to create link panels with properties.
 * <p>
 * Slots:
 * 0 - Ink input (fluid containers)
 * 1 - Paper input
 * 2 - Empty container output
 */
public class InkMixerBlockEntity extends MystcraftBlockEntity implements MenuProvider {

  public static final int SLOT_INK_IN = 0;
  public static final int SLOT_PAPER = 1;
  public static final int SLOT_INK_OUT = 2;
  private static final String TAG_INVENTORY = "inventory";
  private static final String TAG_HAS_INK = "ink";
  private static final String TAG_PROBABILITIES = "probabilities";
  private static final String TAG_BLEND = "blend";
  private static final String TAG_SEED = "seed";
  private final SimpleContainer inventory = new SimpleContainer(3) {
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
      if (slot == SLOT_INK_IN) {
        return isValidInkContainer(stack);
      }
      if (slot == SLOT_PAPER) {
        return isBlankPage(stack);
      }
      return false;
    }

    @Override
    public void setChanged() {
      super.setChanged();
      InkMixerBlockEntity.this.setChanged();
      InkMixerBlockEntity.this.markForUpdate();
    }
  };
  private final InkBlend blend = new InkBlend();
  private boolean hasInk = false;
  private long nextSeed;

  public InkMixerBlockEntity(BlockPos pos, BlockState blockState) {
    super(ModBlockEntities.INK_MIXER.get(), pos, blockState);
    this.nextSeed = new Random().nextLong();
  }

  /**
   * Gets the inventory container for external access.
   */
  public Container getInventory() {
    return inventory;
  }

  @Override
  protected void writeNbt(CompoundTag tag) {
    super.writeNbt(tag);
    ListTag itemList = new ListTag();
    for (int i = 0; i < inventory.getContainerSize(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        CompoundTag itemTag = new CompoundTag();
        itemTag.putInt("Slot", i);
        itemTag.merge(ItemStackNbt.save(stack));
        itemList.add(itemTag);
      }
    }
    tag.put(TAG_INVENTORY, itemList);
    tag.putBoolean(TAG_HAS_INK, hasInk);
    tag.putLong(TAG_SEED, nextSeed);

    // Persist the full affinity blend (covers link properties + symbol bias).
    if (!blend.isEmpty()) {
      tag.put(TAG_BLEND, blend.toNbt());
    }

    // Save legacy probability map for backwards compatibility with old saves
    // and any third-party tools that read the raw map directly.
    Map<String, Float> linkProps = blend.linkPropertyWeights();
    if (!linkProps.isEmpty()) {
      CompoundTag probs = new CompoundTag();
      for (Map.Entry<String, Float> entry : linkProps.entrySet()) {
        probs.putFloat(entry.getKey(), entry.getValue());
      }
      tag.put(TAG_PROBABILITIES, probs);
    }
  }

  @Override
  protected void readNbt(CompoundTag tag) {
    super.readNbt(tag);
    inventory.clearContent();
    ListTag itemList = tag.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
    for (int i = 0; i < itemList.size(); i++) {
      CompoundTag itemTag = itemList.getCompound(i);
      int slot = itemTag.getInt("Slot");
      if (slot >= 0 && slot < inventory.getContainerSize()) {
        CompoundTag itemData = itemTag.contains("Item", Tag.TAG_COMPOUND) ? itemTag.getCompound("Item") : itemTag;
        inventory.setItem(slot, ItemStackNbt.load(itemData));
      }
    }
    hasInk = tag.getBoolean(TAG_HAS_INK);
    nextSeed = tag.getLong(TAG_SEED);

    // Prefer the new blend tag; fall back to the legacy probabilities map for
    // pre-affinity saves so existing worlds still load with the same link-
    // property mix in the basin.
    blend.clear();
    if (tag.contains(TAG_BLEND, Tag.TAG_COMPOUND)) {
      blend.fromNbt(tag.getCompound(TAG_BLEND));
    } else if (tag.contains(TAG_PROBABILITIES, Tag.TAG_COMPOUND)) {
      CompoundTag probs = tag.getCompound(TAG_PROBABILITIES);
      Map<String, Float> linkProps = new HashMap<>();
      for (String key : probs.getAllKeys()) {
        linkProps.put(key, probs.getFloat(key));
      }
      if (!linkProps.isEmpty()) {
        InkAffinity.Entry legacy = new InkAffinity.Entry(
            1f, 0, Map.of(), Map.of(), Map.of(), linkProps);
        blend.add(legacy, 1);
      }
    }
  }

  /**
   * Called every tick to process ink from containers.
   */
  public void tick() {
    if (level == null || level.isClientSide) {
      return;
    }

    if (!hasInk && !inventory.getItem(SLOT_INK_IN).isEmpty()) {
      tryFillFromContainer();
    }
  }

  /**
   * Tries to fill the basin from the ink container.
   */
  private void tryFillFromContainer() {
    ItemStack container = inventory.getItem(SLOT_INK_IN);
    if (container.isEmpty()) {
      return;
    }

    // Check if the container holds valid ink by checking its crafting remainder
    // and if it's a known ink container (e.g., ink bucket)
    if (!isValidInkContainer(container)) {
      return;
    }

    // Get the empty container (crafting remainder)
    ItemStack emptyContainer;
    if (container.getItem().hasCraftingRemainingItem()) {
      emptyContainer = new ItemStack(container.getItem().getCraftingRemainingItem());
    } else {
      emptyContainer = new ItemStack(Items.BUCKET);
    }

    // Check if we can put the empty container in the output
    ItemStack currentOutput = inventory.getItem(SLOT_INK_OUT);
    if (!currentOutput.isEmpty()) {
      if (!ItemStackNbt.isSameItemSameTags(currentOutput, emptyContainer)) {
        return;
      }
      if (currentOutput.getCount() >= currentOutput.getMaxStackSize()) {
        return;
      }
    }

    // Fill the basin
    hasInk = true;
    container.shrink(1);
    if (container.isEmpty()) {
      inventory.setItem(SLOT_INK_IN, ItemStack.EMPTY);
    }

    // Add empty container to output
    if (currentOutput.isEmpty()) {
      inventory.setItem(SLOT_INK_OUT, emptyContainer);
    } else {
      currentOutput.grow(1);
    }

    setChanged();
    markForUpdate();
  }

  /**
   * Checks if a fluid is valid ink by fluid reference.
   */
  private boolean isValidInkFluid(Fluid fluid) {
    return fluid == ModFluids.BLACK_INK_SOURCE.get() ||
        fluid == ModFluids.BLACK_INK_FLOWING.get();
  }

  /**
   * Checks if a stack is a valid ink container.
   * Accepts items that have a crafting remainder and are associated with ink fluid.
   */
  private boolean isValidInkContainer(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }
    // Check if the item is a bucket of ink (the item itself represents the filled container)
    // In vanilla-compatible code, we check the item's registry name or tag
    // For now, accept any item that has a crafting remainder (like buckets)
    // and is registered as an ink bucket in ModFluids
    return stack.is(ModTags.Items.INK_BUCKETS);
  }

  private boolean isBlankPage(ItemStack stack) {
    return !stack.isEmpty() && stack.getItem() instanceof PageItem && Page.isBlank(stack);
  }

  /**
   * Checks if an item can be crafted.
   */
  public boolean canBuildItem() {
    ItemStack paper = inventory.getItem(SLOT_PAPER);
    return isBlankPage(paper) && hasInk;
  }

  /**
   * Gets the item that would be crafted.
   */
  @NotNull
  public ItemStack getCraftedItem() {
    if (!canBuildItem()) {
      return ItemStack.EMPTY;
    }
    return Page.createLinkPage();
  }

  /**
   * Builds the link panel and gives it to the player.
   */
  public void buildItem(@NotNull ItemStack output, @NotNull Player player) {
    if (!canBuildItem()) {
      return;
    }

    // Apply ink probabilities to the link panel
    Random rand = new Random(nextSeed);
    Map<String, Float> linkProps = blend.linkPropertyWeights();
    for (Map.Entry<String, Float> entry : linkProps.entrySet()) {
      String property = entry.getKey();
      float probability = entry.getValue();
      if (rand.nextFloat() < probability) {
        Page.addLinkProperty(output, property);
      }
    }

    // Persist the blended ink tint so the procedural Book UI can render
    // the link panel and book cover with colors derived from the actual
    // ink mixture (the "water" portion of the dependency chain).
    int blendedTint = computeBlendedInkTint(linkProps);
    if (blendedTint != 0) {
      Page.setInkTint(output, blendedTint);
    }

    // Snapshot the affinity blend onto the link panel page so any downstream
    // consumer (booster pack crafted from this ink, themed page roll) can
    // re-hydrate the same biases.
    if (!blend.isEmpty()) {
      Page.setAffinitySnapshot(output, blend.toNbt());
    }

    // Reset state
    nextSeed = rand.nextLong();
    hasInk = false;
    blend.clear();

    // Consume paper
    inventory.getItem(SLOT_PAPER).shrink(1);

    setChanged();
    markForUpdate();
  }

  /**
   * Blends property colors weighted by their probabilities to produce a single
   * representative tint for the resulting link panel. Properties with higher
   * probabilities pull the blend toward their color; an unmodified ink with
   * no extras returns 0 (neutral / black-ink default).
   */
  private static int computeBlendedInkTint(Map<String, Float> probabilities) {
    if (probabilities == null || probabilities.isEmpty()) {
      return 0;
    }
    float r = 0f, g = 0f, b = 0f, total = 0f;
    for (Map.Entry<String, Float> entry : probabilities.entrySet()) {
      InkEffects.PropertyColor color = InkEffects.getPropertyColor(entry.getKey());
      if (color == null) continue;
      float weight = Math.max(0f, entry.getValue());
      r += color.r() * weight;
      g += color.g() * weight;
      b += color.b() * weight;
      total += weight;
    }
    if (total <= 0f) {
      return 0;
    }
    int ri = Math.min(255, Math.round((r / total) * 255f)) & 0xFF;
    int gi = Math.min(255, Math.round((g / total) * 255f)) & 0xFF;
    int bi = Math.min(255, Math.round((b / total) * 255f)) & 0xFF;
    return (ri << 16) | (gi << 8) | bi;
  }

  /**
   * Adds items to modify ink properties.
   * Items with registered ink effects will modify the probabilities of properties.
   *
   * @param stack  The item stack to add
   * @param amount The number of items to consume
   * @return The remaining items that weren't consumed
   */
  @NotNull
  public ItemStack addItems(@NotNull ItemStack stack, int amount) {
    if (!hasInk || stack.isEmpty()) {
      return stack;
    }

    InkAffinity.Entry affinity = InkAffinity.getAffinity(stack);
    if (affinity == null || affinity.isEmpty()) {
      // Fall back to the legacy InkEffects map for items registered through
      // the old (pre-affinity) API. InkEffects.getItemEffects already
      // delegates to InkAffinity, but custom callers may still register
      // entries via InkEffects.addPropertyToItem.
      Map<String, Float> legacy = InkEffects.getItemEffects(stack);
      if (legacy == null || legacy.isEmpty()) {
        return stack;
      }
      affinity = new InkAffinity.Entry(1f, 0, Map.of(), Map.of(), Map.of(), legacy);
    }

    int toConsume = Math.min(amount, stack.getCount());
    if (toConsume <= 0) return stack;
    blend.add(affinity, toConsume);

    ItemStack remainder = stack.copy();
    remainder.shrink(toConsume);

    setChanged();
    markForUpdate();

    return remainder;
  }

  /**
   * Checks if an item can be added to the ink to modify properties.
   */
  public boolean canAddItem(@NotNull ItemStack stack) {
    if (!hasInk || stack.isEmpty()) {
      return false;
    }
    return InkAffinity.hasAffinity(stack) || InkEffects.hasEffects(stack);
  }

  /**
   * Gets whether the mixer has ink.
   */
  public boolean hasInk() {
    return hasInk;
  }

  /**
   * Sets whether the mixer has ink.
   */
  public void setHasInk(boolean hasInk) {
    this.hasInk = hasInk;
    if (!hasInk) {
      blend.clear();
    }
    setChanged();
  }

  /**
   * Gets the current ink probabilities (link-property weights only).
   * Backed by the affinity blend; mutating the returned map has no effect.
   */
  public Map<String, Float> getInkProbabilities() {
    return new HashMap<>(blend.linkPropertyWeights());
  }

  /**
   * Read-only view of the full affinity blend (for tooltips and tests).
   */
  @NotNull
  public InkBlend getBlend() {
    return blend;
  }

  /**
   * Gets drops for when the block is broken.
   */
  public List<ItemStack> getDrops() {
    List<ItemStack> drops = new ArrayList<>();
    for (int i = 0; i < inventory.getContainerSize(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        drops.add(stack.copy());
      }
    }
    return drops;
  }

  // MenuProvider implementation

  @Override
  @NotNull
  public Component getDisplayName() {
    return Component.translatable("container.mystcraft.ink_mixer");
  }

  @Override
  public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
    return new InkMixerMenu(containerId, playerInventory, this);
  }
}
