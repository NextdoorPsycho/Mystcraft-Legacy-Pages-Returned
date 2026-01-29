package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.data.InkEffects;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.menu.InkMixerMenu;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModFluids;
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
  private static final String TAG_SEED = "seed";
  private final SimpleContainer inventory = new SimpleContainer(3) {
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
      if (slot == SLOT_INK_IN) {
        return isValidInkContainer(stack);
      }
      if (slot == SLOT_PAPER) {
        return stack.is(Items.PAPER);
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

  private boolean hasInk = false;
  private final Map<String, Float> inkProbabilities = new HashMap<>();
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
        stack.save(itemTag);
        itemList.add(itemTag);
      }
    }
    tag.put(TAG_INVENTORY, itemList);
    tag.putBoolean(TAG_HAS_INK, hasInk);
    tag.putLong(TAG_SEED, nextSeed);

    // Save probabilities
    CompoundTag probs = new CompoundTag();
    for (Map.Entry<String, Float> entry : inkProbabilities.entrySet()) {
      probs.putFloat(entry.getKey(), entry.getValue());
    }
    tag.put(TAG_PROBABILITIES, probs);
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
        inventory.setItem(slot, ItemStack.of(itemTag));
      }
    }
    hasInk = tag.getBoolean(TAG_HAS_INK);
    nextSeed = tag.getLong(TAG_SEED);

    // Load probabilities
    inkProbabilities.clear();
    CompoundTag probs = tag.getCompound(TAG_PROBABILITIES);
    for (String key : probs.getAllKeys()) {
      inkProbabilities.put(key, probs.getFloat(key));
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
      if (!ItemStack.isSameItemSameTags(currentOutput, emptyContainer)) {
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
    return stack.is(ModFluids.BLACK_INK_BUCKET.get());
  }

  /**
   * Checks if an item can be crafted.
   */
  public boolean canBuildItem() {
    ItemStack paper = inventory.getItem(SLOT_PAPER);
    return !paper.isEmpty() && paper.is(Items.PAPER) && hasInk;
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
    for (Map.Entry<String, Float> entry : inkProbabilities.entrySet()) {
      String property = entry.getKey();
      float probability = entry.getValue();
      if (rand.nextFloat() < probability) {
        Page.addLinkProperty(output, property);
      }
    }

    // Reset state
    nextSeed = rand.nextLong();
    hasInk = false;
    inkProbabilities.clear();

    // Consume paper
    inventory.getItem(SLOT_PAPER).shrink(1);

    setChanged();
    markForUpdate();
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

    Map<String, Float> effects = InkEffects.getItemEffects(stack);
    if (effects == null || effects.isEmpty()) {
      return stack; // Item has no ink effects
    }

    // Consume items and add effects
    int toConsume = Math.min(amount, stack.getCount());
    for (int i = 0; i < toConsume; i++) {
      for (Map.Entry<String, Float> entry : effects.entrySet()) {
        String property = entry.getKey();
        float probability = entry.getValue();

        // Add probability (capped at 1.0)
        float current = inkProbabilities.getOrDefault(property, 0f);
        float newProb = Math.min(1.0f, current + probability);
        inkProbabilities.put(property, newProb);
      }
    }

    // Consume the items
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
    return InkEffects.hasEffects(stack);
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
    setChanged();
  }

  /**
   * Gets the current ink probabilities.
   */
  public Map<String, Float> getInkProbabilities() {
    return new HashMap<>(inkProbabilities);
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
