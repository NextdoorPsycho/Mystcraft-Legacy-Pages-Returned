package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.item.PageItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * Forge-specific PageItem with custom BEWLR renderer.
 */
public class ForgePageItem extends PageItem {

  public ForgePageItem(Properties properties) {
    super(properties);
  }

  @Override
  public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(new IClientItemExtensions() {
      @Override
      public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return art.arcane.mystcraft.client.render.PageItemRendererBEWLR.getInstance();
      }
    });
  }
}
