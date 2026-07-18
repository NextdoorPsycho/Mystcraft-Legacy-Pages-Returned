package art.arcane.mystcraft.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * The Mystcraft Guidebook - an in-game manual explaining the Art of Writing.
 * This item opens a custom GUI that displays tutorial content about how to use
 * Mystcraft's various systems: ink creation, writing pages, crafting linking
 * books and descriptive books, and understanding the grammar of Age creation.
 */
public class GuidebookItem extends Item {

  public GuidebookItem(Properties properties) {
    super(properties);
  }

  @Override
  @NotNull
  public InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    if (level.isClientSide()) {
      ItemClientHooks.openGuidebook();
    }

    return InteractionResult.CONSUME;
  }
}
