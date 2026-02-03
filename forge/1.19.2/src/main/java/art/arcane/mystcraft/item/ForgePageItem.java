package art.arcane.mystcraft.item;

import java.util.function.Consumer;

/**
 * Forge-specific PageItem with custom BEWLR renderer.
 * 1.19.2: PageItemRendererBEWLR not ported, using base PageItem behavior.
 */
public class ForgePageItem extends PageItem {

  public ForgePageItem(Properties properties) {
    super(properties);
  }

  // 1.19.2: PageItemRendererBEWLR not ported, so no custom renderer
  // @Override
  // public void initializeClient(Consumer<IClientItemExtensions> consumer) {
  //   consumer.accept(new IClientItemExtensions() {
  //     @Override
  //     public BlockEntityWithoutLevelRenderer getCustomRenderer() {
  //       return art.arcane.mystcraft.client.render.PageItemRendererBEWLR.getInstance();
  //     }
  //   });
  // }
}
