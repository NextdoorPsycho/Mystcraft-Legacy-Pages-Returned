package art.arcane.mystcraft.mixin;

import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolPageTextureFactory;
import art.arcane.mystcraft.item.PageItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Routes {@link PageItem} stacks through the vanilla {@link
 * net.minecraft.world.item.Items#FILLED_MAP filled-map} render path so
 * they pick up the iconic two-handed (or one-handed offhand) lifted-arms
 * pose. The vanilla map render then calls {@code renderMap} to paint
 * the map paper — we redirect that to draw the procedural symbol-page
 * texture from {@link SymbolPageTextureFactory} instead, leaving the
 * surrounding hand render untouched.
 *
 * <h2>Two interception points</h2>
 * <ol>
 *   <li><b>{@link #mystcraft$pageActsAsMap}</b> — a {@link Redirect} on
 *       the {@code stack.is(Items.FILLED_MAP)} dispatch check inside
 *       {@code renderArmWithItem}. Returns {@code true} for
 *       {@link PageItem} stacks so the method routes them into the
 *       map branch.</li>
 *   <li><b>{@link #mystcraft$renderPageOrMap}</b> — a {@link Redirect}
 *       on the {@code renderMap} call shared by
 *       {@code renderTwoHandedMap} and {@code renderOneHandedMap}.
 *       Substitutes a flat textured quad using the procedural
 *       symbol-page texture for {@link PageItem}; falls through to
 *       the original via {@link ItemInHandRendererInvoker} for actual
 *       maps.</li>
 * </ol>
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

  /**
   * Make {@code stack.is(Items.FILLED_MAP)} return {@code true} when the
   * held stack is a {@link PageItem}. The check itself is otherwise
   * unchanged; we still call {@code stack.is(item)} for any non-FILLED_MAP
   * comparison so this redirect never affects unrelated dispatches.
   */
  @Redirect(
      method = "renderArmWithItem",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
  private boolean mystcraft$pageActsAsMap(ItemStack stack, Item item) {
    if (item == Items.FILLED_MAP && stack.getItem() instanceof PageItem) {
      return true;
    }
    return stack.is(item);
  }

  /**
   * Replaces the inner {@code renderMap} call (responsible for the map
   * paper + checkerboard + map data) with a procedural symbol-page quad
   * when the stack is a {@link PageItem}. For genuine maps we fall
   * through to the vanilla render via {@link ItemInHandRendererInvoker}
   * so map behaviour is unchanged.
   *
   * <p>Coordinate setup mirrors vanilla {@code renderMap} exactly so the
   * page sits at the same depth/scale as a held map and is framed by
   * the same hands rendered by {@code renderMapHand} earlier in the
   * caller.
   */
  @Redirect(
      method = {"renderTwoHandedMap", "renderOneHandedMap"},
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderMap(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ItemStack;)V"))
  private void mystcraft$renderPageOrMap(ItemInHandRenderer self, PoseStack poseStack,
                                         MultiBufferSource buffer, int packedLight,
                                         ItemStack stack) {
    if (stack.getItem() instanceof PageItem) {
      mystcraft$renderHeldPageQuad(poseStack, buffer, packedLight, stack);
    } else {
      ((ItemInHandRendererInvoker) self).mystcraft$invokeRenderMap(poseStack, buffer, packedLight, stack);
    }
  }

  /**
   * Draws a flat textured quad using the procedural page texture from
   * {@link SymbolPageTextureFactory#getPageTexture(ItemStack)}. The
   * coordinate setup ({@code rotate Y 180°, scale 0.38, translate
   * −0.5/−0.5/0}) mirrors vanilla {@code renderMap} so the quad
   * occupies the same plane the map paper would have. Front face only
   * — vanilla maps are also single-sided in this render path.
   */
  private static void mystcraft$renderHeldPageQuad(PoseStack poseStack,
                                                   MultiBufferSource buffer,
                                                   int packedLight,
                                                   ItemStack stack) {
    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    poseStack.scale(0.38F, 0.38F, 0.38F);
    poseStack.translate(-0.5, -0.5, 0.0);

    ResourceLocation pageTex = SymbolPageTextureFactory.getPageTexture(stack);
    VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(pageTex));
    Matrix4f matrix = poseStack.last().pose();

    vc.vertex(matrix, 0, 0, 0).color(255, 255, 255, 255)
        .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
    vc.vertex(matrix, 1, 0, 0).color(255, 255, 255, 255)
        .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
    vc.vertex(matrix, 1, 1, 0).color(255, 255, 255, 255)
        .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
    vc.vertex(matrix, 0, 1, 0).color(255, 255, 255, 255)
        .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();

    poseStack.popPose();
  }
}
