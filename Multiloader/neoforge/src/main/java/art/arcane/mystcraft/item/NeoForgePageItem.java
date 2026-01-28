package art.arcane.mystcraft.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** NeoForge-specific PageItem with custom BEWLR renderer. */
public class NeoForgePageItem extends PageItem {

    public NeoForgePageItem(Properties properties) {
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
