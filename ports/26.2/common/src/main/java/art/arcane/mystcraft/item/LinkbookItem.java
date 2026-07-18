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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Links to a specific location in any dimension. Right-click opens the book
 * GUI; linking happens through activate(). Auto-initializes with current
 * position when first in inventory. Dropped as an entity when used for linking
 * (unless "following" flag is set).
 */
public class LinkbookItem extends Item implements TooltipCompat {

  private static final float DEFAULT_MAX_HEALTH = 10.0f;

  public LinkbookItem(Properties properties) {
    this(properties, true);
  }

  protected LinkbookItem(Properties properties, boolean damageable) {
    super(damageable ? properties.stacksTo(1).durability(10) : properties.stacksTo(1));
  }

  public static void setHealth(@NotNull ItemStack book, float health) {
    if (book.isEmpty()) return;
    float maxHealth = getMaxHealth(book);
    float clampedHealth = Mth.clamp(health, 0.0F, maxHealth);
    CompoundTag tag = ItemStackNbt.getOrCreateTag(book);
    tag.putFloat("damage", maxHealth - clampedHealth);
    ItemStackNbt.setTag(book, tag);
  }

  public static float getHealth(@NotNull ItemStack book) {
    float health = getMaxHealth(book);
    if (book.isEmpty()) return health;
    CompoundTag tag = ItemStackNbt.getTag(book);
    if (tag == null) return health;
    float damage = tag.getFloatOr("damage", 0.0F);
    if (!Float.isFinite(damage)) {
      return health;
    }
    return Mth.clamp(health - damage, 0.0F, health);
  }

  public static float getMaxHealth(@NotNull ItemStack book) {
    if (book.isEmpty()) return DEFAULT_MAX_HEALTH;
    CompoundTag tag = ItemStackNbt.getTag(book);
    if (tag == null || !tag.contains("MaxHealth")) {
      return DEFAULT_MAX_HEALTH;
    }
    float maxHealth = tag.getFloatOr("MaxHealth", DEFAULT_MAX_HEALTH);
    return Float.isFinite(maxHealth) && maxHealth > 0.0F ? maxHealth : DEFAULT_MAX_HEALTH;
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

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                              @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltip,
                              @NotNull TooltipFlag flag) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag != null) {

      String name = LinkOptions.getDisplayName(tag);
      if (!name.isEmpty() && !"???".equals(name)) {
        tooltip.accept(Component.literal(name));
      }
    }
  }

  @Override
  public void inventoryTick(@NotNull ItemStack stack, @NotNull ServerLevel level,
                            @NotNull Entity entity, @Nullable EquipmentSlot slot) {
    validate(level, stack, entity);
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
  public InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (level.isClientSide()) {
      ItemClientHooks.openBook(stack);
    }

    return InteractionResult.CONSUME;
  }

  /**
   * Performs the actual linking. Called from the book GUI or other activation
   * sources.
   */
  public void activate(@NotNull ItemStack stack, Level level, Entity entity) {
    if (level.isClientSide()) {
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

    PendingBookDrop pendingDrop = prepareBookDrop(stack, level, entity);
    LinkingManager.LinkResult result = LinkingManager.performLink(entity, linkData);
    if (result == LinkingManager.LinkResult.SUCCESS && pendingDrop != null) {
      pendingDrop.commit();
    } else if (pendingDrop != null) {
      pendingDrop.rollback();
    }
  }

  /**
   * Drops the book in the source world if the "following" flag is not set.
   * Normal activation prepares this action before teleporting and commits it
   * only after the link succeeds.
   */
  protected void onLink(@NotNull ItemStack stack, Level level, Entity entity) {
    PendingBookDrop pendingDrop = prepareBookDrop(stack, level, entity);
    if (pendingDrop != null) {
      pendingDrop.commit();
    }
  }

  @Nullable
  private PendingBookDrop prepareBookDrop(@NotNull ItemStack stack, Level level, Entity entity) {
    if (entity instanceof Player player) {
      ItemStack mainHand = player.getInventory().getSelectedItem();
      ItemStack offHand = player.getOffhandItem();

      int slotToEmpty = -1;
      if (mainHand == stack) {
        slotToEmpty = player.getInventory().getSelectedSlot();
      } else if (offHand == stack) {
        slotToEmpty = 40;
      } else {
        return null;
      }

      boolean reserveForDisarm = mainHand == stack
          && LinkOptions.getFlag(ItemStackNbt.getTag(stack), LinkFlags.DISARM);
      if (dropItemOnLink(stack) || reserveForDisarm) {
        LinkbookEntity bookEntity = new LinkbookEntity(level, player.getX(), player.getY(), player.getZ());
        bookEntity.setBookItem(stack.copy());
        if (reserveForDisarm) {
          player.getInventory().setItem(slotToEmpty, ItemStack.EMPTY);
        }
        return new PendingBookDrop(level, player, slotToEmpty, stack, bookEntity, reserveForDisarm);
      }
    }
    return null;
  }

  private record PendingBookDrop(Level sourceLevel, Player player, int inventorySlot,
                                 ItemStack expectedStack, LinkbookEntity bookEntity,
                                 boolean reservedForDisarm) {
    private void commit() {
      ItemStack currentStack = player.getInventory().getItem(inventorySlot);
      if (reservedForDisarm ? !currentStack.isEmpty() : currentStack != expectedStack) {
        rollback();
        return;
      }
      if (sourceLevel.addFreshEntity(bookEntity)) {
        if (!reservedForDisarm) {
          player.getInventory().setItem(inventorySlot, ItemStack.EMPTY);
        }
      } else {
        rollback();
      }
    }

    private void rollback() {
      if (!reservedForDisarm) {
        return;
      }
      if (player.getInventory().getItem(inventorySlot).isEmpty()) {
        player.getInventory().setItem(inventorySlot, expectedStack);
      } else {
        player.getInventory().placeItemBackInInventory(expectedStack);
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
      return Collections.singleton(tag.getStringOr("Author", ""));
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

  public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
    return false;
  }

  @Override
  public boolean isBarVisible(@NotNull ItemStack stack) {
    return getHealth(stack) < getMaxHealth(stack);
  }

  @Override
  public int getBarWidth(@NotNull ItemStack stack) {
    float maxHealth = getMaxHealth(stack);
    return Mth.clamp(Math.round(13.0F * getHealth(stack) / maxHealth), 0, 13);
  }

  @Override
  public int getBarColor(@NotNull ItemStack stack) {
    float healthPercentage = getHealth(stack) / getMaxHealth(stack);
    return Mth.hsvToRgb(healthPercentage / 3.0F, 1.0F, 1.0F);
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
