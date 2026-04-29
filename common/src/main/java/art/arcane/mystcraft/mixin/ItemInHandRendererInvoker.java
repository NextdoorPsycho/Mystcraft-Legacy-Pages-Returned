package art.arcane.mystcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor interface that exposes the private {@code renderMap} method
 * on {@link ItemInHandRenderer}. {@link ItemInHandRendererMixin} uses
 * this to fall back to the vanilla map render when the held stack is an
 * actual {@link net.minecraft.world.item.Items#FILLED_MAP} (rather than
 * a {@link art.arcane.mystcraft.item.PageItem} masquerading as one).
 */
@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererInvoker {

  /**
   * Forwards to {@code ItemInHandRenderer.renderMap}, the private vanilla
   * helper that draws the map paper backing + checkerboard + map data.
   * We only call this for genuine map stacks; pages substitute their own
   * quad render via {@link ItemInHandRendererMixin}.
   */
  @Invoker("renderMap")
  void mystcraft$invokeRenderMap(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ItemStack stack);
}
