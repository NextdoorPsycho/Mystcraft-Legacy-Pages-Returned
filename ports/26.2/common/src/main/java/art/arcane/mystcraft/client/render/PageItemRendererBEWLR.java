package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolGlyphFactory;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolMotif;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolPageTextureFactory;
import art.arcane.mystcraft.item.PageItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * 26.2 special-model renderer for procedural symbol pages.
 *
 * <p>The legacy BEWLR class name is retained so loader registration code can
 * migrate without breaking references, but rendering now participates in the
 * extracted {@link SpecialModelRenderer} pipeline.</p>
 */
public final class PageItemRendererBEWLR
    implements SpecialModelRenderer<PageItemRendererBEWLR.RenderData> {

  private static final float Z = 0.5F;
  private static final float Z_OFFSET = 0.003F;
  private static final PageItemRendererBEWLR INSTANCE = new PageItemRendererBEWLR();

  private PageItemRendererBEWLR() {
  }

  public static PageItemRendererBEWLR getInstance() {
    return INSTANCE;
  }

  public static void prewarmCache() {
    SymbolGlyphFactory.warm();
  }

  public static void clearCache() {
    SymbolPageTextureFactory.reset();
    SymbolMotif.invalidateCache();
    SymbolGlyphFactory.reset();
  }

  @Override
  public @Nullable RenderData extractArgument(ItemStack stack) {
    if (!(stack.getItem() instanceof PageItem)) {
      return null;
    }
    return new RenderData(SymbolPageTextureFactory.getPageTexture(stack));
  }

  @Override
  public void submit(@Nullable RenderData data, PoseStack poseStack,
                     SubmitNodeCollector submitNodeCollector, int lightCoords,
                     int overlayCoords, boolean hasFoil, int outlineColor) {
    if (data == null) {
      return;
    }

    submitNodeCollector.submitCustomGeometry(
        poseStack,
        RenderTypes.entityCutout(data.texture()),
        (pose, consumer) -> {
          emitQuad(pose, consumer, Z + Z_OFFSET, 1.0F, lightCoords, overlayCoords);
          emitQuad(pose, consumer, Z - Z_OFFSET, -1.0F, lightCoords, overlayCoords);
        });

    if (hasFoil) {
      submitNodeCollector.submitCustomGeometry(
          poseStack,
          RenderTypes.entityGlint(),
          (pose, consumer) -> emitQuad(
              pose, consumer, Z + Z_OFFSET * 2.0F, 1.0F, lightCoords, overlayCoords));
    }
  }

  private static void emitQuad(PoseStack.Pose pose, VertexConsumer consumer, float z,
                               float normalZ, int lightCoords, int overlayCoords) {
    if (normalZ > 0.0F) {
      emitVertex(pose, consumer, 0, 0, z, 0, 1, normalZ, lightCoords, overlayCoords);
      emitVertex(pose, consumer, 1, 0, z, 1, 1, normalZ, lightCoords, overlayCoords);
      emitVertex(pose, consumer, 1, 1, z, 1, 0, normalZ, lightCoords, overlayCoords);
      emitVertex(pose, consumer, 0, 1, z, 0, 0, normalZ, lightCoords, overlayCoords);
    } else {
      emitVertex(pose, consumer, 0, 1, z, 0, 0, normalZ, lightCoords, overlayCoords);
      emitVertex(pose, consumer, 1, 1, z, 1, 0, normalZ, lightCoords, overlayCoords);
      emitVertex(pose, consumer, 1, 0, z, 1, 1, normalZ, lightCoords, overlayCoords);
      emitVertex(pose, consumer, 0, 0, z, 0, 1, normalZ, lightCoords, overlayCoords);
    }
  }

  private static void emitVertex(PoseStack.Pose pose, VertexConsumer consumer,
                                 float x, float y, float z, float u, float v,
                                 float normalZ, int lightCoords, int overlayCoords) {
    consumer.addVertex(pose, x, y, z)
        .setColor(255, 255, 255, 255)
        .setUv(u, v)
        .setOverlay(overlayCoords)
        .setLight(lightCoords)
        .setNormal(pose, 0, 0, normalZ);
  }

  @Override
  public void getExtents(Consumer<Vector3fc> output) {
    output.accept(new Vector3f(0, 0, Z - Z_OFFSET));
    output.accept(new Vector3f(0, 0, Z + Z_OFFSET));
    output.accept(new Vector3f(1, 0, Z - Z_OFFSET));
    output.accept(new Vector3f(1, 0, Z + Z_OFFSET));
    output.accept(new Vector3f(0, 1, Z - Z_OFFSET));
    output.accept(new Vector3f(0, 1, Z + Z_OFFSET));
    output.accept(new Vector3f(1, 1, Z - Z_OFFSET));
    output.accept(new Vector3f(1, 1, Z + Z_OFFSET));
  }

  public record RenderData(Identifier texture) {
  }
}
