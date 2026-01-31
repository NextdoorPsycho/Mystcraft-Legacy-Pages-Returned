package art.arcane.mystcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

/**
 * Page rendering helper for 1.18.2.
 * Simplified version that renders item icon without D'ni symbols.
 */
public class PageRenderHelper {

  /**
   * Draws a page at the specified location.
   * In 1.18.2, we use a simplified rendering that just shows the item.
   */
  public static void drawPage(PoseStack poseStack, ItemStack stack, int x, int y, int width, int height, int seed) {
    if (stack.isEmpty()) {
      return;
    }

    Minecraft mc = Minecraft.getInstance();
    ItemRenderer itemRenderer = mc.getItemRenderer();

    // Simple item rendering at the specified location
    poseStack.pushPose();
    poseStack.translate(x + width / 2.0 - 8, y + height / 2.0 - 8, 0);
    // Use renderGuiItem for 2D GUI rendering
    itemRenderer.renderGuiItem(stack, 0, 0);
    poseStack.popPose();
  }
}
