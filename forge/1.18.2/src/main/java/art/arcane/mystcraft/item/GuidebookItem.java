package art.arcane.mystcraft.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * The Mystcraft Guidebook - 1.18.2 version.
 * An in-game manual explaining the Art of Writing.
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
      openGuidebookClient();
    }

    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }

  /**
   * Opens the guidebook screen. This method uses reflection to avoid
   * loading client classes on the dedicated server.
   */
  private void openGuidebookClient() {
    try {
      Class<?> clientHelperClass = Class.forName("art.arcane.mystcraft.client.GuidebookClientHelper");
      clientHelperClass.getMethod("openGuidebook").invoke(null);
    } catch (Exception e) {
      // Silently fail - client helper not available
    }
  }
}
