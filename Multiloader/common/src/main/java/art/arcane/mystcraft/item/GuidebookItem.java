package art.arcane.mystcraft.item;

import art.arcane.mystcraft.client.screen.GuidebookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * The Mystcraft Guidebook - an in-game manual explaining the Art of Writing.
 * This item opens a custom GUI that displays tutorial content about how to
 * use Mystcraft's various systems: ink creation, writing pages, crafting
 * linking books and descriptive books, and understanding the grammar of Age creation.
 */
public class GuidebookItem extends Item {

  public GuidebookItem(Properties properties) {
    super(properties);
  }

  @Override
  @NotNull
  public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (level.isClientSide) {
      openGuidebook();
    }

    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }

  private void openGuidebook() {
    Minecraft.getInstance().setScreen(new GuidebookScreen());
  }
}
