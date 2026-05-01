package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.client.render.BookItemRendererBEWLR;
import art.arcane.mystcraft.item.LinkbookItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * Forge-specific {@link LinkbookItem} that wires the procedural
 * {@link BookItemRendererBEWLR} into the item's client extensions, so the
 * inventory icon is drawn from
 * {@link art.arcane.mystcraft.client.gui.procedural.BookItemTextureFactory}
 * (emerald green vine motif) instead of the static
 * {@code mystcraft:items/linkingbook} texture. Behaviour is otherwise inherited
 * unchanged from {@link LinkbookItem}.
 */
public class ForgeLinkbookItem extends LinkbookItem {

  public ForgeLinkbookItem(Properties properties) {
    super(properties);
  }

  @Override
  public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(new IClientItemExtensions() {
      @Override
      public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return BookItemRendererBEWLR.getInstance();
      }
    });
  }
}
