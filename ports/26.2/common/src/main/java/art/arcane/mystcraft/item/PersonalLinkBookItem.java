package art.arcane.mystcraft.item;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.event.PersonalPocketEscapeHandler;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.util.TooltipCompat;
import art.arcane.mystcraft.world.PersonalPocketData;
import art.arcane.mystcraft.world.PersonalPocketDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Personal link book that links to a player-specific pocket dimension.
 */
public class PersonalLinkBookItem extends LinkbookItem implements TooltipCompat {

  public static final String TOOLTIP_KEY = "item.mystcraft.personal_link_book.tooltip";
  private static final Set<String> STORED_LINK_KEYS = Set.of(
      "Dimension",
      "AgeUID",
      "DimensionKey",
      "TargetUUID",
      "SpawnX",
      "SpawnY",
      "SpawnZ",
      "SpawnYaw",
      "DisplayName",
      "LinkColor",
      "LinkDead",
      "Flags",
      "Props",
      "LinkOptions",
      "MaxHealth",
      "damage",
      "NoDecay"
  );

  public PersonalLinkBookItem(Properties properties) {
    super(properties, false);
  }

  private static CompoundTag createPocketLink(ServerPlayer player) {
    CompoundTag tag = new CompoundTag();
    LinkOptions.setDimensionUID(tag, PersonalPocketDimension.getPersonalAgeUid(player.getUUID()));
    LinkOptions.setSpawn(tag, PersonalPocketDimension.getPocketSpawn());
    LinkOptions.setSpawnYaw(tag, player.getYRot());
    return tag;
  }

  private static void clearStoredLinkData(ItemStack stack) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag == null) {
      return;
    }
    for (String key : STORED_LINK_KEYS) {
      tag.remove(key);
    }
    if (tag.keySet().isEmpty()) {
      ItemStackNbt.setTag(stack, null);
    } else {
      ItemStackNbt.setTag(stack, tag);
    }
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                              @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltip,
                              @NotNull TooltipFlag flag) {
    tooltip.accept(Component.translatable(TOOLTIP_KEY));
    if (!MystcraftConfig.enablePersonalLinkBooks.get()) {
      tooltip.accept(Component.literal("Disabled in config"));
    }
  }

  @Override
  @NotNull
  public Component getName(@NotNull ItemStack stack) {
    return Component.translatable(getDescriptionId());
  }

  @Override
  public void inventoryTick(@NotNull ItemStack stack, @NotNull ServerLevel level,
                            @NotNull Entity entity, @Nullable EquipmentSlot slot) {
    clearStoredLinkData(stack);
  }

  @Override
  public void validate(@Nullable Level level, @NotNull ItemStack stack, @Nullable Entity entity) {
    clearStoredLinkData(stack);
  }

  @Override
  protected void initialize(@Nullable Level level, @NotNull ItemStack stack, @Nullable Entity entity) {
    clearStoredLinkData(stack);
  }

  @Override
  @NotNull
  public InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    clearStoredLinkData(stack);
    if (!level.isClientSide()) {
      if (!MystcraftConfig.enablePersonalLinkBooks.get()) {
        player.sendOverlayMessage(Component.literal("Personal link books are disabled in the config."));
        return InteractionResult.FAIL;
      }
    }
    return super.use(level, player, hand);
  }

  @Override
  public void activate(@NotNull ItemStack stack, Level level, Entity entity) {
    if (level.isClientSide()) {
      return;
    }
    if (!MystcraftConfig.enablePersonalLinkBooks.get()) {
      if (entity instanceof ServerPlayer player) {
        player.sendOverlayMessage(Component.literal("Personal link books are disabled in the config."));
      }
      return;
    }
    if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
      return;
    }
    clearStoredLinkData(stack);

    if (PersonalPocketDimension.isPersonalPocket(serverLevel)) {
      PersonalPocketEscapeHandler.returnToEntryPoint(player, false);
      return;
    }

    CompoundTag returnData = new CompoundTag();
    LinkOptions.setDimensionUID(returnData, LinkingManager.getDimensionUID(serverLevel));
    LinkOptions.setSpawn(returnData, player.blockPosition());
    LinkOptions.setSpawnYaw(returnData, player.getYRot());
    PersonalPocketData.get(serverLevel.getServer()).setReturnLink(player.getUUID(), returnData);

    ServerLevel pocketLevel = PersonalPocketDimension.getOrCreate(serverLevel.getServer(), player.getUUID());
    if (pocketLevel == null) {
      art.arcane.mystcraft.Mystcraft.LOGGER.warn("[PersonalPocket] Failed to create pocket for {}", player.getGameProfile().name());
      return;
    }

    if (!PersonalPocketEscapeHandler.spawnOrReplaceProxy(player, returnData)) {
      player.sendOverlayMessage(Component.literal("Unable to create your personal pocket anchor."));
      return;
    }

    CompoundTag pocketLink = createPocketLink(player);
    LinkingManager.LinkResult result = LinkingManager.performLink(player, pocketLink);
    if (result != LinkingManager.LinkResult.SUCCESS) {
      PersonalPocketEscapeHandler.removeActiveProxy(serverLevel.getServer(), player.getUUID());
      art.arcane.mystcraft.Mystcraft.LOGGER.warn("[PersonalPocket] Link failed for {}: {}", player.getGameProfile().name(), result);
    }
    clearStoredLinkData(stack);
  }

  @Override
  protected void onLink(@NotNull ItemStack stack, Level level, Entity entity) {

  }

  @Override
  public boolean dropItemOnLink(@NotNull ItemStack stack) {
    return false;
  }

  @Override
  public boolean hasCustomEntity(@NotNull ItemStack stack) {
    return false;
  }

  @Override
  public Entity createEntity(Level level, Entity location, @NotNull ItemStack stack) {
    return null;
  }

  @Override
  public void setDisplayName(@NotNull ItemStack stack, String name) {
    clearStoredLinkData(stack);
  }

  @Override
  @Nullable
  public BlockPos getDestination(@NotNull ItemStack stack) {
    return null;
  }

  @Override
  public void setDestination(@NotNull ItemStack stack, BlockPos pos) {
    clearStoredLinkData(stack);
  }

  @Override
  public String getDisplayName(@NotNull ItemStack stack) {
    return "???";
  }

}
