package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.LinkFlags;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.util.TooltipCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Links to a specific location in any dimension. Right-click opens the book
 * GUI; linking happens through activate(). Auto-initializes with current
 * position when first in inventory. Dropped as an entity when used for linking
 * (unless "following" flag is set).
 */
public class LinkbookItem extends Item implements TooltipCompat {

  private static final float DEFAULT_MAX_HEALTH = 10.0f;

  private static final ThreadLocal<Boolean> SETTING_HEALTH = ThreadLocal.withInitial(() -> false);

  public LinkbookItem(Properties properties) {
    super(properties.stacksTo(1).durability(10));
  }

  public static void setHealth(@NotNull ItemStack book, float health) {
    if (book.isEmpty()) return;
    if (SETTING_HEALTH.get()) return;
    SETTING_HEALTH.set(true);
    try {
      CompoundTag tag = ItemStackNbt.getOrCreateTag(book);
      tag.putFloat("damage", getMaxHealth(book) - health);
      ItemStackNbt.setTag(book, tag);
    } finally {
      SETTING_HEALTH.set(false);
    }
  }

  public static float getHealth(@NotNull ItemStack book) {
    float health = getMaxHealth(book);
    if (book.isEmpty()) return health;
    CompoundTag tag = ItemStackNbt.getTag(book);
    if (tag == null) return health;
    float damage = tag.getFloat("damage");
    return health - damage;
  }

  public static float getMaxHealth(@NotNull ItemStack book) {
    if (book.isEmpty()) return DEFAULT_MAX_HEALTH;
    CompoundTag tag = ItemStackNbt.getTag(book);
    if (tag == null || !tag.contains("MaxHealth")) {
      return DEFAULT_MAX_HEALTH;
    }
    return tag.getFloat("MaxHealth");
  }

  @NotNull
  public Rarity getRarity(@NotNull ItemStack stack) {
    return Rarity.RARE;
  }

  @Override
  @NotNull
  public Component getName(@NotNull ItemStack stack) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag != null) {
      String displayName = LinkOptions.getDisplayName(tag);
      if (!"???".equals(displayName)) {
        return Component.literal(displayName);
      }
    }
    return super.getName(stack);
  }

  public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag != null) {

      String name = LinkOptions.getDisplayName(tag);
      if (!name.isEmpty() && !"???".equals(name)) {
        tooltip.add(Component.literal(name));
      }
    }
  }

  @Override
  public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {

    if (!level.isClientSide) {
      validate(level, stack, entity);
    }
  }

  /**
   * Ensures the book has been initialized with link data. Re-initialises when
   * either no tag exists OR the tag is missing the {@code DimensionUID}/
   * {@code Spawn} fields {@link LinkingManager#performLink} requires — which
   * happens for stacks that have a damage tag but were never given link data
   * (the path a fresh-from-craft book takes when dropped straight into a
   * receptacle).
   */
  public void validate(@Nullable Level level, @NotNull ItemStack stack, @Nullable Entity entity) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag == null
        || LinkOptions.getDimensionUID(tag) == null
        || LinkOptions.getSpawn(tag) == null) {
      initialize(level, stack, entity);
    }
  }

  /**
   * Creates link info from current position. Called when the book has no tag
   * yet, or when the tag is missing the critical fields. Falls back to the
   * world's spawn (a non-null block pos) when no entity is supplied so a book
   * inserted directly into a receptacle without ever sitting in player
   * inventory still gets a usable Spawn/DimensionUID and the portal can
   * teleport. Preserves any existing tag fields (damage, MaxHealth, etc.) by
   * merging into the existing tag rather than replacing it.
   */
  protected void initialize(@Nullable Level level, @NotNull ItemStack stack, @Nullable Entity entity) {
    if (level == null) {
      return;
    }
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    BlockPos spawn = entity != null ? entity.blockPosition() : BlockPos.ZERO;
    LinkOptions.setSpawn(tag, spawn);
    if (entity != null) {
      LinkOptions.setSpawnYaw(tag, entity.getYRot());
    }
    int dimId = LinkingManager.getDimensionUID(level);
    LinkOptions.setDimensionUID(tag, dimId);

    if (!tag.contains("MaxHealth")) {
      tag.putFloat("MaxHealth", DEFAULT_MAX_HEALTH);
    }

    ItemStackNbt.setTag(stack, tag);
  }

  @Override
  @NotNull
  public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (level.isClientSide) {

      art.arcane.mystcraft.client.screen.BookScreen.open(stack);
    }

    return InteractionResultHolder.consume(stack);
  }

  /**
   * Performs the actual linking. Called from the book GUI or other activation
   * sources.
   */
  public void activate(@NotNull ItemStack stack, Level level, Entity entity) {
    if (level.isClientSide) {
      return;
    }
    if (ItemStackNbt.getTag(stack) == null) {
      return;
    }

    CompoundTag linkData = ItemStackNbt.getTag(stack);

    BlockPos spawn = LinkOptions.getSpawn(linkData);
    Integer dimId = LinkOptions.getDimensionUID(linkData);
    if (spawn == null || dimId == null) {
      return;
    }

    onLink(stack, level, entity);

    LinkingManager.performLink(entity, linkData);
  }

  /**
   * Called before linking. Drops book in world if "following" flag is not set.
   */
  protected void onLink(@NotNull ItemStack stack, Level level, Entity entity) {
    if (entity instanceof Player player) {

      ItemStack mainHand = player.getInventory().getSelected();
      ItemStack offHand = player.getOffhandItem();

      int slotToEmpty = -1;
      if (ItemStackNbt.isSameItemSameTags(mainHand, stack)) {
        slotToEmpty = player.getInventory().selected;
      } else if (ItemStackNbt.isSameItemSameTags(offHand, stack)) {
        slotToEmpty = 40;
      } else {

        return;
      }

      if (dropItemOnLink(stack)) {

        LinkbookEntity bookEntity = new LinkbookEntity(level, player.getX(), player.getY(), player.getZ());
        bookEntity.setBookItem(stack.copy());
        level.addFreshEntity(bookEntity);

        player.getInventory().setItem(slotToEmpty, ItemStack.EMPTY);
      }
    }
  }

  /**
   * Returns true unless "following" flag is set (book stays with player).
   */
  public boolean dropItemOnLink(@NotNull ItemStack stack) {
    if (!art.arcane.mystcraft.config.MystcraftConfig.dropBooksOnRead.get()) {
      return false;
    }
    return !LinkOptions.getFlag(ItemStackNbt.getTag(stack), LinkFlags.FOLLOWING);
  }

  /**
   * Linkbooks always contain a single link page.
   */
  public List<ItemStack> getPageList(Player player, @NotNull ItemStack stack) {
    return Collections.singletonList(Page.createLinkPage());
  }

  /**
   * Gets the authors of this book from NBT.
   */
  public Collection<String> getAuthors(@NotNull ItemStack stack) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag != null && tag.contains("Author")) {
      return Collections.singleton(tag.getString("Author"));
    }
    return Collections.emptySet();
  }

  /**
   * Sets the display name of the linkbook.
   */
  public void setDisplayName(@NotNull ItemStack stack, String name) {
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    LinkOptions.setDisplayName(tag, name);
    ItemStackNbt.setTag(stack, tag);
  }

  /**
   * Gets the display name of the linkbook.
   */
  public String getDisplayName(@NotNull ItemStack stack) {
    return LinkOptions.getDisplayName(ItemStackNbt.getTag(stack));
  }

  /**
   * Gets the destination position.
   */
  @Nullable
  public BlockPos getDestination(@NotNull ItemStack stack) {
    return LinkOptions.getSpawn(ItemStackNbt.getTag(stack));
  }

  /**
   * Sets the destination position.
   */
  public void setDestination(@NotNull ItemStack stack, BlockPos pos) {
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    LinkOptions.setSpawn(tag, pos);
    ItemStackNbt.setTag(stack, tag);
  }

  public boolean isDamageableItem() {
    return true;
  }

  @Override
  public boolean isEnchantable(@NotNull ItemStack stack) {
    return false;
  }

  public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
    return false;
  }

  public boolean isDamaged(@NotNull ItemStack stack) {
    return getHealth(stack) != getMaxHealth(stack);
  }

  public int getDamage(@NotNull ItemStack stack) {
    return (int) getMaxHealth(stack) - (int) getHealth(stack);
  }

  public void setDamage(@NotNull ItemStack stack, int damage) {
    setHealth(stack, getMaxHealth(stack) - damage);
  }

  public int getMaxDamage(@NotNull ItemStack stack) {
    return (int) getMaxHealth(stack);
  }

  public boolean hasCustomEntity(@NotNull ItemStack stack) {
    return art.arcane.mystcraft.config.MystcraftConfig.droppedBooksBecomeLivingEntities.get();
  }

  @Nullable
  public Entity createEntity(Level level, Entity location, @NotNull ItemStack stack) {
    if (!art.arcane.mystcraft.config.MystcraftConfig.droppedBooksBecomeLivingEntities.get()) {
      return null;
    }
    LinkbookEntity entity = new LinkbookEntity(level, location.getX(), location.getY(), location.getZ());
    entity.setBookItem(stack.copy());
    entity.setDeltaMovement(location.getDeltaMovement());
    return entity;
  }

  /**
   * Enchantment glint when "following" flag is set.
   */
  @Override
  public boolean isFoil(@NotNull ItemStack stack) {
    return LinkOptions.getFlag(ItemStackNbt.getTag(stack), LinkFlags.FOLLOWING);
  }
}
