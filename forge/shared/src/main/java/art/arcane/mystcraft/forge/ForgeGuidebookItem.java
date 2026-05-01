package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.client.render.BookItemRendererBEWLR;
import art.arcane.mystcraft.item.GuidebookItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * Forge-specific {@link GuidebookItem} that wires the procedural
 * {@link BookItemRendererBEWLR} into the item's client extensions, so the
 * inventory icon is drawn from
 * {@link art.arcane.mystcraft.client.gui.procedural.BookItemTextureFactory}
 * (midnight blue / antique gold quill motif) instead of the static
 * {@code minecraft:item/book} texture. Behaviour is otherwise inherited
 * unchanged from {@link GuidebookItem}.
 */
public class ForgeGuidebookItem extends GuidebookItem {

  public ForgeGuidebookItem(Properties properties) {
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
