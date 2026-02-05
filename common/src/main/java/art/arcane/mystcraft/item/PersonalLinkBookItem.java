package art.arcane.mystcraft.item;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.world.PersonalPocketData;
import art.arcane.mystcraft.world.PersonalPocketDimension;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.util.TooltipCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Personal link book that links to a player-specific pocket dimension.
 */
public class PersonalLinkBookItem extends LinkbookItem implements TooltipCompat {

  public static final String TOOLTIP_KEY = "item.mystcraft.personal_link_book.tooltip";

  public PersonalLinkBookItem(Properties properties) {
    super(properties.stacksTo(1));
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
    tooltip.add(Component.translatable(TOOLTIP_KEY));
    if (!MystcraftConfig.enablePersonalLinkBooks.get()) {
      tooltip.add(Component.literal("Disabled in config"));
    }
  }

  @Override
  protected void initialize(@Nullable Level level, @NotNull ItemStack stack, @Nullable Entity entity) {
    if (!MystcraftConfig.enablePersonalLinkBooks.get()) {
      return;
    }
    if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
      return;
    }

    // Ensure the personal dimension exists before the first link.
    PersonalPocketDimension.getOrCreate(serverLevel.getServer(), player.getUUID());

    CompoundTag tag = new CompoundTag();
    LinkOptions.setDimensionUID(tag, PersonalPocketDimension.getPersonalAgeUid(player.getUUID()));
    LinkOptions.setSpawn(tag, PersonalPocketDimension.getPocketSpawn());
    LinkOptions.setSpawnYaw(tag, player.getYRot());
    LinkOptions.setDisplayName(tag, "Personal Pocket");
    tag.putFloat("MaxHealth", 10.0f);
    tag.putFloat("damage", 0.0f);
    tag.putBoolean("NoDecay", true);

    ItemStackNbt.setTag(stack, tag);
  }

  @Override
  @NotNull
  public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!level.isClientSide) {
      if (!MystcraftConfig.enablePersonalLinkBooks.get()) {
        player.displayClientMessage(Component.literal("Personal link books are disabled in the config."), true);
        return InteractionResultHolder.fail(stack);
      }
      validate(level, stack, player);
    }
    return super.use(level, player, hand);
  }

  @Override
  public void activate(@NotNull ItemStack stack, Level level, Entity entity) {
    if (level.isClientSide) {
      return;
    }
    if (!MystcraftConfig.enablePersonalLinkBooks.get()) {
      if (entity instanceof ServerPlayer player) {
        player.displayClientMessage(Component.literal("Personal link books are disabled in the config."), true);
      }
      return;
    }
    if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
      return;
    }

    // If already inside the pocket, use the stored return link instead.
    if (PersonalPocketDimension.isPersonalPocket(serverLevel)) {
      CompoundTag returnLink = PersonalPocketData.get(serverLevel.getServer()).getReturnLink(player.getUUID());
      if (returnLink == null) {
        returnLink = new CompoundTag();
        LinkOptions.setDimensionUID(returnLink, LinkingManager.getDimensionUID(serverLevel.getServer().overworld()));
        LinkOptions.setSpawn(returnLink, serverLevel.getServer().overworld().getSharedSpawnPos());
        LinkOptions.setSpawnYaw(returnLink, player.getYRot());
        art.arcane.mystcraft.Mystcraft.LOGGER.warn("[PersonalPocket] Missing return link for {}, using overworld spawn", player.getGameProfile().getName());
      }
      LinkingManager.LinkResult result = LinkingManager.performLink(player, returnLink);
      if (result != LinkingManager.LinkResult.SUCCESS) {
        art.arcane.mystcraft.Mystcraft.LOGGER.warn("[PersonalPocket] Return link failed for {}: {}", player.getGameProfile().getName(), result);
      }
      return;
    }

    // Record return location only when entering from outside the pocket.
    CompoundTag returnData = new CompoundTag();
    LinkOptions.setDimensionUID(returnData, LinkingManager.getDimensionUID(serverLevel));
    LinkOptions.setSpawn(returnData, player.blockPosition());
    LinkOptions.setSpawnYaw(returnData, player.getYRot());
    PersonalPocketData.get(serverLevel.getServer()).setReturnLink(player.getUUID(), returnData);

    // Ensure the pocket dimension exists before linking.
    PersonalPocketDimension.getOrCreate(serverLevel.getServer(), player.getUUID());

    // Ensure link data is initialized and up-to-date.
    validate(serverLevel, stack, player);
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    if (LinkOptions.getDimensionUID(tag) == null) {
      LinkOptions.setDimensionUID(tag, PersonalPocketDimension.getPersonalAgeUid(player.getUUID()));
    }
    if (LinkOptions.getSpawn(tag) == null) {
      LinkOptions.setSpawn(tag, PersonalPocketDimension.getPocketSpawn());
    }
    if (LinkOptions.getSpawnYaw(tag) == 0.0f) {
      LinkOptions.setSpawnYaw(tag, player.getYRot());
    }
    ItemStackNbt.setTag(stack, tag);
    onLink(stack, level, entity);
    LinkingManager.LinkResult result = LinkingManager.performLink(player, tag);
    if (result != LinkingManager.LinkResult.SUCCESS) {
      art.arcane.mystcraft.Mystcraft.LOGGER.warn("[PersonalPocket] Link failed for {}: {}", player.getGameProfile().getName(), result);
    }
  }

  @Override
  protected void onLink(@NotNull ItemStack stack, Level level, Entity entity) {
    if (level instanceof ServerLevel serverLevel && PersonalPocketDimension.isPersonalPocket(serverLevel)) {
      return;
    }
    super.onLink(stack, level, entity);
  }
}
