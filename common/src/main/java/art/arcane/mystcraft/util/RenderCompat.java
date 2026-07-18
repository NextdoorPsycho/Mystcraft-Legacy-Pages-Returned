package art.arcane.mystcraft.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix3f;

import java.util.function.Supplier;

/**
 * Centralizes the mapped 1.20.1 render calls shared by Mystcraft renderers.
 */
public final class RenderCompat {

  private RenderCompat() {
  }

  public static VertexConsumer vertexNormal(VertexConsumer consumer, Matrix3f normal, float nx, float ny, float nz) {
    return consumer.normal(normal, nx, ny, nz);
  }

  public static ShaderInstance getPositionTexColorNormalShader() {
    return GameRenderer.getPositionTexColorNormalShader();
  }

  public static Supplier<ShaderInstance> positionTexColorNormalShaderSupplier() {
    return GameRenderer::getPositionTexColorNormalShader;
  }
}
