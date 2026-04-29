package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolGlyphFactory;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolMotif;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolPageTextureFactory;
import art.arcane.mystcraft.item.PageItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Custom {@link BlockEntityWithoutLevelRenderer} for page items.
 * <p>
 * <b>Procedural-symbol-pages refactor (post phase 1)</b>: the entire
 * pixel-pipeline (loading {@code page_background.png} +
 * {@code symbolcomponents.png} + {@code BufferedImage} composition +
 * async pre-warm queue + manual {@code DynamicTexture} registration)
 * has been replaced with a single delegate to
 * {@link SymbolPageTextureFactory#getPageTexture(ItemStack)}.
 * <p>
 * The factory is fully procedural — it composes a parchment background,
 * the symbol's {@link SymbolMotif} (with glyphs from
 * {@link SymbolGlyphFactory}), and (later phases) flourishes + ink
 * tint on a {@link com.mojang.blaze3d.platform.NativeImage}, caches the
 * result, and returns a {@link ResourceLocation} we can hand to
 * {@link RenderType#entityCutoutNoCull}.
 */
public class PageItemRendererBEWLR extends BlockEntityWithoutLevelRenderer {

  private static PageItemRendererBEWLR instance;

  private PageItemRendererBEWLR() {
    super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
        Minecraft.getInstance().getEntityModels());
  }

  public static PageItemRendererBEWLR getInstance() {
    if (instance == null) {
      instance = new PageItemRendererBEWLR();
    }
    return instance;
  }

  /**
   * Pre-warms the procedural pipeline on a background thread.
   * <p>
   * Called once during client setup from each loader's bootstrap
   * ({@code MystcraftFabricClient}, {@code MystcraftForge}). Replaces
   * the legacy {@code BufferedImage}-based queue with
   * {@link SymbolGlyphFactory#warm()} which iterates every registered
   * symbol's poem words and pre-builds the glyph tiles. The
   * page-level texture cache fills lazily on first render.
   */
  public static void prewarmCache() {
    SymbolGlyphFactory.warm();
  }

  /**
   * Drops every cached page / motif / glyph tile and the associated
   * GPU memory. Call on resource-pack reload so palette changes pick up.
   */
  public static void clearCache() {
    SymbolPageTextureFactory.reset();
    SymbolMotif.invalidateCache();
    SymbolGlyphFactory.reset();
  }

  @Override
  public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    if (!(stack.getItem() instanceof PageItem)) {
      return;
    }

    ResourceLocation texture = SymbolPageTextureFactory.getPageTexture(stack);

    poseStack.pushPose();

    float zOffset = 0.003f;
    int light = (displayContext == ItemDisplayContext.GUI) ? 0xF000F0 : packedLight;

    RenderType renderType = RenderType.entityCutoutNoCull(texture);
    VertexConsumer consumer = bufferSource.getBuffer(renderType);
    Matrix4f matrix = poseStack.last().pose();

    if (displayContext == ItemDisplayContext.GUI) {
      renderQuad(consumer, matrix, 0, 0, 1, 1, 0.5f, zOffset, light, packedOverlay, true);
    } else {
      renderQuad(consumer, matrix, 0, 0, 1, 1, 0.5f, zOffset, light, packedOverlay, true);
      renderQuad(consumer, matrix, 0, 0, 1, 1, 0.5f, zOffset, light, packedOverlay, false);
    }

    poseStack.popPose();
  }

  private void renderQuad(VertexConsumer consumer, Matrix4f matrix,
                          float x1, float y1, float x2, float y2, float z, float zOffset,
                          int light, int overlay, boolean front) {
    float zPos = front ? z + zOffset : z - zOffset;
    float normalZ = front ? 1 : -1;

    if (front) {
      consumer.vertex(matrix, x1, y1, zPos).color(255, 255, 255, 255)
          .uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x2, y1, zPos).color(255, 255, 255, 255)
          .uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x2, y2, zPos).color(255, 255, 255, 255)
          .uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x1, y2, zPos).color(255, 255, 255, 255)
          .uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
    } else {
      consumer.vertex(matrix, x1, y2, zPos).color(255, 255, 255, 255)
          .uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x2, y2, zPos).color(255, 255, 255, 255)
          .uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x2, y1, zPos).color(255, 255, 255, 255)
          .uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x1, y1, zPos).color(255, 255, 255, 255)
          .uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
    }
  }
}
