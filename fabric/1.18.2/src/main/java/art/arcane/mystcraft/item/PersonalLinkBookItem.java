package art.arcane.mystcraft.item;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.LinkOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
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
 * <p>
 * 1.18.2 version - uses TextComponent/TranslatableComponent instead of Component.literal()/translatable().
 */
public class PersonalLinkBookItem extends LinkbookItem {

  public static final String TOOLTIP_KEY = "item.mystcraft.personal_link_book.tooltip";

  public PersonalLinkBookItem(Properties properties) {
    super(properties.stacksTo(1));
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
    tooltip.add(new TranslatableComponent(TOOLTIP_KEY));
    if (!MystcraftConfig.enablePersonalLinkBooks.get()) {
      tooltip.add(new TextComponent("Disabled in config"));
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

    // TODO: Personal pocket dimension not yet implemented for 1.18.2
    // For now, create a basic tag structure
    CompoundTag tag = new CompoundTag();
    LinkOptions.setDisplayName(tag, "Personal Pocket");
    tag.putFloat("MaxHealth", 10.0f);
    tag.putFloat("damage", 0.0f);
    tag.putBoolean("NoDecay", true);

    stack.setTag(tag);
  }

  @Override
  @NotNull
  public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!level.isClientSide) {
      if (!MystcraftConfig.enablePersonalLinkBooks.get()) {
        player.displayClientMessage(new TextComponent("Personal link books are disabled in the config."), true);
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
        player.displayClientMessage(new TextComponent("Personal link books are disabled in the config."), true);
      }
      return;
    }
    // TODO: Personal pocket dimension linking not yet implemented for 1.18.2
  }
}
