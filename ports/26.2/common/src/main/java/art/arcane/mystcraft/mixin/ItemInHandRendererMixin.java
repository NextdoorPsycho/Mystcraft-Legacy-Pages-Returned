package art.arcane.mystcraft.mixin;

import art.arcane.mystcraft.client.render.PageItemRendererBEWLR;
import art.arcane.mystcraft.item.PageItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Gives symbol pages the vanilla two-handed map pose while submitting their
 * procedural texture through the 26.2 special-model renderer.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

  @Redirect(
      method = "submitArmWithItem",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z"))
  private boolean mystcraft$pageActsAsMap(ItemStack stack, DataComponentType<?> componentType) {
    if (componentType == DataComponents.MAP_ID && stack.getItem() instanceof PageItem) {
      return true;
    }
    return stack.has(componentType);
  }

  @Redirect(
      method = {"renderTwoHandedMap", "renderOneHandedMap"},
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderMap(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/world/item/ItemStack;)V"))
  private void mystcraft$renderPageOrMap(ItemInHandRenderer renderer, PoseStack poseStack,
                                         SubmitNodeCollector submitNodeCollector,
                                         int lightCoords, ItemStack stack) {
    if (stack.getItem() instanceof PageItem) {
      submitHeldPage(poseStack, submitNodeCollector, lightCoords, stack);
    } else {
      ((ItemInHandRendererInvoker) renderer).mystcraft$invokeRenderMap(
          poseStack, submitNodeCollector, lightCoords, stack);
    }
  }

  private static void submitHeldPage(PoseStack poseStack,
                                     SubmitNodeCollector submitNodeCollector,
                                     int lightCoords, ItemStack stack) {
    PageItemRendererBEWLR pageRenderer = PageItemRendererBEWLR.getInstance();
    PageItemRendererBEWLR.RenderData data = pageRenderer.extractArgument(stack);
    if (data == null) {
      return;
    }

    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    poseStack.scale(0.38F, 0.38F, 0.38F);
    poseStack.translate(-0.5F, -0.5F, 0.0F);
    pageRenderer.submit(
        data,
        poseStack,
        submitNodeCollector,
        lightCoords,
        OverlayTexture.NO_OVERLAY,
        stack.hasFoil(),
        0);
    poseStack.popPose();
  }
}
